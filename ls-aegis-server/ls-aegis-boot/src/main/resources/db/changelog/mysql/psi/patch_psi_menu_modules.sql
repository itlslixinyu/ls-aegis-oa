-- liquibase formatted sql

-- changeset psi:4
-- validCheckSum: ANY
-- comment PSI 菜单按四大模块归位：基础数据/库存业务/财务管理/合同管理
-- 兼容已执行旧种子的环境库：目录缺失则补、父级纠偏、叶子菜单幂等插入

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10001, '基础数据', 0, 1, '/psi/master', 'PsiMaster', 'Layout', '/psi/unit', 'folder', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10001);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10002, '库存业务', 0, 1, '/psi/stock', 'PsiStock', 'Layout', '/psi/stock-in', 'archive', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10002);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10003, '财务管理', 0, 1, '/psi/finance', 'PsiFinance', 'Layout', '/psi/receipt', 'stamp', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10003);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10004, '合同管理', 0, 1, '/psi/contract', 'PsiContract', 'Layout', '/psi/contract-sale', 'menu-file', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10004);

UPDATE `sys_menu` SET `parent_id` = 10001, `sort` = 1 WHERE `id` = 10010;
UPDATE `sys_menu` SET `parent_id` = 10001, `sort` = 2 WHERE `id` = 10020;
UPDATE `sys_menu` SET `parent_id` = 10001, `sort` = 3 WHERE `id` = 10030;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 1 WHERE `id` = 10040;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 2 WHERE `id` = 10050;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 3 WHERE `id` = 10060;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 4 WHERE `id` = 10070;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10080, '期初建账', 10001, 2, '/psi/init', 'PsiInit', 'psi/init/index', NULL, 'calendar', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10080);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10081, '列表', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10081);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10082, '确认', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:confirm', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10082);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10090, '库存盘点', 10002, 2, '/psi/stock-check', 'PsiStockCheck', 'psi/stock-check/index', NULL, 'select-all', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10090);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10091, '列表', 10090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:check:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10091);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10092, '新增', 10090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:check:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10092);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10093, '确认', 10090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:check:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10093);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10100, '库存调整', 10002, 2, '/psi/stock-adjust', 'PsiStockAdjust', 'psi/stock-adjust/index', NULL, 'tool', b'0', b'0', b'0', NULL, 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10100);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10101, '列表', 10100, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:adjust:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10101);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10102, '新增', 10100, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:adjust:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10102);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10103, '确认', 10100, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:adjust:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10103);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10110, '销售开票', 10003, 2, '/psi/invoice-out', 'PsiInvoiceOut', 'psi/invoice-out/index', NULL, 'export', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10110);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10111, '列表', 10110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-out:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10111);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10112, '新增', 10110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-out:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10112);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10113, '确认', 10110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-out:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10113);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10114, '冲红', 10110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-out:red', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10114);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10120, '采购收票', 10003, 2, '/psi/invoice-in', 'PsiInvoiceIn', 'psi/invoice-in/index', NULL, 'import', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10120);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10121, '列表', 10120, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-in:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10121);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10122, '新增', 10120, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-in:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10122);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10123, '确认', 10120, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-in:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10123);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10124, '冲红', 10120, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-in:red', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10124);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10130, '回款登记', 10003, 2, '/psi/receipt', 'PsiReceipt', 'psi/receipt/index', NULL, 'share-alt', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10130);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10131, '列表', 10130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receipt:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10131);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10132, '新增', 10130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receipt:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10132);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10133, '确认', 10130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receipt:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10133);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10134, '核销', 10130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receipt:writeoff', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10134);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10140, '付款登记', 10003, 2, '/psi/payment', 'PsiPayment', 'psi/payment/index', NULL, 'share-internal', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10140);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10141, '列表', 10140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payment:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10141);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10142, '新增', 10140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payment:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10142);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10143, '确认', 10140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payment:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10143);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10144, '核销', 10140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payment:writeoff', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10144);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10150, '银行账户', 10003, 2, '/psi/account', 'PsiAccount', 'psi/account/index', NULL, 'safe', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10150);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10151, '列表', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10151);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10160, '银行对账', 10003, 2, '/psi/bank', 'PsiBank', 'psi/bank/index', NULL, 'find-replace', b'0', b'0', b'0', NULL, 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10160);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10161, '列表', 10160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:bank:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10161);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10162, '导入', 10160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:bank:import', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10162);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10163, '匹配', 10160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:bank:match', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10163);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10170, '销售合同', 10004, 2, '/psi/contract-sale', 'PsiContractSale', 'psi/contract-sale/index', NULL, 'drive-file', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10170);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10171, '列表', 10170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contract:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10171);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10172, '新增', 10170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contract:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10172);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10173, '修改', 10170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contract:update', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10173);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10180, '采购合同', 10004, 2, '/psi/contract-purchase', 'PsiContractPurchase', 'psi/contract-purchase/index', NULL, 'bookmark', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10180);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10181, '列表', 10180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contract:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10181);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10182, '新增', 10180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contract:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10182);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10183, '修改', 10180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contract:update', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10183);
