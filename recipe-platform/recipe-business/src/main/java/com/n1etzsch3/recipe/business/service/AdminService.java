package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;

public interface AdminService {

    // --- Recipe Audit ---
    Result<IPage<RecipeDetailDTO>> pageAuditRecipes(Integer page, Integer size);

    Result<?> auditRecipe(AuditDTO auditDTO);

    // --- Category Manage ---
    Result<?> addCategory(RecipeCategory category);

    Result<?> deleteCategory(Integer id);

    // --- User Manage ---
    Result<IPage<SysUser>> pageUsers(Integer page, Integer size);

    Result<?> updateUserStatus(Long userId, UserStatusDTO statusDTO);
}
