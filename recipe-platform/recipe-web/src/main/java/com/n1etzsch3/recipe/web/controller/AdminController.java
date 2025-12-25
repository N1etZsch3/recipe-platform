package com.n1etzsch3.recipe.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.AdminBatchAuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.AdminCategoryDTO;
import com.n1etzsch3.recipe.business.domain.dto.AdminOperationLogDTO;
import com.n1etzsch3.recipe.business.domain.dto.AdminRecipeBatchStatusDTO;
import com.n1etzsch3.recipe.business.domain.dto.AdminUserBatchStatusDTO;
import com.n1etzsch3.recipe.business.domain.dto.AdminUserCreateDTO;
import com.n1etzsch3.recipe.business.domain.dto.AdminUserUpdateDTO;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.CommentDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.DashboardDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.business.service.AdminService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.framework.service.UserOnlineService;
import com.n1etzsch3.recipe.system.domain.dto.LoginDTO;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.service.CaptchaService;
import jakarta.validation.Valid;
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
    private final UserOnlineService userOnlineService;
    private final CaptchaService captchaService;

    // ================== Admin Login ==================

    @PostMapping("/login")
    public Result<Map<String, Object>> adminLogin(@RequestBody @Valid LoginDTO loginDTO) {
        log.info("管理员登录: {}", loginDTO.getUsername());
        // 验证码校验
        if (!captchaService.validateCaptcha(loginDTO.getCaptchaId(), loginDTO.getCaptchaCode())) {
            return Result.fail("验证码错误或已过期");
        }
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
    public Result<?> auditRecipe(@RequestBody @Valid AuditDTO auditDTO) {
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

    /**
     * 批量审核菜谱
     */
    @PostMapping("/recipes/batch/audit")
    public Result<?> batchAuditRecipes(@RequestBody @Valid AdminBatchAuditDTO dto) {
        log.info("管理员批量审核菜谱: {}", dto);
        String reason = dto.getReason() != null ? dto.getReason() : "";
        return adminService.batchAuditRecipes(dto.getIds(), dto.getAction(), reason);
    }

    /**
     * 批量更新菜谱状态 (上架/下架)
     */
    @PutMapping("/recipes/batch/status")
    public Result<?> batchUpdateRecipeStatus(@RequestBody @Valid AdminRecipeBatchStatusDTO dto) {
        log.info("管理员批量更新菜谱状态: {}", dto);
        return adminService.batchUpdateRecipeStatus(dto.getIds(), dto.getStatus());
    }

    // ================== Category Management ==================

    @GetMapping("/categories")
    public Result<List<RecipeCategory>> listCategories() {
        return adminService.listCategories();
    }

    @PostMapping("/categories")
    public Result<?> addCategory(@RequestBody @Valid AdminCategoryDTO dto) {
        log.info("管理员新增分类: {}", dto.getName());
        RecipeCategory category = new RecipeCategory();
        category.setName(dto.getName());
        category.setSortOrder(dto.getSortOrder());
        return adminService.addCategory(category);
    }

    @PutMapping("/categories/{id}")
    public Result<?> updateCategory(@PathVariable Integer id, @RequestBody @Valid AdminCategoryDTO dto) {
        log.info("管理员修改分类: id={}, name={}", id, dto.getName());
        RecipeCategory category = new RecipeCategory();
        category.setName(dto.getName());
        category.setSortOrder(dto.getSortOrder());
        return adminService.updateCategory(id, category);
    }

    @DeleteMapping("/categories/{id}")
    public Result<?> deleteCategory(@PathVariable Integer id) {
        log.info("管理员删除分类: id={}", id);
        return adminService.deleteCategory(id);
    }

    // ================== User Management ==================

    @GetMapping("/users")
    public Result<IPage<UserDTO>> pageUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String sortBy) {
        log.info("管理员获取用户列表: page={}, size={}, keyword={}, role={}, sortBy={}", page, size, keyword, role, sortBy);
        return adminService.pageUsers(page, size, keyword, role, sortBy);
    }

    @PostMapping("/users")
    public Result<?> addUser(@RequestBody @Valid AdminUserCreateDTO dto) {
        log.info("管理员新增用户: {}", dto.getUsername());
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());
        user.setRole(dto.getRole());
        user.setIntro(dto.getIntro());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus());
        return adminService.addUser(user);
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody @Valid AdminUserUpdateDTO dto) {
        log.info("管理员修改用户: id={}", id);
        SysUser user = new SysUser();
        user.setNickname(dto.getNickname());
        user.setRole(dto.getRole());
        user.setIntro(dto.getIntro());
        user.setAvatar(dto.getAvatar());
        user.setPassword(dto.getPassword());
        return adminService.updateUser(id, user);
    }

    @PutMapping("/users/{userId}/status")
    public Result<?> updateUserStatus(@PathVariable Long userId, @RequestBody @Valid UserStatusDTO statusDTO) {
        log.info("管理员修改用户状态: userId={}, status={}", userId, statusDTO.getStatus());
        return adminService.updateUserStatus(userId, statusDTO);
    }

    @PutMapping("/users/batch/status")
    public Result<?> batchUpdateStatus(@RequestBody @Valid AdminUserBatchStatusDTO dto) {
        log.info("管理员批量修改用户状态: {}", dto);
        return adminService.batchUpdateStatus(dto.getIds(), dto.getStatus());
    }

    /**
     * 获取用户在线状态
     */
    @GetMapping("/users/online")
    public Result<java.util.Map<Long, Boolean>> getUsersOnlineStatus(@RequestParam List<Long> userIds) {
        log.info("查询用户在线状态: userIds={}", userIds);
        java.util.Map<Long, Boolean> result = userOnlineService.batchCheckOnline(userIds);
        return Result.ok(result);
    }

    /**
     * 踢用户下线
     */
    @PostMapping("/users/{userId}/kick")
    public Result<?> kickUser(@PathVariable Long userId) {
        log.info("管理员踢用户下线: userId={}", userId);
        userOnlineService.kickUser(userId);
        return Result.ok("已将用户踢下线");
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

    // ================== Team Member Management ==================

    private final com.n1etzsch3.recipe.business.service.TeamService teamService;

    @GetMapping("/team/members")
    public Result<java.util.List<com.n1etzsch3.recipe.business.entity.TeamMember>> listTeamMembers() {
        log.info("管理员获取团队成员列表");
        return teamService.listMembers();
    }

    @PostMapping("/team/members")
    public Result<?> addTeamMember(@RequestBody com.n1etzsch3.recipe.business.entity.TeamMember member) {
        log.info("管理员新增团队成员: {}", member.getName());
        return teamService.addMember(member);
    }

    @PutMapping("/team/members/{id}")
    public Result<?> updateTeamMember(@PathVariable Integer id,
            @RequestBody com.n1etzsch3.recipe.business.entity.TeamMember member) {
        log.info("管理员修改团队成员: id={}", id);
        return teamService.updateMember(id, member);
    }

    @DeleteMapping("/team/members/{id}")
    public Result<?> deleteTeamMember(@PathVariable Integer id) {
        log.info("管理员删除团队成员: id={}", id);
        return teamService.deleteMember(id);
    }
}
