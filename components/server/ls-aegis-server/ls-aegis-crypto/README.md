# ls-aegis-crypto（GM/T 软件国密）

独立模块，与业务/鉴权解耦。基于 BouncyCastle 提供 SM2 / SM3 / SM4。**项目整体国密默认开启。**

## 依赖

| 坐标 | 说明 |
|------|------|
| `org.bouncycastle:bcprov-jdk18on:1.78.1` | 算法实现 |
| `org.bouncycastle:bcpkix-jdk18on:1.78.1` | 密钥编解码 |
| `spring-boot-autoconfigure` | 条件装配 |

由 `ls-aegis-starter` 传递引入；业务模块用 `ObjectProvider<IGmCrypto>` 注入。

## 配置样例

```yaml
gm:
  enable: ${GM_ENABLE:true}
  auto-generate: ${GM_AUTO_GENERATE:true}
  key-store-path: ${GM_KEY_STORE_PATH:./data/gm/keys.properties}
  # 存量迁移临时开关（整体国密请保持 false）
  legacy-aes-fallback: ${GM_LEGACY_AES_FALLBACK:false}
  legacy-rsa-fallback: ${GM_LEGACY_RSA_FALLBACK:false}
  sm2:
    public-key: ${GM_SM2_PUBLIC_KEY:}
    private-key: ${GM_SM2_PRIVATE_KEY:}   # 仅服务端，禁止下发前端
  sm4:
    key: ${GM_SM4_KEY:}
    iv: ${GM_SM4_IV:}
```

## 整体国密覆盖面

| 环节 | 算法 |
|------|------|
| 传输口令/密钥 | SM2 |
| 敏感字段存储 | SM4 |
| 新口令哈希 | SM3（校验兼容 `{bcrypt}`） |
| 行为验证码坐标 | SM4/ECB |
| OpenAPI 签名 / clientId / 分片 ETag / 文件指纹 | SM3 |
| SnailJob API 登录摘要 | SM3（调度库须同步，见 `ls-aegis-biz-schedule/README-GM.md`） |

## 业务调用

```java
private final ObjectProvider<IGmCrypto> gmCryptoProvider;

IGmCrypto gm = gmCryptoProvider.getIfAvailable();
if (gm != null && gm.isEnabled()) {
    String digest = gm.sm3Hex(payload);
    String cipher = gm.sm4EncryptToBase64(secret);
}
```

## 信创说明

- 算法为 BouncyCastle **纯 Java**，兼容 x86_64 / ARM64（鲲鹏、飞腾），银河麒麟 / 统信 UOS。
- 达梦 DM8、人大金仓：见 `application-prod.yml` 与 `ls-aegis-common/pom.xml` 注释。

## 单元测试

```bash
mvn -pl ls-aegis-crypto -am test
```
