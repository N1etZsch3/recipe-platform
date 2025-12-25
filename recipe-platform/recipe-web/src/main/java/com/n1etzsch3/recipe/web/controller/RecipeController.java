package com.n1etzsch3.recipe.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO;
import com.n1etzsch3.recipe.business.domain.dto.SimpleRecipeDTO;
import com.n1etzsch3.recipe.business.domain.query.RecipePageQuery;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.service.CategoryService;
import com.n1etzsch3.recipe.business.service.RecipeService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.framework.annotation.Idempotent;
import com.n1etzsch3.recipe.framework.annotation.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final CategoryService categoryService;

    /**
     * 获取所有分类列表（公开接口）
     */
    @Cacheable(value = CacheConstants.CACHE_CATEGORIES, key = "'all'", unless = "#result.data == null")
    @GetMapping("/categories")
    public Result<List<RecipeCategory>> listCategories() {
        log.info("获取分类列表");
        List<RecipeCategory> categories = categoryService.lambdaQuery()
                .orderByAsc(RecipeCategory::getSortOrder)
                .orderByAsc(RecipeCategory::getId)
                .list();
        return Result.ok(categories);
    }

    /**
     * 获取菜谱详情
     */
    @GetMapping("/{id}")
    public Result<RecipeDetailDTO> getDetail(@PathVariable Long id) {
        log.info("收到获取菜谱详情请求: {}", id);
        return recipeService.getRecipeDetail(id);
    }

    /**
     * 分页查询菜谱列表
     */
    @GetMapping("/list")
    public Result<IPage<RecipeDetailDTO>> pageList(RecipePageQuery query) {
        log.info("收到分页查询请求: {}", query);
        return recipeService.pageRecipes(query);
    }

    /**
     * 创建/发布菜谱（简化版 - 前端提交）
     */
    @Idempotent
    @RateLimit(time = 60, count = 10, limitType = RateLimit.LimitType.USER) // 对可能造成的高流量消耗进行限流
    @PostMapping
    public Result<?> createRecipe(@RequestBody @Valid SimpleRecipeDTO dto) {
        log.info("收到创建菜谱请求: title={}, category={}", dto.getTitle(), dto.getCategory());

        RecipePublishDTO publishDTO = buildPublishDTO(dto);

        return recipeService.publishRecipe(publishDTO);
    }

    /**
     * 更新菜谱
     */
    @PutMapping
    public Result<?> updateRecipe(@RequestBody @Valid SimpleRecipeDTO dto) {
        log.info("收到更新菜谱请求: id={}, title={}", dto.getId(), dto.getTitle());

        RecipePublishDTO publishDTO = buildPublishDTO(dto);
        publishDTO.setId(dto.getId());

        return recipeService.updateRecipe(publishDTO);
    }

    private RecipePublishDTO buildPublishDTO(SimpleRecipeDTO dto) {
        // 构建 RecipePublishDTO
        RecipePublishDTO publishDTO = new RecipePublishDTO();
        publishDTO.setTitle(dto.getTitle());
        publishDTO.setCoverImage(dto.getCoverImage());
        publishDTO.setDescription(dto.getDescription());

        // 分类转换：使用 CategoryService 动态获取分类ID
        Integer categoryId = categoryService.getIdByName(dto.getCategory());
        publishDTO.setCategoryId(categoryId);

        // 映射用料列表
        if (dto.getIngredients() != null && !dto.getIngredients().isEmpty()) {
            List<RecipePublishDTO.IngredientDTO> ingredients = dto.getIngredients().stream()
                    .filter(ing -> ing.getName() != null && !ing.getName().isBlank())
                    .map(ing -> {
                        RecipePublishDTO.IngredientDTO ingredientDTO = new RecipePublishDTO.IngredientDTO();
                        ingredientDTO.setName(ing.getName().trim());
                        ingredientDTO.setAmount(ing.getAmount() != null ? ing.getAmount().trim() : "适量");
                        ingredientDTO.setSortOrder(ing.getSortOrder());
                        return ingredientDTO;
                    })
                    .toList();
            publishDTO.setIngredients(ingredients);
        } else {
            publishDTO.setIngredients(Collections.emptyList());
        }

        // 映射步骤列表
        if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
            List<RecipePublishDTO.StepDTO> steps = new java.util.ArrayList<>();
            int stepNo = 1;
            for (SimpleRecipeDTO.StepDTO step : dto.getSteps()) {
                if (step.getDescription() != null && !step.getDescription().isBlank()) {
                    RecipePublishDTO.StepDTO stepDTO = new RecipePublishDTO.StepDTO();
                    stepDTO.setStepNo(step.getStepNo() != null ? step.getStepNo() : stepNo);
                    stepDTO.setDescription(step.getDescription().trim());
                    stepDTO.setImageUrl(step.getImageUrl());
                    steps.add(stepDTO);
                    stepNo++;
                }
            }
            publishDTO.setSteps(steps);
        } else {
            publishDTO.setSteps(Collections.emptyList());
        }

        return publishDTO;
    }

    /**
     * 下架菜谱（将已发布状态改为待审核，以便用户编辑）
     */
    @PostMapping("/{id}/unpublish")
    public Result<?> unpublishRecipe(@PathVariable Long id) {
        log.info("收到下架菜谱请求: {}", id);
        return recipeService.unpublishRecipe(id);
    }

    /**
     * 撤销发布（将待审核状态改为草稿）
     */
    @PostMapping("/{id}/withdraw")
    public Result<?> withdrawRecipe(@PathVariable Long id) {
        log.info("收到撤销发布请求: {}", id);
        return recipeService.withdrawRecipe(id);
    }

    /**
     * 删除菜谱（用户只能删除自己的菜谱）
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteRecipe(@PathVariable Long id) {
        log.info("收到删除菜谱请求: {}", id);
        return recipeService.deleteRecipe(id);
    }
}
