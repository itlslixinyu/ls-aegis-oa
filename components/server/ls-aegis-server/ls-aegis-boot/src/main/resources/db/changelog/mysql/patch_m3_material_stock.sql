-- liquibase formatted sql

-- changeset ls:m3_map_tables
-- comment M3：物料分类/商品迁移对账表（重名禁止静默 dup）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `eoms_m3_category_map` (
  `id`                bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`         bigint(20)    NOT NULL COMMENT '原 eoms_material_category.id',
  `source_code`       varchar(64)   DEFAULT NULL COMMENT '原分类编码',
  `source_name`       varchar(256)  DEFAULT NULL COMMENT '原分类名称',
  `psi_category_id`   bigint(20)    DEFAULT NULL COMMENT '目标 psi_goods_category.id',
  `action`            varchar(32)   NOT NULL COMMENT 'INSERTED/MAPPED_CODE/CONFLICT_NAME',
  `note`              varchar(500)  DEFAULT NULL COMMENT '对账说明',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source_id`),
  KEY `idx_psi_cat` (`psi_category_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='M3物料分类迁移对账';

CREATE TABLE IF NOT EXISTS `eoms_m3_goods_map` (
  `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`      bigint(20)    NOT NULL COMMENT '原 eoms_material.id',
  `source_code`    varchar(64)   DEFAULT NULL COMMENT '原物料编号',
  `source_name`    varchar(256)  DEFAULT NULL COMMENT '原物料名称',
  `psi_goods_id`   bigint(20)    DEFAULT NULL COMMENT '目标 psi_goods.id',
  `action`         varchar(32)   NOT NULL COMMENT 'INSERTED/MAPPED_CODE/CONFLICT_NAME',
  `note`           varchar(500)  DEFAULT NULL COMMENT '对账说明',
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source_id`),
  KEY `idx_psi_goods` (`psi_goods_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='M3物料商品迁移对账';

-- changeset ls:m3_migrate_category_dict
-- comment M3.1：分类（运行时代码列 dict_code/dict_name/parent_code）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_category' AND COLUMN_NAME = 'dict_code'
SET NAMES utf8mb4;

INSERT INTO `eoms_m3_category_map` (`source_id`, `source_code`, `source_name`, `psi_category_id`, `action`, `note`)
SELECT c.`id`, c.`dict_code`, c.`dict_name`, p.`id`, 'MAPPED_CODE',
       CONCAT('编码已存在，沿用商品分类 ', p.`code`)
FROM `eoms_material_category` c
INNER JOIN `psi_goods_category` p ON p.`code` = LEFT(c.`dict_code`, 32) AND p.`deleted` = 0
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`);

INSERT INTO `eoms_m3_category_map` (`source_id`, `source_code`, `source_name`, `psi_category_id`, `action`, `note`)
SELECT c.`id`, c.`dict_code`, c.`dict_name`,
       (SELECT MIN(p.`id`) FROM `psi_goods_category` p WHERE p.`name` = LEFT(c.`dict_name`, 64) AND p.`deleted` = 0),
       'CONFLICT_NAME',
       '名称与已有分类重复，禁止静默 dup，已映射到最小 id'
FROM `eoms_material_category` c
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`)
  AND EXISTS (SELECT 1 FROM `psi_goods_category` p WHERE p.`name` = LEFT(c.`dict_name`, 64) AND p.`deleted` = 0);

INSERT INTO `psi_goods_category` (
    `tenant_id`, `dept_id`, `parent_id`, `code`, `name`, `sort`, `status`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, 0, 0, LEFT(c.`dict_code`, 32), LEFT(c.`dict_name`, 64), IFNULL(c.`sort`, 999),
       CASE WHEN c.`del_flag` <> '0' OR c.`status` = '1' THEN 2 ELSE 1 END,
       1, IFNULL(c.`create_time`, NOW()), 0
FROM `eoms_material_category` c
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND (c.`parent_code` IS NULL OR c.`parent_code` IN ('', '0'))
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`)
  AND NOT EXISTS (SELECT 1 FROM `psi_goods_category` p WHERE p.`code` = LEFT(c.`dict_code`, 32) AND p.`deleted` = 0);

INSERT INTO `eoms_m3_category_map` (`source_id`, `source_code`, `source_name`, `psi_category_id`, `action`, `note`)
SELECT c.`id`, c.`dict_code`, c.`dict_name`, p.`id`, 'INSERTED', '新写入一级商品分类'
FROM `eoms_material_category` c
INNER JOIN `psi_goods_category` p ON p.`code` = LEFT(c.`dict_code`, 32) AND p.`deleted` = 0
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND (c.`parent_code` IS NULL OR c.`parent_code` IN ('', '0'))
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`);

INSERT INTO `psi_goods_category` (
    `tenant_id`, `dept_id`, `parent_id`, `code`, `name`, `sort`, `status`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, 0, pm.`psi_category_id`, LEFT(c.`dict_code`, 32), LEFT(c.`dict_name`, 64), IFNULL(c.`sort`, 999),
       CASE WHEN c.`del_flag` <> '0' OR c.`status` = '1' THEN 2 ELSE 1 END,
       1, IFNULL(c.`create_time`, NOW()), 0
FROM `eoms_material_category` c
INNER JOIN `eoms_material_category` parent ON parent.`dict_code` = c.`parent_code`
INNER JOIN `eoms_m3_category_map` pm ON pm.`source_id` = parent.`id` AND pm.`psi_category_id` IS NOT NULL
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND c.`parent_code` IS NOT NULL AND c.`parent_code` NOT IN ('', '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`)
  AND NOT EXISTS (SELECT 1 FROM `psi_goods_category` p WHERE p.`code` = LEFT(c.`dict_code`, 32) AND p.`deleted` = 0);

INSERT INTO `eoms_m3_category_map` (`source_id`, `source_code`, `source_name`, `psi_category_id`, `action`, `note`)
SELECT c.`id`, c.`dict_code`, c.`dict_name`, p.`id`, 'INSERTED', '新写入二级商品分类'
FROM `eoms_material_category` c
INNER JOIN `psi_goods_category` p ON p.`code` = LEFT(c.`dict_code`, 32) AND p.`deleted` = 0
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`);

-- changeset ls:m3_migrate_category_official
-- comment M3.1：分类（官方 DDL：category_name/parent_id）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_category' AND COLUMN_NAME = 'category_name'
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_category' AND COLUMN_NAME = 'dict_code'
SET NAMES utf8mb4;

INSERT INTO `eoms_m3_category_map` (`source_id`, `source_code`, `source_name`, `psi_category_id`, `action`, `note`)
SELECT c.`id`, CONCAT('EMC', c.`id`), c.`category_name`, p.`id`, 'CONFLICT_NAME',
       '名称与已有分类重复，禁止静默 dup，已映射到最小 id'
FROM `eoms_material_category` c
INNER JOIN `psi_goods_category` p ON p.`name` = LEFT(c.`category_name`, 64) AND p.`deleted` = 0
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`);

INSERT INTO `psi_goods_category` (
    `tenant_id`, `dept_id`, `parent_id`, `code`, `name`, `sort`, `status`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, 0, 0, CONCAT('EMC', c.`id`), LEFT(c.`category_name`, 64), IFNULL(c.`sort`, 999),
       CASE WHEN c.`del_flag` <> '0' OR c.`status` = '1' THEN 2 ELSE 1 END,
       1, IFNULL(c.`create_time`, NOW()), 0
FROM `eoms_material_category` c
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND IFNULL(c.`parent_id`, 0) = 0
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`)
  AND NOT EXISTS (SELECT 1 FROM `psi_goods_category` p WHERE p.`code` = CONCAT('EMC', c.`id`) AND p.`deleted` = 0);

INSERT INTO `eoms_m3_category_map` (`source_id`, `source_code`, `source_name`, `psi_category_id`, `action`, `note`)
SELECT c.`id`, CONCAT('EMC', c.`id`), c.`category_name`, p.`id`, 'INSERTED', '新写入一级商品分类'
FROM `eoms_material_category` c
INNER JOIN `psi_goods_category` p ON p.`code` = CONCAT('EMC', c.`id`) AND p.`deleted` = 0
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND IFNULL(c.`parent_id`, 0) = 0
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`);

INSERT INTO `psi_goods_category` (
    `tenant_id`, `dept_id`, `parent_id`, `code`, `name`, `sort`, `status`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, 0, pm.`psi_category_id`, CONCAT('EMC', c.`id`), LEFT(c.`category_name`, 64), IFNULL(c.`sort`, 999),
       CASE WHEN c.`del_flag` <> '0' OR c.`status` = '1' THEN 2 ELSE 1 END,
       1, IFNULL(c.`create_time`, NOW()), 0
FROM `eoms_material_category` c
INNER JOIN `eoms_m3_category_map` pm ON pm.`source_id` = c.`parent_id` AND pm.`psi_category_id` IS NOT NULL
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND IFNULL(c.`parent_id`, 0) <> 0
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`)
  AND NOT EXISTS (SELECT 1 FROM `psi_goods_category` p WHERE p.`code` = CONCAT('EMC', c.`id`) AND p.`deleted` = 0);

INSERT INTO `eoms_m3_category_map` (`source_id`, `source_code`, `source_name`, `psi_category_id`, `action`, `note`)
SELECT c.`id`, CONCAT('EMC', c.`id`), c.`category_name`, p.`id`, 'INSERTED', '新写入二级商品分类'
FROM `eoms_material_category` c
INNER JOIN `psi_goods_category` p ON p.`code` = CONCAT('EMC', c.`id`) AND p.`deleted` = 0
WHERE (c.`del_flag` IS NULL OR c.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_category_map` m WHERE m.`source_id` = c.`id`);

-- changeset ls:m3_ensure_leaf_category
-- comment M3.1：一级分类下补「维修备件」叶子，供商品挂靠
SET NAMES utf8mb4;

INSERT INTO `psi_goods_category` (
    `tenant_id`, `dept_id`, `parent_id`, `code`, `name`, `sort`, `status`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, 0, pc.`id`, LEFT(CONCAT(IFNULL(pc.`code`, 'EM'), '99'), 32), '维修备件', 999, 1, 1, NOW(), 0
FROM (
    SELECT DISTINCT m.`psi_category_id` AS id
    FROM `eoms_m3_category_map` m
    WHERE m.`psi_category_id` IS NOT NULL
) src
INNER JOIN `psi_goods_category` pc ON pc.`id` = src.`id` AND pc.`deleted` = 0
WHERE pc.`parent_id` = 0
  AND NOT EXISTS (SELECT 1 FROM `psi_goods_category` x WHERE x.`parent_id` = pc.`id` AND x.`deleted` = 0)
  AND NOT EXISTS (
      SELECT 1 FROM `psi_goods_category` x
      WHERE x.`tenant_id` = 0 AND x.`deleted` = 0 AND x.`code` = LEFT(CONCAT(IFNULL(pc.`code`, 'EM'), '99'), 32)
  );

-- changeset ls:m3_migrate_goods_dict
-- comment M3.1：物料（运行时代码列 material_category）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material' AND COLUMN_NAME = 'material_category'
SET NAMES utf8mb4;

INSERT INTO `eoms_m3_goods_map` (`source_id`, `source_code`, `source_name`, `psi_goods_id`, `action`, `note`)
SELECT m.`id`, m.`material_no`, m.`material_name`, g.`id`, 'MAPPED_CODE',
       CONCAT('编码已存在，沿用商品 ', g.`goods_code`)
FROM `eoms_material` m
INNER JOIN `psi_goods` g ON g.`goods_code` = LEFT(m.`material_no`, 32) AND g.`deleted` = 0
WHERE (m.`del_flag` IS NULL OR m.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_goods_map` x WHERE x.`source_id` = m.`id`);

INSERT INTO `eoms_m3_goods_map` (`source_id`, `source_code`, `source_name`, `psi_goods_id`, `action`, `note`)
SELECT m.`id`, m.`material_no`, m.`material_name`,
       (SELECT MIN(g.`id`) FROM `psi_goods` g WHERE g.`goods_name` = LEFT(m.`material_name`, 128) AND g.`deleted` = 0),
       'CONFLICT_NAME',
       '名称与已有商品重复，禁止静默 dup，已映射到最小 id'
FROM `eoms_material` m
WHERE (m.`del_flag` IS NULL OR m.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_goods_map` x WHERE x.`source_id` = m.`id`)
  AND EXISTS (SELECT 1 FROM `psi_goods` g WHERE g.`goods_name` = LEFT(m.`material_name`, 128) AND g.`deleted` = 0);

INSERT INTO `psi_goods` (
    `tenant_id`, `dept_id`, `goods_code`, `goods_name`, `spec`, `unit`, `default_tax_rate`,
    `category_id`, `purchase_price`, `sale_price`, `stock_lower`, `status`, `remark`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, 0, LEFT(m.`material_no`, 32), LEFT(m.`material_name`, 128), LEFT(m.`specification`, 128),
       IFNULL(NULLIF(m.`unit`, ''), '个'), 13.00,
       CASE WHEN pc.`parent_id` = 0 THEN (
           SELECT MIN(ch.`id`) FROM `psi_goods_category` ch WHERE ch.`parent_id` = pc.`id` AND ch.`deleted` = 0
       ) ELSE cm.`psi_category_id` END,
       m.`purchase_price`, m.`sale_price`, m.`min_stock`,
       CASE WHEN m.`del_flag` <> '0' OR m.`status` = '1' THEN 2 ELSE 1 END,
       LEFT(IFNULL(m.`remark`, ''), 500),
       1, IFNULL(m.`create_time`, NOW()), 0
FROM `eoms_material` m
LEFT JOIN `eoms_material_category` c ON c.`dict_code` = m.`material_category`
LEFT JOIN `eoms_m3_category_map` cm ON cm.`source_id` = c.`id`
LEFT JOIN `psi_goods_category` pc ON pc.`id` = cm.`psi_category_id` AND pc.`deleted` = 0
WHERE (m.`del_flag` IS NULL OR m.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_goods_map` x WHERE x.`source_id` = m.`id`)
  AND NOT EXISTS (SELECT 1 FROM `psi_goods` g WHERE g.`goods_code` = LEFT(m.`material_no`, 32) AND g.`deleted` = 0);

INSERT INTO `eoms_m3_goods_map` (`source_id`, `source_code`, `source_name`, `psi_goods_id`, `action`, `note`)
SELECT m.`id`, m.`material_no`, m.`material_name`, g.`id`, 'INSERTED', '新写入进销存商品'
FROM `eoms_material` m
INNER JOIN `psi_goods` g ON g.`goods_code` = LEFT(m.`material_no`, 32) AND g.`deleted` = 0
WHERE (m.`del_flag` IS NULL OR m.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_goods_map` x WHERE x.`source_id` = m.`id`);

-- changeset ls:m3_migrate_goods_official
-- comment M3.1：物料（官方 DDL：category_id）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material' AND COLUMN_NAME = 'category_id'
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material' AND COLUMN_NAME = 'material_category'
SET NAMES utf8mb4;

INSERT INTO `eoms_m3_goods_map` (`source_id`, `source_code`, `source_name`, `psi_goods_id`, `action`, `note`)
SELECT m.`id`, m.`material_no`, m.`material_name`, g.`id`, 'MAPPED_CODE',
       CONCAT('编码已存在，沿用商品 ', g.`goods_code`)
FROM `eoms_material` m
INNER JOIN `psi_goods` g ON g.`goods_code` = LEFT(m.`material_no`, 32) AND g.`deleted` = 0
WHERE (m.`del_flag` IS NULL OR m.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_goods_map` x WHERE x.`source_id` = m.`id`);

INSERT INTO `eoms_m3_goods_map` (`source_id`, `source_code`, `source_name`, `psi_goods_id`, `action`, `note`)
SELECT m.`id`, m.`material_no`, m.`material_name`,
       (SELECT MIN(g.`id`) FROM `psi_goods` g WHERE g.`goods_name` = LEFT(m.`material_name`, 128) AND g.`deleted` = 0),
       'CONFLICT_NAME',
       '名称与已有商品重复，禁止静默 dup，已映射到最小 id'
FROM `eoms_material` m
WHERE (m.`del_flag` IS NULL OR m.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_goods_map` x WHERE x.`source_id` = m.`id`)
  AND EXISTS (SELECT 1 FROM `psi_goods` g WHERE g.`goods_name` = LEFT(m.`material_name`, 128) AND g.`deleted` = 0);

INSERT INTO `psi_goods` (
    `tenant_id`, `dept_id`, `goods_code`, `goods_name`, `spec`, `unit`, `default_tax_rate`,
    `category_id`, `purchase_price`, `sale_price`, `stock_lower`, `status`, `remark`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, 0, LEFT(m.`material_no`, 32), LEFT(m.`material_name`, 128), LEFT(m.`specification`, 128),
       IFNULL(NULLIF(m.`unit`, ''), '个'), 13.00,
       CASE WHEN pc.`parent_id` = 0 THEN (
           SELECT MIN(ch.`id`) FROM `psi_goods_category` ch WHERE ch.`parent_id` = pc.`id` AND ch.`deleted` = 0
       ) ELSE cm.`psi_category_id` END,
       m.`purchase_price`, m.`sale_price`, m.`min_stock`,
       CASE WHEN m.`del_flag` <> '0' OR m.`status` = '1' THEN 2 ELSE 1 END,
       LEFT(IFNULL(m.`remark`, ''), 500),
       1, IFNULL(m.`create_time`, NOW()), 0
FROM `eoms_material` m
LEFT JOIN `eoms_m3_category_map` cm ON cm.`source_id` = m.`category_id`
LEFT JOIN `psi_goods_category` pc ON pc.`id` = cm.`psi_category_id` AND pc.`deleted` = 0
WHERE (m.`del_flag` IS NULL OR m.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_goods_map` x WHERE x.`source_id` = m.`id`)
  AND NOT EXISTS (SELECT 1 FROM `psi_goods` g WHERE g.`goods_code` = LEFT(m.`material_no`, 32) AND g.`deleted` = 0);

INSERT INTO `eoms_m3_goods_map` (`source_id`, `source_code`, `source_name`, `psi_goods_id`, `action`, `note`)
SELECT m.`id`, m.`material_no`, m.`material_name`, g.`id`, 'INSERTED', '新写入进销存商品'
FROM `eoms_material` m
INNER JOIN `psi_goods` g ON g.`goods_code` = LEFT(m.`material_no`, 32) AND g.`deleted` = 0
WHERE (m.`del_flag` IS NULL OR m.`del_flag` = '0')
  AND NOT EXISTS (SELECT 1 FROM `eoms_m3_goods_map` x WHERE x.`source_id` = m.`id`);

-- changeset ls:m3_migrate_stock
-- comment M3.1：EOMS 库存数量迁入 PSI 期初批次+流水
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_stock'
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_stock' AND COLUMN_NAME = 'warehouse_id'
SET NAMES utf8mb4;

INSERT INTO `psi_stock_batch` (
    `tenant_id`, `warehouse_id`, `goods_id`, `batch_no`, `source_type`, `source_id`, `source_no`,
    `in_date`, `origin_in_date`, `total_quantity`, `remain_quantity`, `untax_price`,
    `total_amount`, `remain_amount`, `create_user`, `create_time`, `deleted`
)
SELECT 0,
       COALESCE(s.`warehouse_id`, (
           SELECT p.`id` FROM `psi_warehouse` p WHERE p.`deleted` = 0 ORDER BY p.`is_default` DESC, p.`id` ASC LIMIT 1
       )),
       gm.`psi_goods_id`,
       CONCAT('M3', LPAD(s.`id`, 10, '0')),
       'INIT_STOCK', s.`id`, CONCAT('M3-MIG-', s.`id`),
       IFNULL(s.`create_time`, NOW()), IFNULL(s.`create_time`, NOW()),
       COALESCE(s.`available_quantity`, s.`stock_quantity`, 0),
       COALESCE(s.`available_quantity`, s.`stock_quantity`, 0),
       IFNULL(g.`purchase_price`, 0),
       ROUND(COALESCE(s.`available_quantity`, s.`stock_quantity`, 0) * IFNULL(g.`purchase_price`, 0), 2),
       ROUND(COALESCE(s.`available_quantity`, s.`stock_quantity`, 0) * IFNULL(g.`purchase_price`, 0), 2),
       1, NOW(), 0
FROM `eoms_material_stock` s
INNER JOIN `eoms_m3_goods_map` gm ON gm.`source_id` = s.`material_id` AND gm.`psi_goods_id` IS NOT NULL
LEFT JOIN `psi_goods` g ON g.`id` = gm.`psi_goods_id`
WHERE (s.`del_flag` IS NULL OR s.`del_flag` = '0')
  AND COALESCE(s.`available_quantity`, s.`stock_quantity`, 0) > 0
  AND COALESCE(s.`warehouse_id`, (
      SELECT p.`id` FROM `psi_warehouse` p WHERE p.`deleted` = 0 ORDER BY p.`is_default` DESC, p.`id` ASC LIMIT 1
  )) IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `psi_stock_batch` b WHERE b.`batch_no` = CONCAT('M3', LPAD(s.`id`, 10, '0')) AND b.`deleted` = 0
  );

INSERT INTO `psi_stock_flow` (
    `tenant_id`, `dept_id`, `warehouse_id`, `goods_id`, `flow_type`,
    `before_qty`, `change_qty`, `after_qty`, `before_amount`, `change_amount`, `after_amount`,
    `source_type`, `source_id`, `source_no`, `confirm_user`, `confirm_time`,
    `create_user`, `create_time`, `deleted`
)
SELECT b.`tenant_id`, 0, b.`warehouse_id`, b.`goods_id`, 'IN',
       0, b.`total_quantity`, b.`total_quantity`, 0, b.`total_amount`, b.`total_amount`,
       b.`source_type`, b.`source_id`, b.`source_no`, 1, b.`create_time`,
       1, b.`create_time`, 0
FROM `psi_stock_batch` b
WHERE b.`batch_no` LIKE 'M3%' AND b.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1 FROM `psi_stock_flow` f WHERE f.`source_no` = b.`source_no` AND f.`deleted` = 0
  );

-- changeset ls:m3_repair_material_warehouse_id
-- comment M3.2：工单用料绑定出库仓库
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_material'
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_material' AND COLUMN_NAME = 'warehouse_id'
SET NAMES utf8mb4;
ALTER TABLE `eoms_repair_material`
    ADD COLUMN `warehouse_id` bigint(20) DEFAULT NULL COMMENT '出库仓库ID，关联 psi_warehouse.id' AFTER `material_id`;

-- changeset ls:m3_remap_repair_material
-- comment M3.1：工单用料 material_id 改指 psi_goods.id
SET NAMES utf8mb4;
UPDATE `eoms_repair_material` rm
INNER JOIN `eoms_m3_goods_map` m ON m.`source_id` = rm.`material_id`
SET rm.`material_id` = m.`psi_goods_id`
WHERE m.`psi_goods_id` IS NOT NULL AND rm.`material_id` <> m.`psi_goods_id`;

-- changeset ls:m3_remap_inbound_item
-- comment M3：历史入库明细改绑商品（表可能不存在）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_inbound_item'
SET NAMES utf8mb4;
UPDATE `eoms_inbound_item` i
INNER JOIN `eoms_m3_goods_map` m ON m.`source_id` = i.`material_id`
SET i.`material_id` = m.`psi_goods_id`
WHERE m.`psi_goods_id` IS NOT NULL AND i.`material_id` <> m.`psi_goods_id`;

-- changeset ls:m3_archive_eoms_material_record
-- comment M3：归档 eoms_material_record
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_record'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_material_record_archived`;
RENAME TABLE `eoms_material_record` TO `eoms_material_record_archived`;

-- changeset ls:m3_archive_eoms_material_stock
-- comment M3：归档 eoms_material_stock
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_stock'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_material_stock_archived`;
RENAME TABLE `eoms_material_stock` TO `eoms_material_stock_archived`;

-- changeset ls:m3_archive_eoms_material
-- comment M3：归档 eoms_material
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_material_archived`;
RENAME TABLE `eoms_material` TO `eoms_material_archived`;

-- changeset ls:m3_archive_eoms_material_category
-- comment M3：归档 eoms_material_category
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_category'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_material_category_archived`;
RENAME TABLE `eoms_material_category` TO `eoms_material_category_archived`;

-- changeset ls:m3_archive_eoms_purchase
-- comment M3.3：归档 eoms_purchase
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_purchase_archived`;
RENAME TABLE `eoms_purchase` TO `eoms_purchase_archived`;

-- changeset ls:m3_archive_eoms_inbound_item
-- comment M3.3：归档 eoms_inbound_item
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_inbound_item'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_inbound_item_archived`;
RENAME TABLE `eoms_inbound_item` TO `eoms_inbound_item_archived`;

-- changeset ls:m3_archive_eoms_inbound
-- comment M3.3：归档 eoms_inbound
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_inbound'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_inbound_archived`;
RENAME TABLE `eoms_inbound` TO `eoms_inbound_archived`;

-- changeset ls:m3_dict_repair
-- comment M3.2：出库类型增加维修出库
SET NAMES utf8mb4;
INSERT INTO `sys_dict_item`
(`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 142, '维修出库', 'REPAIR', 'success', 3, 'EOMS 维修结算出库', 1, 104, 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_item` WHERE `id` = 142)
  AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` WHERE `dict_id` = 104 AND `value` = 'REPAIR');

-- changeset ls:m3_menus
-- comment M3.3/M3.4：下线 EOMS 物料/库存/采购/入库菜单，补 PSI 商品与出入库
SET NAMES utf8mb4;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10021
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20020, 20011, 20013, 20022);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10020
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20020, 20021, 10021);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10310
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20021, 20020);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10040
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20023, 20024);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10050
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20022, 20011, 20013);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10001
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10020, 10310, 10021);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10002
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10040, 10050);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10041
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10040, 20023, 20024);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10051
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10050, 20022, 20011, 20013);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10031
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20011, 20013, 20020, 20022, 20023, 20024);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10030
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10031, 20011, 20013);

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (20020, 20021, 20022, 20023, 20024);
DELETE FROM `sys_menu` WHERE `id` IN (20020, 20021, 20022, 20023, 20024);
