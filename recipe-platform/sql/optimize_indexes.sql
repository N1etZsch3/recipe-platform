-- =====================================================
-- 数据库索引优化脚本 (MySQL 兼容版)
-- 项目: recipe-platform
-- 生成时间: 2025-12-21
-- 更新时间: 2025-12-25
-- =====================================================
-- 注意: MySQL 不支持 CREATE INDEX IF NOT EXISTS
-- 此脚本使用存储过程来安全地创建索引（跳过已存在的索引）

DELIMITER //

-- 创建辅助存储过程
DROP PROCEDURE IF EXISTS create_index_if_not_exists//
CREATE PROCEDURE create_index_if_not_exists(
    IN table_name VARCHAR(64),
    IN index_name VARCHAR(64),
    IN index_columns VARCHAR(255),
    IN is_unique BOOLEAN
)
BEGIN
    DECLARE index_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO index_exists
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name
      AND INDEX_NAME = index_name;
    
    IF index_exists = 0 THEN
        IF is_unique THEN
            SET @sql = CONCAT('CREATE UNIQUE INDEX ', index_name, ' ON ', table_name, '(', index_columns, ')');
        ELSE
            SET @sql = CONCAT('CREATE INDEX ', index_name, ' ON ', table_name, '(', index_columns, ')');
        END IF;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Created index: ', index_name, ' on ', table_name) AS result;
    ELSE
        SELECT CONCAT('Index already exists: ', index_name) AS result;
    END IF;
END//

DELIMITER ;

-- ===================== sys_user =====================
-- 用户名唯一索引已在建表时创建（uk_username）

-- ===================== recipe_info ==================
CALL create_index_if_not_exists('recipe_info', 'idx_recipe_user', 'user_id', FALSE);
CALL create_index_if_not_exists('recipe_info', 'idx_recipe_status', 'status', FALSE);
CALL create_index_if_not_exists('recipe_info', 'idx_recipe_category', 'category_id', FALSE);
CALL create_index_if_not_exists('recipe_info', 'idx_recipe_hot', 'status, view_count DESC', FALSE);

-- =================== recipe_ingredient ==============
CALL create_index_if_not_exists('recipe_ingredient', 'idx_ingredient_recipe', 'recipe_id', FALSE);

-- ===================== recipe_step ==================
CALL create_index_if_not_exists('recipe_step', 'idx_step_recipe', 'recipe_id', FALSE);

-- ==================== chat_message ==================
CALL create_index_if_not_exists('chat_message', 'idx_msg_sender', 'sender_id', FALSE);
CALL create_index_if_not_exists('chat_message', 'idx_msg_receiver', 'receiver_id', FALSE);

-- ===================== user_follow ==================
CALL create_index_if_not_exists('user_follow', 'idx_follow_follower', 'follower_id', FALSE);
CALL create_index_if_not_exists('user_follow', 'idx_follow_followed', 'followed_id', FALSE);

-- ================== recipe_category =================
CALL create_index_if_not_exists('recipe_category', 'idx_category_name', 'name', FALSE);

-- 清理辅助存储过程
DROP PROCEDURE IF EXISTS create_index_if_not_exists;

-- =====================================================
-- 验证索引 (可选执行)
-- =====================================================
-- SHOW INDEX FROM sys_user;
-- SHOW INDEX FROM recipe_info;
-- SHOW INDEX FROM recipe_ingredient;
-- SHOW INDEX FROM recipe_step;
-- SHOW INDEX FROM chat_message;
-- SHOW INDEX FROM user_follow;
-- SHOW INDEX FROM recipe_category;
