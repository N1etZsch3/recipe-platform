package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.CommentDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.vo.CommentVO;
import com.n1etzsch3.recipe.business.entity.CommentLike;
import com.n1etzsch3.recipe.business.entity.RecipeComment;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.UserFavorite;
import com.n1etzsch3.recipe.business.mapper.CommentLikeMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeCommentMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.UserFavoriteMapper;
import com.n1etzsch3.recipe.business.service.InteractionService;
import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final UserFavoriteMapper favoriteMapper;
    private final RecipeCommentMapper commentMapper;
    private final RecipeInfoMapper recipeInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final CommentLikeMapper commentLikeMapper;

    @Override
    public Result<?> toggleFavorite(Long recipeId) {
        Long userId = UserContext.getUserId();
        if (userId == null)
            return Result.fail("未登录");

        UserFavorite favorite = favoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getRecipeId, recipeId));

        if (favorite != null) {
            favoriteMapper.deleteById(favorite.getId());
            return Result.ok("已取消收藏");
        } else {
            favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setRecipeId(recipeId);
            favorite.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(favorite);
            return Result.ok("收藏成功");
        }
    }

    @Override
    public Result<?> addComment(CommentDTO commentDTO) {
        Long userId = UserContext.getUserId();

        RecipeComment comment = new RecipeComment();
        comment.setUserId(userId);
        comment.setRecipeId(commentDTO.getRecipeId());
        comment.setParentId(commentDTO.getParentId());
        comment.setContent(commentDTO.getContent());
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());

        commentMapper.insert(comment);
        return Result.ok("评论成功");
    }

    @Override
    public Result<IPage<CommentVO>> pageComments(Long recipeId, Integer page, Integer size) {
        Long currentUserId = UserContext.getUserId();
        Page<RecipeComment> p = new Page<>(page, size);

        Page<RecipeComment> resultPage = commentMapper.selectPage(p, new LambdaQueryWrapper<RecipeComment>()
                .eq(RecipeComment::getRecipeId, recipeId)
                .isNull(RecipeComment::getParentId)
                .orderByDesc(RecipeComment::getCreateTime));

        IPage<CommentVO> voPage = resultPage.convert(comment -> convertToVO(comment, currentUserId, true));

        return Result.ok(voPage);
    }

    private CommentVO convertToVO(RecipeComment comment, Long currentUserId, boolean loadReplies) {
        CommentVO vo = new CommentVO();
        BeanUtil.copyProperties(comment, vo);

        SysUser user = sysUserMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        if (comment.getParentId() != null) {
            RecipeComment parentComment = commentMapper.selectById(comment.getParentId());
            if (parentComment != null) {
                SysUser parentUser = sysUserMapper.selectById(parentComment.getUserId());
                if (parentUser != null) {
                    vo.setReplyToNickname(parentUser.getNickname());
                }
            }
        }

        if (currentUserId != null) {
            Long likeCount = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, comment.getId())
                    .eq(CommentLike::getUserId, currentUserId));
            vo.setIsLiked(likeCount != null && likeCount > 0);
        } else {
            vo.setIsLiked(false);
        }

        if (loadReplies && comment.getParentId() == null) {
            Long replyCount = commentMapper.selectCount(new LambdaQueryWrapper<RecipeComment>()
                    .eq(RecipeComment::getParentId, comment.getId()));
            vo.setReplyCount(replyCount != null ? replyCount.intValue() : 0);

            if (vo.getReplyCount() > 0) {
                List<RecipeComment> replyList = commentMapper.selectList(new LambdaQueryWrapper<RecipeComment>()
                        .eq(RecipeComment::getParentId, comment.getId())
                        .orderByAsc(RecipeComment::getCreateTime)
                        .last("LIMIT 1"));
                vo.setReplies(replyList.stream()
                        .map(r -> convertToVO(r, currentUserId, false))
                        .collect(Collectors.toList()));
            } else {
                vo.setReplies(new ArrayList<>());
            }
        }

        return vo;
    }

    @Override
    public Result<?> toggleCommentLike(Long commentId) {
        Long userId = UserContext.getUserId();
        if (userId == null)
            return Result.fail("未登录");

        CommentLike like = commentLikeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId));

        RecipeComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }

        if (like != null) {
            commentLikeMapper.deleteById(like.getId());
            comment.setLikeCount(Math.max(0, (comment.getLikeCount() != null ? comment.getLikeCount() : 0) - 1));
            commentMapper.updateById(comment);
            return Result.ok("已取消点赞");
        } else {
            like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            commentLikeMapper.insert(like);
            comment.setLikeCount((comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1);
            commentMapper.updateById(comment);
            return Result.ok("点赞成功");
        }
    }

    @Override
    public Result<IPage<CommentVO>> pageReplies(Long parentId, Integer page, Integer size) {
        Long currentUserId = UserContext.getUserId();
        Page<RecipeComment> p = new Page<>(page, size);

        Page<RecipeComment> resultPage = commentMapper.selectPage(p, new LambdaQueryWrapper<RecipeComment>()
                .eq(RecipeComment::getParentId, parentId)
                .orderByAsc(RecipeComment::getCreateTime));

        IPage<CommentVO> voPage = resultPage.convert(comment -> convertToVO(comment, currentUserId, false));

        return Result.ok(voPage);
    }

    @Override
    public Result<IPage<RecipeDetailDTO>> pageMyFavorites(Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        Page<UserFavorite> p = new Page<>(page, size);
        favoriteMapper.selectPage(p, new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreateTime));

        List<RecipeDetailDTO> detailList = p.getRecords().stream().map(fav -> {
            RecipeInfo recipe = recipeInfoMapper.selectById(fav.getRecipeId());
            if (recipe == null)
                return null;

            RecipeDetailDTO dto = new RecipeDetailDTO();
            BeanUtil.copyProperties(recipe, dto);
            SysUser author = sysUserMapper.selectById(recipe.getUserId());
            if (author != null) {
                dto.setAuthorName(author.getNickname());
                dto.setAuthorAvatar(author.getAvatar());
            }
            dto.setIsFavorite(true);
            return dto;
        }).filter(item -> item != null).collect(Collectors.toList());

        Page<RecipeDetailDTO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(detailList);

        return Result.ok(result);
    }

    @Override
    public Result<?> deleteComment(Long commentId) {
        Long userId = UserContext.getUserId();
        if (userId == null)
            return Result.fail("未登录");

        RecipeComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }

        // 检查当前用户信息
        SysUser currentUser = sysUserMapper.selectById(userId);
        boolean isAdmin = currentUser != null && "admin".equals(currentUser.getRole());
        boolean isOwner = comment.getUserId().equals(userId);

        if (!isOwner && !isAdmin) {
            return Result.fail("无权删除此评论");
        }

        // 如果是管理员删除别人的评论，发送私信通知
        if (isAdmin && !isOwner) {
            // TODO: 发送私信通知评论所有者
            // 需要注入 SysMessageMapper 并创建私信记录
        }

        // 删除该评论的所有回复
        commentMapper.delete(new LambdaQueryWrapper<RecipeComment>()
                .eq(RecipeComment::getParentId, commentId));

        // 删除该评论的所有点赞
        commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId));

        // 删除评论本身
        commentMapper.deleteById(commentId);

        return Result.ok("删除成功");
    }
}
