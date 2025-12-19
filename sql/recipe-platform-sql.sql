/*
 * 网上菜谱分享平台数据库初始化脚本
 * 数据库: recipe_platform
 * 版本: 1.0
 */

-- 1. 创建并使用数据库
CREATE DATABASE IF NOT EXISTS `recipe_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `recipe_platform`;

-- ==========================================
-- 1. 用户管理模块
-- ==========================================

-- 表：sys_user (用户表)
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `nickname` varchar(50) NOT NULL COMMENT '用户昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `intro` varchar(200) DEFAULT NULL COMMENT '个人简介',
  `role` varchar(20) NOT NULL DEFAULT 'member' COMMENT '角色: admin-管理员, member-普通用户',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态: 1-正常, 0-封禁',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`) USING BTREE COMMENT '账号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';


-- ==========================================
-- 2. 菜谱核心模块
-- ==========================================

-- 表：recipe_category (菜谱分类表)
CREATE TABLE `recipe_category` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `sort_order` int(4) DEFAULT '0' COMMENT '排序优先级',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱分类表';

-- 表：recipe_info (菜谱主表)
CREATE TABLE `recipe_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(100) NOT NULL COMMENT '菜谱标题',
  `user_id` bigint(20) NOT NULL COMMENT '发布者ID',
  `category_id` int(11) NOT NULL COMMENT '分类ID',
  `cover_image` varchar(255) NOT NULL COMMENT '封面图URL',
  `description` varchar(500) DEFAULT NULL COMMENT '菜谱简介/心得',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态: 0-待审核, 1-已发布, 2-驳回',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '审核驳回原因',
  `view_count` int(11) DEFAULT '0' COMMENT '浏览量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_title` (`title`) COMMENT '用于搜索'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱信息表';

-- 表：recipe_ingredient (菜谱用料表)
CREATE TABLE `recipe_ingredient` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recipe_id` bigint(20) NOT NULL COMMENT '关联菜谱ID',
  `name` varchar(50) NOT NULL COMMENT '食材名称',
  `amount` varchar(50) NOT NULL COMMENT '用量',
  `sort_order` int(4) DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱用料表';

-- 表：recipe_step (菜谱步骤表)
CREATE TABLE `recipe_step` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recipe_id` bigint(20) NOT NULL COMMENT '关联菜谱ID',
  `step_no` int(4) NOT NULL COMMENT '步骤序号',
  `description` text NOT NULL COMMENT '步骤描述',
  `image_url` varchar(255) DEFAULT NULL COMMENT '步骤图URL',
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱步骤表';


-- ==========================================
-- 3. 互动与社交模块
-- ==========================================

-- 表：user_favorite (用户收藏表)
CREATE TABLE `user_favorite` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `recipe_id` bigint(20) NOT NULL COMMENT '菜谱ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_recipe` (`user_id`,`recipe_id`) COMMENT '防止重复收藏',
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- 表：recipe_comment (菜谱评论表)
CREATE TABLE `recipe_comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `recipe_id` bigint(20) NOT NULL COMMENT '菜谱ID',
  `user_id` bigint(20) NOT NULL COMMENT '评论者ID',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父评论ID，null表示顶级评论',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `like_count` int(11) DEFAULT '0' COMMENT '点赞数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱评论表';

-- 表：comment_like (评论点赞表)
CREATE TABLE `comment_like` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` bigint(20) NOT NULL COMMENT '评论ID',
  `user_id` bigint(20) NOT NULL COMMENT '点赞用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`) COMMENT '防止重复点赞',
  KEY `idx_comment_id` (`comment_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

-- ============================================
-- 增量更新脚本（已有数据库升级时使用）
-- ============================================
-- ALTER TABLE recipe_comment ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父评论ID';
-- ALTER TABLE recipe_comment ADD COLUMN like_count INT DEFAULT 0 COMMENT '点赞数';
-- ALTER TABLE recipe_comment ADD INDEX idx_parent_id (parent_id);


-- 表：user_follow (用户关注表)
CREATE TABLE `user_follow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `follower_id` bigint(20) NOT NULL COMMENT '发起关注者ID',
  `followed_id` bigint(20) NOT NULL COMMENT '被关注者ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow_relation` (`follower_id`,`followed_id`) COMMENT '防止重复关注',
  KEY `idx_follower` (`follower_id`),
  KEY `idx_followed` (`followed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- 表：sys_message (私信表)
CREATE TABLE `sys_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sender_id` bigint(20) NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint(20) NOT NULL COMMENT '接收者ID',
  `content` text NOT NULL COMMENT '私信内容',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态: 0-未读, 1-已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_receiver` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内私信表';