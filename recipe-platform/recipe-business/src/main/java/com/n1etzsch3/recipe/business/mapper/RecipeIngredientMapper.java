package com.n1etzsch3.recipe.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.n1etzsch3.recipe.business.entity.RecipeIngredient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecipeIngredientMapper extends BaseMapper<RecipeIngredient> {
}
