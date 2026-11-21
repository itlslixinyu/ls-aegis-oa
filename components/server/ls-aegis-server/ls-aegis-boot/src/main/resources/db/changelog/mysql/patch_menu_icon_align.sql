-- liquibase formatted sql

-- changeset aegis:menu-icon-align validCheckSum:ANY
-- comment 侧栏图标：补缺失 SVG、校正语义撞车（均使用 assets/icons 中已有单色图标）
SET NAMES utf8mb4;

-- 利润分析：bar-chart 文件不存在，侧栏空白
UPDATE `sys_menu` SET `icon` = 'profit-model-alt', `update_time` = NOW() WHERE `id` = 20042 AND `deleted` = 0;

-- 报价管理：有专用 quote，避免与合同/文件共用 file
UPDATE `sys_menu` SET `icon` = 'quote', `update_time` = NOW() WHERE `id` = 40000 AND `deleted` = 0;

-- 财务管理：stamp 与电子签章撞车
UPDATE `sys_menu` SET `icon` = 'insert-chart', `update_time` = NOW() WHERE `id` = 10003 AND `deleted` = 0;

-- 项目管理：有专用 project；基础数据仍用 folder
UPDATE `sys_menu` SET `icon` = 'project', `update_time` = NOW() WHERE `id` = 30100 AND `deleted` = 0;

-- 套餐管理：把 project 让给项目管理
UPDATE `sys_menu` SET `icon` = 'gift', `update_time` = NOW() WHERE `id` = 3020 AND `deleted` = 0;

-- 采购合同：bookmark 是字典图标
UPDATE `sys_menu` SET `icon` = 'drive-file', `update_time` = NOW() WHERE `id` = 10180 AND `deleted` = 0;

-- 报修申请：file 与合同/文件管理撞车
UPDATE `sys_menu` SET `icon` = 'customer-service', `update_time` = NOW() WHERE `id` = 20010 AND `deleted` = 0;

-- 维修结算：stamp 与印章撞车
UPDATE `sys_menu` SET `icon` = 'check-square', `update_time` = NOW() WHERE `id` = 20013 AND `deleted` = 0;
