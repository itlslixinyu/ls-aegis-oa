-- liquibase formatted sql

-- changeset ls:r1_finance_menu_rename
-- comment R1：菜单改为维修应收/维修收款，路由避开 PSI /psi/payment
SET NAMES utf8mb4;

UPDATE `sys_menu` SET `title` = '维修应收' WHERE `id` = 20040;

UPDATE `sys_menu`
SET `title` = '维修收款',
    `path` = '/eoms/finance/receipt',
    `name` = 'EomsFinanceReceipt',
    `component` = 'eoms/finance/receipt'
WHERE `id` = 20041;

-- changeset ls:r3_quote_detail_line_type
-- comment R3：报价明细区分材料/服务行，材料可绑 psi_goods
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_quote_detail' AND COLUMN_NAME = 'line_type'
SET NAMES utf8mb4;

ALTER TABLE `eoms_quote_detail`
  ADD COLUMN `line_type` varchar(16) NOT NULL DEFAULT 'SERVICE' COMMENT 'MATERIAL材料 / SERVICE服务' AFTER `quote_id`,
  ADD COLUMN `goods_id` bigint(20) DEFAULT NULL COMMENT '材料行关联 psi_goods.id' AFTER `line_type`,
  ADD KEY `idx_qd_goods` (`goods_id`);
