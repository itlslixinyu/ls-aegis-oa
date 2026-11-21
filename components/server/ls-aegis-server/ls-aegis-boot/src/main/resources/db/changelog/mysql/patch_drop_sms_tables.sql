-- liquibase formatted sql

-- changeset ls:drop_sms_tables
-- comment 短信模块已移除：删除孤儿表 sys_sms_log / sys_sms_config
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `sys_sms_log`;
DROP TABLE IF EXISTS `sys_sms_config`;
