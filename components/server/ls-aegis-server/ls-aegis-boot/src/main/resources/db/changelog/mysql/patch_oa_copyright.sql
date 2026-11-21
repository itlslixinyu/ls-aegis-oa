-- liquibase formatted sql

-- changeset ls:oa_copyright_20261111
-- comment 用户确认版权文字；只更新默认值，保留现有用户覆盖配置。
UPDATE sys_option SET default_value='LS·Aegis 雷铄 OA 安全应用构建平台' WHERE code='SITE_COPYRIGHT';
