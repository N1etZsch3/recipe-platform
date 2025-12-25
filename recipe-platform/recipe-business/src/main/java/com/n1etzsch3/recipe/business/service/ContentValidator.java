package com.n1etzsch3.recipe.business.service;

import com.n1etzsch3.recipe.business.domain.dto.ValidationResult;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.RecipeStep;

import java.util.List;

/**
 * 菜谱内容验证服务
 */
public interface ContentValidator {

    /**
     * 验证菜谱内容
     * 
     * @param recipe 菜谱信息
     * @param steps  菜谱步骤（可选）
     * @return 验证结果
     */
    ValidationResult validate(RecipeInfo recipe, List<RecipeStep> steps);
}
