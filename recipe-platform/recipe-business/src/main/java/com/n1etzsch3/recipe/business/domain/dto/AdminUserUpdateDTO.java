package com.n1etzsch3.recipe.business.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpdateDTO {
    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 20, message = "昵称长度必须为1-20个字符")
    private String nickname;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String intro;

    private String avatar;

    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "密码只能包含字母、数字和下划线")
    private String password;
}
