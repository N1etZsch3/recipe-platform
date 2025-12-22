package com.n1etzsch3.recipe.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.CommentDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.DashboardDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.service.AdminAuthService;
import com.n1etzsch3.recipe.business.service.AdminCategoryService;
import com.n1etzsch3.recipe.business.service.AdminCommentService;
import com.n1etzsch3.recipe.business.service.AdminDashboardService;
import com.n1etzsch3.recipe.business.service.AdminRecipeService;
import com.n1etzsch3.recipe.business.service.AdminService;
import com.n1etzsch3.recipe.business.service.AdminUserService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminAuthService adminAuthService;
    private final AdminDashboardService adminDashboardService;
    private final AdminRecipeService adminRecipeService;
    private final AdminCategoryService adminCategoryService;
    private final AdminUserService adminUserService;
    private final AdminCommentService adminCommentService;

    @Override
    public Result<Map<String, Object>> adminLogin(String username, String password) {
        return adminAuthService.adminLogin(username, password);
    }

    @Override
    public Result<DashboardDTO> getDashboard() {
        return adminDashboardService.getDashboard();
    }

    @Override
    public Result<IPage<RecipeDetailDTO>> pageAuditRecipes(Integer page, Integer size) {
        return adminRecipeService.pageAuditRecipes(page, size);
    }

    @Override
    public Result<?> auditRecipe(AuditDTO auditDTO) {
        return adminRecipeService.auditRecipe(auditDTO);
    }

    @Override
    public Result<IPage<RecipeDetailDTO>> pageAllRecipes(Integer page, Integer size, Integer status, String keyword) {
        return adminRecipeService.pageAllRecipes(page, size, status, keyword);
    }

    @Override
    public Result<?> deleteRecipe(Long recipeId) {
        return adminRecipeService.deleteRecipe(recipeId);
    }

    @Override
    public Result<?> batchAuditRecipes(List<Long> ids, String action, String reason) {
        return adminRecipeService.batchAuditRecipes(ids, action, reason);
    }

    @Override
    public Result<?> batchUpdateRecipeStatus(List<Long> ids, Integer status) {
        return adminRecipeService.batchUpdateRecipeStatus(ids, status);
    }

    @Override
    public Result<List<RecipeCategory>> listCategories() {
        return adminCategoryService.listCategories();
    }

    @Override
    public Result<?> addCategory(RecipeCategory category) {
        return adminCategoryService.addCategory(category);
    }

    @Override
    public Result<?> updateCategory(Integer id, RecipeCategory category) {
        return adminCategoryService.updateCategory(id, category);
    }

    @Override
    public Result<?> deleteCategory(Integer id) {
        return adminCategoryService.deleteCategory(id);
    }

    @Override
    public Result<IPage<UserDTO>> pageUsers(Integer page, Integer size, String keyword, String role, String sortBy) {
        return adminUserService.pageUsers(page, size, keyword, role, sortBy);
    }

    @Override
    public Result<?> addUser(SysUser user) {
        return adminUserService.addUser(user);
    }

    @Override
    public Result<?> updateUser(Long id, SysUser user) {
        return adminUserService.updateUser(id, user);
    }

    @Override
    public Result<?> updateUserStatus(Long userId, UserStatusDTO statusDTO) {
        return adminUserService.updateUserStatus(userId, statusDTO);
    }

    @Override
    public Result<?> batchUpdateStatus(List<Long> ids, Integer status) {
        return adminUserService.batchUpdateStatus(ids, status);
    }

    @Override
    public Result<IPage<CommentDetailDTO>> pageComments(Integer page, Integer size, String keyword,
            Long userId, Long recipeId, String sortBy) {
        return adminCommentService.pageComments(page, size, keyword, userId, recipeId, sortBy);
    }

    @Override
    public Result<?> deleteComment(Long commentId) {
        return adminCommentService.deleteComment(commentId);
    }
}
