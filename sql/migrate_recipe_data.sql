-- ============================================================
-- 菜谱数据迁移 SQL 脚本
-- 
-- 功能：将 recipe_info.description 中的 JSON 数据迁移到
--       recipe_ingredient 和 recipe_step 表中
--
-- 注意：MySQL 5.7+ 支持 JSON 函数，此脚本基于 MySQL 5.7+ / MariaDB 10.2+
-- 执行前请务必备份数据库！
--
-- 作者：AI Assistant
-- 日期：2025-12-25
-- ============================================================

USE `recipe-platform`;

-- 设置安全更新模式（允许无 WHERE 更新）
SET SQL_SAFE_UPDATES = 0;

-- ============================================================
-- 步骤 0：备份原始数据（建议在执行前手动备份）
-- ============================================================

-- 创建备份表（如果需要）
CREATE TABLE IF NOT EXISTS `recipe_info_backup` AS 
SELECT * FROM `recipe_info`;

SELECT CONCAT('✅ 已创建备份表 recipe_info_backup，共 ', COUNT(*), ' 条记录') AS message 
FROM recipe_info_backup;


-- ============================================================
-- 步骤 1：创建临时存储过程进行数据迁移
-- ============================================================

DROP PROCEDURE IF EXISTS migrate_recipe_data;

DELIMITER $$

CREATE PROCEDURE migrate_recipe_data()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_recipe_id BIGINT;
    DECLARE v_title VARCHAR(100);
    DECLARE v_description TEXT;
    DECLARE v_intro TEXT;
    DECLARE v_ingredients JSON;
    DECLARE v_steps JSON;
    DECLARE v_ing_count INT;
    DECLARE v_step_count INT;
    DECLARE v_idx INT;
    DECLARE v_name VARCHAR(50);
    DECLARE v_amount VARCHAR(50);
    DECLARE v_content TEXT;
    DECLARE v_image_url VARCHAR(255);
    DECLARE v_existing_count INT;
    DECLARE v_success_count INT DEFAULT 0;
    DECLARE v_skip_count INT DEFAULT 0;
    
    -- 游标：遍历所有菜谱
    DECLARE recipe_cursor CURSOR FOR 
        SELECT id, title, description 
        FROM recipe_info 
        WHERE description IS NOT NULL 
          AND description != ''
          AND JSON_VALID(description)
        ORDER BY id;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN recipe_cursor;
    
    read_loop: LOOP
        FETCH recipe_cursor INTO v_recipe_id, v_title, v_description;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 检查是否已有步骤/用料数据
        SELECT COUNT(*) INTO v_existing_count 
        FROM recipe_ingredient 
        WHERE recipe_id = v_recipe_id;
        
        IF v_existing_count > 0 THEN
            SET v_skip_count = v_skip_count + 1;
            -- 跳过已有数据的记录
            ITERATE read_loop;
        END IF;
        
        SELECT COUNT(*) INTO v_existing_count 
        FROM recipe_step 
        WHERE recipe_id = v_recipe_id;
        
        IF v_existing_count > 0 THEN
            SET v_skip_count = v_skip_count + 1;
            ITERATE read_loop;
        END IF;
        
        -- 解析 JSON
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
            
            SET v_intro = JSON_UNQUOTE(JSON_EXTRACT(v_description, '$.intro'));
            SET v_ingredients = JSON_EXTRACT(v_description, '$.ingredients');
            SET v_steps = JSON_EXTRACT(v_description, '$.steps');
        END;
        
        -- 插入用料数据
        IF v_ingredients IS NOT NULL AND JSON_LENGTH(v_ingredients) > 0 THEN
            SET v_ing_count = JSON_LENGTH(v_ingredients);
            SET v_idx = 0;
            
            WHILE v_idx < v_ing_count DO
                SET v_name = JSON_UNQUOTE(JSON_EXTRACT(v_ingredients, CONCAT('$[', v_idx, '].name')));
                SET v_amount = JSON_UNQUOTE(JSON_EXTRACT(v_ingredients, CONCAT('$[', v_idx, '].amount')));
                
                IF v_name IS NOT NULL AND v_name != '' AND v_name != 'null' THEN
                    INSERT INTO recipe_ingredient (recipe_id, name, amount, sort_order)
                    VALUES (v_recipe_id, 
                            SUBSTRING(v_name, 1, 50), 
                            COALESCE(NULLIF(SUBSTRING(v_amount, 1, 50), ''), '适量'),
                            v_idx + 1);
                END IF;
                
                SET v_idx = v_idx + 1;
            END WHILE;
        END IF;
        
        -- 插入步骤数据
        IF v_steps IS NOT NULL AND JSON_LENGTH(v_steps) > 0 THEN
            SET v_step_count = JSON_LENGTH(v_steps);
            SET v_idx = 0;
            
            WHILE v_idx < v_step_count DO
                -- 兼容 content 和 description 两种字段名
                SET v_content = JSON_UNQUOTE(JSON_EXTRACT(v_steps, CONCAT('$[', v_idx, '].content')));
                IF v_content IS NULL OR v_content = '' OR v_content = 'null' THEN
                    SET v_content = JSON_UNQUOTE(JSON_EXTRACT(v_steps, CONCAT('$[', v_idx, '].description')));
                END IF;
                
                SET v_image_url = JSON_UNQUOTE(JSON_EXTRACT(v_steps, CONCAT('$[', v_idx, '].imageUrl')));
                IF v_image_url = 'null' THEN
                    SET v_image_url = NULL;
                END IF;
                
                IF v_content IS NOT NULL AND v_content != '' AND v_content != 'null' THEN
                    INSERT INTO recipe_step (recipe_id, step_no, description, image_url)
                    VALUES (v_recipe_id, v_idx + 1, v_content, v_image_url);
                END IF;
                
                SET v_idx = v_idx + 1;
            END WHILE;
        END IF;
        
        -- 更新 description 为纯文本 intro
        IF v_intro IS NULL OR v_intro = 'null' THEN
            SET v_intro = '';
        END IF;
        
        UPDATE recipe_info 
        SET description = SUBSTRING(v_intro, 1, 500)
        WHERE id = v_recipe_id;
        
        SET v_success_count = v_success_count + 1;
        
    END LOOP;
    
    CLOSE recipe_cursor;
    
    -- 输出统计结果
    SELECT CONCAT('✅ 迁移完成：成功 ', v_success_count, ' 条，跳过 ', v_skip_count, ' 条') AS result;
    
END$$

DELIMITER ;


-- ============================================================
-- 步骤 2：执行迁移
-- ============================================================

-- 查看迁移前的数据状态
SELECT 
    '迁移前状态' AS stage,
    (SELECT COUNT(*) FROM recipe_info WHERE description IS NOT NULL AND JSON_VALID(description)) AS json_recipes,
    (SELECT COUNT(*) FROM recipe_ingredient) AS total_ingredients,
    (SELECT COUNT(*) FROM recipe_step) AS total_steps;

-- 执行迁移存储过程
CALL migrate_recipe_data();

-- 查看迁移后的数据状态
SELECT 
    '迁移后状态' AS stage,
    (SELECT COUNT(*) FROM recipe_info WHERE description IS NOT NULL AND JSON_VALID(description)) AS json_recipes,
    (SELECT COUNT(*) FROM recipe_ingredient) AS total_ingredients,
    (SELECT COUNT(*) FROM recipe_step) AS total_steps;


-- ============================================================
-- 步骤 3：验证迁移结果
-- ============================================================

-- 查看示例数据
SELECT '=== 用料表示例 (前10条) ===' AS title;
SELECT ri.id AS recipe_id, ri.title, ing.name, ing.amount, ing.sort_order
FROM recipe_info ri
JOIN recipe_ingredient ing ON ri.id = ing.recipe_id
ORDER BY ri.id, ing.sort_order
LIMIT 10;

SELECT '=== 步骤表示例 (前10条) ===' AS title;
SELECT ri.id AS recipe_id, ri.title, st.step_no, LEFT(st.description, 50) AS description_preview
FROM recipe_info ri
JOIN recipe_step st ON ri.id = st.recipe_id
ORDER BY ri.id, st.step_no
LIMIT 10;

SELECT '=== 更新后的 description 字段示例 ===' AS title;
SELECT id, title, LEFT(description, 100) AS description_preview
FROM recipe_info
WHERE description IS NOT NULL AND description != ''
LIMIT 5;


-- ============================================================
-- 清理（可选）
-- ============================================================

-- 删除临时存储过程
-- DROP PROCEDURE IF EXISTS migrate_recipe_data;

-- 如果确认迁移成功，可以删除备份表
-- DROP TABLE IF EXISTS recipe_info_backup;

-- 恢复安全更新模式
SET SQL_SAFE_UPDATES = 1;

SELECT '✅ 迁移脚本执行完毕！请检查上方的验证结果。' AS final_message;
