package com.n1etzsch3.recipe.business.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 我的评论VO
 */
@Data
public class MyCommentVO {
    private Long id;
    private String content;
    private Long recipeId;
    private String recipeTitle;
    private String recipeCoverImage;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime createTime;

    // 回复相关字段
    private Long parentId; // 父评论ID（如果是回复）
    private String parentContent; // 父评论内容
    private String replyToUserName; // 被回复的用户名
}
