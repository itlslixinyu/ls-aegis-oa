-- liquibase formatted sql

-- changeset ls:module_option_and_menu
-- comment 系统配置：功能模块总开关（租户 / 应用 / 任务调度）
SET NAMES utf8mb4;

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 48, 'MODULE', '租户管理', 'MODULE_TENANT_ENABLED', NULL, '1', '关闭后隐藏租户管理菜单并拦截相关接口。多租户行级隔离仍由 continew-starter.tenant.enabled 控制。'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'MODULE_TENANT_ENABLED');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 49, 'MODULE', '应用管理', 'MODULE_OPEN_ENABLED', NULL, '1', '关闭后隐藏能力开放/应用管理菜单并拦截相关接口。'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'MODULE_OPEN_ENABLED');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 50, 'MODULE', '任务调度', 'MODULE_SCHEDULE_ENABLED', NULL, '0', '关闭后隐藏任务调度菜单并拦截相关接口。开启后仍须配置 snail-job.enabled=true 并启动调度中心。'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'MODULE_SCHEDULE_ENABLED');

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1220, '功能模块', 1150, 2, '/system/config?tab=module', 'SystemModuleConfig', 'system/config/module/index', NULL, 'apps', b'0', b'0', b'1', NULL, 7, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1220);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1221, '查询', 1220, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:moduleConfig:get', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1221);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1222, '修改', 1220, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:moduleConfig:update', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1222);

UPDATE `sys_menu` SET `sort` = 8 WHERE `id` = 1230 AND `sort` = 7;
UPDATE `sys_menu` SET `sort` = 9 WHERE `id` = 1250 AND `sort` IN (7, 8);

-- changeset ls:module_option_role_menu
-- comment 将功能模块配置权限同步给已有大屏配置权限的角色
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1220
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1210
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1220
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1221
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1211
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1221
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1222
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1212
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1222
  );
