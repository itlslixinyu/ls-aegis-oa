-- liquibase formatted sql

-- changeset ls:eoms_dp_m1_repair_apply validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_apply' AND COLUMN_NAME = 'dept_id'
-- comment EOMS 报修申请补租户/创建人/部门（DataScope M1）
SET NAMES utf8mb4;
ALTER TABLE `eoms_repair_apply`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `del_flag`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `create_user`;
UPDATE `eoms_repair_apply` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0';

-- changeset ls:eoms_dp_m1_repair_order validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_order' AND COLUMN_NAME = 'dept_id'
-- comment EOMS 维修工单补租户/创建人/部门（DataScope M1）
SET NAMES utf8mb4;
ALTER TABLE `eoms_repair_order`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `del_flag`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `create_user`;
UPDATE `eoms_repair_order` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0';

-- changeset ls:eoms_dp_m1_repair_record validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_record' AND COLUMN_NAME = 'dept_id'
-- comment EOMS 维修记录补租户/创建人/部门（DataScope M1）
SET NAMES utf8mb4;
ALTER TABLE `eoms_repair_record`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `del_flag`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `create_user`;
UPDATE `eoms_repair_record` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0';

-- changeset ls:eoms_dp_m1_repair_settlement validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_settlement' AND COLUMN_NAME = 'dept_id'
-- comment EOMS 维修结算补租户/创建人/部门（DataScope M1）
SET NAMES utf8mb4;
ALTER TABLE `eoms_repair_settlement`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `del_flag`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `create_user`;
UPDATE `eoms_repair_settlement` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0';

-- changeset ls:eoms_dp_m1_contract validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_contract' AND COLUMN_NAME = 'dept_id'
-- comment EOMS 合同补租户/创建人/部门（DataScope M1）
SET NAMES utf8mb4;
ALTER TABLE `eoms_contract`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `del_flag`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `create_user`;
UPDATE `eoms_contract` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0';

-- changeset ls:eoms_dp_m1_receivable validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_receivable' AND COLUMN_NAME = 'dept_id'
-- comment EOMS 应收补租户/创建人/部门（DataScope M1）
SET NAMES utf8mb4;
ALTER TABLE `eoms_receivable`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `del_flag`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `create_user`;
UPDATE `eoms_receivable` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0';

-- changeset ls:eoms_dp_m1_payment validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_payment' AND COLUMN_NAME = 'dept_id'
-- comment EOMS 收款补租户/创建人/部门（DataScope M1）
SET NAMES utf8mb4;
ALTER TABLE `eoms_payment`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `del_flag`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人部门' AFTER `create_user`;
UPDATE `eoms_payment` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0';
