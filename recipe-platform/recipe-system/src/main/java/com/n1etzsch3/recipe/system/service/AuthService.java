package com.n1etzsch3.recipe.system.service;

import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.domain.dto.ForceLoginDTO;
import com.n1etzsch3.recipe.system.domain.dto.LoginDTO;
import com.n1etzsch3.recipe.system.domain.dto.PasswordUpdateDTO;
import com.n1etzsch3.recipe.system.domain.dto.UserProfileDTO;
import com.n1etzsch3.recipe.system.domain.dto.RegisterDTO;

import java.util.Map;

public interface AuthService {

    /**
     * 登录
     */
    Result<Map<String, Object>> login(LoginDTO loginDTO);

    /**
     * 注册
     */
    Result<?> register(RegisterDTO registerDTO);

    /**
     * 获取当前用户信息
     */
    Result<?> getProfile(Long userId);

    /**
     * 更新个人资料
     */
    Result<?> updateProfile(Long userId, UserProfileDTO profileDTO);

    /**
     * 修改密码
     */
    Result<?> updatePassword(Long userId, PasswordUpdateDTO passwordDTO);

    /**
     * 强制登录（使用 forceLoginToken，踢掉已在线的旧会话）
     */
    Result<Map<String, Object>> forceLogin(ForceLoginDTO forceLoginDTO);
}
