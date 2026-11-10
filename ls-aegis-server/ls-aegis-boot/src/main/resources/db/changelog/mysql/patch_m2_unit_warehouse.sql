-- liquibase formatted sql

-- changeset ls:m2_map_tables
-- comment M2：往来单位/仓库迁移对账表（重名禁止静默 dup）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `eoms_m2_unit_map` (
  `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_kind`   varchar(16)   NOT NULL COMMENT 'CUSTOMER/SUPPLIER',
  `source_id`     bigint(20)    NOT NULL COMMENT '原表主键',
  `source_code`   varchar(64)   DEFAULT NULL COMMENT '原编号',
  `source_name`   varchar(256)  DEFAULT NULL COMMENT '原名称',
  `psi_unit_id`   bigint(20)    DEFAULT NULL COMMENT '目标 psi_unit.id',
  `action`        varchar(32)   NOT NULL COMMENT 'INSERTED/MAPPED_CODE/CONFLICT_NAME',
  `note`          varchar(500)  DEFAULT NULL COMMENT '对账说明',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source_kind`, `source_id`),
  KEY `idx_psi_unit` (`psi_unit_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='M2往来单位迁移对账';

CREATE TABLE IF NOT EXISTS `eoms_m2_warehouse_map` (
  `id`                 bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`          bigint(20)    NOT NULL COMMENT '原 eoms_warehouse.id',
  `source_code`        varchar(64)   DEFAULT NULL COMMENT '原仓库编号',
  `source_name`        varchar(256)  DEFAULT NULL COMMENT '原仓库名称',
  `psi_warehouse_id`   bigint(20)    DEFAULT NULL COMMENT '目标 psi_warehouse.id',
  `action`             varchar(32)   NOT NULL COMMENT 'INSERTED/MAPPED_CODE/CONFLICT_NAME',
  `note`               varchar(500)  DEFAULT NULL COMMENT '对账说明',
  `create_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source_id`),
  KEY `idx_psi_wh` (`psi_warehouse_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='M2仓库迁移对账';

-- changeset ls:m2_migrate_customers
-- comment M2.1：eoms_customer → psi_unit（编码命中复用，重名记对账禁止静默 dup）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_customer'
SET NAMES utf8mb4;

INSERT INTO `eoms_m2_unit_map` (`source_kind`, `source_id`, `source_code`, `source_name`, `psi_unit_id`, `action`, `note`)
SELECT 'CUSTOMER', c.`id`, c.`customer_no`, c.`customer_name`, u.`id`, 'MAPPED_CODE',
       CONCAT('编码已存在，沿用往来单位 ', u.`unit_code`)
FROM `eoms_customer` c
INNER JOIN `psi_unit` u ON u.`unit_code` = LEFT(c.`customer_no`, 32) AND u.`deleted` = 0
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_unit_map` m WHERE m.`source_kind` = 'CUSTOMER' AND m.`source_id` = c.`id`
);

INSERT INTO `eoms_m2_unit_map` (`source_kind`, `source_id`, `source_code`, `source_name`, `psi_unit_id`, `action`, `note`)
SELECT 'CUSTOMER', c.`id`, c.`customer_no`, c.`customer_name`,
       (SELECT MIN(u.`id`) FROM `psi_unit` u WHERE u.`unit_name` = LEFT(c.`customer_name`, 128) AND u.`deleted` = 0),
       'CONFLICT_NAME',
       CONCAT('名称与已有往来单位重复，禁止静默 dup，已映射到最小 id')
FROM `eoms_customer` c
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_unit_map` m WHERE m.`source_kind` = 'CUSTOMER' AND m.`source_id` = c.`id`
)
AND EXISTS (
    SELECT 1 FROM `psi_unit` u WHERE u.`unit_name` = LEFT(c.`customer_name`, 128) AND u.`deleted` = 0
);

INSERT INTO `psi_unit` (
    `tenant_id`, `unit_code`, `unit_name`, `unit_type`, `unit_nature`,
    `address`, `status`, `remark`, `create_user`, `create_time`, `deleted`
)
SELECT 0,
       LEFT(c.`customer_no`, 32),
       LEFT(c.`customer_name`, 128),
       'CUSTOMER',
       'ENTERPRISE',
       LEFT(c.`address`, 256),
       CASE WHEN c.`del_flag` <> '0' OR c.`status` = '1' THEN 2 ELSE 1 END,
       LEFT(IFNULL(c.`remark`, ''), 500),
       1,
       IFNULL(c.`create_time`, NOW()),
       0
FROM `eoms_customer` c
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_unit_map` m WHERE m.`source_kind` = 'CUSTOMER' AND m.`source_id` = c.`id`
)
AND NOT EXISTS (
    SELECT 1 FROM `psi_unit` u WHERE u.`unit_code` = LEFT(c.`customer_no`, 32) AND u.`deleted` = 0
);

INSERT INTO `eoms_m2_unit_map` (`source_kind`, `source_id`, `source_code`, `source_name`, `psi_unit_id`, `action`, `note`)
SELECT 'CUSTOMER', c.`id`, c.`customer_no`, c.`customer_name`, u.`id`, 'INSERTED', '新写入往来单位（客户）'
FROM `eoms_customer` c
INNER JOIN `psi_unit` u ON u.`unit_code` = LEFT(c.`customer_no`, 32) AND u.`deleted` = 0
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_unit_map` m WHERE m.`source_kind` = 'CUSTOMER' AND m.`source_id` = c.`id`
);

UPDATE `psi_unit` u
INNER JOIN `eoms_m2_unit_map` m ON m.`psi_unit_id` = u.`id` AND m.`source_kind` = 'CUSTOMER'
SET u.`unit_type` = 'BOTH'
WHERE u.`unit_type` = 'SUPPLIER' AND u.`deleted` = 0;

-- changeset ls:m2_migrate_customer_contacts
-- comment M2.1：eoms_customer_contact → psi_unit_contact
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_customer_contact'
SET NAMES utf8mb4;

INSERT INTO `psi_unit_contact` (
    `tenant_id`, `unit_id`, `contact_name`, `mobile`, `phone`, `position`,
    `is_default`, `remark`, `create_user`, `create_time`, `deleted`
)
SELECT 0,
       m.`psi_unit_id`,
       LEFT(IFNULL(NULLIF(cc.`contact_name`, ''), '联系人'), 64),
       LEFT(cc.`mobile`, 32),
       LEFT(cc.`telephone`, 32),
       LEFT(cc.`position`, 64),
       CASE WHEN cc.`is_default` = '1' THEN 1 ELSE 0 END,
       LEFT(cc.`remark`, 500),
       1,
       IFNULL(cc.`create_time`, NOW()),
       0
FROM `eoms_customer_contact` cc
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = cc.`customer_id`
WHERE m.`psi_unit_id` IS NOT NULL
  AND (cc.`del_flag` IS NULL OR cc.`del_flag` = '0');

INSERT INTO `psi_unit_contact` (
    `tenant_id`, `unit_id`, `contact_name`, `mobile`, `is_default`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, m.`psi_unit_id`, '主联系人', LEFT(c.`phone`, 32), 1, 1, IFNULL(c.`create_time`, NOW()), 0
FROM `eoms_customer` c
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = c.`id`
WHERE m.`psi_unit_id` IS NOT NULL
  AND c.`phone` IS NOT NULL AND c.`phone` <> ''
  AND NOT EXISTS (
      SELECT 1 FROM `psi_unit_contact` x WHERE x.`unit_id` = m.`psi_unit_id` AND x.`deleted` = 0
  );

-- changeset ls:m2_migrate_suppliers
-- comment M2.1：eoms_supplier → psi_unit（表可能不存在）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_supplier'
SET NAMES utf8mb4;

INSERT INTO `eoms_m2_unit_map` (`source_kind`, `source_id`, `source_code`, `source_name`, `psi_unit_id`, `action`, `note`)
SELECT 'SUPPLIER', s.`id`, s.`supplier_no`, s.`supplier_name`, u.`id`, 'MAPPED_CODE',
       CONCAT('编码已存在，沿用往来单位 ', u.`unit_code`)
FROM `eoms_supplier` s
INNER JOIN `psi_unit` u ON u.`unit_code` = LEFT(s.`supplier_no`, 32) AND u.`deleted` = 0
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_unit_map` m WHERE m.`source_kind` = 'SUPPLIER' AND m.`source_id` = s.`id`
);

INSERT INTO `eoms_m2_unit_map` (`source_kind`, `source_id`, `source_code`, `source_name`, `psi_unit_id`, `action`, `note`)
SELECT 'SUPPLIER', s.`id`, s.`supplier_no`, s.`supplier_name`,
       (SELECT MIN(u.`id`) FROM `psi_unit` u WHERE u.`unit_name` = LEFT(s.`supplier_name`, 128) AND u.`deleted` = 0),
       'CONFLICT_NAME',
       CONCAT('名称与已有往来单位重复，禁止静默 dup，已映射到最小 id')
FROM `eoms_supplier` s
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_unit_map` m WHERE m.`source_kind` = 'SUPPLIER' AND m.`source_id` = s.`id`
)
AND EXISTS (
    SELECT 1 FROM `psi_unit` u WHERE u.`unit_name` = LEFT(s.`supplier_name`, 128) AND u.`deleted` = 0
);

INSERT INTO `psi_unit` (
    `tenant_id`, `unit_code`, `unit_name`, `unit_type`, `unit_nature`,
    `address`, `status`, `remark`, `create_user`, `create_time`, `deleted`
)
SELECT 0,
       LEFT(s.`supplier_no`, 32),
       LEFT(s.`supplier_name`, 128),
       'SUPPLIER',
       'ENTERPRISE',
       LEFT(s.`address`, 256),
       CASE WHEN s.`del_flag` <> '0' OR s.`status` = '1' THEN 2 ELSE 1 END,
       LEFT(IFNULL(s.`remark`, ''), 500),
       1,
       IFNULL(s.`create_time`, NOW()),
       0
FROM `eoms_supplier` s
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_unit_map` m WHERE m.`source_kind` = 'SUPPLIER' AND m.`source_id` = s.`id`
)
AND NOT EXISTS (
    SELECT 1 FROM `psi_unit` u WHERE u.`unit_code` = LEFT(s.`supplier_no`, 32) AND u.`deleted` = 0
);

INSERT INTO `eoms_m2_unit_map` (`source_kind`, `source_id`, `source_code`, `source_name`, `psi_unit_id`, `action`, `note`)
SELECT 'SUPPLIER', s.`id`, s.`supplier_no`, s.`supplier_name`, u.`id`, 'INSERTED', '新写入往来单位（供应商）'
FROM `eoms_supplier` s
INNER JOIN `psi_unit` u ON u.`unit_code` = LEFT(s.`supplier_no`, 32) AND u.`deleted` = 0
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_unit_map` m WHERE m.`source_kind` = 'SUPPLIER' AND m.`source_id` = s.`id`
);

UPDATE `psi_unit` u
INNER JOIN `eoms_m2_unit_map` m ON m.`psi_unit_id` = u.`id` AND m.`source_kind` = 'SUPPLIER'
SET u.`unit_type` = 'BOTH'
WHERE u.`unit_type` = 'CUSTOMER' AND u.`deleted` = 0;

INSERT INTO `psi_unit_contact` (
    `tenant_id`, `unit_id`, `contact_name`, `mobile`, `is_default`,
    `create_user`, `create_time`, `deleted`
)
SELECT 0, m.`psi_unit_id`,
       LEFT(IFNULL(NULLIF(s.`contact_person`, ''), '联系人'), 64),
       LEFT(s.`contact_phone`, 32),
       1, 1, IFNULL(s.`create_time`, NOW()), 0
FROM `eoms_supplier` s
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'SUPPLIER' AND m.`source_id` = s.`id`
WHERE m.`psi_unit_id` IS NOT NULL
  AND ((s.`contact_person` IS NOT NULL AND s.`contact_person` <> '')
    OR (s.`contact_phone` IS NOT NULL AND s.`contact_phone` <> ''))
  AND NOT EXISTS (
      SELECT 1 FROM `psi_unit_contact` x WHERE x.`unit_id` = m.`psi_unit_id` AND x.`deleted` = 0
  );

-- changeset ls:m2_migrate_warehouses
-- comment M2.4：eoms_warehouse → psi_warehouse（类型/容量丢弃）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_warehouse'
SET NAMES utf8mb4;

INSERT INTO `eoms_m2_warehouse_map` (`source_id`, `source_code`, `source_name`, `psi_warehouse_id`, `action`, `note`)
SELECT w.`id`, w.`warehouse_no`, w.`warehouse_name`, p.`id`, 'MAPPED_CODE',
       CONCAT('编码已存在，沿用进销存仓库 ', p.`warehouse_code`)
FROM `eoms_warehouse` w
INNER JOIN `psi_warehouse` p ON p.`warehouse_code` = LEFT(w.`warehouse_no`, 32) AND p.`deleted` = 0
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_warehouse_map` m WHERE m.`source_id` = w.`id`
);

INSERT INTO `eoms_m2_warehouse_map` (`source_id`, `source_code`, `source_name`, `psi_warehouse_id`, `action`, `note`)
SELECT w.`id`, w.`warehouse_no`, w.`warehouse_name`,
       (SELECT MIN(p.`id`) FROM `psi_warehouse` p WHERE p.`warehouse_name` = LEFT(w.`warehouse_name`, 64) AND p.`deleted` = 0),
       'CONFLICT_NAME',
       CONCAT('名称与已有仓库重复，禁止静默 dup，已映射到最小 id')
FROM `eoms_warehouse` w
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_warehouse_map` m WHERE m.`source_id` = w.`id`
)
AND EXISTS (
    SELECT 1 FROM `psi_warehouse` p WHERE p.`warehouse_name` = LEFT(w.`warehouse_name`, 64) AND p.`deleted` = 0
);

INSERT INTO `psi_warehouse` (
    `tenant_id`, `warehouse_code`, `warehouse_name`, `manager`, `address`,
    `is_default`, `status`, `remark`, `create_user`, `create_time`, `deleted`
)
SELECT 0,
       LEFT(w.`warehouse_no`, 32),
       LEFT(w.`warehouse_name`, 64),
       LEFT(w.`manager`, 32),
       LEFT(w.`address`, 256),
       0,
       CASE WHEN w.`del_flag` <> '0' OR w.`status` = '1' THEN 2 ELSE 1 END,
       LEFT(IFNULL(w.`remark`, ''), 500),
       1,
       IFNULL(w.`create_time`, NOW()),
       0
FROM `eoms_warehouse` w
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_warehouse_map` m WHERE m.`source_id` = w.`id`
)
AND NOT EXISTS (
    SELECT 1 FROM `psi_warehouse` p WHERE p.`warehouse_code` = LEFT(w.`warehouse_no`, 32) AND p.`deleted` = 0
);

INSERT INTO `eoms_m2_warehouse_map` (`source_id`, `source_code`, `source_name`, `psi_warehouse_id`, `action`, `note`)
SELECT w.`id`, w.`warehouse_no`, w.`warehouse_name`, p.`id`, 'INSERTED', '新写入进销存仓库'
FROM `eoms_warehouse` w
INNER JOIN `psi_warehouse` p ON p.`warehouse_code` = LEFT(w.`warehouse_no`, 32) AND p.`deleted` = 0
WHERE NOT EXISTS (
    SELECT 1 FROM `eoms_m2_warehouse_map` m WHERE m.`source_id` = w.`id`
);

-- changeset ls:m2_purchase_supplier_id
-- comment M2.2：采购单供应商改绑 psi_unit
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase'
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase' AND COLUMN_NAME = 'supplier_id'
SET NAMES utf8mb4;
ALTER TABLE `eoms_purchase`
    ADD COLUMN `supplier_id` bigint(20) DEFAULT NULL COMMENT '供应商往来单位ID，关联 psi_unit.id' AFTER `purchase_no`;

-- changeset ls:m2_purchase_warehouse_id
-- comment M2.4：采购单仓库改绑 psi_warehouse
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase'
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase' AND COLUMN_NAME = 'warehouse_id'
SET NAMES utf8mb4;
ALTER TABLE `eoms_purchase`
    ADD COLUMN `warehouse_id` bigint(20) DEFAULT NULL COMMENT '仓库ID，关联 psi_warehouse.id' AFTER `supplier_id`;

-- changeset ls:m2_stock_warehouse_id
-- comment M2.4：物料库存仓库改绑 psi_warehouse
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_stock'
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_stock' AND COLUMN_NAME = 'warehouse_id'
SET NAMES utf8mb4;
ALTER TABLE `eoms_material_stock`
    ADD COLUMN `warehouse_id` bigint(20) DEFAULT NULL COMMENT '仓库ID，关联 psi_warehouse.id' AFTER `material_id`;

-- changeset ls:m2_inbound_supplier_id
-- comment M2.2：入库单供应商改绑 psi_unit
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_inbound'
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_inbound' AND COLUMN_NAME = 'supplier_id'
SET NAMES utf8mb4;
ALTER TABLE `eoms_inbound`
    ADD COLUMN `supplier_id` bigint(20) DEFAULT NULL COMMENT '供应商往来单位ID，关联 psi_unit.id';

-- changeset ls:m2_inbound_warehouse_id
-- comment M2.4：入库单仓库改绑 psi_warehouse
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_inbound'
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_inbound' AND COLUMN_NAME = 'warehouse_id'
SET NAMES utf8mb4;
ALTER TABLE `eoms_inbound`
    ADD COLUMN `warehouse_id` bigint(20) DEFAULT NULL COMMENT '仓库ID，关联 psi_warehouse.id';

-- changeset ls:m2_remap_customer_fk
-- comment M2.2：业务表 customer_id 改指 psi_unit.id
SET NAMES utf8mb4;

UPDATE `eoms_repair_apply` a
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = a.`customer_id`
SET a.`customer_id` = m.`psi_unit_id`
WHERE m.`psi_unit_id` IS NOT NULL AND a.`customer_id` <> m.`psi_unit_id`;

UPDATE `eoms_repair_order` o
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = o.`customer_id`
SET o.`customer_id` = m.`psi_unit_id`
WHERE m.`psi_unit_id` IS NOT NULL AND o.`customer_id` <> m.`psi_unit_id`;

UPDATE `eoms_quote` q
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = q.`customer_id`
SET q.`customer_id` = m.`psi_unit_id`
WHERE m.`psi_unit_id` IS NOT NULL AND q.`customer_id` <> m.`psi_unit_id`;

UPDATE `eoms_contract` ct
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = ct.`customer_id`
SET ct.`customer_id` = m.`psi_unit_id`
WHERE m.`psi_unit_id` IS NOT NULL AND ct.`customer_id` <> m.`psi_unit_id`;

UPDATE `eoms_receivable` r
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = r.`customer_id`
SET r.`customer_id` = m.`psi_unit_id`
WHERE m.`psi_unit_id` IS NOT NULL AND r.`customer_id` <> m.`psi_unit_id`;

UPDATE `eoms_payment` p
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = p.`customer_id`
SET p.`customer_id` = m.`psi_unit_id`
WHERE m.`psi_unit_id` IS NOT NULL AND p.`customer_id` <> m.`psi_unit_id`;

UPDATE `eoms_income` i
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'CUSTOMER' AND m.`source_id` = i.`customer_id`
SET i.`customer_id` = m.`psi_unit_id`
WHERE m.`psi_unit_id` IS NOT NULL AND i.`customer_id` <> m.`psi_unit_id`;

-- changeset ls:m2_remap_purchase_supplier_no
-- comment M2.2：按原供应商编号回填 supplier_id
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase' AND COLUMN_NAME = 'supplier_id'
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase' AND COLUMN_NAME = 'supplier_no'
SET NAMES utf8mb4;
UPDATE `eoms_purchase` p
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'SUPPLIER' AND m.`source_code` = p.`supplier_no`
SET p.`supplier_id` = m.`psi_unit_id`
WHERE p.`supplier_id` IS NULL AND m.`psi_unit_id` IS NOT NULL;

-- changeset ls:m2_remap_purchase_supplier_text
-- comment M2.2：官方 DDL 的 supplier 文本列按名称回填
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase' AND COLUMN_NAME = 'supplier_id'
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_purchase' AND COLUMN_NAME = 'supplier'
SET NAMES utf8mb4;
UPDATE `eoms_purchase` p
INNER JOIN `psi_unit` u ON u.`unit_name` = LEFT(p.`supplier`, 128) AND u.`deleted` = 0
    AND u.`unit_type` IN ('SUPPLIER', 'BOTH')
SET p.`supplier_id` = u.`id`
WHERE p.`supplier_id` IS NULL AND p.`supplier` IS NOT NULL AND p.`supplier` <> '';

-- changeset ls:m2_remap_stock_warehouse_no
-- comment M2.4：按原仓库编号回填 warehouse_id
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_stock' AND COLUMN_NAME = 'warehouse_id'
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_material_stock' AND COLUMN_NAME = 'warehouse_no'
SET NAMES utf8mb4;
UPDATE `eoms_material_stock` s
INNER JOIN `eoms_m2_warehouse_map` m ON m.`source_code` = s.`warehouse_no`
SET s.`warehouse_id` = m.`psi_warehouse_id`
WHERE s.`warehouse_id` IS NULL AND m.`psi_warehouse_id` IS NOT NULL;

-- changeset ls:m2_remap_inbound_nos
-- comment M2.2/M2.4：入库单供应商/仓库编号回填 ID
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_inbound'
SET NAMES utf8mb4;
UPDATE `eoms_inbound` i
INNER JOIN `eoms_m2_unit_map` m ON m.`source_kind` = 'SUPPLIER' AND m.`source_code` = i.`supplier_no`
SET i.`supplier_id` = m.`psi_unit_id`
WHERE i.`supplier_id` IS NULL AND m.`psi_unit_id` IS NOT NULL;

UPDATE `eoms_inbound` i
INNER JOIN `eoms_m2_warehouse_map` m ON m.`source_code` = i.`warehouse_no`
SET i.`warehouse_id` = m.`psi_warehouse_id`
WHERE i.`warehouse_id` IS NULL AND m.`psi_warehouse_id` IS NOT NULL;

-- changeset ls:m2_archive_eoms_customer
-- comment M2：归档 eoms_customer
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_customer'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_customer_archived`;
RENAME TABLE `eoms_customer` TO `eoms_customer_archived`;

-- changeset ls:m2_archive_eoms_customer_contact
-- comment M2：归档 eoms_customer_contact
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_customer_contact'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_customer_contact_archived`;
RENAME TABLE `eoms_customer_contact` TO `eoms_customer_contact_archived`;

-- changeset ls:m2_archive_eoms_supplier
-- comment M2：归档 eoms_supplier（若存在）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_supplier'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_supplier_archived`;
RENAME TABLE `eoms_supplier` TO `eoms_supplier_archived`;

-- changeset ls:m2_archive_eoms_warehouse
-- comment M2：归档 eoms_warehouse（若存在）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_warehouse'
SET NAMES utf8mb4;
DROP TABLE IF EXISTS `eoms_warehouse_archived`;
RENAME TABLE `eoms_warehouse` TO `eoms_warehouse_archived`;

-- changeset ls:m2_menus
-- comment M2.5：下线 EOMS 客户/供应商/仓库菜单，补 PSI 下拉权限
SET NAMES utf8mb4;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10011
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20001, 20031, 20010, 20011, 20032, 20040, 20041, 20023, 20024, 40001, 40011);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10031
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20030, 20022, 20023, 20024);

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (20001, 20002, 20003, 20004, 20005, 20030, 20031);
DELETE FROM `sys_menu` WHERE `id` IN (20001, 20002, 20003, 20004, 20005, 20030, 20031);

UPDATE `sys_menu`
SET `redirect` = '/eoms/repair/apply', `update_time` = NOW()
WHERE `id` = 20000 AND `deleted` = 0;

-- changeset ls:m2_menus_grant_psi_pages
-- comment M2.5：有维修业务的角色可见进销存往来单位/仓库页
SET NAMES utf8mb4;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10001
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10010, 10030);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10010
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20010, 20011, 20032, 20040, 20041, 40001, 10011);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10030
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20022, 20023, 20024, 10031);
