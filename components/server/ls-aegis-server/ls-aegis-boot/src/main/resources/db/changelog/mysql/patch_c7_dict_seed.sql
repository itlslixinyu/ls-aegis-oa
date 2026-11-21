-- liquibase formatted sql

-- changeset ls:c7_dict_seed
-- comment C7：业务字典种子（PSI/EOMS/Sign 标签走 sys_dict，已存在则跳过）
SET NAMES utf8mb4;

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 105, 'PSI来源类型', 'psi_source_type', '库存流水/应收应付来源', b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'psi_source_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 106, 'PSI流水类型', 'psi_flow_type', '库存流水变动类型', b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'psi_flow_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 107, 'PSI发票类型', 'psi_invoice_type', '专票/普票', b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'psi_invoice_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 108, 'PSI退货类型', 'psi_return_type', '销售退货/采购退货', b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'psi_return_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 109, 'PSI打印业务类型', 'psi_print_biz_type', '打印模板业务类型', b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'psi_print_biz_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 113, 'PSI开口状态', 'psi_open_status', '应收应付结清状态', b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'psi_open_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 200, 'EOMS报修状态', 'eoms_apply_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_apply_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 201, 'EOMS工单状态', 'eoms_order_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_order_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 202, 'EOMS紧急程度', 'eoms_urgent_level', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_urgent_level' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 203, 'EOMS故障类型', 'eoms_fault_type', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_fault_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 204, 'EOMS报价状态', 'eoms_quote_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_quote_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 205, 'EOMS应收状态', 'eoms_receivable_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_receivable_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 206, 'EOMS结算状态', 'eoms_settle_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_settle_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 207, 'EOMS客户类型', 'eoms_customer_type', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_customer_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 208, 'EOMS仓库类型', 'eoms_warehouse_type', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_warehouse_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 209, 'EOMS入库状态', 'eoms_inbound_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_inbound_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 210, 'EOMS合同状态', 'eoms_contract_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_contract_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 211, 'EOMS收款方式', 'eoms_payment_method', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_payment_method' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 212, 'EOMS启用状态', 'eoms_enable_status', '0正常 1停用', b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_enable_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 213, 'EOMS报修来源', 'eoms_apply_source', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'eoms_apply_source' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 300, '签章印章类型', 'sign_seal_category', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'sign_seal_category' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 301, '签章项目状态', 'sign_project_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'sign_project_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 302, '签章合同类型', 'sign_contract_type', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'sign_contract_type' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 303, '签章方式', 'sign_mode', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'sign_mode' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 304, '签章审计动作', 'sign_audit_action', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'sign_audit_action' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 305, '签章提取类型', 'sign_seal_extract_category', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'sign_seal_extract_category' AND `deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 150, '采购入库', 'STOCK_IN', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'STOCK_IN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 151, '销售出库', 'STOCK_OUT', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'STOCK_OUT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 152, '销售退货', 'SALE_RETURN', 'warning', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SALE_RETURN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 153, '采购退货', 'PURCHASE_RETURN', 'warning', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PURCHASE_RETURN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 154, '应收期初', 'INIT_AR', 'default', 5, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'INIT_AR' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 155, '应付期初', 'INIT_AP', 'default', 6, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'INIT_AP' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 156, '期初库存', 'INIT_STOCK', 'default', 7, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'INIT_STOCK' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 157, '预付期初', 'INIT_PREPAY', 'default', 8, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'INIT_PREPAY' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 158, '仓间调拨', 'TRANSFER', 'primary', 9, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'TRANSFER' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 159, '盘点', 'STOCK_CHECK', 'warning', 10, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'STOCK_CHECK' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 160, '库存调整', 'STOCK_ADJUST', 'warning', 11, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'STOCK_ADJUST' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 161, '回款', 'RECEIPT', 'success', 12, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'RECEIPT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 162, '付款', 'PAYMENT', 'error', 13, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PAYMENT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 163, '销项发票', 'INVOICE_OUT', 'primary', 14, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'INVOICE_OUT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 164, '进项发票', 'INVOICE_IN', 'primary', 15, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'INVOICE_IN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 165, '销售合同', 'CONTRACT_SALE', 'primary', 16, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CONTRACT_SALE' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 166, '采购合同', 'CONTRACT_PURCHASE', 'success', 17, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_source_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CONTRACT_PURCHASE' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 170, '入库', 'IN', 'success', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_flow_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'IN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 171, '出库', 'OUT', 'error', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_flow_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'OUT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 172, '调拨入', 'TRANSFER_IN', 'primary', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_flow_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'TRANSFER_IN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 173, '调拨出', 'TRANSFER_OUT', 'warning', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_flow_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'TRANSFER_OUT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 174, '盘盈', 'PROFIT', 'success', 5, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_flow_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PROFIT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 175, '盘亏', 'LOSS', 'error', 6, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_flow_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'LOSS' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 176, '调整', 'ADJUST', 'warning', 7, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_flow_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'ADJUST' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 180, '专票', 'SPECIAL', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_invoice_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SPECIAL' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 181, '普票', 'NORMAL', 'default', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_invoice_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'NORMAL' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 182, '销售退货', 'SALE_RETURN', 'warning', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_return_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SALE_RETURN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 183, '采购退货', 'PURCHASE_RETURN', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_return_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PURCHASE_RETURN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 184, '入库单', 'STOCK_IN', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_print_biz_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'STOCK_IN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 185, '出库单', 'STOCK_OUT', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_print_biz_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'STOCK_OUT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 186, '未结清', 'OPEN', 'warning', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_open_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'OPEN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 187, '已结清', 'CLOSED', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'psi_open_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CLOSED' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 400, '待处理', 'WAIT', 'warning', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_apply_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'WAIT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 401, '已确认', 'CONFIRM', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_apply_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CONFIRM' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 402, '已转工单', 'ORDER', 'success', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_apply_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'ORDER' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 403, '已取消', 'CANCEL', 'default', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_apply_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CANCEL' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 410, '待接单', 'WAIT_ACCEPT', 'warning', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_order_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'WAIT_ACCEPT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 411, '已接单', 'ACCEPTED', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_order_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'ACCEPTED' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 412, '维修中', 'PROCESSING', 'success', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_order_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PROCESSING' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 413, '待结算', 'WAIT_SETTLEMENT', 'warning', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_order_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'WAIT_SETTLEMENT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 414, '已完成', 'FINISHED', 'success', 5, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_order_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'FINISHED' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 415, '已关闭', 'CLOSED', 'default', 6, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_order_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CLOSED' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 420, '一般', 'NORMAL', 'success', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_urgent_level' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'NORMAL' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 421, '紧急', 'URGENT', 'warning', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_urgent_level' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'URGENT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 422, '特急', 'VERY_URGENT', 'error', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_urgent_level' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'VERY_URGENT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 430, '电脑设备故障', '电脑设备故障', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_fault_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '电脑设备故障' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 431, '监控设备故障', '监控设备故障', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_fault_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '监控设备故障' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 432, '办公设备故障', '办公设备故障', 'primary', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_fault_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '办公设备故障' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 433, '通用基础故障', '通用基础故障', 'default', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_fault_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '通用基础故障' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 440, '草稿', 'DRAFT', 'default', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_quote_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'DRAFT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 441, '已提交', 'SUBMIT', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_quote_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SUBMIT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 442, '已确认', 'CONFIRM', 'success', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_quote_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CONFIRM' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 443, '已取消', 'CANCEL', 'error', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_quote_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CANCEL' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 450, '未收款', 'UNPAID', 'warning', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_receivable_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'UNPAID' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 451, '部分收款', 'PARTIAL', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_receivable_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PARTIAL' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 452, '已结清', 'PAID', 'success', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_receivable_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PAID' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 460, '草稿', 'DRAFT', 'default', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_settle_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'DRAFT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 461, '已确认', 'CONFIRMED', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_settle_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CONFIRMED' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 470, '企业客户', 'ENTERPRISE', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_customer_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'ENTERPRISE' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 471, '个人客户', 'PERSON', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_customer_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PERSON' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 480, '主仓', 'MAIN', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_warehouse_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'MAIN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 481, '备件仓', 'SPARE', 'warning', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_warehouse_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SPARE' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 490, '草稿', 'DRAFT', 'default', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_inbound_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'DRAFT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 491, '已提交', 'SUBMIT', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_inbound_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SUBMIT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 500, '生效中', 'EFFECTIVE', 'success', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_contract_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'EFFECTIVE' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 501, '已到期', 'EXPIRED', 'warning', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_contract_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'EXPIRED' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 502, '已终止', 'TERMINATED', 'error', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_contract_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'TERMINATED' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 510, '现金', '现金', 'success', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_payment_method' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '现金' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 511, '转账', '转账', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_payment_method' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '转账' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 512, '微信', '微信', 'success', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_payment_method' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '微信' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 513, '支付宝', '支付宝', 'primary', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_payment_method' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '支付宝' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 520, '正常', '0', 'success', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_enable_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '0' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 521, '停用', '1', 'error', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_enable_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '1' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 530, '客户H5', 'H5', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_apply_source' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'H5' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 531, '后台登记', 'ADMIN', 'default', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_apply_source' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'ADMIN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 600, '法定代表人章', 'legal_rep', 'error', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_category' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'legal_rep' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 601, '合同章', 'contract', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_category' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'contract' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 602, '财务章', 'finance', 'warning', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_category' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'finance' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 603, '公司公章', 'company', 'success', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_category' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'company' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 610, '待审核', '0', 'warning', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_project_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '0' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 611, '未开始', '1', 'default', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_project_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '1' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 612, '进行中', '2', 'primary', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_project_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '2' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 613, '已完成', '3', 'success', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_project_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '3' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 614, '已暂停', '4', 'warning', 5, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_project_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '4' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 615, '已取消', '5', 'error', 6, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_project_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '5' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 620, '销售合同', 'SALE', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_contract_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SALE' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 621, '采购合同', 'PURCHASE', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_contract_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'PURCHASE' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 622, '维修合同', 'REPAIR', 'warning', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_contract_type' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'REPAIR' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 630, '单次签章', 'single', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_mode' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'single' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 631, '连续签章', 'continuous', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_mode' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'continuous' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 632, '跨页签章', 'span', 'warning', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_mode' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'span' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 633, '固定位置', 'fixed', 'default', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_mode' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'fixed' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 634, '合并签章', 'unified', 'primary', 5, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_mode' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'unified' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 640, '登录', 'LOGIN', 'primary', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_audit_action' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'LOGIN' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 641, '登出', 'LOGOUT', 'default', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_audit_action' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'LOGOUT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 642, '新增签章', 'SIGN_ADD', 'success', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_audit_action' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SIGN_ADD' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 643, '删除签章', 'SIGN_DELETE', 'error', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_audit_action' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SIGN_DELETE' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 644, '下载签章', 'SIGN_DOWNLOAD', 'primary', 5, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_audit_action' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'SIGN_DOWNLOAD' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 645, '授权', 'AUTH', 'warning', 6, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_audit_action' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'AUTH' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 646, '指纹校验', 'FINGERPRINT', 'success', 7, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_audit_action' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'FINGERPRINT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 650, '公章', '公章', 'success', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_extract_category' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '公章' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 651, '合同章', '合同章', 'primary', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_extract_category' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '合同章' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 652, '财务章', '财务章', 'warning', 3, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_extract_category' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '财务章' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 653, '法人章', '法人章', 'error', 4, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_extract_category' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '法人章' AND i.`deleted` = 0);
