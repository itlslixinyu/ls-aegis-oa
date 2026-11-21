-- liquibase formatted sql

-- changeset ls:weather_qweather_provider
-- comment 天气数据源改为和风；新增 API Key；废弃 Open-Meteo
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `description` = '天气数据来源：本地模拟 / 和风天气（需填写 API Key，开发版有免费额度）'
WHERE `code` = 'WEATHER_PROVIDER';

UPDATE `sys_option`
SET `value` = 'mock', `default_value` = 'mock'
WHERE `code` = 'WEATHER_PROVIDER' AND (`value` = 'open-meteo' OR `default_value` = 'open-meteo');

UPDATE `sys_option`
SET `name` = '和风天气服务地址',
    `description` = '和风实时天气接口根地址，留空默认开发版占位（devapi.qweather.com）'
WHERE `code` = 'WEATHER_API_HOST';

UPDATE `sys_option`
SET `name` = '和风地理编码地址',
    `description` = '和风城市查询接口根地址，留空默认占位（geoapi.qweather.com）'
WHERE `code` = 'WEATHER_GEO_HOST';

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 35, 'WEATHER', '和风 API Key', 'WEATHER_API_KEY', NULL, '', '在和风控制台申请的 Key，仅服务端使用，不下发前端'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_API_KEY');
