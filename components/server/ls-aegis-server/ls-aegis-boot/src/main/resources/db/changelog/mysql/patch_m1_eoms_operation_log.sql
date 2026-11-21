-- liquibase formatted sql

-- changeset ls:m1_eoms_operation_log_archive
-- comment M1.4：归档 eoms_operation_log，HTTP 操作改记平台 sys_log
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_operation_log'
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `eoms_operation_log_archived`;
RENAME TABLE `eoms_operation_log` TO `eoms_operation_log_archived`;

-- changeset ls:m1_eoms_operation_log_menu
-- comment M1.4：下线运维日志菜单 20062
SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` = 20062;
DELETE FROM `sys_menu` WHERE `id` = 20062;
