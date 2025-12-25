package com.n1etzsch3.recipe.business.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.business.entity.RecipeComment;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.RecipeIngredient;
import com.n1etzsch3.recipe.business.entity.RecipeStep;
import com.n1etzsch3.recipe.business.entity.UserFavorite;
import com.n1etzsch3.recipe.business.mapper.RecipeCommentMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeIngredientMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeStepMapper;
import com.n1etzsch3.recipe.business.mapper.UserFavoriteMapper;
import com.n1etzsch3.recipe.common.constant.RecipeConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜谱数据定时清理任务
 * 清理过期的草稿和被驳回的菜谱，释放数据库空间
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeCleanupTask {

    private final RecipeInfoMapper recipeInfoMapper;
    private final RecipeIngredientMapper ingredientMapper;
    private final RecipeStepMapper stepMapper;
    private final RecipeCommentMapper commentMapper;
    private final UserFavoriteMapper favoriteMapper;

    /**
     * 被驳回菜谱保留天数（默认30天）
     */
    @Value("${recipe.cleanup.rejected-days:30}")
    private int rejectedRetentionDays;

    /**
     * 草稿菜谱保留天数（默认90天）
     */
    @Value("${recipe.cleanup.draft-days:90}")
    private int draftRetentionDays;

    /**
     * 每天凌晨 3 点执行清理任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cleanup() {
        log.info("开始执行菜谱数据清理任务...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rejectedThreshold = now.minusDays(rejectedRetentionDays);
        LocalDateTime draftThreshold = now.minusDays(draftRetentionDays);

        int rejectedCount = 0;
        int draftCount = 0;

        // 1. 清理过期的被驳回菜谱
        List<RecipeInfo> rejectedRecipes = recipeInfoMapper.selectList(
                new LambdaQueryWrapper<RecipeInfo>()
                        .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_REJECTED)
                        .lt(RecipeInfo::getUpdateTime, rejectedThreshold));

        for (RecipeInfo recipe : rejectedRecipes) {
            deleteRecipeWithRelations(recipe.getId());
            rejectedCount++;
        }

        // 2. 清理过期的草稿菜谱
        List<RecipeInfo> oldDrafts = recipeInfoMapper.selectList(
                new LambdaQueryWrapper<RecipeInfo>()
                        .eq(RecipeInfo::getStatus, RecipeConstants.STATUS_DRAFT)
                        .lt(RecipeInfo::getUpdateTime, draftThreshold));

        for (RecipeInfo recipe : oldDrafts) {
            deleteRecipeWithRelations(recipe.getId());
            draftCount++;
        }

        log.info("菜谱数据清理完成: 驳回菜谱 {} 个, 过期草稿 {} 个", rejectedCount, draftCount);
    }

    /**
     * 删除菜谱及其关联数据
     */
    private void deleteRecipeWithRelations(Long recipeId) {
        // 删除食材
        ingredientMapper.delete(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, recipeId));

        // 删除步骤
        stepMapper.delete(
                new LambdaQueryWrapper<RecipeStep>()
                        .eq(RecipeStep::getRecipeId, recipeId));

        // 删除评论
        commentMapper.delete(
                new LambdaQueryWrapper<RecipeComment>()
                        .eq(RecipeComment::getRecipeId, recipeId));

        // 删除收藏记录
        favoriteMapper.delete(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getRecipeId, recipeId));

        // 删除菜谱主记录
        recipeInfoMapper.deleteById(recipeId);

        log.debug("已删除菜谱及关联数据: recipeId={}", recipeId);
    }

    /**
     * 手动触发清理（用于管理后台或测试）
     */
    public void manualCleanup() {
        log.info("手动触发菜谱数据清理任务");
        cleanup();
    }
}
