-- liquibase formatted sql

-- changeset aegis:patch_config_tab_menu_path validCheckSum:ANY
-- comment 恢复系统配置子菜单 path 的 tab 查询串，与种子一致（路由由前端去重，避免页套页空白）
SET NAMES utf8mb4;

-- CHAR(63) 为 '?'，避免 Liquibase 把问号当成绑定占位符
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=site') WHERE `id` = 1160 AND `path` = '/system/config';
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=security') WHERE `id` = 1170 AND `path` = '/system/config';
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=login') WHERE `id` = 1180 AND `path` = '/system/config';
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=mail') WHERE `id` = 1190 AND `path` = '/system/config';
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=weather') WHERE `id` = 1200 AND `path` = '/system/config';
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=dashboard') WHERE `id` = 1210 AND `path` = '/system/config';
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=module') WHERE `id` = 1220 AND `path` = '/system/config';
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=storage') WHERE `id` = 1230 AND `path` = '/system/config';
UPDATE `sys_menu` SET `path` = CONCAT('/system/config', CHAR(63), 'tab=client') WHERE `id` = 1250 AND `path` = '/system/config';
