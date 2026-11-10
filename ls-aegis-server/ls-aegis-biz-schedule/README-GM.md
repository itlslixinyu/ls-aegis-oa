# SnailJob 国密对接说明

平台在 `gm.enable=true` 时，`JobClient` 登录调度中心将口令摘要由 **MD5** 改为 **SM3**。

调度中心服务端逻辑不变：校验 `sha256(请求中的 password 字段) == 库中 password`。

## 启用国密后须更新调度库口令

默认明文 `admin`：

| 步骤 | 值 |
|------|-----|
| 线上摘要 SM3(`admin`) | `dc1fd00e3eeeb940ff46f457bf97d66ba7fcc36e0b20802383de142860e76ae6` |
| 库中存储 SHA-256(上述摘要字符串) | `318c70107cb60b3641fc7f720dc0b5ee3a29e06642f7c2799a287e8c42ce84b1` |

在 **snail_job** 库执行（表名以实际版本为准，常见 `sj_system_user`）：

```sql
UPDATE sj_system_user
SET password = '318c70107cb60b3641fc7f720dc0b5ee3a29e06642f7c2799a287e8c42ce84b1'
WHERE username = 'admin';
```

校验：`mvn -pl ls-aegis-biz-schedule -am -Dtest=JobClientGmPasswordTest test`

## 关闭国密

`gm.enable=false` 时仍发送 MD5，可继续使用官方默认 `sha256(md5('admin'))` 种子，无需改库。

## 调度中心 Web 控制台

官方控制台前端仍为 MD5。国密改库后，**控制台账号密码登录会失败**，不影响本平台通过 `JobClient` 调 OpenAPI。若需同时使用官方控制台，请自行将控制台摘要改为 SM3，或临时关闭 `gm.enable`。
