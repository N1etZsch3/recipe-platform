package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;

@Data
public class UserStatusDTO {
    // 0: normal, 1: disabled
    private Integer status;
}
