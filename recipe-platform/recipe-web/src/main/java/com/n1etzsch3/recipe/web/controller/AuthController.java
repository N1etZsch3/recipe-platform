package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.common.utils.JwtUtils;
import com.n1etzsch3.recipe.framework.service.TokenBlacklistService;
import com.n1etzsch3.recipe.system.domain.dto.LoginDTO;
import com.n1etzsch3.recipe.system.domain.dto.PasswordUpdateDTO;
import com.n1etzsch3.recipe.system.domain.dto.RegisterDTO;
import com.n1etzsch3.recipe.system.domain.dto.UserProfileDTO;
import com.n1etzsch3.recipe.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;

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
        return authService.login(loginDTO);
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
     * 退出登录 - 将Token加入黑名单
     */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                String jti = JwtUtils.getJtiFromToken(token);
                long remainingSeconds = JwtUtils.getRemainingExpireSeconds(token);
                if (jti != null && remainingSeconds > 0) {
                    tokenBlacklistService.addToBlacklist(jti, remainingSeconds);
                }
            } catch (Exception e) {
                // Token 可能无效，忽略错误
            }
        }
        return Result.ok("退出成功");
    }
}
