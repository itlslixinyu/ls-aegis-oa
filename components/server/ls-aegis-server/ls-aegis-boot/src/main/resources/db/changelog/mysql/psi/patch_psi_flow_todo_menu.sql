-- liquibase formatted sql

-- changeset psi:9 validCheckSum:ANY
-- comment PSI：库存流水查询 + 业务待办菜单

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10190, '库存流水', 10002, 2, '/psi/stock-flow', 'PsiStockFlow', 'psi/stock-flow/index', NULL, 'storage', b'0', b'0', b'0', NULL, 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10190);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10191, '列表', 10190, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockFlow:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10191);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10195, '业务待办', 10002, 2, '/psi/todo', 'PsiTodo', 'psi/todo/index', NULL, 'calendar', b'0', b'0', b'0', NULL, 8, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10195);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10196, '列表', 10195, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:todo:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10196);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10197, '完成', 10195, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:todo:done', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10197);
