package com.n1etzsch3.recipe.common.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 推送消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 相关业务 ID (如菜谱ID、消息ID等)
     */
    private Long relatedId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 发送者昵称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 消息附带图片 (如菜谱封面、评论图片等)
     */
    private String imageUrl;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
}
