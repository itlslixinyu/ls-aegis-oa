-- liquibase formatted sql

-- changeset ls:weather_option_and_menu
-- comment 系统配置：天气设置（运营中枢顶栏）
SET NAMES utf8mb4;

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 28, 'WEATHER', '启用天气展示', 'WEATHER_ENABLED', NULL, '1', '运营中枢顶栏是否显示天气信息'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_ENABLED');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 29, 'WEATHER', '默认城市', 'WEATHER_CITY', NULL, '北京', '运营中枢顶栏天气展示的默认城市'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_CITY');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 30, 'WEATHER', '刷新间隔（秒）', 'WEATHER_REFRESH_INTERVAL', NULL, '600', '天气数据自动刷新间隔，单位秒（建议 60–86400）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_REFRESH_INTERVAL');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 31, 'WEATHER', '数据来源', 'WEATHER_PROVIDER', NULL, 'mock', '天气数据来源（当前仅支持本地模拟，后续可扩展国内天气 API）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_PROVIDER');

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1200, '天气配置', 1150, 2, '/system/config?tab=weather', 'SystemWeatherConfig', 'system/config/weather/index', NULL, 'cloud', b'0', b'0', b'1', NULL, 5, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1200);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1201, '查询', 1200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:weatherConfig:get', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1201);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1202, '修改', 1200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:weatherConfig:update', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1202);
