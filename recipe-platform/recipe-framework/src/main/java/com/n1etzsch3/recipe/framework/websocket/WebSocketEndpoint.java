package com.n1etzsch3.recipe.framework.websocket;

import cn.hutool.json.JSONUtil;
import com.n1etzsch3.recipe.common.utils.JwtUtils;
import com.n1etzsch3.recipe.common.websocket.MessageType;
import com.n1etzsch3.recipe.common.websocket.WebSocketMessage;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 端点处理器
 * 处理 WebSocket 连接的建立、消息接收、关闭和错误
 */
@Slf4j
@Component
@ServerEndpoint("/ws")
public class WebSocketEndpoint {

    /**
     * 由于 @ServerEndpoint 不是 Spring 管理的 Bean，需要静态注入
     */
    private static WebSocketSessionManager sessionManager;
    private static com.n1etzsch3.recipe.framework.service.UserOnlineService userOnlineService;
    private static com.n1etzsch3.recipe.common.websocket.WebSocketUserStatusCallback userStatusCallback;

    @Autowired
    public void setSessionManager(WebSocketSessionManager manager) {
        WebSocketEndpoint.sessionManager = manager;
    }

    @Autowired
    public void setUserOnlineService(com.n1etzsch3.recipe.framework.service.UserOnlineService service) {
        WebSocketEndpoint.userOnlineService = service;
    }

    @Autowired(required = false)
    public void setUserStatusCallback(com.n1etzsch3.recipe.common.websocket.WebSocketUserStatusCallback callback) {
        WebSocketEndpoint.userStatusCallback = callback;
    }

    /**
     * 当前会话的用户ID
     */
    private Long userId;

    /**
     * 连接建立时调用
     */
    @OnOpen
    public void onOpen(Session session) {
        try {
            // 从查询参数获取 token
            Map<String, List<String>> params = session.getRequestParameterMap();
            List<String> tokenList = params.get("token");

            if (tokenList == null || tokenList.isEmpty()) {
                log.warn("WebSocket 连接失败：缺少 token");
                closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Missing token");
                return;
            }

            String token = tokenList.get(0);

            // 验证 token 并获取用户ID
            if (!JwtUtils.validateToken(token)) {
                log.warn("WebSocket 连接失败：token 无效");
                closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token");
                return;
            }

            this.userId = JwtUtils.getUserIdFromToken(token);
            if (this.userId == null) {
                log.warn("WebSocket 连接失败：无法获取用户ID");
                closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid user");
                return;
            }

            // 注册用户会话
            sessionManager.register(userId, session);
            // 更新 Redis 在线状态
            if (userOnlineService != null) {
                userOnlineService.heartbeat(userId);
            }
            // 广播用户上线状态给管理员（通过回调）
            if (userStatusCallback != null) {
                // 昵称可选，通过回调实现类获取
                userStatusCallback.onUserOnline(userId, null);
            }

            // 发送连接成功消息
            WebSocketMessage welcomeMsg = WebSocketMessage.builder()
                    .type(MessageType.CONNECTED)
                    .title("连接成功")
                    .content("欢迎使用菜谱分享平台")
                    .timestamp(LocalDateTime.now())
                    .build();
            session.getBasicRemote().sendText(JSONUtil.toJsonStr(welcomeMsg));

        } catch (Exception e) {
            log.error("WebSocket 连接异常", e);
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 收到客户端消息时调用
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("收到 WebSocket 消息: userId={}, message={}", userId, message);

        // 处理心跳
        if ("ping".equalsIgnoreCase(message)) {
            try {
                // 更新 Redis 在线状态（刷新 TTL）
                if (userOnlineService != null && userId != null) {
                    userOnlineService.heartbeat(userId);
                }
                session.getBasicRemote().sendText("pong");
            } catch (IOException e) {
                log.error("发送心跳响应失败", e);
            }
        }
        // 可以在这里扩展其他消息类型的处理
    }

    /**
     * 连接关闭时调用
     */
    @OnClose
    public void onClose(Session session) {
        if (userId != null) {
            try {
                sessionManager.remove(userId, session);
                // 检查是否还有其他会话，如果没有则标记离线
                if (!sessionManager.isOnline(userId)) {
                    if (userOnlineService != null) {
                        userOnlineService.offline(userId);
                    }
                    // 广播用户离线状态给管理员（通过回调）
                    if (userStatusCallback != null) {
                        userStatusCallback.onUserOffline(userId);
                    }
                }
            } catch (Exception e) {
                // 应用关闭时 Redis 可能已销毁，忽略此异常
                log.debug("WebSocket 关闭时清理失败（可能是应用正在关闭）: userId={}", userId);
            }
        }
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        // 应用关闭时的 ClosedChannelException 是正常的，降级为 debug 级别
        if (error instanceof java.io.IOException
                || error.getCause() instanceof java.nio.channels.ClosedChannelException) {
            log.debug("WebSocket 连接关闭: userId={}", userId);
        } else {
            log.error("WebSocket 错误: userId={}", userId, error);
        }

        if (userId != null) {
            try {
                sessionManager.remove(userId, session);
                // 检查是否还有其他会话，如果没有则标记离线
                if (!sessionManager.isOnline(userId) && userOnlineService != null) {
                    userOnlineService.offline(userId);
                }
            } catch (Exception e) {
                // 应用关闭时 Redis 可能已销毁，忽略此异常
                log.debug("WebSocket 错误处理时清理失败（可能是应用正在关闭）: userId={}", userId);
            }
        }
    }

    /**
     * 关闭会话的辅助方法
     */
    private void closeSession(Session session, CloseReason.CloseCodes code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (IOException e) {
            log.warn("关闭会话失败", e);
        }
    }
}
