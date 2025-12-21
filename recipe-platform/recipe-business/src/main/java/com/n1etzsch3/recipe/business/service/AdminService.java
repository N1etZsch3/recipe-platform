package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.*;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;

import java.util.List;
import java.util.Map;

public interface AdminService {

    // --- Admin Login ---
    Result<Map<String, Object>> adminLogin(String username, String password);

    // --- Dashboard ---
    Result<DashboardDTO> getDashboard();

    // --- Recipe Audit ---
    Result<IPage<RecipeDetailDTO>> pageAuditRecipes(Integer page, Integer size);

    Result<?> auditRecipe(AuditDTO auditDTO);

    // --- All Recipes Management ---
    Result<IPage<RecipeDetailDTO>> pageAllRecipes(Integer page, Integer size, Integer status, String keyword);

    Result<?> deleteRecipe(Long recipeId);

    // --- Category Manage ---
    Result<List<RecipeCategory>> listCategories();

    Result<?> addCategory(RecipeCategory category);

    Result<?> updateCategory(Integer id, RecipeCategory category);

    Result<?> deleteCategory(Integer id);

    // --- User Manage ---
    Result<IPage<UserDTO>> pageUsers(Integer page, Integer size, String keyword, String role, String sortBy);

    Result<?> addUser(SysUser user);

    Result<?> updateUser(Long id, SysUser user);

    Result<?> updateUserStatus(Long userId, UserStatusDTO statusDTO);

    Result<?> batchUpdateStatus(List<Long> ids, Integer status);

    // --- Comment Manage ---
    Result<IPage<CommentDetailDTO>> pageComments(Integer page, Integer size, String keyword,
            Long userId, Long recipeId, String sortBy);

    Result<?> deleteComment(Long commentId);
}
