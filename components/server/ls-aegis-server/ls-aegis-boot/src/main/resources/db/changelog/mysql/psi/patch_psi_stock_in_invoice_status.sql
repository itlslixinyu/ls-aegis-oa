-- liquibase formatted sql

-- changeset psi:14 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_in' AND COLUMN_NAME = 'invoice_status'
-- comment 入库单补开票状态字段（进项发票回写）；列已存在则跳过

ALTER TABLE `psi_stock_in`
    ADD COLUMN `invoice_status` varchar(16) NOT NULL DEFAULT 'UNINVOICED' COMMENT '开票状态' AFTER `pay_status`;
