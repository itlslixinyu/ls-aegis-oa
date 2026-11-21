-- liquibase formatted sql

-- changeset psi:stock-query-menu-place validCheckSum:ANY
-- comment PSI：库存查询菜单位置纠正（基础数据·仓库管理旁；兼容已执行旧 patch 挂在库存业务下）

UPDATE `sys_menu`
SET `parent_id` = 10001,
    `sort` = 5,
    `title` = '库存查询',
    `path` = '/psi/stock-query',
    `name` = 'PsiStockQuery',
    `component` = 'psi/stock-query/index',
    `icon` = 'search',
    `update_time` = NOW()
WHERE `id` = 10210;

UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10080 AND (`sort` IS NULL OR `sort` <> 6);

-- 若曾把库存流水/待办为腾位置改过 sort，恢复库存业务内顺序：盘点5 调整6 流水7 待办8
UPDATE `sys_menu` SET `sort` = 7, `update_time` = NOW() WHERE `id` = 10190 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 8, `update_time` = NOW() WHERE `id` = 10195 AND `parent_id` = 10002;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10210 AS id UNION ALL SELECT 10211
) m
WHERE r.id IN (1, 3);
