package com.n1etzsch3.recipe.business.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 菜谱DTO（用于前端提交）
 */
@Data
public class SimpleRecipeDTO {

    private Long id; // 更新时使用

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    private String coverImage;

    @Size(max = 500, message = "简介长度不能超过500个字符")
    private String description; // 菜谱简介/心得

    private String category; // 分类名称，将被转换为categoryId

    @Valid
    @NotEmpty(message = "用料不能为空")
    @Size(max = 50, message = "用料不能超过50项")
    private List<IngredientDTO> ingredients; // 用料列表

    @Valid
    @NotEmpty(message = "步骤不能为空")
    @Size(max = 50, message = "步骤不能超过50步")
    private List<StepDTO> steps; // 步骤列表

    /**
     * 用料DTO
     */
    @Data
    public static class IngredientDTO {
        @NotBlank(message = "食材名称不能为空")
        @Size(max = 50, message = "食材名称不能超过50个字符")
        private String name;

        @Size(max = 50, message = "用量不能超过50个字符")
        private String amount;

        private Integer sortOrder;
    }

    /**
     * 步骤DTO
     */
    @Data
    public static class StepDTO {
        private Integer stepNo;

        @NotBlank(message = "步骤描述不能为空")
        @Size(max = 1000, message = "步骤描述不能超过1000个字符")
        private String description;

        private String imageUrl;
    }
}
