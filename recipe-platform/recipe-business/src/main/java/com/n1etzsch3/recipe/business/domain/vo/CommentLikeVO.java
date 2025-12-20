package com.n1etzsch3.recipe.business.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论点赞VO (收到的赞)
 */
@Data
public class CommentLikeVO {
    private Long commentId;
    private String commentContent;

    // 关联菜谱
    private Long recipeId;
    private String recipeTitle;

    // 点赞总数
    private Integer likeCount;

    // 最近点赞者列表 (用于预览)
    private List<LikerInfo> likers;

    // 最近点赞时间
    private LocalDateTime latestLikeTime;

    @Data
    public static class LikerInfo {
        private Long userId;
        private String nickname;
        private String avatar;
        private LocalDateTime likeTime;
    }
}
