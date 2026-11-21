-- liquibase formatted sql

-- changeset ls:sign_dp_m4_project validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'create_user'
-- comment Sign 项目补 create_user/tenant_id（DataScope M4；已有 dept_id）
SET NAMES utf8mb4;
ALTER TABLE `sign_project`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `del_flag`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`;
UPDATE `sign_project` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0)
WHERE t.del_flag = '0';

-- changeset ls:sign_dp_m4_log validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_log' AND COLUMN_NAME = 'dept_id'
-- comment Sign 日志补 dept_id/create_user/tenant_id（DataScope M4）
SET NAMES utf8mb4;
ALTER TABLE `sign_log`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `create_time`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '部门ID' AFTER `create_user`;
UPDATE `sign_log` t
    LEFT JOIN `sys_user` u ON u.id = t.user_id AND u.deleted = 0
    LEFT JOIN `sign_project` p ON p.project_id = t.project_id
SET t.create_user = COALESCE(t.user_id, 0),
    t.dept_id = COALESCE(NULLIF(p.dept_id, 0), u.dept_id, 0);

-- changeset ls:sign_dp_m4_record validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_record' AND COLUMN_NAME = 'dept_id'
-- comment Sign 记录补 dept_id/create_user/tenant_id（DataScope M4）
SET NAMES utf8mb4;
ALTER TABLE `sign_record`
    ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID' AFTER `create_time`,
    ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID' AFTER `tenant_id`,
    ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '部门ID' AFTER `create_user`;
UPDATE `sign_record` t
    LEFT JOIN `sys_user` u ON u.id = t.user_id AND u.deleted = 0
    LEFT JOIN `sign_project` p ON p.project_id = t.project_id
SET t.create_user = COALESCE(t.user_id, 0),
    t.dept_id = COALESCE(NULLIF(p.dept_id, 0), u.dept_id, 0);
