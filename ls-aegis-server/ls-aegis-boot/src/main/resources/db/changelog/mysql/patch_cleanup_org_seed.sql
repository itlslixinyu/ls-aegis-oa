-- liquibase formatted sql

-- changeset ls:cleanup_org_seed
-- comment 精简组织数据：仅保留总公司/总经办、三角色、超管+测试用户
SET NAMES utf8mb4;

-- 先调整保留数据，再删多余（避免外键/归属问题）
UPDATE `sys_dept` SET `name` = '总公司', `description` = '一级部门', `ancestors` = '0', `sort` = 1, `is_system` = b'1' WHERE `id` = 1;

INSERT INTO `sys_dept`
(`id`, `name`, `parent_id`, `ancestors`, `description`, `sort`, `status`, `is_system`, `create_user`, `create_time`)
SELECT 2, '总经办', 1, '0,1', '二级部门', 1, 1, b'0', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_dept` WHERE `id` = 2);

UPDATE `sys_dept` SET `name` = '总经办', `parent_id` = 1, `ancestors` = '0,1', `description` = '二级部门', `sort` = 1, `status` = 1 WHERE `id` = 2;

UPDATE `sys_role` SET `name` = '普通角色' WHERE `id` = 3 AND `code` = 'general';

UPDATE `sys_user`
SET `nickname` = '测试用户',
    `dept_id` = 2,
    `description` = '系统测试账号'
WHERE `id` = 547889293968801822;

DELETE FROM `sys_user_role` WHERE `user_id` = 547889293968801822;
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`)
SELECT 2, 547889293968801822, 3
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_user_role` WHERE `user_id` = 547889293968801822 AND `role_id` = 3
);

-- 清理多余用户及相关
DELETE FROM `sys_user_role` WHERE `user_id` NOT IN (1, 547889293968801822);
DELETE FROM `sys_user_social` WHERE `user_id` NOT IN (1, 547889293968801822);
DELETE FROM `sys_user_password_history` WHERE `user_id` NOT IN (1, 547889293968801822);
DELETE FROM `sys_message_log` WHERE `user_id` NOT IN (1, 547889293968801822);
DELETE FROM `sys_notice_log` WHERE `user_id` NOT IN (1, 547889293968801822);
DELETE FROM `sys_user` WHERE `id` NOT IN (1, 547889293968801822);

-- 清理多余角色及相关
DELETE FROM `sys_role_menu` WHERE `role_id` NOT IN (1, 2, 3);
DELETE FROM `sys_role_dept` WHERE `role_id` NOT IN (1, 2, 3);
DELETE FROM `sys_user_role` WHERE `role_id` NOT IN (1, 2, 3);
DELETE FROM `sys_role` WHERE `id` NOT IN (1, 2, 3);

-- 清理多余部门（保留总公司、总经办）
UPDATE `sys_user` SET `dept_id` = 1 WHERE `id` = 1;
UPDATE `sys_user` SET `dept_id` = 2 WHERE `id` = 547889293968801822;
DELETE FROM `sys_role_dept` WHERE `dept_id` NOT IN (1, 2);
DELETE FROM `sys_dept` WHERE `id` NOT IN (1, 2);
