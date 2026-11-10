-- liquibase formatted sql

-- changeset psi:20 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_in' AND COLUMN_NAME = 'dept_id'
-- comment 入库单补部门字段（数据权限试点）
ALTER TABLE `psi_stock_in`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_stock_in` t
    LEFT JOIN `sys_user` u ON u.id = t.create_user
SET t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.deleted = 0;

-- changeset psi:21 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_receipt' AND COLUMN_NAME = 'dept_id'
-- comment 回款单补部门字段（数据权限试点）
ALTER TABLE `psi_receipt`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_receipt` t
    LEFT JOIN `sys_user` u ON u.id = t.create_user
SET t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.deleted = 0;

-- changeset psi:22 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_sale' AND COLUMN_NAME = 'dept_id'
-- comment 销售合同补部门字段（数据权限试点）
ALTER TABLE `psi_contract_sale`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_contract_sale` t
    LEFT JOIN `sys_user` u ON u.id = t.create_user
SET t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.deleted = 0;
