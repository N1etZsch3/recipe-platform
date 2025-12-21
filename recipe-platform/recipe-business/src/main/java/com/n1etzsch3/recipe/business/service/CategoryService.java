package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;

public interface CategoryService extends IService<RecipeCategory> {

    /**
     * 根据分类名称获取分类ID（带缓存）
     * 
     * @param name 分类名称
     * @return 分类ID，如果不存在则返回默认分类ID（1）
     */
    Integer getIdByName(String name);
}
