-- liquibase formatted sql

-- changeset psi:contract_file_ids_varchar validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_purchase' AND COLUMN_NAME = 'file_id' AND DATA_TYPE = 'bigint'
-- comment 采购合同附件支持多文件：file_id 改为逗号分隔字符串
ALTER TABLE `psi_contract_purchase`
    MODIFY COLUMN `file_id` varchar(500) DEFAULT NULL COMMENT '附件文件ID，多个逗号分隔';

-- changeset psi:contract_sale_file_ids_varchar validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_sale' AND COLUMN_NAME = 'file_id' AND DATA_TYPE = 'bigint'
-- comment 销售合同附件支持多文件：file_id 改为逗号分隔字符串
ALTER TABLE `psi_contract_sale`
    MODIFY COLUMN `file_id` varchar(500) DEFAULT NULL COMMENT '附件文件ID，多个逗号分隔';

-- changeset eoms:contract_file_ids_varchar validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_contract' AND COLUMN_NAME = 'file_id' AND DATA_TYPE = 'bigint'
-- comment 运维合同附件支持多文件：file_id 改为逗号分隔字符串
ALTER TABLE `eoms_contract`
    MODIFY COLUMN `file_id` varchar(500) DEFAULT NULL COMMENT '附件文件ID，多个逗号分隔';
