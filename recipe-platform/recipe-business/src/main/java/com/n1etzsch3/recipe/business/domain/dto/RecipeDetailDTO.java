package com.n1etzsch3.recipe.business.domain.dto;

import com.n1etzsch3.recipe.business.entity.RecipeIngredient;
import com.n1etzsch3.recipe.business.entity.RecipeStep;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecipeDetailDTO {
    private Long id;
    private String title;
    private Long userId;
    private String authorName; // 需要关联查询
    private String authorAvatar;
    private Integer categoryId;
    private String categoryName; // 需要关联查询
    private String coverImage;
    private String description;
    private Integer status;
    private Integer viewCount;
    private Integer commentCount; // 评论数
    private Integer favoriteCount; // 收藏数
    private LocalDateTime createTime;

    // 是否收藏/关注 (针对当前登录用户)
    private Boolean isFavorite;
    private Boolean isFollow;

    private List<RecipeIngredient> ingredients;
    private List<RecipeStep> steps;
}
