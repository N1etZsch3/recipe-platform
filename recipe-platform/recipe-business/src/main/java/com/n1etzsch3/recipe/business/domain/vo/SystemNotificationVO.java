package com.n1etzsch3.recipe.business.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统通知VO
 */
@Data
public class SystemNotificationVO {
    private Long id;

    /**
     * 通知类型: RECIPE_APPROVED, RECIPE_REJECTED, COMMENT_DELETED
     */
    private String type;

    private String title;
    private String content;

    // 关联ID (如菜谱ID)
    private Long relatedId;

    // 是否已读
    private Boolean read;

    private LocalDateTime createTime;
}
