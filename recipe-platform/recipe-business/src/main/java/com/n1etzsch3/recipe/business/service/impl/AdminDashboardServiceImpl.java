package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.n1etzsch3.recipe.business.domain.dto.DashboardDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.entity.RecipeComment;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeCommentMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.service.AdminDashboardService;
import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.common.constant.RecipeConstants;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeCategoryMapper categoryMapper;
    private final RecipeCommentMapper commentMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Cacheable(value = CacheConstants.CACHE_DASHBOARD, key = "'stats'")
    public Result<DashboardDTO> getDashboard() {
        DashboardDTO dto = new DashboardDTO();

        // 用户统计
        dto.setTotalUsers(sysUserMapper.selectCount(null));
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        dto.setTodayNewUsers(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreateTime, todayStart)));

        // 菜谱统计
        dto.setTotalRecipes(recipeInfoMapper.selectCount(null));
        dto.setPendingRecipes(recipeInfoMapper.selectCount(new LambdaQueryWrapper<RecipeInfo>()
                .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PENDING)));
        dto.setPublishedRecipes(recipeInfoMapper.selectCount(new LambdaQueryWrapper<RecipeInfo>()
                .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PUBLISHED)));

        // 评论统计
        dto.setTotalComments(commentMapper.selectCount(null));
        dto.setTodayComments(commentMapper.selectCount(new LambdaQueryWrapper<RecipeComment>()
                .ge(RecipeComment::getCreateTime, todayStart)));

        // 分类统计
        dto.setTotalCategories(categoryMapper.selectCount(null));

        // 月度菜谱发布统计 (当前年份各月份, 使用聚合查询优化)
        int currentYear = LocalDate.now().getYear();
        QueryWrapper<RecipeInfo> monthlyWrapper = new QueryWrapper<>();
        monthlyWrapper.select("MONTH(create_time) as month", "COUNT(*) as count");
        monthlyWrapper.apply("YEAR(create_time) = {0}", currentYear);
        monthlyWrapper.groupBy("MONTH(create_time)");
        List<Map<String, Object>> monthlyList = recipeInfoMapper.selectMaps(monthlyWrapper);

        Map<Integer, Long> monthData = new HashMap<>();
        if (monthlyList != null) {
            for (Map<String, Object> map : monthlyList) {
                Integer m = Convert.toInt(map.get("month"));
                Long c = Convert.toLong(map.get("count"));
                if (m != null) {
                    monthData.put(m, c);
                }
            }
        }

        List<DashboardDTO.MonthlyStatDTO> monthlyStats = new java.util.ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            monthlyStats.add(new DashboardDTO.MonthlyStatDTO(month, monthData.getOrDefault(month, 0L)));
        }
        dto.setMonthlyRecipes(monthlyStats);

        // 分类菜谱分布统计 (使用聚合查询优化)
        QueryWrapper<RecipeInfo> catWrapper = new QueryWrapper<>();
        catWrapper.select("category_id", "COUNT(*) as count");
        catWrapper.isNotNull("category_id");
        catWrapper.groupBy("category_id");
        List<Map<String, Object>> catList = recipeInfoMapper.selectMaps(catWrapper);

        List<RecipeCategory> categories = categoryMapper.selectList(null);
        Map<Integer, String> categoryNames = categories.stream()
                .collect(java.util.stream.Collectors.toMap(RecipeCategory::getId, RecipeCategory::getName));

        List<DashboardDTO.CategoryStatDTO> categoryStats = new java.util.ArrayList<>();
        if (catList != null) {
            for (Map<String, Object> map : catList) {
                Integer cid = Convert.toInt(map.get("category_id"));
                Long count = Convert.toLong(map.get("count"));
                String name = categoryNames.get(cid);
                if (name != null) {
                    categoryStats.add(new DashboardDTO.CategoryStatDTO(name, count));
                }
            }
        }
        categoryStats.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        dto.setCategoryStats(categoryStats);

        // 月度用户增长统计
        QueryWrapper<SysUser> monthlyUserWrapper = new QueryWrapper<>();
        monthlyUserWrapper.select("MONTH(create_time) as month", "COUNT(*) as count");
        monthlyUserWrapper.apply("YEAR(create_time) = {0}", currentYear);
        monthlyUserWrapper.groupBy("MONTH(create_time)");
        List<Map<String, Object>> monthlyUserList = sysUserMapper.selectMaps(monthlyUserWrapper);

        Map<Integer, Long> monthUserData = new HashMap<>();
        if (monthlyUserList != null) {
            for (Map<String, Object> map : monthlyUserList) {
                Integer m = Convert.toInt(map.get("month"));
                Long c = Convert.toLong(map.get("count"));
                if (m != null) {
                    monthUserData.put(m, c);
                }
            }
        }

        List<DashboardDTO.MonthlyStatDTO> monthlyUserStats = new java.util.ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            monthlyUserStats.add(new DashboardDTO.MonthlyStatDTO(month, monthUserData.getOrDefault(month, 0L)));
        }
        dto.setMonthlyUsers(monthlyUserStats);

        // 周活跃用户数（7天内有发布菜谱或评论的用户）
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        QueryWrapper<RecipeInfo> weekRecipeWrapper = new QueryWrapper<>();
        weekRecipeWrapper.select("DISTINCT user_id");
        weekRecipeWrapper.ge("create_time", weekAgo);
        List<Map<String, Object>> weekRecipeUsers = recipeInfoMapper.selectMaps(weekRecipeWrapper);

        QueryWrapper<RecipeComment> weekCommentWrapper = new QueryWrapper<>();
        weekCommentWrapper.select("DISTINCT user_id");
        weekCommentWrapper.ge("create_time", weekAgo);
        List<Map<String, Object>> weekCommentUsers = commentMapper.selectMaps(weekCommentWrapper);

        Set<Long> activeUserIds = new HashSet<>();
        if (weekRecipeUsers != null) {
            weekRecipeUsers.forEach(m -> {
                Long uid = Convert.toLong(m.get("user_id"));
                if (uid != null) {
                    activeUserIds.add(uid);
                }
            });
        }
        if (weekCommentUsers != null) {
            weekCommentUsers.forEach(m -> {
                Long uid = Convert.toLong(m.get("user_id"));
                if (uid != null) {
                    activeUserIds.add(uid);
                }
            });
        }
        dto.setWeeklyActiveUsers((long) activeUserIds.size());

        // 用户增长率（本月 vs 上月）
        int currentMonth = LocalDate.now().getMonthValue();
        Long thisMonthUsers = monthUserData.getOrDefault(currentMonth, 0L);
        Long lastMonthUsers = monthUserData.getOrDefault(currentMonth > 1 ? currentMonth - 1 : 12, 0L);
        if (lastMonthUsers > 0) {
            dto.setUserGrowthRate(((double) (thisMonthUsers - lastMonthUsers) / lastMonthUsers) * 100);
        } else if (thisMonthUsers > 0) {
            dto.setUserGrowthRate(100.0);
        } else {
            dto.setUserGrowthRate(0.0);
        }

        // 总浏览量和今日浏览量（使用菜谱的 viewCount 字段求和）
        QueryWrapper<RecipeInfo> viewWrapper = new QueryWrapper<>();
        viewWrapper.select("IFNULL(SUM(view_count), 0) as total");
        List<Map<String, Object>> viewResult = recipeInfoMapper.selectMaps(viewWrapper);
        Long totalViews = 0L;
        if (viewResult != null && !viewResult.isEmpty()) {
            totalViews = Convert.toLong(viewResult.get(0).get("total"));
            if (totalViews == null) {
                totalViews = 0L;
            }
        }
        dto.setTotalViews(totalViews);
        // 今日浏览量暂时模拟（实际需要记录每日浏览日志）
        dto.setTodayViews(dto.getTodayNewUsers() * 3 + dto.getTodayComments() * 2);

        // 活跃用户列表（发布菜谱数量最多的前5名）
        QueryWrapper<RecipeInfo> topUserWrapper = new QueryWrapper<>();
        topUserWrapper.select("user_id", "COUNT(*) as recipe_count");
        topUserWrapper.groupBy("user_id");
        topUserWrapper.orderByDesc("recipe_count");
        topUserWrapper.last("LIMIT 5");
        List<Map<String, Object>> topUserRecipes = recipeInfoMapper.selectMaps(topUserWrapper);

        List<DashboardDTO.TopUserDTO> topUsers = new java.util.ArrayList<>();
        if (topUserRecipes != null && !topUserRecipes.isEmpty()) {
            List<Long> topUserIds = new java.util.ArrayList<>();
            Map<Long, Long> recipeCountMap = new HashMap<>();
            for (Map<String, Object> map : topUserRecipes) {
                Long userId = Convert.toLong(map.get("user_id"));
                Long recipeCount = Convert.toLong(map.get("recipe_count"));
                if (userId != null) {
                    topUserIds.add(userId);
                    recipeCountMap.put(userId, recipeCount != null ? recipeCount : 0L);
                }
            }

            Map<Long, SysUser> topUserMap = new HashMap<>();
            if (!topUserIds.isEmpty()) {
                List<SysUser> users = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getId, topUserIds));
                for (SysUser user : users) {
                    topUserMap.put(user.getId(), user);
                }
            }

            Map<Long, Long> commentCountMap = new HashMap<>();
            if (!topUserIds.isEmpty()) {
                QueryWrapper<RecipeComment> commentCountWrapper = new QueryWrapper<>();
                commentCountWrapper.select("user_id", "COUNT(*) as cnt");
                commentCountWrapper.in("user_id", topUserIds);
                commentCountWrapper.groupBy("user_id");
                List<Map<String, Object>> commentCounts = commentMapper.selectMaps(commentCountWrapper);
                if (commentCounts != null) {
                    for (Map<String, Object> row : commentCounts) {
                        Long uid = Convert.toLong(row.get("user_id"));
                        Long cnt = Convert.toLong(row.get("cnt"));
                        if (uid != null) {
                            commentCountMap.put(uid, cnt != null ? cnt : 0L);
                        }
                    }
                }
            }

            for (Long userId : topUserIds) {
                SysUser user = topUserMap.get(userId);
                if (user != null) {
                    Long recipeCount = recipeCountMap.getOrDefault(userId, 0L);
                    Long commentCount = commentCountMap.getOrDefault(userId, 0L);
                    int score = Math.min(100, (int) (recipeCount * 10 + commentCount * 2));
                    topUsers.add(new DashboardDTO.TopUserDTO(
                            user.getId(),
                            user.getNickname(),
                            user.getAvatar(),
                            recipeCount,
                            commentCount,
                            score,
                            user.getCreateTime()));
                }
            }
        }
        dto.setTopActiveUsers(topUsers);

        // 最新动态（最近的菜谱发布和评论，取10条）
        List<DashboardDTO.RecentActivityDTO> activities = new java.util.ArrayList<>();

        // 最新发布的菜谱
        List<RecipeInfo> recentRecipes = recipeInfoMapper.selectList(new LambdaQueryWrapper<RecipeInfo>()
                .orderByDesc(RecipeInfo::getCreateTime)
                .last("LIMIT 5"));
        Map<Long, SysUser> recentRecipeAuthorMap = new HashMap<>();
        if (recentRecipes != null && !recentRecipes.isEmpty()) {
            Set<Long> authorIds = new HashSet<>();
            for (RecipeInfo recipe : recentRecipes) {
                if (recipe.getUserId() != null) {
                    authorIds.add(recipe.getUserId());
                }
            }
            if (!authorIds.isEmpty()) {
                List<SysUser> authors = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getId, authorIds));
                for (SysUser author : authors) {
                    recentRecipeAuthorMap.put(author.getId(), author);
                }
            }
        }
        for (RecipeInfo recipe : recentRecipes) {
            SysUser author = recentRecipeAuthorMap.get(recipe.getUserId());
            activities.add(new DashboardDTO.RecentActivityDTO(
                    recipe.getUserId(),
                    author != null ? author.getNickname() : "未知用户",
                    author != null ? author.getAvatar() : null,
                    "PUBLISH_RECIPE",
                    "发布了菜谱",
                    recipe.getTitle(),
                    recipe.getId(),
                    recipe.getCreateTime()));
        }

        // 最新评论
        List<RecipeComment> recentComments = commentMapper.selectList(new LambdaQueryWrapper<RecipeComment>()
                .orderByDesc(RecipeComment::getCreateTime)
                .last("LIMIT 5"));
        Map<Long, SysUser> recentCommenterMap = new HashMap<>();
        Map<Long, RecipeInfo> recentCommentRecipeMap = new HashMap<>();
        if (recentComments != null && !recentComments.isEmpty()) {
            Set<Long> commenterIds = new HashSet<>();
            Set<Long> recipeIds = new HashSet<>();
            for (RecipeComment comment : recentComments) {
                if (comment.getUserId() != null) {
                    commenterIds.add(comment.getUserId());
                }
                if (comment.getRecipeId() != null) {
                    recipeIds.add(comment.getRecipeId());
                }
            }
            if (!commenterIds.isEmpty()) {
                List<SysUser> commenters = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().in(SysUser::getId, commenterIds));
                for (SysUser commenter : commenters) {
                    recentCommenterMap.put(commenter.getId(), commenter);
                }
            }
            if (!recipeIds.isEmpty()) {
                List<RecipeInfo> recipes = recipeInfoMapper.selectList(
                        new LambdaQueryWrapper<RecipeInfo>().in(RecipeInfo::getId, recipeIds));
                for (RecipeInfo recipe : recipes) {
                    recentCommentRecipeMap.put(recipe.getId(), recipe);
                }
            }
        }
        for (RecipeComment comment : recentComments) {
            SysUser commenter = recentCommenterMap.get(comment.getUserId());
            RecipeInfo recipe = recentCommentRecipeMap.get(comment.getRecipeId());
            String targetTitle = recipe != null ? recipe.getTitle() : "某个菜谱";
            activities.add(new DashboardDTO.RecentActivityDTO(
                    comment.getUserId(),
                    commenter != null ? commenter.getNickname() : "未知用户",
                    commenter != null ? commenter.getAvatar() : null,
                    "COMMENT",
                    "评论了",
                    targetTitle,
                    comment.getRecipeId(),
                    comment.getCreateTime()));
        }

        // 按时间排序，取最新10条
        activities.sort((a, b) -> b.getTime().compareTo(a.getTime()));
        if (activities.size() > 10) {
            activities = activities.subList(0, 10);
        }
        dto.setRecentActivities(activities);

        return Result.ok(dto);
    }
}
