package com.n1etzsch3.recipe.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<RecipeCategoryMapper, RecipeCategory> implements CategoryService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "recipe:category:name2id";
    private static final Integer DEFAULT_CATEGORY_ID = 1; // 默认分类ID

    @Override
    public Integer getIdByName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_CATEGORY_ID;
        }

        try {
            // 1. 尝试从缓存获取
            Object cached = redisTemplate.opsForHash().get(CACHE_KEY, name);
            if (cached != null) {
                log.debug("从缓存获取分类ID: {} -> {}", name, cached);
                return (Integer) cached;
            }

            // 2. 查询数据库
            RecipeCategory category = this.lambdaQuery()
                    .eq(RecipeCategory::getName, name)
                    .one();

            Integer id = category != null ? category.getId() : DEFAULT_CATEGORY_ID;
            log.info("从数据库查询分类: {} -> {}", name, id);

            // 3. 写入缓存（24小时过期）
            redisTemplate.opsForHash().put(CACHE_KEY, name, id);
            redisTemplate.expire(CACHE_KEY, 24, TimeUnit.HOURS);

            return id;
        } catch (Exception e) {
            log.error("获取分类ID失败: {}", name, e);
            return DEFAULT_CATEGORY_ID;
        }
    }
}
