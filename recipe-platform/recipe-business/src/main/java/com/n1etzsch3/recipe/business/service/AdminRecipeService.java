package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.common.core.domain.Result;

import java.util.List;

public interface AdminRecipeService {

    Result<IPage<RecipeDetailDTO>> pageAuditRecipes(Integer page, Integer size);

    Result<?> auditRecipe(AuditDTO auditDTO);

    Result<IPage<RecipeDetailDTO>> pageAllRecipes(Integer page, Integer size, Integer status, String keyword);

    Result<?> deleteRecipe(Long recipeId);

    Result<?> batchAuditRecipes(List<Long> ids, String action, String reason);

    Result<?> batchUpdateRecipeStatus(List<Long> ids, Integer status);
}
