-- liquibase formatted sql

-- changeset ls:module_tenant_default_off
-- comment 租户管理产品开关默认改为关闭（与中间件 CONTINEW_STARTER_TENANT_ENABLED 默认 false 对齐）
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `default_value` = '0',
    `value` = '0',
    `description` = '默认关闭。开启后显示租户管理菜单与登录租户编码框；关闭则隐藏菜单、拦截相关接口。完整启用还须 CONTINEW_STARTER_TENANT_ENABLED=true。'
WHERE `code` = 'MODULE_TENANT_ENABLED';
