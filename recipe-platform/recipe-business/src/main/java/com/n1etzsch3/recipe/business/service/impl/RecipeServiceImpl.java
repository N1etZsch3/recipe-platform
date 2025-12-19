package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.n1etzsch3.recipe.business.domain.dto.RecipeDetailDTO;
import com.n1etzsch3.recipe.business.domain.dto.RecipePublishDTO;
import com.n1etzsch3.recipe.business.domain.query.RecipePageQuery;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.RecipeIngredient;
import com.n1etzsch3.recipe.business.entity.RecipeStep;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeIngredientMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeStepMapper;
import com.n1etzsch3.recipe.business.service.RecipeService;
import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.n1etzsch3.recipe.common.constant.RecipeConstants;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> publishRecipe(RecipePublishDTO publishDTO) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 正在发布菜谱: {}", userId, publishDTO.getTitle());
        if (userId == null) {
            return Result.fail("未登录");
        }

        // 1. 保存基本信息
        RecipeInfo recipe = new RecipeInfo();
        BeanUtil.copyProperties(publishDTO, recipe);
        recipe.setUserId(userId);
        recipe.setStatus(RecipeConstants.STATUS_PENDING); // 待审核
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
            // 批量保存食材? MyBatisPlus ServiceImpl 没有直接 batch save different entity?
            // 这里为了简单，且数量不多，直接循环 insert 或者用 mapper
            ingredients.forEach(ingredientMapper::insert);
        }

        // 3. 保存步骤
        if (CollUtil.isNotEmpty(publishDTO.getSteps())) {
            List<RecipeStep> steps = publishDTO.getSteps().stream().map(item -> {
                RecipeStep entity = new RecipeStep();
                BeanUtil.copyProperties(item, entity);
                entity.setRecipeId(recipeId);
                return entity;
            }).collect(Collectors.toList());
            steps.forEach(stepMapper::insert);
        }

        return Result.ok("发布成功，等待审核");
    }

    @Override
    public Result<RecipeDetailDTO> getRecipeDetail(Long id) {
        log.info("查询菜谱详情: {}", id);
        // 1. 获取基本信息
        RecipeInfo recipe = this.getById(id);
        if (recipe == null) {
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
        List<RecipeIngredient> ingredients = ingredientMapper.selectList(new LambdaQueryWrapper<RecipeIngredient>()
                .eq(RecipeIngredient::getRecipeId, id)
                .orderByAsc(RecipeIngredient::getSortOrder));
        detailDTO.setIngredients(ingredients);

        // 5. 获取步骤
        List<RecipeStep> steps = stepMapper.selectList(new LambdaQueryWrapper<RecipeStep>()
                .eq(RecipeStep::getRecipeId, id)
                .orderByAsc(RecipeStep::getStepNo));
        detailDTO.setSteps(steps);

        // 6. TODO: 增加浏览量 (异步优化)
        recipe.setViewCount(recipe.getViewCount() + 1);
        this.updateById(recipe);

        // 7. 检查当前用户是否收藏
        Long currentUserId = com.n1etzsch3.recipe.common.context.UserContext.getUserId();
        if (currentUserId != null) {
            Long isFav = favoriteMapper.selectCount(
                    new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.UserFavorite>()
                            .eq(com.n1etzsch3.recipe.business.entity.UserFavorite::getRecipeId, id)
                            .eq(com.n1etzsch3.recipe.business.entity.UserFavorite::getUserId, currentUserId));
            detailDTO.setIsFavorite(isFav != null && isFav > 0);

            // 检查是否关注作者
            Long isFollow = followMapper.selectCount(
                    new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.UserFollow>()
                            .eq(com.n1etzsch3.recipe.business.entity.UserFollow::getFollowerId, currentUserId)
                            .eq(com.n1etzsch3.recipe.business.entity.UserFollow::getFollowedId, recipe.getUserId()));
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

        // 1. Status Filter
        if (query.getStatus() != null) {
            wrapper.eq(RecipeInfo::getStatus, query.getStatus());
        } else if (query.getAuthorId() == null) {
            // Default: Only show published if not filtering by specific author/status
            wrapper.eq(RecipeInfo::getStatus, RecipeConstants.STATUS_PUBLISHED);
        }

        // 2. Author Filter
        if (query.getAuthorId() != null) {
            wrapper.eq(RecipeInfo::getUserId, query.getAuthorId());
        }

        // 3. Category Filter
        if (query.getCategoryId() != null) {
            wrapper.eq(RecipeInfo::getCategoryId, query.getCategoryId());
        }

        // 4. Keyword Filter
        if (StrUtil.isNotBlank(query.getKeyword())) {
            wrapper.like(RecipeInfo::getTitle, query.getKeyword());
        }

        // 5. Sort
        if (StringUtils.hasText(query.getSort()) && RecipeConstants.SORT_HOT.equals(query.getSort())) {
            wrapper.orderByDesc(RecipeInfo::getViewCount);
        } else {
            wrapper.orderByDesc(RecipeInfo::getCreateTime);
        }

        Page<RecipeInfo> resultPage = this.page(page, wrapper);

        // 转换为 DTO Page
        IPage<RecipeDetailDTO> dtoPage = resultPage.convert(recipe -> {
            RecipeDetailDTO dto = new RecipeDetailDTO();
            BeanUtil.copyProperties(recipe, dto);
            // 简单列表不需要查 steps 和 ingredients，但需要作者名
            SysUser author = sysUserMapper.selectById(recipe.getUserId());
            if (author != null) {
                dto.setAuthorName(author.getNickname());
                dto.setAuthorAvatar(author.getAvatar());
            }

            // 统计评论数
            Long recipeId = recipe.getId();
            Long commentCnt = commentMapper.selectCount(
                    new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.RecipeComment>()
                            .eq(com.n1etzsch3.recipe.business.entity.RecipeComment::getRecipeId, recipeId));
            dto.setCommentCount(commentCnt != null ? commentCnt.intValue() : 0);

            // 统计收藏数
            Long favoriteCnt = favoriteMapper.selectCount(
                    new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.UserFavorite>()
                            .eq(com.n1etzsch3.recipe.business.entity.UserFavorite::getRecipeId, recipeId));
            dto.setFavoriteCount(favoriteCnt != null ? favoriteCnt.intValue() : 0);

            // 分类名称映射
            dto.setCategoryName(mapCategoryIdToName(recipe.getCategoryId()));

            // 检查当前用户是否收藏
            Long currentUserId = com.n1etzsch3.recipe.common.context.UserContext.getUserId();
            if (currentUserId != null) {
                Long isFav = favoriteMapper.selectCount(
                        new LambdaQueryWrapper<com.n1etzsch3.recipe.business.entity.UserFavorite>()
                                .eq(com.n1etzsch3.recipe.business.entity.UserFavorite::getRecipeId, recipeId)
                                .eq(com.n1etzsch3.recipe.business.entity.UserFavorite::getUserId, currentUserId));
                dto.setIsFavorite(isFav != null && isFav > 0);
            } else {
                dto.setIsFavorite(false);
            }

            return dto;
        });

        return Result.ok(dtoPage);
    }

    // 分类ID映射到名称
    private String mapCategoryIdToName(Integer categoryId) {
        if (categoryId == null)
            return "美食";
        return switch (categoryId) {
            case 1 -> "家常菜";
            case 2 -> "下饭菜";
            case 3 -> "烘焙";
            case 4 -> "肉类";
            case 5 -> "汤羹";
            case 6 -> "主食";
            case 7 -> "小吃";
            case 8 -> "其他";
            default -> "美食";
        };
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

        return Result.ok("修改成功，请等待审核");
    }

    @Override
    public Result<?> deleteRecipe(Long id) {
        Long userId = UserContext.getUserId();
        log.info("用户 {} 删除菜谱: {}", userId, id);
        RecipeInfo recipe = this.getById(id);
        if (recipe == null)
            return Result.fail("菜谱不存在");
        if (!recipe.getUserId().equals(userId))
            return Result.fail("无权删除");

        this.removeById(id);
        // 级联删除 steps ingredients?
        ingredientMapper.delete(new LambdaQueryWrapper<RecipeIngredient>().eq(RecipeIngredient::getRecipeId, id));
        stepMapper.delete(new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, id));

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

        return Result.ok("下架成功，您现在可以编辑菜谱了");
    }
}
