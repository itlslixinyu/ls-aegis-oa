-- liquibase formatted sql

-- changeset ls:r27_message_inbox_delete_pgsql dbms:postgresql
-- comment 个人消息删除墓碑：保留主消息与其他接收人的已读记录
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='sys_message_log' AND column_name='is_deleted'

ALTER TABLE sys_message_log ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN sys_message_log.is_deleted IS '个人收件箱已删除';
