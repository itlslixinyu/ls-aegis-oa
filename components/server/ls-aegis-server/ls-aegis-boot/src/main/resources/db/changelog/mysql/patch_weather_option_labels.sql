-- liquibase formatted sql

-- changeset ls:weather_option_labels_sort
-- comment 天气配置项名称与合规说明文案对齐
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `name` = '是否启用',
    `description` = '控制运营中枢顶栏是否展示天气信息。'
WHERE `code` = 'WEATHER_ENABLED';

UPDATE `sys_option`
SET `name` = '默认城市',
    `description` = '固定城市模式下使用；定位未授权或解析失败时作为展示城市。'
WHERE `code` = 'WEATHER_CITY';

UPDATE `sys_option`
SET `name` = '刷新时间间隔（秒）',
    `description` = '天气信息自动刷新周期，单位秒，建议 60～86400。'
WHERE `code` = 'WEATHER_REFRESH_INTERVAL';

UPDATE `sys_option`
SET `description` = '本地模拟仅用于联调展示；和风天气为第三方服务，须自行申请有效凭据并遵守其服务协议与调用配额，异常时回退本地模拟。'
WHERE `code` = 'WEATHER_PROVIDER';

UPDATE `sys_option`
SET `description` = '「和风 GeoAPI 定位」在用户授权后使用浏览器位置解析城市；「固定城市」仅使用默认城市。位置信息仅用于天气展示。'
WHERE `code` = 'WEATHER_CITY_MODE';

UPDATE `sys_option`
SET `name` = '和风 GeoAPI 地址',
    `description` = '填写服务商控制台提供的城市查询地址，须与当前账号权限及官方文档一致。'
WHERE `code` = 'WEATHER_GEO_HOST';

UPDATE `sys_option`
SET `description` = '填写和风控制台中的项目 ID，须与当前凭据一致。'
WHERE `code` = 'WEATHER_JWT_PROJECT_ID';

UPDATE `sys_option`
SET `description` = '填写和风控制台中的 JWT 凭据 ID，须与已上传公钥对应。'
WHERE `code` = 'WEATHER_JWT_KID';

UPDATE `sys_option`
SET `description` = '填写控制台「设置」中的专属 API 访问地址，请以控制台实际值为准。'
WHERE `code` = 'WEATHER_API_HOST';

UPDATE `sys_option`
SET `description` = '私钥仅服务端保存，请勿外传或写入代码仓库。脱敏显示时未修改直接保存不会覆盖原私钥。'
WHERE `code` = 'WEATHER_JWT_PRIVATE_KEY';
