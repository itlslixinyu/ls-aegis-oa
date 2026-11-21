-- liquibase formatted sql

-- changeset ls:m1_eoms_option_geo_watermark
-- comment M1.1：EOMS 地理/水印参数写入 sys_option
SET NAMES utf8mb4;

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 66, 'EOMS', '百度地图 AK', 'GEO_BAIDU_AK', NULL, NULL, '逆地理编码 Web 服务 AK；未填时现场定位降级为坐标字符串'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'GEO_BAIDU_AK');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 67, 'EOMS', '百度逆地理编码地址', 'GEO_BAIDU_REGEO_URL', NULL, 'https://api.map.baidu.com/reverse_geocoding/v3/', '可换成内网代理域名；须与申请 AK 的服务端类型匹配'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'GEO_BAIDU_REGEO_URL');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 68, 'EOMS', '水印图最长边（像素）', 'WATERMARK_MAX_SIDE', NULL, '1280', '现场照烧录水印前缩放上限，建议 640～4096'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WATERMARK_MAX_SIDE');

-- changeset ls:m1_eoms_config_migrate
-- comment M1.1：已上线库把 eoms_config 自定义值迁入 sys_option 后归档
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_config'
SET NAMES utf8mb4;

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.customer.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'CUSTOMER_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.repair.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'REPAIR_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.order.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'ORDER_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.quote.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'QUOTE_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.contract.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'CONTRACT_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.purchase.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'PURCHASE_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.income.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'INCOME_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.payment.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'PAYMENT_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.receivable.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'RECEIVABLE_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.material.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'MATERIAL_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.category.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'CATEGORY_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.settlement.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'SETTLEMENT_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.warehouse.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'WAREHOUSE_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.supplier.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'SUPPLIER_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.inbound.no.prefix' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = UPPER(TRIM(c.config_value))
WHERE o.code = 'INBOUND_NO_PREFIX' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.geo.baidu.ak' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = TRIM(c.config_value)
WHERE o.code = 'GEO_BAIDU_AK' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.geo.baidu.regeo.url' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = TRIM(c.config_value)
WHERE o.code = 'GEO_BAIDU_REGEO_URL' AND IFNULL(TRIM(c.config_value), '') <> '';

UPDATE `sys_option` o
INNER JOIN `eoms_config` c ON c.config_key = 'eoms.watermark.maxSide' AND IFNULL(c.del_flag, '0') = '0'
SET o.value = TRIM(c.config_value)
WHERE o.code = 'WATERMARK_MAX_SIDE' AND IFNULL(TRIM(c.config_value), '') <> '';

DROP TABLE IF EXISTS `eoms_config_archived`;
RENAME TABLE `eoms_config` TO `eoms_config_archived`;

-- changeset ls:m1_eoms_config_menu
-- comment M1.1：下线运维业务参数菜单，系统配置增加运维报修页签
SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` = 20060;
DELETE FROM `sys_menu` WHERE `id` = 20060;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1260, '运维报修', 1150, 2, CONCAT('/system/config', CHAR(63), 'tab=eoms'), 'SystemEomsConfig', 'system/config/eoms/index', NULL, 'tool', b'0', b'0', b'1', NULL, 10, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1260);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1261, '查询', 1260, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:eomsConfig:get', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1261);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1262, '修改', 1260, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:eomsConfig:update', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1262);

-- changeset ls:m1_eoms_config_role_menu
-- comment M1.1：把运维报修配置权限同步给已有功能模块配置权限的角色
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1260
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1220
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1260);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1261
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1221
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1261);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1262
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1222
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1262);
