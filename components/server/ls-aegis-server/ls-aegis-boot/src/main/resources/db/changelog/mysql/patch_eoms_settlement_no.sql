-- liquibase formatted sql

-- changeset ls:eoms_settlement_no validCheckSum:ANY
-- comment 维修结算表补 settlement_no（代码/利润分析/编号生成器依赖，建表种子遗漏）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_settlement' AND COLUMN_NAME = 'settlement_no'

SET NAMES utf8mb4;

ALTER TABLE `eoms_repair_settlement`
    ADD COLUMN `settlement_no` varchar(32) DEFAULT NULL COMMENT '结算单号（JS前缀）' AFTER `id`,
    ADD UNIQUE KEY `uk_settlement_no` (`settlement_no`);

-- 存量无单号：按创建日 + id 回填，避免利润分析/列表空号
UPDATE `eoms_repair_settlement`
SET `settlement_no` = CONCAT(
        'JS',
        DATE_FORMAT(COALESCE(`create_time`, NOW()), '%Y%m%d'),
        LPAD(`id` % 1000, 3, '0')
    )
WHERE `settlement_no` IS NULL OR `settlement_no` = '';
