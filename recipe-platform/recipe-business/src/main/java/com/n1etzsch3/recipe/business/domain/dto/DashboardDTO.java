package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;

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
}
