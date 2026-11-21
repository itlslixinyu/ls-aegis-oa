-- liquibase formatted sql

-- changeset ls:m1_eoms_contract_file_id
-- comment M1.2：合同附件改为 eoms_contract.file_id 指向 sys_file
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_contract' AND COLUMN_NAME = 'file_id'
SET NAMES utf8mb4;

ALTER TABLE `eoms_contract`
  ADD COLUMN `file_id` bigint(20) DEFAULT NULL COMMENT '平台 sys_file.id' AFTER `remark`,
  ADD KEY `idx_ec_file` (`file_id`);

-- changeset ls:m1_eoms_attachment_migrate
-- comment M1.2：合同桥表记录迁入 file_id 后归档 eoms_attachment
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_attachment'
SET NAMES utf8mb4;

UPDATE `eoms_contract` c
INNER JOIN `eoms_attachment` a
  ON a.`business_id` = c.`id`
 AND LOWER(a.`business_type`) = 'contract'
 AND a.`file_path` REGEXP '^[0-9]+$'
SET c.`file_id` = CAST(a.`file_path` AS UNSIGNED)
WHERE c.`file_id` IS NULL;

UPDATE `eoms_contract` c
INNER JOIN `eoms_attachment` a
  ON a.`business_id` = c.`id`
 AND LOWER(a.`business_type`) = 'contract'
 AND a.`file_path` LIKE 'sys_file:%'
SET c.`file_id` = CAST(SUBSTRING(a.`file_path`, 10) AS UNSIGNED)
WHERE c.`file_id` IS NULL;

DROP TABLE IF EXISTS `eoms_attachment_archived`;
RENAME TABLE `eoms_attachment` TO `eoms_attachment_archived`;
-- 归档表中仍为 /profile/... 的本地路径不自动迁入 sys_file，需人工补传或忽略。

-- changeset ls:m1_eoms_attachment_menu
-- comment M1.2：下线运维附件管理菜单 20061
SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` = 20061;
DELETE FROM `sys_menu` WHERE `id` = 20061;
