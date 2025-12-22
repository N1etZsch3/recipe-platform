package com.n1etzsch3.recipe.business.config;

import com.n1etzsch3.recipe.business.service.NotificationService;
import com.n1etzsch3.recipe.common.websocket.WebSocketUserStatusCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * WebSocket 用户状态回调实现
 * 当用户上线/离线时，广播状态给管理员
 */
@Component
@RequiredArgsConstructor
public class WebSocketUserStatusCallbackImpl implements WebSocketUserStatusCallback {

    private final NotificationService notificationService;

    @Override
    public void onUserOnline(Long userId, String nickname) {
        notificationService.broadcastUserOnline(userId, nickname);
    }

    @Override
    public void onUserOffline(Long userId) {
        notificationService.broadcastUserOffline(userId);
    }
}
