package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;

@Data
public class UserStatusDTO {
    // 0: normal(正常), 1: disabled(封禁)
    private Integer status;
}
