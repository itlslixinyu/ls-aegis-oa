-- liquibase formatted sql

-- changeset psi:menu-move-master-to-stock validCheckSum:ANY
-- comment PSI：商品分类/商品档案/仓库管理/库存查询 从基础数据移至库存业务

-- 库存业务目录 redirect 指向首个子菜单
UPDATE `sys_menu`
SET `redirect` = '/psi/goods-category',
    `update_time` = NOW()
WHERE `id` = 10002;

-- 四菜单挂到库存业务，排在出入库等业务单据之前
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 1, `update_time` = NOW() WHERE `id` = 10310;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 2, `update_time` = NOW() WHERE `id` = 10020;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 3, `update_time` = NOW() WHERE `id` = 10030;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 4, `update_time` = NOW() WHERE `id` = 10210;

-- 原库存业务单据顺延
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 10040 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10050 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 7, `update_time` = NOW() WHERE `id` = 10060 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 8, `update_time` = NOW() WHERE `id` = 10070 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 9, `update_time` = NOW() WHERE `id` = 10090 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 10, `update_time` = NOW() WHERE `id` = 10100 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 11, `update_time` = NOW() WHERE `id` = 10190 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 12, `update_time` = NOW() WHERE `id` = 10195 AND `parent_id` = 10002;

-- 基础数据仅保留往来单位、期初建账
UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 10010 AND `parent_id` = 10001;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 10080 AND `parent_id` = 10001;
