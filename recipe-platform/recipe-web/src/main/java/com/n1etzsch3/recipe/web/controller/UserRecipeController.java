package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO;
import com.n1etzsch3.recipe.business.service.RecipeService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/my/recipes")
@RequiredArgsConstructor
public class UserRecipeController {

    private final RecipeService recipeService;

    /**
     * 发布菜谱
     */
    @PostMapping
    public Result<?> publish(@RequestBody @Valid RecipePublishDTO publishDTO) {
        log.info("发布菜谱: title={}", publishDTO.getTitle());
        return recipeService.publishRecipe(publishDTO);
    }

    /**
     * 修改菜谱
     */
    @PutMapping
    public Result<?> update(@RequestBody @Valid RecipePublishDTO publishDTO) {
        log.info("修改菜谱: id={}, title={}", publishDTO.getId(), publishDTO.getTitle());
        return recipeService.updateRecipe(publishDTO);
    }

    /**
     * 删除菜谱
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        log.info("删除菜谱: id={}", id);
        return recipeService.deleteRecipe(id);
    }

    // TODO: 获取我的发布列表
}
