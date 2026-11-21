-- liquibase formatted sql

-- changeset ls:eoms_receivable_no validCheckSum:ANY
-- comment 维修应收表补 receivable_no（代码/编号生成器/列表筛选依赖，建表种子遗漏）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_receivable' AND COLUMN_NAME = 'receivable_no'

SET NAMES utf8mb4;

ALTER TABLE `eoms_receivable`
    ADD COLUMN `receivable_no` varchar(32) DEFAULT NULL COMMENT '应收单号（YS前缀）' AFTER `id`,
    ADD UNIQUE KEY `uk_receivable_no` (`receivable_no`);

-- 存量无单号：按创建日 + id 回填
UPDATE `eoms_receivable`
SET `receivable_no` = CONCAT(
        'YS',
        DATE_FORMAT(COALESCE(`create_time`, NOW()), '%Y%m%d'),
        LPAD(`id` % 1000, 3, '0')
    )
WHERE `receivable_no` IS NULL OR `receivable_no` = '';
