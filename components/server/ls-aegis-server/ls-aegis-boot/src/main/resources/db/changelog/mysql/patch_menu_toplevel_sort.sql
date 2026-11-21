-- liquibase formatted sql

-- changeset ls:menu_toplevel_sort_1_to_6
-- comment 一级目录菜单排序改为连续 1-6（系统管理/监控/租户/开放/调度/开发工具）
SET NAMES utf8mb4;

UPDATE `sys_menu` SET `sort` = 1 WHERE `id` = 1000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2 WHERE `id` = 2000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 3000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 7000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 8000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 9000 AND `deleted` = 0;
