-- liquibase formatted sql

-- changeset ls:eoms_payment_no validCheckSum:ANY
-- comment 维修收款表补 payment_no / batch_id（核销写库/编号生成器依赖，建表种子遗漏）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_payment' AND COLUMN_NAME = 'payment_no'

SET NAMES utf8mb4;

ALTER TABLE `eoms_payment`
    ADD COLUMN `payment_no` varchar(32) DEFAULT NULL COMMENT '收款单号（SK前缀）' AFTER `receivable_id`,
    ADD COLUMN `batch_id` varchar(64) DEFAULT NULL COMMENT '批量核销批次ID' AFTER `payment_no`,
    ADD KEY `idx_payment_no` (`payment_no`),
    ADD KEY `idx_batch_id` (`batch_id`);

-- 存量无单号：按创建日 + id 回填
UPDATE `eoms_payment`
SET `payment_no` = CONCAT(
        'SK',
        DATE_FORMAT(COALESCE(`create_time`, NOW()), '%Y%m%d'),
        LPAD(`id` % 1000, 3, '0')
    )
WHERE `payment_no` IS NULL OR `payment_no` = '';
