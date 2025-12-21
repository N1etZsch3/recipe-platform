package com.n1etzsch3.recipe.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.query.RecipePageQuery;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.service.RecipeService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeCategoryMapper categoryMapper;

    /**
     * 获取所有分类列表（公开接口）
     */
    @GetMapping("/categories")
    public Result<List<RecipeCategory>> listCategories() {
        log.info("获取分类列表");
        List<RecipeCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<RecipeCategory>()
                        .orderByAsc(RecipeCategory::getSortOrder)
                        .orderByAsc(RecipeCategory::getId));
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
    @PostMapping
    public Result<?> createRecipe(@RequestBody java.util.Map<String, Object> payload) {
        log.info("收到创建菜谱请求: {}", payload);

        // 构建简化的 RecipePublishDTO
        com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO publishDTO = new com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO();

        publishDTO.setTitle((String) payload.get("title"));
        publishDTO.setCoverImage((String) payload.get("coverImage"));
        publishDTO.setDescription((String) payload.get("content"));

        // 分类转换：前端发送字符串，需要映射到 categoryId
        String category = (String) payload.get("category");
        Integer categoryId = mapCategoryToId(category);
        publishDTO.setCategoryId(categoryId);

        // 设置空的食材和步骤列表（简化版）
        publishDTO.setIngredients(java.util.Collections.emptyList());
        publishDTO.setSteps(java.util.Collections.emptyList());

        return recipeService.publishRecipe(publishDTO);
    }

    /**
     * 更新菜谱
     */
    @PutMapping
    public Result<?> updateRecipe(@RequestBody java.util.Map<String, Object> payload) {
        log.info("收到更新菜谱请求: {}", payload);

        Long id = Long.valueOf(payload.get("id").toString());

        // 构建更新的 RecipePublishDTO
        com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO publishDTO = new com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO();

        publishDTO.setId(id);
        publishDTO.setTitle((String) payload.get("title"));
        publishDTO.setCoverImage((String) payload.get("coverImage"));
        publishDTO.setDescription((String) payload.get("content"));

        // 分类转换
        String category = (String) payload.get("category");
        Integer categoryId = mapCategoryToId(category);
        publishDTO.setCategoryId(categoryId);

        // 设置空的食材和步骤列表（简化版）
        publishDTO.setIngredients(java.util.Collections.emptyList());
        publishDTO.setSteps(java.util.Collections.emptyList());

        return recipeService.updateRecipe(publishDTO);
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
     * 分类名称映射到 ID
     */
    private Integer mapCategoryToId(String category) {
        if (category == null)
            return 1; // 默认家常菜
        return switch (category) {
            case "家常菜" -> 1;
            case "下饭菜" -> 2;
            case "烘焙" -> 3;
            case "肉类" -> 4;
            case "汤羹" -> 5;
            case "主食" -> 6;
            case "小吃" -> 7;
            case "甜品" -> 9;
            case "其他" -> 8;
            default -> 1;
        };
    }
}
