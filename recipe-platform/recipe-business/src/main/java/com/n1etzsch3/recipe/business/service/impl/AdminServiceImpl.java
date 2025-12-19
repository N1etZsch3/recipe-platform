package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.service.AdminService;
import com.n1etzsch3.recipe.common.constant.RecipeConstants;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeCategoryMapper categoryMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public Result<IPage<RecipeDetailDTO>> pageAuditRecipes(Integer page, Integer size) {
        Page<RecipeInfo> p = new Page<>(page, size);
        Page<RecipeInfo> resultPage = recipeInfoMapper.selectPage(p, new LambdaQueryWrapper<RecipeInfo>()
                .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PENDING) // 待审核
                .orderByDesc(RecipeInfo::getCreateTime));

        IPage<RecipeDetailDTO> dtoPage = resultPage.convert(recipe -> {
            RecipeDetailDTO dto = new RecipeDetailDTO();
            BeanUtil.copyProperties(recipe, dto);
            SysUser author = sysUserMapper.selectById(recipe.getUserId());
            if (author != null) {
                dto.setAuthorName(author.getNickname());
                dto.setAuthorAvatar(author.getAvatar());
            }
            return dto;
        });
        return Result.ok(dtoPage);
    }

    @Override
    public Result<?> auditRecipe(AuditDTO auditDTO) {
        log.info("正在处理审核: recipeId={}, action={}, reason={}", auditDTO.getRecipeId(), auditDTO.getAction(),
                auditDTO.getReason());
        RecipeInfo recipe = recipeInfoMapper.selectById(auditDTO.getRecipeId());
        if (recipe == null)
            return Result.fail("菜谱不存在");

        if ("pass".equals(auditDTO.getAction())) {
            recipe.setStatus(RecipeConstants.STATUS_PUBLISHED); // 已发布
        } else if ("reject".equals(auditDTO.getAction())) {
            recipe.setStatus(RecipeConstants.STATUS_REJECTED); // 驳回
            recipe.setRejectReason(auditDTO.getReason());
        } else {
            return Result.fail("未知操作");
        }
        recipe.setUpdateTime(LocalDateTime.now());
        recipeInfoMapper.updateById(recipe);

        return Result.ok("操作成功");
    }

    @Override
    public Result<?> addCategory(RecipeCategory category) {
        categoryMapper.insert(category);
        return Result.ok("添加成功");
    }

    @Override
    public Result<?> deleteCategory(Integer id) {
        categoryMapper.deleteById(id);
        return Result.ok("删除成功");
    }

    @Override
    public Result<IPage<SysUser>> pageUsers(Integer page, Integer size) {
        Page<SysUser> p = new Page<>(page, size);
        Page<SysUser> result = sysUserMapper.selectPage(p, new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreateTime));
        // 注意：生产环境应脱敏，此处简化直接返回实体
        return Result.ok(result);
    }

    @Override
    public Result<?> updateUserStatus(Long userId, UserStatusDTO statusDTO) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null)
            return Result.fail("用户不存在");

        user.setStatus(statusDTO.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        return Result.ok("状态修改成功");
    }
}
