package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.CommentDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.vo.CommentVO;
import com.n1etzsch3.recipe.common.core.domain.Result;

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
}
