package com.n1etzsch3.recipe.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.CommentDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.vo.CommentVO;
import com.n1etzsch3.recipe.business.domain.vo.MyCommentVO;
import com.n1etzsch3.recipe.business.domain.vo.CommentReplyVO;
import com.n1etzsch3.recipe.business.domain.vo.CommentLikeVO;
import com.n1etzsch3.recipe.business.domain.vo.SystemNotificationVO;
import com.n1etzsch3.recipe.business.service.InteractionService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    /**
     * 收藏/取消收藏
     */
    @PostMapping("/favorite/{recipeId}")
    public Result<?> toggleFavorite(@PathVariable Long recipeId) {
        return interactionService.toggleFavorite(recipeId);
    }

    /**
     * 发表评论
     */
    @PostMapping("/comments")
    public Result<?> addComment(@RequestBody CommentDTO commentDTO) {
        return interactionService.addComment(commentDTO);
    }

    /**
     * 评论列表
     */
    @GetMapping("/comments/{recipeId}")
    public Result<IPage<CommentVO>> listComments(@PathVariable Long recipeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return interactionService.pageComments(recipeId, page, size);
    }

    /**
     * 我的收藏
     */
    @GetMapping("/favorites")
    public Result<IPage<RecipeDetailDTO>> myFavorites(@RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return interactionService.pageMyFavorites(page, size);
    }

    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/comments/{commentId}/like")
    public Result<?> likeComment(@PathVariable Long commentId) {
        return interactionService.toggleCommentLike(commentId);
    }

    /**
     * 获取评论的回复列表
     */
    @GetMapping("/comments/{parentId}/replies")
    public Result<IPage<CommentVO>> listReplies(@PathVariable Long parentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer size) {
        return interactionService.pageReplies(parentId, page, size);
    }

    /**
     * 删除评论（评论者或管理员可删除）
     */
    @DeleteMapping("/comments/{commentId}")
    public Result<?> deleteComment(@PathVariable Long commentId) {
        return interactionService.deleteComment(commentId);
    }

    // ============= 信息中心相关 API =============

    /**
     * 我的评论列表
     */
    @GetMapping("/my-comments")
    public Result<IPage<MyCommentVO>> myComments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return interactionService.pageMyComments(page, size);
    }

    /**
     * 批量删除我的评论
     */
    @DeleteMapping("/my-comments")
    public Result<?> deleteMyComments(@RequestBody List<Long> commentIds) {
        return interactionService.deleteMyComments(commentIds);
    }

    /**
     * 回复我的评论列表
     */
    @GetMapping("/replies-for-me")
    public Result<IPage<CommentReplyVO>> repliesForMe(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return interactionService.pageRepliesForMe(page, size);
    }

    /**
     * 收到的点赞列表
     */
    @GetMapping("/likes-for-me")
    public Result<IPage<CommentLikeVO>> likesForMe(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return interactionService.pageLikesForMe(page, size);
    }

    /**
     * 点赞详情
     */
    @GetMapping("/comments/{commentId}/likers")
    public Result<IPage<CommentLikeVO>> likeDetail(@PathVariable Long commentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return interactionService.pageLikeDetail(commentId, page, size);
    }

    /**
     * 系统通知列表
     */
    @GetMapping("/system-notifications")
    public Result<IPage<SystemNotificationVO>> systemNotifications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return interactionService.pageSystemNotifications(page, size);
    }
}
