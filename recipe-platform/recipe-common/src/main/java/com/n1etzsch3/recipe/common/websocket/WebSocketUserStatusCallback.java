package com.n1etzsch3.recipe.common.websocket;

/**
 * WebSocket 用户状态变化回调接口
 * 用于通知其他模块用户的上线/离线状态变化
 * 
 * 放在 common 模块避免 framework 和 business 模块的循环依赖
 */
public interface WebSocketUserStatusCallback {

    /**
     * 用户上线时调用
     *
     * @param userId   用户ID
     * @param nickname 用户昵称（可能为null）
     */
    void onUserOnline(Long userId, String nickname);

    /**
     * 用户离线时调用
     *
     * @param userId 用户ID
     */
    void onUserOffline(Long userId);
}
