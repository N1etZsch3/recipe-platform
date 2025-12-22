package com.n1etzsch3.recipe.business.service;

import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.common.core.domain.Result;

import java.util.List;

public interface AdminCategoryService {

    Result<List<RecipeCategory>> listCategories();

    Result<?> addCategory(RecipeCategory category);

    Result<?> updateCategory(Integer id, RecipeCategory category);

    Result<?> deleteCategory(Integer id);
}
