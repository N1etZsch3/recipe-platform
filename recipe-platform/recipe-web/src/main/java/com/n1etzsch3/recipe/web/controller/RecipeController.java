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
    @Cacheable(value = CacheConstants.CACHE_CATEGORIES, key = "'list'", unless = "#result.data == null")
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
    @RateLimit(time = 60, count = 10, limitType = RateLimit.LimitType.USER)  // 对可能造成的高流量消耗进行限流
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
        publishDTO.setDescription(dto.getContent());

        // 分类转换：使用 CategoryService 动态获取分类ID
        Integer categoryId = categoryService.getIdByName(dto.getCategory());
        publishDTO.setCategoryId(categoryId);

        // 设置空的食材和步骤列表（简化版）
        // TODO: 前端提交完整结构后，在此映射 ingredients/steps（含排序字段），并移除空列表占位。
        publishDTO.setIngredients(Collections.emptyList());
        publishDTO.setSteps(Collections.emptyList());

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
}
