-- liquibase formatted sql
-- changeset ls:site_title_short_maxlen_8
-- comment 系统名称简称最多 8 个汉字
UPDATE `sys_option`
SET `description` = '显示在左侧栏 Logo 旁的系统简称（必填，最多 8 个字）'
WHERE `code` = 'SITE_TITLE_SHORT';