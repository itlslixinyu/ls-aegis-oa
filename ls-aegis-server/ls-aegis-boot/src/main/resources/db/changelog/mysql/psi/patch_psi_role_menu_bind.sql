-- liquibase formatted sql

-- changeset psi:12 validCheckSum:ANY
-- comment PSI：工作台/流水/待办菜单绑定普通角色 + 超管（幂等）

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10185 AS id UNION ALL SELECT 10186
    UNION ALL SELECT 10190 UNION ALL SELECT 10191
    UNION ALL SELECT 10195 UNION ALL SELECT 10196 UNION ALL SELECT 10197
) m
WHERE r.id IN (1, 3);
