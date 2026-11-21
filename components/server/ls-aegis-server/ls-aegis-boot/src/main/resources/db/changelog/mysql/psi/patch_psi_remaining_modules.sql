-- liquibase formatted sql

-- changeset psi:6
-- validCheckSum: ANY
-- comment PSI 剩余模块表结构：期初/盘点调整/收付核销/发票/银行账户对账/合同

-- ========== 期初 ==========
CREATE TABLE IF NOT EXISTS `psi_init_stock` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`        varchar(32)   NOT NULL COMMENT '单号',
    `doc_status`    varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `init_date`     datetime      NOT NULL COMMENT '期初日期',
    `warehouse_id`  bigint(20)    NOT NULL COMMENT '仓库ID',
    `idempotent_key` varchar(64)  DEFAULT NULL COMMENT '幂等键',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`  bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`  datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_init_stock_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_init_stock_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存期初';

CREATE TABLE IF NOT EXISTS `psi_init_stock_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `init_id`      bigint(20)     NOT NULL COMMENT '期初单ID',
    `goods_id`     bigint(20)     NOT NULL COMMENT '商品ID',
    `quantity`     decimal(18,4)  NOT NULL COMMENT '数量',
    `untax_price`  decimal(18,6)  NOT NULL COMMENT '未税成本单价',
    `untax_amount` decimal(18,2)  NOT NULL COMMENT '未税金额',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_init` (`init_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存期初明细';

CREATE TABLE IF NOT EXISTS `psi_init_receivable` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`         varchar(32)   NOT NULL COMMENT '单号',
    `doc_status`     varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `init_date`      datetime      NOT NULL COMMENT '期初日期',
    `idempotent_key` varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`   bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`   datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_init_ar_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_init_ar_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收期初';

CREATE TABLE IF NOT EXISTS `psi_init_receivable_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `init_id`              bigint(20)     NOT NULL COMMENT '期初单ID',
    `unit_id`              bigint(20)     NOT NULL COMMENT '客户ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_init` (`init_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收期初明细';

CREATE TABLE IF NOT EXISTS `psi_init_payable` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`         varchar(32)   NOT NULL COMMENT '单号',
    `doc_status`     varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `init_date`      datetime      NOT NULL COMMENT '期初日期',
    `idempotent_key` varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`   bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`   datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_init_ap_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_init_ap_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应付期初';

CREATE TABLE IF NOT EXISTS `psi_init_payable_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `init_id`              bigint(20)     NOT NULL COMMENT '期初单ID',
    `unit_id`              bigint(20)     NOT NULL COMMENT '供应商ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_init` (`init_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应付期初明细';

CREATE TABLE IF NOT EXISTS `psi_init_prepay` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`         varchar(32)   NOT NULL COMMENT '单号',
    `doc_status`     varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `init_date`      datetime      NOT NULL COMMENT '期初日期',
    `idempotent_key` varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`   bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`   datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_init_pre_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_init_pre_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预付期初';

CREATE TABLE IF NOT EXISTS `psi_init_prepay_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `init_id`      bigint(20)     NOT NULL COMMENT '期初单ID',
    `unit_id`      bigint(20)     NOT NULL COMMENT '供应商ID',
    `amount`       decimal(18,2)  NOT NULL COMMENT '预付金额',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_init` (`init_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预付期初明细';

-- ========== 盘点 / 调整 ==========
CREATE TABLE IF NOT EXISTS `psi_stock_check` (
    `id`                bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`         bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`            varchar(32)   NOT NULL COMMENT '盘点单号',
    `doc_status`        varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `check_date`        datetime      NOT NULL COMMENT '盘点日期',
    `warehouse_id`      bigint(20)    NOT NULL COMMENT '仓库ID',
    `snapshot_version`  bigint(20)    NOT NULL DEFAULT 0 COMMENT '账面快照版本',
    `idempotent_key`    varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`            varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`      bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`      datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`       bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`       datetime      NOT NULL COMMENT '创建时间',
    `update_user`       bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`       datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`           bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_check_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_check_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点';

CREATE TABLE IF NOT EXISTS `psi_stock_check_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `check_id`     bigint(20)     NOT NULL COMMENT '盘点单ID',
    `goods_id`     bigint(20)     NOT NULL COMMENT '商品ID',
    `book_qty`     decimal(18,4)  NOT NULL DEFAULT 0 COMMENT '账面数量',
    `check_qty`    decimal(18,4)  NOT NULL COMMENT '实盘数量',
    `diff_qty`     decimal(18,4)  NOT NULL COMMENT '差异数量',
    `untax_price`  decimal(18,6)  DEFAULT NULL COMMENT '盘盈未税单价',
    `cost_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_check` (`check_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点明细';

CREATE TABLE IF NOT EXISTS `psi_stock_adjust` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`         varchar(32)   NOT NULL COMMENT '调整单号',
    `doc_status`     varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `adjust_date`    datetime      NOT NULL COMMENT '调整日期',
    `warehouse_id`   bigint(20)    NOT NULL COMMENT '仓库ID',
    `idempotent_key` varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`   bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`   datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_adjust_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_adjust_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调整';

CREATE TABLE IF NOT EXISTS `psi_stock_adjust_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `adjust_id`    bigint(20)     NOT NULL COMMENT '调整单ID',
    `goods_id`     bigint(20)     NOT NULL COMMENT '商品ID',
    `adjust_type`  varchar(16)    NOT NULL COMMENT 'PROFIT/LOSS',
    `quantity`     decimal(18,4)  NOT NULL COMMENT '数量',
    `untax_price`  decimal(18,6)  DEFAULT NULL COMMENT '盘盈未税单价',
    `cost_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_adjust` (`adjust_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调整明细';

-- ========== 银行账户 / 收付 / 核销 ==========
CREATE TABLE IF NOT EXISTS `psi_company_account` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `account_name`  varchar(128)  NOT NULL COMMENT '户名',
    `bank_name`     varchar(128)  NOT NULL COMMENT '开户行',
    `account_no`    varchar(64)   NOT NULL COMMENT '账号',
    `status`        tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_account_no` (`tenant_id`, `account_no`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司银行账户';

CREATE TABLE IF NOT EXISTS `psi_receipt` (
    `id`                  bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`           bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`              varchar(32)    NOT NULL COMMENT '回款单号',
    `doc_status`          varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `receipt_date`        datetime       NOT NULL COMMENT '回款日期',
    `unit_id`             bigint(20)     NOT NULL COMMENT '客户ID',
    `account_id`          bigint(20)     DEFAULT NULL COMMENT '收款账户',
    `pay_method`          varchar(32)    DEFAULT NULL COMMENT '收款方式',
    `amount`              decimal(18,2)  NOT NULL COMMENT '金额',
    `unallocated_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '未分配金额',
    `idempotent_key`      varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `remark`              varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`        bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`        datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`         bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`         datetime       NOT NULL COMMENT '创建时间',
    `update_user`         bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`         datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`             bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_receipt_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_receipt_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_unit` (`unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回款登记';

CREATE TABLE IF NOT EXISTS `psi_payment` (
    `id`                  bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`           bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`              varchar(32)    NOT NULL COMMENT '付款单号',
    `doc_status`          varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `payment_date`        datetime       NOT NULL COMMENT '付款日期',
    `unit_id`             bigint(20)     NOT NULL COMMENT '供应商ID',
    `account_id`          bigint(20)     DEFAULT NULL COMMENT '付款账户',
    `pay_method`          varchar(32)    DEFAULT NULL COMMENT '付款方式',
    `amount`              decimal(18,2)  NOT NULL COMMENT '金额',
    `unallocated_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '未分配金额',
    `idempotent_key`      varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `remark`              varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`        bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`        datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`         bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`         datetime       NOT NULL COMMENT '创建时间',
    `update_user`         bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`         datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`             bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_payment_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_payment_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_unit` (`unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款登记';

CREATE TABLE IF NOT EXISTS `psi_write_off` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`        varchar(32)   NOT NULL COMMENT '核销单号',
    `write_off_date` datetime     NOT NULL COMMENT '核销日期',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_writeoff_no` (`tenant_id`, `doc_no`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核销主表';

CREATE TABLE IF NOT EXISTS `psi_write_off_item` (
    `id`            bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `write_off_id`  bigint(20)     NOT NULL COMMENT '核销主表ID',
    `receipt_id`    bigint(20)     DEFAULT NULL COMMENT '回款ID',
    `payment_id`    bigint(20)     DEFAULT NULL COMMENT '付款ID',
    `open_type`     varchar(8)     NOT NULL COMMENT 'AR/AP',
    `open_id`       bigint(20)     NOT NULL COMMENT '开口ID',
    `direction`     varchar(16)    NOT NULL COMMENT 'WRITE_OFF/REVERSE',
    `amount`        decimal(18,2)  NOT NULL COMMENT '金额',
    `reason`        varchar(500)   DEFAULT NULL COMMENT '原因',
    `create_user`   bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`   datetime       NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_writeoff` (`write_off_id`),
    INDEX `idx_receipt` (`receipt_id`),
    INDEX `idx_payment` (`payment_id`),
    INDEX `idx_open` (`open_type`, `open_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核销明细';

-- ========== 发票 ==========
CREATE TABLE IF NOT EXISTS `psi_invoice_out` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`               varchar(32)    NOT NULL COMMENT '系统单号',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `invoice_no`           varchar(64)    NOT NULL COMMENT '发票号码',
    `invoice_type`         varchar(16)    NOT NULL COMMENT 'SPECIAL/NORMAL',
    `invoice_date`         datetime       NOT NULL COMMENT '开票日期',
    `unit_id`              bigint(20)     NOT NULL COMMENT '客户ID',
    `buyer_tax_no`         varchar(64)    NOT NULL COMMENT '购方税号',
    `seller_tax_no`        varchar(64)    NOT NULL COMMENT '销方税号',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `red_of_id`            bigint(20)     DEFAULT NULL COMMENT '冲红蓝字发票ID',
    `idempotent_key`       varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `file_id`              bigint(20)     DEFAULT NULL COMMENT '附件',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`         bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`         datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_inv_out_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_inv_out_biz` (`tenant_id`, `invoice_no`, `buyer_tax_no`, `seller_tax_no`, `invoice_type`, `deleted`),
    UNIQUE INDEX `uk_inv_out_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售开票';

CREATE TABLE IF NOT EXISTS `psi_invoice_out_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `invoice_id`           bigint(20)     NOT NULL COMMENT '发票ID',
    `goods_name`           varchar(128)   NOT NULL COMMENT '货物或应税劳务',
    `quantity`             decimal(18,4)  DEFAULT NULL COMMENT '数量',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `tax_rate`             decimal(5,2)   NOT NULL COMMENT '税率',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销项发票明细';

CREATE TABLE IF NOT EXISTS `psi_invoice_out_link` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `invoice_id`   bigint(20)     NOT NULL COMMENT '发票ID',
    `stock_out_id` bigint(20)     NOT NULL COMMENT '出库单ID',
    `amount`       decimal(18,2)  NOT NULL COMMENT '关联含税金额',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_stock_out` (`stock_out_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销项发票关联出库';

CREATE TABLE IF NOT EXISTS `psi_invoice_in` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `doc_no`               varchar(32)    NOT NULL COMMENT '系统单号',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `invoice_no`           varchar(64)    NOT NULL COMMENT '发票号码',
    `invoice_type`         varchar(16)    NOT NULL COMMENT 'SPECIAL/NORMAL/NO_INVOICE',
    `invoice_date`         datetime       NOT NULL COMMENT '收票日期',
    `unit_id`              bigint(20)     NOT NULL COMMENT '供应商ID',
    `buyer_tax_no`         varchar(64)    NOT NULL COMMENT '购方税号',
    `seller_tax_no`        varchar(64)    NOT NULL COMMENT '销方税号',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `red_of_id`            bigint(20)     DEFAULT NULL COMMENT '冲红蓝字发票ID',
    `idempotent_key`       varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `file_id`              bigint(20)     DEFAULT NULL COMMENT '附件',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`         bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`         datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_inv_in_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_inv_in_biz` (`tenant_id`, `invoice_no`, `buyer_tax_no`, `seller_tax_no`, `invoice_type`, `deleted`),
    UNIQUE INDEX `uk_inv_in_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购收票';

CREATE TABLE IF NOT EXISTS `psi_invoice_in_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `invoice_id`           bigint(20)     NOT NULL COMMENT '发票ID',
    `goods_name`           varchar(128)   NOT NULL COMMENT '货物或应税劳务',
    `quantity`             decimal(18,4)  DEFAULT NULL COMMENT '数量',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `tax_rate`             decimal(5,2)   NOT NULL COMMENT '税率',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进项发票明细';

CREATE TABLE IF NOT EXISTS `psi_invoice_in_link` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `invoice_id`   bigint(20)     NOT NULL COMMENT '发票ID',
    `stock_in_id`  bigint(20)     NOT NULL COMMENT '入库单ID',
    `amount`       decimal(18,2)  NOT NULL COMMENT '关联含税金额',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_stock_in` (`stock_in_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进项发票关联入库';

-- ========== 银行对账 ==========
CREATE TABLE IF NOT EXISTS `psi_bank_statement` (
    `id`             bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`        bigint(20)     NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `account_id`     bigint(20)     NOT NULL COMMENT '账户ID',
    `stmt_date`      datetime       NOT NULL COMMENT '流水日期',
    `direction`      varchar(8)     NOT NULL COMMENT 'IN/OUT',
    `amount`         decimal(18,2)  NOT NULL COMMENT '金额',
    `counterparty`   varchar(128)   DEFAULT NULL COMMENT '对方户名',
    `summary`        varchar(256)   DEFAULT NULL COMMENT '摘要',
    `bank_seq_no`    varchar(64)    DEFAULT NULL COMMENT '银行流水号',
    `fingerprint`    varchar(64)    NOT NULL COMMENT '去重指纹SM3',
    `match_status`   varchar(16)    NOT NULL DEFAULT 'UNMATCHED' COMMENT '匹配状态',
    `match_biz_type` varchar(32)    DEFAULT NULL COMMENT '匹配业务类型',
    `match_biz_id`   bigint(20)     DEFAULT NULL COMMENT '匹配业务ID',
    `create_user`    bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`    datetime       NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_bank_fp` (`tenant_id`, `account_id`, `fingerprint`, `deleted`),
    UNIQUE INDEX `uk_bank_seq` (`tenant_id`, `account_id`, `bank_seq_no`, `deleted`),
    INDEX `idx_match` (`match_status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行流水';

-- ========== 合同 ==========
CREATE TABLE IF NOT EXISTS `psi_contract_sale` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `contract_no`          varchar(32)    NOT NULL COMMENT '合同编号',
    `contract_name`        varchar(128)   NOT NULL COMMENT '合同名称',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `sign_date`            datetime       DEFAULT NULL COMMENT '签订日',
    `unit_id`              bigint(20)     NOT NULL COMMENT '客户ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税总额',
    `start_date`           datetime       DEFAULT NULL COMMENT '开始日',
    `end_date`             datetime       DEFAULT NULL COMMENT '结束日',
    `file_id`              bigint(20)     DEFAULT NULL COMMENT 'PDF附件',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_cs_no` (`tenant_id`, `contract_no`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售合同';

CREATE TABLE IF NOT EXISTS `psi_contract_purchase` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `contract_no`          varchar(32)    NOT NULL COMMENT '合同编号',
    `contract_name`        varchar(128)   NOT NULL COMMENT '合同名称',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `sign_date`            datetime       DEFAULT NULL COMMENT '签订日',
    `unit_id`              bigint(20)     NOT NULL COMMENT '供应商ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税总额',
    `start_date`           datetime       DEFAULT NULL COMMENT '开始日',
    `end_date`             datetime       DEFAULT NULL COMMENT '结束日',
    `file_id`              bigint(20)     DEFAULT NULL COMMENT 'PDF附件',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_cp_no` (`tenant_id`, `contract_no`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购合同';
