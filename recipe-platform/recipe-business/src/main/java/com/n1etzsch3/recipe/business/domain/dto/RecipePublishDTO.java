package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecipePublishDTO {
    private Long id; // 用于修改时传递ID

    @jakarta.validation.constraints.NotBlank(message = "标题不能为空")
    private String title;

    @jakarta.validation.constraints.NotNull(message = "分类不能为空")
    private Integer categoryId;

    private String coverImage;
    private String description;

    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotEmpty(message = "食材不能为空")
    private List<IngredientDTO> ingredients;

    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotEmpty(message = "步骤不能为空")
    private List<StepDTO> steps;

    @Data
    public static class IngredientDTO {
        @jakarta.validation.constraints.NotBlank(message = "食材名称不能为空")
        private String name;
        private String amount;
        private Integer sortOrder;
    }

    @Data
    public static class StepDTO {
        private Integer stepNo;

        @jakarta.validation.constraints.NotBlank(message = "步骤描述不能为空")
        private String description;

        private String imageUrl;
    }
}
