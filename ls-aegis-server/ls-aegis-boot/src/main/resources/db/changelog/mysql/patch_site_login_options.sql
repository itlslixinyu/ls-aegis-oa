-- liquibase formatted sql

-- changeset ls:site_login_branding_options
-- comment 登录页站点参数：公司名、公安备案号、公安备案图标
INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 7, 'SITE', '公司名称', 'SITE_COMPANY', NULL, '雷铄科技', '显示在登录页顶栏的公司名称'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'SITE_COMPANY');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 8, 'SITE', '公安备案号', 'SITE_BEIAN_GONGAN', NULL, '冀公网安备 00000000000000 号', '显示在登录页底部的公安备案编号'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'SITE_BEIAN_GONGAN');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 9, 'SITE', '公安备案图标', 'SITE_BEIAN_GONGAN_ICON', NULL, '/beian-gongan.png', '公安备案号左侧图标（建议小尺寸 PNG）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'SITE_BEIAN_GONGAN_ICON');

UPDATE `sys_option`
SET `name` = 'ICP备案号',
    `description` = '工信部 ICP 备案编号（如：冀ICP备00000000号）'
WHERE `code` = 'SITE_BEIAN';

UPDATE `sys_option`
SET `default_value` = '冀ICP备 00000000 号'
WHERE `code` = 'SITE_BEIAN' AND (`default_value` IS NULL OR `default_value` = '');

UPDATE `sys_option`
SET `default_value` = 'LS-Aegis 雷铄御警安全应用构建平台',
    `description` = '显示在浏览器标题栏和登录页顶栏的产品/项目名称'
WHERE `code` = 'SITE_TITLE';

UPDATE `sys_option`
SET `default_value` = '© 2026 雷铄科技 · LS-Aegis 雷铄御警安全应用构建平台'
WHERE `code` = 'SITE_COPYRIGHT';
