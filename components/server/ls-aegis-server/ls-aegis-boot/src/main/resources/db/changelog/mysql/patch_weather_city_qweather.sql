-- liquibase formatted sql

-- changeset ls:weather_city_via_qweather
-- comment 天气城市改为和风 GeoAPI 解析（坐标反查/城市搜索），不再依赖 IP 库
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `description` = 'auto=浏览器定位后经和风反查城市；fixed=使用默认城市经和风城市搜索'
WHERE `code` = 'WEATHER_CITY_MODE';

UPDATE `sys_option`
SET `description` = '固定城市模式使用；自动定位失败时也使用该城市（经和风解析 LocationID）'
WHERE `code` = 'WEATHER_CITY';

UPDATE `sys_option`
SET `description` = '留空则优先用和风 API Host 的 /geo/v2/city/lookup；兼容旧版可填 geoapi.qweather.com'
WHERE `code` = 'WEATHER_GEO_HOST';
