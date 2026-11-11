-- liquibase formatted sql

-- changeset ls:oa_site_identity_20261111
-- comment 用户确认的平台名称；保留自定义覆盖值、版权和备案信息。
UPDATE sys_option SET default_value='LS·Aegis 雷铄 OA 安全应用构建平台' WHERE code IN ('SITE_TITLE','SITE_DESCRIPTION');
UPDATE sys_option SET default_value='ls·aegis·oa', description='显示在左侧栏 Logo 旁的系统简称（必填，最多 32 个字符）' WHERE code='SITE_TITLE_SHORT';
