-- liquibase formatted sql

-- changeset ls:project_contract_bind validCheckSum:ANY
-- comment 三类合同表增加可空 project_id，便于从合同反查所属项目（主绑定仍在 sign_project）
SET NAMES utf8mb4;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_sale' AND COLUMN_NAME = 'project_id') = 0,
    'ALTER TABLE `psi_contract_sale` ADD COLUMN `project_id` bigint(20) DEFAULT NULL COMMENT ''所属项目'' AFTER `unit_id`, ADD INDEX `idx_cs_project` (`project_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_purchase' AND COLUMN_NAME = 'project_id') = 0,
    'ALTER TABLE `psi_contract_purchase` ADD COLUMN `project_id` bigint(20) DEFAULT NULL COMMENT ''所属项目'' AFTER `unit_id`, ADD INDEX `idx_cp_project` (`project_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_contract' AND COLUMN_NAME = 'project_id') = 0,
    'ALTER TABLE `eoms_contract` ADD COLUMN `project_id` bigint(20) DEFAULT NULL COMMENT ''所属项目'' AFTER `customer_id`, ADD INDEX `idx_ec_project` (`project_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
