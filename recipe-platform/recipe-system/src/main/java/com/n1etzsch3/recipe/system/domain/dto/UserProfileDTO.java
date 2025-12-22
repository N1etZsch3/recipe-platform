package com.n1etzsch3.recipe.system.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileDTO {
    @Size(min = 1, max = 20, message = "昵称长度必须为1-20个字符")
    private String nickname;

    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatar;

    @Size(max = 200, message = "个人简介长度不能超过200个字符")
    private String intro;
}
