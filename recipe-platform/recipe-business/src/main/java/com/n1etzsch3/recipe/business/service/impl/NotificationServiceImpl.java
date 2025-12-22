package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.business.service.NotificationService;
import com.n1etzsch3.recipe.common.websocket.MessageType;
import com.n1etzsch3.recipe.common.websocket.WebSocketMessage;
import com.n1etzsch3.recipe.framework.websocket.WebSocketSessionManager;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 通知推送服务实现
 * 通过 WebSocket 将消息推送给在线用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final WebSocketSessionManager sessionManager;
    private final SysUserMapper sysUserMapper;

    @Override
    public boolean sendToUser(Long userId, WebSocketMessage message) {
        if (userId == null || message == null) {
            return false;
        }

        // 设置时间戳
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        String json = JSONUtil.toJsonStr(message);

        boolean success = sessionManager.sendMessage(userId, json);
        if (success) {
            log.info("通知发送成功: userId={}, type={}", userId, message.getType());
        } else {
            log.info("用户不在线，通知未发送: userId={}, type={}", userId, message.getType());
            // TODO: 可选择存储离线消息到数据库，用户上线后推送
        }
        return success;
    }

    @Override
    public void sendRecipeApproved(Long userId, Long recipeId, String recipeTitle) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.RECIPE_APPROVED)
                .title("菜谱审核通过 🎉")
                .content("您的菜谱「" + recipeTitle + "」已审核通过，快去看看吧！")
                .relatedId(recipeId)
                .build();
        sendToUser(userId, message);
    }

    @Override
    public void sendRecipeRejected(Long userId, Long recipeId, String recipeTitle, String reason) {
        String content = "您的菜谱「" + recipeTitle + "」审核未通过";
        if (reason != null && !reason.isEmpty()) {
            content += "，原因：" + reason;
        }

        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.RECIPE_REJECTED)
                .title("菜谱审核未通过")
                .content(content)
                .relatedId(recipeId)
                .build();
        sendToUser(userId, message);
    }

    @Override
    public void sendNewMessage(Long receiverId, Long senderId, String senderName, String senderAvatar, String content) {
        // 截取消息预览（最多50字符）
        String preview = content;
        if (content != null && content.length() > 50) {
            preview = content.substring(0, 50) + "...";
        }

        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.NEW_MESSAGE)
                .title("新私信")
                .content(preview)
                .senderId(senderId)
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .build();
        sendToUser(receiverId, message);
    }

    @Override
    public void sendNewFollower(Long followedId, Long followerId, String followerName, String followerAvatar) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.NEW_FOLLOWER)
                .title("新粉丝")
                .content(followerName + " 关注了你")
                .senderId(followerId)
                .senderName(followerName)
                .senderAvatar(followerAvatar)
                .build();
        sendToUser(followedId, message);
    }

    @Override
    public void sendNewComment(Long authorId, Long commenterId, String commenterName, Long recipeId,
            String recipeTitle) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.NEW_COMMENT)
                .title("新评论")
                .content(commenterName + " 评论了您的菜谱「" + recipeTitle + "」")
                .relatedId(recipeId)
                .senderId(commenterId)
                .senderName(commenterName)
                .build();
        sendToUser(authorId, message);
    }

    @Override
    public void sendNewRecipePending(Long recipeId, String recipeTitle, Long authorId, String authorName) {
        // 查询所有管理员 (status = 0 表示正常状态)
        List<SysUser> admins = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "admin")
                .eq(SysUser::getStatus, 0)); // 0=正常, 1=封禁

        if (admins.isEmpty()) {
            log.warn("没有找到管理员，无法发送待审核通知");
            return;
        }

        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.NEW_RECIPE_PENDING)
                .title("新菜谱待审核 📝")
                .content("用户「" + authorName + "」提交了新菜谱「" + recipeTitle + "」，请尽快审核")
                .relatedId(recipeId)
                .senderId(authorId)
                .senderName(authorName)
                .build();

        // 向所有管理员发送通知
        for (SysUser admin : admins) {
            sendToUser(admin.getId(), message);
        }

        log.info("已向 {} 位管理员发送新菜谱待审核通知: recipeId={}, title={}",
                admins.size(), recipeId, recipeTitle);
    }

    @Override
    public void sendCommentReply(Long originalCommenterId, Long replierId, String replierName,
            Long recipeId, String recipeTitle, String originalContent, String replyContent) {
        // 不通知自己
        if (originalCommenterId.equals(replierId)) {
            return;
        }

        // 截取预览内容
        String originalPreview = originalContent != null && originalContent.length() > 30
                ? originalContent.substring(0, 30) + "..."
                : originalContent;
        String replyPreview = replyContent != null && replyContent.length() > 50
                ? replyContent.substring(0, 50) + "..."
                : replyContent;

        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.COMMENT_REPLY)
                .title("评论被回复")
                .content(replierName + " 回复了你的评论：" + replyPreview)
                .relatedId(recipeId)
                .senderId(replierId)
                .senderName(replierName)
                .build();
        sendToUser(originalCommenterId, message);
    }

    @Override
    public void sendCommentLiked(Long commentOwnerId, Long likerId, String likerName,
            Long recipeId, String recipeTitle, String commentContent) {
        // 不通知自己
        if (commentOwnerId.equals(likerId)) {
            return;
        }

        // 截取评论预览
        String contentPreview = commentContent != null && commentContent.length() > 30
                ? commentContent.substring(0, 30) + "..."
                : commentContent;

        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.COMMENT_LIKED)
                .title("评论被点赞")
                .content(likerName + " 赞了你的评论：" + contentPreview)
                .relatedId(recipeId)
                .senderId(likerId)
                .senderName(likerName)
                .build();
        sendToUser(commentOwnerId, message);
    }

    // ========== 管理员广播实现 ==========

    @Override
    public void broadcastUserOnline(Long userId, String nickname) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.USER_ONLINE)
                .title("用户上线")
                .content(nickname != null ? nickname + " 已上线" : "用户已上线")
                .relatedId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        broadcastToAllOnlineUsers(message);
        log.debug("广播用户上线: userId={}", userId);
    }

    @Override
    public void broadcastUserOffline(Long userId) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(MessageType.USER_OFFLINE)
                .title("用户离线")
                .content("用户已离线")
                .relatedId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        broadcastToAllOnlineUsers(message);
        log.debug("广播用户离线: userId={}", userId);
    }

    @Override
    public void broadcastToAdmins(WebSocketMessage message) {
        // 查询所有管理员（包括超级管理员和普通管理员）
        List<SysUser> admins = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getRole, "admin", "common_admin")
                .eq(SysUser::getStatus, 0)); // 0=正常

        if (admins.isEmpty()) {
            return;
        }

        // 设置时间戳
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        String json = JSONUtil.toJsonStr(message);
        int sentCount = 0;

        for (SysUser admin : admins) {
            if (sessionManager.sendMessage(admin.getId(), json)) {
                sentCount++;
            }
        }

        if (sentCount > 0) {
            log.debug("管理员广播完成: type={}, 在线管理员={}/{}",
                    message.getType(), sentCount, admins.size());
        }
    }

    /**
     * 广播消息给所有在线用户
     * 用于用户状态变化等需要全局通知的场景
     */
    private void broadcastToAllOnlineUsers(WebSocketMessage message) {
        // 设置时间戳
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        String json = JSONUtil.toJsonStr(message);

        // 获取所有在线用户ID
        Set<Long> onlineUserIds = sessionManager.getOnlineUserIds();
        if (onlineUserIds.isEmpty()) {
            return;
        }

        int sentCount = 0;
        for (Long userId : onlineUserIds) {
            if (sessionManager.sendMessage(userId, json)) {
                sentCount++;
            }
        }

        if (sentCount > 0) {
            log.debug("全局广播完成: type={}, 在线用户={}",
                    message.getType(), sentCount);
        }
    }
}
