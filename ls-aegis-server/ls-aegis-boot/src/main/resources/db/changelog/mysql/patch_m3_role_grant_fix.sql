-- liquibase formatted sql

-- changeset ls:m3_role_grant_without_deleted_menus
-- comment M3 收尾：角色补授 PSI 商品/分类/出入库，不依赖已删菜单 20020～20024（全新 init 无这些 ID）
SET NAMES utf8mb4;

-- 曾持有维修菜单、或已部分拿到 PSI 商品/出库/仓库菜单的角色，补齐合表后应有的进销存菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, m.`id`
FROM `sys_role_menu` rm
CROSS JOIN (
  SELECT 10001 AS `id` UNION ALL SELECT 10002
  UNION ALL SELECT 10020 UNION ALL SELECT 10021 UNION ALL SELECT 10310
  UNION ALL SELECT 10040 UNION ALL SELECT 10041
  UNION ALL SELECT 10050 UNION ALL SELECT 10051
  UNION ALL SELECT 10030 UNION ALL SELECT 10031
) m
WHERE rm.`menu_id` IN (20011, 20013, 10020, 10021, 10050, 10051, 10030, 10031, 10040, 10041, 10310);

-- 种子角色 1/2/3 显式补授（超管旁路不受影响；普通管理员/业务角色可进商品与采购入库）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`id`, m.`id`
FROM `sys_role` r
CROSS JOIN (
  SELECT 10001 AS `id` UNION ALL SELECT 10002
  UNION ALL SELECT 10020 UNION ALL SELECT 10021 UNION ALL SELECT 10310
  UNION ALL SELECT 10040 UNION ALL SELECT 10041
  UNION ALL SELECT 10050 UNION ALL SELECT 10051
  UNION ALL SELECT 10030 UNION ALL SELECT 10031
) m
WHERE r.`id` IN (1, 2, 3);

-- changeset ls:m3_drop_dead_inbound_status_dict
-- comment M3 收尾：清理已无业务的 EOMS 入库状态字典
SET NAMES utf8mb4;

DELETE FROM `sys_dict_item`
WHERE `dict_id` IN (SELECT `id` FROM (SELECT `id` FROM `sys_dict` WHERE `code` = 'eoms_inbound_status') t);

DELETE FROM `sys_dict` WHERE `code` = 'eoms_inbound_status';
