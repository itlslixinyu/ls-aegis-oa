-- liquibase formatted sql

-- changeset psi:7 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_out' AND COLUMN_NAME = 'pay_status'
-- comment 出库单补支付状态字段（核销回写）；列已存在则跳过

ALTER TABLE `psi_stock_out`
    ADD COLUMN `pay_status` varchar(16) NOT NULL DEFAULT 'UNPAID' COMMENT '回款状态' AFTER `invoice_status`;
