-- liquibase formatted sql

-- changeset psi:17 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_company_account' AND COLUMN_NAME = 'account_no' AND CHARACTER_MAXIMUM_LENGTH < 255
-- comment 公司账户账号列扩宽至 255，容纳 SM4 Base64 密文
ALTER TABLE `psi_company_account`
    MODIFY COLUMN `account_no` varchar(255) NOT NULL COMMENT '账号（库内 SM4）';

-- changeset psi:18 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_unit_pay' AND COLUMN_NAME = 'account_no' AND CHARACTER_MAXIMUM_LENGTH < 255
-- comment 单位收款账号列扩宽至 255
ALTER TABLE `psi_unit_pay`
    MODIFY COLUMN `account_no` varchar(255) NOT NULL COMMENT '账号（库内 SM4）';

-- changeset psi:19 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_unit_invoice' AND COLUMN_NAME = 'account_no' AND CHARACTER_MAXIMUM_LENGTH < 255
-- comment 单位开票账号列扩宽至 255
ALTER TABLE `psi_unit_invoice`
    MODIFY COLUMN `account_no` varchar(255) DEFAULT NULL COMMENT '账号（库内 SM4）';
