package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO;
import com.n1etzsch3.recipe.business.domain.query.RecipePageQuery;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.common.core.domain.Result;

public interface RecipeService extends IService<RecipeInfo> {

    /**
     * 发布菜谱
     */
    Result<?> publishRecipe(RecipePublishDTO publishDTO);

    /**
     * 获取菜谱详情
     */
    Result<RecipeDetailDTO> getRecipeDetail(Long id);

    /**
     * 分页查询菜谱
     */
    Result<IPage<RecipeDetailDTO>> pageRecipes(RecipePageQuery query);

    /**
     * 修改菜谱
     */
    Result<?> updateRecipe(RecipePublishDTO publishDTO);

    /**
     * 删除菜谱 (逻辑删除)
     */
    Result<?> deleteRecipe(Long id);

    /**
     * 下架菜谱（将已发布状态改为待审核）
     */
    Result<?> unpublishRecipe(Long id);

    /**
     * 撤销发布（将待审核状态改为草稿）
     */
    Result<?> withdrawRecipe(Long id);
}
