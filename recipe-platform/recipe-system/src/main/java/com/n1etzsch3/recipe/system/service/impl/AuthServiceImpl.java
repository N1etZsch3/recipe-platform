package com.n1etzsch3.recipe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.common.constant.UserConstants;
import com.n1etzsch3.recipe.common.core.domain.LoginUser;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.common.utils.JwtUtils;
import com.n1etzsch3.recipe.system.domain.dto.PasswordUpdateDTO;
import com.n1etzsch3.recipe.system.domain.dto.RegisterDTO;
import com.n1etzsch3.recipe.system.domain.dto.UserProfileDTO;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import com.n1etzsch3.recipe.system.service.AuthService;
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

    @Override
    public Result<Map<String, Object>> login(String username, String password) {
        // 1. 查询用户
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail("密码错误");
        }

        // 3. 校验状态 (DISABLE = 1 表示被封禁)
        if (user.getStatus() != null && user.getStatus() == UserConstants.DISABLE) {
            return Result.fail("账号已被封禁");
        }

        // 4. 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());

        String token = JwtUtils.generateToken(claims);

        // 5. 构造返回数据
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
        // 1. 检查用户名是否存在
        SysUser exists = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, registerDTO.getUsername()));
        if (exists != null) {
            return Result.fail("用户名已存在");
        }

        // 2. 创建用户
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());
        user.setPassword(encodedPassword);
        user.setNickname(registerDTO.getNickname());
        user.setRole(UserConstants.ROLE_MEMBER); // 默认角色
        user.setStatus(UserConstants.NORMAL); // 正常
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 3. 保存
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
