-- liquibase formatted sql

-- changeset psi:15 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_sale' AND COLUMN_NAME = 'confirm_user'
-- comment 销售合同确认人/时间（列已存在则跳过）
ALTER TABLE `psi_contract_sale`
    ADD COLUMN `confirm_user` bigint(20) DEFAULT NULL COMMENT '确认人' AFTER `remark`,
    ADD COLUMN `confirm_time` datetime DEFAULT NULL COMMENT '确认时间' AFTER `confirm_user`;

-- changeset psi:16 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_purchase' AND COLUMN_NAME = 'confirm_user'
-- comment 采购合同确认人/时间（列已存在则跳过）
ALTER TABLE `psi_contract_purchase`
    ADD COLUMN `confirm_user` bigint(20) DEFAULT NULL COMMENT '确认人' AFTER `remark`,
    ADD COLUMN `confirm_time` datetime DEFAULT NULL COMMENT '确认时间' AFTER `confirm_user`;
