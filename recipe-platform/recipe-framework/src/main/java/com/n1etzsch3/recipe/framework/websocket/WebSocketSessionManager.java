package com.n1etzsch3.recipe.framework.websocket;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
     * Key: userId, Value: WebSocket Session
     */
    private static final ConcurrentHashMap<Long, Session> USER_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 注册用户会话
     * 
     * @param userId  用户ID
     * @param session WebSocket会话
     */
    public void register(Long userId, Session session) {
        // 关闭旧连接（如果存在）
        Session oldSession = USER_SESSIONS.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
                log.info("关闭用户旧的WebSocket连接: userId={}", userId);
            } catch (IOException e) {
                log.warn("关闭旧会话失败: userId={}", userId, e);
            }
        }
        USER_SESSIONS.put(userId, session);
        log.info("WebSocket 用户上线: userId={}, 当前在线人数: {}", userId, USER_SESSIONS.size());
    }

    /**
     * 移除用户会话
     * 
     * @param userId 用户ID
     */
    public void remove(Long userId) {
        USER_SESSIONS.remove(userId);
        log.info("WebSocket 用户下线: userId={}, 当前在线人数: {}", userId, USER_SESSIONS.size());
    }

    /**
     * 获取用户会话
     * 
     * @param userId 用户ID
     * @return WebSocket会话，不存在则返回null
     */
    public Session getSession(Long userId) {
        return USER_SESSIONS.get(userId);
    }

    /**
     * 判断用户是否在线
     * 
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        Session session = USER_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 发送消息给指定用户
     * 
     * @param userId  目标用户ID
     * @param message 消息内容（JSON字符串）
     * @return 是否发送成功
     */
    public boolean sendMessage(Long userId, String message) {
        Session session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                log.debug("WebSocket 消息发送成功: userId={}", userId);
                return true;
            } catch (IOException e) {
                log.error("WebSocket 消息发送失败: userId={}", userId, e);
            }
        }
        return false;
    }

    /**
     * 获取在线用户数
     * 
     * @return 在线用户数量
     */
    public int getOnlineCount() {
        return USER_SESSIONS.size();
    }
}
