-- liquibase formatted sql

-- changeset ls:weather_open_meteo_options
-- comment 天气：城市自动定位 + 免费 Open-Meteo 数据源配置
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `name` = '默认城市（兜底）',
    `description` = '固定城市模式使用；自动定位失败时也使用该城市'
WHERE `code` = 'WEATHER_CITY';

UPDATE `sys_option`
SET `description` = '天气数据来源：本地模拟 / 免费 Open-Meteo（无需 Key，需能访问天气服务地址）'
WHERE `code` = 'WEATHER_PROVIDER';

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 32, 'WEATHER', '城市模式', 'WEATHER_CITY_MODE', NULL, 'auto', 'auto=按访问 IP 自动定位；fixed=使用默认城市'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_CITY_MODE');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 33, 'WEATHER', '天气服务地址', 'WEATHER_API_HOST', NULL, '', 'Open-Meteo 天气接口根地址，留空则用内置默认占位'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_API_HOST');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 34, 'WEATHER', '地理编码地址', 'WEATHER_GEO_HOST', NULL, '', '城市名转经纬度服务根地址，留空则用内置默认占位'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_GEO_HOST');
