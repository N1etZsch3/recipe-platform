package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.CommentDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.vo.CommentVO;
import com.n1etzsch3.recipe.business.domain.vo.MyCommentVO;
import com.n1etzsch3.recipe.business.domain.vo.CommentReplyVO;
import com.n1etzsch3.recipe.business.domain.vo.CommentLikeVO;
import com.n1etzsch3.recipe.business.domain.vo.SystemNotificationVO;
import com.n1etzsch3.recipe.business.entity.CommentLike;
import com.n1etzsch3.recipe.business.entity.RecipeComment;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.UserFavorite;
import com.n1etzsch3.recipe.business.mapper.CommentLikeMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeCommentMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.UserFavoriteMapper;
import com.n1etzsch3.recipe.business.service.InteractionService;
import com.n1etzsch3.recipe.business.service.NotificationService;
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
    private final NotificationService notificationService;

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

        // 发送通知
        try {
            SysUser commenter = sysUserMapper.selectById(userId);
            String commenterName = commenter != null ? commenter.getNickname() : "用户";
            RecipeInfo recipe = recipeInfoMapper.selectById(commentDTO.getRecipeId());
            String recipeTitle = recipe != null ? recipe.getTitle() : "菜谱";

            if (commentDTO.getParentId() != null) {
                // 回复评论 - 通知原评论作者
                RecipeComment parentComment = commentMapper.selectById(commentDTO.getParentId());
                if (parentComment != null) {
                    notificationService.sendCommentReply(
                            parentComment.getUserId(),
                            userId,
                            commenterName,
                            commentDTO.getRecipeId(),
                            recipeTitle,
                            parentComment.getContent(),
                            commentDTO.getContent());
                }
            } else if (recipe != null && !recipe.getUserId().equals(userId)) {
                // 直接评论菜谱 - 通知菜谱作者
                notificationService.sendNewComment(
                        recipe.getUserId(),
                        userId,
                        commenterName,
                        recipe.getId(),
                        recipeTitle);
            }
        } catch (Exception e) {
            // 通知失败不影响评论写入
        }

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

            // 发送点赞通知
            try {
                SysUser liker = sysUserMapper.selectById(userId);
                String likerName = liker != null ? liker.getNickname() : "用户";
                RecipeInfo recipe = recipeInfoMapper.selectById(comment.getRecipeId());
                String recipeTitle = recipe != null ? recipe.getTitle() : "菜谱";
                notificationService.sendCommentLiked(
                        comment.getUserId(),
                        userId,
                        likerName,
                        comment.getRecipeId(),
                        recipeTitle,
                        comment.getContent());
            } catch (Exception e) {
                // 通知失败不影响点赞操作
            }

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

    // ============= 信息中心相关 API 实现 =============

    @Override
    public Result<IPage<MyCommentVO>> pageMyComments(Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        if (userId == null)
            return Result.fail("未登录");

        Page<RecipeComment> p = new Page<>(page, size);
        Page<RecipeComment> resultPage = commentMapper.selectPage(p, new LambdaQueryWrapper<RecipeComment>()
                .eq(RecipeComment::getUserId, userId)
                // 移除 isNull(ParentId) 限制，同时查询顶级评论和回复评论
                .orderByDesc(RecipeComment::getCreateTime));

        IPage<MyCommentVO> voPage = resultPage.convert(comment -> {
            MyCommentVO vo = new MyCommentVO();
            vo.setId(comment.getId());
            vo.setContent(comment.getContent());
            vo.setRecipeId(comment.getRecipeId());
            vo.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
            vo.setCreateTime(comment.getCreateTime());
            vo.setParentId(comment.getParentId());

            // 获取菜谱信息
            RecipeInfo recipe = recipeInfoMapper.selectById(comment.getRecipeId());
            if (recipe != null) {
                vo.setRecipeTitle(recipe.getTitle());
                vo.setRecipeCoverImage(recipe.getCoverImage());
            }

            // 如果是回复评论，获取父评论信息
            if (comment.getParentId() != null) {
                RecipeComment parentComment = commentMapper.selectById(comment.getParentId());
                if (parentComment != null) {
                    vo.setParentContent(parentComment.getContent());
                    // 获取被回复用户的昵称
                    SysUser parentUser = sysUserMapper.selectById(parentComment.getUserId());
                    if (parentUser != null) {
                        vo.setReplyToUserName(parentUser.getNickname());
                    }
                }
            }

            // 回复数量（只对顶级评论有意义）
            Long replyCount = commentMapper.selectCount(new LambdaQueryWrapper<RecipeComment>()
                    .eq(RecipeComment::getParentId, comment.getId()));
            vo.setReplyCount(replyCount != null ? replyCount.intValue() : 0);

            return vo;
        });

        return Result.ok(voPage);
    }

    @Override
    public Result<?> deleteMyComments(List<Long> commentIds) {
        Long userId = UserContext.getUserId();
        if (userId == null)
            return Result.fail("未登录");

        for (Long commentId : commentIds) {
            RecipeComment comment = commentMapper.selectById(commentId);
            if (comment != null && comment.getUserId().equals(userId)) {
                // 删除回复
                commentMapper.delete(new LambdaQueryWrapper<RecipeComment>()
                        .eq(RecipeComment::getParentId, commentId));
                // 删除点赞
                commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getCommentId, commentId));
                // 删除评论
                commentMapper.deleteById(commentId);
            }
        }
        return Result.ok("删除成功");
    }

    @Override
    public Result<IPage<CommentReplyVO>> pageRepliesForMe(Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        if (userId == null)
            return Result.fail("未登录");

        // 查找我的评论
        List<Long> myCommentIds = commentMapper.selectList(new LambdaQueryWrapper<RecipeComment>()
                .eq(RecipeComment::getUserId, userId)
                .select(RecipeComment::getId))
                .stream().map(RecipeComment::getId).collect(Collectors.toList());

        if (myCommentIds.isEmpty()) {
            return Result.ok(new Page<>(page, size, 0));
        }

        // 查找回复我的评论
        Page<RecipeComment> p = new Page<>(page, size);
        Page<RecipeComment> resultPage = commentMapper.selectPage(p, new LambdaQueryWrapper<RecipeComment>()
                .in(RecipeComment::getParentId, myCommentIds)
                .ne(RecipeComment::getUserId, userId) // 排除自己的回复
                .orderByDesc(RecipeComment::getCreateTime));

        IPage<CommentReplyVO> voPage = resultPage.convert(reply -> {
            CommentReplyVO vo = new CommentReplyVO();
            vo.setId(reply.getId());
            vo.setContent(reply.getContent());
            vo.setCreateTime(reply.getCreateTime());

            // 回复者信息
            SysUser replyUser = sysUserMapper.selectById(reply.getUserId());
            if (replyUser != null) {
                vo.setReplyUserId(replyUser.getId());
                vo.setReplyUserName(replyUser.getNickname());
                vo.setReplyUserAvatar(replyUser.getAvatar());
            }

            // 我的原评论
            RecipeComment myComment = commentMapper.selectById(reply.getParentId());
            if (myComment != null) {
                vo.setMyCommentId(myComment.getId());
                vo.setMyCommentContent(myComment.getContent());
            }

            // 菜谱信息
            RecipeInfo recipe = recipeInfoMapper.selectById(reply.getRecipeId());
            if (recipe != null) {
                vo.setRecipeId(recipe.getId());
                vo.setRecipeTitle(recipe.getTitle());
            }

            return vo;
        });

        return Result.ok(voPage);
    }

    @Override
    public Result<IPage<CommentLikeVO>> pageLikesForMe(Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        if (userId == null)
            return Result.fail("未登录");

        // 查找我的被点赞的评论
        Page<RecipeComment> p = new Page<>(page, size);
        Page<RecipeComment> resultPage = commentMapper.selectPage(p, new LambdaQueryWrapper<RecipeComment>()
                .eq(RecipeComment::getUserId, userId)
                .gt(RecipeComment::getLikeCount, 0)
                .orderByDesc(RecipeComment::getCreateTime));

        IPage<CommentLikeVO> voPage = resultPage.convert(comment -> {
            CommentLikeVO vo = new CommentLikeVO();
            vo.setCommentId(comment.getId());
            vo.setCommentContent(comment.getContent());
            vo.setLikeCount(comment.getLikeCount());

            // 菜谱信息
            RecipeInfo recipe = recipeInfoMapper.selectById(comment.getRecipeId());
            if (recipe != null) {
                vo.setRecipeId(recipe.getId());
                vo.setRecipeTitle(recipe.getTitle());
            }

            // 最近点赞者
            List<CommentLike> recentLikes = commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, comment.getId())
                    .orderByDesc(CommentLike::getCreateTime)
                    .last("LIMIT 3"));

            List<CommentLikeVO.LikerInfo> likers = recentLikes.stream().map(like -> {
                CommentLikeVO.LikerInfo liker = new CommentLikeVO.LikerInfo();
                SysUser user = sysUserMapper.selectById(like.getUserId());
                if (user != null) {
                    liker.setUserId(user.getId());
                    liker.setNickname(user.getNickname());
                    liker.setAvatar(user.getAvatar());
                }
                liker.setLikeTime(like.getCreateTime());
                return liker;
            }).collect(Collectors.toList());

            vo.setLikers(likers);
            if (!likers.isEmpty()) {
                vo.setLatestLikeTime(likers.get(0).getLikeTime());
            }

            return vo;
        });

        return Result.ok(voPage);
    }

    @Override
    public Result<IPage<CommentLikeVO>> pageLikeDetail(Long commentId, Integer page, Integer size) {
        // 获取评论的所有点赞者
        Page<CommentLike> p = new Page<>(page, size);
        Page<CommentLike> resultPage = commentLikeMapper.selectPage(p, new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .orderByDesc(CommentLike::getCreateTime));

        RecipeComment comment = commentMapper.selectById(commentId);

        Page<CommentLikeVO> voPage = new Page<>(page, size, resultPage.getTotal());

        // 转换为单个点赞者信息的VO列表
        List<CommentLikeVO> likeList = resultPage.getRecords().stream().map(like -> {
            CommentLikeVO vo = new CommentLikeVO();
            vo.setCommentId(commentId);
            if (comment != null) {
                vo.setCommentContent(comment.getContent());
            }

            CommentLikeVO.LikerInfo liker = new CommentLikeVO.LikerInfo();
            SysUser user = sysUserMapper.selectById(like.getUserId());
            if (user != null) {
                liker.setUserId(user.getId());
                liker.setNickname(user.getNickname());
                liker.setAvatar(user.getAvatar());
            }
            liker.setLikeTime(like.getCreateTime());
            vo.setLikers(List.of(liker));
            vo.setLatestLikeTime(like.getCreateTime());

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(likeList);
        return Result.ok(voPage);
    }

    @Override
    public Result<IPage<SystemNotificationVO>> pageSystemNotifications(Integer page, Integer size) {
        // 系统通知目前从前端 notificationStore 获取
        // 这里返回空列表，实际生产中应该从数据库获取
        Page<SystemNotificationVO> emptyPage = new Page<>(page, size, 0);
        emptyPage.setRecords(new ArrayList<>());
        return Result.ok(emptyPage);
    }
}
