-- liquibase formatted sql

-- changeset psi:146
-- validCheckSum: ANY
-- comment PSI：分类/账户 导入导出菜单权限（幂等）

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10316, '导出', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:export', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10316);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10317, '导入', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:import', 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10317);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10155, '导出', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:export', 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10155);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10156, '导入', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:import', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10156);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10316 AS id UNION ALL SELECT 10317
    UNION ALL SELECT 10155 UNION ALL SELECT 10156
) m
WHERE r.id IN (1, 3);
