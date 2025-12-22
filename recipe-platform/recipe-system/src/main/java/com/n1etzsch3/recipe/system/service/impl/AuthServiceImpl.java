package com.n1etzsch3.recipe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.common.constant.UserConstants;
import com.n1etzsch3.recipe.common.core.domain.LoginUser;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.common.utils.JwtUtils;
import com.n1etzsch3.recipe.system.domain.dto.LoginDTO;
import com.n1etzsch3.recipe.system.domain.dto.PasswordUpdateDTO;
import com.n1etzsch3.recipe.system.domain.dto.RegisterDTO;
import com.n1etzsch3.recipe.system.domain.dto.UserProfileDTO;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import com.n1etzsch3.recipe.system.service.AuthService;
import com.n1etzsch3.recipe.system.service.CaptchaService;
import com.n1etzsch3.recipe.framework.service.UserOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final UserOnlineService userOnlineService;

    @Override
    public Result<Map<String, Object>> login(LoginDTO loginDTO) {
        // 1. 验证验证码
        if (!captchaService.validateCaptcha(loginDTO.getCaptchaId(), loginDTO.getCaptchaCode())) {
            return Result.fail("验证码错误或已过期");
        }

        // 2. 查询用户
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername()));

        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 3. 禁止管理员通过普通接口登录
        if (UserConstants.ROLE_ADMIN.equals(user.getRole())) {
            return Result.fail("管理员请使用专用入口登录");
        }

        // 4. 校验密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.fail("密码错误");
        }

        // 5. 校验状态 (DISABLE = 1 表示被封禁)
        if (user.getStatus() != null && user.getStatus() == UserConstants.DISABLE) {
            return Result.fail("账号已被封禁");
        }

        // 6. 检查用户是否已在线
        if (userOnlineService.isOnline(user.getId())) {
            // 返回特殊状态码，提示前端需要确认
            Map<String, Object> data = new HashMap<>();
            data.put("requireConfirm", true);
            data.put("userId", user.getId());
            data.put("message", "该账号已在其他设备登录，是否强制登录？");
            return Result.result(data, 409, "账号已在其他设备登录");
        }

        // 7. 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());

        String token = JwtUtils.generateToken(claims);

        // 8. 构造返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar());

        return Result.ok(data, "登录成功");
    }

    @Override
    public Result<Map<String, Object>> forceLogin(LoginDTO loginDTO) {
        // 1. 验证验证码
        if (!captchaService.validateCaptcha(loginDTO.getCaptchaId(), loginDTO.getCaptchaCode())) {
            return Result.fail("验证码错误或已过期");
        }

        // 2. 查询用户
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername()));

        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 3. 禁止管理员通过普通接口登录
        if (UserConstants.ROLE_ADMIN.equals(user.getRole())) {
            return Result.fail("管理员请使用专用入口登录");
        }

        // 4. 校验密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.fail("密码错误");
        }

        // 5. 校验状态
        if (user.getStatus() != null && user.getStatus() == UserConstants.DISABLE) {
            return Result.fail("账号已被封禁");
        }

        // 6. 强制踢掉旧会话
        if (userOnlineService.isOnline(user.getId())) {
            userOnlineService.kickUser(user.getId());
        }

        // 7. 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());

        String token = JwtUtils.generateToken(claims);

        // 8. 构造返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar());

        return Result.ok(data, "登录成功");
    }

    @Override
    public Result<?> register(RegisterDTO registerDTO) {
        // 1. 验证验证码
        if (!captchaService.validateCaptcha(registerDTO.getCaptchaId(), registerDTO.getCaptchaCode())) {
            return Result.fail("验证码错误或已过期");
        }

        // 2. 检查用户名是否存在
        SysUser exists = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, registerDTO.getUsername()));
        if (exists != null) {
            return Result.fail("用户名已存在");
        }

        // 3. 创建用户
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());
        user.setPassword(encodedPassword);
        user.setNickname(registerDTO.getNickname());
        user.setRole(UserConstants.ROLE_MEMBER); // 默认角色
        user.setStatus(UserConstants.NORMAL); // 正常
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 4. 保存
        sysUserMapper.insert(user);

        return Result.ok("注册成功");
    }

    @Override
    public Result<?> getProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null)
            return Result.fail("用户不存在");

        // 脱敏处理，不返回密码
        user.setPassword(null);
        return Result.ok(user);
    }

    @Override
    public Result<?> updateProfile(Long userId, UserProfileDTO profileDTO) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null)
            return Result.fail("用户不存在");

        if (profileDTO.getNickname() != null)
            user.setNickname(profileDTO.getNickname());
        if (profileDTO.getAvatar() != null)
            user.setAvatar(profileDTO.getAvatar());
        if (profileDTO.getIntro() != null)
            user.setIntro(profileDTO.getIntro());

        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        return Result.ok("更新成功");
    }

    @Override
    public Result<?> updatePassword(Long userId, PasswordUpdateDTO passwordDTO) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null)
            return Result.fail("用户不存在");

        if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())) {
            return Result.fail("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        return Result.ok("密码修改成功");
    }
}
