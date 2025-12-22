package com.n1etzsch3.recipe.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 强制登录请求 DTO
 * 用于用户确认强制登录时，不需要验证码
 */
@Data
public class ForceLoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "强制登录令牌不能为空")
    private String forceLoginToken;
}
