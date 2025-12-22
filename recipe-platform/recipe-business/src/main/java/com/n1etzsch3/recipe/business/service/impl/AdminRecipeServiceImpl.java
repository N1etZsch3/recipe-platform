package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.entity.RecipeComment;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeCommentMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeIngredientMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeStepMapper;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.business.service.AdminRecipeService;
import com.n1etzsch3.recipe.business.service.NotificationService;
import com.n1etzsch3.recipe.common.constant.RecipeConstants;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRecipeServiceImpl implements AdminRecipeService {

    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeCategoryMapper categoryMapper;
    private final RecipeCommentMapper commentMapper;
    private final RecipeIngredientMapper ingredientMapper;
    private final RecipeStepMapper stepMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;
    private final AdminLogService adminLogService;

    @Override
    public Result<IPage<RecipeDetailDTO>> pageAuditRecipes(Integer page, Integer size) {
        Page<RecipeInfo> p = new Page<>(page, size);
        Page<RecipeInfo> resultPage = recipeInfoMapper.selectPage(p, new LambdaQueryWrapper<RecipeInfo>()
                .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PENDING)
                .orderByDesc(RecipeInfo::getCreateTime));

        return Result.ok(convertToDetailPage(resultPage));
    }

    @Override
    public Result<?> auditRecipe(AuditDTO auditDTO) {
        log.info("正在处理审核: recipeId={}, action={}, reason={}", auditDTO.getRecipeId(), auditDTO.getAction(),
                auditDTO.getReason());
        RecipeInfo recipe = recipeInfoMapper.selectById(auditDTO.getRecipeId());
        if (recipe == null) {
            return Result.fail("菜谱不存在");
        }

        if ("pass".equals(auditDTO.getAction())) {
            recipe.setStatus(RecipeConstants.STATUS_PUBLISHED);
            notificationService.sendRecipeApproved(recipe.getUserId(), recipe.getId(), recipe.getTitle());
            adminLogService.log("RECIPE_APPROVE", "recipe", recipe.getId(), recipe.getTitle(), null);
        } else if ("reject".equals(auditDTO.getAction())) {
            recipe.setStatus(RecipeConstants.STATUS_REJECTED);
            recipe.setRejectReason(auditDTO.getReason());
            notificationService.sendRecipeRejected(recipe.getUserId(), recipe.getId(),
                    recipe.getTitle(), auditDTO.getReason());
            adminLogService.log("RECIPE_REJECT", "recipe", recipe.getId(), recipe.getTitle(), auditDTO.getReason());
        } else {
            return Result.fail("未知操作");
        }
        recipe.setUpdateTime(LocalDateTime.now());
        recipeInfoMapper.updateById(recipe);

        return Result.ok("操作成功");
    }

    @Override
    public Result<IPage<RecipeDetailDTO>> pageAllRecipes(Integer page, Integer size, Integer status, String keyword) {
        Page<RecipeInfo> p = new Page<>(page, size);
        LambdaQueryWrapper<RecipeInfo> wrapper = new LambdaQueryWrapper<RecipeInfo>()
                .eq(status != null, RecipeInfo::getStatus, status)
                .like(StrUtil.isNotBlank(keyword), RecipeInfo::getTitle, keyword)
                .orderByDesc(RecipeInfo::getCreateTime);

        Page<RecipeInfo> resultPage = recipeInfoMapper.selectPage(p, wrapper);
        return Result.ok(convertToDetailPage(resultPage));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteRecipe(Long recipeId) {
        RecipeInfo recipe = recipeInfoMapper.selectById(recipeId);
        if (recipe == null) {
            return Result.fail("菜谱不存在");
        }

        ingredientMapper.delete(new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.RecipeIngredient>()
                .eq(com.n1etzsch3.recipe.business.entity.RecipeIngredient::getRecipeId, recipeId));
        stepMapper.delete(new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.RecipeStep>()
                .eq(com.n1etzsch3.recipe.business.entity.RecipeStep::getRecipeId, recipeId));
        commentMapper.delete(new LambdaQueryWrapper<RecipeComment>()
                .eq(RecipeComment::getRecipeId, recipeId));

        recipeInfoMapper.deleteById(recipeId);

        adminLogService.log("RECIPE_DELETE", "recipe", recipeId, recipe.getTitle(), null);
        log.info("管理员删除菜谱: id={}, title={}", recipeId, recipe.getTitle());
        return Result.ok("删除成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchAuditRecipes(List<Long> ids, String action, String reason) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要审核的菜谱");
        }

        int newStatus = "pass".equalsIgnoreCase(action) ? RecipeConstants.STATUS_PUBLISHED
                : RecipeConstants.STATUS_REJECTED;
        int count = 0;

        List<RecipeInfo> recipes = recipeInfoMapper.selectList(new LambdaQueryWrapper<RecipeInfo>()
                .in(RecipeInfo::getId, ids)
                .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PENDING));
        for (RecipeInfo recipe : recipes) {
            recipe.setStatus(newStatus);
            if (newStatus == RecipeConstants.STATUS_REJECTED) {
                recipe.setRejectReason(reason);
            }
            recipeInfoMapper.updateById(recipe);

            if (newStatus == RecipeConstants.STATUS_PUBLISHED) {
                notificationService.sendRecipeApproved(recipe.getUserId(), recipe.getId(), recipe.getTitle());
            } else {
                notificationService.sendRecipeRejected(recipe.getUserId(), recipe.getId(), recipe.getTitle(), reason);
            }
            count++;
        }

        String actionDesc = newStatus == RecipeConstants.STATUS_PUBLISHED ? "批量通过" : "批量驳回";
        adminLogService.log("RECIPE_BATCH_AUDIT", "recipe", null, actionDesc + " " + count + " 个菜谱", null);
        log.info("管理员批量审核菜谱: action={}, count={}", action, count);
        return Result.ok(actionDesc + "成功，共 " + count + " 个菜谱");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchUpdateRecipeStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要操作的菜谱");
        }
        if (status == null) {
            return Result.fail("请指定目标状态");
        }

        RecipeInfo update = new RecipeInfo();
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        int count = recipeInfoMapper.update(update, new LambdaQueryWrapper<RecipeInfo>().in(RecipeInfo::getId, ids));

        String statusDesc = status == RecipeConstants.STATUS_PUBLISHED ? "上架"
                : (status == RecipeConstants.STATUS_PENDING ? "下架" : "更新状态");
        adminLogService.log("RECIPE_BATCH_STATUS", "recipe", null, "批量" + statusDesc + " " + count + " 个菜谱", null);
        log.info("管理员批量更新菜谱状态: status={}, count={}", status, count);
        return Result.ok("批量" + statusDesc + "成功，共 " + count + " 个菜谱");
    }

    private IPage<RecipeDetailDTO> convertToDetailPage(Page<RecipeInfo> resultPage) {
        Map<Integer, RecipeCategory> categoryMap = new HashMap<>();
        Map<Long, SysUser> authorMap = new HashMap<>();
        List<RecipeInfo> recipes = resultPage.getRecords();
        if (!recipes.isEmpty()) {
            Set<Integer> categoryIds = new HashSet<>();
            Set<Long> authorIds = new HashSet<>();
            for (RecipeInfo recipe : recipes) {
                if (recipe.getCategoryId() != null) {
                    categoryIds.add(recipe.getCategoryId());
                }
                if (recipe.getUserId() != null) {
                    authorIds.add(recipe.getUserId());
                }
            }

            if (!categoryIds.isEmpty()) {
                List<RecipeCategory> categories = categoryMapper.selectList(
                        new LambdaQueryWrapper<RecipeCategory>().in(RecipeCategory::getId, categoryIds));
                for (RecipeCategory category : categories) {
                    categoryMap.put(category.getId(), category);
                }
            }

            if (!authorIds.isEmpty()) {
                List<SysUser> authors = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getId, authorIds));
                for (SysUser author : authors) {
                    authorMap.put(author.getId(), author);
                }
            }
        }

        return resultPage.convert(recipe -> {
            RecipeDetailDTO dto = new RecipeDetailDTO();
            BeanUtil.copyProperties(recipe, dto);

            RecipeCategory category = categoryMap.get(recipe.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }

            SysUser author = authorMap.get(recipe.getUserId());
            if (author != null) {
                dto.setAuthorId(author.getId());
                dto.setAuthorName(author.getNickname());
                dto.setAuthorAvatar(author.getAvatar());
            }
            return dto;
        });
    }
}
