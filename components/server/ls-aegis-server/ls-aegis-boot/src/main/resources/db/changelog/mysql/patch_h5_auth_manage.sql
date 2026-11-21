-- liquibase formatted sql

-- changeset ls:h5_auth_option
-- comment H5 客户授权：令牌有效期与活跃续期配置项
SET NAMES utf8mb4;

INSERT INTO `sys_option`
(`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 69, 'EOMS', 'H5令牌有效期（分钟）', 'H5_TOKEN_EXPIRE_MINUTES', NULL, '120', '客户 H5 身份令牌 Redis TTL，建议 30～480；国密环境下令牌由 SM3 生成'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'H5_TOKEN_EXPIRE_MINUTES');

INSERT INTO `sys_option`
(`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 70, 'EOMS', 'H5令牌活跃续期', 'H5_TOKEN_AUTO_REFRESH', NULL, '1', '1=每次请求续期；0=固定 TTL 到期失效'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'H5_TOKEN_AUTO_REFRESH');

-- changeset ls:h5_auth_menu
-- comment 系统配置增加 H5 客户授权页签及权限
SET NAMES utf8mb4;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1270, 'H5客户授权', 1150, 2, CONCAT('/system/config', CHAR(63), 'tab=h5Auth'), 'SystemH5AuthConfig', 'system/config/h5-auth/index', NULL, 'mobile', b'0', b'0', b'1', NULL, 11, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1270);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1271, '查询配置', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:get', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1271);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1272, '修改配置', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:update', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1272);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1273, '在线列表', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:list', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1273);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1274, '强退令牌', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:kickout', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1274);

-- changeset ls:h5_auth_role_menu
-- comment 同步 H5 客户授权权限给已有运维报修配置权限的角色
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1270
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1260
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1270);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1271
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1261
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1271);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1272
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1262
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1272);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1273
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1261
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1273);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1274
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1262
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1274);
