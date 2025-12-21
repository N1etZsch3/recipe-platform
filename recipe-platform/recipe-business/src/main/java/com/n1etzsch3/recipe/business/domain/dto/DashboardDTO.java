package com.n1etzsch3.recipe.business.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    // ========== 新增用户相关统计 ==========

    // 月度用户增长 - 用于用户增长趋势图
    private List<MonthlyStatDTO> monthlyUsers;

    // 周活跃用户数（7天内有操作的用户）
    private Long weeklyActiveUsers;

    // 用户增长率（相比上月，百分比）
    private Double userGrowthRate;

    // 总浏览量（菜谱浏览总数）
    private Long totalViews;

    // 今日浏览量
    private Long todayViews;

    // 活跃用户列表（TOP 5）
    private List<TopUserDTO> topActiveUsers;

    // 最新动态列表
    private List<RecentActivityDTO> recentActivities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStatDTO {
        private Integer month;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatDTO {
        private String name;
        private Long count;
    }

    /**
     * 活跃用户DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopUserDTO {
        private Long id;
        private String nickname;
        private String avatar;
        private Long recipeCount;
        private Long commentCount;
        private Integer activityScore; // 活跃度分数 0-100
        private LocalDateTime registerTime;
    }

    /**
     * 最新动态DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDTO {
        private Long userId;
        private String userName;
        private String userAvatar;
        private String action; // "PUBLISH_RECIPE", "COMMENT", "REGISTER"
        private String actionText; // 显示文本
        private String targetTitle; // 菜谱名或内容摘要
        private Long targetId;
        private LocalDateTime time;
    }
}
