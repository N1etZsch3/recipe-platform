package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.MessageSendDTO;
import com.n1etzsch3.recipe.business.domain.vo.MessageVO;
import com.n1etzsch3.recipe.business.domain.vo.UserVO;
import com.n1etzsch3.recipe.common.core.domain.Result;

public interface SocialService {

    /**
     * 关注/取关
     */
    Result<?> toggleFollow(Long targetUserId);

    /**
     * 我的关注列表
     */
    Result<IPage<UserVO>> pageMyFollows(Integer page, Integer size);

    /**
     * 我的粉丝列表
     */
    Result<IPage<UserVO>> pageMyFans(Integer page, Integer size);

    /**
     * 发送私信
     */
    Result<?> sendMessage(MessageSendDTO sendDTO);

    /**
     * 获取聊天记录
     */
    Result<IPage<MessageVO>> pageMessages(Long otherUserId, Integer page, Integer size);

    /**
     * 获取会话列表
     */
    Result<java.util.List<com.n1etzsch3.recipe.business.domain.vo.ConversationVO>> listConversations();

    /**
     * 获取用户公开信息
     */
    Result<UserVO> getUserProfile(Long userId);

    /**
     * 标记消息为已读
     */
    Result<?> markRead(Long senderId);
}
