-- liquibase formatted sql

-- changeset aegis:contract-menu-icon-mono validCheckSum:ANY
-- comment 合同管理菜单改用单色图标 file，避免彩色 menu-file

UPDATE `sys_menu` SET `icon` = 'file', `update_time` = NOW() WHERE `id` = 10004 AND `title` = '合同管理';
UPDATE `sys_menu` SET `icon` = 'file', `update_time` = NOW() WHERE `id` = 20032 AND `title` = '合同管理';
