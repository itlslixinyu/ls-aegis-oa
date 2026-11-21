-- liquibase formatted sql

-- changeset ls:sign_project_contract_bind validCheckSum:ANY
-- comment 项目绑定一份合同（销售/采购/维修），存类型、ID 与编号/往来单位快照
SET NAMES utf8mb4;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'contract_type') = 0,
    'ALTER TABLE `sign_project` ADD COLUMN `contract_type` varchar(16) DEFAULT NULL COMMENT ''绑定合同类型：SALE销售/PURCHASE采购/REPAIR维修'' AFTER `description`, ADD COLUMN `contract_id` bigint DEFAULT NULL COMMENT ''绑定合同ID'' AFTER `contract_type`, ADD COLUMN `contract_no` varchar(64) DEFAULT NULL COMMENT ''合同编号快照'' AFTER `contract_id`, ADD COLUMN `contract_name` varchar(200) DEFAULT NULL COMMENT ''合同名称快照'' AFTER `contract_no`, ADD COLUMN `partner_name` varchar(200) DEFAULT NULL COMMENT ''往来单位快照'' AFTER `contract_name`, ADD KEY `idx_sign_project_contract` (`contract_type`, `contract_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
