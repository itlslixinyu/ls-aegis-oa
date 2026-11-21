-- liquibase formatted sql

-- changeset ls:sign_audit_to_monitor_log
-- comment 电子签章审计日志迁入系统监控-系统日志（权限按钮 2034，下线独立菜单 30007）
SET NAMES utf8mb4;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 2034, '签章审计', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sign:audit:list', 4, 1, 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 2034);

-- 种子角色默认具备签章审计按钮权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 2034
FROM `sys_role` r
WHERE r.id IN (1, 3);

-- 原「审计日志」菜单角色、以及已有系统日志列表权限的角色 → 签章审计按钮
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 2034
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (30007, 2031)
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 2034
  );

-- 仅有签章审计、尚无系统日志入口的角色：补齐「系统监控 / 系统日志」父菜单，保证侧栏可达
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, m.`id`
FROM `sys_role_menu` rm
CROSS JOIN (SELECT 2000 AS `id` UNION ALL SELECT 2030) m
WHERE rm.`menu_id` = 30007
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = m.`id`
  );

DELETE FROM `sys_role_menu` WHERE `menu_id` = 30007;
DELETE FROM `sys_menu` WHERE `id` = 30007;
