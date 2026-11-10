-- liquibase formatted sql

-- changeset ls:weather_api_key_restore
-- comment 恢复和风 API Key 为正式鉴权方式（无 JWT 私钥时可用）
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `name` = '和风 API Key',
    `description` = '控制台创建「API KEY」类型凭据后的 Key；无 JWT 私钥时填此项即可。仅服务端保存，不下发前端。'
WHERE `code` = 'WEATHER_API_KEY';

UPDATE `sys_option`
SET `description` = '本地模拟仅用于联调；和风天气支持 API Key 或 JWT，须自行申请凭据并遵守协议与配额，异常时回退本地模拟。'
WHERE `code` = 'WEATHER_PROVIDER';

UPDATE `sys_option`
SET `description` = '填写和风控制台 JWT 凭据 ID（kid）；仅用 API Key 时可留空。'
WHERE `code` = 'WEATHER_JWT_KID';

UPDATE `sys_option`
SET `description` = '填写和风控制台项目 ID；仅用 API Key 时可留空。'
WHERE `code` = 'WEATHER_JWT_PROJECT_ID';

UPDATE `sys_option`
SET `description` = 'JWT Ed25519 私钥 PEM；仅用 API Key 时可留空。私钥仅服务端保存。'
WHERE `code` = 'WEATHER_JWT_PRIVATE_KEY';
