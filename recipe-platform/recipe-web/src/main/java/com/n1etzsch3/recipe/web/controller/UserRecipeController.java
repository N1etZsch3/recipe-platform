package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO;
import com.n1etzsch3.recipe.business.service.RecipeService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/my/recipes")
@RequiredArgsConstructor
public class UserRecipeController {

    private final RecipeService recipeService;

    /**
     * 发布菜谱
     */
    @PostMapping
    public Result<?> publish(@RequestBody @jakarta.validation.Valid RecipePublishDTO publishDTO) {
        return recipeService.publishRecipe(publishDTO);
    }

    /**
     * 修改菜谱
     */
    @PutMapping
    public Result<?> update(@RequestBody @jakarta.validation.Valid RecipePublishDTO publishDTO) {
        return recipeService.updateRecipe(publishDTO);
    }

    /**
     * 删除菜谱
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return recipeService.deleteRecipe(id);
    }

    // TODO: 获取我的发布列表
}
