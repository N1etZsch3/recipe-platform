package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.domain.dto.LoginDTO;
import com.n1etzsch3.recipe.system.domain.dto.PasswordUpdateDTO;
import com.n1etzsch3.recipe.system.domain.dto.RegisterDTO;
import com.n1etzsch3.recipe.system.domain.dto.UserProfileDTO;
import com.n1etzsch3.recipe.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody @jakarta.validation.Valid RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @jakarta.validation.Valid LoginDTO loginDTO) {
        return authService.login(loginDTO.getUsername(), loginDTO.getPassword());
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<?> getProfile() {
        return authService.getProfile(UserContext.getUserId());
    }

    /**
     * 修改个人资料
     */
    @PutMapping("/me")
    public Result<?> updateProfile(@RequestBody UserProfileDTO profileDTO) {
        return authService.updateProfile(UserContext.getUserId(), profileDTO);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody @jakarta.validation.Valid PasswordUpdateDTO passwordDTO) {
        return authService.updatePassword(UserContext.getUserId(), passwordDTO);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        // JWT无状态，前端清除token即可
        return Result.ok("退出成功");
    }
}
