-- liquibase formatted sql

-- changeset psi:11 validCheckSum:ANY
-- comment PSI：往来单位子表（联系人/开票/收货地址/收款信息）

CREATE TABLE IF NOT EXISTS `psi_unit_contact` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `contact_name`  varchar(64)   NOT NULL COMMENT '联系人',
    `mobile`        varchar(32)   DEFAULT NULL COMMENT '手机',
    `phone`         varchar(32)   DEFAULT NULL COMMENT '电话',
    `email`         varchar(128)  DEFAULT NULL COMMENT '邮箱',
    `position`      varchar(64)   DEFAULT NULL COMMENT '职务',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='往来单位联系人';

CREATE TABLE IF NOT EXISTS `psi_unit_invoice` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `title`         varchar(128)  NOT NULL COMMENT '发票抬头',
    `tax_no`        varchar(64)   DEFAULT NULL COMMENT '税号',
    `bank_name`     varchar(128)  DEFAULT NULL COMMENT '开户行',
    `account_no`    varchar(64)   DEFAULT NULL COMMENT '账号',
    `address`       varchar(256)  DEFAULT NULL COMMENT '地址',
    `phone`         varchar(32)   DEFAULT NULL COMMENT '电话',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='往来单位开票信息';

CREATE TABLE IF NOT EXISTS `psi_unit_address` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `contact_name`  varchar(64)   DEFAULT NULL COMMENT '收货人',
    `mobile`        varchar(32)   DEFAULT NULL COMMENT '手机',
    `region`        varchar(128)  DEFAULT NULL COMMENT '省市区',
    `address`       varchar(256)  NOT NULL COMMENT '详细地址',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='往来单位收货地址';

CREATE TABLE IF NOT EXISTS `psi_unit_pay` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `account_name`  varchar(128)  NOT NULL COMMENT '户名',
    `bank_name`     varchar(128)  DEFAULT NULL COMMENT '开户行',
    `account_no`    varchar(64)   NOT NULL COMMENT '账号',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='往来单位收款信息';
