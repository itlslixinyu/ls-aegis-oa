-- liquibase formatted sql

-- changeset ls:quote_single_track
-- comment 平台一体 P1：停用 eoms 报价菜单 20033，重建 40000 段 quote:* 菜单与按钮权限
SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` = 20033;
UPDATE `sys_menu`
SET `deleted` = `id`, `update_user` = 1, `update_time` = NOW()
WHERE `id` = 20033 AND `deleted` = 0;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 40000 AND 40999;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 40000 AND 40999;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(40000, '报价管理', 0, 1, '/quote', 'QuoteRoot', 'Layout', '/quote/index', 'file', b'0', b'0', b'0', NULL, 7, 1, 1, NOW()),
(40001, '报价列表', 40000, 2, '/quote/index', 'QuoteIndex', 'quote/index', NULL, 'list', b'0', b'0', b'0', 'quote:list', 1, 1, 1, NOW()),
(40002, '报价编辑', 40000, 2, '/quote/editor', 'QuoteEditor', 'quote/editor', NULL, 'edit', b'0', b'0', b'0', 'quote:editor', 2, 1, 1, NOW()),
(40003, '历史报价', 40000, 2, '/quote/history', 'QuoteHistory', 'quote/history', NULL, 'history', b'0', b'0', b'0', 'quote:history', 3, 1, 1, NOW()),
(40004, '报价设置', 40000, 2, '/quote/settings', 'QuoteSettings', 'quote/settings', NULL, 'settings', b'0', b'0', b'0', 'quote:settings', 4, 1, 1, NOW()),
(40010, '查询', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:query', 1, 1, 1, NOW()),
(40011, '新增', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:add', 2, 1, 1, NOW()),
(40012, '修改', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:update', 3, 1, 1, NOW()),
(40013, '删除', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:delete', 4, 1, 1, NOW()),
(40014, '导出', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:export', 5, 1, 1, NOW());

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (SELECT `id` FROM `sys_menu` WHERE `id` BETWEEN 40000 AND 40999) m
WHERE r.id IN (1, 3);
