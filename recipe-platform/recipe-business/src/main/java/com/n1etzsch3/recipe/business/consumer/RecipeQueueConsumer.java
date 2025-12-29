package com.n1etzsch3.recipe.business.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.business.domain.dto.ValidationResult;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.RecipeStep;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeStepMapper;
import com.n1etzsch3.recipe.business.service.ContentValidator;
import com.n1etzsch3.recipe.business.service.NotificationService;
import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.common.constant.RecipeConstants;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 菜谱发布队列消费者
 * 使用 Redis Streams 实现异步处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeQueueConsumer {

    private final StringRedisTemplate redisTemplate;
    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeStepMapper recipeStepMapper;
    private final SysUserMapper sysUserMapper;
    private final ContentValidator contentValidator;
    private final NotificationService notificationService;

    private static final String STREAM_KEY = CacheConstants.STREAM_RECIPE_PUBLISH;
    private static final String GROUP_NAME = CacheConstants.STREAM_GROUP_RECIPE;
    private static final String CONSUMER_NAME = CacheConstants.STREAM_CONSUMER_PREFIX + "1";

    /**
     * 初始化消费者组
     */
    @PostConstruct
    public void initConsumerGroup() {
        try {
            // 创建 Stream（如果不存在）
            Boolean hasKey = redisTemplate.hasKey(STREAM_KEY);
            if (Boolean.FALSE.equals(hasKey)) {
                // 添加一条初始消息来创建 Stream，然后立即删除
                redisTemplate.opsForStream().add(STREAM_KEY, Map.of("init", "true"));
                log.info("创建 Redis Stream: {}", STREAM_KEY);
            }

            // 创建消费者组
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
            log.info("创建消费者组: {}", GROUP_NAME);
        } catch (Exception e) {
            // 组已存在时会抛出异常，可以忽略
            if (!e.getMessage().contains("BUSYGROUP")) {
                log.warn("初始化消费者组时出现警告: {}", e.getMessage());
            }
        }
    }

    /**
     * 定时消费队列消息
     * 每秒执行一次
     */
    @Scheduled(fixedDelay = 1000)
    public void consume() {
        try {
            // 从消费者组读取消息
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                    Consumer.from(GROUP_NAME, CONSUMER_NAME),
                    StreamReadOptions.empty().count(5).block(Duration.ofMillis(500)),
                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (records == null || records.isEmpty()) {
                return;
            }

            log.debug("收到 {} 条待处理消息", records.size());

            for (MapRecord<String, Object, Object> record : records) {
                processRecord(record);
            }
        } catch (Exception e) {
            // 避免频繁打印相同错误
            if (!e.getMessage().contains("NOGROUP")) {
                log.error("消费队列异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 处理单条消息
     */
    private void processRecord(MapRecord<String, Object, Object> record) {
        String recordId = record.getId().getValue();
        Map<Object, Object> value = record.getValue();

        // 跳过初始化消息
        if (value.containsKey("init")) {
            acknowledge(record);
            return;
        }

        String recipeIdStr = (String) value.get("recipeId");
        String userIdStr = (String) value.get("userId");

        if (recipeIdStr == null || userIdStr == null) {
            log.warn("消息格式错误，跳过: recordId={}", recordId);
            acknowledge(record);
            return;
        }

        Long recipeId = Long.parseLong(recipeIdStr);
        Long userId = Long.parseLong(userIdStr);

        try {
            // 获取菜谱信息
            RecipeInfo recipe = recipeInfoMapper.selectById(recipeId);
            if (recipe == null) {
                log.warn("菜谱不存在，跳过: recipeId={}", recipeId);
                acknowledge(record);
                return;
            }

            // 只处理「处理中」状态的菜谱
            if (recipe.getStatus() != RecipeConstants.STATUS_PROCESSING) {
                log.debug("菜谱状态不是处理中，跳过: recipeId={}, status={}", recipeId, recipe.getStatus());
                acknowledge(record);
                return;
            }

            // 获取步骤信息用于验证
            List<RecipeStep> steps = recipeStepMapper.selectList(
                    new LambdaQueryWrapper<RecipeStep>()
                            .eq(RecipeStep::getRecipeId, recipeId));

            // 内容预审
            ValidationResult result = contentValidator.validate(recipe, steps);

            if (result.isPassed()) {
                // 预审通过 → 待审核
                recipe.setStatus(RecipeConstants.STATUS_PENDING);
                recipe.setUpdateTime(LocalDateTime.now());
                recipeInfoMapper.updateById(recipe);

                // 获取作者信息
                SysUser author = sysUserMapper.selectById(userId);
                String authorName = author != null ? author.getNickname() : "用户" + userId;

                // 通知管理员有新菜谱待审核
                notificationService.sendNewRecipePending(recipeId, recipe.getTitle(), userId, authorName,
                        recipe.getCoverImage());

                log.info("菜谱预审通过，已进入待审核队列: recipeId={}, title={}", recipeId, recipe.getTitle());
            } else {
                // 预审失败 → 退回草稿
                recipe.setStatus(RecipeConstants.STATUS_DRAFT);
                recipe.setRejectReason("自动检测：" + result.getReason());
                recipe.setUpdateTime(LocalDateTime.now());
                recipeInfoMapper.updateById(recipe);

                // 通知用户
                notificationService.sendRecipeRejected(userId, recipeId, recipe.getTitle(), result.getReason());

                log.info("菜谱预审未通过，已退回草稿: recipeId={}, reason={}", recipeId, result.getReason());
            }

            // 确认消息处理完成
            acknowledge(record);

        } catch (Exception e) {
            log.error("处理菜谱 {} 失败: {}", recipeId, e.getMessage(), e);
            // 不 ACK，让消息保留在 pending 列表中，后续可以重试
        }
    }

    /**
     * 确认消息已处理
     */
    private void acknowledge(MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
    }
}
