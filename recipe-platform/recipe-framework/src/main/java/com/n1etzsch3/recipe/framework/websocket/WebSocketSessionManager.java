package com.n1etzsch3.recipe.framework.websocket;

import com.n1etzsch3.recipe.common.websocket.MessageType;
import com.n1etzsch3.recipe.common.websocket.WebSocketMessage;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器
 * 管理用户ID与WebSocket会话的映射关系
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 存储用户ID与WebSocket会话的映射
     * Key: userId, Value: Set of WebSocket Sessions
     */
    private static final ConcurrentHashMap<Long, Set<Session>> USER_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 注册用户会话
     * 
     * @param userId  用户ID
     * @param session WebSocket会话
     */
    public void register(Long userId, Session session) {
        USER_SESSIONS.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket 用户上线: userId={}, sessionId={}, 当前在线用户数: {}", userId, session.getId(), USER_SESSIONS.size());
    }

    /**
     * 移除用户会话
     * 
     * @param userId  用户ID
     * @param session WebSocket会话
     */
    public void remove(Long userId, Session session) {
        Set<Session> sessions = USER_SESSIONS.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                USER_SESSIONS.remove(userId);
            }
        }
        log.info("WebSocket 用户会话移除: userId={}, sessionId={}, 当前在线用户数: {}", userId, session.getId(),
                USER_SESSIONS.size());
    }

    /**
     * 判断用户是否在线
     * 
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        Set<Session> sessions = USER_SESSIONS.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 发送消息给指定用户的所有会话
     * 
     * @param userId  目标用户ID
     * @param message 消息内容（JSON字符串）
     * @return 至少发送成功一个会话即返回true
     */
    public boolean sendMessage(Long userId, String message) {
        Set<Session> sessions = USER_SESSIONS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }

        boolean sent = false;
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    // 同步发送，避免并发写入冲突
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.getBasicRemote().sendText(message);
                            sent = true;
                        }
                    }
                } catch (IOException e) {
                    log.warn("WebSocket 消息发送失败: userId={}, sessionId={}", userId, session.getId());
                } catch (IllegalStateException e) {
                    // 会话正在写入中或已关闭，忽略此错误
                    log.debug("WebSocket 会话状态异常，跳过发送: userId={}, sessionId={}", userId, session.getId());
                }
            }
        }
        if (sent) {
            log.debug("WebSocket 消息推送成功: userId={}", userId);
        }
        return sent;
    }

    /**
     * 获取在线用户数
     * 
     * @return 在线用户数量
     */
    public int getOnlineCount() {
        return USER_SESSIONS.size();
    }

    /**
     * 获取所有在线用户的ID集合
     * 
     * @return 在线用户ID集合
     */
    public Set<Long> getOnlineUserIds() {
        return USER_SESSIONS.keySet();
    }

    /**
     * 关闭用户的所有 WebSocket 会话
     * 用于踢用户下线或强制单设备登录
     * 
     * @param userId 用户ID
     * @param reason 下线原因消息
     */
    public void closeAllSessions(Long userId, String reason) {
        Set<Session> sessions = USER_SESSIONS.remove(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        // 构建强制下线消息
        WebSocketMessage forceLogoutMsg = WebSocketMessage.builder()
                .type(MessageType.FORCED_LOGOUT)
                .title("强制下线")
                .content(reason)
                .timestamp(LocalDateTime.now())
                .build();
        String msgJson = cn.hutool.json.JSONUtil.toJsonStr(forceLogoutMsg);

        for (Session session : sessions) {
            try {
                if (session.isOpen()) {
                    // 先发送强制下线消息
                    session.getBasicRemote().sendText(msgJson);
                    // 再关闭会话
                    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
                }
            } catch (IOException e) {
                log.error("关闭用户会话失败: userId={}, sessionId={}", userId, session.getId(), e);
            }
        }
        log.info("已关闭用户所有会话: userId={}, 会话数={}", userId, sessions.size());
    }
}
