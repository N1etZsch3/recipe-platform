package com.n1etzsch3.recipe.common.constant;

public class RecipeConstants {

    /** 状态：待审核 */
    public static final int STATUS_PENDING = 0;

    /** 状态：已发布 */
    public static final int STATUS_PUBLISHED = 1;

    /** 状态：驳回 */
    public static final int STATUS_REJECTED = 2;

    /** 状态：草稿 */
    public static final int STATUS_DRAFT = 3;

    /** 状态：处理中（队列处理阶段） */
    public static final int STATUS_PROCESSING = 4;

    /** 状态：已下架（用户或管理员主动下架） */
    public static final int STATUS_UNPUBLISHED = 5;

    /** 用户待审核菜谱最大数量 */
    public static final int MAX_PENDING_RECIPES = 10;

    /** 排序：最新 */
    public static final String SORT_NEW = "new";

    /** 排序：最热 */
    public static final String SORT_HOT = "hot";

    /** 缓存预热数量限制 */
    public static final int WARMUP_LIMIT = 20;
}
