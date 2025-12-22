package com.n1etzsch3.recipe.common.constant;

/**
 * 缓存相关常量
 */
public class CacheConstants {

    // ==================== 缓存名称 ====================
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_DASHBOARD = "dashboard";
    public static final String CACHE_USER = "user";
    public static final String CACHE_RECIPE = "recipe";
    public static final String CACHE_HOT_RECIPES = "hotRecipes";

    // ==================== Key 前缀 ====================
    public static final String KEY_PREFIX = "recipe:";
    public static final String KEY_CATEGORIES_ALL = KEY_PREFIX + "categories:all";
    public static final String KEY_DASHBOARD = KEY_PREFIX + "dashboard";
    public static final String KEY_USER_INFO = KEY_PREFIX + "user:info:";
    public static final String KEY_RECIPE_DETAIL = KEY_PREFIX + "recipe:detail:";
    public static final String KEY_RECIPE = KEY_PREFIX + "recipe:";

    // ==================== 安全相关 ====================
    public static final String KEY_TOKEN_BLACKLIST = KEY_PREFIX + "token:blacklist:";
    public static final String KEY_LOGIN_FAIL = KEY_PREFIX + "login:fail:";
    public static final String KEY_CAPTCHA = KEY_PREFIX + "captcha:";
    public static final String KEY_RATE_LIMIT = KEY_PREFIX + "rate:limit:";
    public static final String KEY_FORCE_LOGIN = KEY_PREFIX + "force_login:";

    // ==================== TTL (秒) ====================
    public static final long TTL_CATEGORIES = 86400; // 24小时
    public static final long TTL_DASHBOARD = 300; // 5分钟
    public static final long TTL_USER = 1800; // 30分钟
    public static final long TTL_RECIPE = 7200; // 2小时
    public static final long TTL_HOT_RECIPES = 600; // 10分钟
    public static final long TTL_CAPTCHA = 300; // 5分钟
}
