package com.n1etzsch3.recipe.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.service.AdminService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // --- 菜谱审核 ---

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

    // --- 分类管理 ---

    @PostMapping("/categories")
    public Result<?> addCategory(@RequestBody RecipeCategory category) {
        return adminService.addCategory(category);
    }

    @DeleteMapping("/categories/{id}")
    public Result<?> deleteCategory(@PathVariable Integer id) {
        return adminService.deleteCategory(id);
    }

    // --- 用户管理 ---

    @GetMapping("/users")
    public Result<IPage<SysUser>> pageUsers(@RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return adminService.pageUsers(page, size);
    }

    @PutMapping("/users/{userId}/status")
    public Result<?> updateUserStatus(@PathVariable Long userId, @RequestBody UserStatusDTO statusDTO) {
        return adminService.updateUserStatus(userId, statusDTO);
    }
}
