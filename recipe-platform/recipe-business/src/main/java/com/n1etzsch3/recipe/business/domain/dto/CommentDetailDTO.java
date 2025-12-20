package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDetailDTO {
    private Long id;
    private Long recipeId;
    private String recipeTitle;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private String content;
    private LocalDateTime createTime;
}
