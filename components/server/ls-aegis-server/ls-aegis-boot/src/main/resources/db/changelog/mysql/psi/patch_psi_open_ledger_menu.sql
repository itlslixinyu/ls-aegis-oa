-- liquibase formatted sql

-- changeset psi:8 validCheckSum:ANY
-- comment PSI：应收/应付台账菜单 + 银行账户增删改权限

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10125, '应收台账', 10003, 2, '/psi/receivable', 'PsiReceivable', 'psi/receivable/index', NULL, 'list', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10125);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10126, '列表', 10125, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receivable:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10126);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10127, '应付台账', 10003, 2, '/psi/payable', 'PsiPayable', 'psi/payable/index', NULL, 'ordered-list', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10127);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10128, '列表', 10127, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payable:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10128);

-- 银行账户增删改权限
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10152, '新增', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10152);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10153, '修改', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:update', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10153);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10154, '删除', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:delete', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10154);

-- 回款/付款菜单排序后移（应收应付插入 3/4，原回款等顺延）
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 10130 AND `sort` < 5;
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 10140 AND `sort` < 6;
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 10150 AND `sort` < 7;
UPDATE `sys_menu` SET `sort` = 8 WHERE `id` = 10160 AND `sort` < 8;
