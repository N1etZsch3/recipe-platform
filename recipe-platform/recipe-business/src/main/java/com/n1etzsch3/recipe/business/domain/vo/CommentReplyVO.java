package com.n1etzsch3.recipe.business.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论回复VO (回复我的)
 */
@Data
public class CommentReplyVO {
    private Long id;
    private String content;

    // 回复者信息
    private Long replyUserId;
    private String replyUserName;
    private String replyUserAvatar;

    // 我的原评论
    private Long myCommentId;
    private String myCommentContent;

    // 关联菜谱
    private Long recipeId;
    private String recipeTitle;

    private LocalDateTime createTime;
}
