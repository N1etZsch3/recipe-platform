package com.n1etzsch3.recipe.business.service;

import com.n1etzsch3.recipe.common.websocket.WebSocketMessage;

/**
 * 通知推送服务接口
 * 负责将实时消息推送给在线用户
 */
public interface NotificationService {

        /**
         * 发送通知给指定用户
         * 
         * @param userId  目标用户ID
         * @param message 消息内容
         * @return 是否发送成功（用户不在线时返回 false）
         */
        boolean sendToUser(Long userId, WebSocketMessage message);

        /**
         * 发送菜谱审核通过通知
         * 
         * @param userId      菜谱作者ID
         * @param recipeId    菜谱ID
         * @param recipeTitle 菜谱标题
         */
        void sendRecipeApproved(Long userId, Long recipeId, String recipeTitle);

        /**
         * 发送菜谱审核驳回通知
         * 
         * @param userId      菜谱作者ID
         * @param recipeId    菜谱ID
         * @param recipeTitle 菜谱标题
         * @param reason      驳回原因
         */
        void sendRecipeRejected(Long userId, Long recipeId, String recipeTitle, String reason);

        /**
         * 发送新私信通知
         * 
         * @param receiverId   接收者ID
         * @param senderId     发送者ID
         * @param senderName   发送者昵称
         * @param senderAvatar 发送者头像
         * @param content      消息内容预览
         */
        void sendNewMessage(Long receiverId, Long senderId, String senderName, String senderAvatar, String content);

        /**
         * 发送新关注者通知
         * 
         * @param followedId     被关注者ID
         * @param followerId     关注者ID
         * @param followerName   关注者昵称
         * @param followerAvatar 关注者头像
         */
        void sendNewFollower(Long followedId, Long followerId, String followerName, String followerAvatar);

        /**
         * 发送新评论通知
         * 
         * @param authorId      菜谱作者ID
         * @param commenterId   评论者ID
         * @param commenterName 评论者昵称
         * @param recipeId      菜谱ID
         * @param recipeTitle   菜谱标题
         */
        void sendNewComment(Long authorId, Long commenterId, String commenterName, Long recipeId, String recipeTitle);

        /**
         * 发送新菜谱待审核通知给所有管理员
         *
         * @param recipeId    菜谱ID
         * @param recipeTitle 菜谱标题
         * @param authorId    作者ID
         * @param authorName  作者昵称
         * @param coverImage  菜谱封面图
         */
        void sendNewRecipePending(Long recipeId, String recipeTitle, Long authorId, String authorName,
                        String coverImage);

        /**
         * 发送评论被回复通知
         *
         * @param originalCommenterId 原评论作者ID
         * @param replierId           回复者ID
         * @param replierName         回复者昵称
         * @param recipeId            菜谱ID
         * @param recipeTitle         菜谱标题
         * @param originalContent     原评论内容
         * @param replyContent        回复内容
         */
        void sendCommentReply(Long originalCommenterId, Long replierId, String replierName,
                        Long recipeId, String recipeTitle, String originalContent, String replyContent);

        /**
         * 发送评论被点赞通知
         *
         * @param commentOwnerId 评论作者ID
         * @param likerId        点赞者ID
         * @param likerName      点赞者昵称
         * @param recipeId       菜谱ID
         * @param recipeTitle    菜谱标题
         * @param commentContent 评论内容
         */
        void sendCommentLiked(Long commentOwnerId, Long likerId, String likerName,
                        Long recipeId, String recipeTitle, String commentContent);

        // ========== 管理员广播 ==========

        /**
         * 广播用户上线状态给所有在线管理员
         *
         * @param userId   上线的用户ID
         * @param nickname 用户昵称
         */
        void broadcastUserOnline(Long userId, String nickname);

        /**
         * 广播用户离线状态给所有在线管理员
         *
         * @param userId 离线的用户ID
         */
        void broadcastUserOffline(Long userId);

        /**
         * 发送菜谱撤销/删除通知给所有管理员
         *
         * @param recipeId    菜谱ID
         * @param recipeTitle 菜谱标题
         * @param authorId    作者ID
         * @param authorName  作者昵称
         */
        void sendRecipeWithdrawn(Long recipeId, String recipeTitle, Long authorId, String authorName);

        /**
         * 广播消息给所有在线管理员
         *
         * @param message 消息内容
         */
        void broadcastToAdmins(WebSocketMessage message);
}
