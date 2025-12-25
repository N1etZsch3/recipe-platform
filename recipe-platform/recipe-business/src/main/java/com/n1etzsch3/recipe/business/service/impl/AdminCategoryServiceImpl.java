package com.n1etzsch3.recipe.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.service.AdminCategoryService;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final RecipeCategoryMapper categoryMapper;
    private final RecipeInfoMapper recipeInfoMapper;
    private final AdminLogService adminLogService;

    @Override
    @Cacheable(value = CacheConstants.CACHE_CATEGORIES, key = "'all'")
    public Result<List<RecipeCategory>> listCategories() {
        List<RecipeCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<RecipeCategory>()
                .orderByAsc(RecipeCategory::getSortOrder)
                .orderByAsc(RecipeCategory::getId));
        return Result.ok(categories);
    }

    @Override
    @CacheEvict(value = CacheConstants.CACHE_CATEGORIES, key = "'all'")
    public Result<?> addCategory(RecipeCategory category) {
        // 检查分类名是否已存在
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<RecipeCategory>()
                .eq(RecipeCategory::getName, category.getName()));
        if (count > 0) {
            return Result.fail("分类名称已存在");
        }

        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        adminLogService.log("CATEGORY_ADD", "category", Long.valueOf(category.getId()), category.getName(), null);
        return Result.ok("添加成功");
    }

    @Override
    @CacheEvict(value = CacheConstants.CACHE_CATEGORIES, key = "'all'")
    public Result<?> updateCategory(Integer id, RecipeCategory category) {
        RecipeCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            return Result.fail("分类不存在");
        }

        // 检查分类名是否已被其他分类使用
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<RecipeCategory>()
                .eq(RecipeCategory::getName, category.getName())
                .ne(RecipeCategory::getId, id));
        if (count > 0) {
            return Result.fail("分类名称已存在");
        }

        existing.setName(category.getName());
        existing.setSortOrder(category.getSortOrder());
        categoryMapper.updateById(existing);
        adminLogService.log("CATEGORY_UPDATE", "category", Long.valueOf(id), existing.getName(), null);
        return Result.ok("修改成功");
    }

    @Override
    @CacheEvict(value = CacheConstants.CACHE_CATEGORIES, key = "'all'")
    public Result<?> deleteCategory(Integer id) {
        Long count = recipeInfoMapper.selectCount(new LambdaQueryWrapper<RecipeInfo>()
                .eq(RecipeInfo::getCategoryId, id));
        if (count > 0) {
            return Result.fail("该分类下存在菜谱，无法删除");
        }
        RecipeCategory category = categoryMapper.selectById(id);
        String categoryName = category != null ? category.getName() : "ID:" + id;
        categoryMapper.deleteById(id);
        adminLogService.log("CATEGORY_DELETE", "category", Long.valueOf(id), categoryName, null);
        return Result.ok("删除成功");
    }
}
