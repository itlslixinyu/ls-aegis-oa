-- liquibase formatted sql

-- changeset aegis:eoms_menu
-- comment P5：写入 eoms 平台菜单 20000 段
SET NAMES utf8mb4;

-- EOMS 平台菜单（ID 段 20000–20999）
-- 幂等：先删角色绑定与菜单，再写入；可重复执行
-- 组件路径须与前端 asyncModules 键一致（无前导 /、无 .vue）

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 20000 AND 20999;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 20000 AND 20999;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
-- 顶层 Layout
(20000, '运维报修', 0, 1, '/eoms', 'EomsRoot', 'Layout', '/eoms/customer', 'tool', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()),

-- 客户
(20001, '客户管理', 20000, 2, '/eoms/customer', 'EomsCustomer', 'eoms/customer/index', NULL, 'user-group', b'0', b'0', b'0', 'eoms:customer:list', 1, 1, 1, NOW()),
(20002, '新增', 20001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:customer:add', 1, 1, 1, NOW()),
(20003, '修改', 20001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:customer:update', 2, 1, 1, NOW()),
(20004, '删除', 20001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:customer:delete', 3, 1, 1, NOW()),
(20005, '导出', 20001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:customer:export', 4, 1, 1, NOW()),

-- 报修
(20010, '报修申请', 20000, 2, '/eoms/repair/apply', 'EomsRepairApply', 'eoms/repair/apply', NULL, 'file', b'0', b'0', b'0', 'eoms:repair:apply:list', 2, 1, 1, NOW()),
(20011, '报修工单', 20000, 2, '/eoms/repair/order', 'EomsRepairOrder', 'eoms/repair/order', NULL, 'list', b'0', b'0', b'0', 'eoms:repair:order:list', 3, 1, 1, NOW()),
(20012, '维修记录', 20000, 2, '/eoms/repair/record', 'EomsRepairRecord', 'eoms/repair/record', NULL, 'history', b'0', b'0', b'0', 'eoms:repair:record:list', 4, 1, 1, NOW()),
(20013, '维修结算', 20000, 2, '/eoms/repair/settlement', 'EomsRepairSettlement', 'eoms/repair/settlement', NULL, 'stamp', b'0', b'0', b'0', 'eoms:repair:settlement:list', 5, 1, 1, NOW()),

-- 物料
(20020, '物料档案', 20000, 2, '/eoms/material/list', 'EomsMaterialList', 'eoms/material/list', NULL, 'tags', b'0', b'0', b'0', 'eoms:material:list', 6, 1, 1, NOW()),
(20021, '物料分类', 20000, 2, '/eoms/material/category', 'EomsMaterialCategory', 'eoms/material/category', NULL, 'mind-mapping', b'0', b'0', b'0', 'eoms:material:category:list', 7, 1, 1, NOW()),
(20022, '物料库存', 20000, 2, '/eoms/material/stock', 'EomsMaterialStock', 'eoms/material/stock', NULL, 'storage', b'0', b'0', b'0', 'eoms:material:stock', 8, 1, 1, NOW()),
(20023, '采购入库', 20000, 2, '/eoms/material/purchase', 'EomsMaterialPurchase', 'eoms/material/purchase', NULL, 'archive', b'0', b'0', b'0', 'eoms:material:purchase', 9, 1, 1, NOW()),
(20024, '入库单', 20000, 2, '/eoms/material/inbound', 'EomsMaterialInbound', 'eoms/material/inbound/index', NULL, 'import', b'0', b'0', b'0', 'eoms:inbound:list', 10, 1, 1, NOW()),

-- 主数据 / 业务
(20030, '仓库管理', 20000, 2, '/eoms/warehouse', 'EomsWarehouse', 'eoms/warehouse/index', NULL, 'location', b'0', b'0', b'0', 'eoms:warehouse:list', 11, 1, 1, NOW()),
(20031, '供应商', 20000, 2, '/eoms/supplier', 'EomsSupplier', 'eoms/supplier/index', NULL, 'relation', b'0', b'0', b'0', 'eoms:supplier:list', 12, 1, 1, NOW()),
(20032, '合同管理', 20000, 2, '/eoms/contract', 'EomsContract', 'eoms/contract/index', NULL, 'menu-file', b'0', b'0', b'0', 'eoms:contract:list', 13, 1, 1, NOW()),
-- 报价已迁平台 quote 菜单（40000 段），勿再写入 20033

-- 财务
(20040, '应收账款', 20000, 2, '/eoms/finance/receivable', 'EomsFinanceReceivable', 'eoms/finance/receivable', NULL, 'stamp', b'0', b'0', b'0', 'eoms:receivable:list', 15, 1, 1, NOW()),
(20041, '收款登记', 20000, 2, '/eoms/finance/payment', 'EomsFinancePayment', 'eoms/finance/payment', NULL, 'common', b'0', b'0', b'0', 'eoms:payment:list', 16, 1, 1, NOW()),
(20042, '利润分析', 20000, 2, '/eoms/finance/profit', 'EomsFinanceProfit', 'eoms/finance/profit', NULL, 'bar-chart', b'0', b'0', b'0', 'eoms:finance:profit', 17, 1, 1, NOW()),

-- 统计 / 配置 / 看板
(20050, '统计分析', 20000, 2, '/eoms/statistics', 'EomsStatistics', 'eoms/statistics/index', NULL, 'dashboard', b'0', b'0', b'0', 'eoms:statistics:list', 18, 1, 1, NOW()),
(20060, '业务参数', 20000, 2, '/eoms/config/param', 'EomsConfigParam', 'eoms/config/param', NULL, 'settings', b'0', b'0', b'0', 'eoms:config:param', 19, 1, 1, NOW()),
(20061, '附件管理', 20000, 2, '/eoms/config/attachment', 'EomsConfigAttachment', 'eoms/config/attachment', NULL, 'attachment', b'0', b'0', b'0', 'eoms:config:attachment', 20, 1, 1, NOW()),
(20062, '操作日志', 20000, 2, '/eoms/config/log', 'EomsConfigLog', 'eoms/config/log', NULL, 'history', b'0', b'0', b'0', 'eoms:config:log', 21, 1, 1, NOW()),
(20070, '运维看板', 20000, 2, '/eoms/dashboard', 'EomsDashboard', 'eoms/dashboard', NULL, 'dashboard', b'0', b'0', b'0', 'eoms:dashboard:view', 0, 1, 1, NOW());

-- 绑定超管(1)与普通角色(3)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (SELECT `id` FROM `sys_menu` WHERE `id` BETWEEN 20000 AND 20999) m
WHERE r.id IN (1, 3);
