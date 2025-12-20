-- 管理员操作日志表
CREATE TABLE `admin_operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id` bigint(20) NOT NULL COMMENT '操作管理员ID',
  `admin_name` varchar(50) NOT NULL COMMENT '管理员用户名',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型',
  `target_type` varchar(50) DEFAULT NULL COMMENT '目标类型(user/recipe/category/comment)',
  `target_id` bigint(20) DEFAULT NULL COMMENT '目标ID',
  `target_name` varchar(100) DEFAULT NULL COMMENT '目标名称(用户昵称/菜谱标题等)',
  `detail` varchar(500) DEFAULT NULL COMMENT '操作详情/备注',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';
