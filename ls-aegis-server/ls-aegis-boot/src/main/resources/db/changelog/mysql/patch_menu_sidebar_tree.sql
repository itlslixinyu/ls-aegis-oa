-- liquibase formatted sql

-- changeset ls:menu_sidebar_tree validCheckSum:ANY
-- comment 侧栏完整树：归属/sort/隐藏重复；项目升顶层；报价桩下线；合同权限拆分
SET NAMES utf8mb4;

-- ========== 1) 报价半成品下线 ==========
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (40002, 40003, 40004);
DELETE FROM `sys_menu` WHERE `id` IN (40002, 40003, 40004);

-- ========== 2) 项目管理：升为顶层 Layout（30100）+ 原 30004 改为项目列表 ==========
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 30100, '项目管理', 0, 1, '/project', 'ProjectRoot', 'Layout', '/sign/project', 'folder', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 30100);

UPDATE `sys_menu`
SET `parent_id` = 30100,
    `title` = '项目列表',
    `sort` = 1,
    `update_time` = NOW()
WHERE `id` = 30004 AND `deleted` = 0;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 30100
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 30004;

-- ========== 3) 维修合同 → 合同管理；利润分析 → 财务管理 ==========
UPDATE `sys_menu`
SET `parent_id` = 10004,
    `title` = '维修合同',
    `sort` = 3,
    `is_hidden` = b'0',
    `update_time` = NOW()
WHERE `id` = 20032 AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 10003,
    `title` = '利润分析',
    `sort` = 7,
    `is_hidden` = b'0',
    `update_time` = NOW()
WHERE `id` = 20042 AND `deleted` = 0;

UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 10170 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 10180 AND `deleted` = 0;

UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 10110 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 10120 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 10125 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 10127 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 10130 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10140 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 8, `update_time` = NOW() WHERE `id` = 10150 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 9, `update_time` = NOW() WHERE `id` = 10160 AND `deleted` = 0;

-- ========== 4) 运维/签章：侧栏隐藏重复能力与看板 ==========
UPDATE `sys_menu`
SET `is_hidden` = b'1', `update_time` = NOW()
WHERE `id` IN (
    20001, 20020, 20021, 20022, 20023, 20024,
    20030, 20031, 20040, 20041, 20050,
    20060, 20061, 20062, 20070,
    30001
) AND `deleted` = 0;

UPDATE `sys_menu`
SET `redirect` = '/eoms/repair/apply', `update_time` = NOW()
WHERE `id` = 20000 AND `deleted` = 0;

UPDATE `sys_menu`
SET `redirect` = '/sign/seal/extract', `update_time` = NOW()
WHERE `id` = 30000 AND `deleted` = 0;

-- 运维可见页排序
UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 20010 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 20011 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 20012 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 20013 AND `deleted` = 0;

-- 电子签章：先提取后管理
UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 30003 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 30002 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 30005 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 30006 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 30008 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 30007 AND `deleted` = 0;

-- ========== 5) 租户子菜单改名 ==========
UPDATE `sys_menu` SET `title` = '租户列表', `update_time` = NOW() WHERE `id` = 3010 AND `deleted` = 0;

-- ========== 6) PSI 销售/采购合同权限拆分 ==========
UPDATE `sys_menu` SET `permission` = 'psi:contractSale:list', `update_time` = NOW() WHERE `id` = 10171 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractSale:create', `update_time` = NOW() WHERE `id` = 10172 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractSale:update', `update_time` = NOW() WHERE `id` = 10173 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractPurchase:list', `update_time` = NOW() WHERE `id` = 10181 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractPurchase:create', `update_time` = NOW() WHERE `id` = 10182 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractPurchase:update', `update_time` = NOW() WHERE `id` = 10183 AND `deleted` = 0;

-- ========== 7) 顶层 sort（仪表盘为前端固定第 1） ==========
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 40000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 10004 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 30100 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 10002 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10003 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 7, `update_time` = NOW() WHERE `id` = 20000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 8, `update_time` = NOW() WHERE `id` = 30000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 9, `update_time` = NOW() WHERE `id` = 10001 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 10, `update_time` = NOW() WHERE `id` = 1000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 11, `update_time` = NOW() WHERE `id` = 2000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 12, `update_time` = NOW() WHERE `id` = 3000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 13, `update_time` = NOW() WHERE `id` = 7000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 14, `update_time` = NOW() WHERE `id` = 8000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 15, `update_time` = NOW() WHERE `id` = 9000 AND `deleted` = 0;
