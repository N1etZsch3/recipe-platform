package com.n1etzsch3.recipe.business.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RecipePublishDTO {
    private Long id; // 用于修改时传递ID

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotNull(message = "分类不能为空")
    private Integer categoryId;

    private String coverImage;
    private String description;

    @Valid
    @NotEmpty(message = "食材不能为空")
    private List<IngredientDTO> ingredients;

    @Valid
    @NotEmpty(message = "步骤不能为空")
    private List<StepDTO> steps;

    @Data
    public static class IngredientDTO {
        @NotBlank(message = "食材名称不能为空")
        private String name;
        private String amount;
        private Integer sortOrder;
    }

    @Data
    public static class StepDTO {
        private Integer stepNo;

        @NotBlank(message = "步骤描述不能为空")
        private String description;

        private String imageUrl;
    }
}
