package com.n1etzsch3.recipe.system.domain.dto;

import lombok.Data;

@Data
public class UserProfileDTO {
    private String nickname;
    private String avatar;
    private String intro;
}
