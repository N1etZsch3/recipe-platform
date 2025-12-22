package com.n1etzsch3.recipe.business.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUserUpdateDTO {
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String intro;

    private String avatar;

    private String password;
}
