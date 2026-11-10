-- liquibase formatted sql

-- changeset ls:weather_qweather_jwt
-- comment 和风天气改为 JWT（Ed25519）鉴权配置
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `description` = '天气数据来源：本地模拟 / 和风天气（JWT 鉴权，开发版有免费额度）'
WHERE `code` = 'WEATHER_PROVIDER';

UPDATE `sys_option`
SET `name` = '和风 API Host',
    `description` = '控制台项目中的 API Host（如 https://xxx.qweatherapi.com），留空则用兼容默认'
WHERE `code` = 'WEATHER_API_HOST';

UPDATE `sys_option`
SET `name` = '和风 GeoAPI 地址',
    `description` = '城市查询根地址，留空默认 geoapi.qweather.com'
WHERE `code` = 'WEATHER_GEO_HOST';

UPDATE `sys_option`
SET `description` = '已废弃：请改用 JWT（凭据ID/项目ID/私钥）。保留字段仅为兼容旧数据'
WHERE `code` = 'WEATHER_API_KEY';

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 36, 'WEATHER', '和风凭据 ID（kid）', 'WEATHER_JWT_KID', NULL, '', '控制台凭据 ID，对应 JWT Header.kid'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_JWT_KID');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 37, 'WEATHER', '和风项目 ID（sub）', 'WEATHER_JWT_PROJECT_ID', NULL, '', '控制台项目 ID，对应 JWT Payload.sub'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_JWT_PROJECT_ID');

INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 38, 'WEATHER', '和风 JWT 私钥', 'WEATHER_JWT_PRIVATE_KEY', NULL, '', 'Ed25519 私钥 PEM（仅服务端保存，勿提交仓库）'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WEATHER_JWT_PRIVATE_KEY');
