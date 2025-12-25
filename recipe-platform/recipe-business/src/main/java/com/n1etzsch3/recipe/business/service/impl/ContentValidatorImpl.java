package com.n1etzsch3.recipe.business.service.impl;

import com.n1etzsch3.recipe.business.domain.dto.ValidationResult;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.entity.RecipeStep;
import com.n1etzsch3.recipe.business.service.ContentValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 菜谱内容验证器实现
 * 用于预审菜谱内容，过滤敏感词、广告等
 */
@Slf4j
@Component
public class ContentValidatorImpl implements ContentValidator {

    /**
     * 敏感词列表（实际项目中应从数据库或配置文件加载）
     * 这里仅作示例，包含一些常见的需要过滤的词汇类型
     */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            // 广告相关
            "加微信", "加QQ", "免费领取", "点击链接",
            // 政治敏感（示例）
            "政治敏感词示例",
            // 色情词汇（示例）
            "色情词汇示例");

    /**
     * URL 模式（简单匹配）
     */
    private static final String URL_PATTERN = "(https?://|www\\.)\\S+";

    @Override
    public ValidationResult validate(RecipeInfo recipe, List<RecipeStep> steps) {
        // 1. 验证标题
        ValidationResult titleResult = validateText(recipe.getTitle(), "标题");
        if (!titleResult.isPassed()) {
            return titleResult;
        }

        // 2. 验证描述
        if (recipe.getDescription() != null) {
            ValidationResult descResult = validateText(recipe.getDescription(), "简介");
            if (!descResult.isPassed()) {
                return descResult;
            }
        }

        // 3. 验证步骤描述
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                RecipeStep step = steps.get(i);
                if (step.getDescription() != null) {
                    ValidationResult stepResult = validateText(
                            step.getDescription(),
                            "步骤 " + (i + 1));
                    if (!stepResult.isPassed()) {
                        return stepResult;
                    }
                }
            }
        }

        log.debug("菜谱内容验证通过: recipeId={}", recipe.getId());
        return ValidationResult.pass();
    }

    /**
     * 验证文本内容
     */
    private ValidationResult validateText(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return ValidationResult.pass();
        }

        String lowerText = text.toLowerCase();

        // 检查敏感词
        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                log.warn("检测到敏感词: field={}, word={}", fieldName, word);
                return ValidationResult.fail(fieldName + "包含敏感内容，请修改后重新提交");
            }
        }

        // 检查 URL（菜谱中不应包含外部链接）
        if (text.matches(".*" + URL_PATTERN + ".*")) {
            log.warn("检测到外部链接: field={}", fieldName);
            return ValidationResult.fail(fieldName + "不能包含外部链接");
        }

        return ValidationResult.pass();
    }
}
