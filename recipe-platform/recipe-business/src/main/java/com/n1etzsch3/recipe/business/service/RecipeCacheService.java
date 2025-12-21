package com.n1etzsch3.recipe.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.RecipeIngredient;
import com.n1etzsch3.recipe.business.entity.RecipeStep;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeIngredientMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeStepMapper;
import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.service.RedisUserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 菜谱详情 Redis 缓存服务
 * 包含缓存穿透、雪崩防护
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeIngredientMapper ingredientMapper;
    private final RecipeStepMapper stepMapper;
    private final RecipeCategoryMapper categoryMapper;
    private final RedisUserCacheService userCacheService;

    // 空值标记，用于缓存穿透防护
    private static final String NULL_PLACEHOLDER = "NULL";
    private static final long NULL_TTL = 60; // 空值缓存60秒
    private static final long TTL_RANDOM_OFFSET = 300; // 随机偏移防止雪崩

    /**
     * 获取菜谱详情（优先从缓存）
     * 包含缓存穿透和雪崩防护
     */
    public RecipeDetailDTO getRecipeDetail(Long recipeId) {
        if (recipeId == null) {
            return null;
        }

        String key = CacheConstants.KEY_RECIPE_DETAIL + recipeId;
        Object cached = redisTemplate.opsForValue().get(key);

        // 1. 命中空值缓存（防止穿透）
        if (NULL_PLACEHOLDER.equals(cached)) {
            log.debug("命中空值缓存: {}", key);
            return null;
        }

        // 2. 命中正常缓存
        if (cached != null) {
            log.debug("命中缓存: {}", key);
            return (RecipeDetailDTO) cached;
        }

        // 3. 缓存未命中，查询数据库
        RecipeInfo recipe = recipeInfoMapper.selectById(recipeId);

        if (recipe == null) {
            // 缓存空值防止穿透（短TTL）
            redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, NULL_TTL, TimeUnit.SECONDS);
            log.info("缓存空值防穿透: {}", key);
            return null;
        }

        // 4. 构建DTO
        RecipeDetailDTO dto = buildRecipeDetailDTO(recipe, recipeId);

        // 5. 写入缓存，添加随机偏移防止雪崩
        long ttl = CacheConstants.TTL_RECIPE + RandomUtil.randomLong(0, TTL_RANDOM_OFFSET);
        redisTemplate.opsForValue().set(key, dto, ttl, TimeUnit.SECONDS);
        log.info("写入缓存: {} TTL={}s", key, ttl);

        return dto;
    }

    /**
     * 构建菜谱详情DTO
     */
    private RecipeDetailDTO buildRecipeDetailDTO(RecipeInfo recipe, Long recipeId) {
        RecipeDetailDTO dto = new RecipeDetailDTO();
        BeanUtil.copyProperties(recipe, dto);

        // 获取食材
        List<RecipeIngredient> ingredients = ingredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, recipeId)
                        .orderByAsc(RecipeIngredient::getSortOrder));
        dto.setIngredients(ingredients);

        // 获取步骤
        List<RecipeStep> steps = stepMapper.selectList(
                new LambdaQueryWrapper<RecipeStep>()
                        .eq(RecipeStep::getRecipeId, recipeId)
                        .orderByAsc(RecipeStep::getStepNo));
        dto.setSteps(steps);

        // 获取分类名称
        RecipeCategory category = categoryMapper.selectById(recipe.getCategoryId());
        if (category != null) {
            dto.setCategoryName(category.getName());
        }

        // 获取作者信息（使用用户缓存服务）
        SysUser author = userCacheService.getUserById(recipe.getUserId());
        if (author != null) {
            dto.setAuthorId(author.getId());
            dto.setAuthorName(author.getNickname());
            dto.setAuthorAvatar(author.getAvatar());
        }

        return dto;
    }

    /**
     * 清除菜谱详情缓存
     */
    public void evictRecipe(Long recipeId) {
        if (recipeId != null) {
            String key = CacheConstants.KEY_RECIPE_DETAIL + recipeId;
            redisTemplate.delete(key);
            log.info("清除缓存: {}", key);
        }
    }

    /**
     * 更新菜谱缓存（先删除，下次访问时重新加载）
     */
    public void refreshRecipe(Long recipeId) {
        evictRecipe(recipeId);
    }
}
