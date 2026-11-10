-- liquibase formatted sql

-- changeset psi:23 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_out' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_stock_out.dept_id
ALTER TABLE `psi_stock_out`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_stock_out` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:24 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_return' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_stock_return.dept_id
ALTER TABLE `psi_stock_return`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_stock_return` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:25 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_transfer' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_stock_transfer.dept_id
ALTER TABLE `psi_stock_transfer`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_stock_transfer` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:26 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_check' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_stock_check.dept_id
ALTER TABLE `psi_stock_check`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_stock_check` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:27 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_adjust' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_stock_adjust.dept_id
ALTER TABLE `psi_stock_adjust`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_stock_adjust` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:28 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_purchase' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_contract_purchase.dept_id
ALTER TABLE `psi_contract_purchase`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_contract_purchase` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:29 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_payment' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_payment.dept_id
ALTER TABLE `psi_payment`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_payment` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:30 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_invoice_out' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_invoice_out.dept_id
ALTER TABLE `psi_invoice_out`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_invoice_out` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:31 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_invoice_in' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_invoice_in.dept_id
ALTER TABLE `psi_invoice_in`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_invoice_in` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:32 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_write_off' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_write_off.dept_id
ALTER TABLE `psi_write_off`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_write_off` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:33 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_init_stock' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_init_stock.dept_id
ALTER TABLE `psi_init_stock`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_init_stock` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:34 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_init_receivable' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_init_receivable.dept_id
ALTER TABLE `psi_init_receivable`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_init_receivable` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:35 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_init_payable' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_init_payable.dept_id
ALTER TABLE `psi_init_payable`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_init_payable` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:36 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_init_prepay' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_init_prepay.dept_id
ALTER TABLE `psi_init_prepay`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_init_prepay` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:37 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_receivable' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_receivable.dept_id
ALTER TABLE `psi_receivable`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_receivable` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:38 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_payable' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_payable.dept_id
ALTER TABLE `psi_payable`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_payable` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:39 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_unit' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_unit.dept_id
ALTER TABLE `psi_unit`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_unit` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:40 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_goods' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_goods.dept_id
ALTER TABLE `psi_goods`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_goods` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:41 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_warehouse' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_warehouse.dept_id
ALTER TABLE `psi_warehouse`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_warehouse` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:42 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_company_account' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限铺开：psi_company_account.dept_id
ALTER TABLE `psi_company_account`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_company_account` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;
