package com.n1etzsch3.recipe.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.service.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<RecipeCategoryMapper, RecipeCategory> implements CategoryService {
}
