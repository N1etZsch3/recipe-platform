package com.n1etzsch3.recipe.business.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内容验证结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /** 是否通过验证 */
    private boolean passed;

    /** 失败原因（通过时为 null） */
    private String reason;

    public static ValidationResult pass() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult fail(String reason) {
        return new ValidationResult(false, reason);
    }
}
