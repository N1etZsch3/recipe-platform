package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;

@Data
public class AuditDTO {
    private Long recipeId;
    // "pass" or "reject"
    private String action;
    private String reason;
}
