package com.n1etzsch3.recipe.business.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCategoryDTO {
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Min(value = 0, message = "排序值非法")
    private Integer sortOrder;
}
