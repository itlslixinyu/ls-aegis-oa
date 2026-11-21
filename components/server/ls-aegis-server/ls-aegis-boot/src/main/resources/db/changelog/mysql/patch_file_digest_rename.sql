-- liquibase formatted sql

-- changeset ls:rename_sys_file_sha256_to_file_digest
-- comment 文件表指纹列 sha256 → file_digest（国密下存 SM3；语义为文件指纹）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file' AND COLUMN_NAME = 'sha256'
SET NAMES utf8mb4;

ALTER TABLE `sys_file`
  CHANGE COLUMN `sha256` `file_digest` varchar(256) DEFAULT NULL COMMENT '文件指纹（国密下为 SM3；普通上传可能为存储引擎 SHA256）';

-- changeset ls:rename_sys_file_idx_sha256_to_file_digest
-- comment 同步重命名文件指纹索引
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_file' AND INDEX_NAME = 'idx_sha256'
ALTER TABLE `sys_file` RENAME INDEX `idx_sha256` TO `idx_file_digest`;
