package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO;
import com.n1etzsch3.recipe.business.domain.query.RecipePageQuery;
import com.n1etzsch3.recipe.business.entity.*;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeIngredientMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeStepMapper;
import com.n1etzsch3.recipe.business.service.CategoryService;
import com.n1etzsch3.recipe.business.service.NotificationService;
import com.n1etzsch3.recipe.business.service.RecipeService;
import com.n1etzsch3.recipe.common.constant.UserConstants;
import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.LoginUser;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.common.constant.RecipeConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl extends ServiceImpl<RecipeInfoMapper, RecipeInfo> implements RecipeService {

    private final RecipeIngredientMapper ingredientMapper;
    private final RecipeStepMapper stepMapper;
    private final SysUserMapper sysUserMapper;
    private final com.n1etzsch3.recipe.business.mapper.RecipeCommentMapper commentMapper;
    private final com.n1etzsch3.recipe.business.mapper.UserFavoriteMapper favoriteMapper;
    private final com.n1etzsch3.recipe.business.mapper.UserFollowMapper followMapper;
    private final CategoryService categoryService;
    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> publishRecipe(RecipePublishDTO publishDTO) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 正在发布菜谱: {}", userId, publishDTO.getTitle());
        if (userId == null) {
            return Result.fail("未登录");
        }

        // 检查用户待审核菜谱数量限制
        long pendingCount = this.count(new LambdaQueryWrapper<RecipeInfo>()
                .eq(RecipeInfo::getUserId, userId)
                .in(RecipeInfo::getStatus,
                        RecipeConstants.STATUS_PENDING,
                        RecipeConstants.STATUS_PROCESSING));
        if (pendingCount >= RecipeConstants.MAX_PENDING_RECIPES) {
            return Result.fail("您有太多待审核的菜谱（最多 " + RecipeConstants.MAX_PENDING_RECIPES + " 个），请等待审核后再发布");
        }

        // 1. 保存基本信息
        RecipeInfo recipe = new RecipeInfo();
        BeanUtil.copyProperties(publishDTO, recipe);
        recipe.setUserId(userId);
        recipe.setStatus(RecipeConstants.STATUS_PROCESSING); // 处理中（队列处理阶段）
        recipe.setViewCount(0);
        recipe.setCreateTime(LocalDateTime.now());
        recipe.setUpdateTime(LocalDateTime.now());

        this.save(recipe);

        Long recipeId = recipe.getId();

        // 2. 保存食材
        if (CollUtil.isNotEmpty(publishDTO.getIngredients())) {
            List<RecipeIngredient> ingredients = publishDTO.getIngredients().stream().map(item -> {
                RecipeIngredient entity = new RecipeIngredient();
                BeanUtil.copyProperties(item, entity);
                entity.setRecipeId(recipeId);
                return entity;
            }).collect(Collectors.toList());
            ingredients.forEach(ingredientMapper::insert);
        }

        // 3. 保存步骤
        if (CollUtil.isNotEmpty(publishDTO.getSteps())) {
            List<RecipeStep> steps = publishDTO.getSteps().stream().map(item -> {
                RecipeStep entity = new RecipeStep();
                BeanUtil.copyProperties(item, entity);
                entity.setRecipeId(recipeId);
                return entity;
            }).toList();
            steps.forEach(stepMapper::insert);
        }

        // 4. 写入 Redis Stream 队列，异步处理
        try {
            stringRedisTemplate.opsForStream().add(
                    CacheConstants.STREAM_RECIPE_PUBLISH,
                    Map.of(
                            "recipeId", recipeId.toString(),
                            "userId", userId.toString(),
                            "timestamp", String.valueOf(System.currentTimeMillis())));
            log.info("菜谱已提交到处理队列: recipeId={}, title={}", recipeId, publishDTO.getTitle());
        } catch (Exception e) {
            // 队列写入失败时，回退到直接审核模式
            log.warn("写入队列失败，回退到直接审核模式: {}", e.getMessage());
            recipe.setStatus(RecipeConstants.STATUS_PENDING);
            this.updateById(recipe);

            SysUser author = sysUserMapper.selectById(userId);
            String authorName = author != null ? author.getNickname() : "用户" + userId;
            notificationService.sendNewRecipePending(recipeId, publishDTO.getTitle(), userId, authorName,
                    recipe.getCoverImage());
        }

        return Result.ok(Map.of(
                "recipeId", recipeId,
                "message", "已提交，正在处理中..."));
    }

    @Override
    public Result<RecipeDetailDTO> getRecipeDetail(Long id) {
        log.info("查询菜谱详情: {}", id);
        // 1. 获取基本信息
        RecipeInfo recipe = this.getById(id);
        if (recipe == null) {
            return Result.fail("菜谱不存在");
        }

        LoginUser loginUser = UserContext.get();
        Long currentUserId = loginUser != null ? loginUser.getId() : null;
        boolean isAdmin = loginUser != null &&
                (UserConstants.ROLE_ADMIN.equals(loginUser.getRole()) ||
                        UserConstants.ROLE_COMMON_ADMIN.equals(loginUser.getRole()));
        boolean isOwner = currentUserId != null && currentUserId.equals(recipe.getUserId());
        if (!Integer.valueOf(RecipeConstants.STATUS_PUBLISHED).equals(recipe.getStatus()) && !isOwner && !isAdmin) {
            return Result.fail("菜谱不存在");
        }

        // 2. 组装 DTO
        RecipeDetailDTO detailDTO = new RecipeDetailDTO();
        BeanUtil.copyProperties(recipe, detailDTO);

        // 3. 获取作者信息
        SysUser author = sysUserMapper.selectById(recipe.getUserId());
        if (author != null) {
            detailDTO.setAuthorName(author.getNickname());
            detailDTO.setAuthorAvatar(author.getAvatar());
        }

        // 4. 获取食材
        // 如果未维护食材表，这里可能为空列表
        List<RecipeIngredient> ingredients = ingredientMapper.selectList(new LambdaQueryWrapper<RecipeIngredient>()
                .eq(RecipeIngredient::getRecipeId, id)
                .orderByAsc(RecipeIngredient::getSortOrder));
        detailDTO.setIngredients(ingredients);

        // 5. 获取步骤
        // 如果未维护步骤表，这里可能为空列表
        List<RecipeStep> steps = stepMapper.selectList(new LambdaQueryWrapper<RecipeStep>()
                .eq(RecipeStep::getRecipeId, id)
                .orderByAsc(RecipeStep::getStepNo));
        detailDTO.setSteps(steps);

        // 6. 增加浏览量（原子更新，避免并发丢失）
        int currentViewCount = recipe.getViewCount() != null ? recipe.getViewCount() : 0;
        int nextViewCount = currentViewCount + 1;
        LambdaUpdateWrapper<RecipeInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(RecipeInfo::getId, id)
                .setSql("view_count = COALESCE(view_count, 0) + 1");
        this.baseMapper.update(null, updateWrapper);
        detailDTO.setViewCount(nextViewCount);

        // 7. 检查当前用户是否收藏
        if (currentUserId != null) {
            Long isFav = favoriteMapper.selectCount(
                    new LambdaQueryWrapper<UserFavorite>()
                            .eq(UserFavorite::getRecipeId, id)
                            .eq(UserFavorite::getUserId, currentUserId));
            detailDTO.setIsFavorite(isFav != null && isFav > 0);

            // 检查是否关注作者
            Long isFollow = followMapper.selectCount(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getFollowerId, currentUserId)
                            .eq(UserFollow::getFollowedId, recipe.getUserId()));
            detailDTO.setIsFollow(isFollow != null && isFollow > 0);
        } else {
            detailDTO.setIsFavorite(false);
            detailDTO.setIsFollow(false);
        }

        return Result.ok(detailDTO);
    }

    @Override
    public Result<IPage<RecipeDetailDTO>> pageRecipes(RecipePageQuery query) {
        log.info("分页查询菜谱: page={}, size={}, keyword={}, authorId={}, status={}",
                query.getPage(), query.getSize(), query.getKeyword(), query.getAuthorId(), query.getStatus());
        Page<RecipeInfo> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<RecipeInfo> wrapper = new LambdaQueryWrapper<>();

        // 1. 状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(RecipeInfo::getStatus, query.getStatus());
        } else if (query.getAuthorId() == null) {
            // 默认：未指定作者/状态时只显示已发布
            wrapper.eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PUBLISHED);
        }

        // 2. 作者筛选
        if (query.getAuthorId() != null) {
            wrapper.eq(RecipeInfo::getUserId, query.getAuthorId());
        }

        // 3. 分类筛选
        if (query.getCategoryId() != null) {
            wrapper.eq(RecipeInfo::getCategoryId, query.getCategoryId());
        }

        // 4. 关键词筛选
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.like(RecipeInfo::getTitle, query.getKeyword());
        }

        // 5. 排序
        if (StringUtils.hasText(query.getSort()) && RecipeConstants.SORT_HOT.equals(query.getSort())) {
            wrapper.orderByDesc(RecipeInfo::getViewCount);
        } else {
            wrapper.orderByDesc(RecipeInfo::getCreateTime);
        }

        Page<RecipeInfo> resultPage = this.page(page, wrapper);
        List<RecipeInfo> recipes = resultPage.getRecords();

        if (recipes.isEmpty()) {
            return Result.ok(resultPage.convert(r -> new RecipeDetailDTO()));
        }

        // === 批量查询优化 ===

        // 收集所有ID
        List<Long> recipeIds = recipes.stream().map(RecipeInfo::getId).collect(Collectors.toList());
        List<Long> authorIds = recipes.stream().map(RecipeInfo::getUserId).distinct().collect(Collectors.toList());
        Long currentUserId = com.n1etzsch3.recipe.common.context.UserContext.getUserId();

        // 1. 批量查询作者
        java.util.Map<Long, SysUser> authorMap = new java.util.HashMap<>();
        if (!authorIds.isEmpty()) {
            List<SysUser> authors = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().in(SysUser::getId, authorIds));
            authors.forEach(a -> authorMap.put(a.getId(), a));
        }

        // 2. 批量查询评论计数
        java.util.Map<Long, Long> commentCountMap = new java.util.HashMap<>();
        List<java.util.Map<String, Object>> commentCounts = commentMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.n1etzsch3.recipe.business.entity.RecipeComment>()
                        .select("recipe_id", "count(*) as cnt")
                        .in("recipe_id", recipeIds)
                        .groupBy("recipe_id"));
        for (java.util.Map<String, Object> row : commentCounts) {
            Long rid = ((Number) row.get("recipe_id")).longValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            commentCountMap.put(rid, cnt);
        }

        // 3. 批量查询收藏计数
        java.util.Map<Long, Long> favoriteCountMap = new java.util.HashMap<>();
        List<java.util.Map<String, Object>> favoriteCounts = favoriteMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.n1etzsch3.recipe.business.entity.UserFavorite>()
                        .select("recipe_id", "count(*) as cnt")
                        .in("recipe_id", recipeIds)
                        .groupBy("recipe_id"));
        for (java.util.Map<String, Object> row : favoriteCounts) {
            Long rid = ((Number) row.get("recipe_id")).longValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            favoriteCountMap.put(rid, cnt);
        }

        // 4. 批量查询当前用户的收藏状态
        java.util.Set<Long> userFavoriteRecipeIds = new java.util.HashSet<>();
        if (currentUserId != null && !recipeIds.isEmpty()) {
            List<com.n1etzsch3.recipe.business.entity.UserFavorite> userFavorites = favoriteMapper.selectList(
                    new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.UserFavorite>()
                            .eq(com.n1etzsch3.recipe.business.entity.UserFavorite::getUserId, currentUserId)
                            .in(com.n1etzsch3.recipe.business.entity.UserFavorite::getRecipeId, recipeIds));
            userFavorites.forEach(f -> userFavoriteRecipeIds.add(f.getRecipeId()));
        }

        // 转换为 DTO Page（使用批量查询的数据）
        IPage<RecipeDetailDTO> dtoPage = resultPage.convert(recipe -> {
            RecipeDetailDTO dto = new RecipeDetailDTO();
            BeanUtil.copyProperties(recipe, dto);

            // 作者信息（从映射获取）
            SysUser author = authorMap.get(recipe.getUserId());
            if (author != null) {
                dto.setAuthorName(author.getNickname());
                dto.setAuthorAvatar(author.getAvatar());
            }

            // 评论数（从映射获取）
            dto.setCommentCount(commentCountMap.getOrDefault(recipe.getId(), 0L).intValue());

            // 收藏数（从映射获取）
            dto.setFavoriteCount(favoriteCountMap.getOrDefault(recipe.getId(), 0L).intValue());

            // 分类名称映射
            dto.setCategoryName(mapCategoryIdToName(recipe.getCategoryId()));

            // 是否收藏（从集合获取）
            dto.setIsFavorite(userFavoriteRecipeIds.contains(recipe.getId()));

            return dto;
        });

        return Result.ok(dtoPage);
    }

    // 分类ID映射到名称
    private String mapCategoryIdToName(Integer categoryId) {
        return categoryService.getNameById(categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateRecipe(RecipePublishDTO publishDTO) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 更新菜谱: {}", userId, publishDTO.getId());
        RecipeInfo recipe = this.getById(publishDTO.getId());
        if (recipe == null)
            return Result.fail("菜谱不存在");
        if (!recipe.getUserId().equals(userId))
            return Result.fail("无权修改");

        // 只有 待审核 和 驳回 可以修改
        if (recipe.getStatus() == RecipeConstants.STATUS_PUBLISHED)
            return Result.fail("已发布菜谱无法直接修改，请申请下架");

        BeanUtil.copyProperties(publishDTO, recipe);
        recipe.setStatus(RecipeConstants.STATUS_PENDING); // 修改后重新审核
        this.updateById(recipe);

        // 删除旧的 steps/ingredients 重新插入
        ingredientMapper
                .delete(new LambdaQueryWrapper<RecipeIngredient>().eq(RecipeIngredient::getRecipeId, recipe.getId()));
        stepMapper.delete(new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, recipe.getId()));

        // 重新插入逻辑 (复用 publish 的部分逻辑，这里为了简洁先省略提取 verify)
        Long recipeId = recipe.getId();
        if (CollUtil.isNotEmpty(publishDTO.getIngredients())) {
            publishDTO.getIngredients().forEach(item -> {
                RecipeIngredient entity = new RecipeIngredient();
                BeanUtil.copyProperties(item, entity);
                entity.setRecipeId(recipeId);
                ingredientMapper.insert(entity);
            });
        }
        if (CollUtil.isNotEmpty(publishDTO.getSteps())) {
            publishDTO.getSteps().forEach(item -> {
                RecipeStep entity = new RecipeStep();
                BeanUtil.copyProperties(item, entity);
                entity.setRecipeId(recipeId);
                stepMapper.insert(entity);
            });
        }

        // 发送待审核通知
        SysUser author = sysUserMapper.selectById(userId);
        String authorName = author != null ? author.getNickname() : "用户" + userId;
        notificationService.sendNewRecipePending(recipeId, publishDTO.getTitle(), userId, authorName,
                recipe.getCoverImage());

        return Result.ok("修改成功，请等待审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteRecipe(Long id) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 删除菜谱: {}", userId, id);
        RecipeInfo recipe = this.getById(id);
        if (recipe == null)
            return Result.fail("菜谱不存在");
        if (!recipe.getUserId().equals(userId))
            return Result.fail("无权删除");

        // 如果是待审核状态，通知管理员移除
        boolean checkPending = recipe.getStatus() == RecipeConstants.STATUS_PENDING;

        this.removeById(id);
        // 级联删除 steps ingredients?
        ingredientMapper.delete(new LambdaQueryWrapper<RecipeIngredient>().eq(RecipeIngredient::getRecipeId, id));
        stepMapper.delete(new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, id));

        if (checkPending) {
            SysUser author = sysUserMapper.selectById(userId);
            String authorName = author != null ? author.getNickname() : "用户" + userId;
            notificationService.sendRecipeWithdrawn(id, recipe.getTitle(), userId, authorName);
        }

        return Result.ok("删除成功");
    }

    @Override
    public Result<?> unpublishRecipe(Long id) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 下架菜谱: {}", userId, id);
        RecipeInfo recipe = this.getById(id);
        if (recipe == null)
            return Result.fail("菜谱不存在");
        if (!recipe.getUserId().equals(userId))
            return Result.fail("无权操作");

        // 只有已发布状态才能下架
        if (recipe.getStatus() != RecipeConstants.STATUS_PUBLISHED) {
            return Result.fail("只有已发布的菜谱才能下架");
        }

        // 将状态改为待审核
        recipe.setStatus(RecipeConstants.STATUS_PENDING);
        this.updateById(recipe);

        // 下架变成待审核，也应该通知管理员
        SysUser author = sysUserMapper.selectById(userId);
        String authorName = author != null ? author.getNickname() : "用户" + userId;
        notificationService.sendNewRecipePending(id, recipe.getTitle(), userId, authorName, recipe.getCoverImage());

        return Result.ok("下架成功，您现在可以编辑菜谱了");
    }

    @Override
    public Result<?> withdrawRecipe(Long id) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 撤销发布菜谱: {}", userId, id);
        RecipeInfo recipe = this.getById(id);
        if (recipe == null) {
            return Result.fail("菜谱不存在");
        }
        if (!recipe.getUserId().equals(userId)) {
            return Result.fail("无权操作");
        }

        // 只有待审核或处理中状态才能撤销
        if (recipe.getStatus() != RecipeConstants.STATUS_PENDING
                && recipe.getStatus() != RecipeConstants.STATUS_PROCESSING) {
            return Result.fail("只有待审核的菜谱才能撤销发布");
        }

        // 将状态改为草稿
        recipe.setStatus(RecipeConstants.STATUS_DRAFT);
        recipe.setUpdateTime(LocalDateTime.now());
        this.updateById(recipe);

        // 发送撤销通知
        SysUser author = sysUserMapper.selectById(userId);
        String authorName = author != null ? author.getNickname() : "用户" + userId;
        notificationService.sendRecipeWithdrawn(id, recipe.getTitle(), userId, authorName);

        return Result.ok("已撤销发布，菜谱已保存为草稿");
    }
}
