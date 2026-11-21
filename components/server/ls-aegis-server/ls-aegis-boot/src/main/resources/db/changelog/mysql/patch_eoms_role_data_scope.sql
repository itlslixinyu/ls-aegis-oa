-- liquibase formatted sql

-- changeset ls:eoms_role_data_scope validCheckSum:ANY
-- comment EOMS 角色默认 data_scope 种子（主管/工程师/财务）
SET NAMES utf8mb4;

-- 11 EOMS 主管：本部门及以下（data_scope=2）
INSERT INTO `sys_role`
(`id`, `name`, `code`, `data_scope`, `description`, `sort`, `is_system`, `create_user`, `create_time`)
SELECT 11, 'EOMS 主管', 'eoms_manager', 2, '运维报修主管：本部门及下级数据', 11, b'0', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 11 OR `code` = 'eoms_manager');

-- 12 EOMS 工程师：仅本人（data_scope=4）；工单 SELF 额外匹配 engineer_id
INSERT INTO `sys_role`
(`id`, `name`, `code`, `data_scope`, `description`, `sort`, `is_system`, `create_user`, `create_time`)
SELECT 12, 'EOMS 工程师', 'eoms_engineer', 4, '运维工程师：仅本人创建或派给自己的工单', 12, b'0', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 12 OR `code` = 'eoms_engineer');

-- 13 EOMS 财务：本部门（data_scope=3）
INSERT INTO `sys_role`
(`id`, `name`, `code`, `data_scope`, `description`, `sort`, `is_system`, `create_user`, `create_time`)
SELECT 13, 'EOMS 财务', 'eoms_finance', 3, '运维财务：本部门结算/应收/收款', 13, b'0', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 13 OR `code` = 'eoms_finance');

-- 已存在同 code 时纠正 data_scope（不覆盖超管手工改大的 ALL）
UPDATE `sys_role` SET `data_scope` = 2, `name` = 'EOMS 主管', `description` = '运维报修主管：本部门及下级数据'
WHERE `code` = 'eoms_manager' AND `data_scope` NOT IN (1);

UPDATE `sys_role` SET `data_scope` = 4, `name` = 'EOMS 工程师', `description` = '运维工程师：仅本人创建或派给自己的工单'
WHERE `code` = 'eoms_engineer' AND `data_scope` NOT IN (1);

UPDATE `sys_role` SET `data_scope` = 3, `name` = 'EOMS 财务', `description` = '运维财务：本部门结算/应收/收款'
WHERE `code` = 'eoms_finance' AND `data_scope` NOT IN (1);

-- changeset ls:eoms_role_data_scope_menus validCheckSum:ANY
-- comment EOMS 三角色绑定默认菜单（幂等）
SET NAMES utf8mb4;

-- 主管：运维报修全树（现网仍保留的页面 + 按钮）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT `id` FROM `sys_menu`
    WHERE `id` IN (20000, 20010, 20011, 20012, 20013, 20032, 20040, 20041, 20042, 20050, 20070)
       OR (`id` BETWEEN 20101 AND 20199)
       OR (`id` BETWEEN 20111 AND 20120)
) m
WHERE r.`code` = 'eoms_manager';

-- 工程师：工单 + 维修记录 + 对应按钮
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT `id` FROM `sys_menu`
    WHERE `id` IN (20000, 20011, 20012)
       OR (`id` BETWEEN 20111 AND 20120)
       OR (`id` BETWEEN 20121 AND 20129)
) m
WHERE r.`code` = 'eoms_engineer';

-- 财务：结算 + 应收/收款/利润 + 对应按钮
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT `id` FROM `sys_menu`
    WHERE `id` IN (20000, 20013, 20040, 20041, 20042)
       OR (`id` BETWEEN 20131 AND 20139)
       OR (`id` BETWEEN 20151 AND 20159)
       OR (`id` BETWEEN 20161 AND 20169)
       OR (`id` BETWEEN 20171 AND 20179)
) m
WHERE r.`code` = 'eoms_finance';
