-- liquibase formatted sql

-- changeset psi:2 validCheckSum:ANY
-- comment PSI 入库/出库菜单与确认权限（挂到库存业务模块，幂等）
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10040, '采购入库', 10002, 2, '/psi/stock-in', 'PsiStockIn', 'psi/stock-in/index', NULL, 'download', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10040);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10041, '列表', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:list', 1, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10041);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10042, '详情', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:get', 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10042);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10043, '新增', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:create', 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10043);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10044, '修改', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:update', 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10044);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10045, '删除', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:delete', 5, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10045);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10046, '确认', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:confirm', 6, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10046);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10050, '销售出库', 10002, 2, '/psi/stock-out', 'PsiStockOut', 'psi/stock-out/index', NULL, 'upload', b'0', b'0', b'0', NULL, 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10050);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10051, '列表', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:list', 1, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10051);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10052, '详情', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:get', 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10052);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10053, '新增', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:create', 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10053);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10054, '修改', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:update', 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10054);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10055, '删除', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:delete', 5, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10055);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10056, '确认', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:confirm', 6, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10056);
