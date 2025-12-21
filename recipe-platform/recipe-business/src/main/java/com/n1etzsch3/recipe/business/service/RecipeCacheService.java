package com.n1etzsch3.recipe.business.service;

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
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 菜谱详情 Redis 缓存服务
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

    /**
     * 获取菜谱详情（优先从缓存）
     */
    public RecipeDetailDTO getRecipeDetail(Long recipeId) {
        if (recipeId == null) {
            return null;
        }

        String key = CacheConstants.KEY_RECIPE_DETAIL + recipeId;
        RecipeDetailDTO dto = (RecipeDetailDTO) redisTemplate.opsForValue().get(key);

        if (dto == null) {
            RecipeInfo recipe = recipeInfoMapper.selectById(recipeId);
            if (recipe == null) {
                return null;
            }

            dto = new RecipeDetailDTO();
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

            // 写入缓存
            redisTemplate.opsForValue().set(key, dto,
                    CacheConstants.TTL_RECIPE, TimeUnit.SECONDS);
        }

        return dto;
    }

    /**
     * 清除菜谱详情缓存
     */
    public void evictRecipe(Long recipeId) {
        if (recipeId != null) {
            redisTemplate.delete(CacheConstants.KEY_RECIPE_DETAIL + recipeId);
        }
    }

    /**
     * 更新菜谱缓存（先删除，下次访问时重新加载）
     */
    public void refreshRecipe(Long recipeId) {
        evictRecipe(recipeId);
    }
}
