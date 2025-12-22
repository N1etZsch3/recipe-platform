package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.CommentDetailDTO;
import com.n1etzsch3.recipe.business.entity.RecipeComment;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.service.AdminCommentService;
import com.n1etzsch3.recipe.business.mapper.RecipeCommentMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommentServiceImpl implements AdminCommentService {

    private final RecipeCommentMapper commentMapper;
    private final RecipeInfoMapper recipeInfoMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public Result<IPage<CommentDetailDTO>> pageComments(Integer page, Integer size, String keyword,
            Long userId, Long recipeId, String sortBy) {
        Page<RecipeComment> p = new Page<>(page, size);
        LambdaQueryWrapper<RecipeComment> wrapper = new LambdaQueryWrapper<RecipeComment>()
                .like(StrUtil.isNotBlank(keyword), RecipeComment::getContent, keyword)
                .eq(userId != null, RecipeComment::getUserId, userId)
                .eq(recipeId != null, RecipeComment::getRecipeId, recipeId);

        if ("oldest".equals(sortBy)) {
            wrapper.orderByAsc(RecipeComment::getCreateTime);
        } else {
            wrapper.orderByDesc(RecipeComment::getCreateTime);
        }

        Page<RecipeComment> resultPage = commentMapper.selectPage(p, wrapper);

        Map<Long, RecipeInfo> recipeMap = new HashMap<>();
        Map<Long, SysUser> userMap = new HashMap<>();
        List<RecipeComment> comments = resultPage.getRecords();
        if (!comments.isEmpty()) {
            Set<Long> recipeIds = new HashSet<>();
            Set<Long> userIds = new HashSet<>();
            for (RecipeComment comment : comments) {
                if (comment.getRecipeId() != null) {
                    recipeIds.add(comment.getRecipeId());
                }
                if (comment.getUserId() != null) {
                    userIds.add(comment.getUserId());
                }
            }

            if (!recipeIds.isEmpty()) {
                List<RecipeInfo> recipes = recipeInfoMapper.selectList(
                        new LambdaQueryWrapper<RecipeInfo>().in(RecipeInfo::getId, recipeIds));
                for (RecipeInfo recipe : recipes) {
                    recipeMap.put(recipe.getId(), recipe);
                }
            }

            if (!userIds.isEmpty()) {
                List<SysUser> users = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds));
                for (SysUser user : users) {
                    userMap.put(user.getId(), user);
                }
            }
        }

        IPage<CommentDetailDTO> dtoPage = resultPage.convert(comment -> {
            CommentDetailDTO dto = new CommentDetailDTO();
            BeanUtil.copyProperties(comment, dto);

            RecipeInfo recipe = recipeMap.get(comment.getRecipeId());
            if (recipe != null) {
                dto.setRecipeTitle(recipe.getTitle());
            }

            SysUser user = userMap.get(comment.getUserId());
            if (user != null) {
                dto.setUserNickname(user.getNickname());
                dto.setUserAvatar(user.getAvatar());
            }

            return dto;
        });

        return Result.ok(dtoPage);
    }

    @Override
    public Result<?> deleteComment(Long commentId) {
        RecipeComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        commentMapper.deleteById(commentId);
        log.info("管理员删除评论: id={}", commentId);
        return Result.ok("删除成功");
    }
}
