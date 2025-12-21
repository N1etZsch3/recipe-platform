-- =====================================================
-- 数据库索引优化脚本
-- 项目: recipe-platform
-- 生成时间: 2025-12-21
-- =====================================================

-- ===================== sys_user =====================
-- 用户名唯一索引（登录/注册查询）
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username ON sys_user(username);

-- ===================== recipe_info ==================
-- 用户ID索引（查询用户菜谱）
CREATE INDEX IF NOT EXISTS idx_recipe_user ON recipe_info(user_id);

-- 状态索引（按状态过滤）
CREATE INDEX IF NOT EXISTS idx_recipe_status ON recipe_info(status);

-- 分类ID索引（按分类过滤）
CREATE INDEX IF NOT EXISTS idx_recipe_category ON recipe_info(category_id);

-- 复合索引：热门菜谱排序（状态+浏览量）
CREATE INDEX IF NOT EXISTS idx_recipe_hot ON recipe_info(status, view_count DESC);

-- =================== recipe_ingredient ==============
-- 菜谱ID索引（关联查询食材）
CREATE INDEX IF NOT EXISTS idx_ingredient_recipe ON recipe_ingredient(recipe_id);

-- ===================== recipe_step ==================
-- 菜谱ID索引（关联查询步骤）
CREATE INDEX IF NOT EXISTS idx_step_recipe ON recipe_step(recipe_id);

-- ==================== chat_message ==================
-- 发送方ID索引
CREATE INDEX IF NOT EXISTS idx_msg_sender ON chat_message(sender_id);

-- 接收方ID索引
CREATE INDEX IF NOT EXISTS idx_msg_receiver ON chat_message(receiver_id);

-- ===================== user_follow ==================
-- 关注者ID索引（获取关注列表）
CREATE INDEX IF NOT EXISTS idx_follow_follower ON user_follow(follower_id);

-- 被关注者ID索引（获取粉丝列表）
CREATE INDEX IF NOT EXISTS idx_follow_followed ON user_follow(followed_id);

-- ================== recipe_category =================
-- 分类名称索引（按名称查询分类）
CREATE INDEX IF NOT EXISTS idx_category_name ON recipe_category(name);

-- =====================================================
-- 验证索引
-- =====================================================
-- SHOW INDEX FROM sys_user;
-- SHOW INDEX FROM recipe_info;
-- SHOW INDEX FROM recipe_ingredient;
-- SHOW INDEX FROM recipe_step;
-- SHOW INDEX FROM chat_message;
-- SHOW INDEX FROM user_follow;
-- SHOW INDEX FROM recipe_category;
