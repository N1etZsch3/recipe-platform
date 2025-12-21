package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.AuditDTO;
import com.n1etzsch3.recipe.business.domain.dto.CommentDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.DashboardDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.business.entity.RecipeCategory;
import com.n1etzsch3.recipe.business.entity.RecipeComment;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.mapper.RecipeCategoryMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeCommentMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeIngredientMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeStepMapper;
import com.n1etzsch3.recipe.business.service.AdminService;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.business.service.NotificationService;
import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.common.constant.RecipeConstants;
import com.n1etzsch3.recipe.common.constant.UserConstants;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.common.utils.JwtUtils;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeCategoryMapper categoryMapper;
    private final RecipeCommentMapper commentMapper;
    private final RecipeIngredientMapper ingredientMapper;
    private final RecipeStepMapper stepMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final AdminLogService adminLogService;

    // ================== Admin Login ==================

    @Override
    public Result<Map<String, Object>> adminLogin(String username, String password) {
        // 1. 查询用户
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 2. 验证是否为管理员 (支持超级管理员和普通管理员)
        if (!UserConstants.ROLE_ADMIN.equals(user.getRole())
                && !UserConstants.ROLE_COMMON_ADMIN.equals(user.getRole())) {
            return Result.fail("非管理员账号");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail("密码错误");
        }

        // 4. 校验状态 (DISABLE = 1 表示被封禁)
        if (user.getStatus() != null && user.getStatus() == UserConstants.DISABLE) {
            return Result.fail("账号已被封禁");
        }

        // 5. 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());

        String token = JwtUtils.generateToken(claims);

        // 6. 构造返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar());

        return Result.ok(data, "登录成功");
    }

    // ================== Dashboard ==================

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
                // Compatible with different DBs returning different types for count/month
                Integer m = Convert.toInt(map.get("month"));
                Long c = Convert.toLong(map.get("count"));
                if (m != null)
                    monthData.put(m, c);
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
        // 按数量降序排序
        categoryStats.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        dto.setCategoryStats(categoryStats);

        return Result.ok(dto);
    }

    // ================== Recipe Audit ==================

    @Override
    public Result<IPage<RecipeDetailDTO>> pageAuditRecipes(Integer page, Integer size) {
        Page<RecipeInfo> p = new Page<>(page, size);
        Page<RecipeInfo> resultPage = recipeInfoMapper.selectPage(p, new LambdaQueryWrapper<RecipeInfo>()
                .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PENDING)
                .orderByDesc(RecipeInfo::getCreateTime));

        return Result.ok(convertToDetailPage(resultPage));
    }

    @Override
    public Result<?> auditRecipe(AuditDTO auditDTO) {
        log.info("正在处理审核: recipeId={}, action={}, reason={}", auditDTO.getRecipeId(), auditDTO.getAction(),
                auditDTO.getReason());
        RecipeInfo recipe = recipeInfoMapper.selectById(auditDTO.getRecipeId());
        if (recipe == null)
            return Result.fail("菜谱不存在");

        if ("pass".equals(auditDTO.getAction())) {
            recipe.setStatus(RecipeConstants.STATUS_PUBLISHED);
            notificationService.sendRecipeApproved(recipe.getUserId(), recipe.getId(), recipe.getTitle());
            adminLogService.log("RECIPE_APPROVE", "recipe", recipe.getId(), recipe.getTitle(), null);
        } else if ("reject".equals(auditDTO.getAction())) {
            recipe.setStatus(RecipeConstants.STATUS_REJECTED);
            recipe.setRejectReason(auditDTO.getReason());
            notificationService.sendRecipeRejected(recipe.getUserId(), recipe.getId(),
                    recipe.getTitle(), auditDTO.getReason());
            adminLogService.log("RECIPE_REJECT", "recipe", recipe.getId(), recipe.getTitle(), auditDTO.getReason());
        } else {
            return Result.fail("未知操作");
        }
        recipe.setUpdateTime(LocalDateTime.now());
        recipeInfoMapper.updateById(recipe);

        return Result.ok("操作成功");
    }

    // ================== All Recipes Management ==================

    @Override
    public Result<IPage<RecipeDetailDTO>> pageAllRecipes(Integer page, Integer size, Integer status, String keyword) {
        Page<RecipeInfo> p = new Page<>(page, size);
        LambdaQueryWrapper<RecipeInfo> wrapper = new LambdaQueryWrapper<RecipeInfo>()
                .eq(status != null, RecipeInfo::getStatus, status)
                .like(StrUtil.isNotBlank(keyword), RecipeInfo::getTitle, keyword)
                .orderByDesc(RecipeInfo::getCreateTime);

        Page<RecipeInfo> resultPage = recipeInfoMapper.selectPage(p, wrapper);
        return Result.ok(convertToDetailPage(resultPage));
    }

    @Override
    @Transactional
    public Result<?> deleteRecipe(Long recipeId) {
        RecipeInfo recipe = recipeInfoMapper.selectById(recipeId);
        if (recipe == null) {
            return Result.fail("菜谱不存在");
        }

        // 删除关联数据
        ingredientMapper.delete(new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.RecipeIngredient>()
                .eq(com.n1etzsch3.recipe.business.entity.RecipeIngredient::getRecipeId, recipeId));
        stepMapper.delete(new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.RecipeStep>()
                .eq(com.n1etzsch3.recipe.business.entity.RecipeStep::getRecipeId, recipeId));
        commentMapper.delete(new LambdaQueryWrapper<RecipeComment>()
                .eq(RecipeComment::getRecipeId, recipeId));

        // 删除菜谱
        recipeInfoMapper.deleteById(recipeId);

        adminLogService.log("RECIPE_DELETE", "recipe", recipeId, recipe.getTitle(), null);
        log.info("管理员删除菜谱: id={}, title={}", recipeId, recipe.getTitle());
        return Result.ok("删除成功");
    }

    // ================== Category Management ==================

    @Override
    @Cacheable(value = CacheConstants.CACHE_CATEGORIES, key = "'all'")
    public Result<List<RecipeCategory>> listCategories() {
        List<RecipeCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<RecipeCategory>()
                .orderByAsc(RecipeCategory::getSortOrder)
                .orderByAsc(RecipeCategory::getId));
        return Result.ok(categories);
    }

    @Override
    @CacheEvict(value = CacheConstants.CACHE_CATEGORIES, key = "'all'")
    public Result<?> addCategory(RecipeCategory category) {
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        adminLogService.log("CATEGORY_ADD", "category", Long.valueOf(category.getId()), category.getName(), null);
        return Result.ok("添加成功");
    }

    @Override
    @CacheEvict(value = CacheConstants.CACHE_CATEGORIES, key = "'all'")
    public Result<?> updateCategory(Integer id, RecipeCategory category) {
        RecipeCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            return Result.fail("分类不存在");
        }
        existing.setName(category.getName());
        existing.setSortOrder(category.getSortOrder());
        categoryMapper.updateById(existing);
        adminLogService.log("CATEGORY_UPDATE", "category", Long.valueOf(id), existing.getName(), null);
        return Result.ok("修改成功");
    }

    @Override
    @CacheEvict(value = CacheConstants.CACHE_CATEGORIES, key = "'all'")
    public Result<?> deleteCategory(Integer id) {
        // 检查是否有菜谱使用该分类
        Long count = recipeInfoMapper.selectCount(new LambdaQueryWrapper<RecipeInfo>()
                .eq(RecipeInfo::getCategoryId, id));
        if (count > 0) {
            return Result.fail("该分类下存在菜谱，无法删除");
        }
        RecipeCategory category = categoryMapper.selectById(id);
        String categoryName = category != null ? category.getName() : "ID:" + id;
        categoryMapper.deleteById(id);
        adminLogService.log("CATEGORY_DELETE", "category", Long.valueOf(id), categoryName, null);
        return Result.ok("删除成功");
    }

    // ================== User Management ==================

    @Override
    public Result<IPage<UserDTO>> pageUsers(Integer page, Integer size, String keyword, String role, String sortBy) {
        Page<SysUser> p = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getRole, UserConstants.ROLE_ADMIN)
                .eq(StrUtil.isNotBlank(role), SysUser::getRole, role)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(SysUser::getNickname, keyword)
                        .or()
                        .like(SysUser::getUsername, keyword));

        if ("oldest".equals(sortBy)) {
            wrapper.orderByAsc(SysUser::getCreateTime);
        } else {
            wrapper.orderByDesc(SysUser::getCreateTime);
        }

        Page<SysUser> result = sysUserMapper.selectPage(p, wrapper);

        IPage<UserDTO> dtoPage = result.convert(user -> {
            UserDTO dto = new UserDTO();
            BeanUtil.copyProperties(user, dto);
            dto.setPassword(null);

            // 统计发布的菜谱数量
            Long count = recipeInfoMapper.selectCount(new LambdaQueryWrapper<RecipeInfo>()
                    .eq(RecipeInfo::getUserId, user.getId()));
            dto.setRecipeCount(count);
            return dto;
        });

        return Result.ok(dtoPage);
    }

    @Override
    public Result<?> addUser(SysUser user) {
        // 检查用户名是否存在
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.fail("用户名已存在");
        }

        // 禁止创建超级管理员
        if (UserConstants.ROLE_ADMIN.equals(user.getRole())) {
            return Result.fail("无法创建超级管理员");
        }

        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 密码加密
        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            return Result.fail("密码不能为空");
        }

        // 默认状态
        if (user.getStatus() == null) {
            user.setStatus(UserConstants.NORMAL);
        }
        // 如果没有头像，设置默认? 前端可能处理

        sysUserMapper.insert(user);
        adminLogService.log("USER_ADD", "user", user.getId(), user.getUsername(), null);
        return Result.ok("添加成功");
    }

    @Override
    public Result<?> updateUser(Long id, SysUser user) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            return Result.fail("用户不存在");
        }

        // 禁止操作系统超级管理员
        if (UserConstants.ROLE_ADMIN.equals(existing.getRole())) {
            return Result.fail("无法修改超级管理员信息");
        }

        // 禁止提升为超级管理员
        if (UserConstants.ROLE_ADMIN.equals(user.getRole())) {
            return Result.fail("无法设置为超级管理员角色");
        }

        existing.setNickname(user.getNickname());
        existing.setRole(user.getRole());
        existing.setIntro(user.getIntro());
        existing.setUpdateTime(LocalDateTime.now());

        // 更新头像 (允许为空字符串，表示删除头像)
        if (user.getAvatar() != null) {
            existing.setAvatar(user.getAvatar());
        }

        // 更新密码 (仅当不为空时)
        if (StrUtil.isNotBlank(user.getPassword())) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        sysUserMapper.updateById(existing);
        adminLogService.log("USER_UPDATE", "user", id, existing.getUsername(), null);
        return Result.ok("修改成功");
    }

    @Override
    public Result<?> updateUserStatus(Long userId, UserStatusDTO statusDTO) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null)
            return Result.fail("用户不存在");

        user.setStatus(statusDTO.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        String operationType = statusDTO.getStatus() == 1 ? "USER_BAN" : "USER_UNBAN";
        adminLogService.log(operationType, "user", userId, user.getNickname(), null);

        return Result.ok("状态修改成功");
    }

    @Override
    public Result<?> batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("未选择用户");
        }

        // 安全检查：防止操作超级管理员
        Long adminCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, ids)
                .eq(SysUser::getRole, UserConstants.ROLE_ADMIN));
        if (adminCount > 0) {
            return Result.fail("无法更改超级管理员状态");
        }

        SysUser update = new SysUser();
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());

        sysUserMapper.update(update, new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids));

        String operationType = status == 1 ? "USER_BATCH_BAN" : "USER_BATCH_UNBAN";
        adminLogService.log(operationType, "user", 0L, "Batch count: " + ids.size(), null);

        return Result.ok("批量操作成功");
    }

    // ================== Comment Management ==================

    @Override
    public Result<IPage<CommentDetailDTO>> pageComments(Integer page, Integer size, String keyword,
            Long userId, Long recipeId, String sortBy) {
        Page<RecipeComment> p = new Page<>(page, size);
        LambdaQueryWrapper<RecipeComment> wrapper = new LambdaQueryWrapper<RecipeComment>()
                .like(StrUtil.isNotBlank(keyword), RecipeComment::getContent, keyword)
                .eq(userId != null, RecipeComment::getUserId, userId)
                .eq(recipeId != null, RecipeComment::getRecipeId, recipeId);

        // 排序逻辑
        if ("oldest".equals(sortBy)) {
            wrapper.orderByAsc(RecipeComment::getCreateTime);
        } else {
            wrapper.orderByDesc(RecipeComment::getCreateTime); // 默认最新优先
        }

        Page<RecipeComment> resultPage = commentMapper.selectPage(p, wrapper);

        IPage<CommentDetailDTO> dtoPage = resultPage.convert(comment -> {
            CommentDetailDTO dto = new CommentDetailDTO();
            dto.setId(comment.getId());
            dto.setRecipeId(comment.getRecipeId());
            dto.setUserId(comment.getUserId());
            dto.setContent(comment.getContent());
            dto.setCreateTime(comment.getCreateTime());

            // 获取菜谱标题
            RecipeInfo recipe = recipeInfoMapper.selectById(comment.getRecipeId());
            if (recipe != null) {
                dto.setRecipeTitle(recipe.getTitle());
            }

            // 获取用户信息
            SysUser user = sysUserMapper.selectById(comment.getUserId());
            if (user != null) {
                dto.setUserNickname(user.getNickname());
                dto.setUserAvatar(user.getAvatar());
            }

            return dto;
        });

        return Result.ok(dtoPage);
    }

    @Override
    public Result<?> deleteComment(Long commentId) {
        RecipeComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        commentMapper.deleteById(commentId);
        log.info("管理员删除评论: id={}", commentId);
        return Result.ok("删除成功");
    }

    // ================== Private Helper Methods ==================

    private IPage<RecipeDetailDTO> convertToDetailPage(Page<RecipeInfo> resultPage) {
        return resultPage.convert(recipe -> {
            RecipeDetailDTO dto = new RecipeDetailDTO();
            BeanUtil.copyProperties(recipe, dto);

            // 获取分类名称
            RecipeCategory category = categoryMapper.selectById(recipe.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }

            // 获取作者信息
            SysUser author = sysUserMapper.selectById(recipe.getUserId());
            if (author != null) {
                dto.setAuthorId(author.getId());
                dto.setAuthorName(author.getNickname());
                dto.setAuthorAvatar(author.getAvatar());
            }
            return dto;
        });
    }
}
