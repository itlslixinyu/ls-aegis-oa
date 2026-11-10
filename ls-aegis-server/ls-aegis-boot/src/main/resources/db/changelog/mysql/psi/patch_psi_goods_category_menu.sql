-- liquibase formatted sql

-- changeset psi:120 validCheckSum:ANY
-- comment PSI：商品分类菜单与角色授权

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10310, '商品分类', 10001, 2, '/psi/goods-category', 'PsiGoodsCategory', 'psi/goods-category/index', NULL, 'mind-mapping', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10310);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10311, '列表', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10311);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10312, '详情', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:get', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10312);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10313, '新增', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:create', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10313);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10314, '修改', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:update', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10314);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10315, '删除', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:delete', 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10315);

-- 商品档案排序让到分类之后
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 10020 AND `sort` < 3;
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 10030 AND `sort` < 4;
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 10080 AND `sort` < 5;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10310 AS id UNION ALL SELECT 10311 UNION ALL SELECT 10312
    UNION ALL SELECT 10313 UNION ALL SELECT 10314 UNION ALL SELECT 10315
) m
WHERE r.id IN (1, 3);
