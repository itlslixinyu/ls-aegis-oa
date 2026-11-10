-- liquibase formatted sql

-- changeset ls:module_tenant_option_desc
-- comment 租户模块开关说明：同步控制登录页租户编码框
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `description` = '关闭后隐藏租户管理菜单、拦截相关接口，且登录页不显示租户编码框。中间件是否装载由 continew-starter.tenant.enabled 控制。'
WHERE `code` = 'MODULE_TENANT_ENABLED';
