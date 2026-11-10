-- liquibase formatted sql

-- changeset psi:144
-- validCheckSum: ANY
-- comment PSI：仓库/单位/商品 导入导出菜单权限（幂等）

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10016, '导出', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:export', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10016);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10017, '导入', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:import', 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10017);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10026, '导出', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:export', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10026);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10027, '导入', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:import', 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10027);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10036, '导出', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:export', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10036);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10037, '导入', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:import', 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10037);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10083, '导入', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:import', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10083);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10016 AS id UNION ALL SELECT 10017
    UNION ALL SELECT 10026 UNION ALL SELECT 10027
    UNION ALL SELECT 10036 UNION ALL SELECT 10037
    UNION ALL SELECT 10083
) m
WHERE r.id IN (1, 3);
