-- liquibase formatted sql

-- changeset psi:3
-- validCheckSum: ANY
-- comment PSI 退货/调拨表结构与菜单

CREATE TABLE IF NOT EXISTS `psi_stock_return` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`               varchar(32)    NOT NULL COMMENT '退货单号',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `return_type`          varchar(32)    NOT NULL COMMENT 'SALE_RETURN/PURCHASE_RETURN',
    `return_date`          datetime       NOT NULL COMMENT '退货日期',
    `warehouse_id`         bigint(20)     NOT NULL COMMENT '仓库ID',
    `unit_id`              bigint(20)     DEFAULT NULL COMMENT '往来单位ID',
    `origin_stock_out_id`  bigint(20)     DEFAULT NULL COMMENT '原出库单ID',
    `origin_stock_in_id`   bigint(20)     DEFAULT NULL COMMENT '原入库单ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '含税总金额',
    `untax_amount`         decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '未税总金额',
    `tax_amount`           decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '税额',
    `cost_amount`          decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本金额',
    `idempotent_key`       varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`         bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`         datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_return_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_return_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_origin_out` (`origin_stock_out_id`),
    INDEX `idx_origin_in` (`origin_stock_in_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货单';

CREATE TABLE IF NOT EXISTS `psi_stock_return_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `return_id`            bigint(20)     NOT NULL COMMENT '退货单ID',
    `origin_item_id`       bigint(20)     NOT NULL COMMENT '原出入库明细ID',
    `goods_id`             bigint(20)     NOT NULL COMMENT '商品ID',
    `quantity`             decimal(18,4)  NOT NULL COMMENT '退货数量',
    `tax_inclusive_price`  decimal(18,6)  NOT NULL COMMENT '含税单价',
    `tax_rate`             decimal(5,2)   NOT NULL COMMENT '税率',
    `untax_price`          decimal(18,6)  NOT NULL COMMENT '未税单价',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `cost_amount`          decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_return` (`return_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货明细';

CREATE TABLE IF NOT EXISTS `psi_stock_transfer` (
    `id`                 bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`          bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`             varchar(32)   NOT NULL COMMENT '调拨单号',
    `doc_status`         varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `transfer_date`      datetime      NOT NULL COMMENT '调拨日期',
    `from_warehouse_id`  bigint(20)    NOT NULL COMMENT '调出仓',
    `to_warehouse_id`    bigint(20)    NOT NULL COMMENT '调入仓',
    `idempotent_key`     varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`             varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`       bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`       datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`        bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`        datetime      NOT NULL COMMENT '创建时间',
    `update_user`        bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`        datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`            bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_transfer_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_transfer_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓间调拨单';

CREATE TABLE IF NOT EXISTS `psi_stock_transfer_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `transfer_id`  bigint(20)     NOT NULL COMMENT '调拨单ID',
    `goods_id`     bigint(20)     NOT NULL COMMENT '商品ID',
    `quantity`     decimal(18,4)  NOT NULL COMMENT '数量',
    `cost_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_transfer` (`transfer_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨明细';

CREATE TABLE IF NOT EXISTS `psi_todo` (
    `id`           bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `todo_type`    varchar(32)   NOT NULL COMMENT '待办类型',
    `title`        varchar(200)  NOT NULL COMMENT '标题',
    `biz_type`     varchar(32)   DEFAULT NULL COMMENT '业务类型',
    `biz_id`       bigint(20)    DEFAULT NULL COMMENT '业务ID',
    `biz_no`       varchar(64)   DEFAULT NULL COMMENT '业务单号',
    `done`         tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否完成',
    `create_user`  bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`  datetime      NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_todo_open` (`tenant_id`, `done`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PSI待办';

INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10060, '退货管理', 10002, 2, '/psi/stock-return', 'PsiStockReturn', 'psi/stock-return/index', NULL, 'reply', b'0', b'0', b'0', NULL, 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10060);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10061, '列表', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:list', 1, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10061);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10062, '详情', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:get', 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10062);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10063, '新增', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:create', 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10063);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10064, '修改', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:update', 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10064);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10065, '删除', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:delete', 5, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10065);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10066, '确认', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:confirm', 6, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10066);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10070, '仓间调拨', 10002, 2, '/psi/stock-transfer', 'PsiStockTransfer', 'psi/stock-transfer/index', NULL, 'swap', b'0', b'0', b'0', NULL, 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10070);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10071, '列表', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:list', 1, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10071);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10072, '详情', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:get', 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10072);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10073, '新增', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:create', 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10073);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10074, '修改', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:update', 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10074);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10075, '删除', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:delete', 5, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10075);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10076, '确认', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:confirm', 6, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10076);
