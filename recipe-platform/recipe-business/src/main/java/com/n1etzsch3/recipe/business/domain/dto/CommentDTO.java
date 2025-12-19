package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;

@Data
public class CommentDTO {
    private Long recipeId;
    private Long parentId; // 回复的父评论ID，null表示顶级评论
    private String content;
}
