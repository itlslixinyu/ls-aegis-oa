-- liquibase formatted sql

-- changeset psi:148 validCheckSum:ANY
-- comment PSI/系统：顶栏菜单排序 仪表盘(前端) → 合同 → 库存 → 财务 → 基础数据 → 系统管理 → 系统监控

UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 10004;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 10002;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 10003;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 10001;
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 1000;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 2000;
