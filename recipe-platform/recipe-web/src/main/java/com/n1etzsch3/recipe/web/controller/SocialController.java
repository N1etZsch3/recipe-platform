package com.n1etzsch3.recipe.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.vo.UserVO;
import com.n1etzsch3.recipe.business.domain.dto.MessageSendDTO;
import com.n1etzsch3.recipe.business.service.SocialService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    /**
     * 关注/取关
     */
    @PostMapping("/follow/{userId}")
    public Result<?> toggleFollow(@PathVariable Long userId) {
        return socialService.toggleFollow(userId);
    }

    /**
     * 我的关注列表
     */
    @GetMapping("/follows")
    public Result<IPage<UserVO>> myFollows(@RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return socialService.pageMyFollows(page, size);
    }

    /**
     * 我的粉丝列表
     */
    @GetMapping("/fans")
    public Result<IPage<UserVO>> myFans(@RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return socialService.pageMyFans(page, size);
    }

    /**
     * 发送私信
     */
    @PostMapping("/messages")
    public Result<?> sendMessage(@RequestBody @jakarta.validation.Valid MessageSendDTO sendDTO) {
        return socialService.sendMessage(sendDTO);
    }

    /**
     * 获取聊天记录
     */
    @GetMapping("/messages/{userId}")
    public Result<?> pageMessages(@PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return socialService.pageMessages(userId, page, size);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<?> listConversations() {
        return socialService.listConversations();
    }

    /**
     * 获取用户公开信息
     */
    @GetMapping("/user/{userId}")
    public Result<UserVO> getUserProfile(@PathVariable Long userId) {
        return socialService.getUserProfile(userId);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/messages/read/{senderId}")
    public Result<?> markRead(@PathVariable Long senderId) {
        return socialService.markRead(senderId);
    }
}
