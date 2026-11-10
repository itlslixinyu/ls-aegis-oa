-- liquibase formatted sql

-- changeset ls:site_title_short
-- comment 系统名称简称：侧栏 Logo 旁展示，最多 6 字
INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 18, 'SITE', '系统名称简称', 'SITE_TITLE_SHORT', NULL, '雷铄御警', '显示在左侧栏 Logo 旁的系统简称（必填，最多 6 个字）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'SITE_TITLE_SHORT');
