-- liquibase formatted sql

-- changeset ls:sign_project_multi_contract validCheckSum:ANY
-- comment 项目可同时绑定销售/采购/维修各一份合同；存量从 contract_type+contract_id 迁移
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'sale_contract_id'

ALTER TABLE `sign_project`
    ADD COLUMN `sale_contract_id` bigint DEFAULT NULL COMMENT '销售合同ID' AFTER `partner_name`,
    ADD COLUMN `sale_contract_no` varchar(64) DEFAULT NULL COMMENT '销售合同编号快照' AFTER `sale_contract_id`,
    ADD COLUMN `sale_contract_name` varchar(200) DEFAULT NULL COMMENT '销售合同名称快照' AFTER `sale_contract_no`,
    ADD COLUMN `sale_partner_name` varchar(200) DEFAULT NULL COMMENT '销售往来单位快照' AFTER `sale_contract_name`,
    ADD COLUMN `purchase_contract_id` bigint DEFAULT NULL COMMENT '采购合同ID' AFTER `sale_partner_name`,
    ADD COLUMN `purchase_contract_no` varchar(64) DEFAULT NULL COMMENT '采购合同编号快照' AFTER `purchase_contract_id`,
    ADD COLUMN `purchase_contract_name` varchar(200) DEFAULT NULL COMMENT '采购合同名称快照' AFTER `purchase_contract_no`,
    ADD COLUMN `purchase_partner_name` varchar(200) DEFAULT NULL COMMENT '采购往来单位快照' AFTER `purchase_contract_name`,
    ADD COLUMN `repair_contract_id` bigint DEFAULT NULL COMMENT '维修合同ID' AFTER `purchase_partner_name`,
    ADD COLUMN `repair_contract_no` varchar(64) DEFAULT NULL COMMENT '维修合同编号快照' AFTER `repair_contract_id`,
    ADD COLUMN `repair_contract_name` varchar(200) DEFAULT NULL COMMENT '维修合同名称快照' AFTER `repair_contract_no`,
    ADD COLUMN `repair_partner_name` varchar(200) DEFAULT NULL COMMENT '维修往来单位快照' AFTER `repair_contract_name`,
    ADD KEY `idx_sign_project_sale_contract` (`sale_contract_id`),
    ADD KEY `idx_sign_project_purchase_contract` (`purchase_contract_id`),
    ADD KEY `idx_sign_project_repair_contract` (`repair_contract_id`);

-- 存量：原「三选一」落到对应类型列
UPDATE `sign_project`
SET `sale_contract_id` = `contract_id`,
    `sale_contract_no` = `contract_no`,
    `sale_contract_name` = `contract_name`,
    `sale_partner_name` = `partner_name`
WHERE `del_flag` = '0' AND `contract_type` = 'SALE' AND `contract_id` IS NOT NULL;

UPDATE `sign_project`
SET `purchase_contract_id` = `contract_id`,
    `purchase_contract_no` = `contract_no`,
    `purchase_contract_name` = `contract_name`,
    `purchase_partner_name` = `partner_name`
WHERE `del_flag` = '0' AND `contract_type` = 'PURCHASE' AND `contract_id` IS NOT NULL;

UPDATE `sign_project`
SET `repair_contract_id` = `contract_id`,
    `repair_contract_no` = `contract_no`,
    `repair_contract_name` = `contract_name`,
    `repair_partner_name` = `partner_name`
WHERE `del_flag` = '0' AND `contract_type` = 'REPAIR' AND `contract_id` IS NOT NULL;
