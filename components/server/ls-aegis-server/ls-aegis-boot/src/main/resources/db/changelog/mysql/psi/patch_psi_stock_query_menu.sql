-- liquibase formatted sql

-- changeset psi:stock-query-menu validCheckSum:ANY
-- comment PSI：库存查询菜单（挂基础数据·仓库管理旁，仓×商品实时剩余）

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10210, '库存查询', 10001, 2, '/psi/stock-query', 'PsiStockQuery', 'psi/stock-query/index', NULL, 'search', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10210);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10211, '列表', 10210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockQuery:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10211);

-- 已存在时纠正挂载：基础数据下、仓库管理之后
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

-- 期初建账顺延到仓库管理、库存查询之后
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10080 AND `sort` < 6;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10210 AS id UNION ALL SELECT 10211
) m
WHERE r.id IN (1, 3);
