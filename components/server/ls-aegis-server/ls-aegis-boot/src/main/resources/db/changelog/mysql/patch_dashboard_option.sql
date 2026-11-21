-- liquibase formatted sql

-- changeset ls:dashboard_option_and_menu
-- comment 系统配置：大屏设置（运营中枢标题与展示开关）
SET NAMES utf8mb4;

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 39, 'DASHBOARD', '大屏名称', 'DASHBOARD_TITLE', NULL, 'LS-Aegis 雷铄御警安全应用运营中枢', '运营中枢顶栏主标题'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_TITLE');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 40, 'DASHBOARD', '显示时钟', 'DASHBOARD_SHOW_CLOCK', NULL, '1', '是否在顶栏右侧显示日期时间'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_SHOW_CLOCK');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 41, 'DASHBOARD', '显示运行状态', 'DASHBOARD_SHOW_STATUS', NULL, '1', '是否在工具栏显示系统运行状态指示灯'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_SHOW_STATUS');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 42, 'DASHBOARD', '显示指标概览', 'DASHBOARD_SHOW_KPI', NULL, '1', '是否显示顶部 KPI 指标卡片'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_SHOW_KPI');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 43, 'DASHBOARD', '显示公告', 'DASHBOARD_SHOW_NOTICE', NULL, '1', '是否显示底部最新公告面板'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_SHOW_NOTICE');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 44, 'DASHBOARD', '全屏入口提示', 'DASHBOARD_SHOW_FS_TIP', NULL, '1', '进入大屏时是否短暂提示右下角全屏入口'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_SHOW_FS_TIP');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 45, 'DASHBOARD', '显示装饰动效', 'DASHBOARD_SHOW_DECOR', NULL, '1', '是否显示边框装饰与扫描线动效'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_SHOW_DECOR');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 46, 'DASHBOARD', '默认统计周期', 'DASHBOARD_DEFAULT_DAYS', NULL, '7', '进入大屏时默认统计周期：7 或 30（天）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_DEFAULT_DAYS');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 47, 'DASHBOARD', '数据刷新间隔（秒）', 'DASHBOARD_REFRESH_INTERVAL', NULL, '60', '仪表盘数据自动刷新间隔，单位秒；0 表示不自动刷新'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'DASHBOARD_REFRESH_INTERVAL');

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1210, '大屏配置', 1150, 2, '/system/config?tab=dashboard', 'SystemDashboardConfig', 'system/config/dashboard/index', NULL, 'dashboard', b'0', b'0', b'1', NULL, 6, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1210);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1211, '查询', 1210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dashboardConfig:get', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1211);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1212, '修改', 1210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dashboardConfig:update', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1212);

UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 1230 AND `sort` = 6;
UPDATE `sys_menu` SET `sort` = 8 WHERE `id` = 1250 AND `sort` = 7;

-- changeset ls:dashboard_option_role_menu
-- comment 将大屏配置权限同步给已有天气配置权限的角色
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1210
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1200
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1210
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1211
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1201
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1211
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1212
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1202
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1212
  );

