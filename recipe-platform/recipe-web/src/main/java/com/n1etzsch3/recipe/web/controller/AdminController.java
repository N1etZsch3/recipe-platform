package com.n1etzsch3.recipe.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.AdminOperationLogDTO;
import com.n1etzsch3.recipe.business.domain.dto.CommentDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.DashboardDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.business.service.AdminService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.domain.dto.LoginDTO;
import com.n1etzsch3.recipe.system.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminLogService adminLogService;

    // ================== Admin Login ==================

    @PostMapping("/login")
    public Result<Map<String, Object>> adminLogin(@RequestBody LoginDTO loginDTO) {
        log.info("管理员登录: {}", loginDTO.getUsername());
        return adminService.adminLogin(loginDTO.getUsername(), loginDTO.getPassword());
    }

    // ================== Dashboard ==================

    @GetMapping("/dashboard")
    public Result<DashboardDTO> getDashboard() {
        log.info("管理员获取仪表盘数据");
        return adminService.getDashboard();
    }

    // ================== Recipe Audit ==================

    @GetMapping("/recipes/audit")
    public Result<IPage<RecipeDetailDTO>> listAuditRecipes(@RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("管理员获取待审核列表: page={}, size={}", page, size);
        return adminService.pageAuditRecipes(page, size);
    }

    @PostMapping("/recipes/audit")
    public Result<?> auditRecipe(@RequestBody AuditDTO auditDTO) {
        log.info("管理员提交审核: {}", auditDTO);
        return adminService.auditRecipe(auditDTO);
    }

    // ================== All Recipes Management ==================

    @GetMapping("/recipes")
    public Result<IPage<RecipeDetailDTO>> listAllRecipes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        log.info("管理员获取菜谱列表: page={}, size={}, status={}, keyword={}", page, size, status, keyword);
        return adminService.pageAllRecipes(page, size, status, keyword);
    }

    @DeleteMapping("/recipes/{id}")
    public Result<?> deleteRecipe(@PathVariable Long id) {
        log.info("管理员删除菜谱: id={}", id);
        return adminService.deleteRecipe(id);
    }

    // ================== Category Management ==================

    @GetMapping("/categories")
    public Result<List<RecipeCategory>> listCategories() {
        return adminService.listCategories();
    }

    @PostMapping("/categories")
    public Result<?> addCategory(@RequestBody RecipeCategory category) {
        log.info("管理员新增分类: {}", category.getName());
        return adminService.addCategory(category);
    }

    @PutMapping("/categories/{id}")
    public Result<?> updateCategory(@PathVariable Integer id, @RequestBody RecipeCategory category) {
        log.info("管理员修改分类: id={}, name={}", id, category.getName());
        return adminService.updateCategory(id, category);
    }

    @DeleteMapping("/categories/{id}")
    public Result<?> deleteCategory(@PathVariable Integer id) {
        log.info("管理员删除分类: id={}", id);
        return adminService.deleteCategory(id);
    }

    // ================== User Management ==================

    @GetMapping("/users")
    public Result<IPage<SysUser>> pageUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        log.info("管理员获取用户列表: page={}, size={}, keyword={}", page, size, keyword);
        return adminService.pageUsers(page, size, keyword);
    }

    @PutMapping("/users/{userId}/status")
    public Result<?> updateUserStatus(@PathVariable Long userId, @RequestBody UserStatusDTO statusDTO) {
        log.info("管理员修改用户状态: userId={}, status={}", userId, statusDTO.getStatus());
        return adminService.updateUserStatus(userId, statusDTO);
    }

    // ================== Comment Management ==================

    @GetMapping("/comments")
    public Result<IPage<CommentDetailDTO>> pageComments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long recipeId,
            @RequestParam(required = false) String sortBy) {
        log.info("管理员获取评论列表: page={}, size={}, keyword={}, userId={}, recipeId={}, sortBy={}",
                page, size, keyword, userId, recipeId, sortBy);
        return adminService.pageComments(page, size, keyword, userId, recipeId, sortBy);
    }

    @DeleteMapping("/comments/{id}")
    public Result<?> deleteComment(@PathVariable Long id) {
        log.info("管理员删除评论: id={}", id);
        return adminService.deleteComment(id);
    }

    // ================== Operation Logs ==================

    @GetMapping("/logs")
    public Result<IPage<AdminOperationLogDTO>> pageLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("查询操作日志: page={}, size={}, type={}", page, size, operationType);
        return adminLogService.pageLogs(page, size, operationType, adminId, startDate, endDate);
    }
}
