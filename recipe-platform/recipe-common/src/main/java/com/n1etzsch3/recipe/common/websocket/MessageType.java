package com.n1etzsch3.recipe.common.websocket;

/**
 * WebSocket 消息类型枚举
 */
public enum MessageType {
    // ========== 系统消息 ==========
    /**
     * 连接成功
     */
    CONNECTED,

    /**
     * 心跳
     */
    HEARTBEAT,

    // ========== 业务消息 ==========
    /**
     * 菜谱审核通过
     */
    RECIPE_APPROVED,

    /**
     * 菜谱审核驳回
     */
    RECIPE_REJECTED,

    /**
     * 新私信
     */
    NEW_MESSAGE,

    /**
     * 新关注者
     */
    NEW_FOLLOWER,

    /**
     * 新评论
     */
    NEW_COMMENT,

    /**
     * 评论被回复
     */
    COMMENT_REPLY,

    /**
     * 评论被点赞
     */
    COMMENT_LIKED,

    /**
     * 新菜谱待审核（通知管理员）
     */
    NEW_RECIPE_PENDING,

    /**
     * 强制下线（用户被踢出或在其他设备登录）
     */
    FORCED_LOGOUT,

    // ========== 管理员实时推送 ==========
    /**
     * 用户上线（通知管理员）
     */
    USER_ONLINE,

    /**
     * 用户离线（通知管理员）
     */
    USER_OFFLINE,

    /**
     * 新评论（通知管理员）
     */
    ADMIN_NEW_COMMENT,

    /**
     * 菜谱撤销审核/删除（通知管理员）
     */
    RECIPE_WITHDRAWN
}
