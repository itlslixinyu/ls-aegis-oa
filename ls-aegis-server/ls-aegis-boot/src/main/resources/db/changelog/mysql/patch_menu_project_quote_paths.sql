-- liquibase formatted sql

-- changeset ls:menu_project_quote_paths
-- comment 项目管理路径与报价对齐：目录 /sign/project，列表 /sign/project/index，避免被电子签章 /sign 抢走
SET NAMES utf8mb4;

UPDATE `sys_menu`
SET `path` = '/sign/project',
    `redirect` = '/sign/project/index',
    `update_time` = NOW()
WHERE `id` = 30100 AND `deleted` = 0;

UPDATE `sys_menu`
SET `path` = '/sign/project/index',
    `update_time` = NOW()
WHERE `id` = 30004 AND `deleted` = 0;
