-- liquibase formatted sql

-- changeset psi:145 validCheckSum:ANY
-- comment PSI：侧栏改为两层（去掉 PSI业务；四大模块升为顶层 Layout；工作台挂到基础数据）

-- 四大模块升为顶层目录（需 Layout 承载页面壳）
UPDATE `sys_menu`
SET `parent_id` = 0,
    `component` = 'Layout',
    `sort` = 2,
    `redirect` = '/psi/workbench',
    `update_time` = NOW()
WHERE `id` = 10001;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `component` = 'Layout',
    `sort` = 3,
    `redirect` = '/psi/stock-in',
    `update_time` = NOW()
WHERE `id` = 10002;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `component` = 'Layout',
    `sort` = 4,
    `redirect` = '/psi/receipt',
    `update_time` = NOW()
WHERE `id` = 10003;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `component` = 'Layout',
    `sort` = 5,
    `redirect` = '/psi/contract-sale',
    `update_time` = NOW()
WHERE `id` = 10004;

-- 原挂在 PSI业务 下的工作台，改挂基础数据
UPDATE `sys_menu`
SET `parent_id` = 10001,
    `sort` = 0,
    `update_time` = NOW()
WHERE `id` = 10185;

-- 角色若只勾了「PSI业务」目录，补绑四大模块，避免升层后丢入口
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.`role_id`, m.`id`
FROM `sys_role_menu` rm
CROSS JOIN (
    SELECT 10001 AS id UNION ALL SELECT 10002 UNION ALL SELECT 10003 UNION ALL SELECT 10004
) m
WHERE rm.`menu_id` = 10000;

DELETE FROM `sys_role_menu` WHERE `menu_id` = 10000;
DELETE FROM `sys_menu` WHERE `id` = 10000;
