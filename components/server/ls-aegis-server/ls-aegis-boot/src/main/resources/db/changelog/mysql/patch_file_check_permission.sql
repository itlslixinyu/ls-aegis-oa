-- liquibase formatted sql

-- changeset ls:file_check_permission
-- comment 补充文件秒传/去重校验权限 system:file:check，并同步给已有上传权限的角色
SET NAMES utf8mb4;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1123, '校验文件', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:check', 13, 1, 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1123);

-- 已有「上传」权限的角色同步获得「校验」权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1123
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1113
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1123
  );
