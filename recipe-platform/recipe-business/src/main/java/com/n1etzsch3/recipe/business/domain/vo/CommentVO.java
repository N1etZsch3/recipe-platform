package com.n1etzsch3.recipe.business.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long recipeId;
    private Long userId;
    private Long parentId; // 父评论ID
    private String nickname;
    private String avatar;
    private String content;
    private Integer likeCount; // 点赞数
    private Boolean isLiked; // 当前用户是否已点赞
    private String replyToNickname; // 被回复人昵称（回复时显示）
    private Integer replyCount; // 回复总数
    private List<CommentVO> replies; // 子回复列表（预览）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
