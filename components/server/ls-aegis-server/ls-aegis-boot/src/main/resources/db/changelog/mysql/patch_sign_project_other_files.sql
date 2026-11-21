-- liquibase formatted sql

-- changeset ls:sign_project_other_files validCheckSum:ANY
-- comment 签章项目支持其他文件附件（sys_file.id 逗号分隔），可与关联合同二选一或并存
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'other_file_ids'

ALTER TABLE `sign_project`
    ADD COLUMN `other_file_ids` varchar(1000) DEFAULT NULL COMMENT '其他文件：平台sys_file.id，逗号分隔' AFTER `repair_partner_name`;
