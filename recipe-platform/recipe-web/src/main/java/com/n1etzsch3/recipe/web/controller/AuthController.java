package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.common.utils.JwtUtils;
import com.n1etzsch3.recipe.framework.annotation.RateLimit;
import com.n1etzsch3.recipe.framework.service.TokenBlacklistService;
import com.n1etzsch3.recipe.system.domain.dto.LoginDTO;
import com.n1etzsch3.recipe.system.domain.dto.PasswordUpdateDTO;
import com.n1etzsch3.recipe.system.domain.dto.RegisterDTO;
import com.n1etzsch3.recipe.system.domain.dto.UserProfileDTO;
import com.n1etzsch3.recipe.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 用户注册
     */
    @RateLimit(time = 60, count = 3, limitType = RateLimit.LimitType.IP)
    @PostMapping("/register")
    public Result<?> register(@RequestBody @Valid RegisterDTO registerDTO) {
        log.info("用户注册请求: username={}", registerDTO.getUsername());
        return authService.register(registerDTO);
    }

    /**
     * 用户登录
     */
    @RateLimit(time = 60, count = 5, limitType = RateLimit.LimitType.IP)
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginDTO loginDTO) {
        log.info("用户登录请求: username={}", loginDTO.getUsername());
        return authService.login(loginDTO);
    }

    /**
     * 强制登录（用户确认后踢掉已在线的旧会话）
     */
    @RateLimit(time = 60, count = 5, limitType = RateLimit.LimitType.IP)
    @PostMapping("/force-login")
    public Result<Map<String, Object>> forceLogin(@RequestBody @Valid LoginDTO loginDTO) {
        log.info("用户强制登录请求: username={}", loginDTO.getUsername());
        return authService.forceLogin(loginDTO);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<?> getProfile() {
        log.info("获取用户信息: userId={}", UserContext.getUserId());
        return authService.getProfile(UserContext.getUserId());
    }

    /**
     * 修改个人资料
     */
    @PutMapping("/me")
    public Result<?> updateProfile(@RequestBody @Valid UserProfileDTO profileDTO) {
        log.info("修改个人资料: userId={}", UserContext.getUserId());
        return authService.updateProfile(UserContext.getUserId(), profileDTO);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody @Valid PasswordUpdateDTO passwordDTO) {
        log.info("修改密码: userId={}", UserContext.getUserId());
        return authService.updatePassword(UserContext.getUserId(), passwordDTO);
    }

    /**
     * 退出登录 - 将Token加入黑名单
     */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        log.info("用户退出登录: userId={}", UserContext.getUserId());
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
