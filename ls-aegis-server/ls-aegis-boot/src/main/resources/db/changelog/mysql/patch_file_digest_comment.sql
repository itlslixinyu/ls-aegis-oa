-- liquibase formatted sql

-- changeset ls:file_digest_comment_sm3_only
-- comment 纠正 file_digest 列注释：指纹统一为 SM3（不再提及存储引擎 SHA256）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file' AND COLUMN_NAME = 'file_digest'
SET NAMES utf8mb4;

ALTER TABLE `sys_file`
  MODIFY COLUMN `file_digest` varchar(256) DEFAULT NULL COMMENT '文件指纹（统一 SM3）';
