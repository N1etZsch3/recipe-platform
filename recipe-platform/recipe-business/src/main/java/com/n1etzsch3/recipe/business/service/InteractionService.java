package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.CommentDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.vo.CommentVO;
import com.n1etzsch3.recipe.business.domain.vo.MyCommentVO;
import com.n1etzsch3.recipe.business.domain.vo.CommentReplyVO;
import com.n1etzsch3.recipe.business.domain.vo.CommentLikeVO;
import com.n1etzsch3.recipe.business.domain.vo.SystemNotificationVO;
import com.n1etzsch3.recipe.common.core.domain.Result;

import java.util.List;

public interface InteractionService {

    /**
     * 收藏/取消收藏
     */
    Result<?> toggleFavorite(Long recipeId);

    /**
     * 发表评论
     */
    Result<?> addComment(CommentDTO commentDTO);

    /**
     * 分页查询评论
     */
    Result<IPage<CommentVO>> pageComments(Long recipeId, Integer page, Integer size);

    /**
     * 我的收藏列表
     */
    Result<IPage<RecipeDetailDTO>> pageMyFavorites(Integer page, Integer size);

    /**
     * 点赞/取消点赞评论
     */
    Result<?> toggleCommentLike(Long commentId);

    /**
     * 分页获取评论的回复
     */
    Result<IPage<CommentVO>> pageReplies(Long parentId, Integer page, Integer size);

    /**
     * 删除评论（评论者或管理员可删除）
     */
    Result<?> deleteComment(Long commentId);

    // ============= 信息中心相关 API =============

    /**
     * 我的评论列表
     */
    Result<IPage<MyCommentVO>> pageMyComments(Integer page, Integer size);

    /**
     * 批量删除我的评论
     */
    Result<?> deleteMyComments(List<Long> commentIds);

    /**
     * 回复我的评论列表
     */
    Result<IPage<CommentReplyVO>> pageRepliesForMe(Integer page, Integer size);

    /**
     * 收到的点赞列表
     */
    Result<IPage<CommentLikeVO>> pageLikesForMe(Integer page, Integer size);

    /**
     * 点赞详情（获取某条评论的所有点赞者）
     */
    Result<IPage<CommentLikeVO>> pageLikeDetail(Long commentId, Integer page, Integer size);

    /**
     * 系统通知列表
     */
    Result<IPage<SystemNotificationVO>> pageSystemNotifications(Integer page, Integer size);
}
