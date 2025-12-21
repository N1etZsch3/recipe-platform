package com.n1etzsch3.recipe.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * XSS 过滤工具类
 */
public class XssUtils {

    /**
     * 清理潜在的 XSS 攻击内容
     */
    public static String clean(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        // 基础 HTML 转义
        value = value.replace("&", "&amp;");
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");
        value = value.replace("\"", "&quot;");
        value = value.replace("'", "&#x27;");
        // 过滤 JavaScript 相关
        value = value.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        value = value.replaceAll("(?i)javascript:", "");
        value = value.replaceAll("(?i)on\\w+\\s*=", "");
        return value;
    }

    /**
     * 检查是否包含危险内容
     */
    public static boolean containsDangerousContent(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        String lowerValue = value.toLowerCase();
        return lowerValue.contains("<script")
                || lowerValue.contains("javascript:")
                || lowerValue.matches(".*on\\w+\\s*=.*");
    }
}
