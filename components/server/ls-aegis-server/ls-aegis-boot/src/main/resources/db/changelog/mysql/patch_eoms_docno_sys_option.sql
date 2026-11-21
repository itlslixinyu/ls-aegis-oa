-- liquibase formatted sql

-- changeset ls:eoms_docno_sys_option
-- comment C1：EOMS 编号前缀迁入 sys_option（category=EOMS），供 DocNoService 读取
SET NAMES utf8mb4;

INSERT INTO `sys_option`
(`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
VALUES
(51, 'EOMS', '客户编号前缀', 'CUSTOMER_NO_PREFIX', NULL, 'KH', '客户档案自动编号前缀（2～4 位大写字母）'),
(52, 'EOMS', '报修编号前缀', 'REPAIR_NO_PREFIX', NULL, 'BX', '报修申请自动编号前缀'),
(53, 'EOMS', '工单编号前缀', 'ORDER_NO_PREFIX', NULL, 'GD', '维修工单自动编号前缀'),
(54, 'EOMS', '报价编号前缀', 'QUOTE_NO_PREFIX', NULL, 'BJ', '报价单自动编号前缀'),
(55, 'EOMS', '合同编号前缀', 'CONTRACT_NO_PREFIX', NULL, 'HT', '维修合同自动编号前缀'),
(56, 'EOMS', '采购编号前缀', 'PURCHASE_NO_PREFIX', NULL, 'CG', '采购单自动编号前缀'),
(57, 'EOMS', '收入编号前缀', 'INCOME_NO_PREFIX', NULL, 'SR', '收入单自动编号前缀'),
(58, 'EOMS', '收款编号前缀', 'PAYMENT_NO_PREFIX', NULL, 'SK', '收款单自动编号前缀'),
(59, 'EOMS', '应收编号前缀', 'RECEIVABLE_NO_PREFIX', NULL, 'YS', '应收单自动编号前缀'),
(60, 'EOMS', '物料编号前缀', 'MATERIAL_NO_PREFIX', NULL, 'WL', '物料档案自动编号前缀（分类层级编码另算）'),
(61, 'EOMS', '物料分类编号前缀', 'CATEGORY_NO_PREFIX', NULL, 'WL', '物料分类自动编号前缀'),
(62, 'EOMS', '结算编号前缀', 'SETTLEMENT_NO_PREFIX', NULL, 'JS', '维修结算单自动编号前缀'),
(63, 'EOMS', '仓库编号前缀', 'WAREHOUSE_NO_PREFIX', NULL, 'CK', '仓库档案自动编号前缀'),
(64, 'EOMS', '供应商编号前缀', 'SUPPLIER_NO_PREFIX', NULL, 'GYS', '供应商档案自动编号前缀'),
(65, 'EOMS', '入库编号前缀', 'INBOUND_NO_PREFIX', NULL, 'RK', '入库单自动编号前缀');
