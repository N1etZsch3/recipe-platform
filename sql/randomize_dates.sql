/**
 * 随机化时间脚本
 * 
 * 功能：
 * 1. 随机修改 sys_user 表的 create_time（用户注册时间）
 * 2. 随机修改 recipe_info 表的 create_time（菜谱发布时间）
 * 
 * 规则：
 * - 保留原有年份
 * - 月份：随机 1-12 月
 * - 日期：随机（根据月份自动适配1-28/30/31）
 * - 小时：随机 8-22（模拟正常使用时间）
 * - 分钟和秒：随机 0-59
 */

USE `recipe_platform`;

-- ==========================================
-- 1. 随机化用户注册时间 (sys_user.create_time)
-- ==========================================

UPDATE `sys_user` SET 
    `create_time` = DATE_ADD(
        MAKEDATE(YEAR(`create_time`), 1),  -- 当年1月1日
        INTERVAL (
            -- 随机天数 (0-364)
            FLOOR(RAND() * 365)
        ) DAY
    ) + INTERVAL FLOOR(8 + RAND() * 14) HOUR  -- 随机小时 8-21
      + INTERVAL FLOOR(RAND() * 60) MINUTE    -- 随机分钟
      + INTERVAL FLOOR(RAND() * 60) SECOND    -- 随机秒
WHERE `id` > 0;

-- 同时更新 update_time 为 create_time 之后的随机时间
UPDATE `sys_user` SET 
    `update_time` = `create_time` + INTERVAL FLOOR(RAND() * 30) DAY
                                  + INTERVAL FLOOR(RAND() * 24) HOUR
                                  + INTERVAL FLOOR(RAND() * 60) MINUTE
WHERE `id` > 0;


-- ==========================================
-- 2. 随机化菜谱发布时间 (recipe_info.create_time)
-- ==========================================

UPDATE `recipe_info` SET 
    `create_time` = DATE_ADD(
        MAKEDATE(YEAR(`create_time`), 1),  -- 当年1月1日
        INTERVAL (
            -- 随机天数 (0-364)
            FLOOR(RAND() * 365)
        ) DAY
    ) + INTERVAL FLOOR(8 + RAND() * 14) HOUR  -- 随机小时 8-21
      + INTERVAL FLOOR(RAND() * 60) MINUTE    -- 随机分钟
      + INTERVAL FLOOR(RAND() * 60) SECOND    -- 随机秒
WHERE `id` > 0;

-- 同时更新 update_time 为 create_time 之后的随机时间
UPDATE `recipe_info` SET 
    `update_time` = `create_time` + INTERVAL FLOOR(RAND() * 7) DAY
                                  + INTERVAL FLOOR(RAND() * 24) HOUR
                                  + INTERVAL FLOOR(RAND() * 60) MINUTE
WHERE `id` > 0;


-- ==========================================
-- 3. 验证结果
-- ==========================================

-- 查看用户时间分布
SELECT 
    id,
    username,
    nickname,
    DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
    DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') AS update_time
FROM `sys_user` 
ORDER BY create_time
LIMIT 10;

-- 查看菜谱时间分布
SELECT 
    id,
    title,
    MONTH(create_time) AS month,
    DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS create_time
FROM `recipe_info`
ORDER BY create_time
LIMIT 10;

-- 统计每月菜谱数量分布
SELECT 
    MONTH(create_time) AS month,
    COUNT(*) AS recipe_count
FROM `recipe_info`
GROUP BY MONTH(create_time)
ORDER BY month;
