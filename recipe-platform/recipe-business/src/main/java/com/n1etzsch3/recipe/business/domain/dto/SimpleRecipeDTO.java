package com.n1etzsch3.recipe.business.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 简化版菜谱DTO（用于前端简化提交）
 */
@Data
public class SimpleRecipeDTO {

    private Long id; // 更新时使用

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    private String coverImage;

    @Size(max = 2000, message = "内容长度不能超过2000个字符")
    private String content;

    private String category; // 分类名称，将被转换为categoryId
}
