-- liquibaseFormattedSql
-- changeset ls:patch_security_audit_hardening
-- comment: 全量审计加固：去弱口令注释效果（强制首登改密）、本地存储去路径穿越、密码默认有效期 90 天

-- 仍使用种子默认口令哈希的账号：置旧改密时间，配合密码有效期触发强制改密（已改密用户不受影响）
UPDATE `sys_user`
SET `pwd_reset_time` = '2000-01-01 00:00:00'
WHERE `username` IN ('admin', 'test')
  AND `password` IN (
    '{sm3}v1$ef4114fbfe4881f4710b626429c97c04$bd43c9a531b4cc2705aaff6d1a2f9563906c254cc3ffe05fbb21c2456bd86d99',
    '{sm3}v1$cfa6968c5df393377b04ff6e33f6f90c$025649846502c230dbfcff724d5724a2aa0af1c2a97defc84da59c70b9df24c8'
  );

-- 密码有效期默认 90 天（value 未自定义时用 default_value；同时校正 default_value）
UPDATE `sys_option`
SET `default_value` = '90',
    `description` = '密码强制修改周期（0-999天，0表示永不过期；默认90天，种子账号首登须改密）',
    `value` = CASE
        WHEN `value` IS NULL OR `value` = '' OR `value` = '0' THEN '90'
        ELSE `value`
    END
WHERE `code` = 'PASSWORD_EXPIRATION_DAYS';

-- 本地存储路径去掉 ../，与 Docker 挂载 /app/data/file 对齐
UPDATE `sys_storage`
SET `bucket_name` = 'data/file/'
WHERE `bucket_name` IN ('../data/file/', '../data/file');
