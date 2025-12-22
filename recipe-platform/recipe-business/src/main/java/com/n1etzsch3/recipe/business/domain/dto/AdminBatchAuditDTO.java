package com.n1etzsch3.recipe.business.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AdminBatchAuditDTO {
    @NotEmpty(message = "菜谱ID不能为空")
    private List<Long> ids;

    @NotBlank(message = "操作类型不能为空")
    private String action;

    private String reason;
}
