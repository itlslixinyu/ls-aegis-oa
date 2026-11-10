-- liquibase formatted sql

-- changeset ls:r27_message_inbox_delete_mysql dbms:mysql
-- comment 个人消息删除墓碑：保留主消息与其他接收人的已读记录
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_message_log' AND COLUMN_NAME='is_deleted'

ALTER TABLE sys_message_log ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '个人收件箱已删除';
