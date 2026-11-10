-- liquibase formatted sql

-- changeset ls:site_company_short
-- comment 公司简称：网站配置表单与系统名称简称对称展示
INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 19, 'SITE', '公司简称', 'SITE_COMPANY_SHORT', NULL, '雷铄', '公司名称简称（选填，最多 8 个字）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'SITE_COMPANY_SHORT');
