package com.n1etzsch3.recipe.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.CommentDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.vo.CommentVO;
import com.n1etzsch3.recipe.business.service.InteractionService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
