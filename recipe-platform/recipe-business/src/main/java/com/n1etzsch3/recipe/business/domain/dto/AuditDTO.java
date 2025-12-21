package com.n1etzsch3.recipe.business.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AuditDTO {
    @NotNull(message = "菜谱ID不能为空")
    private Long recipeId;

    @NotBlank(message = "审核操作不能为空")
    @Pattern(regexp = "^(pass|reject)$", message = "审核操作只能是pass或reject")
    private String action;

    private String reason; // 拒绝时可填写原因
}
