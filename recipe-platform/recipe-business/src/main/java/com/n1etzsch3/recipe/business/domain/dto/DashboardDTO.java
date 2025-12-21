package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardDTO {
    private Long totalUsers;
    private Long todayNewUsers;
    private Long totalRecipes;
    private Long pendingRecipes;
    private Long publishedRecipes;
    private Long totalComments;
    private Long todayComments;
    private Long totalCategories;

    // 月度菜谱统计 - 用于图表展示
    private List<MonthlyStatDTO> monthlyRecipes;

    // 分类统计 - 用于饼图展示
    private List<CategoryStatDTO> categoryStats;

    @Data
    public static class MonthlyStatDTO {
        private Integer month;
        private Long count;

        public MonthlyStatDTO(Integer month, Long count) {
            this.month = month;
            this.count = count;
        }
    }

    @Data
    public static class CategoryStatDTO {
        private String name;
        private Long count;

        public CategoryStatDTO(String name, Long count) {
            this.name = name;
            this.count = count;
        }
    }
}
