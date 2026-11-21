-- liquibase formatted sql

-- changeset psi:43 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_bank_statement' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限补齐：psi_bank_statement.dept_id
ALTER TABLE `psi_bank_statement`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_bank_statement` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:44 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_todo' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限补齐：psi_todo.dept_id
ALTER TABLE `psi_todo`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_todo` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:45 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_flow' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限补齐：psi_stock_flow.dept_id（详情防 IDOR）
ALTER TABLE `psi_stock_flow`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_stock_flow` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:46 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_print_template' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限补齐：psi_print_template.dept_id
ALTER TABLE `psi_print_template`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_print_template` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- changeset psi:47 validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_goods_category' AND COLUMN_NAME = 'dept_id'
-- comment 数据权限补齐：psi_goods_category.dept_id
ALTER TABLE `psi_goods_category`
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `tenant_id`;
UPDATE `psi_goods_category` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;
