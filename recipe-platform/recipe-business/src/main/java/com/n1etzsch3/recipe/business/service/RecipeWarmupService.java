package com.n1etzsch3.recipe.business.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.RecipeIngredient;
import com.n1etzsch3.recipe.business.entity.RecipeStep;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeIngredientMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeStepMapper;
import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.common.constant.RecipeConstants;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 热门菜谱缓存预热服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeWarmupService {

    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeIngredientMapper ingredientMapper;
    private final RecipeStepMapper stepMapper;
    private final SysUserMapper sysUserMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每5分钟预热热门菜谱缓存
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 10000)
    public void warmupHotRecipes() {
        log.info("开始预热热门菜谱缓存...");
        try {
            // 查询浏览量最高的前20个已发布菜谱
            List<RecipeInfo> hotRecipes = recipeInfoMapper.selectList(
                    new LambdaQueryWrapper<RecipeInfo>()
                            .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PUBLISHED)
                            .orderByDesc(RecipeInfo::getViewCount)
                            .last("LIMIT 20"));

            int warmedCount = 0;
            for (RecipeInfo recipe : hotRecipes) {
                String key = CacheConstants.KEY_RECIPE + recipe.getId();

                // 检查是否已经缓存
                if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    continue;
                }

                // 组装详情 DTO
                RecipeDetailDTO dto = buildRecipeDetail(recipe);

                // 缓存到 Redis (2小时)
                redisTemplate.opsForValue().set(key, dto, CacheConstants.TTL_RECIPE, TimeUnit.SECONDS);
                warmedCount++;
            }

            log.info("热门菜谱缓存预热完成，新增缓存 {} 条", warmedCount);
        } catch (Exception e) {
            log.error("热门菜谱缓存预热失败", e);
        }
    }

    private RecipeDetailDTO buildRecipeDetail(RecipeInfo recipe) {
        RecipeDetailDTO dto = new RecipeDetailDTO();
        BeanUtil.copyProperties(recipe, dto);

        // 作者信息
        SysUser author = sysUserMapper.selectById(recipe.getUserId());
        if (author != null) {
            dto.setAuthorName(author.getNickname());
            dto.setAuthorAvatar(author.getAvatar());
        }

        // 食材
        List<RecipeIngredient> ingredients = ingredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, recipe.getId())
                        .orderByAsc(RecipeIngredient::getSortOrder));
        dto.setIngredients(ingredients);

        // 步骤
        List<RecipeStep> steps = stepMapper.selectList(
                new LambdaQueryWrapper<RecipeStep>()
                        .eq(RecipeStep::getRecipeId, recipe.getId())
                        .orderByAsc(RecipeStep::getStepNo));
        dto.setSteps(steps);

        return dto;
    }
}
