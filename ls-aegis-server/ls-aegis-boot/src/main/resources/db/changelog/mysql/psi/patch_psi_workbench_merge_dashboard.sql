-- liquibase formatted sql

-- changeset psi:147 validCheckSum:ANY
-- comment PSI：删除基础数据下工作台菜单（内容已并入仪表盘工作台）

UPDATE `sys_menu`
SET `redirect` = '/psi/unit',
    `update_time` = NOW()
WHERE `id` = 10001
  AND (`redirect` IS NULL OR `redirect` = '/psi/workbench');

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (10185, 10186);
DELETE FROM `sys_menu` WHERE `id` IN (10185, 10186);
