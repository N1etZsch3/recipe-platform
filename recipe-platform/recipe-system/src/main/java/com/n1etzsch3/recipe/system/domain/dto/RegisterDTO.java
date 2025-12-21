package com.n1etzsch3.recipe.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
