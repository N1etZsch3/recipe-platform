package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.MessageSendDTO;
import com.n1etzsch3.recipe.business.domain.vo.MessageVO;
import com.n1etzsch3.recipe.business.domain.vo.UserVO;
import com.n1etzsch3.recipe.business.entity.ChatMessage;
import com.n1etzsch3.recipe.business.entity.UserFollow;
import com.n1etzsch3.recipe.business.mapper.ChatMessageMapper;
import com.n1etzsch3.recipe.business.mapper.UserFollowMapper;
import com.n1etzsch3.recipe.business.service.NotificationService;
import com.n1etzsch3.recipe.business.service.SocialService;
import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final UserFollowMapper followMapper;
    private final SysUserMapper sysUserMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final NotificationService notificationService;

    @Override
    public Result<?> toggleFollow(Long targetUserId) {
        Long userId = UserContext.getUserId();
        if (userId.equals(targetUserId)) {
            return Result.fail("不能关注自己");
        }

        UserFollow follow = followMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFollowedId, targetUserId));

        if (follow != null) {
            followMapper.deleteById(follow.getId());
            return Result.ok("已取消关注");
        } else {
            follow = new UserFollow();
            follow.setFollowerId(userId);
            follow.setFollowedId(targetUserId);
            follow.setCreateTime(LocalDateTime.now());
            followMapper.insert(follow);

            // 发送新关注者通知
            SysUser follower = sysUserMapper.selectById(userId);
            if (follower != null) {
                notificationService.sendNewFollower(targetUserId, userId,
                        follower.getNickname(), follower.getAvatar());
            }

            return Result.ok("关注成功");
        }
    }

    @Override
    public Result<IPage<UserVO>> pageMyFollows(Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        Page<UserFollow> p = new Page<>(page, size);
        followMapper.selectPage(p, new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .orderByDesc(UserFollow::getCreateTime));

        // 查我关注的人的信息 (FollowedId)
        List<UserVO> userList = p.getRecords().stream().map(f -> {
            SysUser user = sysUserMapper.selectById(f.getFollowedId());
            if (user == null)
                return null;
            UserVO vo = new UserVO();
            BeanUtil.copyProperties(user, vo);
            return vo;
        }).filter(item -> item != null).collect(Collectors.toList());

        Page<UserVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(userList);
        return Result.ok(result);
    }

    @Override
    public Result<IPage<UserVO>> pageMyFans(Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        Page<UserFollow> p = new Page<>(page, size);
        followMapper.selectPage(p, new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowedId, userId)
                .orderByDesc(UserFollow::getCreateTime));

        // 查关注我的人的信息 (FollowerId)
        List<UserVO> userList = p.getRecords().stream().map(f -> {
            SysUser user = sysUserMapper.selectById(f.getFollowerId());
            if (user == null)
                return null;
            UserVO vo = new UserVO();
            BeanUtil.copyProperties(user, vo);
            // TODO: 是否回关 logic
            return vo;
        }).filter(item -> item != null).collect(Collectors.toList());

        Page<UserVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(userList);
        return Result.ok(result);
    }

    @Override
    public Result<?> sendMessage(MessageSendDTO messageDTO) {
        Long userId = UserContext.getUserId();
        Long receiverId = messageDTO.getReceiverId();

        // 1. 检查对方是否关注了我（互关状态）
        Long count = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, receiverId)
                .eq(UserFollow::getFollowedId, userId));
        boolean isFollowedBack = count != null && count > 0;

        // 2. 如果未互关，检查是否已发送过消息
        if (!isFollowedBack) {
            Long msgCount = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSenderId, userId)
                    .eq(ChatMessage::getReceiverId, receiverId));
            if (msgCount != null && msgCount >= 1) {
                return Result.fail("对方未回关，只能发送一条消息");
            }
        }

        ChatMessage msg = new ChatMessage();
        msg.setSenderId(userId);
        msg.setReceiverId(receiverId);
        msg.setContent(messageDTO.getContent());
        msg.setCreateTime(LocalDateTime.now());
        msg.setIsRead(0);

        chatMessageMapper.insert(msg);

        // 发送新私信通知
        SysUser sender = sysUserMapper.selectById(userId);
        if (sender != null) {
            notificationService.sendNewMessage(receiverId, userId,
                    sender.getNickname(), sender.getAvatar(), messageDTO.getContent());
        }

        return Result.ok("发送成功");
    }

    @Override
    public Result<IPage<MessageVO>> pageMessages(Long targetUserId, Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        Page<ChatMessage> p = new Page<>(page, size);

        // (sender = me and receiver = target) or (sender = target and receiver = me)
        chatMessageMapper.selectPage(p, new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, targetUserId))
                .or(w -> w.eq(ChatMessage::getSenderId, targetUserId).eq(ChatMessage::getReceiverId, userId))
                .orderByDesc(ChatMessage::getCreateTime)); // Latest first

        List<MessageVO> vos = p.getRecords().stream().map(msg -> {
            MessageVO vo = new MessageVO();
            BeanUtil.copyProperties(msg, vo);

            // Fill user info if needed, but VO usually just has sender/receiver generic
            // info
            SysUser sender = sysUserMapper.selectById(msg.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname());
                vo.setSenderAvatar(sender.getAvatar());
            }
            vo.setIsMe(msg.getSenderId().equals(userId));
            return vo;
        }).collect(Collectors.toList());

        Page<MessageVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(vos);
        return Result.ok(result);
    }

    @Override
    public Result<List<com.n1etzsch3.recipe.business.domain.vo.ConversationVO>> listConversations() {
        Long userId = UserContext.getUserId();

        // 1. 获取所有与当前用户相关的消息，按用户分组
        List<ChatMessage> allMessages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSenderId, userId)
                        .or()
                        .eq(ChatMessage::getReceiverId, userId)
                        .orderByDesc(ChatMessage::getCreateTime));

        // 2. 按对方用户分组，获取最新消息
        java.util.Map<Long, ChatMessage> latestMessages = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Integer> unreadCounts = new java.util.HashMap<>();

        for (ChatMessage msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();

            if (!latestMessages.containsKey(otherUserId)) {
                latestMessages.put(otherUserId, msg);
            }

            if (msg.getReceiverId().equals(userId) && msg.getIsRead() == 0) {
                unreadCounts.merge(otherUserId, 1, Integer::sum);
            }
        }

        // 3. 构建结果集 Map (UserId -> VO)，方便去重合并
        java.util.Map<Long, com.n1etzsch3.recipe.business.domain.vo.ConversationVO> resultMap = new java.util.LinkedHashMap<>();

        // 先添加已有会话
        for (java.util.Map.Entry<Long, ChatMessage> entry : latestMessages.entrySet()) {
            Long otherUserId = entry.getKey();
            ChatMessage lastMsg = entry.getValue();

            SysUser otherUser = sysUserMapper.selectById(otherUserId);
            if (otherUser == null)
                continue;

            com.n1etzsch3.recipe.business.domain.vo.ConversationVO vo = new com.n1etzsch3.recipe.business.domain.vo.ConversationVO();
            vo.setUserId(otherUserId);
            vo.setNickname(otherUser.getNickname());
            vo.setAvatar(otherUser.getAvatar());
            vo.setLastMessage(lastMsg.getContent());
            vo.setLastTime(lastMsg.getCreateTime());
            vo.setUnreadCount(unreadCounts.getOrDefault(otherUserId, 0));
            resultMap.put(otherUserId, vo);
        }

        // 4. 获取我关注的人
        List<UserFollow> myFollows = followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .orderByDesc(UserFollow::getCreateTime));

        // 5. 将我关注的人合并到列表 (如果还未在会话中)
        for (UserFollow follow : myFollows) {
            Long followedUserId = follow.getFollowedId();
            if (!resultMap.containsKey(followedUserId)) {
                SysUser followedUser = sysUserMapper.selectById(followedUserId);
                if (followedUser == null)
                    continue;

                com.n1etzsch3.recipe.business.domain.vo.ConversationVO vo = new com.n1etzsch3.recipe.business.domain.vo.ConversationVO();
                vo.setUserId(followedUserId);
                vo.setNickname(followedUser.getNickname());
                vo.setAvatar(followedUser.getAvatar());
                vo.setLastMessage("开始聊天吧");
                vo.setLastTime(follow.getCreateTime()); // 使用关注时间作为排序时间
                vo.setUnreadCount(0);
                resultMap.put(followedUserId, vo);
            }
        }

        // 6. 转换为 List 并按时间倒序排序
        List<com.n1etzsch3.recipe.business.domain.vo.ConversationVO> resultList = new java.util.ArrayList<>(
                resultMap.values());
        resultList.sort((a, b) -> b.getLastTime().compareTo(a.getLastTime()));

        return Result.ok(resultList);
    }

    @Override
    public Result<UserVO> getUserProfile(Long targetUserId) {
        SysUser user = sysUserMapper.selectById(targetUserId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);

        Long currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            // Check follow status
            Long count = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, currentUserId)
                    .eq(UserFollow::getFollowedId, targetUserId));
            vo.setIsFollow(count != null && count > 0);
        } else {
            vo.setIsFollow(false);
        }

        return Result.ok(vo);
    }

    @Override
    public Result<?> markRead(Long senderId) {
        Long currentUserId = UserContext.getUserId();

        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getSenderId, senderId)
                .eq(ChatMessage::getReceiverId, currentUserId)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1));

        return Result.ok();
    }
}
