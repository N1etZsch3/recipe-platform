package com.n1etzsch3.recipe.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRoleEnum {
    ADMIN("admin", "管理员"),
    MEMBER("member", "普通用户");

    private final String code;
    private final String info;
}
