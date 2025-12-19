package com.n1etzsch3.recipe.business.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecipeDetailVO {
    private Long id;
    private String title;
    private String coverImage;
    private String description;
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer categoryId;
    private String categoryName;
    private Integer viewCount;
    private LocalDateTime createTime;

    private List<IngredientVO> ingredients;
    private List<StepVO> steps;

    @Data
    public static class IngredientVO {
        private String name;
        private String amount;
    }

    @Data
    public static class StepVO {
        private Integer stepNo;
        private String description;
        private String imageUrl;
    }
}
