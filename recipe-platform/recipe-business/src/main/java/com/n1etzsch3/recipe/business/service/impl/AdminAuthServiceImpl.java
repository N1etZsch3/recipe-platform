package com.n1etzsch3.recipe.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.business.service.AdminAuthService;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.common.constant.UserConstants;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.common.utils.JwtUtils;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminLogService adminLogService;

    @Override
    public Result<Map<String, Object>> adminLogin(String username, String password) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            // 记录登录失败日志：账号不存在
            adminLogService.logLogin(null, username, false, "账号不存在");
            return Result.fail("账号或密码错误");
        }

        if (!UserConstants.ROLE_ADMIN.equals(user.getRole())
                && !UserConstants.ROLE_COMMON_ADMIN.equals(user.getRole())) {
            // 记录登录失败日志：非管理员账号
            adminLogService.logLogin(user.getId(), username, false, "非管理员账号尝试登录管理后台");
            return Result.fail("非管理员账号");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 记录登录失败日志：密码错误
            adminLogService.logLogin(user.getId(), username, false, "密码错误");
            return Result.fail("账号或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == UserConstants.DISABLE) {
            // 记录登录失败日志：账号被封禁
            adminLogService.logLogin(user.getId(), username, false, "账号已被封禁");
            return Result.fail("账号已被封禁");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());

        String token = JwtUtils.generateToken(claims);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar());

        // 记录登录成功日志
        adminLogService.logLogin(user.getId(), username, true, "登录成功");

        return Result.ok(data, "登录成功");
    }
}
