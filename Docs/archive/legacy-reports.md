# LS・Aegis 项目报告合集（Reports-README）

> 本文件为仓库**唯一**正式报告/平台说明落盘文件（原 `reports/` 分册 + 根目录 `docs/*.md` 已全部并入；目录现名 `Docs/`）。  
> 探针、jar、log 勿放入 `Docs/`。新增结论请直接追加到本文对应章节。  
> 部署 SQL 仍只认 `ls-aegis-server/sql/init.sql`，不在本文维护。

> 日期任务目录为本地工作资料，不随当前 Git 分支分发；正式说明以本文件为准，旧版本资料仍可在 Git 历史中追溯。

## 目录

1. [安全审计与外连扫描](#1-安全审计与外连扫描)
2. [国密功能测试](#2-国密功能测试)
3. [平台一体收敛](#3-平台一体收敛)
4. [租户 · 应用 · 任务调度使用说明](#4-租户--应用--任务调度使用说明)
5. [进销存业务说明](#5-进销存业务说明)
6. [全项目冒烟测试](#6-全项目冒烟测试)
7. [平台说明（INTEGRATION）](#7-平台说明integration)
8. [平台一体 · 开发分步](#8-平台一体--开发分步)
9. [平台一体 · 整改清单](#9-平台一体--整改清单)
10. [平台一体 · 评审门禁](#10-平台一体--评审门禁)
11. [已清理的历史分册](#11-已清理的历史分册)
12. [电子签章 · 发起签章页后续扩展方案](#12-电子签章--发起签章页后续扩展方案)
13. [RBAC 权限管理检查与修复方案](#13-rbac-权限管理检查与修复方案)
14. [本地开发（中间件 Docker + 本机后端）](#14-本地开发中间件-docker--本机后端)
15. [前端雪花 ID 精度与 Select 回显](#15-前端雪花-id-精度与-select-回显)
16. [雪花 ID 精度影响全量审计](#16-雪花-id-精度影响全量审计)
17. [后端启动警告治理](#17-后端启动警告治理)
18. [项目 Agent、渐进优化与业务测试基线](#18-项目-agent渐进优化与业务测试基线)
19. [类型归零、完整构建与业务闭环实测](#19-类型归零完整构建与业务闭环实测)
20. [平台整体性能优化第一轮](#20-平台整体性能优化第一轮)
21. [代码检查 Agent 与第二轮优化验收](#21-代码检查-agent-与第二轮优化验收)
22. [在用环境分阶段测试首批执行](#22-在用环境分阶段测试首批执行)
23. [全模块覆盖盘点与在用续测结果](#23-全模块覆盖盘点与在用续测结果)
24. [下载与数量修复及续测复核](#24-下载与数量修复及续测复核)
25. [浏览器来源权限专项诊断](#25-浏览器来源权限专项诊断)
26. [R27 正常登录接口实测与修复部署](#26-r27-正常登录接口实测与修复部署)
27. [R28 在用环境业务逻辑缺陷实测](#27-r28-在用环境业务逻辑缺陷实测)
28. [R29 修复交付与 R30 联合回归](#28-r29-修复交付与-r30-联合回归)
29. [R31 页面复测与容量优化](#29-r31-页面复测与容量优化)
30. [R34 照片定位与维修联动补测](#30-r34-照片定位与维修联动补测)
31. [R36 真实设备与外部地图验证](#31-r36-真实设备与外部地图验证)
32. [逐功能业务流程实测与清理](#32-逐功能业务流程实测与清理)
33. [R39 页面实测与 R40 完整业务联动回归](#33-r39-页面实测与-r40-完整业务联动回归)
34. [PDF 上传验真与完整签署接口补测](#34-pdf-上传验真与完整签署接口补测)
35. [R43 功能缺口补测与业务清理闭环](#35-r43-功能缺口补测与业务清理闭环)
36. [移除代码生成与应用管理](#36-移除代码生成与应用管理)
37. [菜单缓存残留与实际运行环境复核](#37-菜单缓存残留与实际运行环境复核)
38. [业务数据全量清理范围盘点](#38-业务数据全量清理范围盘点)
39. [V2.0 代码发布与版本统一](#39-v20-代码发布与版本统一)
40. [Git 失效资料清理与本地保留](#40-git-失效资料清理与本地保留)
41. [本地冗余工具代码清理与回归](#41-本地冗余工具代码清理与回归)
42. [V1 旧产品版本标记核对与运行版本同步](#42-v1-旧产品版本标记核对与运行版本同步)
43. [Git 云端 V1 标签与本地旧构建包清理](#43-git-云端-v1-标签与本地旧构建包清理)
44. [截图旧提交路径的 V2 工程适配](#44-截图旧提交路径的-v2-工程适配)
45. [项目规范目录迁移为 rules](#45-项目规范目录迁移为-rules)
46. [Skills 根目录迁移与历史共同作者盘点](#46-skills-根目录迁移与历史共同作者盘点)
47. [授权清除 Cursor 历史共同作者署名](#47-授权清除-cursor-历史共同作者署名)
48. [GitHub 主页侧栏与贡献 API 差异复核](#48-github-主页侧栏与贡献-api-差异复核)
49. [本轮架构精简、业务回归与性能审查](#49-本轮架构精简业务回归与性能审查)
50. [四项顺序优化与联动回归](#50-四项顺序优化与联动回归)
51. [全仓结构与冗余审查及受控精简](#51-全仓结构与冗余审查及受控精简)
52. [工作台聚合、商品导入与消息计数优化](#52-工作台聚合商品导入与消息计数优化)
53. [报价拆分、预览会话与公共加载优化](#53-报价拆分预览会话与公共加载优化)
54. [conservative清理规范与阶段1基线](#54-conservative清理规范与阶段1基线)
55. [JDK21与pnpm9适配、服务恢复及阶段2静态清单](#55-jdk21与pnpm9适配服务恢复及阶段2静态清单)
56. [四项import受控清理、单测及候选启动回归](#56-四项import受控清理单测及候选启动回归)
57. [110项证据不足记录的深入审查与回滚门禁](#57-110项证据不足记录的深入审查与回滚门禁)
58. [原Docker数据目录恢复与单行import清理验收](#58-原docker数据目录恢复与单行import清理验收)
59. [前端局部未用绑定清理与双端构建回归](#59-前端局部未用绑定清理双端构建回归)
60. [保守清理收尾、清单闭环与本地提交](#60-保守清理收尾清单闭环与本地提交)
61. [业务工作流Agent与逐功能真实测试首轮](#61-业务工作流agent与逐功能真实测试首轮)
62. [V2.2.0版本同步与Git发布](#62-v220版本同步与git发布)
63. [EOMS 原生 Android 双端工作流、Agent 与 Skill](#63-eoms-原生-android-双端工作流agent-与-skill)
64. [网站配置页布局优化](#64-网站配置页布局优化)
78. [本机统一Compose重建与数据保留验收](#78-本机统一compose重建与数据保留验收)
79. [统一部署复查与Android双端模拟器启动](#79-统一部署复查与android双端模拟器启动)
80. [ls-aegis-oa目录文档整理工作流与发布准备规范](#80-ls-aegis-oa目录文档整理工作流与发布准备规范)
81. [目录文档整理阶段0基线与阶段1完整盘点](#81-目录文档整理阶段0基线与阶段1完整盘点)

---

## 1. 安全审计与外连扫描

| 项 | 内容 |
|---|---|
| 版本 | v1.1.0 → v1.1.1 审计修复后工作区 |
| 审计日期 | 2026-08-13 |
| 修复落地 | 2026-08-13（配置/种子/前端已改；Docker 已按 `.env` 重建部署） |
| 范围 | `ls-aegis-web`、`ls-aegis-server`、`docker`、`sql`；外连扫描含前端构建产物（`docker/nginx/web`） |
| 方法 | 静态全库检索（外连 / 命令执行 / 后门 / 硬编码密钥 / 遥测）+ 关键路径人工核对 + 逐项修复；外连专项为只读扫描 |
| 总结论 | **未发现后门、反弹 Shell、隐藏执行接口、静默遥测/偷传。** 主要风险为硬编码密钥与弱默认口令、可选业务外连；所列高/中/低风险项均已 **FIXED**、**MITIGATED** 或明确 **ACCEPTED**。 |
| 合并说明 | 由原「安全审计与修复报告」「全量安全审计报告」「外连数据发送扫描报告」合并为一份，避免互为补充的分散落盘。启动模块现名 `ls-aegis-boot`（历史曾用 `ls-demo`）。 |

---

### 1. 执行摘要

| 类别 | 结论 |
|---|---|
| 静默上报 / 偷偷上传本机密钥与业务数据 | **未发现** |
| 陌生第三方运行时外连（非业务） | **未发现**（备案外链需用户点击；alicdn 死 SVG 已删除） |
| 命令执行 / 动态代码执行后门 | **未发现** |
| 硬编码运行密钥（RSA/AES/JWT 字面量） | **环境变量注入**（历史泄露仍须轮换） |
| WebSocket 长连接 | 仅 `/websocket`，握手校验 Sa-Token |
| 默认全新部署是否外发业务数据 | **否**（JustAuth 关、无短信、MAIL_HOST 空、本地存储、SnailJob 关） |
| 审计残留项 | **已全部处理** |

#### 风险统计

| 级别 | 数量 | 状态 |
|---|---|---|
| CRITICAL | 2 | 全部 FIXED |
| HIGH | 5+（含种子口令注释等） | 全部 FIXED |
| MEDIUM | 若干 | FIXED / MITIGATED / ACCEPTED |
| LOW / INFO | 若干 | FIXED / ACCEPTED |
| 干净检查项 | 8+ | CLEAN（维持） |
| 静默非业务偷传（外连专项） | 0 | — |

---

### 2. 干净项（未发现）

| 检查项 | 结果 |
|---|---|
| `Runtime.exec` / `ProcessBuilder` / `ScriptEngine` / `GroovyShell` / `child_process` | 未发现 |
| `eval` / `new Function` / `defineClass` / `URLClassLoader` | 未发现 |
| 反弹 Shell（`/dev/tcp`、`nc -e`、`powershell -enc` 等） | 未发现 |
| 隐藏接口（`/shell`、`/cmd`、`/backdoor`、`/god` 等） | 未发现 |
| 遥测 SDK（Sentry / GA / 百度统计 / Mixpanel / `sendBeacon`） | 未发现 |
| `index.html` 第三方 script / 跟踪像素 | 未发现 |
| 收集主机名/MAC/OS 后外传 | 未发现（仅本地审计日志；IP 归属为离线库） |
| Docker 构建 `curl`/`wget` 远程脚本 | 未发现 |
| npm `postinstall` 恶意联网脚本 | 未发现 |
| Base64 解码后执行 / C2 混淆串 | 未发现 |
| 硬编码公网业务 IP | 未发现 |

#### 可疑特征专项

| 特征 | 结论 |
|---|---|
| 长 Base64 | 验证码 UI、国密密文；非可执行载荷 |
| 动态拼接 URL | JobClient / 业务下载 / 菜单外链；已加协议校验 |
| 不明工具类 | 未发现；新增 `safeUrl.ts` 为防护工具 |
| 死代码外连残留 | alicdn SVG **已删除** |

---

### 3. 风险与修复明细

#### 3.1 严重 / 高危（密钥与默认口令）

| 编号 | 级别 | 状态 | 位置 | 修复前 | 修复后 |
|---|---|---|---|---|---|
| S1 | CRITICAL | FIXED | `ls-aegis-boot/.../application-prod.yml`、`application-dev.yml`（`continew-starter.encrypt.field`） | RSA `private-key` / `public-key` 明文进仓库 | `${ENCRYPT_RSA_PUBLIC_KEY:}` / `${ENCRYPT_RSA_PRIVATE_KEY:}`，仓库不再含密钥 |
| S2 | CRITICAL | FIXED | 同上 `password` | `AES password: abcdefghijklmnop` | `${ENCRYPT_AES_PASSWORD:}`；本地由 `docker/.env` 注入（已 gitignore） |
| S3 | HIGH | FIXED | `application.yml` JWT 配置 | `jwt-secret-key: asdasdas…` 弱默认 | `jwt-secret-key: ${JWT_SECRET_KEY}`，无弱默认；Compose 必填注入 |
| S4 | HIGH | FIXED | `docker/docker-compose.yml`、`docker/redis/conf/redis.conf` | MySQL/Redis/`DB_PWD` 写死 `123456` | `${MYSQL_ROOT_PASSWORD}` / `${REDIS_PWD}` / `${DB_PWD}`；Redis `--requirepass` 命令行注入；提供 `docker/.env.example` |
| S5 | HIGH | FIXED | `ls-aegis-web/.../login/components/account/index.vue` | 预填 `admin` / `admin123` | `username` / `password` 默认空 |
| S6 | HIGH | FIXED | `application-prod.yml` `snail-job` | 默认 `enabled:true` + 固定 token/`admin` | 默认 `enabled:false`；token/用户/口令仅环境变量 |
| N1 | HIGH | FIXED | JustAuth + 登录页 / 个人中心 Social | `justauth.enabled=true` + 真实 `client-id` | `JUSTAUTH_ENABLED` 默认 `false`；凭证全环境变量；登录去掉 OAuth 入口；个人中心提示已关闭 |
| H1 | HIGH | FIXED | `sql/init.sql` / mysql+pg `main_data.sql` 种子用户段 | 注释写明 `admin/admin123`、`test/test123` | 删除明文口令注释；`pwd_reset_time` 置 `2000-01-01`；`PASSWORD_EXPIRATION_DAYS` 默认 **90**，种子账号首登强制改密；已上线库见 `patch_security_audit_hardening.sql` |

#### 3.2 中危

| 编号 | 级别 | 状态 | 位置 | 修复前 / 原风险 | 修复后 |
|---|---|---|---|---|---|
| N3 | MEDIUM | FIXED | `main_data.sql`、`init.sql`、`patch_security_clear_mail_host.sql` | 种子 `MAIL_HOST=smtp.126.com` | 种子 `NULL` + Liquibase patch 清空存量 |
| A1 | MEDIUM | MITIGATED | `sa-token.extension.security.excludes` | 整段 `/file/**` 匿名放行 | 仅放行 `/file/user/avatar/**`；其它本地文件须鉴权 |
| A2 | MEDIUM | FIXED | JetCache remote | `valueEncoder/Decoder: java` | 改为 `kryo5` |
| A3 | MEDIUM | FIXED | `application-dev.yml` CORS | `allowed-origins: '*'` | 仅本机 `5173`/`8080` 与 `application.url` |
| N4 | MEDIUM | FIXED | JustAuth `type.*.client-id` | Gitee/GitHub 等 ID 明文 | 全部改为 `${JUSTAUTH_*}` 空默认 |
| M1 | MEDIUM | FIXED | `LsAegisApplication.java` `GET /` | 匿名返回完整 `applicationProperties` | 仅返回 `name`、`version` |
| M2 | MEDIUM | ACCEPTED | `security.excludes` `/file/user/avatar/**` | 头像匿名可读 | 产品需要公开头像；上传须登录且路径固定；配合路径穿越防护 |
| M3 | MEDIUM | FIXED | `FileServiceImpl.pretreatmentPath` | 未剥离 `../` | 规范化后拒绝含 `..` 的路径 |
| M4 | MEDIUM | FIXED | `StorageServiceImpl` / 本地 `bucket` | `../data/file/` + ResourceHandler | 种子改为 `data/file/`；创建/加载时拒绝 `..` |
| M5 | MEDIUM | FIXED | `AuthController` `GET /auth/{source}` | 社交授权匿名入口 | `justauth.enabled=false` 时直接抛「社交登录未启用」 |
| M6 | MEDIUM | FIXED | `docker-compose.yml` MySQL/Redis 端口 | 暴露到宿主机所有接口 | 映射为 `127.0.0.1:3306` / `127.0.0.1:6379` |
| M7 | MEDIUM | MITIGATED | 菜单 `window.open` 外链 | 任意 http(s) 打开 | `openSafeExternal`：仅 http(s)+`noopener,noreferrer` |
| M8 | MEDIUM | MITIGATED | `downloadFile.ts` 绝对 URL | 任意协议/地址 fetch | `assertSafeDownloadUrl` 仅允许 http(s) |

#### 3.3 低危 / 信息

| 编号 | 级别 | 状态 | 位置 | 说明 |
|---|---|---|---|---|
| N5 | LOW | ACCEPTED | `login/index.vue` 备案链接 | `beian.miit.gov.cn` / `beian.gov.cn` 仅用户点击，无自动上报；合规展示保留 |
| N6 | LOW | FIXED | `ls-aegis-web/package.json` | 移除未使用的 `@amap/amap-jsapi-loader`（避免未来高德 CDN） |
| L1 | LOW | FIXED | `file-open.svg` 内嵌 alicdn | 死资源外连残留，已删除未引用文件 |
| L2 | LOW | FIXED | SnailJob API URL | 改为 `${SNAIL_JOB_URL:...}`，compose / `.env.example` 已透出 |
| I1 | INFO | FIXED | 仓库根 `captcha-tmp.json` | 验证码响应落盘残留，已删除 |
| — | — | FIXED | `justauth.cache.type` | 与 Redisson `stringRedisTemplate` 冲突导致启动失败，已改为 `DEFAULT`（社交登录仍关闭） |
| — | INFO | ACCEPTED | 登录 / GM 公钥 / 验证码 / 站点配置 `@SaIgnore` | 登录与公开配置必需 |
| — | INFO | ACCEPTED | WebSocket excludes + 握手验 token | 非裸奔 |
| — | INFO | ACCEPTED | npmmirror / OpenAPI 示例 URL | 构建期或文档，非运行时回传 |

#### 3.4 已加固基线（维持）

| 项 | 状态 |
|---|---|
| JWT / AES / RSA 环境变量化 | 维持 |
| JustAuth 默认关闭 | 维持，且授权入口二次校验 |
| Docker 口令 `.env` | 维持 |
| 邮件种子 SMTP 清空 | 维持 |

> 历史曾入库的 RSA/AES/JWT 弱值即使已删除，仍应视为**已暴露并轮换**。若存量字段曾用旧 AES 加密，迁移前谨慎开启 `gm.legacy-aes-fallback`。

---

### 4. 外连数据发送扫描

只读扫描；过滤本项目自身业务栈互访（同源 `/api`、Vite/Nginx 反代、Compose 内 `mysql`/`redis`/`server`、默认本机 MySQL/Redis/SnailJob、入站 WebSocket）。

#### 4.1 运行时可能触达的域名 / 地址

| 目标 | 触发条件 | 分类 | 来源示例 |
|---|---|---|---|
| `beian.miit.gov.cn` / `beian.gov.cn` | 登录页用户点击 | 业务合规（主动） | `login/index.vue` |
| `gitee.com` / `github.com` / `open.weixin.qq.com` | `JUSTAUTH_ENABLED=true` 且走社交登录 | 可选业务 | JustAuth（默认 **关闭**） |
| ~~短信厂商 API~~ | ~~已移除短信模块~~ | — | — |
| SMTP 主机 | 启用邮件验证码 | 可选业务 | `MAIL_HOST` 种子为 **NULL** |
| S3/OSS endpoint | 存储类型非本地 | 可选业务 | `S3ClientFactory` |
| SnailJob API | `SNAIL_JOB_ENABLED=true` | 本机/内网调度 | 默认关闭、默认本机 |
| `127.0.0.1` / Compose 内网 | 常态 | 本地栈 | MySQL/Redis 宿主机端口仅绑定 `127.0.0.1` |
| `registry.npmmirror.com` | `pnpm bootstrap` | 供应链（构建期） | `package.json` |
| Docker Hub / Maven Central | `docker` / `mvn` | 供应链（部署/构建期） | compose / pom |

#### 4.2 WebSocket

| 端点 | 鉴权 | 用途 | 判断 |
|---|---|---|---|
| `/websocket?token=` | 握手时 `StpUtil.getLoginIdByToken`，无效拒绝 | 通知未读数推送 | **业务需要** |

#### 4.3 正常业务外连明细（配置驱动）

##### 后端

| # | 位置 | 发送什么数据 | 目标 | 触发条件 |
|---|---|---|---|---|
| B1 | `SocialLoginHandler` | OAuth `code`/`state`；换 token 并拉第三方用户资料 | 微信 / Gitee / GitHub（JustAuth） | `JUSTAUTH_ENABLED=true`；默认 **false** |
| B2 | `UserProfileController` | 同上（绑定三方账号） | 同上 | 已登录且 IdP 已配置 |
| B3 | `CaptchaController` | HTML 邮件 + **邮箱验证码** | `sys_option.MAIL_HOST` | 管理员配置 SMTP 后 |
| B4 | ~~`CaptchaController` 短信~~ | ~~已移除~~ | — | 短信模块与相关 API 已删除 |
| B5–B7 | `S3ClientFactory` / `S3StorageHandler` / `FileServiceImpl` | S3 客户端与**文件字节** | `sys_storage.endpoint` | 启用 OSS 存储；默认本地则不外连 |

##### 前端

| # | 位置 | 发送什么数据 | 目标 | 触发条件 |
|---|---|---|---|---|
| F1–F2 | `login/index.vue` | 浏览器导航（无业务 POST） | ICP / 公安备案站 | 用户点击 |
| F3 | `safeUrl.ts` ← 菜单 | 打开外链（无应用体 POST） | 菜单配置的 http(s) | 管理员配置外链菜单且用户点击 |
| F4–F5 | `downloadFile.ts` / 预览组件等 | GET 拉取文件/资源 | 若 URL 为公网 OSS/CDN | 资源配置为外域绝对地址时 |

#### 4.4 可疑 / 依赖库默认行为

| # | 位置 | 发送什么 | 目标 | 说明 |
|---|---|---|---|---|
| S1 | `@vue-office/pdf`（构建产物） | GET 拉取 PDF.js 静态资源 | 默认 `https://unpkg.com/pdfjs-dist@3.1.81/` | **预览 PDF 时可能打海外 CDN**；非项目主动埋点 |
| S2–S3 | `aieditor` 内置 | 若启用：POST 聊天内容 + token | 百度文心 / OpenAI 默认端点 | 业务侧 **未配置 `ai`**，**默认不发起**；属打包能力残留 |

#### 4.5 已排除（自身业务栈）

| 项 | 排除原因 |
|---|---|
| 前端 axios `/api` | 同源或反代本机后端 |
| WebSocket | 默认本机/同源；自建推送 |
| SnailJob HTTP | 默认本机且组件关闭 |
| MySQL / Redis | 本项目中间件 |
| Docker 拉镜像 / pnpm 安装 | 构建/部署期，非运行时业务数据外传 |
| 入站接口、离线 IP 归属、注释/Swagger 示例 | 不主动外发或非执行路径 |

#### 4.6 默认部署外连结论

| 场景 | 是否向外网发业务数据 |
|---|---|
| 默认配置启动 | **否**（运行时业务路径） |
| 用户点击备案链接 | 仅浏览器打开政府备案站 |
| 用户预览 PDF（未改 vue-office 默认 CDN） | **可能**请求 `unpkg.com`（S1） |
| 管理员启用社交/SMTP/OSS | **是**，目标由配置决定 |

#### 4.7 外连统计

| 类别 | 数量 |
|---|---|
| 正常业务外连（后端+前端） | 12（含条件 OSS 资源加载） |
| 可疑/依赖默认外连 | 3（PDF CDN 1 + AI 客户端 2；AI 默认不触发） |
| 静默非业务偷传 | **0** |

---

### 5. 关键配置变更清单

| 文件 / 变更 | 要点 |
|---|---|
| `application.yml` | JWT → `${JWT_SECRET_KEY}` |
| `application-prod.yml` / `-dev.yml` | AES/RSA 环境变量；JustAuth 默认关；SnailJob 默认关；JetCache `kryo5`；`/file/user/avatar/**`；dev CORS 白名单；`SNAIL_JOB_URL` |
| `LsAegisApplication.java` | 根路径仅返回 name/version |
| `FileServiceImpl` / `StorageServiceImpl` / `StorageTypeEnum` | 路径穿越防护；本地 bucket `data/file/` |
| `AuthController` | 社交登录门禁 |
| `docker-compose.yml` / `redis.conf` | 口令/JWT 全环境变量；端口仅 `127.0.0.1` |
| `docker/.env.example` + `.gitignore` | 密钥模板；`docker/.env` 不入库 |
| `ls-aegis-web` 登录 / Social / `package.json` | 去预填口令、去 OAuth；移除高德依赖 |
| `safeUrl.ts` / `downloadFile.ts` / 菜单 | 外链与下载协议校验 |
| `router/guard.ts` | 过期改密路由 `next` 后 `return` |
| `main_data` + `init.sql` + `patch_security_*.sql` | 清空 MAIL_HOST；种子口令注释清理；强制改密补丁 |
| 删除 `file-open.svg` / `captcha-tmp.json` | 外连残留与探针清理 |

---

### 6. 运维与部署注意

1. **密钥**：复制 `docker/.env.example` → `docker/.env`，填写强随机值；**禁止提交** `docker/.env`。
2. **已有 MySQL 数据卷**：首次初始化后的 root 口令不会随环境变量自动变更；改密需 `ALTER USER` 或 `docker compose down -v` 重建（会清空库）。
3. **已有库安全补丁**：重启后 Liquibase 执行 `patch_security_audit_hardening.sql`；或手工对齐 `init.sql` 效果。
4. **本地存储目录**：由 `../data/file/` 改为 `data/file/`，确认进程工作目录下存在该路径（Docker 已挂载 `/app/data/file/`）。
5. **种子口令**：使用默认种子口令登录后会进入「密码已过期」页，须先改密；仓库不再写明文。
6. **MySQL/Redis**：仅本机可达；远程运维请走 SSH 隧道（勿对公网放开）。
7. **改配置后（部署态）**：`mvn -pl ls-aegis-boot -am package -DskipTests` → jar 放入 `docker/ls-aegis-server/bin/` → `docker compose up -d --build`。日常开发请用 [§14](#14-本地开发中间件-docker--本机后端)（只起 MySQL/Redis，本机跑后端），勿每次重建镜像。
8. **生产**：`JWT_SECRET_KEY`、`DB_PWD`、`REDIS_PWD`、国密密钥等必须由密钥管理/环境注入，禁止回退弱默认。
9. 后续若重新开启 JustAuth / SnailJob / 外网 SMTP/OSS，须单独做外连与密钥合规评审。短信模块已移除，勿再按短信厂商外连评估。

---

### 7. 验证记录（本地 Docker，2026-08-13）

| 检查 | 结果 |
|---|---|
| Maven 打包 / 前端构建 / server 镜像 | 成功 |
| MySQL / Redis | healthy（口令与 `.env` 对齐） |
| `GET /auth/gm/public-key` | `enable=true`，`algorithm=SM2` |
| 前端 `http://localhost:8080/` | 已同步新构建 |

---

### 8. 说明与局限

1. 基于源码静态审计与配置核对，**未做**全量依赖 SCA、渗透测试与生产流量抓包。
2. `docker/nginx/web/static` 哈希包未逐文件逆向；前端风险以 **源码** 与构建入口为准（部署需重新构建同步 dist）。
3. 开放 API 签名（Sa-Token sign）属产品能力，需保护 `sys_app` 密钥。
4. 种子哈希仍对应历史默认口令：依赖 **强制改密** 与部署后口令轮换。
5. 正式落盘：本文件 §1。

---

## 2. 国密功能测试

- **版本**：v1.1.0  
- **生成时间**：2026-08-13 09:00（+08:00）  
- **来源**：Maven / Node sm-crypto / 静态配置 / 本地联调探针  
- **可复跑自检**：`ls-aegis-server/ls-aegis-crypto/src/test/java/com/ls/aegis/crypto/GmIntegrationReportTest.java`

### 结论

Docker server 已重建部署。公钥接口与 admin SM2 登录联调通过；国密算法 / 配置 / 前端互通用例全部通过（**20/20**）。

| 指标 | 数值 |
|---|---|
| 用例总数 | 20 |
| 通过 | 20 |
| 失败 | 0 |
| 跳过 | 0 |
| 通过率 | 100% |

### 能力覆盖

| 能力 | 验证状态 | 备注 |
|---|---|---|
| 传输 SM2 | 联调通过 | 公钥下发 + 登录密文解密成功 |
| 字段 SM4 | 单元往返通过 | 存量 AES 需 legacy 开关迁移 |
| 口令 SM3 | 种子匹配 + 登录通过 | admin123 / test123 |
| 验证码 SM4/ECB | 联调通过 | 图形验证码 + SM4 坐标校验 |
| 分片指纹 SM3 | 代码与 Worker 就绪 | 未跑端到端上传 |
| SnailJob SM3 | 摘要向量通过 | 需调度库改口令后联调 |
| OpenAPI 签名 SM3 | 代码路径已切换 | 未跑外部客户端联调 |

### 用例明细

| 编号 | 用例 | 类别 | 结果 | 说明 |
|---|---|---|---|---|
| A1 | mvn ls-crypto -am test | Maven | 通过 | 13 tests · 0 fail · BUILD SUCCESS（含 GmIntegrationReportTest） |
| A2 | JobClientGmPasswordTest | Maven | 通过 | SnailJob SM3 口令摘要向量通过 |
| B1 | SM3(admin) + SHA-256 | 算法 | 通过 | 调度中心库口令向量对齐 README-GM |
| B2 | 种子口令 admin123 → SM3 | 算法 | 通过 | Sm3PasswordEncoder.matches 与 init.sql 一致 |
| B3 | 种子口令 test123 → SM3 | 算法 | 通过 | Sm3PasswordEncoder.matches 与 init.sql 一致 |
| B4 | 行为验证码 SM4/ECB | 算法 | 通过 | Sm4EcbUtils 固定向量 + 加解密往返 |
| B5 | SoftwareGmCrypto 全套 | 算法 | 通过 | SM2 加解密 / 签验 · SM4-CBC 往返 · GM_REPORT_OK |
| C1 | 前端 sm-crypto SM3(abc) | 前端 | 通过 | 标准向量 66c7f0f4…ba8e0 |
| C2 | 前端 SM4/ECB ↔ Java | 前端 | 通过 | 与 Sm4EcbUtils Base64 完全一致 |
| D1 | gm.enable 默认 true | 配置 | 通过 | application.yml `${GM_ENABLE:true}` |
| D2 | legacy-aes-fallback=false | 配置 | 通过 | 整体国密默认不回退 AES |
| D3 | legacy-rsa-fallback=false | 配置 | 通过 | 整体国密默认不回退 RSA |
| D4 | 自动装配注册 | 配置 | 通过 | GmCrypto + GmPasswordEncoder AutoConfiguration |
| D5 | 种子用户 {sm3} | 配置 | 通过 | init.sql / main_data 无 {bcrypt} 种子口令 |
| D6 | 前端 encrypt API | 配置 | 通过 | encryptTransport / encryptByCaptcha / encryptBySm4Ecb |
| D7 | 验证码 AESUtil 覆盖 | 配置 | 通过 | ls-aegis-boot …/anji/captcha/util/AESUtil.java |
| D8 | JobClient 摘要 SM3 | 配置 | 通过 | gm 开启时 digestPassword → sm3Hex |
| E1 | GET /auth/gm/public-key | 联调 | 通过 | enable=true · algorithm=SM2 · publicKeyHex 已下发（Docker server 已重建） |
| E2 | 前端静态站点 :8080 | 联调 | 通过 | Nginx 返回 index HTML |
| E3 | admin SM2 登录 | 联调 | 通过 | SM2(C1C3C2)+图形验证码 → code=0 拿到 JWT；库口令已为 {sm3} |

### 部署复测结果

1. `mvn -pl ls-aegis-boot -am package -DskipTests` SUCCESS（短路径 `D:\LSAEGI~2`；历史 artifact 曾为 `com.ls:ls-demo`）
2. `docker compose build/up server` → `ls-aegis-server-1` Up（:18000）
3. `GET /auth/gm/public-key` → enable=true / SM2 / publicKeyHex
4. admin/admin123：SM2 加密口令 + 图形验证码 → JWT（code=0）

> 注意：发给后端的 SM2 密文需带 `04` 前缀（与 BC SM2Engine 对齐）；sm-crypto 默认输出不含此前缀。

---

## 3. 平台一体收敛

对照：本文 [§8](#8-平台一体--开发分步)、[§7](#7-平台说明integration)、`.cursor/rules/平台一体.mdc`。  
合表/抽公共件（D/C/M/R）**已全部结束**；范围说明见本文 [§9](#9-平台一体--整改清单)。

| 项 | 内容 |
|---|---|
| 主线日期 | 2026-08-15 ～ 2026-08-18 |
| 结论 | P0～P6 **已完成**；部署只认 `init.sql` + Liquibase；报价/eoms/sign 单轨挂在 `ls-aegis-boot` |

---

### 1. 阶段总表

| 阶段 | 结果 |
|---|---|
| P0 冻结债务 | 完成。评审清单见 [§10](#10-平台一体--评审门禁)（原门禁脚本已随 `scripts/` 删除） |
| P1 报价单轨 | 完成。删除 `/eoms/quote` 兼容层与前端占位；菜单仅 **40000** 段 |
| P2 接口迁 R | 完成。sign / eoms PC / H5 去掉 adapter，统一 `R` / `PageResp` |
| P3 包名迁域 | 完成。sign/eoms → `com.ls.aegis.biz.*`；业务模块无 `package com.itls` |
| P4 删 legacy | 完成。删除 `ls-aegis-legacy`；存量工具在 `com.ls.aegis.compat.*` |
| P5 SQL 单轨 | 完成。`sql` **仅** `init.sql`；四域 DDL/菜单已并入；Liquibase 供已上线库 |
| P6 H5 对齐 | 完成。工程师 `/auth/login` + SM2；客户 `h5-token`；外观走 SITE 字典 |

**残留（不阻塞上线）：** C 批次（C1～C9）已完成。compat 系统实体（`SysUser` 等）仍 `BaseEntity`；EOMS 与 PSI 主数据双轨留给 M。

---

### 2. 关键做法（摘录）

#### P1 报价

- 删除 eoms `QuoteController` 兼容转发与 `views/eoms/quote`
- 保留 `EomsQuoteNoAllocator` / `EomsQuoteToRepairBridge`（编号与转工单）
- 08-18：下线半成品菜单 40002–40004 与空页

#### P2 / P6 接口与 H5

- 删除 `apis/sign/adapter.ts`、`apis/eoms/adapter.ts`
- PC eoms Controller 统一 `R` / `PageResp`（C5 起走 `PageQueryHelper`）
- 工程师登录走平台鉴权；客户 H5 临时令牌有意保留（不是第二套工程师登录）

#### P4 删 legacy

1. 源码迁入 `ls-aegis-starter`，`com.itls.*` → `com.ls.aegis.compat.*`
2. 业务 import 改写；去掉若依 `BaseController` / `TableDataInfo`
3. 父 POM 去掉 module；删除 `ls-aegis-legacy` 目录

#### P5 SQL

- Liquibase：`plugin_eoms` / `plugin_sign` / `mysql/psi/*` + 菜单 patch
- `init.sql`：eoms 20000、sign 30000、quote 40000、psi 10001+ 及业务表终态
- 08-18：删除 `sql` 下 `_archive` 与域拆分包，避免第二套「完整 init」

---

### 3. P5 联调复测（2026-08-17，admin）

| 接口 | 结果 |
|---|---|
| `/auth/user/info`、`/auth/user/route` | OK |
| `/sign/project/list`、`/sign/seal/list`、`/sign/dashboard/stats` | OK |
| `/eoms/repair/order/list` | OK |
| `GET /quote`（不是 `/quote/list`） | OK |
| `/psi/unit` | OK |

当时修过：SignConfig 自定义 ObjectMapper 导致时间序列化失败；`*:*:*` 权限未覆盖两段式 `quote:list`；sign/eoms Mapper 列名对齐平台 `sys_*`。

---

### 4. 前期升版时间线（v1.3.0 → v1.5.2，2026-08-15/16）

| 版本 | 要点 |
|---|---|
| v1.3.0 | 后端目录/包名统一 `ls-aegis-server` / `com.ls.aegis` |
| v1.3.1 | 清出入库构建产物与死代码 |
| v1.3.2 | 文件指纹 SM3；用户偏好落库 |
| v1.3.3～1.3.4 | 移除短信模块与手机登录 |
| v1.3.5 | 前端表格规范落盘 |
| v1.4.x | 工作台/分析页运维大屏与公告滚动 |
| v1.5.x | 和风天气 JWT；公告发布时间 |

---

### 5. 08-18 重复功能 D 批次

D1–D5 已完成，D6 因删除 `clients/` 取消。C / M / R 亦已全部结束（原施工勾选清单已清理，结论以本文 §3 与 §9 为准）。

---

## 4. 租户 · 应用 · 任务调度使用说明

> 面向非专业运维 / 业务管理员。说明以当前 LS-Aegis 产品界面为准。  
> 日期：2026-08-16（08-18 与截图分册合并为一份）

---

### 读前必懂的三句话

1. **租户管理**：把一套系统「租」给多家单位用，每家的数据互相看不见。
2. **应用管理**（能力开放）：给外部系统发「门禁卡」（密钥），让对方按规则调用本系统接口。
3. **任务调度**：让系统在指定时间自动干活（发公告、清理数据等），并可在界面启停、看日志。

三者互不替代。下面先讲如何打开菜单，再分模块说明「怎么用」和「什么时候用」。

---

### 0. 使用前：如何让菜单出现

路径：**系统管理 → 系统配置 → 功能模块**

| 开关名称 | 默认 | 打开后你会看到 | 关闭后会发生什么 |
|----------|------|----------------|------------------|
| 租户管理 | **关** | 左侧「租户管理」菜单；登录页可出现「租户编码」输入框 | 菜单隐藏、相关接口不可用；登录页不显示租户编码框 |
| 应用管理 | **关** | 左侧「能力开放 → 应用管理」 | 菜单隐藏、相关接口不可用 |
| 任务调度 | **关** | 左侧「任务调度」菜单（实际跑任务还须运维开调度服务） | 菜单隐藏、相关接口不可用 |

**给非专业人士的提醒：**

- 改完开关后，若菜单没立刻变化，可刷新页面或重新登录。
- 「任务调度」只开本页开关还不够，还要运维启动调度中心（第 3 章）。
- 「租户管理」完整启用：本页开关开 **且** 运维设置 `CONTINEW_STARTER_TENANT_ENABLED=true`（默认均为关）。
- 多租户的数据隔离依赖服务器中间件配置；产品开关管「菜单与登录框」。一般由运维统一处理。

---

### 1. 租户管理

#### 1.1 这是干什么的（大白话）

想象本系统是一栋写字楼：

- **平台超级管理员**负责「哪几家公司可以入驻、每家能用哪些楼层（功能）」。
- 每一家公司就是一个**租户**。
- **套餐**就是「功能菜单包」：基础版、标准版、旗舰版等。
- 每家公司入驻时，系统自动建好该公司的管理员账号；之后该公司管理员只管理自己公司的用户、部门、业务数据，**看不到别的公司**。

#### 1.2 菜单在哪里

| 菜单 | 路径 | 用途 |
|------|------|------|
| 租户管理 → 套餐管理 | `/tenant/package` | 先定义「能用哪些功能」 |
| 租户管理 → 租户管理 | `/tenant/management` | 再开通具体客户 / 组织 |

**推荐顺序：先建套餐，再建租户。**

#### 1.3 套餐管理：怎么用

##### 什么时候需要建套餐

- 要给客户 A、客户 B 不同功能范围（例如 A 有任务调度，B 没有）。
- 要做「试用版 / 正式版」功能差异。
- 集团内多子公司，功能包不完全相同。

##### 操作步骤（新增套餐）

1. 进入 **套餐管理**，点 **新增**。
2. 填写：
   - **名称**：例如「标准版」「试用版」。
   - **排序**：数字越小越靠前（列表展示用）。
   - **描述**：给人看的说明，可写「含用户/部门/文件，不含开放能力」。
   - **状态**：启用 / 禁用。只有**启用**的套餐才能被新租户选用。
   - **关联菜单**：在菜单树里勾选该套餐能用的功能。可用展开、全选；注意父子联动。
3. 保存。

##### 修改套餐要注意什么

- 改「关联菜单」后，**所有已使用该套餐的租户**会同步：
  - 租户管理员：会补齐套餐里新增的菜单；
  - 其它角色：套餐外的菜单会被拿掉。
- **结论：** 改套餐等于改「已开通客户的功能上限」，改前先评估影响面。
- **有租户正在使用的套餐不能删除。** 要删须先给相关租户换套餐，或先处理完租户。

#### 1.4 租户管理：怎么用

##### 列表里能看到什么

编码、名称、套餐、域名、过期时间、管理员用户、状态、描述、创建时间等。可用名称/描述搜索，可按套餐筛选。

##### 新增租户（完整流程）

1. 确认已有**启用中**的套餐。
2. 点 **新增**，填写：

| 字段 | 是否必填 | 怎么填（非专业人士版） |
|------|----------|------------------------|
| 名称 | 必填 | 客户或组织的显示名，最多 30 字，如「华东分公司」 |
| 域名 | 可选 | 若希望用域名自动识别租户，填如 `example.com`（界面会带 `http(s)://` 前缀说明）。不绑域名可留空，改用登录时的「租户编码」 |
| 过期时间 | 可选 | 不选 = 永不过期。试用客户可设到期日；到期后无法正常登录使用 |
| 管理员用户 | 仅新增必填 | 该公司管理员的登录名。系统会自动创建该用户，并赋予套餐对应权限 |
| 管理员密码 | 仅新增必填 | 初始密码；请按单位规范设置并妥善告知对方，建议首次登录后修改 |
| 套餐 | 必填 | 选择启用中的套餐 |
| 描述 | 可选 | 备注，如合同号、对接人 |
| 状态 | — | 启用 / 禁用。禁用后该租户不可用 |

3. 保存后，系统会自动：
   - 生成**租户编码**（界面上不能手填，用于登录识别）；
   - 在该租户下创建根部门、管理员角色、管理员用户，并绑好套餐菜单。

4. 把下列信息交给客户管理员：
   - 系统访问地址；
   - **租户编码**（列表/详情里可见）；
   - 管理员用户名与初始密码；
   - 若配置了域名，说明用哪个域名访问。

##### 客户如何登录

1. 打开登录页。
2. 若功能模块「租户管理」已开，通常会出现**租户编码**输入框：填入刚才的编码。
3. 再填管理员账号、密码登录。
4. 不填租户编码时，一般进入**默认租户**（平台自身那一套），不是客户租户。

##### 日常运维操作

| 操作 | 说明 |
|------|------|
| 修改 | 可改名称、域名、过期时间、套餐、描述、状态等；**不能在修改里改管理员账号**（账号是创建时定的） |
| 详情 | 查看完整信息 |
| 修改管理员密码 | 「更多」里；客户忘记密码时由平台侧重置 |
| 删除 | 会清理该租户下用户、部门、角色等数据并踢下线。**不可恢复**，且部分内置记录可能禁止删除。务必确认无在线业务后再删 |

##### 更换套餐

在修改租户时换套餐 → 该租户可用菜单按新套餐同步。换之前想清楚：会不会突然拿走客户正在用的菜单。

#### 1.5 典型使用场景

| 场景 | 建议做法 |
|------|----------|
| SaaS 卖给多家客户 | 每客户一租户；按合同选套餐；正式客户过期时间留空或设合同结束日 |
| 集团多组织 | 每法人/事业部一租户；套餐可相同；用名称、描述区分 |
| 试用 30 天 | 建「试用套餐」（菜单少一点）→ 建租户并设过期时间 → 到期自动不可用 |
| 按域名进不同门户 | 租户填域名；用户访问对应域名时系统识别租户（与租户编码登录可并存） |
| 客户管理员忘密 | 平台超管：租户管理 → 更多 → 修改管理员密码 |

#### 1.6 常见误区与注意

- **关了功能模块开关 ≠ 库里的租户数据消失**，只是菜单和登录框隐藏。
- **套餐一改，已开通租户跟着变**，不是「只影响以后新建的」。
- **删除租户不可逆**，先备份或确认业务已迁移。
- 租户管理员只能管本租户；**不要**把「建租户 / 建套餐」的权限随便给普通业务员。
- 域名写错会导致「打不开」或进错租户，改前先小范围验证。

---

### 2. 应用管理（能力开放）

#### 2.1 这是干什么的（大白话）

当**别的软件**（ERP、门户、脚本、合作方系统）需要调用本系统的开放接口时，不能用人名密码登录网页那一套，而是发一张「应用凭证」：

- **Access Key（访问密钥 / AK）**：公开身份，类似账号。
- **Secret Key（秘密密钥 / SK）**：签名私钥，类似密码，**绝不能公开张贴**。

对方每次请求时用 SK 做签名，本系统验签通过才放行。

> **重要区分：**  
> 菜单「能力开放 → 应用管理」管的是 **对外 API 凭证**。  
> 「系统配置 → 客户端配置」管的是 **网页/客户端登录 Token 策略**（如 PC 端登录多久过期）。  
> **二者不是一回事。** 下文只讲「应用管理」。

#### 2.2 菜单在哪里

**能力开放 → 应用管理**（路径 `/open/app`）

若看不到菜单：到「功能模块」确认「应用管理」开关为开，并确认当前账号有权限。

#### 2.3 怎么用

##### 新增应用

1. 点 **新增**。
2. 填写：

| 字段 | 说明 |
|------|------|
| 名称 | 必填。建议写成对接系统名，如「财务系统对接」「门户单点」 |
| 失效时间 | 可选。不选 = 不过期。临时对接可设到期日 |
| 描述 | 可选。写对接人、用途、工单号 |
| 状态 | 启用 / 禁用。禁用后立刻不能调接口 |

3. 保存后，系统**自动生成** Access Key 与 Secret Key，**不需要也不会让你手填**。

##### 查看与保管密钥

- 列表里 Secret 默认是掩码（打码）的。
- 需要把密钥交给开发对接时：使用 **查看密钥**（需有对应权限）。
- 建议：通过加密通道或当面交接；不要发到公开群；对方存到配置中心/环境变量，不要写进前端代码仓库。

##### 重置密钥

- 怀疑泄漏、人员离职、对接方要求轮换时：点 **重置密钥**。
- **旧 Secret 立即失效**，对方必须同步改配置，否则接口全部失败。
- 重置前先通知对接方，约好切换时间。

##### 修改 / 禁用 / 删除

- **修改**：可改名称、失效时间、描述、状态。
- **禁用**：紧急止损（怀疑滥用）时可先禁用，比删除更可逆。
- **删除**：确认不再使用再删；删除后该 AK 不可再调用。
- **导出**：按权限导出列表，注意导出文件也含敏感信息，同样要保管好。

#### 2.4 对接方需要知道什么（你可转述给开发）

调用开放接口时，请求一般需带：`accessKey`、`timestamp`（时间戳）、`nonce`（一次性随机串）、`sign`（签名）。  
签名用 Secret 对参数排序拼接后做摘要（国密开启时为 SM3，关闭时为 MD5）。  
具体字段名与接口列表以贵司开放 API 文档为准；业务管理员只需保证：**应用是启用的、未过期、密钥正确且未重置丢同步**。

#### 2.5 典型使用场景

| 场景 | 建议做法 |
|------|----------|
| 第三方系统每天同步数据 | 建一个专用应用，描述写清系统名；密钥只给对方运维；可设较长或不过期 |
| 临时联调 / 外包短期接入 | 设失效时间；到期自动失效，减少遗忘关停风险 |
| 多套外部系统 | **一系统一应用**，便于单独禁用、轮换密钥，互不影响 |
| 密钥疑似泄露 | 先**禁用**或**重置密钥**，再排查；通知对方立即更换 |
| 项目结束 | 禁用 → 确认无调用 → 再删除 |

#### 2.6 常见误区与注意

- **不能**用 Access Key / Secret Key 代替网页登录账号。
- **不能**把 Secret 贴到前端页面或公开仓库。
- 重置密钥后旧的一定失效，对接方未改配置会表现为「全部报签名错误」。
- 查看 / 重置密钥权限应只给少数管理员。
- 应用过期或禁用后，表现也是调用失败，排查时先看状态与失效时间。

---

### 3. 任务调度

#### 3.1 这是干什么的（大白话）

有些事不适合人工每天点按钮，例如：

- 每天零点发布到期公告；
- 定期清理过期临时数据；
- 定时对账、同步。

**任务调度**就是：在界面上登记「什么时候、跑哪个程序入口」，由调度中心到点通知本系统执行，并留下日志；出问题可停止、可重试。

#### 3.2 菜单在哪里

| 菜单 | 路径 | 用途 |
|------|------|------|
| 任务调度 → 任务管理 | `/schedule/job` | 建任务、启停、立即执行 |
| 任务调度 → 任务日志 | `/schedule/log` | 看执行结果、停止运行中任务、失败重试 |

#### 3.3 使用前必须具备的条件（缺一不可）

业务管理员自己能做的：

1. **功能模块**里「任务调度」开关打开。

必须由运维 / 开发配合的：

2. 服务器配置 **`snail-job.enabled=true`**，并启动 **SnailJob 调度中心**（独立服务）。
3. 账号、命名空间、任务组、Token、调度中心地址与口令配置正确（与调度中心一致）。
4. 业务代码里已经写好对应的**执行器**（用注解注册的任务方法名，例如公告相关的 `NoticePublishJob`）。  
   **只在界面建任务、没有对应执行器 = 到点也不会真正干活。**

若国密开启（`gm.enable=true`），调度中心库中的登录口令摘要还需按国密说明更新，否则平台连不上调度中心。细节见模块文档 `ls-aegis-biz-schedule/README-GM.md`（运维执行即可）。

**菜单仍不出现时，按此顺序排查：** 功能模块开关 → `snail-job.enabled` → 调度中心是否启动 → 重新登录。

#### 3.4 任务管理：怎么用

##### 查询列表

- 须先选择**任务组**再查询（没有任务组时列表可能查不出数据）。
- 任务组需与调度中心 / 配置里的分组一致（常见默认组名与配置中的 `snail-job.group` 对应，如 `ls-aegis`）。
- 还可按任务名称、任务状态筛选。

##### 新增任务（字段说明）

表单分四块：

**① 基础配置**

| 字段 | 说明 |
|------|------|
| 任务组 | 必选。选错组会找不到执行节点 |
| 任务名称 | 给人看的名字，如「每日公告发布」 |
| 描述 | 用途说明 |

**② 调度配置（什么时候跑）**

| 触发类型 | 怎么理解 | 填什么 |
|----------|----------|--------|
| 固定时间（间隔） | 每隔 N 秒跑一次 | 间隔时长，单位秒，例如 60 = 每分钟 |
| CRON | 按日历规则跑 | Cron 表达式；界面有常用预设（如每分钟）和「时钟」按钮打开可视化生成器 |

Cron 对非专业人士较难，可用生成器，或请开发给一条表达式。常见例子：

| 含义 | 表达式示例 |
|------|------------|
| 每分钟 | `0 * * * * ?` |
| 每天 0 点 | `0 0 0 * * ?` |
| 每天 9:30 | `0 30 9 * * ?` |

**③ 任务配置（跑什么）**

| 字段 | 说明 |
|------|------|
| 任务类型 | **集群**：多台机器里选一台跑（最常用）；**广播**：所有机器都跑；**静态切片**：把参数拆成多片并行处理（参数为多组分片值） |
| 执行器名称 | **必须与代码里注册的名字完全一致**（大小写都要一致）。写错会出现「无客户端节点」之类失败 |
| 任务参数 | 普通任务填一段文本参数；切片任务按行/多项填分片参数（按开发要求） |

**④ 高级配置（一般保持默认，特殊需求再改）**

| 字段 | 通俗含义 |
|------|----------|
| 路由策略 | 多台机器时怎么选：轮询、随机、一致性哈希、LRU 等 |
| 阻塞策略 | 上次还没跑完、这次又到点了：丢弃新的 / 覆盖 / 并行再开 |
| 超时时间 | 跑太久算失败，单位秒 |
| 最大重试次数 | 失败后再试几次 |
| 重试间隔 | 两次重试之间隔多久（秒） |
| 并行数 | 允许并行的程度 |

不确定时：请开发按任务类型给出推荐值；业务侧重点核对「时间」和「执行器名称」。

##### 日常操作

| 操作 | 说明 |
|------|------|
| 行内启停开关 | 关闭后不再按计划调度；开启后恢复 |
| 立即执行 | 不等到点，马上触发一次。**只表示「已请求执行」**，成败请到「任务日志」看 |
| 修改 | 改调度时间、参数等 |
| 查看日志 | 跳到日志页并带上该任务 |
| 删除 | 确认不再需要后再删 |

#### 3.5 任务日志：怎么用

##### 能查什么

- 筛选：任务组、任务名、执行状态、时间范围（默认常为近 7 天）。
- 列表：调度时间、执行状态、执行备注、执行耗时等。

##### 执行状态（看懂结果）

| 状态 | 含义 | 你可以做什么 |
|------|------|--------------|
| 待处理 | 已排队，还没跑完 | 等待 |
| 运行中 | 正在执行 | 必要时点 **停止** |
| 成功 | 正常完成 | 无需处理 |
| 已失败 | 执行失败 | 看备注原因；修好后可 **重试** |
| 已停止 | 被手动停掉 | 可按需 **重试** |
| 已取消 | 被取消 | 可按需 **重试** |

备注里常见提示含义：

- **超时**：任务跑太久，考虑加大超时或优化任务本身。
- **无客户端节点**：执行器名写错、应用未连上调度中心、或任务组不对。
- **手动停止 / 执行失败 / 执行成功**：字面含义。

#### 3.6 典型使用场景

| 场景 | 建议做法 |
|------|----------|
| 公告到期自动发布 | 使用已有执行器（如 `NoticePublishJob`）；Cron 设合适频率；用日志确认是否成功 |
| 每天凌晨批处理 | Cron 设 `0 0 0 * * ?`；集群类型即可；避开业务高峰 |
| 临时补跑一次 | 任务管理 → **立即执行** → 到日志确认结果 |
| 发现任务死循环或拖垮系统 | 先**行内关闭**或对运行中日志点**停止**，再改间隔/阻塞策略 |
| 偶发失败 | 日志里 **重试**；若反复失败，把备注交给开发查执行器与数据 |
| 多机部署要每台都做本地清理 | 任务类型选**广播**（需开发确认任务是否适合广播） |

#### 3.7 常见误区与注意

- 只开「功能模块」不开 `snail-job` / 不启调度中心 → **菜单可能仍没有**，或有菜单也无法真正调度。
- 界面建了任务但**执行器名称与代码不一致** → 日志里常见「无客户端节点」。
- 「立即执行」成功提示 ≠ 业务一定成功，**以任务日志为准**。
- 任务调度与「应用管理」的 AK/SK、与「客户端配置」无关。
- 国密环境下改调度库口令后，官方 SnailJob 网页控制台可能无法用原方式登录，**不影响本平台任务管理页通过接口调度**（以运维说明为准）。

---

### 4. 三者怎么配合（场景串联）

#### 场景 A：给新客户开通一套「标准功能」

1. 套餐管理：勾选客户合同内的菜单（若含开放能力、调度，一并勾上）。
2. 租户管理：新建租户，选该套餐，设管理员与过期时间。
3. 把租户编码与管理员账号交给客户。
4. 若客户还要对接外部系统：在**该租户上下文**下（或按你们权限模型）建「应用」，把 AK/SK 给客户开发。
5. 若客户需要定时任务：确认调度基础设施已开，再建任务（通常由平台或客户侧有权限的管理员操作）。

#### 场景 B：只给内部用、暂不开放第三方

- 租户：可用默认租户，或不开多租户产品开关。
- 应用管理：功能模块可关，减少误建密钥。
- 任务调度：按需开启。

#### 场景 C：密钥泄漏 + 定时任务异常同时发生

1. 应用管理：立即**禁用**或**重置密钥**，通知对接方。
2. 任务调度：关闭异常任务开关，必要时停止运行中日志，再查备注交给开发。
3. 二者分开处理，不要混用概念。

---

### 5. 权限与职责建议（非专业团队怎么分工）

| 角色 | 建议可做 | 不建议随便做 |
|------|----------|--------------|
| 平台超级管理员 | 套餐、租户、功能模块开关、应用密钥、任务启停 | 无评估地改套餐菜单、删租户 |
| 租户管理员 | 本租户用户/业务；若授权则可管本租户应用 | 建/删其它租户 |
| 对接开发 | 保管 SK、实现签名调用、提供执行器名称与 Cron | 在生产随意重置密钥不通知业务 |
| 运维 | 调度中心、配置项、国密口令、备份 | 业务侧乱改套餐与租户过期 |

各操作还需账号具备对应权限码（如 `tenant:management:create`、`open:app:secret`、`schedule:job:trigger` 等），由角色授权控制。

---

### 6. 术语速查表

| 术语 | 一句话解释 |
|------|------------|
| 租户 | 一套数据隔离边界，通常对应一家客户或一个组织 |
| 租户编码 | 登录时用来定位是哪家租户的编码 |
| 套餐 | 该租户能用哪些菜单/功能的上限模板 |
| 租户隔离 | 不同租户的数据互不可见 |
| Access Key | 开放应用的公开身份 |
| Secret Key | 开放应用的签名私钥，等同密码 |
| 客户端配置 | 网页等登录端的 Token 策略，不是开放应用 |
| 调度中心 | 负责任务计时与下发的独立服务（SnailJob） |
| 执行器 | 业务程序里注册的任务方法名 |
| 任务组 | 任务的逻辑分组，须与配置/中心一致 |
| Cron | 用表达式描述「几点几分跑」 |
| 阻塞策略 | 上次未跑完又触发时如何处理 |
| 路由策略 | 多台机器时选哪台执行 |

---

### 7. 快速自检清单

**租户**

- [ ] 功能模块「租户管理」已开  
- [ ] 已建启用套餐并勾选菜单  
- [ ] 已建租户并记下编码、管理员账号  
- [ ] 用租户编码可登录；到期/禁用符合预期  

**应用**

- [ ] 功能模块「应用管理」已开  
- [ ] 已建应用且为启用、未过期  
- [ ] Secret 已安全交接；重置后对接方已同步  

**任务调度**

- [ ] 功能模块「任务调度」已开  
- [ ] 运维已开 `snail-job` 并启动调度中心  
- [ ] 任务组可选、执行器名称与开发确认一致  
- [ ] 立即执行后日志为成功；失败备注已处理  

---

### 附录：相关代码 / 文档索引（给运维或二次开发）

| 说明 | 位置 |
|------|------|
| 租户前端 | `ls-aegis-web/src/views/tenant/` |
| 开放应用前端 | `ls-aegis-web/src/views/open/app/` |
| 任务调度前端 | `ls-aegis-web/src/views/schedule/` |
| 功能模块开关 | `ls-aegis-web/src/views/system/config/module/` |
| 客户端配置（勿与应用管理混淆） | `ls-aegis-web/src/views/system/config/client/` |
| 调度国密口令 | `ls-aegis-server/ls-aegis-biz-schedule/README-GM.md` |
| 菜单与权限种子 | `ls-aegis-server/sql/init.sql` |

---

## 5. 进销存业务说明

> 模块：`ls-aegis-biz-psi`（`com.ls.aegis.biz.psi`）  
> 前端：`ls-aegis-web` 的 `apis/psi` + `views/psi`  
> 菜单 ID：**10001–10999**  
> SQL：全新安装 `sql/init.sql`；已上线库走 Liquibase `mysql/psi/*`  
> 更新日期：2026-08-18（合并原开发文档 / 导入导出清单 / 列表与表单说明）

---

### 1. 工程约定

| 项 | 约定 |
|---|---|
| 挂载 | 由 `ls-aegis-boot` 扫描，禁止独立启动 |
| 实体 | `TenantBaseDO`（含 `tenantId`）；现网租户中间件默认关，写入 `0` |
| 启停 | `status`：1 启用 / 2 禁用 |
| 单据状态 | 业务字段 `doc_status` |
| 逻辑删除 | `deleted`：0 未删，删后写自身 id |
| 权限 | `psi:<资源>:<动作>`；禁止信任前端传的隔离字段 |
| 国密 | 敏感字段 SM4；传输 SM2 |

表格/表单观感以 Cursor 规则为准：`.cursor/rules/前端表格规范.mdc`、`.cursor/rules/前端统一架构.mdc`。列表必须走 `BaseTable`，禁止 `scroll.x: '100%'`。

**表单档位（已落地）：** 无明细弹窗宽 640；有明细 960；核销小窗约 520；业务表单 `layout="vertical"` + `.psi-biz-form`。

---

### 2. 导入 / 导出 / 附件（已完成）

| 阶段 | 内容 |
|---|---|
| 基建 | `PsiImportHelper` + `PsiImportDrawer` + 权限种子 |
| P0 | 仓库 / 往来单位 / 商品 导入导出；库存期初导入；银行流水 Excel |
| P1 | 打印模板 / 合同附件；流水、出入库、收付、应收应付、发票列表导出；盘点实盘导入 |
| P2 | 分类 / 账户导入导出；退货/调拨/调整/盘点/合同列表导出；发票扫描件 |
| P3 | 出入库草稿明细导入；进销项税控 Excel 导入 |

框架事实：ContiNew 有 `Api.EXPORT`、无 `Api.IMPORT`；导入对标用户模块 `template → parse → import`。

#### 列表页能力（25）

| 页面 | 导入 | 导出 | 附件 | 状态 |
|---|---|---|---|---|
| 商品 / 单位 / 仓库 | 是 | 是 | — | 完成 |
| 商品分类 / 公司账户 | 是 | 是 | — | 完成 |
| 期初 | 库存期初 | 导出待 | — | 导入完成 |
| 采/销合同 | 否 | 是 | fileId | 完成 |
| 入/出库 | 明细行 | 是 | 打印/Word | 完成 |
| 退货 / 调拨 / 调整 | 否 | 是 | — | 完成 |
| 盘点 | 实盘 | 是 | — | 完成 |
| 库存流水 | 否 | 是 | — | 完成 |
| 库存查询（仓×商品实时剩余） | 否 | 是 | — | 完成；菜单在基础数据·仓库管理旁 |
| 收款 / 付款 / 应收 / 应付 | 否 | 是 | — | 完成 |
| 进/销项发票 | 税控 | 是 | 扫描件 | 完成 |
| 银行对账 | Excel+手填 | — | — | 完成 |
| 打印模板 | 否 | 否 | Word | 完成 |
| 待办 / 工作台 | 否 | 否 | — | 工作台已并入仪表盘 |

经营概览：仪表盘工作台 `PsiBusinessPanel`，接口 `GET /psi/workbench/summary`。

---

### 3. 后续（不在本文施工）

主数据与 EOMS 的合表（客户/供应商/仓库/物料）已按「主数据以 PSI 为准」收敛完毕，结论见本文 [§3](#3-平台一体收敛) 与 [§9](#9-平台一体--整改清单)。EOMS 只保留报修/工单/结算。

---

## 6. 全项目冒烟测试

| 项 | 内容 |
|---|---|
| 日期 | 2026-08-19 |
| 环境 | Docker：`ls-aegis-web:8080` · `ls-aegis-server:18000` · MySQL · Redis |
| 账号 | `admin`（SM2 传输加密登录；本机 `LOGIN_CAPTCHA_ENABLED=0`） |
| 脚本 | 当时仓库根 `_smoke_e2e.mjs`（已清理，勿再依赖） |
| 结论 | **71 / 73 通过**（0 失败 · 2 跳过） |

---

### 1. 结论

API 全量冒烟覆盖：鉴权国密、进销存、报价转单、工程师全链路、结算确认→应收→关单、**应收部分收款→结清→实收入账**、H5 报修确认转工单、签章合并/单页、进销存单据浅读。

跳过 2 条均为预期：已有叶子分类（D1b）、派单即接单无需单独 accept（D6b）。

---

### 2. 用例明细

#### 读路径 + 浅写

| 编号 | 用例 | 结果 |
|---|---|---|
| S0 | PC 前端静态站 `:8080` | PASS |
| S1–S3 | 国密公钥 / 验证码 / admin SM2 登录 | PASS |
| S4a / S4b | 用户信息 / 路由 | PASS |
| S5 | 无 Token 拒绝 | PASS |
| S6a / S6b | 往来单位列表 / 新建 | PASS |
| S7 / S8 | 工单列表 / 报价列表 | PASS |
| S9a / S9b | 维修应收 / 维修收款 | PASS |
| S10a–c | 签章项目 / 印章 / 看板 | PASS |
| S11a–d | 签章文件 allowed / 上传 / 回读 / 下载 | PASS |
| S12 | H5 工程师同 `/auth/login` | PASS |
| S13a–d / S13u | H5 令牌 / 水印图 / 报修申请 / 列表详情 | PASS |
| S14a | `GET /psi/payment` | PASS |

#### 写路径加深

| 编号 | 用例 | 结果 | 说明 |
|---|---|---|---|
| D1a–c | 分类树 / 商品 | PASS（D1b SKIP） | 已有叶子分类 |
| D2a–e | 报价明细 → 改合计 → 转工单 | PASS | 合计 240 |
| D3 | 转单后工单列表 | PASS | |
| D4a / D4b | 工程师 profile / tasks | PASS | |
| D5 | PC 报修申请列表 | PASS | |
| D6a–e | 派工→开始→照片/记录→完工 | PASS（D6b SKIP） | → `WAIT_SETTLEMENT` |

#### 结算闭环

| 编号 | 用例 | 结果 | 说明 |
|---|---|---|---|
| D8a | 回查结算草稿 | PASS | 完工自动建草稿 |
| D8b | 填写服务/人工费用 | PASS | serviceFee=100 |
| D8c | 确认结算 | PASS | `CONFIRMED` income=160 |
| D8d | 工单 `FINISHED` | PASS | |
| D8e | 生成应收 | PASS | amount=160 `UNPAID` |
| D8f | 关闭工单 | PASS | → `CLOSED` |

#### 应收核销 / 实收入账

| 编号 | 用例 | 结果 | 说明 |
|---|---|---|---|
| D12a | 部分收款 `/receipt/verify` | PASS | 60 → `PARTIAL` 欠款 100 |
| D12b | 结清收款 | PASS | 100 → `PAID` 欠款 0 |
| D12c | 收款单回读 | PASS | 2 笔合计 160 |
| D12d | 实收收入回读 | PASS | 2 条合计 160（与应收一致） |
| D12e | 已结清再收款应拒绝 | PASS | 接口拒绝；对外文案见 §3.3 |

#### 签章落章

| 编号 | 用例 | 结果 | 说明 |
|---|---|---|---|
| D7a–c | 可签项目 / 印章 / 可签 PDF | PASS | |
| D7d | 合并签章 `/unified` | PASS | |
| D7e / D7f | 下载已签 PDF / 回单 | PASS | ~293KB |
| D7g | 单页签章 `/run` | PASS | PDFBox 3.0.3 |
| D11a | 签章日志列表 | PASS | |

#### 报修确认转单 + 进销存浅读

| 编号 | 用例 | 结果 | 说明 |
|---|---|---|---|
| D9a | 确认 H5 报修 | PASS | → `CONFIRM` |
| D9b | 报修转工单 | PASS | → `ORDER` |
| D10a–g | 仓库/流水/入出库/采销合同/选项 | PASS | 空库表亦 200 |

---

### 3. 缺陷与阻塞

#### 3.1 单页签章 PDFBox 混用（已修复）

进销存用 PDFBox 3.0.3、签章曾声明 2.0.32 调用 `PDDocument.load`。已统一 `pdfbox.version=3.0.3`，`SignPdfServiceImpl` 改 `Loader.loadPDF`。D7g 回归通过。

#### 3.3 已结清再收款的对外文案（未修）

核销逻辑已抛 `ServiceException("应收账款已结清，不可再次收款")`，容器日志可见。前端拿到的是 `code=1 msg=服务器异常，请联系管理员`（graceful-response 包了一层）。业务已拒绝，不影响核销闭环；应对齐 `ServiceExceptionHandler`，把原文案回给客户端。

#### 3.4 其它说明

- 雪花 ID 禁止 JS `Number()` 丢精度（完整约定见 [§15](#15-前端雪花-id-精度与-select-回显)）。
- `assignEngineer` 为「派单即接单」。
- M2 校对集已落 `utf8mb4_general_ci`。

---

### 4. 复跑

原 `_smoke_e2e.mjs` 已清理，勿再依赖仓库根冒烟脚本。结论以本节明细为准。


### 5. 仍未覆盖（非阻塞）

| 项 | 说明 |
|---|---|
| 浏览器 UI 点击流 | 仍为 API 冒烟 |
| 进销存完整入出库写路径 | 本机无仓库种子，仅列表浅读 |
| 口令过期强制改密 | 本机 admin 未过期 |
| 已结清再收款对外文案 | 见 §3.3，接口已拒绝 |

---

## 7. 平台说明（INTEGRATION）

### 目标

仓库里只有「一个平台」：

- **同一套鉴权**：Sa-Token + RBAC + 国密
- **同一套接口形态**：平台 `R` / 分页（与 psi 一致）
- **同一套前端范式**：Vue3 + Arco + `GiPageLayout` / `BaseTable` / `GiForm`（见 `.cursor/rules/前端统一架构.mdc`）
- **同一份部署**：统一 SQL / Liquibase + `sql/init.sql`

业务（psi / eoms / sign / quote）只是平台上的**功能域**，不是拼装的独立子系统。原则见 `.cursor/rules/平台一体.mdc`。

**开发落地步骤**（P0～P6 壳层，已完成）：见 [§8 平台一体 · 开发分步](#8-平台一体--开发分步)。  
**后续整改**（只扩功能域、不扩第二套壳）：见 [§9 平台一体 · 整改清单](#9-平台一体--整改清单)。

| 能力 | 模块代号 | 技术落点 |
|------|----------|----------|
| 鉴权 / RBAC / 国密 / 文件 / 调度 / 租户 | 底座 | Spring Boot 3 + Sa-Token + Vue3 + Arco |
| 进销存 | `psi` | `ls-aegis-biz-psi` / `views/psi` |
| 运维报修 | `eoms` | `ls-aegis-biz-eoms` / `views/eoms` / `ls-aegis-h5` |
| 电子签章 | `sign` | `ls-aegis-biz-sign` / `views/sign` |
| 报价 | `quote` | `ls-aegis-biz-quote` / `views/quote` |

### 模块扫描

| 模块 | 包 / AutoConfiguration |
|------|-------------------|
| psi | `com.ls.aegis.biz.psi`（boot `scanBasePackages=com.ls.aegis`） |
| quote | `com.ls.aegis.biz.quote.config.QuoteConfiguration` |
| eoms | `com.ls.aegis.biz.eoms`（`EomsConfiguration`；Controller 在 `…eoms.controller`） |
| sign | `com.ls.aegis.biz.sign`（`SignConfiguration`） |
| compat（过渡） | `com.ls.aegis.compat.*`（在 `ls-aegis-starter`；原 legacy，禁止扩大） |

### SQL / 部署

**部署只认：**

1. Liquibase：`ls-aegis-boot/.../db/changelog/db.changelog-master.yaml`
2. 手工/镜像初始化：`ls-aegis-server/sql/init.sql`

二者均已含平台主数据 + **psi / eoms / sign / quote** 菜单与业务表种子。`sql` 下仅保留 `init.sql`，不要再对库执行第二套脚本。

字符集：MySQL 客户端 `utf8mb4`；含中文 SQL 用 `docker cp` + 容器内导入，避免 PowerShell 管道乱码。

菜单 ID 段：psi `10001+` · eoms `20000+` · sign `30000+` · quote `40000+`。

### 收敛状态

| 项 | 状态 |
|---|---|
| 前端单 UI（Arco，无 Element Plus） | 已完成 |
| 报价单轨（biz-quote，退役 `/eoms/quote` 双轨） | **已完成**（2026-08-17） |
| eoms/sign 响应迁平台 `R`、去掉前端 adapter | **已完成**（含 H5，2026-08-17） |
| `com.itls.*` → `com.ls.aegis.biz.*`，删除 `ls-aegis-legacy` | **已完成**（包名 P3；模块 P4）；实体 Excel 暂在 `com.ls.aegis.compat.*` |
| H5 鉴权 / 国密与 PC 对齐 | **已完成**（工程师平台登录+SM2；客户 h5-token；Vant 仅 UI） |
| 业务种子完全并入统一部署 | **已完成**（P5：`init.sql` + Liquibase 含四域；`sql` 仅留 init） |

分步与看板：[§8 平台一体 · 开发分步](#8-平台一体--开发分步)；  
整改范围：[§9 平台一体 · 整改清单](#9-平台一体--整改清单)（D/C/M/R 已全部结束）；  
进度记录：本文 [§3 平台一体收敛](#3-平台一体收敛)。

### 刻意不做

- 不再提供可独立启动的 RuoYi `admin` 应用
- 不复活 `apps/` 独立工程树
- 统一使用仓库根 `docker/` 部署
- 本地密钥、`node_modules`、`target`、Electron 安装包不入库

---

## 8. 平台一体 · 开发分步

> 目标见 [§7 平台说明](#7-平台说明integration)、原则见 `.cursor/rules/平台一体.mdc`。  
> 本节把「一个平台」收敛拆成可执行阶段；每阶段有入口条件、动作清单、完成标准。

### 总原则（全程遵守）

1. **只扩功能域，不扩第二套壳**：新能力进 `ls-aegis-biz-*` + `ls-aegis-web`，禁止独立 admin / `apps/` / 第二套鉴权。
2. **四统一**：鉴权（Sa-Token + RBAC + 国密）→ 接口（`R` / 分页）→ 前端（Arco + `GiPageLayout` / `BaseTable` / `GiForm`）→ 部署（Liquibase + `sql/init.sql`）。
3. **禁止扩大债务**：禁止新增 `AjaxResult` / `TableDataInfo` / 若依 `BaseController` / `com.itls.*` / 前端若依 adapter 依赖面。
4. **示范域**：`psi` 为范式标杆；迁移时对照 `biz-psi` 与 `views/psi`。

```mermaid
flowchart LR
  P0[P0 冻结债务] --> P1[P1 报价单轨]
  P1 --> P2[P2 接口迁 R]
  P2 --> P3[P3 包名迁域]
  P3 --> P4[P4 删 legacy]
  P4 --> P5[P5 SQL 单轨]
  P5 --> P6[P6 H5 对齐]
```

---

### P0 · 冻结债务（立刻，1 天内）

**目的**：停止「拼装式」继续扩大。

| 动作 | 说明 |
|------|------|
| 代码评审门禁 | 新 PR 不得新增 `AjaxResult` / `TableDataInfo` / `BaseController` / `com.itls` 新类 |
| 功能域挂载 | 新接口只进对应 `ls-aegis-biz-*`，由 `ls-aegis-boot` 扫描 |
| 菜单/种子 | 改种子必须同步 Liquibase + 重新对齐 `sql/init.sql`（见 SQL 规范） |
| 文档 | 新同学先读本文 §8 + §7，再动 eoms/sign |

**完成标准**

- [x] 团队知悉「禁止扩大 legacy」（原则见 `.cursor/rules/平台一体.mdc`）
- [x] Review 检查清单：见 [§10](#10-平台一体--评审门禁)

---

### P1 · 报价单轨（优先，约 3～5 天）

**目的**：消灭「eoms 报价 + biz-quote」双轨。

| 步骤 | 后端 | 前端 | 部署 |
|------|------|------|------|
| 1.1 | ~~迁入 biz-quote~~ **已完成**（主实现在 `com.ls.aegis.biz.quote`） | — | — |
| 1.2 | ~~兼容 `/eoms/quote`~~ **已删除**兼容 Controller | `apis/quote` + `views/quote`；eoms 占位页已删 | 菜单仅 40000 段 |
| 1.3 | eoms 仅保留 `QuoteNoAllocator` / `QuoteToRepairBridge` 桥接 | 已删 `apis/eoms/quote.ts` | `init.sql` 已并入 40000 |
| 1.4 | 桌面壳仅离线 | — | — |

**完成标准**

- [x] 业务持久化仅在 `biz-quote`
- [x] 无第二套报价菜单主路径 / 无 `/eoms/quote` Controller
- [x] `views/quote` 接真实 REST
- [x] `biz-quote` 去掉 `ls-aegis-legacy`（并入 P4）

---

### P2 · 接口形态迁 `R`（按域推进，约 1～2 周）

**目的**：eoms / sign 与 psi 同一响应约定，去掉前端 adapter。

#### 推荐顺序

1. **sign**（Controller 少）→ 再 **eoms**
2. 每个 Controller：**先改返回类型与分页，再改前端调用，最后删 adapter 引用**

#### 单接口迁移模板

```
后端：AjaxResult / TableDataInfo → R<T> / PageResp（对照 psi）
前端：apis/{域} 去掉 adapter，直接 http.get/post
联调：列表/详情/写操作各验一条；权限拒绝路径验一条
```

| 子阶段 | 范围 | 完成标准 |
|--------|------|----------|
| P2.1 | sign 全部 Controller → `R` + 前端去 adapter | **已完成** |
| P2.2 | eoms 非 H5 Controller → `R` + 删 `apis/eoms/adapter.ts` | **已完成**（2026-08-17） |
| P2.3 | eoms H5 / 工程师接口 → `R`（与 P6 可同做） | **已完成**（2026-08-17） |
| P2.4 | 删除 eoms adapter | **已完成**（随 P2.2） |

**完成标准**

- [x] sign 业务 Controller / 前端无若依 adapter
- [x] eoms **PC** Controller 零 `AjaxResult` / `TableDataInfo`（H5 属 P2.3）
- [x] PC 前端无若依 adapter（`apis/eoms` / `apis/sign`）
- [x] 新增接口评审只接受平台 `R`（P0 门禁）
- [x] H5 Controller 迁 `R`（P2.3 / P6）

---

### P3 · 包名迁功能域（与 P2 可重叠，约 1～2 周）

**目的**：`com.itls.*` → `com.ls.aegis.biz.*`，模块边界与包一致。

| 步骤 | 动作 |
|------|------|
| 3.1 | ~~`sign`：`com.itls.sign` → `com.ls.aegis.biz.sign`~~ **已完成**（`SignConfiguration` / AutoConfiguration.imports） |
| 3.2 | ~~`eoms`：`com.itls.eoms` + `com.itls.web.controller.eoms` → `com.ls.aegis.biz.eoms`（Controller → `…eoms.controller`）~~ **已完成** |
| 3.3 | ~~MyBatis mapper XML namespace、`@ComponentScan` / `@MapperScan`~~ **已完成** |
| 3.4 | ~~业务模块内无 `package com.itls`~~ **已完成**（实体等仍 import `com.itls.common.*`，属 P4） |

**完成标准**

- [x] `biz-eoms` / `biz-sign` 内无 `package com.itls`
- [x] 模块编译通过（`biz-sign` / `biz-eoms` / `boot`）；建议启动后菜单权限再冒烟

---

### P4 · 拆除 legacy（P2+P3 完成后，约 2～3 天）

**目的**：删除过渡桥模块；业务不再依赖 `ls-aegis-legacy` / `com.itls.*`。

| 步骤 | 动作 | 状态 |
|------|------|------|
| 4.1 | 盘点 `ls-aegis-legacy` 仍被谁依赖 | **完成** |
| 4.2 | 鉴权桥仍经 `compat.SecurityUtils`（内部已桥 `UserContextHolder`）；业务无 `com.itls` | **完成**（进一步直连可后续） |
| 4.3 | 仍需的 Excel/`BaseEntity`/工具迁入 `ls-aegis-starter` 包 `com.ls.aegis.compat.*`；删除未用若依 `BaseController`/`PageUtils` | **完成** |
| 4.4 | eoms/sign/quote `pom` 去掉 `ls-aegis-legacy`；PageHelper 改由各 biz 直引；父 POM 移除模块 | **完成** |
| 4.5 | 删除 `ls-aegis-legacy` 目录 | **完成** |

**完成标准**

- [x] 仓库无 `ls-aegis-legacy` 模块；业务无 `com.itls.*` import
- [x] `mvn -pl ls-aegis-biz-sign,ls-aegis-biz-eoms,ls-aegis-biz-quote,ls-aegis-boot -am compile` 通过
- [ ] 建议人工：登录/鉴权/eoms/sign/psi 冒烟

**残留（非模块债务，属后续收敛）**：Sign / Quote / EOMS 已对齐 `XmlMapperDO`（C4）；Excel / 分页 / 幂等 / 字典 / 国密 / 看板已随 C2～C9 收敛。compat 系统实体仍 `BaseEntity`。

---

### P5 · 部署 SQL 单轨（可与 P1～P3 并行收尾，约 3～5 天）

**目的**：一份安装、一份真相源。

| 步骤 | 动作 | 状态 |
|------|------|------|
| 5.1 | 梳理 `sql/{psi,eoms,sign,quote}/`：哪些已在 Liquibase / `init.sql`，哪些是重复/历史 | **已完成** |
| 5.2 | 删除或归档重复副本（尤其 `sql/eoms/eoms/` 套娃目录） | **已完成**（冲突脚本已删除，仅留 `init.sql`） |
| 5.3 | 业务种子只保留「插件初始化」说明入口；**部署只认** `sql/init.sql` + Liquibase changelog | **已完成** |
| 5.4 | 已上线库增量：只加 `patch_*.sql` → 挂 `db.changelog-master.yaml` → 效果并回 `main_data` / `init.sql` | **已完成**（四域挂 master；效果已并回 `init.sql`） |
| 5.5 | 更新 INTEGRATION「SQL / 部署」章节，去掉「多目录各跑一遍」的暗示 | **已完成** |

**完成标准**

- [x] 全新环境仅执行统一流水线即可含 psi/eoms/sign/quote 菜单与基础种子
- [x] `sql` 下仅保留 `init.sql`，无第二套完整 init / 域拆分包

---

### P6 · H5 与 PC 鉴权/国密对齐（约 3～5 天）

**目的**：现场端是同一平台的客户端，不是第二套登录体系。

| 步骤 | 动作 | 状态 |
|------|------|------|
| 6.1 | H5 登录/刷新走平台鉴权接口；Token 头与 PC 一致 | **已完成**：工程师 `/auth/login` + `Authorization: Bearer`；客户仍 `h5-token`（业务临时令牌） |
| 6.2 | 国密：`gm.enable` 下传输加密与 PC 同策略（公钥接口 + `encryptTransport`） | **已完成**：`/auth/gm/public-key` + SM2（`sm-crypto`） |
| 6.3 | H5 API 全部走已迁 `R` 的后端；去掉临时兼容分支 | **已完成**：成功码 `code===0`；分页 `page/size` + `data.list` |
| 6.4 | 文档写明：H5 可用 Vant（移动端），但鉴权/接口/国密与 PC 同一平台 | **已完成**（见下） |

**说明（6.4）**

- UI：`ls-aegis-h5` 使用 **Vant**（移动端），与 PC Arco 并存合法——只是客户端壳不同。
- 鉴权：工程师与 PC 同一套 Sa-Token（`/auth/*`）；客户报修用编号换 `h5-token`（`@SaIgnore` + 拦截器），不是第二套账号体系。
- 国密 / 响应：与 PC 同策略（SM2 传输、`R` / `PageResp`）。
- 外观：读平台 `/system/common/dict/option/site`（SITE_*），不再依赖若依 `systemInfo`。

**完成标准**

- [x] H5 登录、报修主流程在国密开启下可走通（代码对齐；建议现场再冒烟）
- [x] INTEGRATION 收敛表「H5 鉴权/国密」标为已完成

---

### 并行与依赖（简表）

| 阶段 | 可并行 | 硬依赖 |
|------|--------|--------|
| P0 | — | 无 |
| P1 报价单轨 | 可与 P2.1（sign）并行 | P0 |
| P2 迁 R | sign ∥ 报价；eoms 建议在报价迁出后 | P0 |
| P3 改包名 | 可与同域 P2 同 PR 或紧随 | 建议该域 P2 完成后再大挪包，减少冲突 |
| P4 删 legacy | — | P2 + P3 完成 |
| P5 SQL | 全程可穿插 | 菜单/种子变更当时就要同步 |
| P6 H5 | 可与 P2.3 同做 | 至少 H5 相关接口已迁 `R` |

---

### 每阶段验收清单（通用）

1. **编译**：`ls-aegis-server` 全模块通过  
2. **前端**：对应 `apis` / `views` 无临时 adapter、无双菜单  
3. **联调**：登录 → 列表 → 写操作 → 权限拒绝  
4. **国密**：`gm.enable=true` 下关键写路径不回退到「仅明文调试」  
5. **部署**：种子变更已进 Liquibase，并反映到 `sql/init.sql`  
6. **文档**：更新本文件勾选 + INTEGRATION「收敛状态」表

---

### 刻意不做（防止走偏）

- 不为 eoms/sign 再起独立 Spring Boot 工程  
- 不引入第二套 PC UI 库 / 第二套 `AjaxResult` 兼容层「长期共存」  
- 不另起独立报价桌面/独立用户体系（在线报价只走平台 `views/quote`）  
- 不把报告、临时脚本当成部署 SQL 真相源  

---

### 状态看板（维护用）

| 阶段 | 状态 | 备注 |
|------|------|------|
| P0 冻结债务 | **已完成** | 评审门禁见 [§10](#10-平台一体--评审门禁) |
| P1 报价单轨 | **已完成** | 兼容 Controller/占位页已删；`init.sql` 含 40000 |
| P2 接口迁 R | **已完成** | P2.1～P2.3：sign + eoms PC/H5 全迁 `R` |
| P3 包名迁域 | **已完成** | sign/eoms → `com.ls.aegis.biz.*`；Controller 归入 `…eoms.controller` |
| P4 删 legacy | **已完成** | 模块已删；能力暂在 `com.ls.aegis.compat.*`（starter）；实体→`BaseDO` 可后续 |
| P5 SQL 单轨 | **已完成** | init + Liquibase 含四域；`sql` 仅留 `init.sql` |
| P6 H5 对齐 | **已完成** | 工程师 `/auth/login`+SM2；客户 `h5-token`；响应 `R`；SITE 外观 |

更新约定：完成某阶段后改本表状态，并同步 [§7 平台说明](#7-平台说明integration) 收敛状态。

进度记录：见本文 [§3 平台一体收敛](#3-平台一体收敛)。  
P0～P6 之后的合表/抽公共件：**整改范围**见 [§9 平台一体 · 整改清单](#9-平台一体--整改清单)（D/C/M/R 已全部结束）。

---

## 9. 平台一体 · 整改清单

> 盘点日期：2026-08-18  
> 原则：`.cursor/rules/平台一体.mdc`  
> 配套：[§7 平台说明](#7-平台说明integration) · [§8 平台一体 · 开发分步](#8-平台一体--开发分步) · [§10 平台一体 · 评审门禁](#10-平台一体--评审门禁)  
> 施工勾选：D/C/M/R **已全部结束**；结论见本文 [§3](#3-平台一体收敛)。

---

### 0. 一句话目标

仓库里只有 **一个平台**（LS・Aegis / ITLS）。新能力只进 `ls-aegis-biz-*` + `ls-aegis-web`（H5 仅移动端壳），由 `ls-aegis-boot` 挂载。

**只扩功能域，不扩第二套壳。**

| 四统一 | 唯一选型 | 示范 |
|---|---|---|
| 鉴权 | Sa-Token + 平台 RBAC + 国密（`ls-aegis-security` / `ls-aegis-crypto`） | `/auth/*` + SM2 |
| 接口 | 平台 `R` / `PageQuery` / `PageResp` | `biz-psi` |
| 前端 | Arco + `GiPageLayout` / `BaseTable` / `GiForm` | `views/psi`、`views/system/user` |
| 部署 | Liquibase + `ls-aegis-server/sql/init.sql` | 菜单段 psi `10001+` / eoms `20000+` / sign `30000+` / quote `40000+` |

功能域：`psi`（进销存）· `eoms`（报修/工单/结算）· `sign`（电子签章）· `quote`（报价）。Maven 多模块只是代码边界，不是独立系统。

---

### 1. 扫描结论（当前仓库）

壳层收敛 **P0～P6 已完成**，不再缺「第二套后台工程」。  
剩下的不是再砌一套平台，而是 **功能域里还有第二套主数据 / 第二套工具栈**。

| 维度 | 结论 |
|---|---|
| 启动入口 | 仅 `ls-aegis-boot`（`scanBasePackages=com.ls.aegis`） |
| 独立壳 | 无 `apps/`、无 `clients/`、无 `ls-aegis-legacy`、无 `com.itls.*` |
| PC UI | 仅 Arco；无 Element Plus / `<el-*>`；业务 `apis` 无若依 adapter |
| 接口形态 | Controller 已无 `AjaxResult` / `TableDataInfo`；`AjaxResult` 仅残留在 `compat.ExcelUtil` |
| 报价菜单 | 仅 40000 段；无 `/eoms/quote` 双轨 |
| SQL 目录 | `sql` 仅 `init.sql` |
| 未完成 | compat 工具栈、EOMS 与 PSI 主数据双轨、签章自建 MinIO |

---

### 2. 已达标（不要回潮）

下列项视为 **已整改完成**。后续 PR 只允许加固，不允许再开平行实现。

| 项 | 现状 |
|---|---|
| 一个 Boot | 四域由 `ls-aegis-boot` 扫描挂载 |
| 报价单轨 | `com.ls.aegis.biz.quote` + `views/quote`；菜单只留 40000/40001 |
| 接口迁 R | sign / eoms PC / H5 去掉 adapter |
| 包名迁域 | 业务包在 `com.ls.aegis.biz.*` |
| 删 legacy 模块 | 能力暂沉在 `ls-aegis-starter` 的 `com.ls.aegis.compat.*`（禁止扩大） |
| SQL 单轨目录 | 部署只认 Liquibase + `init.sql` |
| H5 鉴权对齐 | 工程师 `/auth/login` + SM2；客户 `h5-token`（外部客户，不是第二套员工登录） |
| PC 列表壳 | eoms / sign / quote 列表页已用 `GiPageLayout` + `BaseTable` |
| XML 分页 | eoms / quote / sign 走 `PageQueryHelper` + `PageQuery`/`PageResp`；禁止裸 `PageHelper.startPage` |
| 业务字典 | PC `useDict` / H5 `loadBizDicts`；禁止页面内写死「正常/停用」 |
| 国密传输 | H5 与 PC 同 SM2 C1C3C2 + `/auth/gm/public-key` |
| EOMS 看板 | 仪表盘 KPI；趋势/Top 只在统计分析 |

---

### 3. 待整改（按优先级）

判定：**同一现实对象只留一个真相源**。工单、结算、签章、报价单据本身 **不合进 PSI**。

#### G0 · 冻结（立刻，全程有效）

| ID | 动作 | 验收 |
|---|---|---|
| G0.1 | 新代码禁止新增 `AjaxResult` / `TableDataInfo` / 若依 `BaseController` / `com.itls.*` / 扩大 `compat.*` | 评审按 [§10 平台一体 · 评审门禁](#10-平台一体--评审门禁) |
| G0.2 | 新接口只进对应 `ls-aegis-biz-*`，由 boot 挂载 | 无第二套 `@SpringBootApplication` |
| G0.3 | 改菜单/字典/种子必须同步 Liquibase，并反映到 `init.sql` | 全新库跑一份 init 即可 |
| G0.4 | 新页只抄 psi：Arco + `GiPageLayout` / `BaseTable` / `GiForm` | 不引入第二套 UI 库 |

#### G1 · 抽公共件（不改业务表，先做）

对应施工任务 **C1～C9**。目的：去掉「每个域一套工具」，不是再写一套 compat。

| ID | 问题（现网） | 改成 | 不要做成 |
|---|---|---|---|
| G1.1 | 三套编号：`DocNoGenerator` / `EomsNoGenerator` / `QuoteNoAllocator`；EOMS 前缀读 `SysConfigServiceStub` 实际总是降级 | 平台 `DocNoService`（Redis 序列，前缀走 `sys_option`） | 不要在 eoms 再留一套 Redis 编号器 |
| G1.2 | 约 24 个 Controller `new ExcelUtil`；`ReportExportController` 手写 POI；`ExcelUtil` 仍返回 `AjaxResult` | EasyExcel + starter `ExcelUtils`；导出走 DTO | 不要继续给 `compat.ExcelUtil` 加功能 |
| G1.3 | 上传：PSI 有 `PsiFileField`；EOMS/H5 走本地盘 `ItlsConfig.getUploadPath()`；签章自建 `MinioConfig` | 升为平台文件组件 + `/system/common/file` | 不要再给 eoms 加一套附件中心 |
| G1.4 | Sign / Quote / EOMS 已迁 `XmlMapperDO`；`@Excel` 已随 G1.2 去掉。compat `SysUser` 等仍 `BaseEntity` | `BaseDO` / `TenantBaseDO` / XML 存量表 `XmlMapperDO` | 不要新实体再继承 `BaseEntity` |
| G1.5 | Quote/Sign/EOMS 已走 starter `PageQueryHelper` + `PageQuery`/`PageResp`（C5） | XML 域列表用 `PageQueryHelper.page`，导出用 `startExportLimit`；PSI 仍 `PageResp.build` | 不要新接口直接 `PageHelper.startPage` |
| G1.6 | PSI / EOMS 已共用 starter `IdempotentHelper`（C6） | PSI `execute` 回放首次结果；EOMS 收款 `runOnce` | 不要再写第二套 Redis token |
| G1.7 | PSI / EOMS / Sign / H5 已走 `useDict` / `/system/common/dict`（C7） | 改 `sys_dict` 即改页面 | 不要页面内写死「正常/停用」当唯一源 |
| G1.8 | H5 / PC 均为 `sm-crypto` 0.5.x；SM2 C1C3C2 + `/auth/gm/public-key`（C8） | `gm.enable=true` 下工程师 H5 登录与 PC 同协议 | 不要 H5 另做一套加密协议 |
| G1.9 | 仪表盘只 KPI；趋势/物料 Top 只在统计分析（C9） | 同一图表不出现两次 | 不要再加第三块看板 |

#### G2 · 基础设施合表（主数据之前）

对应 **M1**。平台已有配置 / 文件 / 操作日志，EOMS 不应再养一套。

| ID | 去掉 | 留下 | 菜单 |
|---|---|---|---|
| G2.1 | 表 `eoms_config`、`SysConfigServiceStub` | `sys_option`（category = `eoms`） | 下线 **20060** `/eoms/config/param` |
| G2.2 | 表 `eoms_attachment`、本地盘路径、`ItlsConfig` 业务上传 | `sys_file` + `/system/common/file`；工单存 `fileId` | 下线 **20061** |
| G2.3 | sign 独立 `MinioConfig` / `MinioClient` | 平台存储（bucket 可仍名 `esign`）；`/sign/file/upload` 对内转发 | 签署流程仍可用 |
| G2.4 | 表 `eoms_operation_log`、compat `@Log` | 平台 `sys_log` + 监控「操作日志」 | 下线 **20062**（`sign_log` / `sign_record` 不合） |

#### G3 · 往来单位 + 仓库（高风险）

对应 **M2**。侧栏现在同时有进销存往来单位 **和** EOMS 客户/供应商/仓库，这是典型「第二套壳」。

| ID | 去掉（作为独立档案） | 留下 | 菜单 |
|---|---|---|---|
| G3.1 | `eoms_customer` / `eoms_customer_contact` / `eoms_supplier` | `psi_unit`（及 contact/address/invoice/pay） | 下线 **20001 / 20031**，跳转 `/psi/unit` |
| G3.2 | `eoms_warehouse`（代码与菜单在，**`init.sql` / Liquibase 无 DDL**） | `psi_warehouse` | 下线 **20030**，跳转 `/psi/warehouse` |
| G3.3 | 工单/合同/报价/收款上的客户 ID 指向 eoms 客户表 | 外键改 `psi_unit.id`；`QuoteMapper` JOIN `psi_unit` | 报价/工单客户名来自往来单位 |
| G3.4 | H5 `/eoms/h5/auth/token` 读 eoms 客户编号 | 按 `psi_unit.unit_code` 换令牌 | **仍用 h5-token**，不合进 `sys_user` |

**部署注意：** `eoms_warehouse` / `eoms_supplier` / `eoms_inbound` 有 Java、菜单、Mapper，但统一 SQL **没有建表**。  
整改时 **不要** 把这三张「第二套表」补进 `init.sql` 当长期能力；已上线库若缺表，用一次性 patch 迁数据或补齐后再归档。全新库只建 PSI 主数据。

#### G4 · 物料 + 库存（最重，单独一轮）

对应 **M3**。默认假设：维修备件与进销存商品是同一 SKU、同一仓库数量。

| ID | 去掉（作为独立库存账） | 留下 |
|---|---|---|
| G4.1 | `eoms_material*`、`eoms_material_stock`、`eoms_material_record` | `psi_goods` / `psi_goods_category` / `psi_stock_*` |
| G4.2 | EOMS「直接改数量」 | 结算出库记 PSI `stock-out`（类型可 REPAIR） |
| G4.3 | EOMS 采购/入库页（`eoms_purchase` / `eoms_inbound`） | 只走 `/psi/stock-in`（或采购合同） |
| G4.4 | 菜单 **20020～20023** | 侧栏无第二套物料维护 |

若业务确认「备件库永远独立」：不合 `eoms_material`，但仓库仍必须用 G3 的 `psi_warehouse`，且禁止手改库存。该决策必须写进施工清单回写栏，禁止默默保留双库存。

#### G5 · 单据不合表，只改引用和名字

对应 **R1～R4**。

| ID | 表 | 动作 |
|---|---|---|
| G5.1 | `eoms_payment` / `eoms_receivable` | **留表**。页面与 API 改名为维修收款/维修应收，避免与 PSI `/psi/payment`（付款）撞名 |
| G5.2 | `eoms_contract` vs `psi_contract_*` | **不合**。维修合同外键绑 `psi_unit`（依赖 G3） |
| G5.3 | `eoms_quote` | 已是唯一报价表。列表接明细编辑（替代已删的 editor/history/settings） |
| G5.4 | `sign_*` | 业务表保留。存储走 G2.3。不读写 PSI/EOMS 合同表 |

---

### 4. 红线：不扩第二套壳

出现下列任一行为，视为整改失败（即使功能能跑）。

| 禁止 | 说明 |
|---|---|
| 再引入 `ls-aegis-legacy` 或 `com.itls.*` | 已清零，禁止复活 |
| 扩大 `com.ls.aegis.compat.*` 新能力 | 只减不增；新工具进 starter 正式包或 psi 已有能力上提 |
| 同一业务双菜单 / 双 API | 例：再加一套报价、再加一套客户维护 |
| 前端若依 adapter、第二套 UI 库 | PC 禁止 Element Plus；H5 允许 Vant（移动端壳） |
| 复活可独立启动的 RuoYi admin / `apps/` / 桌面在线同步 | 在线报价只走 `views/quote` |
| 为 eoms/sign 再起独立 Spring Boot | 模块可保留，进程只有 boot |
| `sql` 再拆第二套完整 init | 部署只认一份 `init.sql` |
| 把缺失的 `eoms_warehouse` 等补成「长期第二套主数据」 | 合到 PSI，而不是补齐双轨 DDL |

#### 刻意保留（不要当整改项删掉）

| 项 | 原因 |
|---|---|
| 客户 `h5-token` | 外部客户不是员工；G3.4 只换读取源 |
| H5 用 Vant | 允许的移动端壳；鉴权/接口/国密仍走平台 |
| 签章项目/印章/证书表 | 不是合同台账 |
| 平台 `/dashboard` vs 业务 KPI | 主题不同 |
| PSI 调拨/发票/核销 vs 工单结算 | 账不同 |
| `security.base.controller.BaseController` | 平台 CRUD 基类，不是若依 `BaseController` |

---

### 5. 执行顺序

不要并行 G3 与 G4。合表前先抽公共件，避免迁表时还在复制旧工具。

```
G0 冻结（已生效，持续）
  → G1 抽公共件（C）
  → G2 配置/文件/日志合表（M1）
  → G3 客户供应商仓库（M2）
  → G4 物料库存（M3）
  → G5 单据改名与在线编辑（R）
```

每轮完成标准：

1. `mvn -pl ls-aegis-boot -am compile` 通过  
2. 对应 `apis` / `views` 无临时 adapter、无双菜单  
3. 联调：登录 → 列表 → 写操作 → 权限拒绝  
4. `gm.enable=true` 下关键写路径不回退明文调试  
5. 种子变更已进 Liquibase，并反映到 `init.sql`  
6. 若有新一轮专项整改，在本 `Reports-README.md` 对应章节追加，勿再拆分子目录报告  

建议冒烟：PC 登录、进销存往来单位、EOMS 工单、报价列表、H5 工程师登录、H5 客户编号报修、签章上传。

---

### 6. 文档怎么用

| 章节 | 角色 |
|---|---|
| **本文 §9** | 整改范围与红线（为什么改、改到什么算一个平台） |
| [§8](#8-平台一体--开发分步) | P0～P6 壳层收敛（已完成，勿重开阶段） |
| [§10](#10-平台一体--评审门禁) | 每个 PR 必问 5 条 |
| [§7](#7-平台说明integration) | 平台说明与收敛状态总表 |
| [§3](#3-平台一体收敛) | P0～P6 及后续合表完成记录 |

---

## 10. 平台一体 · 评审门禁

配套：[§8 平台一体 · 开发分步](#8-平台一体--开发分步)、[§9 平台一体 · 整改清单](#9-平台一体--整改清单)、`.cursor/rules/平台一体.mdc`。

### 合并前必问

1. 是否新增了 `AjaxResult` / `TableDataInfo` / 若依 `BaseController`？→ **否**
2. 是否新增了 `com.itls.*` 类或扩大对 `ls-aegis-legacy` 的依赖？→ **否**
3. 新接口是否落在对应 `ls-aegis-biz-*`，并由 `ls-aegis-boot` 挂载？→ **是**
4. 前端是否走 Arco + `GiPageLayout` / `BaseTable` / `GiForm`，且未新增若依 adapter？→ **是**
5. 菜单/字典/种子变更是否已同步 Liquibase，并反映到 `sql/init.sql`？→ **是**（若改了种子）

### 本地扫描

按上方 5 条人工核对即可（P4 后 `ls-aegis-legacy` / `com.itls` 已清零）。不再提供仓库内扫描脚本。

额外：业务模块（`ls-aegis-biz-eoms` / `quote` / `sign` / `psi` / `rbac`）禁止 `new ExcelUtil`。导出须走 `com.ls.aegis.starter.support.excel.ExcelExportHelper` + DTO。`compat.ExcelUtil` 仅存量类本身可保留。

业务模块禁止新增实体继承 `compat.BaseEntity`。新实体用 `BaseDO` / `TenantBaseDO`；XML 存量表用 `XmlMapperDO`。

业务模块（`ls-aegis-biz-eoms` / `quote` / `sign`）禁止直接 `PageHelper.startPage`。XML Mapper 列表走 `PageQueryHelper.page`，导出走 `startExportLimit`；MyBatis-Plus 域（如 PSI）继续 `PageResp.build`。

业务页禁止把「正常/停用」等状态文案写死在页面里当唯一源。展示/筛选项走 `useDict` / `/system/common/dict`。

H5 与 PC 国密传输必须同协议：`sm-crypto` 0.5.x、SM2 C1C3C2、密文补 `04`、公钥 `/auth/gm/public-key`。

### 刻意允许（过渡期）

- ~~`ls-aegis-legacy`~~ **已删除（P4）**；存量工具在 `com.ls.aegis.compat.*`（`ls-aegis-starter`），**禁止扩大**
- Sign / Quote / EOMS 已对齐 `XmlMapperDO`（C4）；Excel 已走 EasyExcel（C2）；分页已走 `PageQueryHelper`（C5）；幂等已走 `IdempotentHelper`（C6）；字典已走 `useDict`（C7）；H5/PC 国密已对齐（C8）；EOMS 看板已拆 KPI/统计（C9）。compat 系统实体仍可 `BaseEntity`，业务模块禁止新增

---

## 11. 已清理的历史分册

| 原文件 | 原因 |
|---|---|
| 原 `reports/` 主题子目录分册与旧 README | 已并入本文件 §1～§6；目录现名 `Docs/` |
| `docs/INTEGRATION.md` | 已并入 §7 |
| `docs/平台一体-开发分步.md` | 已并入 §8 |
| `docs/平台一体-整改.md` | 已并入 §9 |
| `docs/平台一体-评审门禁.md` | 已并入 §10 |
| `前端/前端表格规范差异报告.md` 等 | 见历史清理记录；差异已消除或施工已结束 |

> 根目录 `docs/`、`reports/` 已停用（现统一为 `Docs/`）。部署 SQL **仍只认** `ls-aegis-server/sql/init.sql`（与本报告无关）。

---

## 12. 电子签章 · 发起签章页后续扩展方案

| 项 | 内容 |
|---|---|
| 文档日期 | 2026-08-19 |
| 页面路径 | `/sign/execute`（菜单：签署执行 / 发起签章） |
| 前端入口 | `ls-aegis-web/src/views/sign/execute/` |
| 后端入口 | `ls-aegis-biz-sign` → `SignExecuteController` / `UnifiedSignServiceImpl` |
| 状态 | **方案落盘**；当日未实施，供后续迭代按优先级拆分任务 |
| 关联规范 | 平台一体（统一 `R` 响应、Arco + `GiPageLayout`）、国密（传输 SM2 / 摘 SM3）、SQL 规范（若改菜单/字典须同步 `init.sql`） |

### 12.1 当前已落地能力（基线）

以下为 2026-08-19 前已在工作区实现、可作为后续扩展起点的能力摘要。

| 模块 | 已实现 | 说明 |
|---|---|---|
| 页面布局 | ✅ | 左栏流程 + 项目 + 放置模式 + 印章列表；右栏 PDF 预览 + 待签章任务 + 底栏提交 |
| PDF 渲染 | ✅ | `pdfjs-dist` + `SignPdfPlacer.vue` 单页 canvas 预览 |
| 普通章 | ✅ | 单页拖放 / 点击定位 / 等比缩放；坐标 pt、原点页面左上角 |
| 批量章 | ✅ | 连续范围 / 全部页 / 奇数页 / 偶数页；前端 `expandBatchPlacements` 展开为多页 `positions` |
| 骑缝章 | ✅ | 起止页 + 尺寸 mm + 右侧 Y 拖动；对接 `spanSeals` |
| 统一提交 | ✅ | `unifiedSign`：`generalSeals` + `spanSeals`；同印章多位置按 `sealId` 合并 |
| 任务互转 | ✅ | 普通章 ↔ 批量章（扩展 / 改单页） |
| 页码条 | ✅ | ≤40 页时工具栏紫色点阵标示批量覆盖页 |

**刻意未做 / 半成品（本方案覆盖）**

| 项 | 现状 |
|---|---|
| `prepareSign` 两阶段校验 | API 存在（`/sign/execute/prepare`），前端**未调用** |
| `runSign` 非 unified 模式 | API 存在，前端已统一走 `unifiedSign` |
| 非连续页码批量 | 仅 range/all/odd/even，不支持 `1,3,5` 或「每 N 页」 |
| 签章前预览合成效果 | 仅静态 overlay，非真实 PDF 渲染结果 |
| 大 PDF（>100 页）性能 | 批量展开受后端 `positions` 单组上限 100 约束，前端无分批策略 |
| 移动端 | `GiPageLayout` 左栏可折叠，拖放体验未专门优化 |
| 业务 composable | 逻辑内联在 `index.vue`，未抽 `useSignExecute` |

---

### 12.2 目标与原则

1. **不破坏平台一体**：继续单页工作台范式；不引入第二套 UI / 响应约定；列表历史仍走「签署记录」。
2. **前后端契约清晰**：前端任务模型（`PendingSealTask`）与提交 VO（`UnifiedSignRequest`）保持可映射；扩展时优先扩 VO / 校验，避免「前端能配、后端 silently skip」。
3. **默认可关闭**：新能力（时间戳、Append 模式、模板坐标等）均需配置或显式开关，兼容 `gm.enable=false` 与非国密环境。
4. **安全与权限**：提交前校验项目成员、印章授权、文件归属；`prepareSign` 应成为正式门禁，不仅靠前端 disabled。
5. **可观测**：大批量签章需进度与失败页回显；记录 `positions` JSON 便于审计（后端已写 `SignRecord.positions`）。

---

### 12.3 后端能力边界（扩展设计须对齐）

| 能力 | 后端现状 | 对前端约束 |
|---|---|---|
| 合并签章 | `POST /sign/execute/unified` → `UnifiedSignRequest` | 主入口；`generalSeals` / `spanSeals` 可并存 |
| 普通章 | `GeneralSealConfig`：`mode=single\|fixed\|custom`；`positions` **最多 100** / 每组 | 批量章展开时须监控总数；超限时需分组多次 `generalSeals` 项或后端提限 |
| 骑缝章 | `SpanSealConfig`：`sealSize` 实际按 **mm→pt** 换算（`SpanSealRenderTask`）；`xPosition` 目前固定右缘，前端传 0 | 若做「左缘骑缝」需改 `SpanSealRenderTask` |
| 数字签名 | `DigitalSignTask`：主印章第一个普通章首位置或默认右下角；跳过与主章同 seal 的第一处渲染 | 多印章 + 批量时须理解「外观章 vs 数字签名外观」关系 |
| 准备校验 | `prepareSign(SignRequest)`：模式白名单、权限、文件；**不是** `UnifiedSignRequest` | 需新增 `prepareUnified` 或扩展 prepare 入参，否则前端只能 client-side 校验 |
| 旧模式 | `runSign` + `signMode`（single/fixed/custom/span/multi/continuous） | 项目/合同若配置了「定页模板」，可与此链路对接 |
| 时间戳 | `UnifiedSignRequest.hasTimestamp` 字段存在 | 前端未暴露；需 UI + 证书能力确认 |
| 追加签章 | `UnifiedSignRequest.appendMode` | 对已签 PDF 再签；需文件来源与验签策略 |

---

### 12.4 扩展项总览（按优先级）

| 优先级 | 编号 | 扩展项 | 用户价值 | 复杂度 |
|---|---|---|---|---|
| P0 | E-01 | 非连续页码批量 | 合同常指定「第 1、3、5 页」 | 中 |
| P0 | E-02 | `prepareUnified` 提交前校验 | 权限/页码/证书失败提前提示 | 中 |
| P0 | E-03 | 超 100 处定位拆分与提示 | 避免提交后 400 / 静默丢章 | 低 |
| P1 | E-04 | 指定页码多选 UI | 页码条点击多选、导入列表 | 中 |
| P1 | E-05 | 每 N 页 / 自定义规则 | 「每 2 页盖一次」 | 中 |
| P1 | E-06 | 项目/合同预设坐标 | 选项目后带出模板位置 | 高 |
| P1 | E-07 | 签章前服务端预览（可选） | 高保真确认 | 高 |
| P1 | E-08 | 提交进度与失败页回显 | 大文档体验 | 中 |
| P2 | E-09 | 骑缝章左/右/上下边 | 装订线场景 | 中 |
| P2 | E-10 | 时间戳开关 | 合规 | 低 |
| P2 | E-11 | Append 再签模式 | 多轮签署 | 高 |
| P2 | E-12 | 任务模板保存/复用 | 重复业务 | 中 |
| P2 | E-13 | 撤销/重做 | 编辑体验 | 中 |
| P2 | E-14 | 多 PDF 批量发起 | 运营效率 | 高 |
| P3 | E-15 | composable 与单测 | 可维护性 | 中 |
| P3 | E-16 | 移动端拖放优化 | 平板现场签署 | 中 |
| P3 | E-17 | 无障碍与键盘操作 | 信创终端 | 低 |
| P3 | E-18 | 大 PDF 虚拟滚动缩略图 | >40 页导航 | 高 |

---

### 12.5 分项设计

#### E-01 / E-04 / E-05 页码策略增强（批量章）

**需求**

- 在现有 `range | all | odd | even` 之外，增加：
  - `custom`：用户点选页码集合，如 `{1,3,5,8}`
  - `step`：每 N 页，如 start=1,end=20,step=2 → 1,3,5…
  - （可选）`last`：仅最后一页；`firstAndLast`：首页+末页

**前端**

```ts
// types.ts 扩展示意
export type BatchPageMode = 'range' | 'all' | 'odd' | 'even' | 'custom' | 'step'

export interface BatchPageRange {
  startPage: number
  endPage: number
  mode?: BatchPageMode
  customPages?: number[]   // mode=custom
  step?: number            // mode=step，默认 2
}
```

- `SignPdfPlacer` 页码条：支持 **Ctrl/Shift 多选**；>40 页改用「页码列表输入框」（如 `1,3,5-8,12` 解析器）。
- 任务摘要：`共 N 页 · 自定义` / `每 2 页`.

**后端**

- 无需改 VO：仍展开为 `List<Position>`；须校验 **展开后 ≤100**（见 E-03）。
- `SignRecord.positions` 建议存 `mode + customPages`，便于回显。

**验收**

- 20 页 PDF 选 custom `1,3,5` 提交后，验 PDF 仅 3 处有章；签署记录 positions JSON 可还原。

---

#### E-02 提交前统一校验（prepareUnified）

**问题**：现前端仅 `canSubmit` 本地判断；`prepareSign` 面向 `SignRequest`，与 `UnifiedSignRequest` 不一致。

**方案 A（推荐）**

新增 `POST /sign/execute/prepare-unified`，入参同 `UnifiedSignRequest`，返回：

```json
{
  "ok": true,
  "warnings": ["印章 A 证书 30 天内过期"],
  "pageCount": 25,
  "totalStampCount": 13,
  "errors": []
}
```

校验项：项目权限、印章权限、文件可读、页码范围、positions 数量、证书存在性、PDF 是否加密。

**方案 B（过渡）**

前端提交前调用现有 `getSeal` / `getSignFileInfo` 聚合校验；**不能替代**服务端权威校验。

**前端**

- `onSubmit` 先 `prepareUnified` → 有 `warnings` 弹 `Modal.confirm` → 再 `unifiedSign`。
- 失败时将 errors 映射到任务行（印章名 + 页码）。

**验收**

- 无权限印章点击提交 → prepare 阶段拦截，不触发 120s 签章超时。

---

#### E-03 单组 100 处上限与拆分策略

**约束**：`GeneralSealConfig.positions` `@Size(max=100)`。

**前端策略**

1. `buildGeneralSeals` 后检测每组 `positions.length`；
2. 若某 `sealId` 展开 >100：
   - **策略 1**：自动拆成同一 `generalSeals` 数组内多条相同 `sealId` 记录（需确认后端是否允许多条同 sealId — 当前 `SealImageLoadTask` 用 Set 去重，**渲染支持**）；
   - **策略 2**：阻断提交并提示「请减少批量页数或拆分两次签章」；
3. 侧栏实时显示：`将产生 120 处盖章，超过单次上限 100`.

**后端（可选增强）**

- 将上限改为可配置 `sign.max-positions-per-request`；
- 或在 `UnifiedSignServiceImpl` 内自动分批渲染（对前端透明）。

**验收**

- 150 页「全部页」批量：要么自动拆分成功，要么明确错误文案。

---

#### E-06 项目/合同预设坐标

**需求**：项目管理已绑定印章；扩展「默认签章位置模板」（如合同盖骑缝 + 末页公章）。

**数据**

- 方案 1：`sign_project` 增 JSON 字段 `default_layout`（Liquibase + `init.sql` 同步）；
- 方案 2：字典 / 参数表 `sign_layout_template`。

**交互**

- 用户选项目后，提示「加载项目模板」→ 自动生成 `pendingTasks`（可删改）。
- 模板项：`{ type, sealId, batchPages, placement, spanConfig }`。

**后端**

- 项目详情 API 返回模板；**不强制**模板与 unified 耦合。

**验收**

- 选项目 A 自动带出 2 个任务；换项目 B 提示是否覆盖当前任务。

---

#### E-07 签章前高保真预览

**需求**：overlay 与真实合成存在字体/透明度差异。

**方案**

- 轻量：`POST /sign/execute/preview-unified` 返回临时 PDF（MinIO / 短期 fileId），前端 `FilePreview` 打开；**不写入 SignRecord**。
- 重量：前端 wasm/pdf-lib 本地合成 — 不推荐（与国密、证书链不一致）。

**注意**

- 预览须走权限校验；临时文件 TTL + 定时清理；
- 预览与正式签章间隔内源 PDF 不变更（fileId 锁）。

---

#### E-08 提交进度与失败回显

**需求**：100+ 页 unified 签章可能 30～120s。

**前端**

- `unifiedSign` 等待期间展示步骤：`加载印章 → 渲染普通章 → 渲染骑缝 → 数字签名 → 保存`（可先假进度，后续接 SSE）。
- 失败时展示 `Message` + 任务面板标红；若后端返回部分成功页码，支持「重试失败页」。

**后端（可选）**

- 异步任务 + `GET /sign/execute/job/{id}` 查询进度（与 SnailJob 域对齐时复用调度模块）。

---

#### E-09 骑缝章位置扩展

**现状**：`SpanSealRenderTask` 固定 `xOffset = docWidth - sealWidth`（右缘）。

**扩展**

- `SpanSealConfig` 增 `edge: left|right|top|bottom`（或复用 `xPosition` 语义文档化）；
- 前端骑缝模式增「装订边」选择；拖动方向随 edge 变化（左右缘 → 上下拖；上下缘 → 左右拖）。

**兼容**

- 默认 `right`，旧数据不受影响。

---

#### E-10 / E-11 时间戳与追加签章

| 字段 | UI | 说明 |
|---|---|---|
| `hasTimestamp` | 提交前 Checkbox「加盖时间戳」 | 确认 `DigitalSignTask` / TSA 能力是否启用 |
| `appendMode` | 高级选项「在已有签章 PDF 上追加」 | 仅允许选已签文件或上传已签 PDF；须接验签 |

**风险**

- Append 模式下指纹链、回单校验逻辑更复杂；须与「验真核验」页联动测试。

---

#### E-12 任务模板本地/服务端保存

**本地**：`localStorage` 存最近 5 套 `pendingTasks` + `placementMode`（不含 fileId）。

**服务端**（可选）：用户级「我的签章方案」表；字段：名称、projectId、tasks JSON。

**菜单**：不新增顶级菜单；在发起签章页「新建签章」旁增加「加载方案」下拉。

---

#### E-13 撤销 / 重做

- 维护 `tasksHistory: PendingSealTask[][]` + 指针；
- 拖放 / 增删 / 改页码范围时 `pushSnapshot()`；
- 快捷键 Ctrl+Z / Ctrl+Y（桌面端）。

---

#### E-14 多 PDF 批量发起

**流程**：上传多个 PDF → 共享同一套 tasks 或每文件独立 tasks → 队列调用 `unifiedSign`。

**实现**

- 第一版：串行提交 + 结果表格（文件名、recordId、失败原因）；
- 第二版：后端 batch API（事务策略：部分失败是否回滚）。

---

#### E-15 代码结构 refactor

建议拆分：

```
views/sign/execute/
  index.vue              # 布局与编排
  composables/
    useSignExecute.ts    # 项目/印章/任务/提交
    useSignTasks.ts      # 增删改/互转/expand
    useSealImages.ts     # 图片 ObjectURL 生命周期
  components/
    SignPdfPlacer.vue
    SignTaskList.vue     # 待签章任务面板（可选拆）
  types.ts
  utils/
    pageRangeParser.ts   # "1,3,5-8" 解析
apis/sign/execute.ts     # 补充 UnifiedSignRequest TS 类型
```

**单测重点**：`resolveBatchPages`、`expandBatchPlacements`、`buildGeneralSeals` 分组与 100 上限。

---

#### E-16 / E-17 / E-18 体验与性能

| 项 | 做法 |
|---|---|
| 移动端 | 拖放改「长按进入拖动」；增大 hit area；骑缝仅显示当前页简化条 |
| 键盘 | 方向键微调 1pt；Tab 切换任务；Enter 提交 |
| 大 PDF | 页码条 >40 改用虚拟列表；PDF 页 lazy render（仅渲染 current±1）；批量 overlay 非当前页不算 marker DOM |
| 无障碍 | marker 加 `aria-label`；任务列表与预览联动 `aria-selected` |

---

### 12.6 推荐实施分期

#### 第一期（1～2 天，可独立上线）

| 任务 | 编号 |
|---|---|
| 自定义页码 + step 批量 | E-01, E-04, E-05 |
| 100 处上限检测与拆分/阻断 | E-03 |
| `prepareUnified` 后端 + 前端接入 | E-02 |
| `execute.ts` 补 TypeScript 类型 | E-15 部分 |

#### 第二期（3～5 天，业务增强）

| 任务 | 编号 |
|---|---|
| 项目预设坐标模板 | E-06 |
| 提交进度 UX + 错误映射 | E-08 |
| 骑缝边位 | E-09 |
| 任务方案本地保存 | E-12 |
| composable 拆分 | E-15 |

#### 第三期（按需）

| 任务 | 编号 |
|---|---|
| 服务端预览 PDF | E-07 |
| 时间戳 / Append | E-10, E-11 |
| 多 PDF 队列 | E-14 |
| 大 PDF 性能与 a11y | E-16～E-18 |

---

### 12.7 测试计划（扩展完成后）

| 类别 | 用例 |
|---|---|
| 功能 | 普通/批量/骑缝混排；odd/even/custom/step；普通↔批量互转 |
| 边界 | 0 页 tasks；单页 PDF；100/101 处批量；加密 PDF；无证书印章 |
| 权限 | 非项目成员、无印章授权、他人 fileId |
| 视觉 | 不同 page size（A4/Letter）；缩放后坐标与提交一致 |
| 回归 | 统一签章后「签署记录」下载；「验真核验」指纹通过 |
| 性能 | 50 页 batch 提交耗时 < 阈值（定义 SLA，如 60s P95） |
| 信创 | 麒麟 + 国产浏览器下 pdfjs worker 正常 |

---

### 12.8 风险与依赖

| 风险 | 缓解 |
|---|---|
| positions 超限 silently 截断 | E-03 必做；prepare 返回 count |
| 数字签名外观与视觉章不一致 | 文档说明；UI 标注「首个普通章位置用于签名外观」 |
| 大 PDF 浏览器 OOM | lazy render + 限制单次页数 |
| prepare / unified 双轨维护 | 以 unified 为主，prepare 对齐后废弃 SignRequest 单页入口 |
| 改菜单/字典 | 若增「签章方案」菜单须走 SQL 规范同步 `init.sql` |

---

### 12.9 不在本期范围（明确排除）

- 手写签名板 / 图片自由旋转任意角度（需后端渲染支持）
- 多人会签流转（属 OA/合同域，非 execute 页）
- 独立 H5 发起签章（若要做须单独 PRD，复用 apis）
- 复活 `ls-aegis-legacy` 或若依双轨 UI

---

### 12.10 变更记录

| 日期 | 说明 |
|---|---|
| 2026-08-19 | 初版：基于发起签章页已实现的普通/批量/骑缝能力，整理 P0～P3 扩展方案与分期建议 |

---

## 13. RBAC 权限管理检查与修复方案

| 项 | 内容 |
|---|---|
| 版本 | 工作区当前态（未单独升版） |
| 检查日期 | 2026-08-19 |
| 范围 | `ls-aegis-security`、`ls-aegis-rbac`、`ls-aegis-mybatis`、各 `ls-aegis-biz-*`、前端 `ls-aegis-web` |
| 方法 | 全库静态检索（Sa-Token / `@SaCheckPermission` / 菜单种子 / `v-permission`）+ 关键链路人工核对 |
| 总结论 | **后端 RBAC 架构完整可用**；主要缺口为前端角色码历史遗留、EOMS 菜单按钮种子缺失、前后端权限码不一致、数据权限仅 PSI 接入 |
| 修复顺序 | **阶段一**（前端基础）→ **阶段二**（EOMS 三端对齐）→ **阶段三**（EOMS/Sign DataScope 分期） |

---

### 13.1 架构总览（检查结论）

平台采用 **Sa-Token + RBAC（用户-角色-菜单/权限码）** 统一鉴权：

```
┌─────────────────────────────────────────────────────────────┐
│  ls-aegis-web（Vue3 + Pinia）                                │
│  路由守卫 → getUserInfo / getUserRoute → v-permission 指令    │
└──────────────────────────┬──────────────────────────────────┘
                           │ Bearer Token
┌──────────────────────────▼──────────────────────────────────┐
│  ls-aegis-security                                           │
│  SaExtensionInterceptor / SaTokenPermissionImpl / BaseController│
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│  ls-aegis-rbac（用户/角色/菜单/登录/路由树）                   │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│  ls-aegis-mybatis（DefaultDataPermissionUserDataProvider）    │
└─────────────────────────────────────────────────────────────┘
```

| 模块 | 职责 |
|---|---|
| `ls-aegis-rbac` | 用户/角色/菜单/部门 CRUD、登录认证、路由树构建 |
| `ls-aegis-security` | Sa-Token 拦截器、权限提供者、CRUD 自动鉴权 |
| `ls-aegis-common` | `UserContext` / `RoleContext`、内置角色枚举 |
| `ls-aegis-mybatis` | 数据权限（DataScope）SQL 改写 |
| `ls-aegis-web` | 路由守卫、动态路由、`v-permission` 按钮控制 |

**数据模型 ER**：`sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu`；自定义数据范围经 `sys_role_dept`。

**内置角色**（`RoleCodeEnum`）：`super_admin`（超管）、`admin`（租户管理员）、`sys_admin`、`general`。

**权限校验链路**：登录并行加载 permissions + roles → 写入 Sa-Token Session → 请求经 `SaExtensionInterceptor` → `@SaCheckPermission` / `BaseController` 自动鉴权 → 角色变更时 `RoleServiceImpl.updateUserContext()` 热更新在线用户。

---

### 13.2 检查发现汇总

| 维度 | 评分 | 说明 |
|---|---|---|
| 架构设计 | ✅ 良好 | 统一 Sa-Token + 菜单驱动权限码 |
| 后端鉴权 | ✅ 完整 | 登录拦截、注解鉴权、CRUD 自动鉴权、超管通配、权限热更新 |
| 前端控制 | ✅ 基本完整 | 路由后端过滤 + `v-permission`（约 70+ 页面） |
| 数据权限 | ⚠️ 部分 | 仅 PSI 模块 Mapper 接入 `@DataPermission` |
| 一致性 | ⚠️ 待改进 | 权限码命名、前端角色码、EOMS 菜单种子 |

| # | 问题 | 严重度 | 现状 |
|---|---|---|---|
| P1 | 前端超管角色码不一致 | 中 | 后端 `super_admin`，前端 `hasRole.ts`/`has.ts` 写死 `role_admin`；全项目零 `v-hasRole` 引用，实际影响有限 |
| P2 | EOMS 菜单按钮种子缺失 | **高** | `patch_eoms_menu.sql` 仅有页面菜单 + 工单按钮（20111–20120）；非超管缺 `eoms:repair:add` 等 → 接口 403 |
| P3 | EOMS 前后端权限码不一致 | **高** | 如前端 `eoms:repair:toOrder` vs 后端 `eoms:repair:assign`；看板 `view` vs `list` |
| P4 | 权限标识命名不统一 | 低 | PSI/EOMS 三段式 vs Quote 两段式 |
| P5 | 前端权限匹配弱于后端 | 中 | 后端 `simpleMatch` 通配，前端仅精确匹配 + `*:*:*` |
| P6 | permissions 持久化 localStorage | 中 | 角色变更后按钮状态可能过期（后端仍拦截） |
| P7 | 数据权限覆盖不均 | 中 | EOMS/Sign/Quote 无 DataScope |
| P8 | Sign 双重权限模型 | 低 | 平台 RBAC + `sign_seal_permission` 印章 ACL 并存 |
| P9 | 全项目零 `@SaCheckRole` | 信息 | 角色校验全靠 permission 码，可维持 |

---

### 13.3 修复方案总览

按 **1 → 2 → 3** 顺序实施，每阶段可独立验收：

| 阶段 | 目标 | 工期 | 风险 | 可独立上线 |
|---|---|---|---|---|
| **阶段一** | 前端角色码与权限基础设施 | 0.5～1 天 | 低 | ✅ |
| **阶段二** | EOMS 权限清单三端对齐 | 2～3 天 | 中（SQL 种子） | ✅ |
| **阶段三** | EOMS/Sign DataScope 分期改造 | EOMS 1～2 周；Sign 3～5 天 | 高 | 分期 |

---

### 13.4 阶段一：前端角色码与权限基础设施修复

**目标**：消除前后端角色码不一致，补齐前端权限能力，降低后续 EOMS 对齐误报。

#### 13.4.1 修复超管角色码（P0）

| 现状 | 目标 |
|---|---|
| 后端 `RoleCodeEnum.SUPER_ADMIN` = `super_admin` | 前后端统一 |
| 前端 `hasRole.ts` / `has.ts` 写死 `role_admin` | 改为 `super_admin` |

**改动文件**：

| 文件 | 动作 |
|---|---|
| `ls-aegis-web/src/constant/role.ts` | **新建**，与 `RoleCodeEnum` 对齐的唯一权威常量 |
| `ls-aegis-web/src/directives/permission/hasRole.ts` | 引用 `role.ts` |
| `ls-aegis-web/src/utils/has.ts` | 引用 `role.ts` |

**`role.ts` 建议**：

```typescript
/** 与后端 RoleCodeEnum 对齐 */
export const SUPER_ADMIN_ROLE = 'super_admin'
export const TENANT_ADMIN_ROLE = 'admin'
export const SUPER_ROLE_CODES = [SUPER_ADMIN_ROLE, TENANT_ADMIN_ROLE] as const

export function isSuperRole(role: string): boolean {
  return (SUPER_ROLE_CODES as readonly string[]).includes(role)
}
```

**`hasRole` / `has.ts` 逻辑**：超管/租户管理员均视为超级角色（对齐 `RoleCodeEnum.getSuperRoleCodes()`）。

**兼容策略**：过渡期可同时识别 `role_admin`（读 localStorage 旧缓存），1～2 个版本后删除。

**验收**：超管 `roles` 含 `super_admin` 时，`has.hasRole('super_admin')` 为 true。

#### 13.4.2 前端权限匹配对齐后端（P1）

后端 `SaTokenConfiguration.configurePermissionMatch()` 已支持 `PatternMatchUtils.simpleMatch` + `*:*:*`；前端 `v-permission` 仅精确匹配。

在 `hasPerm.ts` / `has.ts` 的 `authPermission` 中增加通配匹配（与后端 `simpleMatch` 语义对齐）。

**验收**：用户持有 `eoms:order:*` 时，`v-permission="['eoms:order:update']"` 可见。

#### 13.4.3 权限缓存策略（P1）

`user.ts` 当前：`persist: { paths: ['token', 'roles', 'permissions', 'pwdExpiredShow'] }`。

**推荐**：`permissions` / `roles` 不持久化，每次路由守卫 `getInfo()` 刷新；后端已有 Session 热更新。

#### 13.4.4 阶段一交付清单

| 项 | 文件 | 优先级 |
|---|---|---|
| 角色常量统一 | `constant/role.ts` + 2 处引用 | P0 |
| 通配权限匹配 | `hasPerm.ts` + `has.ts` | P1 |
| 取消 permissions 持久化 | `stores/modules/user.ts` | P1 |

---

### 13.5 阶段二：EOMS 权限清单梳理与三端对齐

**目标**：以**后端 `@SaCheckPermission` 为权威源**，补齐菜单种子、修正前后端不一致，使非超管角色可正常分配 EOMS 权限。

#### 13.5.1 命名规范（定稿）

EOMS 统一 **三段式** `eoms:{resource}:{action}`：

| 层级 | 格式 | 示例 |
|---|---|---|
| 页面（type=2） | `eoms:{resource}:list` | `eoms:repair:apply:list` |
| 按钮（type=3） | `eoms:{resource}:{action}` | `eoms:repair:add` |
| 动作动词 | list / query / add / update / delete / export / confirm / cancel / close / assign / save | 与 PSI 一致 |

**废弃/合并项**：

| 问题权限 | 权威值 | 处理方式 |
|---|---|---|
| `eoms:repair:toOrder`（前端） | `eoms:repair:assign`（后端） | **改前端** `apply.vue` |
| `eoms:dashboard:view`（菜单/前端） | `eoms:dashboard:list`（后端） | **改菜单 + 前端** `dashboard.vue` |
| `eoms:finance:profit`（菜单） | `eoms:profit:query`（后端） | **改菜单** 20042 |
| `eoms:repair:settlement`（后端一处） | `eoms:settlement:query` | **改后端** `RepairSettlementController` |
| `eoms:record:*` vs 页面 `eoms:repair:record:list` | 页面 list 保留；按钮用 `eoms:record:*` | 菜单补按钮，不改后端 |
| `eoms:payment:list`（页面） vs `eoms:payment:query`（接口） | 页面 list + 按钮 query/add/delete | 菜单补按钮 |

#### 13.5.2 EOMS 完整权限矩阵（后端权威）

**报修申请**（`RepairApplyController`）

| 权限码 | 接口 | 菜单按钮 | 前端 v-permission |
|---|---|---|---|
| `eoms:repair:list` | GET list/export | ❌ 缺 | — |
| `eoms:repair:query` | GET /{id} | ❌ 缺 | ✅ apply.vue |
| `eoms:repair:add` | POST | ❌ 缺 | ✅ |
| `eoms:repair:confirm` | PUT confirm | ❌ 缺 | ✅ |
| `eoms:repair:cancel` | PUT cancel | ❌ 缺 | ✅ |
| `eoms:repair:close` | PUT close | ❌ 缺 | ✅ |
| `eoms:repair:assign` | PUT toOrder | ❌ 缺 | ❌ 用了 toOrder |
| `eoms:repair:remove` | DELETE | ❌ 缺 | — |

**报修工单**（`RepairOrderController`）

| 权限码 | 菜单按钮 | 前端 |
|---|---|---|
| `eoms:order:query` | ✅ 20111 | — |
| `eoms:order:add` | ✅ 20112 | ✅ |
| `eoms:order:update` | ✅ 20113 | ✅ |
| `eoms:order:delete` | ✅ 20114 | ✅ |
| `eoms:order:export` | ✅ 20115 | — |
| `eoms:order:accept` | ✅ 20116 | — |
| `eoms:order:start` | ✅ 20117 | ✅ |
| `eoms:order:finish` | ✅ 20118 | ✅ |
| `eoms:order:close` | ✅ 20119 | ✅ |
| `eoms:repair:assign` | ✅ 20120 | — |

**维修记录**（`RepairRecordController`）

| 权限码 | 菜单 | 前端 |
|---|---|---|
| `eoms:record:list/query/export/add/update/delete` | ❌ 全缺 | ✅ add/update/delete |

**维修结算**（`RepairSettlementController`）

| 权限码 | 菜单 | 前端 |
|---|---|---|
| `eoms:settlement:query/save/confirm` | ❌ 全缺 | ✅ |
| `eoms:repair:settlement` | — | —（应删除，统一 settlement） |

**合同 / 财务 / 统计**

| 模块 | 后端权限 | 菜单按钮 | 前端 |
|---|---|---|---|
| 合同 | list/export/query/add/update/delete | ❌ 全缺 | ✅ add/update/delete |
| 应收 | query/export/add/update/delete | ❌ 全缺 | ✅ add/update/delete |
| 收款 | query/add/update/delete | ❌ 全缺 | ✅ |
| 利润 | `eoms:profit:query` | 菜单码错误 | 待查 |
| 统计 | `eoms:statistics:list` | 仅页面 | — |
| 看板 | `eoms:dashboard:list` | 菜单 view 错误 | view 错误 |
| 收入 | `eoms:income:*`（6 个） | ❌ 无菜单 | 无页面 |

**系统配置**（挂 `system` 前缀）

| 权限码 | 说明 |
|---|---|
| `system:eomsConfig:get/update` | EOMS 业务参数 |
| `system:h5Auth:list/kickout` | H5 授权管理 |

#### 13.5.3 核心问题：菜单按钮种子严重缺失

当前 `patch_eoms_menu.sql` / `init.sql` 中 EOMS **仅有页面菜单 + 工单按钮（20111–20120）**，其余模块按钮权限均未写入 `sys_menu`。

**影响**：非超管用户即使勾选 EOMS 菜单，也拿不到 `eoms:repair:add` 等按钮权限 → 接口 403、按钮隐藏。

**修复**：新增 Liquibase patch `patch_eoms_permission_buttons.sql`（建议 ID 段 20101–20199），示例：

```sql
-- 报修申请 20010 下按钮
(20101, '查询', 20010, 3, ..., 'eoms:repair:list', ...),
(20102, '新增', 20010, 3, ..., 'eoms:repair:add', ...),
(20103, '确认', 20010, 3, ..., 'eoms:repair:confirm', ...),
(20104, '取消', 20010, 3, ..., 'eoms:repair:cancel', ...),
(20105, '关闭', 20010, 3, ..., 'eoms:repair:close', ...),
(20106, '转工单', 20010, 3, ..., 'eoms:repair:assign', ...),
(20107, '删除', 20010, 3, ..., 'eoms:repair:remove', ...),
-- 维修记录 20012、结算 20013、合同 20032、应收 20040、收款 20041 ...
```

**SQL 规范同步**（强制）：

1. 新增 `patch_eoms_permission_buttons.sql` → 挂 `db.changelog-master.yaml`
2. 合并到 `main_data.sql`
3. 同步 `sql/init.sql`
4. 已上线库：patch 幂等执行 + 超管/默认角色自动绑定新按钮

#### 13.5.4 前后端代码对齐（小改）

| 文件 | 改动 |
|---|---|
| `ls-aegis-web/src/views/eoms/repair/apply.vue` | `eoms:repair:toOrder` → `eoms:repair:assign` |
| `ls-aegis-web/src/views/eoms/dashboard.vue` | `eoms:dashboard:view` → `eoms:dashboard:list` |
| `RepairSettlementController.java` | `eoms:repair:settlement` → `eoms:settlement:query` |
| `patch_eoms_menu.sql` + `init.sql` | 20042 → `eoms:profit:query`；20070 → `eoms:dashboard:list` |

#### 13.5.5 阶段二验收标准

1. 新建「EOMS 运维员」角色，仅勾选 EOMS 菜单树 → 可正常增删改查各模块
2. 非超管用户（已分配权限）调用 `/eoms/repair/apply` POST 不再 403
3. 建议 CI 增加三方 diff：`@SaCheckPermission` ↔ `sys_menu.permission` ↔ `v-permission`

---

### 13.6 阶段三：EOMS / Sign 数据权限（DataScope）改造评估

**目标**：EOMS 核心业务表实现与 PSI 一致的行级数据隔离；Sign 明确「平台 RBAC vs 印章 ACL」边界。

#### 13.6.1 现状对比

| 维度 | PSI（已接入） | EOMS | Sign |
|---|---|---|---|
| ORM | MyBatis-Plus + `DataPermissionMapper` | 传统 XML Mapper + `XmlMapperDO` | 传统 XML Mapper |
| 表结构 | `create_user`(bigint) + `dept_id` + `tenant_id` | `create_by`(varchar) + `del_flag`，**无 dept_id** | 同 EOMS 风格 |
| 数据权限 | `@DataPermission` 自动 SQL 改写 | 无 | 无 |
| 业务 ACL | 无 | 无 | `sign_seal_permission` 印章级授权 |

**结论**：EOMS/Sign 不能直接照搬 PSI 的 `@DataPermission`，需先做**表结构标准化**，再选接入路径。

#### 13.6.2 EOMS 数据权限改造（推荐分三期）

**第一期：表结构标准化（前置，必须）**

为核心业务表补平台标准字段（参考 `patch_psi_data_permission_pilot.sql`）：

| 表 | 新增字段 | 回填规则 |
|---|---|---|
| `eoms_repair_apply` | `tenant_id`, `create_user`, `dept_id` | `create_user` ← JOIN `sys_user` ON username=`create_by` |
| `eoms_repair_order` | 同上 + 保留 `engineer_id` | 同上 |
| `eoms_repair_record` | 同上 | 同上 |
| `eoms_repair_settlement` | 同上 | 同上 |
| `eoms_contract` | 同上 | 同上 |
| `eoms_receivable` / `eoms_payment` | 同上 | 同上 |

注意：保留 `create_by` 过渡期只读；新写入走 `create_user`；`engineer_id` 可作为 SELF 范围补充（「只看派给我的工单」）。

**第二期：查询层接入 DataScope（试点）**

| 路径 | 说明 | 推荐 |
|---|---|---|
| **路径 A** | Service 层手动注入 `deptIds` / `createUser` 过滤条件，兼容现有 XML Mapper | ✅ 推荐 |
| **路径 B** | 迁移 MyBatis-Plus + `DataPermissionMapper` | 长期，成本高 |

试点表：`eoms_repair_order`（工程师 + 部门隔离需求最强）。

**第三期：全量铺开 + 角色默认 DataScope**

| 角色类型 | 建议 data_scope | 说明 |
|---|---|---|
| EOMS 主管 | DEPT_AND_CHILD | 本部门及下级工单 |
| EOMS 工程师 | SELF | 仅 `engineer_id` 或 `create_user` 相关工单 |
| EOMS 财务 | DEPT | 本部门应收/收款 |
| 超管/租户管理员 | ALL | 已有豁免 |

统计/看板：聚合 SQL 单独处理；DataScope 在明细层，汇总层按部门参数过滤。

#### 13.6.3 Sign 模块评估结论

Sign **不建议**全量照搬平台 DataScope，建议双层模型：

```
┌─────────────────────────────────────────┐
│ 平台 RBAC（sys_menu.permission）         │
│ → 能否进入模块 / 调用管理接口             │
├─────────────────────────────────────────┤
│ 印章 ACL（sign_seal_permission）         │
│ → 能否对具体印章执行 use/extract/auth    │
└─────────────────────────────────────────┘
```

| 实体 | 是否接 DataScope | 理由 |
|---|---|---|
| `sign_project` | ✅ 建议接 | 项目级隔离，按部门/创建人 |
| `sign_log` / `sign_record` | ✅ 建议接 | 审计数据按部门可见 |
| `sign_seal` | ❌ 不接 | 已有 `sign_seal_permission` 精确授权 |
| 证书/密钥 | ❌ 暂不接 | 安全敏感，仅超管/专用角色 |

Sign 改造量：project + log 核心表补 `dept_id`/`create_user`，Service 层试点过滤即可。

#### 13.6.4 阶段三里程碑与风险

| 里程碑 | 交付物 | 风险 |
|---|---|---|
| M1（3 天） | EOMS 6 张核心表 patch + 回填脚本 | 存量 `create_by` 无法匹配用户 → 默认 dept_id=0 |
| M2（5 天） | 工单/报修试点 DataScope + 角色种子 | H5 端与后台 DataScope 规则需统一 |
| M3（5 天） | 合同/财务铺开 | 跨部门合同可见性需业务确认 |
| M4（3 天） | Sign project/log 试点 | 与 seal ACL 文档化 |

**不建议一次性做的**：

- EOMS 全模块同时改 ORM
- Sign 印章表接 DataScope（与 ACL 冲突）
- Quote 模块（两段式权限，优先级低于 EOMS）

#### 13.6.5 阶段三落地进度（2026-08-20）

| 里程碑 | 状态 | 交付物 |
|---|---|---|
| **M1 表结构** | ✅ 已落地 | `patch_eoms_data_permission_m1.sql`：七表补 `tenant_id`/`create_user`/`dept_id`，按 `create_by`→`sys_user.username` 回填；已挂 master 并入 `init.sql` |
| **M2 工单试点** | ✅ 已落地 | `EomsDataScopeHelper` + `RepairOrderMapper` XML 过滤 + `RepairOrderServiceImpl` 列表/详情/写入 |
| **M2b 报修/记录/结算** | ✅ 已落地 | 申请(`a`) / 记录(`r`) / 结算(`s`) 列表·详情·写入接 DataScope；转工单/提交结算继承部门 |
| **M2c 角色种子** | ✅ 已落地 | `eoms_manager`(DEPT_AND_CHILD) / `eoms_engineer`(SELF) / `eoms_finance`(DEPT) + 默认菜单；`patch_eoms_role_data_scope.sql` |
| **M3 合同/财务铺开** | ✅ 已落地 | 合同 / 应收 / 收款复用共用片段 `EomsDataScopeMapper.dataScopeFilter`；结算生成应收、核销写收款均 `fillOnInsert` |
| **M4 Sign 试点** | ✅ 已落地 | project/log/record 接 DataScope；印章仍走 `sign_seal_permission` ACL |

**M2/M3 过滤规则（与 ContiNew 对齐）**：

- 超管 / 租户管理员：不过滤
- 多角色 DataScope：**OR** 合并；任一角色 `ALL` 则整体放行
- `DEPT`：`dept_id = 当前部门`
- `DEPT_AND_CHILD`：本部门 + `sys_dept.ancestors`（`find_in_set`）
- `SELF`：`create_user = 当前用户`；工单额外 **或** `engineer_id = 当前用户`
- `CUSTOM`：`dept_id IN (sys_role_dept WHERE role_id IN …)`
- 新写入：`fillOnInsert` 填 `create_user`/`dept_id`/`tenant_id`，保留 `create_by`

**M4 Sign 规则与边界**：

```
┌─────────────────────────────────────────┐
│ 平台 DataScope（project / log / record） │
│ → 按部门/本人过滤列表与详情               │
├─────────────────────────────────────────┤
│ 印章 ACL（sign_seal_permission）         │
│ → 能否对具体印章执行 use/extract/auth    │
│ → 不接平台 DataScope                     │
└─────────────────────────────────────────┘
```

| 实体 | DataScope | 说明 |
|---|---|---|
| `sign_project` | ✅ | SELF 含 `create_user` 或 `manager_ids` |
| `sign_log` / `sign_record` | ✅ | SELF 含 `create_user` 或 `user_id`；写入继承项目部门 |
| `sign_seal` | ❌ | 仅 ACL + 选章 SQL 部门/授权 |

**关键文件**：

| 用途 | 路径 |
|---|---|
| M1 patch | `ls-aegis-boot/.../mysql/patch_eoms_data_permission_m1.sql` |
| Helper | `ls-aegis-biz-eoms/.../utils/EomsDataScopeHelper.java` |
| 共用 SQL 片段 | `ls-aegis-biz-eoms/.../mapper/eoms/EomsDataScopeMapper.xml` |
| 工单 | `RepairOrderMapper.xml` / `RepairOrderServiceImpl` |
| 报修申请 | `RepairApplyMapper.xml` / `RepairApplyServiceImpl`（别名 `a`） |
| 维修记录 | `RepairRecordMapper.xml` / `RepairRecordServiceImpl`（别名 `r`） |
| 维修结算 | `RepairSettlementMapper.xml` / `RepairSettlementServiceImpl`（别名 `s`） |
| 合同 | `ContractMapper.xml` / `ContractServiceImpl`（别名 `ct`） |
| 应收 | `ReceivableMapper.xml` / `ReceivableServiceImpl`（别名 `r`，含未结清查询） |
| 收款 | `PaymentMapper.xml` / `PaymentServiceImpl`（别名 `p`） |
| EOMS 角色种子 | `patch_eoms_role_data_scope.sql`（`eoms_manager`/`eoms_engineer`/`eoms_finance`） |
| Sign M4 patch | `ls-aegis-boot/.../mysql/patch_sign_data_permission_m4.sql` |
| Sign Helper | `ls-aegis-biz-sign/.../utils/SignDataScopeHelper.java` |
| Sign SQL 片段 | `ls-aegis-biz-sign/.../mapper/sign/SignDataScopeMapper.xml` |

**验收建议**：

1. 角色 data_scope=SELF 的工程师：仅见 `engineer_id` 或本人创建的工单；申请/记录/结算仅见本人 `create_user`（或本部门视角色）
2. 角色 data_scope=DEPT：仅见本部门 `dept_id` 工单 / 申请 / 记录 / 结算 / 合同 / 应收 / 收款
3. 角色 `eoms_manager`：DEPT_AND_CHILD；`eoms_engineer`：SELF；`eoms_finance`：DEPT（种子已写入）
4. 超管：列表与改前一致（全量）
5. 结算确认生成的应收、收款核销写入带当前用户 `dept_id`；提交结算继承工单部门
6. Sign：DEPT 角色仅见本部门项目及对应 log/record；无 ACL 印章不可用；超管全量
7. 已上线库：Liquibase 执行 M1/M4/角色种子 patch；全新安装走 `init.sql` 幂等 ALTER

---

### 13.7 总体排期建议

| 阶段 | 优先级 | 可独立上线 | 业务价值 |
|---|---|---|---|
| 一 | **立即** | ✅ | 消除隐性 Bug，为后续打基础 |
| 二 | **高** | ✅ | 非超管 EOMS 用户可正常使用 |
| 三 | **中** | 分期 | 多部门/工程师数据隔离 |

```
阶段一（0.5～1 天）  角色码 + 权限匹配 + 缓存
    ↓
阶段二（2～3 天）    EOMS 权限矩阵 + SQL 种子 + 前后端对齐
    ↓
阶段三（1～2 周）    EOMS 表结构 → 工单试点 → 合同财务 → Sign 试点
```

---

### 13.8 核心文件索引

| 类别 | 路径 |
|---|---|
| Sa-Token 配置 | `ls-aegis-security/.../SaTokenConfiguration.java` |
| 登录权限加载 | `ls-aegis-rbac/.../AbstractLoginHandler.java` |
| 超管权限短路 | `ls-aegis-rbac/.../RoleServiceImpl.java` |
| CRUD 自动鉴权 | `ls-aegis-security/.../BaseController.java` |
| 数据权限提供者 | `ls-aegis-mybatis/.../DefaultDataPermissionUserDataProvider.java` |
| 前端路由守卫 | `ls-aegis-web/src/router/guard.ts` |
| 前端按钮权限 | `ls-aegis-web/src/directives/permission/hasPerm.ts` |
| EOMS 菜单种子 | `ls-aegis-boot/.../patch_eoms_menu.sql` |
| EOMS DataScope M1 | `ls-aegis-boot/.../patch_eoms_data_permission_m1.sql` |
| EOMS 角色 data_scope | `ls-aegis-boot/.../patch_eoms_role_data_scope.sql` |
| EOMS DataScope Helper | `ls-aegis-biz-eoms/.../EomsDataScopeHelper.java` |
| Sign DataScope M4 | `ls-aegis-boot/.../patch_sign_data_permission_m4.sql` |
| Sign DataScope Helper | `ls-aegis-biz-sign/.../SignDataScopeHelper.java` |
| PSI DataScope 参考 | `ls-aegis-boot/.../psi/patch_psi_data_permission_*.sql` |

---

### 13.9 变更记录

| 日期 | 说明 |
|---|---|
| 2026-08-19 | 初版：RBAC 全项目检查结论 + 阶段一～三修复方案（前端角色码、EOMS 权限矩阵、DataScope 评估） |
| 2026-08-20 | 阶段一/二落地；阶段三 M1 七表字段 + M2 工单 DataScope 试点 |
| 2026-08-20 | 阶段三 M3：合同/应收/收款 DataScope 铺开 + 共用 SQL 片段 |
| 2026-08-20 | 阶段三 M4：Sign project/log/record DataScope；init 合并 patch；合并签章 fillAuditRow |
| 2026-08-20 | 阶段三 M2b/M2c：报修申请/记录/结算三表 DataScope + EOMS 三角色默认 data_scope 种子 |
| 2026-08-20 | **部署闭环**：`mvn package` → Docker 重建 `ls-aegis-server`；Liquibase 执行 `eoms_role_data_scope`；超管三表 list `code=0`；`test`+`eoms_engineer`(SELF) 工单 list=0 vs 超管=3，行级过滤生效 |

---

## 14. 本地开发（中间件 Docker + 本机后端）

> 更新日期：2026-08-21（`TOKEN_SECRET` 与库口令同一套：空则 `ensure-env.ps1` 随机写回 `.env`，见 §17）  
> 目标：日常改后端不必每次 `mvn package` + 重建 Docker 镜像；与前端 Vite HMR 对称。

### 一键启动（CMD 一条命令）

先确保有 `docker/.env`（可从 `docker/.env.example` 复制，或直接跑 `dev-up` / `ensure-env.ps1` 自动创建）。  
`MYSQL_ROOT_PASSWORD` / `DB_PWD` / `REDIS_PWD` / `JWT_SECRET_KEY` / `TOKEN_SECRET` 若为空，由 `docker/ensure-env.ps1` **随机生成并写回 `.env`**（已有值不覆盖）；Compose / 本机后端均读该文件。

在仓库根目录 `d:\itls`：

**CMD：**

```bat
dev.cmd
```

**PowerShell（须加 `.\`）：**

```powershell
.\dev.cmd
```

任意目录等价写法：

```bat
d:\itls\dev.cmd
```

或：

```bat
d:\itls\docker\dev-all.cmd
```

效果：

1. 仅启动 Docker 中的 MySQL / Redis（并停掉容器内 server/web/h5）
2. 新开窗口启动本机后端（`dev`，端口 **8000**）；若已在跑则复用
3. **轮询** `GET http://127.0.0.1:8000/auth/gm/public-key` 直至 200（默认最长约 10 分钟）
4. 后端就绪后再新开：PC **5173**、H5 客户 **5174**、H5 工程师 **5175**（均代理本机 8000）

浏览器：

| 端 | 地址 |
|---|---|
| PC | http://localhost:5173 |
| H5 客户报修 | http://localhost:5174 |
| H5 工程师 | http://localhost:5175 |
| API | http://127.0.0.1:8000 |

关掉对应窗口即停止该进程；中间件保留，可用 `powershell -File docker/dev-down.ps1` 停止。  
若等待超时：先看后端窗口日志，修好后再执行 `dev.cmd`，或手动起前端。

### 模式

| 组件 | 日常开发 | 完整部署栈 |
|---|---|---|
| MySQL / Redis | Docker（`dev-up.ps1` / `dev.cmd`） | Docker |
| `ls-aegis-server` | 本机 `dev`（端口 **8000** + DevTools） | 容器 `prod`（**18000**） |
| `ls-aegis-web` | `npm run dev`（5173 → 代理 8000） | Nginx 镜像 8080 |
| `ls-aegis-h5` | `dev:customer` **5174** / `dev:engineer` **5175** → 代理 8000 | 容器 18081 / 18082 |

### 分步启动（可选）

1. 中间件：`powershell -File docker/dev-up.ps1`  
   - 实际命令：`docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d`  
   - 仅起 MySQL/Redis；应用容器挂 `profiles: [full]`，默认不起。
2. 后端二选一：
   - 脚本：`powershell -File docker/dev-run-server.ps1`  
     - 在 `ls-aegis-server` reactor 执行：`mvn -pl ls-aegis-boot -am org.springframework.boot:spring-boot-maven-plugin:3.3.12:run`  
     - 插件钉死 **3.3.12**（与依赖一致；省略 version 会误解析到 4.x）；父工程 / 非 boot 模块 `skip=true`，仅 boot 真正启动  
     - 工作目录对齐 `docker/data/ls-aegis-server`，读 `docker/.env` 并改写国密路径  
   - IDE：运行 `ls-aegis-boot`，profile=`dev`；工作目录设为 `docker/data/ls-aegis-server`；环境变量与 `docker/.env` 对齐，且 `GM_KEY_STORE_PATH` 指向该目录下 `data/gm/keys.properties`（勿用容器内 `/app/...`）
3. 前端：`cd ls-aegis-web && npm run dev`（`.env.development` 已指向 `http://127.0.0.1:8000`）  
   - 若出现 `ECONNREFUSED 127.0.0.1:8000`，说明后端尚未起来，等 boot 窗口出现 `Started LsAegisApplication` 后再刷新。  
   - **勿**裸跑 `mvn spring-boot:run`（父工程会报 `No plugin found for prefix 'spring-boot'`；请用上面的脚本或完整 GAV）。

### 热更新说明

- **前端**：Vite HMR，无需 build。
- **后端**：已加 `spring-boot-devtools`（optional）。IDE 保存并编译后自动重启上下文；方法体小改部分场景可热替换。结构变更（类签名、配置）仍会触发快速重启，远快于打镜像。
- **发版 / 整栈联调**：仍用 `docker compose up -d --build`（勿用 `docker-compose.dev.yml`）。

### 相关文件

| 文件 | 作用 |
|---|---|
| `dev.cmd`（仓库根） | CMD 一键启动入口 |
| `docker/dev-all.cmd` / `dev-all.ps1` | 起中间件 + 新窗口：后端 / PC / H5×2 |
| `docker/dev-up.ps1` | 仅 MySQL/Redis |
| `docker/dev-run-server.ps1` | 仅本机后端 |
| `docker/dev-down.ps1` | 停中间件（保留数据卷） |
| `docker/docker-compose.dev.yml` | 开发覆盖（应用服务默认不起） |
| `ls-aegis-web/.env.development` | PC 代理 `127.0.0.1:8000` |
| `ls-aegis-h5/.env.customer` / `.env.engineer` | H5 5174/5175，代理 `8000` |
| `ls-aegis-boot` | `spring-boot-devtools`；`application-dev.yml` 端口 8000 |

---

## 15. 前端雪花 ID 精度与 Select 回显

> 日期：2026-08-20  
> 现象：`a-select` / `GiForm` 多选回显显示裸数字 ID（如 `547889293968801800`），不显示姓名/名称。  
> 根因：雪花 `Long` 超过 JS `Number.MAX_SAFE_INTEGER`，前端 `Number()` / `parseInt` 丢精度后与 options 的 `value` 对不上，Arco 只能渲染原始 value。

### 约定（强制）

| 层 | 规则 |
|---|---|
| 后端 | Jackson `Long` → `ToStringSerializer`（`JacksonConfig`），ID 一律字符串下发 |
| 前端工具 | `ls-aegis-web/src/utils/biz-id.ts`：`toBizId` / `toBizIdList` / `toSelectOpts` / `toUserSelectOpts` / `toLabelValueOpts` / `ensureSelectOpt` / `normalizeTreeBizIds` |
| 表单绑定 | Select 的 `v-model` 与 `options[].value` **全程 string**，禁止对业务主键 `Number()` |
| 编辑回填 | 选项列表加载后，对已选 ID 调 `ensureSelectOpt(..., id, 名称兜底)`，避免用户已删/不在当前列表时仍显示裸 ID |

### 已落地范围（2026-08-20）

| 域 | 点位 |
|---|---|
| 系统 | 公告 `noticeUsers`；部门/菜单树；`UserSelect`；角色权限相关 ID 列表 |
| 租户 | 套餐 `toLabelValueOpts` + 编辑 `ensureSelectOpt(packageName)` |
| 签章 | 项目负责人/部门/印章；印章授权用户；印章新增部门回填 |
| 进销存·库存 | 入/出/退/调拨/盘点/调整：`toBizId` + `ensureSelectOpt`（盘点已纠正 loadOptions→fill 顺序） |
| 进销存·主数据 | 仓库负责人回填 `ensureSelectOpt` |
| 进销存·单据 | 销售/采购合同、付款、收款、进/销项发票、期初、银行流水匹配 |
| 运维 | 运维合同客户；应收客户；报修客户；用料商品仓库；派单工程师（含 `nickName` 兼容） |
| 报价 | 商品回填 `ensureSelectOpt` / 库存缺项兜底 |

编辑/查看统一顺序：**先 `loadOptions` → 再 `fill`（含 `ensureSelectOpt`）→ 再 `visible`**，避免后载 options 冲掉兜底项。

### 补充踩坑（续修）

1. **`toUserSelectOpts` 必须兼容 `nickName`**：部分接口（如 EOMS `engineerOptions`）MyBatis Map 字段是 `nickName`，只读 `nickname` 时 label 会退化成 ID，下拉本身全是裸数字。  
2. **盘点等弹窗**：禁止 `fill` 后再 `loadOptions` 覆盖 options；否则 `ensureSelectOpt` 兜底项会被冲掉。  
3. **租户套餐**：`reset()` 里异步拉字典时，`onUpdate` 须 `await getPackageList()` 后再回填并 `ensureSelectOpt`。

### 新页清单（勿再踩坑）

1. 拉选项：`toUserSelectOpts` / `toSelectOpts` / `toLabelValueOpts`，不要手写 `value: u.id` 且再 `Number(id)`。  
2. 详情回填：`form.xxxId = toBizId(data.xxxId)`；多选 `toBizIdList(...)`。  
3. 立刻 `ensureSelectOpt(options, id, data.xxxName)`。  
4. 树选：`normalizeTreeBizIds`。  
5. 金额、排序、level 等**非雪花**数值仍可用 `Number`；**仅禁业务主键**。

### 回归抽查

- 编辑含多负责人的签章项目 → 标签为昵称，非裸 ID。  
- 公告指定用户、印章授权用户 → 同上。  
- PSI 编辑入库单 / 盘点单已选仓库/商品 → 名称完整。  
- EOMS 派单工程师下拉 → 显示昵称，非裸 ID。  
- 租户编辑套餐 → 显示套餐名。

---

## 16. 雪花 ID 精度影响全量审计

> 日期：2026-08-20  
> 范围：`ls-aegis-server`（CosId / Jackson / Long DTO）、`ls-aegis-web`（biz-id / API 类型 / Select）、`ls-aegis-h5`、`Docs/20260820_171500_雪花ID孤儿扫描/`  
> 关联：[§15](#15-前端雪花-id-精度与-select-回显)（Select 回显专项）；本篇为**精度链路全量审计**与残留风险清单。

### 16.1 结论（一句话）

**传输与生成侧已基本安全**（Jackson `Long→string` + 前端 `biz-id` 约定）；**真实伤害在历史 `Number()` 写库导致的脏外键**，以及 **TS 类型仍大量 `string | number` / 个别 `number`** 带来的复发风险。

### 16.2 根因机制

| 项 | 值 |
|---|---|
| JS 安全整数上限 | `Number.MAX_SAFE_INTEGER` = `9007199254740991`（约 16 位） |
| CosId 典型值 | 18～19 位，例如 `880831416592498907` |
| 丢精度示例 | `Number(880831416592498907)` → `880831416592499000`（末三位被四舍五入） |
| 业务后果 | Select `v-model` 与 `options.value` 对不上 → 裸 ID；更严重时错误 ID 落库 → 孤儿 FK / 关联断裂 |

### 16.3 当前链路（目标态）

```
CosId snowflake → MySQL bigint(20) → Java Long
  → Jackson ToStringSerializer → JSON "8808…"
  → Axios 收到 string → toBizId / Select value(string)
  → 请求体字符串 → Jackson 反序列化为 Long → DB
```

任意一环经 JS `Number` / `parseInt` 或 JSON **数字**字面量，都会再次丢精度。

### 16.4 已落实防护

| 层 | 状态 | 位置 |
|---|---|---|
| ID 生成 | CosId + MyBatis `ASSIGN_ID` | `application.yml`（`id-type: ASSIGN_ID`，`id-generator.type: COSID`，snowflake 已开） |
| 库存储 | `bigint(20)` PK/FK | Liquibase / `init.sql` 一致 |
| API 序列化 | **全局** `Long`/`long` → 字符串 | `JacksonConfig.java` L53–55 |
| 前端约定 | `BizId = string` + 工具集 | `ls-aegis-web/src/utils/biz-id.ts` |
| Select / 树 | 多域已迁 `toBizId` + `ensureSelectOpt` | 见 [§15 已落地范围](#15-前端雪花-id-精度与-select-回显) |
| 静态扫描 | PC 端业务代码中**未发现**对 `*Id` 的 `Number()`/`parseInt` | 剩余 `Number` 多为金额/尺寸/校验码 |

配置摘录：

```53:55:ls-aegis-server/ls-aegis-boot/src/main/java/com/ls/aegis/boot/config/JacksonConfig.java
            // 雪花 ID（Long）一律序列化为字符串，前端禁止 Number() 再转
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
```

### 16.5 已证实的数据污染（Critical）

扫描目录：`Docs/20260820_171500_雪花ID孤儿扫描/`。  
可执行脚本：`scan_final_mysql.sql`（单引号 + 与现网 schema 对齐；原 `scan_final.sql` 中 `stock_out_batch.goods_id` 已不存在，应改用 `batch_id`）。

| 真值（ids.tsv） | 污染后（扫描检索值） | 说明 |
|---|---|---|
| `880831416592498907` | `880831416592499000` | 商品等 |
| `880831622973227231` | `880831622973227300` | 仓库等 |
| `880831655659438306` | `880831655659438300` | 仓库等 |
| `880831053256720584` | `880831053256720600` | 往来单位等 |
| `880831095984095435` | `880831095984095500` | 往来单位等 |
| `880831123985268942` | `880831123985269000` | 往来单位等 |

#### 本地库实测（2026-08-20，`ls-aegis-mysql` / `ls_aegis`）

| 检查项 | 结果 |
|---|---|
| corrupt_hits（20 项已知污染值） | **全部 0** |
| orphan FK（合同/收付/发票/应收付/用料/仓管/出库批次/核销） | **全部 0** |

本环境数据干净。若其他环境（预发/生产）曾用前端 `Number()` 提交过，仍应用同一 SQL 复扫。

**处置建议：** 在目标库执行 `scan_final_mysql.sql`；对仍命中的行按业务回写真值、置空或重建关联。

### 16.6 残留风险清单

| # | 问题 | 严重度 | 现状 | 建议 |
|---|---|---|---|---|
| 1 | 历史脏 FK / 孤儿关联 | **Critical** | 扫描 SQL 就绪 | 跑库 → 修复 → 回归业务单据 |
| 2 | 再次对业务 ID `Number()` | **Critical**（若复现） | PC 业务路径已基本清掉 | Code Review 门禁；可加 ESLint 规则 |
| 3 | Jackson Long→string 被绕过 | **Critical** | 全局已配 | 禁止业务模块另起无 Long 序列化的 `ObjectMapper` Bean |
| 4 | API TS 大量 `string \| number` | **Medium** | EOMS / 签章 / 报价 / PSI 嵌套 FK 普遍 | 统一改为 `BizId` / `string` |
| 5 | 显式 `id: number` | **Medium** | `eoms/h5Auth.ts` 的 `customerId`；`user-message.ts` 的 get 详情；`DictItemResp.dictId` | 改为 `string` |
| 6 | H5 无 `biz-id` 工具 | **Medium** | 当前无对 ID 的 `Number()`，依赖后端字符串 | 抽共享 `toBizId` 或复制薄封装 |
| 7 | CosId `safe-js` 未作默认生成器 | **Low** | 已注册 `machine-bit:7 / sequence-bit:9` 但默认仍为全宽雪花 | **优先保持字符串传输**；不必靠缩 bit |
| 8 | `GiCellTag` 中 `Number(props.value)` | **Low** | 字典 value 多为小码 | 改为纯字符串比较更稳 |
| 9 | 调度 Job `id: number` | **Low** | SnailJob 侧，多半非 CosId | 确认后可保留 |

典型错误类型声明：

```7:7:ls-aegis-web/src/apis/eoms/h5Auth.ts
  customerId: number
```

```19:19:ls-aegis-web/src/apis/system/user-message.ts
export function getUserMessage(id: number) {
```

### 16.7 影响面（按现象）

| 现象 | 是否仍可能发生 | 说明 |
|---|---|---|
| Select / 树只显示裸 ID | 低（已迁页面） | 未迁或新页未按 §15 清单实现时仍会 |
| 提交后关联错人/错仓/错商品 | 中（依赖脏数据是否清理） | 历史污染行；新提交在字符串链路下应正常 |
| 列表能查、详情/联表空 | 中 | 孤儿 FK 典型表现 |
| H5 客户/工单串数据 | 低～中 | 依赖 `customerId` 等是否曾被 Number 化；类型仍写 `number` 易误导 |

### 16.8 行动项（建议优先级）

1. **P0** ~~现网执行 `scan_final.sql`~~ → 已用 `scan_final_mysql.sql` 对本地 `ls_aegis` 实测 **全 0**；其他环境仍需复扫。  
2. **P1** ~~前端 API 类型收敛~~ → 已完成：EOMS/签章/报价/PSI/common 等 CosId 字段改为 `string`；`h5Auth.customerId`、`user-message`、`dictId` 已改。调度 Job 仍为 `number`（非 CosId）。  
3. **P1** ~~H5 引入 `toBizId`~~ → 已加 `ls-aegis-h5/src/utils/biz-id.js`，并用于客户登录、报修详情、工程师任务/记录。  
4. **P2** Review / lint：禁止标识符含 `Id`/`id` 的调用点使用 `Number(`（金额/qty 白名单）。  
5. **P2** ~~`GiCellTag` 字符串比较~~ → 已改为 `String(d.value) === String(props.value)`。  
6. **P3** 不推荐默认切 `safe-js` 作为主生成器；字符串下发已足够。

### 16.9 验收标准

- [x] 本地库扫描 corrupt_hits / orphan 全 0（其他环境待复扫）。  
- [ ] 编辑态 Select 抽样（签章负责人、公告用户、PSI 仓/商品、EOMS 工程师、租户套餐）均显示名称。  
- [ ] Network 面板：业务 ID JSON 字段均为 **带引号字符串**，无裸数字大整数。  
- [x] `apis/**` 中 CosId 相关 `string | number` 已清零（调度 Job 等非 CosId 除外）。  
- [x] H5 关键提交路径使用 `toBizId`。  
- [x] `GiCellTag` 不再对 value 做 `Number()`。  

### 16.10 与 §15 的关系

| 文档 | 侧重 |
|---|---|
| §15 | Select 回显修复约定与已落地页面清单 |
| **§16（本篇）** | 精度问题端到端审计、数据污染证据、类型债务、后续行动与验收 |

**总结论：** 精度问题影响面大（UI + 数据完整性），但**主护栏已装上**；当前最大未闭环项是 **脏数据清理** 与 **类型/H5 对齐防复发**。

---

## 17. 后端启动警告治理

> 日期：2026-08-21  
> 依据：本机 `docker/dev-run-server.ps1` 一次完整启动日志（`Started LsAegisApplication`）  
> 原则：**消警告、不改业务语义、密钥不入库**

### 17.1 治理前规模

| 口径 | 数量 |
|---|---|
| 日志含 `WARN` / `WARNING` 行 | 约 77 |
| Maven 构建期 `[WARNING]` | 约 63（同一根因多模块刷屏） |
| Spring 运行期 `WARN` | 14 |
| 实质问题类型 | 6 类 |

应用当时可正常启动；其中签章密钥缺失会影响签章敏感字段加解密。

### 17.2 问题与处置一览

| # | 现象 | 根因 | 处置 | 状态 |
|---|---|---|---|---|
| 1 | 各模块 `maven-compiler-plugin` version missing + Ignoring 4.0.0-beta-* | 父 POM 声明插件未写 version，解析到需 Maven 4 的 beta | `ls-aegis-server/pom.xml` 的 `pluginManagement` 固定 **3.13.0** | 已修；`mvn validate` 无该 WARNING |
| 2 | `未配置 TOKEN_SECRET` / 占位符解析失败 | 本机/Compose 未注入签章密钥 | 与库口令同一套路：`ensure-env.ps1` 空值随机写回 `.env`；`application.yml` → `itls.sign.token-secret: ${TOKEN_SECRET}`；应用侧仍 Fail-Fast | 已修（密钥不进仓库） |
| 3 | 5 个实体 `Not found @TableId` | 联合主键关联表无单列 `@TableId` | `RoleMenuDO` / `RoleDeptDO` / `MessageLogDO` / `NoticeLogDO` / `PackageMenuDO` 首列加 `@TableId(type = IdType.INPUT)`，避免全局 `ASSIGN_ID` 覆盖业务 ID | 已修 |
| 4 | Undertow `UT026010` Buffer pool | WebSocket 部署信息未设 BufferPool | `ls-aegis-boot/.../UndertowWebSocketConfig`：仅在 buffers 为空时补 `DefaultByteBufferPool` | 已修 |
| 5 | `No MyBatis mapper was found in '[com.ls.aegis.boot]'` | MP 自动扫启动类包；真实 Mapper 已由 `mybatis-plus.extension.mapper-package` 扫描 | **不**加 `@MapperScan`（防双重注册）；仅 `application-dev.yml` 将该 logger 调至 ERROR | 已抑（dev） |
| 6 | BeanPostProcessorChecker（Crane4j / JetCache / CrudApi*） | 第三方非 static BPP 工厂的常规提示 | 不改第三方；仅 `application-dev.yml` 将 `PostProcessorRegistrationDelegate` 调至 ERROR | 已抑（dev） |

### 17.3 关键变更清单

| 路径 | 变更要点 |
|---|---|
| `ls-aegis-server/pom.xml` | `maven-compiler-plugin` → 3.13.0（pluginManagement） |
| `docker/ensure-env.ps1` | 新建：空密钥随机写回 `.env`（含 TOKEN_SECRET） |
| `docker/.env.example` | 说明由 ensure-env 自动补空密钥 |
| `docker/docker-compose.yml` | `TOKEN_SECRET: ${TOKEN_SECRET}`（无 `:-` 空兜底） |
| `docker/dev-up.ps1` / `dev-run-server.ps1` | 启动前调用 `ensure-env.ps1` |
| `application.yml` | `itls.sign.token-secret: ${TOKEN_SECRET}` |
| `SignApplicationRunner` | 空密钥抛异常阻止启动 |
| `ls-aegis-rbac/.../RoleMenuDO.java`、`RoleDeptDO.java` | `@TableId(IdType.INPUT)` |
| `ls-aegis-biz-notice/.../MessageLogDO.java`、`NoticeLogDO.java` | 同上 |
| `ls-aegis-biz-tenant/.../PackageMenuDO.java` | 同上 |
| `ls-aegis-boot/.../UndertowWebSocketConfig.java` | 新建 |
| `ls-aegis-boot/.../application-dev.yml` | 两处框架噪音 logger → ERROR |

### 17.4 运维备查

**本机开发 / Docker 全栈（同一方式）**

1. `dev-up.ps1` / `dev-run-server.ps1` 会先跑 `ensure-env.ps1`：无 `.env` 则从 example 复制；`MYSQL_ROOT_PASSWORD`/`DB_PWD`/`REDIS_PWD`/`JWT_SECRET_KEY`/`TOKEN_SECRET` 为空则随机生成写回。  
2. **已有值绝不覆盖**（避免 MySQL/Redis 数据卷或签章密文与密钥错位）。  
3. 应用启动仍校验 `TOKEN_SECRET`；勿把 `docker/.env` 提交仓库。

**关联表约定**

- 联合主键表实体用 `IdType.INPUT` 标注联合主键之一，仅消除 MP 警告。  
- 业务侧继续用 `lambdaQuery` / `lambdaUpdate`，**不要**对这类实体调用语义含糊的 `xxById`。

**刻意未改**

- ContiNew / JetCache / Crane4j 的 BPP 工厂改 static（改依赖源码，收益低）。  
- prod 配置不压制框架 WARN，便于线上观察真实问题。

### 17.5 验收

- [x] `mvn -pl ls-aegis-boot -am -DskipTests compile` 成功  
- [x] `mvn ... validate` 不再出现 compiler-plugin missing / 4.0.0-beta Ignoring  
- [ ] 本机重启 `dev-run-server.ps1`：无 TOKEN_SECRET 警告；无 `@TableId` 警告；无 UT026010（需人工确认一次完整启动日志）


## 18. 项目 Agent、渐进优化与业务测试基线

> 本节为首轮历史基线；类型修复与业务执行的最新结果见第 19 节。

日期：2026-11-05（北京时间）。范围：当前 `ls-aegis-mcs` checkout；用户确认维护复杂、业务操作复杂、启动构建与运行占用三个方向均需优化。本节记录第一批落地结果，不把基线建立等同于全项目优化完成。

### 18.1 项目专用 Agent 与 Skills

- 根 `AGENTS.md` 为项目开发与优化 Agent 入口，`tests/AGENTS.md` 为业务测试角色；均继承现有平台、SQL、前端与报告规则。
- `.agents/skills/itls-maintain`：开发、修复、运行与验证。
- `.agents/skills/itls-optimize`：维护成本、操作路径、构建启动与运行性能的基线和渐进优化。
- `.agents/skills/itls-business-test`：跨域流程、权限、状态机、库存、金额、幂等与失败回滚；详细用例位于其 `references/flows.md`。
- 三个 Skills 的命名、frontmatter 已通过 skill-creator 校验，配有 UI 元数据，可按 `$itls-maintain`、`$itls-optimize`、`$itls-business-test` 使用。它们是仓库内工作流，不是三个常驻运行服务。

### 18.2 体检与本轮已实施

基线统计来自 `git ls-files`，不含 node_modules、target、数据卷与新增未跟踪文件：后端 1,305 文件、约 4.97 MB；PC 861 文件、约 3.86 MB；H5 40 文件、约 0.27 MB；历史 Docs 约 4.14 MB。现有 Java `*Test.java` 为 11 个，数量不代表覆盖率。源码热点：旧兼容 ExcelUtil 2,119 行、签章执行页 1,605 行、报价表单 1,165 行。Mock 行政区数据 15,840 行属于数据体积，不应当作业务复杂度直接删除。

| 实施项 | 原问题 | 本轮结果 |
|---|---|---|
| 开发依赖启动 | dev-up 起中间件后还会停止应用容器 | 显式只启动 MySQL/Redis，去掉顺带停应用行为；保留现有开发与生产配置边界 |
| 全新安装 SQL | Liquibase 注释前置条件被 mysql 客户端忽略，重复添加已有 code 列/索引；表及列排序规则混杂 | 全量安装移除已折叠的重复 ALTER，统一为 general_ci；独立空库安装验证通过 |
| 页面源文件管理 | `**/site/` 会忽略网站配置业务目录；原仓库引用该页但未包含源码，Vite 打包失败 | 收窄为 Maven target/site，补齐网站配置页，保留读取/修改/取消/恢复默认及权限控制 |
| 配置加载 | 系统配置页静态导入全部 11 个标签模块 | 各标签改为异步组件，打开哪个加载哪个；Mock/DevTools 的依赖仅在显式开启时导入 |
| API 边界 | 顶层 API 入口混合导出业务域，合同/财务等名称冲突 | 业务域改从各自路径显式导入；去掉无调用需求的顶层业务聚合，消除 9 条冲突诊断 |
| 类型检查可信度 | vue-tsc 2.0.19 + TS 5.0.4 触发 ScriptKind 异常，工具吞异常并返回 0 | TS 调整至 5.4.5；增加遇错失败的 typecheck 入口，并接入 build/build:test；现存类型错误不再被伪装为通过 |
| 运行目录与资料 | 本地运行配置会混入待提交列表，PSI README 引用已废弃 reports 路径 | 忽略 .local，Docker context 排除 Agent/测试/本地运行目录，修正资料入口 |

SQL 改动针对**全新安装**。本轮没有改已执行 Liquibase changeset，也没有对现有业务库自动实施排序规则迁移；历史库升级需要单独生成和验证增量迁移。全量安装脚本不是可在已有库上重复执行的升级脚本。

### 18.3 自动化工具与实测

可重复执行的入口：

```powershell
python -X utf8 scripts/quality/repo_inventory.py
python -X utf8 scripts/quality/service_smoke.py
python -X utf8 scripts/quality/verify_mysql_init.py
python -X utf8 -m unittest discover -s tests -p 'test_*.py' -v
# PC 目录
pnpm typecheck
```

| 检查 | 结果 | 范围与限制 |
|---|---|---|
| Skills 格式校验 | PASS，3/3 | 不等同于所有未来任务行为均已验证 |
| 本机只读冒烟 | PASS，9/9 | PC/H5 首页、后端与代理 R 响应、签章未登录拦截；不创建业务数据 |
| 全新 MySQL 安装 | PASS | 独立无网络、无端口、无持久卷的临时 MySQL 8.0.42；故意使用服务器 0900 默认规则，校验完成标志、全部表 general_ci、管理员、菜单与分类 code；测试容器已清理 |
| 冒烟误判回归 | PASS | 1 个 unittest、7 个响应场景，覆盖业务失败、非平台 JSON、登录 HTML、未登录和重定向，避免 HTTP 200 假通过 |
| 现有核心单测 | PASS，13/13 | SoftwareGmCrypto 7、SM3 密码 1、SM4 3、FIFO 1、税额 1；Maven reactor 9 模块成功 |
| H5 打包 | PASS，客户/工程师两种 mode | 仅构建，不代表登录后角色闭环通过 |
| PC Vite 生产打包 | PASS | 补齐网站配置后可生成产物；与完整 build 的类型门禁分开记录 |
| PC 真实类型检查 | FAIL，225 条诊断 / 102 文件 | 首次恢复真实检查为 235 条；修正本轮配置类型后为 234，API 边界清理再消除 9；本轮网站配置/插件配置未检出类型错误。正常 pnpm build 会受此门禁阻挡，不能称完整构建链通过 |

类型日志在本机 `.local/typecheck.log`，属于运行产物不入库。诊断集中于配置页、验证码、公共表格、角色权限、业务页面与接口类型契约；后续按公共根因分批修，不使用全局忽略或关闭检查规避。

性能基线（本机、非压测，不据此推断生产容量）：

- PC 配置加载旧实现 3 次 3371/1383/1558 ms，中位 1558 ms；新实现 1512/1613/1910 ms，中位 1613 ms。缓存和并发构建影响较大，**未证明启动时间改善**；按需加载仅作为已落地结构变化。
- 补齐配置页后的 Vite 生产打包两轮约 91 秒和 53 秒（缓存和并发负载不同，不能直接视作提速收益）；最大单个 JS chunk 约 4.10 MB，vue-vendor 约 1.28 MB。chunk 体积不等于首页实际下载量，需下一轮用浏览器请求链确认。
- 空闲容器单次采样：后端约 1.214 GiB、MySQL 529.3 MiB、Redis 12.95 MiB；这不是峰值/泄漏结论。
- 本机公钥接口 10 次样本，中位 19.7 ms、最大 35.6 ms；不代表报价、维修结算或导出接口的响应表现。

### 18.4 业务闭环验收矩阵

当前已完成源码定位和用例整理。下表为验收范围，均未冒充执行通过；需要专用角色账号、可撤销测试数据与必要测试证书。真实执行时按 Skill 核对状态、关联 ID、数据库/页面、数量/金额与权限，记录 PASS/FAIL/BLOCKED/NOT_RUN。

| 用例 | 业务主链 | 当前状态 |
|---|---|---|
| AUTH-01 | 登录、菜单权限、退出失效、越权拒绝 | NOT_RUN，L0 未登录拦截已另行通过 |
| PSI-01 | 采购入库→库存流水→应付→付款/核销 | NOT_RUN |
| PSI-02 | 销售出库→FIFO 成本→应收→收款/核销 | NOT_RUN，底层 FIFO/税额已单测 |
| EOMS-01 | 客户报修→PC 派单→工程师接单/维修→提交 | NOT_RUN |
| EOMS-02 | 维修结算确认→PSI 维修出库→应收→实际收款 | NOT_RUN；特别检查不把应收当实收，以及库存不足原子回滚 |
| QUOTE-01 | 报价编辑/提交→转工单→关联回查→Word/PDF | NOT_RUN |
| SIGN-01 | 授权项目/文件/印章→准备→签章→回执核验 | NOT_RUN，需测试印章/证书 |
| X-01 | 报价→EOMS→PSI→财务跨域闭环 | NOT_RUN |

每条都要补非法状态、跨部门/客户访问、重复提交和并发反例；财务及库存清理走应用撤销/冲销，禁止直接删流水。

### 18.5 三方向优化顺序与验收口径

| 优先级 | 方向 | 下一批工作 | 验收口径 |
|---|---|---|---|
| P0 | 可维护性 | 清理 225 条类型诊断的公共根因，先表格/接口类型与配置模型，再业务页；保持类型门禁遇错失败 | typecheck 真正归零，关键页面与域接口无回归 |
| P0 | 业务正确性 | 在专用测试数据下执行 QUOTE-01、EOMS-01/02、X-01，再扩展采购销售与签章 | 状态、关联、数量、金额、权限与回滚证据完整 |
| P1 | 业务操作 | 实测报价/维修结算的角色任务路径；核对已有自动带入，减少缺失字段的重复填写；低频动作收纳；共享基础资料选择 | 同一任务比较点击数、页面跳转数和重复录入字段，权限/审计不减少 |
| P1 | 前端性能 | 分析 4.10 MB chunk 和 Office/PDF/编辑器的真实加载路径，按需拆分；避免为了 chunk 数量而拆出循环依赖 | 同环境比较首屏请求字节、页面可交互耗时及业务页冷加载 |
| P1 | 代码组织 | 按闭环拆报价表单和签章执行页；把状态、计算和页面展示分离；盘点 ExcelUtil 真实调用后分批迁移 | 单文件职责减少、没有平行抽象、导入导出及核心单测通过 |
| P2 | 后端/数据库性能 | 对真实业务接口做多样本 P50/P95、SQL 次数、分页与批量查询检查；记录空闲/高峰内存与 GC | 同数据量、同机器和同缓存条件下比较，避免盲目降内存或合并模块 |
| P2 | 资料与工程组织 | 历史 SQL 备份和一次性草稿单独盘点；统一维护正式入口，不直接删除既有资料 | 无失效引用，构建 context/源码包减少且部署可重现 |

暂未执行功能删减、业务模块合并、历史数据清除或生产发布。继续优化时以本节基线和对应业务用例复验，不以减少文件数替代产品效果。


## 19. 类型归零、完整构建与业务闭环实测

日期：2026-11-05（北京时间）。本轮按用户要求先修复 225 条类型诊断，再执行整理好的业务流程；没有提交、推送或生产发布。

### 19.1 构建与类型修复

PC 完整 `npm run build` 退出码为 0：先由 `scripts/typecheck.cjs` 执行真实类型检查，再执行 Vite 生产打包，最终打包耗时 1 分 55 秒。225 条诊断已归零，保留 strict 与遇错失败门禁，没有以全局忽略规避检查。

修复集中在公共 Form/Table 的实例与插槽契约、Vue 默认值工厂、验证码坐标与父实例、配置模型和选项值、Axios 请求类型、接口 DTO 与枚举、可空数据处理和雪花 ID 字符串传递。CodeMirror 主题使用实际提供的显式 JS 模块路径，避免仅类型可见而运行时导出为空。现有约 4.1 MB 大分包警告仍存在，本轮不据此宣称首屏性能改善。

后端在 JDK 21 / Maven 3.9 容器中执行 `mvn -B -pl ls-aegis-boot -am -DskipTests package`，18 个 reactor 模块 BUILD SUCCESS，最终修复包耗时 3 分 53 秒。此 package 命令跳过测试；鉴权回归单测另行执行，2 个测试通过、0 失败/错误/跳过。Windows 挂载目录的 `.jar.original` 导致首次 repackage 失败，最终在容器原生文件系统构建后复制 jar，未将失败构建算作通过。开发后端已更新为最终修复包并重启。

### 19.2 实测发现并修复的问题

1. 请求身份串用：Spring 在拦截器 preHandle 抛异常或拒绝时不会调用该拦截器的 afterCompletion，线程复用可能留下上一用户上下文。SaExtensionInterceptor 现在在鉴权前清理，并在未放行的 finally 中清理；正常完成仍清理。2 个单测覆盖异常与正常放行；API 连续 20 轮交替受限账号、管理员、工程师验证隔离。
2. 重复签章：旧兼容 RepeatSubmit 注解没有执行切面，并发相同请求可重复成功。普通签章与合并签章复用平台 IdempotentHelper，按租户、用户、接口和请求指纹加锁，成功后保留 5 秒标记，失败允许重试。两个接口各自并发相同请求均仅一次成功，回执和签名有效。

上述约束已补充到项目 AGENTS 与业务测试 Skill，防止后续修改重新引入。

### 19.3 业务 API 测试结果

执行入口：`python -X utf8 scripts/quality/run_business_flows.py`。驱动创建专用 Docker 网络、临时 MySQL/Redis/后端和随机测试身份，不修改既有管理员或当前开发业务库。业务写入经过真实 API，SQL 仅用于测试身份准备和只读断言。最终结果时间为北京时间 15:02:25，**13 PASS / 0 FAIL / 0 BLOCKED**。

| 用例 | 实测与断言 | 结果 |
|---|---|---|
| AUTH-01 | SM2 登录、错误密码、角色权限、401/403、退出失效 | PASS |
| AUTH-02-CONTEXT-ISOLATION | 20 轮账号交替、拒绝请求后管理员和工程师身份不串用 | PASS |
| FIXTURE-01 | 真实 API 建客户、供应商、分类、商品、仓库 | PASS |
| PSI-01 | 入库 10×100、重试不重复、应付 1000、付款核销归零、超额拒绝 | PASS |
| PSI-02 | 合同销售 2×150、FIFO 成本 200、库存余 8/金额 800、缺货拒绝、收款核销 | PASS |
| QUOTE-01 | 兼容报价转工单链、总额 300、终态修改/重复转换拒绝、DOCX/PDF 导出与文本 | PASS |
| QUOTE-02-INDEPENDENT | 独立服务报价 50，草稿→提交→确认，无须工单，终态回退拒绝 | PASS |
| EOMS-01 | 非法提前提交拒绝、派工程师、其他工程师拒绝、定位、维修记录/材料、完工待结算 | PASS |
| EOMS-02-X-01 | 并发结算只成功一次、维修出库唯一、库存余 7、应收不记实收、超额拒绝、收款重试仅一条实收 | PASS |
| EOMS-03-STOCK-ROLLBACK | 缺货结算失败，结算仍草稿、工单仍待结算、库存不变、无应收与遗留出库单 | PASS |
| SIGN-01-AUTH | 未登录/无权限拒绝，完整请求中的非法项目/文件/印章拒绝 | PASS |
| SIGN-01-SIGNATURE | 测试印章、托管证书、普通与合并签章并发、回执、OpenSSL 独立内容签名校验、篡改后验证失败 | PASS |
| EOMS-01-CUSTOMER-H5 | 客户认证/验证码、伪造客户归属不生效、其他客户拒绝、报修→派单→维修→客户确认→结算关闭、退出失效 | PASS |

独立报价作为主流程；旧报价转工单接口仅作为现有兼容路径另测，不以测试结果推动恢复业务耦合。合成测试凭证、结果与构建日志位于被忽略的 `.local`，正式结论以本节为准。

### 19.4 覆盖边界

本轮为真实后端 API 与文件产物测试，未完成浏览器交互、布局或点击路径验收。PC 图形验证码只在专用测试数据库关闭，H5 验证码通过真实挑战接口生成并读取专用 Redis 中的答案，未验证图形识别体验。签章使用临时自签测试证书，OpenSSL 校验内容签名并拒绝篡改，未验证外部证书信任链或外部 TSA。没有调用真实银行、支付网关、邮件或短信服务。已覆盖工程师/客户归属与无权限拒绝，未穷举部门数据权限和多租户组合。13 组通过不代表全部业务情景或前端交互均已验证。


### 19.5 最终环境复检与清理状态

最终业务测试进程退出码 0，结果 JSON 记录 13 PASS。开发后端的最终包复制及重启命令已成功执行，但重启后的 L0 复检与临时环境清理未完成：工具自动审批两次超过期限，命令未启动；沙箱内替代检查无法访问本机 HTTP/Docker，不能据此判断服务失败。第 18 节的历史 9/9 不能替代本次重启后结果。

专用测试栈因 `--keep` 仍保留，清理入口为 `python -X utf8 scripts/quality/run_business_flows.py --cleanup`；清理前校验容器与网络的本轮标签，Docker 不可访问时保留 manifest，避免丢失记录。待具备权限后执行开发服务 `service_smoke.py`、响应误判 unittest 及清理。临时构建容器和本地构建缓存镜像也尚未清理；均未作为生产服务使用。当前状态不应表述为“环境已全部清理”或“本次开发服务 9/9 通过”。


## 20. 平台整体性能优化第一轮

> 本节为第一轮结果；代码检查、Nginx/运行验证及后续优化的最新结果见第 21 节。

日期：2026-11-05（北京时间）。用户要求在一个平台的前提下提高页面加载、运行效率和模块协作。本轮优先处理全站入口、公共表格、统一预览、工作台和报价材料匹配；保留已有工作区改动，没有提交、推送或生产发布。读取并使用项目 itls-optimize / itls-maintain 工作流，业务边界沿用 itls-business-test。

### 20.1 问题与已实施改动

| 路径 | 源码发现的问题 | 本轮落地 |
|---|---|---|
| 文件预览 | 列表挂载 FilePreview 时静态加载 PDF、Word、Excel 三套引擎 | 单一 FilePreview 入口，三个按需 Renderer；CSS 随类型加载，保留文件、Excel 配置、样式与事件透传 |
| 启动加密依赖 | encrypt.ts 引入完整 CryptoJS，实际只用 AES/ECB/PKCS7 与编码模块；公钥并发调用重复发请求 | 显式按需导入；只合并在途公钥请求，失败可重试，保持国密与验证码协议 |
| 全站 DOM 处理 | 任意节点/类变化触发整页表单扫描；逐 label 扫全部 id | 初次扫描后按变动表单区域合并处理，忽略纯时钟文本与 class 变化；修复自身写入期间暂停观察；按 id 定位；生成跨局部区域唯一标识，卸载取消帧 |
| 登录导航 | 用户验证、偏好、菜单串行等待；每次路由 GET 首页检查版本 | 用户验证后偏好与菜单并行；版本探测改 HEAD，每 60 秒最多一次，合并在途请求，失败不影响业务导航 |
| 统一工作台 | 报修卡和运维汇总各自请求相同概览；刷新 PSI 不同步这些卡片 | 父面板统一请求一次运维概览，两个卡片与关注条共用快照；PSI/运维并行并独立保留结果；维持各卡片权限判断 |
| 运营中枢/天气 | keep-alive 离开仍轮询；隐藏标签仍计时；初始业务 KPI 等待天气/定位；天气重复取数 | 离开、隐藏与卸载暂停定时器，恢复只启动一套；中枢请求不重叠；天气独立配置周期、并发合并；模拟天气不请求定位，业务 KPI 不等待天气 |
| 公共列表 | 快速搜索/翻页时旧响应覆盖新页，旧失败清空新数据；多选误检查 rowKey | 用请求版本保护最新数据、total、loading；修复检查 rowKeys，保留字符串 ID |
| 报价材料 | 回显和保存逐行 find/some 扫描全部商品；旧商品详情可能覆盖新选择 | 商品 Map 与回显 Set 将批量匹配由 O(材料行×商品数) 转为 O(商品数+材料行)；旧选择的返回不覆盖当前商品，不缓存库存 |
| 生产静态分发 | Nginx 未配置文本压缩和哈希资源长期缓存 | 增加 gzip、Vary、static 哈希目录缓存、HTML 协商检查；不存在的资源返回 404；保留安全响应头与 API 路由。配置尚未部署及通过 nginx -t，不能声称线上已生效 |

### 20.2 一个平台的关联边界

| 关联 | 本轮核对与保留的职责 |
|---|---|
| PC/H5→平台认证 | 沿用平台 Sa-Token、RBAC、国密与 R；本轮没有建立另一套登录或接口适配层。H5 源码未修改 |
| 工作台→PSI/EOMS/报价/签章 | 各域继续提供自己的 API；工作台负责编排展示，不以一个全权限聚合接口取代原有后端权限 |
| 报价→PSI 商品 | 复用 PSI 商品、价格、规格、库存选项；Map 仅用于当前页面匹配，不维护第二份库存或财务真值 |
| EOMS 维修结算→PSI | 保留 RepairStockOutApi 执行维修出库，维持 CAS、事务、库存不足回滚，不直接改库存表 |
| 结算→应收→实际收款 | 保留确认结算产生应收、实际核销才产生 Income 的口径；未为性能改写账务或削弱幂等 |
| 签章→平台文件/证书/权限 | 继续使用统一文件和授权能力；预览按需加载属于前端资源边界，不拆出签章独立系统 |

本轮未改后端业务、SQL、账务状态机或数据权限。发现 PSI 工作台 summary 仍拉取多组应收/应付/单据明细后在 Java 求和，库存预警也扫描未限定到监控商品的库存批次；这是下一轮有真实数据库指标后优先验证的热点，不在缺少执行证据时直接重写聚合或加跨用户缓存。

### 20.3 验证入口与口径

```powershell
# 仓库根目录：12 项行为回归，无需新增测试框架
node --test scripts/quality/frontend_performance.test.cjs
# PC 完整类型门禁与构建
cd ls-aegis-web
npm run build
cd ..
# 生产产物的页面静态依赖闭包，检查三个独立预览入口
node scripts/quality/frontend_assets.mjs --check
# 可选受控对照：恢复三引擎静态导入与 CryptoJS 全量导入，不改源码/正式 dist
node scripts/quality/frontend_preview_baseline.mjs
node scripts/quality/frontend_assets.mjs .local/perf-eager-preview --baseline
```

行为回归已执行 **12 PASS / 0 FAIL**：预览文件/配置/事件透传；表格旧响应与旧失败；报价字符串精度与商品选择乱序；工作台一次概览及 PSI 失败隔离；版本探测节流/并发/失败；AES 原始密文一致、SM3 标准向量、SM4 编码；公钥并发/重试及 SM2 解密；模拟天气不定位；天气可见性与生命周期；DOM 局部批处理与取消帧；局部字段唯一 id。测试使用真实 Vue/TS 与原加密库；API、定时器和 DOM 调度模型受控替代，不等于浏览器交互测试。

在含 5,000 个无关节点的受控 DOM 模型中，60 次时钟文本更新触发新增扫描 **0**；一次表单区域的 10 条变更合并成一帧，仅扫描该表单。此项证明扫描范围变化，不代表浏览器实测 FPS 或毫秒提升。版本检查的 20 次同周期调用只有一次读取；两个工作台卡片每次刷新只发一次概览请求，以上均有行为测试断言。

### 20.4 生产产物受控对照

最终 `npm run build` **退出码 0**，真实类型门禁和生产打包通过；对最终源码另行 `npm run typecheck` **退出码 0**。Vite 本次耗时 4 分 45 秒，受控对照耗时 3 分 24 秒；并发检查、缓存与机器负载不同，**不将这两个耗时作为提速对比**。大 chunk 警告与 sm-crypto 的浏览器 crypto 外部化提示仍存在，未关闭检查或抬高门槛消除提示。

对照以本轮相同源码和配置构建，仅由测试插件恢复旧版三预览引擎静态加载与 CryptoJS 全量导入；正式源码和 dist 不被对照覆盖。两份 manifest 都包含相同平台 Layout，并按静态依赖去重统计 JS/CSS。结果单位 **KiB**：

| 入口/页面 | 原始字节：对照 → 优化后 | 减少 | gzip level 5 离线估计：对照 → 优化后 |
|---|---:|---:|---:|
| 应用入口 | 2343.3 → 2292.6 | 2.2% | 600.4 → 580.7 |
| 运营中枢 + Layout | 3203.3 → 3152.6 | 1.6% | 889.4 → 869.8 |
| 报价表单 + Layout | 2524.1 → 2473.4 | 2.0% | 664.3 → 644.7 |
| 签章执行 + Layout | 2887.6 → 2836.9 | 1.8% | 769.2 → 749.5 |
| 文件管理 + Layout | 7649.6 → 3581.3 | 53.2% | 2086.5 → 1001.5 |

文件管理的初始 JS/CSS 依赖约减少 **53%**；该页 gzip 离线估计约减少 **52%**。应用入口约减少 51 KiB 原始依赖；其他页面的资源收益主要来自按需加密导入。资源检查 **PASS**：三个 Renderer 为独立异步入口，所列页面的静态依赖均不引用这些入口。第一次打开某类文件仍需下载相应引擎，未实测其打开耗时，不能把依赖减少比例直接称为页面速度提升比例。

本表不含图片、字体、API、worker、CPU 与浏览器缓存效果；各行共享资源，不可相加。gzip 为估计，不能替代部署后的响应头与实际传输量。正式证据是忽略目录 `.local/frontend-assets-{baseline,after}.json`、`perf-build-final.log`、`perf-typecheck-final.log`、`perf-controlled-baseline-build.log` 与 `frontend-perf-tests.log`；复现脚本已纳入工程。Nginx 配置尚待 Docker 权限恢复后 `nginx -t` 和响应头验证。

### 20.5 验收边界与后续热点

本机 HTTP/Docker 基线与复测两次因自动审批超时未执行，沙箱不能访问宿主服务；当前无法给出浏览器 FCP/LCP、交互耗时、API P50/P95、Nginx 实际压缩头或新一轮跨域 API 闭环结果。第 19 节 13/13 为上一轮已执行结果，不能当成本轮浏览器回归。临时测试栈与构建容器的既有清理状态仍见 19.5。

下一批需要在可访问的测试环境下验证：真实首屏请求链与 Arco/global CSS 占比；上述 PSI summary 的 SQL 聚合、扫描行数和数据权限；报价大数据量与大 PDF 的输入/渲染耗时；PC/H5 同一业务数据的回显与导航路径。本轮不宣称全项目性能问题已全部解决，不以文件数或构建时间替代页面体验指标。


## 21. 代码检查 Agent 与第二轮优化验收

日期：2026-11-05（北京时间）。本轮继续优化并实际创建、执行一个独立代码检查 Agent，保持一个平台的鉴权、权限、文件、库存与财务链路。未提交、推送或生产发布。前几轮的运行权限阻碍已在本轮部分恢复；以本节最新实测为准。

### 21.1 Agent 与 Skill 已创建并执行

- 角色规范：`agents/code-review/AGENTS.md`，根 AGENTS.md 和 README 已加入入口。
- 项目 Skill：`.agents/skills/itls-code-review/SKILL.md`，附 agents/openai.yaml，可使用 `$itls-code-review`；已通过 skill-creator 格式校验。
- 独立 Agent 实际读取并执行该 Skill，两轮只读审查；主 Agent 统一修改，避免同一工作区互相覆盖。角色文件不是额外运行服务，也没有安装到用户全局配置。
- 第一轮审查复现四项缺陷；修复后第二轮复核及额外交错请求验证未发现新的需修复问题。独立日期模型交叉核对不作为 Java/MySQL 集成测试数量。

### 21.2 已修复的真实缺陷

| 问题 | 触发与影响 | 修复与回归 |
|---|---|---|
| 报价 A→B→A 选择乱序 | 最旧 A 最后返回时 ID 仍相同，旧价格 999 覆盖最新 150，数量 2 的金额从 300 变成 1998 | 按材料行 WeakMap 记录请求版本；既防同 ID 回切也保持不同行独立；旧失败不重新计算新行 |
| 趋势筛选与轮询竞争 | 当前已选 30 天，旧 7 天请求或轮询晚到覆盖当前图表 | 手动与轮询共用版本与日期快照；失败保留最近有效趋势；补手动/轮询交错及筛选反例 |
| 健康状态误报 | 无权限占位 Promise 成功被算成真实服务可用，所有实际请求失败仍显示可用 | 健康判定只统计真实授权请求，保留部分域成功的语义；补全部实际请求失败反例 |
| 预览异步 chunk 失败一直转圈 | 引擎尚未加载时不会发组件 error 事件，原逻辑等待约 60 秒才结束 | 三种预览入口统一处理异步 loader onError，立即停止 loading、清理定时器并提示关闭后重试；真实 Vue SSR 受控验证 |

### 21.3 PSI 工作台查询优化

- 应收、应付只投影 unwritten_amount，草稿回款与未匹配银行流水只投影 amount，保留原计数、空金额与 BigDecimal 求和语义。
- 本月与近 30 天高度重叠，出/入库单据头由各两次读取改为各一次，**单据头查询 4→2**；共同窗口只读取 id、confirm_time、tax_inclusive_amount，再分桶，明细数量仍走原 Mapper。
- 预警商品只读取展示及阈值字段；库存批次限定到当前监控商品集合，避免读取无关商品批次；空监控集合不访问批次。
- 保留原 selectList 数据权限与租户调用链。检查发现 DataPermissionMapper 未给 selectMaps 覆盖同一注解，故没有为了 SQL 聚合直接改用它，也没有新建跨用户缓存或另一套库存真值。
- 没有修改账务状态机、CAS、库存写入、应收/实收规则、SQL schema 或既有迁移。本轮量化的是读取次数与字段范围，不冒充 API P95 或生产负载收益；大规模数据的数据库端聚合仍需单独验证权限与执行计划。

### 21.4 本轮真实验证结果

| 检查 | 最终结果 | 证据与范围 |
|---|---|---|
| PC 完整构建 | PASS，退出码 0 | npm run build，真实类型门禁 + Vite；Vite 4分53秒，不据此声称构建提速 |
| 前端行为回归 | PASS，18/18 | 原 12 项 + 本轮 6 项，真实 Vue/TS/加密库，受控 API 与生命周期；不是浏览器操作验收 |
| 产物依赖检查 | PASS | 仍有 3 个独立异步预览入口，受检页面不静态引用引擎；受控基线脚本同步适配新入口 |
| JDK 21 后端完整构建 | PASS，18 模块，31/31 单测 | mvn -o -B -pl ls-aegis-boot -am package，未跳过测试；13 个测试类、0 失败/错误/跳过；7分30秒 |
| 工作台针对性单测 | PASS，5/5，包含于上述31项 | 金额精度/空金额、月初与31日月份窗口、批次范围与跨批次求和、空集合不查询；断言单据头只读取一次 |
| 独立业务 API 闭环 | PASS，14/14 | 全新临时 MySQL/Redis/JRE21 后端，候选 JAR；原13组及新增 PSI-03-WORKBENCH |
| 工作台 API/数据库回查 | PASS，20字段 | 新用例使用独立 MySQL 只读聚合核对金额、笔数、单据及明细数量；按测试 JVM Asia/Shanghai 日期取窗口 |
| Nginx 配置与真实 HTTP | PASS，7/7 | 临时 nginx:1.27.0 + 只读 dist；配置、gzip/Vary、缓存、安全头、worker MIME、缺失资源404、SPA、HEAD/ETag；临时容器已自动移除 |
| 开发服务只读冒烟 | PASS，9/9（更新前、更新后均通过） | PC/H5/后端及代理平台响应，不创建业务数据 |
| Skill 校验与差异检查 | PASS | quick_validate 与 git diff --check；仅既有 CRLF 提示 |

Nginx 实际测试中入口 JS 455,483 bytes 压缩为 167,354 bytes，解压内容与产物逐字节一致。这是该文件在临时服务器的传输结果，不是整页加载耗时或生产生效结论。Nginx 配置未发布生产。

本机先用 JDK17/release17 做离线基线及 7 项针对性测试；Mockito 自动附加代理等待后中止的那次不算通过，预加载已有 Byte Buddy 测试代理后通过。最终以独立 JDK21 容器的完整31项测试和候选 JRE21 业务闭环为准，未降低项目运行时版本。

新增复现入口：`scripts/quality/verify_nginx.py`、`package_backend.sh`；业务驱动支持 `--jar-dir .local/backend-candidate`，先验候选包再更新开发服务。证据保存在被忽略的 `.local/review-jdk21-build.log`、`review-full-build.log`、`review-frontend-tests.log`、`nginx-check-results.json`、`business-results.json`；上一轮13组结果已另存 round19 文件。正式结论以本节为准。

### 21.5 覆盖边界与收尾

14 组为真实后端 API 与文件产物测试，未完成浏览器点击、布局、首屏 FCP/LCP 与交互耗时。PC 验证码仅在独立测试库关闭；H5 验证码读取独立 Redis 答案；签章使用自签测试证书，验证内容签名与篡改拒绝，不验证外部信任链。没有调用真实银行、邮件、短信或外部 TSA，未穷举多租户/部门权限组合，也未完成大数据量 SQL 执行计划和压力测试。

收尾完成：已核对开发后端的项目挂载路径，备份旧 JAR 后换入通过验证的候选包，SHA256 一致，开发后端重启成功，更新后只读冒烟 9/9 通过。临时业务 MySQL/Redis/后端栈、临时 Nginx 与已成功退出的构建容器均已清理，原开发 MySQL/Redis和其他服务未变。候选与回退 JAR 保留在忽略目录 .local 供复核，不进入 Git；未发布生产。


## 22. 在用环境分阶段测试首批执行

日期：2026-11-05（北京时间），运行号 A91D。目标由用户确认为本机 PC5173/API8000、H5 5174/5175，在用实例当前仍为 dev 配置，不冒充公网生产。用户已授权必要测试及测试数据清理，后续不重复索取相同授权。

### 22.1 执行规范与标准边界

项目 Agent 位于 agents/staged-test/AGENTS.md；Skill 为 .agents/skills/itls-staged-test，含 references/acceptance.md 与 standards.md，已通过格式校验。此前独立 Agent 已确认全部17个后端子模块和三端的归属，补齐无独立页面的底座适用性及有证据的 N/A 退出规则。标准参考 GB/T 38634 系列、GB/T 25000.10-2016、适用时的 GB/T 25000.51-2016、GB/T 39788-2021，来源与适用边界见 Skill；不声称认证或未读正文的逐条符合性。

阶段顺序为：平台底座→认证/RBAC→租户→文件→消息→开放能力→调度→PSI→报价→EOMS→签章→三端，全部必测模块通过后才进行跨域联合验证。旧隔离环境14组结果属于第21节历史证据，没有重定向隔离夹具到在用数据库，也不把它们当成本轮结果。

### 22.2 首批实际结果与修复

| 检查 | 本轮结果 | 证据/边界 |
|---|---|---|
| 服务只读冒烟 | PASS，9/9 | 当前 PC/H5/API 与代理平台响应，不创建业务数据 |
| 模块匿名入口检查 | PASS，12/12 | scripts/quality/live_module_preflight.py 串行执行；公共公钥入口与各域未登录拒绝。鉴权可先于路由匹配，401/403不证明端点存在或模块完整通过 |
| 内置浏览器登录态 | 已验证 | 当前可直接进入工作总览，显示超级管理员；读取用户管理页，未读取凭据、未重置账号、未切换既有会话 |
| 部门空名称 | PASS | 空名称被表单拒绝 |
| 顶部新增部门 | 原FAIL，修复重测PASS | 原表单隐藏空 parentId，但服务端要求非空；填写后返回“上级部门不能为空”。修复为空父值的新建显示选择器，缺父选择在前端拒绝 |
| 深层父部门识别 | 单测PASS | 使用 findTree 替代仅根层 find，防止编辑时误隐藏合法嵌套父部门；实际深层树交互不冒充已执行 |
| 部门新增/编辑/清理 | PASS | 只创建运行号合成对象，指定总公司为父，新增、编辑均显示成功；清理后页面恢复原两条部门，独立SQL确认逻辑删除 |
| 前端回归 | PASS，19/19 | 原18项及部门选择器回归，真实 Vue/TS 受控测试 |
| 完整构建/资源边界 | PASS | npm run build 退出0，Vite 3分36秒；3个独立异步预览入口仍通过检查；不以构建耗时声称性能收益 |

修复文件：ls-aegis-web/src/views/system/dept/AddModal.vue；回归代码为 scripts/quality/frontend_performance.test.cjs。修复通过在用开发前端热更新并经浏览器重测，未发布其他环境。

### 22.3 清理回查与未完成范围

本轮唯一新增业务对象：sys_dept.id=908792423415545885，父部门1，运行号标识 ITLS-TEST-A91D。新增时 deleted=0；经业务界面清理后 deleted=本行ID，这是平台逻辑删除口径（非物理抹除）。原部门仍可见，未给测试部门关联用户、角色、库存、账务或业务单据。保留正常审计，不通过删审计“清场”。本轮没有新增其他模块测试对象。

脱敏台账与证据：.local/live-staged-A91D.json、live-a91d-dept.json、live-module-preflight.json、live-a91d-build.log、live-a91d-department-cleanup.jpg，均不入库。清理截图为在用实例实际页面。

当前模块状态：底座可用性单项已完成，完整阶段未宣告通过；认证/RBAC 的部门子项通过，多角色、跨部门/本人越权、完整登录/退出及强制改密流程仍缺适用会话/前置，标记 BLOCKED，不能靠修改原账号或借用历史凭据绕过。阶段03–12完整验收 NOT_RUN，已做匿名前置的部分不转成模块PASS。联合测试 NOT_RUN，因为尚未满足全部模块退出条件。本节是首批执行进展，不是“全项目测试完成”。

## 23. 全模块覆盖盘点与在用续测结果

日期：2026-11-05，北京时间；运行号 R23。目标仍为用户指定的本机 PC5173/API8000、客户H5 5174、工程师H5 5175。执行项目 itls-staged-test / itls-business-test Skill，沿用第22节标准框架与边界；本节不是国家标准认证报告。

**结论：全模块与联合验收尚未完成，不具备“全部通过”的证据。** 本轮实际完成文件上传/元信息/回收、公告草稿持久化及只读回归；发现文件下载失败。随后浏览器明确拒绝本站来源授权，未能继续页面写操作。没有可用的独立 API 测试身份，不能用匿名入口检查替代受保护业务测试，也没有读取浏览器 Token、重置原管理员、修改验证码或重定向隔离夹具。

### 23.1 版本及证据口径

- Git HEAD：995c7356bb37c869666ebfb860822878be8ee0a1；工作区存在前轮未提交修改，HEAD 本身不代表完整源代码快照。
- 本地后端 target JAR SHA256：af3bc12375100c43285221d37d2d3bea26ec513bf82759e6ae3ebb9b484fad2c。此为本地文件指纹，本轮没有重打包、替换或重启后端，未另行证明容器内文件逐字节一致。
- 本轮未修改产品代码/配置，未提交或推送；19项前端行为测试为本轮重新执行。第21节的31项后端单测、14组隔离业务闭环和构建结果仍属于历史证据，没有重新标成本轮执行。
- 台账：.local/live-staged-R23.json；精确ID与独立SQL回查：.local/live-r23-readback.json；回查脚本只执行限定本轮标记的 SELECT，不输出连接凭据。匿名入口详情：.local/live-module-preflight.json。

### 23.2 本轮实际执行用例

| 用例 | 预期与操作 | 实际结果 | 状态 |
|---|---|---|---|
| R23-BASE-01 | 四个服务及代理公共接口可用，检查 HTTP 与 R | 9/9，保留正常访问审计，无业务写入 | PASS |
| R23-AUTH-01 | 公钥可访问，其余模块入口匿名拒绝 | 12/12；鉴权可能先于路由匹配，不证明端点存在或模块完整通过 | PASS，限匿名边界 |
| R23-PC-01 | 列表乱序、报价回显/精度、趋势、权限聚合、公钥、预览、生命周期与部门回归 | node --test scripts/quality/frontend_performance.test.cjs；19/19，0失败/跳过 | PASS，受控行为测试 |
| R23-FILE-01 | 初始文件列表为空，上传唯一合成TXT | ITLS-TEST-R23.txt，89字节；页面共1条；独立SQL确认ID与大小 | PASS |
| R23-FILE-02 | 详情可回显原名、路径、大小及SM3 | 详情显示 local_dev；SM3=1ef5c3f3508b64ed4b1a021c498bd8a1135c94508626350ab5c50f7838cef78a；未另做下载字节校验 | PASS，限元信息 |
| R23-FILE-03 | 下载产生可验证文件 | 等待下载事件10秒超时；浏览器错误为 TypeError: Failed to fetch | FAIL |
| R23-FILE-04 | 仅回收本轮文件，活动列表恢复基线 | 活动列表0条/0 Bytes；回收站显示本轮唯一文件，SQL deleted=1 | PASS，逻辑回收 |
| R23-NOTICE-01 | 不发布、不广播，保存合成公告草稿 | ITLS-TEST-R23-仅草稿；列表从20变21；SQL status=1、deleted=0；投递日志数量0 | PASS，限草稿持久化 |
| R23-NOTICE-02 | 删除本轮草稿，恢复公告基线 | 点击本轮行“更多”前，浏览器来源授权明确拒绝；未执行删除 | BLOCKED |
| R23-DIFF-01 | 差异无空白错误 | git diff --check 退出0，仅既有CRLF提示 | PASS |

公告空正文时未观察到持久化成功，填写正文后才成功；本轮没有将该过程定义为已验证的完整参数边界用例。没有点击“发布”，未通知真实用户。

### 23.3 缺陷与执行阻塞

**D23-FILE-01：在用文件下载不可用（未关闭）。** 复现：上传合成TXT→右键下载；实际 Failed to fetch，未收到下载文件。只读回查得到该文件存储域名 http://localhost:8080/api/file/，本机8080端口未查询到监听。域名/端口不匹配是当前明确的配置疑点，与失败现象一致；尚未取得完整网络响应，不能排除跨域等共同原因。不要直接修改所有历史URL或放宽跨域作为修复。需要按实际部署确认可访问的存储域名与代理，再以新合成附件验证预览、下载字节和指纹。

**D23-FILE-02：文件数量显示异常（未关闭）。** 上传1个文件后侧栏“数量”显示00:00:00，列表共1条；清理后显示0。后端统计字段为Long，前端直接赋给a-statistic，需检查字符串序列化及数值归一化。此项是实际显示异常及静态定位线索，不冒充已修复。

**B23-BROWSER-01：浏览器来源权限被拒绝。** 操作本轮公告行按钮时，浏览器明确返回本站来源授权未获允许且用户拒绝。已经停止浏览器操作；未改用其他浏览器、隐藏会话、原始协议、后台写库或其他路径绕过。只读服务检查和本轮数据状态回查属于后续较低风险核验，不执行被拒绝的草稿删除。

### 23.4 全模块覆盖与剩余必测项

已从父POM核对17个子模块；下表覆盖全部归属。表中状态指完整在用阶段验收，不将子项通过推导为整阶段通过。默认串行并发1，无压力或故障注入。

| 阶段/模块归属 | 当前本轮证据 | 完整验收状态与剩余内容 |
|---|---|---|
| 01 boot/common/starter/mybatis/redis/crypto | 服务/公钥/代理可用；前端加密向量与公钥并发测试通过 | BLOCKED：完整异常链、运行配置/监控/回退验收未齐 |
| 02 security/rbac | 管理员会话可进入文件/公告；匿名拒绝；部门见第22节 | BLOCKED：登录退出、多角色/部门/本人越权、失败后身份隔离实测未齐 |
| 03 biz-tenant | 匿名拒绝；源码默认租户中间件关闭 | BLOCKED：尚未核实运行时覆盖值，不能仅凭源码默认记N/A；启用时需双租户隔离 |
| 04 biz-file | 上传、详情、回收通过；下载失败 | FAIL：D23-FILE-01/02未关闭，预览/越权/引用/失败重试未完成 |
| 05 biz-notice | 草稿持久化通过、投递0 | BLOCKED：草稿清理、专用接收人发布/已读/越权未完成 |
| 06 biz-open | 匿名拒绝 | BLOCKED：应用凭据、无效签名与撤销的认证用例未执行；未新建访问凭据 |
| 07 biz-schedule | 匿名拒绝；源码SnailJob默认关闭 | BLOCKED：运行状态未核实，不据源码记N/A；无副作用任务的异常/重试/幂等未执行 |
| 08 biz-psi | 匿名拒绝；受控工作台前端行为通过 | BLOCKED：资料→采购→FIFO库存→销售→应收应付→收付款核销→冲销及金额/净量断言未执行 |
| 09 biz-quote | 商品回显、字符串ID、A→B→A竞态受控测试通过 | BLOCKED：在用报价状态/金额、终态限制、DOCX/PDF及兼容转单未执行 |
| 10 biz-eoms | 匿名拒绝；统一概览受控测试通过 | BLOCKED：报修/派单/维修/PSI出库/结算/应收/实收、客户和工程师权限未执行 |
| 11 biz-sign | 匿名拒绝 | BLOCKED：在用合成证书/授权/普通与合并签章/回执/篡改/双请求幂等未执行；没有借用真实印章 |
| 12 PC/客户H5/工程师H5 | 三端HTML及公钥代理可用；PC局部交互如上 | BLOCKED：三端角色闭环、刷新与异常反馈、设备适配、FCP/LCP/交互耗时未完成 |

监控、系统配置、字典/菜单、代码生成归平台/RBAC；本轮没有执行其管理写入或源码生成。性能验收指标未给定，不编造国家标准固定耗时或并发门槛。源码测试类数量盘点在台账中，仅为覆盖发现线索，不等于已运行或覆盖充分。

### 23.5 联动测试判定

以下四条完整联动均为 **NOT_RUN**：

1. 客户/商品/仓库→报价/兼容工单→PSI维修出库→维修结算应收→实际收款→工作台。
2. 采购→库存FIFO→合同销售→应收应付→核销→汇总→冲销。
3. 平台文件→项目/测试印章授权→签章→回执/验证/下载→附件引用处置。
4. PC派单→客户H5报修/确认→工程师H5维修→PC结算关闭，并验证跨角色访问拒绝。

原因：itls-staged-test/SKILL.md规定“全部必测模块通过后才执行联合测试”；当前存在下载缺陷、模块必测缺口及浏览器权限阻断。没有绕过阶段退出条件，更没有将历史隔离14组结果挪作本次在用联合验收。

### 23.6 清理、残留与恢复执行顺序

| 对象 | 精确ID/标记 | 最终回查 | 处置 |
|---|---|---|---|
| 合成附件 | sys_file.id=908798053845041260；ITLS-TEST-R23.txt；89字节 | deleted=1；活动列表0，回收站1 | 已逻辑回收，尚保留可恢复文件，未永久清空回收站 |
| 合成公告 | sys_notice.id=908798778079707254；ITLS-TEST-R23-仅草稿 | status=1，deleted=0；sys_notice_log匹配数0 | 草稿仍在，清理BLOCKED；未发布、未投递 |

本轮没有创建客户、商品、仓库、库存/财务单据、签章、应用凭据或测试账号；没有实际支付、短信、邮件、正式签章及库存变更。不能因此宣称所有模块的清理都已验证。保留正常上传、回收和草稿审计。

恢复本站浏览器来源权限后，先核对上述ID与运行号，通过应用清理草稿；回收附件的永久移除另遵循相应确认要求。随后修复并重测下载配置和统计显示，取得各角色可用测试身份，按阶段补齐上表必测内容。所有适用模块退出后，才用全新运行号执行四条联动并按依赖逆序补偿清理。无需重新确认已明确的测试目标及业务范围。

截图证据：.local/live-r23-file-recycle.jpg；显示活动文件清零和本轮附件回收记录。本报告截至本次权限阻断后的独立回查；未提供“全项目测试完成”或“测试数据全部清理”的失实结论。

## 24. 下载与数量修复及续测复核

日期：2026-11-05；运行号 R24，仍为本机5173/8000及H5 5174/5175。用户再次确认放开权限并要求修复、清理和完成全部测试。本轮已完成两个缺陷的代码修复与自动回归；**在用浏览器功能重测、剩余数据清理和全模块联合验收仍未完成**，以下不冒充完成报告。

### 24.1 修复与平台关联

1. **文件下载调用错误路径。** 文件页原来直接使用sys_file关联存储URL，当前local_dev域名仍为localhost:8080；a标签/裸fetch还不能携带平台Authorization。改为直接导入已有平台common.downloadFile，调用 `/system/common/file/{id}`，复用统一http实例的Bearer/租户上下文与useDownload。后端已有FileService.download先验证FileAccessService再输出二进制，因此没有增加另一套下载后端、放开/file路径、将Token拼到URL或发送给任意存储域名。文件ID完整保留为字符串，下载保留原名；移除未验证响应就显示“下载成功”的提示和不必要的列表刷新。本轮没有改存储域名或历史URL，也没有修复所有预览直链场景。
2. **统计Long字符串被当作时间。** 后端FileStatisticsResp.number/size实际可能是字符串，Arco Statistic会把非数字值按HH:mm:ss格式化；字符串零还会使格式化大小出现NaN。只在文件统计显示边界转成有限非负数字，统计/饼图数值一致处理；接口类型修正为number|string，文件、用户、租户等ID没有转Number。真实Arco SSR断言数量12显示为12，不出现00:00:00。
3. **错误响应不再下载成文件。** 通用downloadByUrl先检查HTTP状态和明确平台R失败，再生成下载链接；同源/跨源都检查，不向外部URL附加平台Token。失败不生成文件，链接与ObjectURL在成功或点击异常后清理。统一http的JSON Blob分支允许普通JSON文件和非平台内容，仍拒绝success=false且包含code的平台错误；不再仅因缺少success字段就拒绝合法文件。

改动：ls-aegis-web/src/views/system/file/main/FileMain/index.vue、FileAsideStatistics.vue；src/apis/system/type.ts（仅统计契约）、src/utils/downloadFile.ts、src/utils/http.ts。受控用例位于scripts/quality/file_resource_contracts.test.cjs和frontend_performance.test.cjs。本轮未修改后端产品代码、数据库或运行配置，不提交、不推送。

### 24.2 本轮实际验证

| 检查 | 结果与范围 |
|---|---|
| 文件专项契约 | PASS，6/6；真实Arco SSR、Long字符串/零值、同源跨源401/403/404/500、HTTP200下R错误、合法JSON/二进制、网络与点击异常及URL释放 |
| 前端行为回归 | PASS，21/21；原19项及文件页→鉴权下载API链、实际http Blob响应分支；长ID/文件名/失败不落盘。两套合计27项，0失败/跳过 |
| PC完整构建 | PASS，最终npm run build退出0；类型门禁和Vite均通过，Vite 2分19秒。复核发现code对象合法JSON边界后已补反例、收紧判定并重建；不据此声称实际页面加载提速 |
| 产物依赖检查 | PASS，frontend_assets.mjs --check；仍有3个Office独立异步入口 |
| JDK21完整后端构建/单测 | PASS，本轮重新执行mvn -o -B -pl ls-aegis-boot -am package；父项目及17子模块共18项SUCCESS，13个测试类31/31，0失败/错误/跳过；10分42秒。属于构建/单测，不是全部模块业务验收 |
| 当前服务只读冒烟 | PASS，9/9；没有登录后业务写入 |
| 匿名权限入口 | PASS，13/13；新增平台文件流接口匿名拒绝；不证明所有路由及完整功能通过 |
| 浏览器真实下载/数量重测 | BLOCKED；两次读取本站页面均被已保存权限或托管策略拒绝，未能观察修复后的在用页面 |
| 剩余数据清理 | BLOCKED；只读回查如下，无新的业务对象 |
| 全模块及四条联合验收 | BLOCKED / NOT_RUN；第23.4、23.5节剩余用例未因单测或构建通过而转PASS |

证据：.local/r24-frontend-tests.log、r24-pc-build.log、r24-assets.log、r24-assets.json、r24-smoke.log、r24-preflight.log、r24-backend-tests.log；源码SHA256、目标及执行分类写入.local/live-staged-R24.json。没有用构建或SSR代替真实浏览器验收。

后端独立构建容器使用只读源码挂载，构建和测试在容器/build中完成，没有覆盖在用JAR或重启在用服务。按运行标签R24及exited 0双重核对后，已提取JUnit XML到.local/r24-surefire并汇总为r24-backend-summary.json，随后仅移除本轮临时构建容器itls-r24-build。原MySQL、Redis、后端和媒体服务未停止；未运行隔离业务建库脚本。差异检查通过，仅既有CRLF提示。

### 24.3 权限复核和残留变化

用户报告“权限已经放开”后，尝试通过原in-app浏览器读取已有标签页；工具明确拒绝对 `http://127.0.0.1:5173` 的页面观察，原因是saved user permission setting或managed policy拒绝来源访问。用户再次告知开启后再读，仍相同拒绝。文件系统和命令执行已可用，但未由此推断浏览器站点权限也已生效。没有另开浏览器或用隐藏状态、原始协议、后台写入绕过拒绝。

本轮只读SQL限定上轮唯一文件名与公告标题；没有执行发布或删除。最新回查：

| 对象 | 精确ID | R24状态 | 清理结论 |
|---|---|---|---|
| ITLS-TEST-R23.txt | 908798053845041260 | sys_file.deleted=1，89字节，local_dev | 上轮已回收，可恢复副本仍保留；未永久清空 |
| ITLS-TEST-R23-仅草稿 | 908798778079707254 | sys_notice.status=3、deleted=0；关联notice_log数量0 | 与R23记录status=1相比已变为已发布；本轮没有发布，变化来源未确定，不把它继续称作草稿，也不直接强删 |

投递日志为0不证明公告未被其他页面展示或无人看到。第23节描述的是当时的草稿状态；本节记录后续变化，两者不合并成“仍未发布”。R24新回查独立保存在.local/live-r24-readback.json；R23原快照从其原台账保留，不用新状态覆盖历史证据。

恢复浏览器来源授权或取得正常的独立API测试身份后，需先核对公告当前状态与引用，经应用支持的撤回/删除完成清理，再重测上传→统计→鉴权下载字节/SM3→回收。随后按第23.4节补齐角色、状态/金额/库存和异常必测，所有模块退出后执行第23.5节四条联动，并按业务补偿清理。此次没有重新询问已授权的目标或操作范围；上述未执行项目仍如实保留。

## 25. 浏览器来源权限专项诊断

日期：2026-11-05，运行号R25。用户再次确认放开权限，要求自行排查。本轮定位拒绝所在层级，没有修改浏览器安全策略或项目产品代码，没有新增测试数据。

| 检查 | 实际证据 | 判断 |
|---|---|---|
| 原内置浏览器连接 | 同一iab浏览器可列出当前两个真实标签，路径分别为/user/message?tab=notice与/dashboard/workplace；标签ID已由前轮4变化为5、6 | 浏览器连接与标签清单可用；未复用失效标签4 |
| 当前页面读取 | 对当前标签6执行文档支持的getTab；在返回任何页面内容前被拒绝 | 拒绝来源为http://127.0.0.1:5173的origin approval gate，不是已观察到的项目401/403、登录失效或下载接口错误 |
| 拒绝原因 | 工具明确返回saved user permission setting或managed policy denies access | 只能定位为已保存权限或托管策略；现有返回不提供具体规则ID，不能断言是哪条规则或哪一设置修改未生效 |
| 普通插件权限诊断 | get_app_permissions(unified-computer-use)返回not_installed；全局普通插件权限为Allow low-risk actions | 该查询未取得内置浏览器本站例外规则；不将not_installed误判为内置浏览器不存在，也不将普通插件默认权限当作本站授权 |
| 官方说明 | OpenAI文档将内置浏览器站点访问置于Settings > Browser；网站来源审批与执行器网络权限独立 | 关闭文件系统沙箱、允许命令或修改项目RBAC均不能证明站点origin审批已放行 |

官方来源：[使用电脑与浏览器](https://developers.openai.com/training/walkthroughs/using-your-computer-and-browser)、[Computer use来源授权说明](https://developers.openai.com/api/docs/guides/agents-api/tools/computer-use)。API文档用于说明审批层的独立性，不据此宣称当前桌面应用的内部实现、配置文件位置或已知缺陷；未找到能够证明“清缓存/重装/重启一定解决”的官方依据，未执行这些操作。

结论：来源权限仍被宿主拒绝，当前工具没有提供查看或撤销这条浏览器来源拒绝规则的接口。不能由代理自行删除宿主权限文件、改策略、换localhost/端口/浏览器、提取会话或通过其他控制路径实现被拒绝的操作。恢复需要宿主实际解除针对精确来源http://127.0.0.1:5173的拒绝；若由组织托管，需其管理方处理。用户已给业务范围授权，无需再重复询问是否允许测试，但聊天中的授权未替代工具层实际审批结果。

清理与模块/联动执行仍BLOCKED/NOT_RUN，沿用第24节最后一次数据库回查；本轮没有再次查库，因此不把旧状态标为当前已复核。前端27项、后端31项与完整构建仍为第24节已完成证据，本轮没有重复运行或增加通过计数。未完成附件永久清理及公告处置，也未将联动标PASS。

追加复核（R26，2026-11-05）：用户明确表示已在浏览器设置后，重新列出原iab浏览器标签，确认当前5、6仍存在，再对当前标签6执行getTab。来源http://127.0.0.1:5173仍在返回页面内容前被saved permission or managed policy拒绝，随后结束浏览器操作。工具未提供具体命中的规则或设置同步状态，不能断言用户未操作设置，也不能声称权限已恢复。本次没有清理写入、没有新增测试数据、没有再次查库或运行模块联动；残留状态仍须恢复访问后重新核对。没有新增PASS计数。

## 26. R27 正常登录接口实测与修复部署

### 26.1 范围、版本及总判断

执行日期：2026-11-05，结束时间22:34（Asia/Shanghai）。目标为用户确认的本机API `127.0.0.1:8000`、PC5173、客户H5 5174、工程师H5 5175，后端实际为 **dev profile**，不能等同公网生产验收。用户提供可用管理员身份后，使用公开图形验证码和正常国密登录；另经用户管理API建立5个合成角色账号，执行初始密码强制修改及正常二次登录。没有提取浏览器会话、读取验证码答案缓存、关闭验证码或改动原管理员密码。本轮接口通路已经恢复，第25节的“所有业务因浏览器阻断”不再代表当前状态；浏览器页面交互依然未执行。

**本轮完成多模块接口实测、三项故障修复、最终版本部署及数据清理回查；未达到“所有模块完整验收及最终联合测试全部通过”。** 财务确认/冲销缺口仍阻断完整联合退出，不能用已通过的单模块、只读工作台或旧隔离环境14组结果替代。PSI调拨、盘点、调价、期初、发票、银行对账等分支及报价终态/转工单全路径也没有在本轮全部重跑，状态为NOT_RUN，不能把核心采购销售样本扩大成全功能覆盖。

最终后端：JDK21完整18模块reactor（父工程+17子模块）package成功，**58项测试、16个测试类，0失败/错误/跳过**。包大小194800958 bytes，SHA256 `119f2c157cb4d5b9044661758481bfa0f205b326093a163ba25c08a236c41fc0`。该包已经部署到本机8000并检查就绪；文件、报价、维修回滚、签章和工作台在此版本再次测试。H5客户归属用例及角色交替用例在同一工作区前一后端版本执行，后续变更为个人消息及公共逻辑删除桥接；H5认证/归属路径无代码变化，但不冒充部署后重新登录的测试。

前端此前第24节的27项测试、完整PC构建和225条类型错误归零属于既有证据，本轮没有新增前端生产代码或再次计算这些通过数。5173/5174/5175均实际返回HTTP200与HTML；这只说明服务入口可达。

### 26.2 已发现、修复并复测的缺陷

| 缺陷 | 复现与根因 | 修复及最终证据 |
|---|---|---|
| 高：个人消息越权全局删除 | 非接收者不能查看工程师专属消息，但直接DELETE `/user/message`传该ID可成功删除主消息；仅对R27合成消息复现 | 删除前整批校验接收范围，任一无权ID返回R.code=403且零部分写入；删除改为当前用户墓碑，不再删除共享主消息。page/detail/unread/read/readAll一致过滤；upsert不复活墓碑、保留已读时间、排序锁顺序。9项定向测试通过；MySQL真实接口验证混合批次403、两次并发删除均成功且仅一条墓碑、另一接收者可读、已删者列表及未读一致 |
| 高：公共逻辑删除SQL参数绑定失败 | 收款草稿和缺货出库草稿DELETE报`Parameter 'updateUser' not found`，无法经业务接口清理。公共DataPermissionMapper将MP默认scalar-ID删除覆盖成抽象方法，参数图只有id/param1，缺少自动填充字段 | 恢复`BaseMapper.super.deleteById(id)`默认桥，为实体重载继续保留@DataPermission；未散改业务SQL、未移除审计填充。18项真实MapperProxy/生成SQL/PreparedStatement参数绑定测试通过，含16种实际PSI Mapper及旧错误声明反例；最终包上原两个阻断草稿及新收付款/出库草稿DELETE成功，deleted等于原ID |
| 中：本机报价PDF导出缺中文字体 | DOCX正常，PDF返回统一失败R：缺可用TrueType中文字体（需glyf），没有把HTTP200误判为文件成功 | 给本机开发容器补入本机已安装simhei.ttf，忽略的本机Compose增加只读字体挂载；不提交或对外分发字体。独立报价金额100，DOCX内容和ZIP头、PDF头、HTML内容通过，最终版本PDF 30860 bytes，报价已业务删除 |

消息修复新增MySQL/PG `patch_message_inbox_delete.sql`并登记对应master，init同步新列，未改旧changeset。当前本机明确`SPRING_LIQUIBASE_ENABLED=false`，故部署前仅按审核后的新增patch增加`sys_message_log.is_deleted`列，没有运行安装SQL或整套迁移；未来启用Liquibase时前置检查会对已存在列MARK_RAN。MySQL已实测，PG仅做动态SQL绑定与迁移源码检查，未宣称PG数据库实测通过。

部署前保存原JAR。新墓碑生效后旧JAR会忽略个人删除，旧readAll还可能撞联合主键，因此**不能简单换回旧包或删列作为无风险回退**；优先前向修复。两个R27构建容器及专属临时镜像已在产物/日志留存后按名称和标签核对清除，在用后端、MySQL、Redis及其他容器保留。

### 26.3 分模块执行结果

下列PASS只对应所列用例，未覆盖分支不自动通过。请求同时检查HTTP及R.success/code，持久化断言采用只读SELECT；并发仅同一合成记录两个请求，未做无界压力测试。

| 模块/能力 | 本轮实际用例与结果 | 限制/未覆盖 |
|---|---|---|
| 平台底座、部署、国密 | PASS：正常SM2登录、11个路由根、菜单/部门树/角色/字典读取、在线和日志接口读取、最终包启动；统一R错误分支实际触发 | 在线列表和日志原文未落盘；未做全环境灾备演练 |
| 用户、RBAC、身份隔离 | PASS：5角色、2测试部门、空业务权限角色；初始密码强制变更、旧会话失效；3轮拒绝→管理员→工程师交替；文件/报价/工单权限拒绝 | 不等同所有模块全部数据范围组合覆盖 |
| 租户 | N/A：有效接口`/system/common/dict/option/tenant`实际返回false | 没有为测试打开租户；启用后的隔离没有验收 |
| 文件 | PASS：TXT/PDF上传、长度与SM3、下载字节完全一致；匿名与其他用户拒绝；不支持JSON类型被拒绝；回收/还原/精确永久删除、统计数量和大小回到上传前 | 数量线上为字符串Long，测试显式转数；页面点击未复测。最终版本当次统计5→7→5，后续签章新增审计附件另列，不要求全站文件清零 |
| 消息公告 | PASS：草稿增改删无消息发送；发布仅合成接收人；修复后的个人删除、混合越权、并发、已读/未读及共享接收人隔离 | 共享主消息/个人墓碑保留审计；没有真实广播或短信邮件 |
| 开放能力 | N/A：实际功能门拒绝应用管理；MODULE_OPEN_ENABLED在用value=NULL，服务按默认false解析 | 第一次尝试创建被配置门拒绝，未创建应用，不是凭据逻辑已通过；没有为测试启用模块 |
| 调度 | N/A：MODULE_SCHEDULE_ENABLED在用value=NULL，实际接口返回模块禁用；容器SNAIL_JOB_ENABLED=false，与dev配置默认false一致 | 未创建或执行任务，不把404当唯一证据 |
| PSI基础资料 | PASS：同一BOTH单位、两级分类、商品、仓库正常增查，受限角色不能详情访问 | 与库存审计关联的基础资料已禁用保留 |
| PSI库存核心 | PASS：采购10件单价100，销售2件单价150，FIFO成本200；同键确认重试不重复应收应付；1000件缺货失败保持草稿和库存不变；全额销售退货后采购退货，库存数量/成本、未清应收/应付均0 | 两轮合成确认与退货审计保留；其他PSI分支NOT_RUN。已确认资金核销与完整补偿BLOCKED，见下节 |
| PSI收付款 | PASS：收款/付款草稿增查改删、0金额拒绝、原删除故障复测 | 确认收付款→核销→反核销→退款/作废完整路径BLOCKED，未创建无法补偿的已确认资金单 |
| 独立报价 | PASS：不强制仓库，服务行2×50=100；DRAFT→SUBMIT→DRAFT，HTML/DOCX内容及PDF文件核验，受限角色拒绝，精确删除 | 独立终态和兼容转工单全路径NOT_RUN；未声称报价全状态已通过 |
| 运维维修 | PASS：报修确认/关闭/删除；派单自动接单→开始→维修记录/材料→提交结算；其他工程师拒绝；缺货确认结算保持DRAFT，无泄漏维修出库、应收、实收 | 正向结算和实际收款补偿缺口BLOCKED；未以删除结算头冒充回滚出库 |
| 客户H5 | PASS（API）：两个合成客户经公开图形验证码正常登录；伪造customerId被服务端覆盖；详情和列表跨客户拒绝、退出后令牌失效；报修已删除 | 未读验证码缓存；浏览器界面、设备适配NOT_RUN |
| 工程师H5 | PASS（API）：本人任务可读，其他工程师任务拒绝，正常提交维修记录 | 经纬度反向定位/图片水印/完整终结路径NOT_RUN，测试未调用外部定位 |
| 签章 | PASS：合成PDF和红色测试图形，内部自签证书，无外部CA/TSA；普通和合并接口分别两个并发仅1成功；回执verifyStatus=1、下载；独立OpenSSL验证CMS成功，篡改失败 | `-noverify`只证明签名完整性，不证明外部证书信任/法定效力；未用真实单位印章 |
| PC/H5入口与页面 | PASS：三个HTTP入口；按用户选择执行API替代 | 页面交互、加载渲染、移动适配NOT_RUN，未再次绕行宿主浏览器拒绝 |
| 工作台聚合 | PASS：同身份、同租户、上海日期口径，对源表独立聚合核对20个数量/金额字段，清理后再核对 | 只读跨来源一致性诊断，不是完整联合业务验收 |

清理后工作台串行10次接口采样：最小46ms、中位54ms、P95=126ms。样本少、无浏览器渲染和并发负载，且没有约定验收目标，故只记录测量，不能据此声称性能验收通过或“页面已全面提速”。

### 26.4 未关闭的完整联合测试阻断

| 用例/退出项 | 状态 | 证据和继续条件 |
|---|---|---|
| PSI确认收付款→核销/反核销→退款/作废→净余额恢复 | BLOCKED | Receipt/PaymentServiceImpl只允许删DRAFT；确认设置正未分配金额，反核销又加回未分配额。当前没有作废/退款归属及两端自动冲抵链；建相反方向普通单只能金额净0，不能清掉两端未分配余额。需先完善正式补偿能力并测试，不能SQL删除流水 |
| EOMS正向结算→PSI维修出库→应收→实际收款→完整补偿 | BLOCKED | 结算删除仅软删单头，无库存/成本/应收反确认；PaymentServiceImpl删除回滚应收和付款行，但没有同步撤回核销产生的Income，且Income缺paymentId归属。应增加可追溯、幂等、事务一致的补偿链再测 |
| PC/客户H5/工程师H5完整联合闭环 | BLOCKED | 依赖上项正向财务与补偿；当前只验证各端可执行的API子路径，未做最终全链验收 |
| 所有必测模块退出后的新资料最终联合测试 | BLOCKED，尚未执行 | 按分阶段Skill验收矩阵，金额/库存/清理缺陷未关闭且若干业务分支NOT_RUN，不能进入最终联合PASS。已执行的采购销售及签章内部跨域验证不改名为最终联合验收 |
| 全量性能与浏览器体验验收 | NOT_RUN/BLOCKED | 尚无完整负载/延迟/资源指标，图形界面通路仍受宿主来源门限制；HTTP/API计时不能代替页面渲染 |

这些缺口是源码确认的流程/补偿问题，不是权限仍未放开，也不是需要用户再次提供密码。没有为获取“全绿”制造无法安全补偿的新资金流水。

### 26.5 清理、留存和证据

- R23旧公告精确ID `908798778079707254`及回收文件 `908798053845041260`已通过原业务接口处理；公告、消息和文件的软删元数据可能保留，不能用物理COUNT(*)=0作清理标准，没有使用全站回收站清空。
- R27共5个测试账号、1专属角色、2部门已业务删除，两个H5客户会话和5角色会话已正常退出；本次管理员会话最后正常注销，**原管理员密码未改动**。会话定时检查助手已停止，本次临时Token/随机测试账号凭据文件已删除。
- 报价、报修、工单/材料/记录/草稿结算、收付款草稿和缺货草稿均已业务删除并回查。最终台账没有ACTIVE待清理对象；合成账号有效行、收付款草稿、缺货草稿均0。
- 两轮已确认采购/销售通过全额退货补偿，最终合成仓库/商品数量0、库存成本0、单位未清应收0、未清应付0。确认单、退货、合同及逻辑删除元数据保留，未直删账务/库存流水。
- 与历史库存关联的1单位、1商品、1仓库、2级分类共5项基础资料已禁用保留，防止破坏审计关联；另一个合成客户已业务删除。这些是明确的审计留存，**不是所有测试行已物理清零**。
- 合成印章ID1已禁用，授权表0条；合成签章项目ID1已取消并清空项目授权。共5条成功签章审计和7个引用平台附件（源PDF、测试图形、5个签章PDF）及内部自签测试证书/密钥存储保留，未强删不可追溯证据。第一次签章因测试台账错误限制UUID而记录FAIL，该次签章实际成功，已从项目/签章表独立补录审计清单。不存在真实合同或真实单位印章。
- 早期脚本对空统计字段、软删行数、UUID、重复项目审核和若干端点猜测有测试自身失败；均保留历史，后续修正并执行对应复测，不能计为多项已修产品缺陷。开放模块禁用属于N/A，消息越权和公共逻辑删除属于真实产品缺陷；报价缺字体属于本机部署故障。

可复核证据位于忽略目录：`.local/r27-ledger.json`（步骤结果/历史失败/ID/补偿）、`r27-build-evidence.json`与`r27-backend-build.log`（58项全构建）、`r27-delete-binding-test.log`、`r27-deployment.json`（实际部署hash/单列变更）、合成导出与签章核验产物。台账无密码或Token，身份凭据文件不作为交付。执行入口为 `scripts/quality/live_api.mjs`，逐阶段显式调用、固定本机及运行号，不包含隔离bootstrap；本轮已关闭，不能在缺少新夹具和会话时盲目重放。公开验证码放大辅助脚本为 `scripts/quality/render_public_captcha.py`，需要Python/Pillow，不读取服务端答案。

测试组织参照既有分阶段Agent/Skill和GB/T 38634系列、25000.10、适用时25000.51、39788的质量/测试框架，官方题录与适用边界在 `.agents/skills/itls-staged-test/references/standards.md`（2026-11-05核对）。未取得全部授权正文，**本报告不是国标逐条符合性认证或第三方测评通过证明**。

## 27. R28 在用环境业务逻辑缺陷实测

### 27.1 执行范围与结果

用户要求“使用测试数据在生产环境下测试找出bug，查看业务逻辑是否明确”。沿用此前明确指定的本机在用环境：API8000、PC5173；实际后端仍为dev profile，不能将本轮称为公网生产环境验证。2026-11-05晚执行，23:02（Asia/Shanghai）关闭本轮。使用原管理员正常国密登录和公开图形验证码，新建`ITLS-TEST-R28`专属单位、商品、分类、仓库，没有使用真实业务对象或重置原账户密码。

**按用例ID取最后一次执行：27组检查，17 PASS、10 FAIL。** 其中24组为夹具、业务和边界检查（14 PASS、10 FAIL），另外3组为待办清理、数据净额回查及本轮会话退出（3 PASS）。10个失败场景归为下列7类缺陷；不按重复执行次数累计缺陷或通过数。仅增加测试脚本和报告，**未修复这7类业务缺陷、未修改或部署新的业务代码**，在用版本保持第26节最终JAR。此前58项后端单测是旧证据，没有在本轮重复运行或冒充新失败场景的覆盖。

测试方法为真实API请求、统一R检查和独立只读SQL断言。异常输入只进入可业务删除的草稿；没有确认负价格、错误原单或重复超量退货，没有确认不真实发票，也未创建已确认收付款或对外资金动作。正常采购、盘盈、调拨、盘点使用合成对象，已完成业务补偿。

### 27.2 已复现缺陷及最小步骤

| 编号/优先级 | 失败用例 | 实际结果、预期与源码定位 |
|---|---|---|
| R28-B01 / P1 报价总额未由持久化明细和税率统一计算 | `QUOTE-TAX-ONLY`、`QUOTE-FORGED-TOTAL` | 建服务行2×50、税率13%的报价，初始总额113。PUT `/quote`仅传`{id,taxRate:6}`后税率6、总额仍113，预期106；另一单仅传`{id,taxRate:13,totalAmount:1}`后总额变成1，明细仍100、税率仍13。两种结果都已GET及落库回查。`QuoteServiceImpl.java:116-121`只在details非null时重新汇总，否则直接将客户端总额交Mapper更新。派生金额不能任意信任请求值 |
| R28-B02 / P1 只提交状态会改写既有税率 | `QUOTE-STATUS-ONLY` | 初始税率13、总额113，PUT `/quote`只传`{id,status:'SUBMIT'}`，状态正常提交，但税率变4、总额仍113。预期未传税率时保留13。`QuoteServiceImpl.java:244-247`对新增和修改均将null税率设为4，未合并existing。该现象先后在两笔独立测试报价复现 |
| R28-B03 / P1 退货关联链未完整校验 | `RETURN-CROSS-UNIT`、`RETURN-CROSS-ORIGIN-ITEM`、`RETURN-WRONG-GOODS` | 两笔已确认合成采购单A/B、同仓库，分别构造：原单A+供应商B；原单A+原单B明细；原单A的商品A明细+商品B。三种请求均成功创建并持久化退货草稿，预期拒绝不一致组合。`StockReturnServiceImpl.java:333-377`检查原单确认/仓库/明细存在/单行数量，但缺明细所属原单、goodsId和往来单位的一致性校验。**只证实非法草稿可保存，未确认这些非法单，未声称实测造成错账或跨租户越权** |
| R28-B04 / P1 退货接受负含税单价 | `RETURN-NEGATIVE-PRICE` | 合法原采购明细单价100，退货请求数量1、单价-100，成功保存草稿及负金额。前端`psi/stock-return/EditModal.vue:85`限制min=0，但后端`TaxCalcUtils.java`只检查正数量/非负税率，不检查负价格；退货Service直接用其计算。应在后端落实非负价格规则，零价是否允许另作明确政策，不能以按钮控件替代API校验 |
| R28-B05 / P2 重复原明细未按累计数量校验 | `RETURN-DUPLICATE-ORIGIN-OVER-TOTAL` | 原明细10件，同一请求放两行相同originItemId，各6件，累计12件，草稿成功保存。预期合并校验累计可退量或拒绝重复明细。当前逐行将6与10比较而通过。**未确认此非法单，未证明实际已超退；确认时下游库存检查可能再拒绝，不能将草稿校验缺失夸大成已发生库存穿透** |
| R28-B06 / P1 盘盈接受负成本价格 | `ADJUST-NEGATIVE-COST-PRICE` | `/psi/stock-adjust`传PROFIT、quantity=1、untaxPrice=-10成功保存，GET回显负价。前端`psi/stock-adjust/EditModal.vue:71`min=0；Service insertItems只检查quantity>0及价格非null，缺非负校验。应使前后端业务规则一致。非法盘盈未确认，未向库存写入负成本 |
| R28-B07 / P2 未付款退货误生退款待办 | `UNPAID-RETURN-REFUND-TODO` | 两笔合成采购都未付款，正常全额采购退货后应付为0，却新生2条`RECEIVE_REFUND`未完成待办。独立SQL确认该单位已确认付款0条。预期无已付款退款需求时不生成待收退款。`StockReturnServiceImpl.java:325-326`无条件生成退款/收退款待办；待办应由实际资金归属和可退金额驱动，不能把退货等同退款 |

表中路径均相对对应业务模块的`src/main/java`，前端路径相对`ls-aegis-web/src/views`。每个接受异常草稿的场景均在台账记录完整合成请求、返回码、草稿ID及GET详情，随后通过其原DELETE接口删除；报价5—9均已删除。当前缺陷状态为OPEN，不以草稿清理成功当成产品修复。

### 27.3 通过的业务规则与实际联接

| 检查 | 实际结果 |
|---|---|
| 调拨同仓/负数 | 同一调入调出仓、负数量均被API拒绝 |
| 空库存调拨 | 可保存正常草稿，但确认缺货被拒绝，状态保持DRAFT，随后业务删除；库存未变化 |
| 盘点负实盘数 | API拒绝，未留下有效草稿 |
| 发票确认与价税 | 无关联单据的销项发票确认失败并保持草稿；进项表头含税100、未税100、税额13不一致被API拒绝。测试号仅`TEST-NOT-REAL/INVALID-TAX`，没有真实税务开票 |
| 退货有效性部分规则 | 负数量、超原明细单行数量11>10、错误仓库均被拒绝；正常全量采购退货确认后库存/应付确实归零 |
| 盘盈幂等及调拨成本守恒 | 合成盘盈5件×20成本100，同键重试没有增加第二份库存；调拨2件成本40，再反向调拨2件成本40，回到原仓5件/100、另一仓0。最后盘亏补偿5件，两个仓数量和成本均0 |
| 零差盘点 | 空库存账面0、实盘0确认成功，数量成本保持0，确认记录保留审计 |
| 统一数据关联 | 单位/分类/商品/仓库和采购/退货/调拨均来自同一平台真实API，ID用字符串，没有复制一套独立业务库制造通过 |

第一轮调拨测试错误地从单头读取costAmount而得到NaN；接口实际成本位于items[]，修正断言后新一轮盘盈/双向调拨/补偿通过。这是测试自身错误，**不计入10个产品失败**。清理首轮误查不存在的统一发票表，修正为`psi_invoice_in/out`后回查通过，同样不计产品缺陷。

### 27.4 业务逻辑是否明确

结论：**主干职责较明确，但金额更新契约、退货归属、价格边界及退款条件没有在服务端形成一致规则，目前不能判为业务逻辑已完整明确。**

| 业务规则 | 当前实现及需要收敛的口径 |
|---|---|
| 报价部分更新 | 源码允许details省略表示不修改明细，因此只改状态/税率是有效接口路径；未传字段应保留，新增默认税率不能作用于修改。税率或明细变化后必须由后端使用最终持久化内容重算金额；客户端总额只能作为核对值或明确的受控调价流程，不能覆盖派生金额 |
| 退货业务归属 | 需要同时满足原单、原明细、商品、仓库、往来单位一致。行数可多，但同一原明细累计不得超过原数量减已确认退货量；确认阶段还需在事务内重新校验并发可退量。当前只有部分关系明确地实现 |
| 价格及金额符号 | 正常采购退货、库存盘盈的数量与单价应符合业务符号规则；界面已有min=0，应由服务端落实。合法折价、赠品零成本、红字发票分别走明确场景，不能让负正常单价隐式承担相反业务 |
| 退货、反核销、退款 | 退货是货物流，调整应收/应付是债权债务，退款是实际资金流，待办完成是人工工作状态。四者不能互相等同；未发生付款就不应提示收退款，真实退款还需追踪对应原资金单、退货及金额。当前无条件待办及第26节补偿链缺口仍在 |
| 维修财务 | 第26节静态审计确认的结算删除与出库/应收反确认缺口、收款删除未同步收入回滚仍为OPEN；本轮未新增不可补偿的正向维修收款，未把静态审计再次算成新接口FAIL |

处理顺序应先关闭报价金额/税率和退货关联校验，再统一负价边界、重复行累计及退款条件；每项用本轮失败请求作回归。完整财务确认/冲销及最终联合验收仍不能记PASS。当前结果不是全模块、全部角色、浏览器界面或公网生产的完整验收。

### 27.5 清理复核与R27补查纠正

R28清理终检通过：7类单据表（采购、退货、调拨、调整、盘点、进项发票、销项发票）本轮有效DRAFT均0，报价本轮有效行0；专属2仓/2商品合计库存数量0、库存成本0，测试供应商未清应付0，台账无ACTIVE待清理对象。两笔已确认采购以正常全额采购退货补偿，盘盈以盘亏补偿；确认单、退货、调拨、盘点的审计行保留，未直接删库存/账务流水。与历史单据关联的7项基础资料已禁用保留，另一个无有效业务引用的合成单位已删除。

**本轮补查发现R27还遗留4条误生成的合成退款待办，第26节“台账无ACTIVE”仅覆盖当时登记实体，没有证明待办全清。** R28另有2条，合计6条。已通过关联原退货ID与精确`ITLS-TEST-R27/R28-补偿`标记确认所有权，独立核实两个测试单位都无已确认收款/付款，随后只对这6条不适用的测试提示调用`/psi/todo/{id}/done`，逐条回查done=1。此操作是关闭误提示，不是实际退款完成，也没有清理真实待办；待办审计保留。第26节历史描述应结合本次补查使用，不能再宣称上一轮已经覆盖全部待办。

本次管理员会话已正常退出，测试会话/凭据文件已删除，原管理员密码不变。没有新建角色账号、启用禁用模块、触发短信邮件/外部CA/TSA、执行安装SQL或数据库恢复。服务继续运行在原修复版本。

证据：忽略目录`.local/r28-ledger.json`保留全部历史执行、最后结果、合成请求/详情、报价修改前后、退款待办原状态、业务补偿及终检；没有密码或Token。测试入口`scripts/quality/live_logic_r28.mjs`采用固定目标/运行号、显式阶段、纯SELECT持久化断言，运行关闭后拒绝重放；原隔离bootstrap脚本没有指向在用实例。对齐第26节国家标准框架的证据/缺陷/结束过程，不宣称认证。

## 28. R29 修复交付与 R30 联合回归

### 28.1 交付结论与适用范围

执行日期：2026-11-05夜间至11-06凌晨（北京时间）。本轮响应“八小时内修复此前发现问题”的授权，完成第26节财务补偿阻断、第27节7类业务缺陷，以及修复过程中发现的鉴权、并发和部署问题。最终版本已运行在用户明确指定的本机API8000，PC5173、客户H5 5174、工程师H5 5175入口可用。保留既有工作区修改，没有提交、推送、升版或重置原管理员密码。

**已完成的交付范围：完整构建、适用模块API回归、全新合成资料的跨模块API联合闭环、真实角色边界、业务补偿清理，以及同一最终JAR的隔离生产配置验证。所列已复现缺陷已修复并有对应重测，当前台账无未关闭FAIL。** 第26、27节的历史OPEN/BLOCKED由本节相应证据更新，不改写原始执行历史。

**整体生产验收仍有边界：在用实例为dev profile，未部署公网生产；浏览器页面、真实移动设备/GPS、正式外部CA/TSA及完整性能验收未完成。** 本节交付的是通过所列验证的生产候选版本，不能宣称全产品、所有配置组合和全部生产非功能指标均已验收。浏览器来源策略拒绝依旧存在，用户已明确授权使用API替代，未绕过策略。

| 证据层级 | 最终结果 | 解释 |
|---|---|---|
| 后端干净完整构建 | PASS：18个reactor模块，334项测试，失败/错误/跳过均0 | 1372个源码/资源文件与构建快照SHA逐项匹配；父工程含17个子模块 |
| PC完整构建及类型门禁 | PASS：`pnpm build`，4m49s | 原225条类型诊断已由此前修复归零，本轮在最终前端源码上重新完整构建 |
| 客户/工程师H5构建 | PASS：两种mode | pnpm触发本机受限目录链接EPERM后，直接使用已安装Vite CLI构建，未安装或改写外部依赖目录 |
| 前端行为回归 | PASS：27/27，无跳过 | 包含权限/幂等键、数据与组件边界等；受控模型不是浏览器点击 |
| R29逐模块及修复重测 | PASS：57个按ID最新记录，84条历史执行 | 含身份准备、模块出口、清理及审计，不能将57称为57个独立业务模块；历史失败保留 |
| R30-JOINT新资料联合回归 | PASS：28/28 | 在最终v6上新建业务资料，实际完成四条跨域链；不是从旧模块PASS推导 |
| 最终生产运行配置冒烟 | PASS | 独立MySQL/Redis、prod+generator、无宿主端口映射，实际最终JAR启动/迁移/鉴权/PDF验证 |
| 浏览器与完整性能验收 | NOT_RUN / BLOCKED | 页面来源策略阻断；缺正式负载、延迟与资源验收目标，不能用HTTP200或少量接口计时替代 |

最终JAR：`ls-aegis-server/ls-aegis-boot/target/ls-aegis-server.jar`，194933932字节，SHA256：

```text
814965a3084533397405911675bde320465f63862ca64408aeee16c7c43adf9f
```

本机最终部署记录`.local/r29-deployment-v6.json`：ready=true、Asia/Shanghai、审核范围迁移pending0；本次v6部署无新增DDL。最终JAR同时留存`.local/backend-r29-v6/ls-aegis-server.jar`，隔离生产镜像`r29-v6-7604098f:R29-v6`仅留本机，未推送。

### 28.2 第27节缺陷逐项关闭

| 原缺陷 | 修复后的明确规则 | 有效重测与结论 |
|---|---|---|
| R28-B01 报价派生金额失真 | 从最终持久化明细与税率计算；请求总额不能覆盖派生值，数值按既定精度归一 | 税率13%总额113→6%总额106；伪造总额1仍为113。真实GET/落库回查PASS |
| R28-B02 状态更新改写税率 | 修改合并existing，省略税率保持原值；新增默认值仅用于新增 | 仅提交SUBMIT，既有13%及113不变，PASS |
| R28-B03 退货关联缺失 | 原单、明细所属原单、商品、仓库、往来单位、类型完整一致；确认事务内锁定后重验 | 跨单位、跨原单明细、错误商品及仓库均被拒绝；正常完整退货补偿PASS |
| R28-B04 负退货价 | 正常单据单价不可负；合法零价允许，红字走正式场景 | 负价请求明确拒绝，正常零价边界有测试；真实API PASS |
| R28-B05 重复原明细累计 | 同一退货单拒绝重复originItemId，确认时检查已确认可退余额 | 两行6+6针对原10件拒绝；隔离并发及真实正常链PASS |
| R28-B06 盘盈负成本 | 盘盈成本非负，保持数量及成本守恒 | 负价请求拒绝；盘盈→双向调拨→补偿、空库存零差盘点实际PASS |
| R28-B07 未付款误生退款待办 | 待办基于真实资金归属、可退款额及已处理退款动态生成/关闭 | 未付款全退无退款待办；已付款退货→退款→反退款→原资金作废后待办一致，PASS |

发票两次早期“拒绝PASS”由无关的buyer_tax_no前置数据库错误触发，不能证明金额校验；台账标记INVALID_PRECONDITION并排除。修正合成请求后，重现表头价税不一致并修复，最终在正确前置下验证拒绝原因及状态不变，未掩盖这两次无效证据。

### 28.3 财务、库存与并发补偿

PSI收付款新增正式作废、退款及反退款，记录原资金归属与持久操作序号，事务锁覆盖现金单、排序后的来源库存单和应收应付。核销、反核销、退货与退款在相同租户账本口径下计算，金额净零同时要求未分配额、应收应付、待办为零，不再以相反方向普通单掩盖悬挂余额。对账具备解除匹配和数据库唯一约束，单一资金不能重复匹配。已复现的历史来源交叉锁死锁，经统一锁顺序后两事务均完成；隔离真实事务测试与在用两请求回归分别保留证据。

库存变更通过共享PSI服务和持久租户/仓库/商品锁锚点串行，空批次同样有锁。FIFO扣减和退还保留末分金额，整次涉及的库存键按序加锁；维修模块不直接写库存。盘点误操作纠正使用带原批次ID的正式盘盈，防止创建不相干成本批次。

EOMS结算确认从PSI接口取得实际FIFO成本，不采信材料录入时的预估成本；出库、成本、应收与结算在同一事务边界内生效。实际收款才写收入，删除收款执行可追溯反向收入与应收恢复；结算反确认恢复原批次、成本和应收，保留不可变审计事件，可在冲销后重新生成结算草稿。

新增正式工单撤销关闭接口`PUT /eoms/repair/order/cancel/{id}?reason=...`：仅允许已反确认且无活动结算、欠款、实收、成本或维修出库残留的待结算工单关闭。沿用`eoms:order:close`权限，必须说明原因，重复撤销不生成第二事件；正常完结工单不被误当撤销重放。原备注与原因合并超500字符时不截断原备注，合法原因完整存入独立事件；原因自身超限仍拒绝。

| 关联问题 | 最终修复与验证 |
|---|---|
| 幂等与事务提交时机 | 共享IdempotentHelper只在提交后缓存结果，事务完成释放看门锁；回滚无假成功。报价并发转工单、库存确认、资金/退款和签章各验证单次效果 |
| 锁SQL与权限解析 | 修复3处ORDER BY/FOR UPDATE被拦截器改写后的MySQL语法问题；实际TenantLine解析回归，并在真实MySQL链重新确认 |
| 零金额维修 | 应收直接PAID，不进入未收清列表，零金额收款拒绝；实际经理/工程师/财务角色0元与20元链PASS |
| 工单权限 | 状态变更先DataScope可见性再当前读锁和归属复核；其他工程师不能开始他人工单；派单目标必须为本租户启用工程师角色 |
| 发票一致性 | 表头/税额/明细/关联来源复核，来源按序锁定，跨部门同租户总额准确；并发蓝票限额、来源单位、红票同/异幂等键重放单次效果，红票不能再红冲 |
| 银行导入 | 账户授权与行锁；明确流水号为主身份，金额/日期/字符串规范化；无流水号兼容旧语义hash，金额/时间精度差不产生重复；独立真实事务验证唯一约束 |
| 银行账号密文 | 导入SKIP/UPDATE用平台同一SM4实现匹配；禁止请求改部门/租户，密文长度边界验证。自动旧账号迁移默认关闭，仅显式精确ID白名单且整批预检，未知/AES/已SM4密文不盲目二次加密 |

### 28.4 H5身份与客户确认

修复期间发现客户仅凭可预测单位编号加图形验证码即可换取会话，不能证明客户身份。最终版本取消该路径：具备`psi:unit:update`权限的员工针对当前可见且启用的客户生成一次性访问码；32字节安全随机值、Redis只存摘要、10分钟过期、原子GETDEL消费、绑定客户与租户。客户仍需正常图形验证码，旧弱身份会话不再接受，没有兼容降级开关。

PC单位行提供“客户访问码”临时弹窗，关闭清除，H5登录使用访问码输入；不放URL、不持久保存、不记录敏感请求/返回审计正文。邀请接口及鉴权接口实测LogHandler忽略敏感参数；没有关闭在用验证码或正常审计。

在线会话管理返回不具备登录能力的sessionId，列表不泄漏bearer；踢出时重验客户/租户授权。实际发现“会话有效但在线数量0”，根因为默认RedisTemplate的JDK键序列化与Redisson命名映射不一致；最终改为从既有Redisson摘要索引枚举，再读取原会话及TTL。旧v5合法会话无需重登，在v6实际显示2条，管理句柄无法登录，踢出后原令牌立即失效。独立真实Redis/JDK序列化与NameMapper反例回归17项断言通过，不将其混入334项reactor测试数量。

退出改为`POST /eoms/h5/auth/logout`，令牌置请求头；旧GET返回业务405。客户报修的customerId和tenantId由已验证会话覆盖，跨客户或跨租户关联详情拒绝。

客户确认从“界面提示成功”改为不可变`ORDER/CUSTOMER_CONFIRM`事件：仅允许待结算/已完工、本人客户/租户的工单；并发确认唯一，第一次说明与时间不可覆盖。PC与客户详情读取相同确认对象，已关闭工单保留确认历史但不接受新确认；客户确认本身不产生实收。真实R30联合链验证了错误状态、其他客户、伪造customer/tenant、两次并发、重试、PC/H5回显和关闭后历史读取。

### 28.5 R30全新资料的四条联合链

先完成R29适用模块的API出口与业务净零，再以`ITLS-TEST-R30-JOINT`创建另一套客户、两种商品和两个仓库。沿用专用测试员工的既有经理/工程师/财务角色，没有扩权给经理以代替权限测试；报价/PSI管理链使用管理员，工单及财务角色边界由真实角色分支验证。R30全部28项在最终v6运行，完成时间北京时间11-06 03:16；台账closed后拒绝重放。

| 联合链 | 实际执行与独立断言 |
|---|---|
| 基础资料→采购→销售→资金→退货→退款→银行→发票 | 同一客户/商品/仓库贯通；3笔收款300/300/50、付款1000及核销，销售与采购全退、正式退款及反退款/作废；6条银行记录匹配IN1600/OUT1600，净0、未匹配0；蓝票并发限额/关联归属、红票幂等，待办清理后0 |
| 报价→工单→维修→PSI→结算→应收→实收 | 并发转工单仅1张，采购材料共享给维修；预估材料价999不影响实际FIFO材料成本100+人工10=110，应收220、实际收220，随后收款冲销、结算反确认/重确认/再反确认、终态关闭、采购全退；收入/成本/库存/欠款净0 |
| 客户H5→PC派单→工程师→客户确认→财务 | H5发起归属不可伪造，PC确认转单，其他工程师及缺坐标请求拒绝；无外部定位配置时用合成0,0执行H5工程师开始/完工；并发客户确认一次事件，材料实际成本50、应收/收款100，反向补偿、关闭、全退归零。真实设备定位及水印不在此结果内 |
| 文件→项目/授权→签章→下载/回执→完整性 | 仅合成PDF/测试图形/内部自签证书；普通/合并签章分别两个并发各1成功，回执verifyStatus=1，下载PDF，独立OpenSSL CMS验证成功、篡改失败；最后取消项目、清空项目授权、禁用印章、撤销授权，保留引用审计文件 |

文件模块另验证上传内容、下载字节/SM3、匿名/他人拒绝、回收还原及数量/容量恢复。公告只建合成草稿；个人消息只面向专用测试接收者，整批越权零部分写入、墓碑隔离、并发删除及未读一致性通过，没有真实广播。

工作台以同租户、上海日期对源表独立SELECT聚合，实际核对20个数量/金额字段；10次串行样本最小39ms、中位46ms、P95约97ms。样本过少且无并发负载，仅记录测量，不判性能验收PASS。历史workbench证据中固定`fullJointAcceptance=NOT_RUN until...`为旧描述元数据；联合结论取后续明确的金融/H5/签章用例及finalAudit，保留旧字段并在脚本修正后续表述，不篡改历史台账。

### 28.6 迁移、生产配置和前端构建

四个新增patch为库存变更锁、PSI财务补偿、EOMS财务补偿、原批次盘盈纠正；MySQL和PG源/master/init同步，未改已执行changeset正文。PG本轮仅源码/动态SQL检查，未声明实际PG数据库通过。在用库使用审核后的增量，未运行全量init/master或changelogSync，未整体恢复数据库。

提供打包后的独立`R29MigrationCli`、固定52项发布changelog、`scripts/migrate-r29.sh`及`docker/ls-aegis-server/MIGRATION-R29.md`。validate/status只读且不创建历史表；apply要求显式目标及确认，校验固定清单/摘要并拒绝篡改或历史checksum不符。用独立DDL账号执行，不给应用账号DDL，不将口令置命令行。隔离MySQL三种实际数据库：全新init结构、旧结构、已执行52记录，验证应执行/应MARK_RAN/无变化重跑及种子指纹不变。v6在用部署仅执行包内validate/status，pending0，无新增结构修改。

最终v6实际生产镜像启动验证：prod+generator healthy，UID/GID10001，CST+0800，应用账号仅CRUD且20个真实连接；独立迁移账号52条记录=3 EXECUTED+49 MARK_RAN、pending0。内部网络且无宿主端口，字体使用镜像WQY，通过真实QuotePdfExporter输出及PDFBox准确提取中文。未登录、无权邀请、customerNo-only及旧GET退出按统一R返回401/400/405，验证码仍开启。自建3容器、网络及4个临时凭据文件已精确清理。

Docker生产入口在root时必须以setpriv降权，缺工具直接失败，不回退root运行；非root直接执行。时区显式Asia/Shanghai。本机开发容器继续用已有字体只读挂载，未提交商业字体。

PC保持Arco、H5保持Vant及统一API/权限体系；补充正式退款/作废/解除匹配、反结算后重新结算、取消原因、零应收显示、预估/实际成本区分。Office三类预览引擎异步边界检查通过，最终入口资源约2292.6KiB/gzip580.7KiB，无Office渲染器提前进入入口依赖；仍有大chunk警告，未靠调高告警阈值伪装优化。构建及接口速度不能替代页面首屏和设备适配验收。

### 28.7 测试事故、清理及可追溯性

早期真实MySQL退货锁SQL失败后，测试脚本未阻止后续“空库存零差盘点”，在本轮自有商品仍20件/成本2000时误盘为0。这是测试前置/停止门错误，独立事实归纳将原PASS作为TEST_HARNESS_INCIDENT排除，历史台账原样保留；随后暂停新增业务写入并保留快照。通过正式原批次盘盈纠正单908881864515649539，将原批次908874705459478580、908874706117984318各恢复10件/1000，再做原采购全额退货，使库存与应付恢复0。原盘点908874714003275865及纠正链全部保留；没有SQL改库存、删除账务或整体恢复数据库，没有涉及真实客户/商品。

收款重复请求有平台防重提交时正确拒绝“请勿重复提交”，测试最初错误期待成功响应，后改为断言拒绝且只存在一笔收款；从已登记的原收款继续冲销，没有新收第二笔。另一次全客户AP断言混入同客户独立维修采购200，改为精确来源后回查通过。这些均保留为测试自身失败，不计产品缺陷或虚增修复数量。

最终R29和R30各有独立24项净零proof：库存数量/成本、PSI未清应收应付、资金未分配额、退款待办、开放工单、活动结算、EOMS欠款/收入/成本/活动收款、维修出库、发票草稿、未登记资金/库存/发票表头、未匹配银行及银行/现金净额均0。两轮activeOwnedObjects均0。不是以DELETE返回成功代替验收。

| 清理对象 | 最终状态 |
|---|---|
| 可删除合成草稿/临时文件 | 通过业务接口删除，下载/回收及数量独立回查 |
| 已确认库存/财务/发票 | 正式全退、退款/反退款/作废、红票、收款冲销、结算反确认；保留来源、补偿和审计记录 |
| 维修工单 | 补偿后正式CLOSED；未接受可删报修按API删除，已关联历史保留 |
| 合成客户/商品/仓库/分类/账户 | 有审计引用者禁用保留；无有效引用的其他客户删除 |
| 印章/项目/引用文件 | 测试印章禁用、权限0；项目取消并清空授权；自签证书与签章文件保留合成审计，不是法定签章撤回 |
| 共享测试身份 | R30结束后精确删除5个本轮用户、1个受限角色、2个部门；各用户先退出并验证会话失效，原角色定义/原账号未改 |
| 管理员及本地凭据 | 本任务唯一自有管理员会话正常退出并验证撤销；两轮token/账号私密文件删除，会话检查辅助进程已停止，原管理员密码不变 |
| 测试基础设施 | 14个名称与标签核对后的自有构建/结构测试容器已移除，日志留存；生产冒烟自建资源已清，原后端/MySQL/Redis/media与数据卷保留 |

### 28.8 剩余验收边界与证据索引

模块Agent/Skill沿用仓库`agents/staged-test/AGENTS.md`、`.agents/skills/itls-staged-test/SKILL.md`及验收矩阵。主代理负责合成业务与修复集成，既有代码检查和分阶段测试角色分别独立核对最终生产运行环境、源码/版本、覆盖、历史有效性和清理事实。所有正常登录经公开验证码与既有SM2协议，没有读取验证码缓存、伪造会话、降低校验或读取浏览器登录数据。

测试组织参考GB/T 38634.1/.2/.3/.4-2020、GB/T 25000.10-2016及GB/T 39788-2021，GB/T 25000.51-2016仅在产品适用范围内参考。官方题录和使用边界见`.agents/skills/itls-staged-test/references/standards.md`。本项目规则不是标准原文条款；没有完整逐条符合性、第三方测评或国家标准认证结论，不编造“法定3秒/100并发”。

| 剩余项 | 状态及边界 |
|---|---|
| PC/两端H5浏览器点击、渲染、设备适配、刷新交互 | NOT_RUN：Windows Chrome可枚举，但来源URL无法可靠识别，计算机使用策略拒绝；没有尝试绕过，API替代已完成 |
| 公网生产上线与真实数据迁移/灾备演练 | NOT_RUN：本机dev在用与隔离prod验证不能替代指定公网目标部署；提供正式增量工具与运行证据 |
| 完整性能、并发容量和资源门槛 | BLOCKED：未约定验收指标，少量串行采样不代表压力测试；未在在用实例做无界压测 |
| 真实GPS、逆地理定位、拍照水印/真实移动设备 | NOT_RUN：合成坐标只验证服务状态与授权，未调用外部地理服务 |
| 正式外部CA/TSA、短信邮件/实际银行支付 | NOT_RUN：仅合成内部证书和系统记账，不创建对外不可撤销业务 |
| 租户/开放平台/调度运行验收 | 当前部署N/A：真实配置禁用，没有为测试开启；启用后须另做适用验收，不能沿用N/A当作PASS |

以上边界不会被报告为已通过。当前没有所列范围内尚未关闭的已复现业务、安全、金额/库存或清理缺陷；这不表示尚未运行的分支不存在问题。生产部署发生VOID/REVERSED/退款及会话协议变化后，不可将旧JAR/删列/整体数据库恢复视为无风险回退，应保留备份并前向修复。

复核入口与脱敏证据（均在忽略目录，不能提交凭据、证书、日志或数据卷）：

- `scripts/quality/live_release_r29.mjs`：固定本机目标、显式模块命令、`--joint`独立资料、每步登记ID、SELECT-only回查、关闭台账禁止重放。
- `.local/r29-ledger.json` / `.local/r30-joint-ledger.json`：全部历史、最后有效结果、来源/补偿关系、24项净零与closed时间。
- `.local/r29-build-v6-evidence.json`、`r29-backend-build-v6.log`、`r29-surefire-v6/`、`r29-deployment-v6.json`：334项测试、源文件匹配、最终包与部署。
- `.local/r29-pc-build-v5.log`、`r29-h5-customer-build-v5-direct.log`、`r29-h5-engineer-build-v5-direct.log`、`r29-frontend-tests-v5.log`、`frontend-assets-after.json`：最终前端构建/27项行为回归/资源。v6只修后端在线枚举，不影响此前最终前端源码证据。
- `.local/r29-release-cli/summary.json`、`.local/r29-v6-prod-smoke/summary.json`：三类迁移数据库及最终prod真实镜像/字体/低权限/鉴权检查。
- `.local/r29-coverage-facts.json`：独立角色冻结的覆盖、有效性和清理事实，不作为第二份正式报告。
- `.local/r29-session-final-cleanup.json`、`.local/r29-resource-cleanup/summary.json`：退出/私密文件移除及14个自有测试容器精确清理。

## 29. R31 页面复测与容量优化

### 29.1 范围、版本与交付判定

本轮延续用户授权，在本机 PC5173/API8000、客户5174、工程师5175 进行合成资料复测；另以完全隔离的 MySQL/Redis/生产镜像执行容量试验。时间为2026-11-06至11-07（北京时间）。本机后端仍为 dev profile，不把它描述为公网生产。此前§28的全部模块/联合API历史结果保留其原始v6版本，本轮不将其改写成v9重新执行通过。

最终后端v9 SHA256为 `ee1eab3483e7cb15380f96db1318a332670dd469284cfa6906193e4d2c0198b1`：18个reactor模块、377项测试，失败/错误/跳过均0，1383份源文件与候选包构建清单匹配。已仅替换本机backend并核对实际容器内JAR、就绪接口、UUIDv4 Trace和JVM上海时区；原v7独立备份保留。部署前包内R29固定52项和R31固定1项均只读validate/status、pending=0，替换过程未执行DDL/master。

**目前不能判定完整生产交付验收通过。** PC部分页面、两H5真实验证码登录与列表已补齐；照片上传通道、真实GPS、完整三端页面联动及真实设备适配仍有未完成项。工程容量试验通过不等于未知业务SLA通过，标准参考不等于国家标准认证。

### 29.2 已落地修复

| 问题 | 最终实现与验证边界 |
|---|---|
| 工作台预警逐批加载及SQL扫描开销 | 改为数据库按商品聚合正库存，避免传回约25000批次实体；覆盖索引`(tenant_id,deleted,goods_id,warehouse_id,remain_quantity)`。保留仓库部门权限、商品权限、租户和BigDecimal边界；真实权限处理器测试和SQL独立数量核验。原有最多500个监控商品范围仍保留，不能宣称2000商品全部监控 |
| 高压下1—3ms时钟回拨导致Trace生成失败 | 正式TLogIdGenerator Bean改UUIDv4，覆盖框架默认Bean；业务雪花ID与CosID不变。真实filter/上下文清理、并发唯一性、CosID小回拨恢复及超限拒绝受控回归通过；最终600秒压力阶段无Trace错误 |
| 纯服务报价保存/加载自动弹新增商品 | 加载不再主动弹窗，明确点选物料仍保留提示。真实PC服务报价金额113、PDF及DOCX下载通过；PDF实际31379字节，不以按钮点击代替下载完成 |
| PC过期响应重复退出、嵌套登录回跳 | 按请求身份合并退出，拒绝旧会话响应清除新会话，安全内部return path、AxiosHeaders大小写读取。行为回归通过；真实长时间过期/刷新页面全链未重新穷举 |
| H5旧401及旧登录异步回调清新会话 | 客户/工程师身份隔离，公开请求无身份401不清会话；请求token及session generation匹配后才清理/延迟跳转，旧getInfo失败不清新登录。15项真实Axios/Pinia回归通过；两H5实际正常验证码登录、身份信息/列表业务code0已保存 |
| 定位等待只依赖浏览器回调 | 三处定位统一应用层期限，超时/权限拒绝/同步异常可结束等待，忽略迟到坐标；照片继续既有仅时间水印降级，工程师签到签退仍要求真实坐标。新增3项受控测试通过；真实设备及浏览器上传之后的效果尚未复测 |

R31迁移提供独立CLI、`scripts/migrate-r31.sh`和`docker/ls-aegis-server/MIGRATION-R31.md`；固定SQL与release摘要，拒绝索引列序/类型/前缀/唯一性/不可见等漂移，不自动修复历史。隔离MySQL实际21例通过：旧库增量、全新init已有索引MARK_RAN、只读无历史写、重复执行、错误目标/确认/checksum/结构拒绝。原R29固定清单未改。全新安装唯一入口仍为init.sql，不能宣称全master可无条件安装空库；PG本轮没有真实数据库验收。

在用库索引已通过包内R31工具明确目标增量应用；此前仅备份精确库存批次表，前后CHECKSUM一致，临时最小DDL账号已撤销。证据`.local/r31-live-index/summary.json`；包含真实数据的备份不得提交。

最终PC完整build（含类型检查）通过，Office三种渲染入口仍保持异步边界；入口约2293.2KiB/gzip581.0KiB，仍有大chunk告警，未提高阈值掩盖。最终两H5 build通过，前端行为测试48/48、无失败/跳过。相关日志为`.local/r31-pc-build-actual-final.log`、`r31-h5-customer-actual-final.log`、`r31-h5-engineer-actual-final.log`、`r31-frontend-all-final.log`；3项定位测试不是实际设备通过的替代。

### 29.3 页面实际覆盖与测试通道事故

| 实际页面路径 | 结果 |
|---|---|
| PC工作台、报价服务明细及导出 | 实际页面操作、金额回显、PDF/Word下载完成；保存`.local/r31-ui/pc-quote-fixed.png`等证据 |
| PC采购确认→库存 | 自有采购2件，含税113，实际库存成本100；页面和API/SQL一致。后续必须以原单采购全退恢复，保留历史 |
| 工程师H5正常验证码→身份信息→任务列表 | 实际成功，三接口HTTP200且R.code0，任务列表显示自有工程师；证据`engineer-network-login-proof.json`和`engineer-tasks-after-login.png` |
| 客户H5一次性访问码+验证码→报修列表 | 实际成功，认证与列表HTTP200/R.code0。进入报修表单，填写合成联系人/故障类型/描述，缺照片提交正确拒绝 |
| 上传、真实GPS与后续维修页面联动 | BLOCKED/NOT_RUN。文件选择后工具调用不按超时返回，返回时页面仍“正在获取定位”；不能断言照片已上传或报修成功。最终定位超时修复仅受控测试/build通过 |
| 移动尺寸和真实设备 | 未形成有效验收：早期360/390/768探针实际DOM宽度未变，不能将标签当适配证据。清除测试遗留CDP视口覆盖后，报修按钮可正常响应；该点击问题属工具坐标/视口状态，不列为产品缺陷 |

IAB可用，原“所有浏览器不可用”不再适用；原生Chrome来源限制仍未绕过。正常UI登录使用用户本次明确批准的三次验证码流程；正常API会话经公开验证码和SM2登录，没有读取验证码缓存/浏览器Token或关闭验证码。

必须如实保留工具停顿：早期上传约21606秒，视口调用约7824秒，最终H5 setFiles约44477秒，均远超请求超时。它们不是测试执行时长或负载持续时间；本轮不再次调用该上传通道。客户首次本轮登录500与自有后端替换窗口重叠，随后就绪后实际重测成功，未计入产品缺陷或成功用例。图片`customer-upload-blocked.png`保留未完成现场。

### 29.4 容量试验与适用上限

最终测试基于v9实际生产镜像、本地未推送，应用UID/GID10001、CST+0800、验证码开启、内部网络无宿主端口。合成规模为10000单位、2000商品、100000库存批次。应用2CPU/1536MiB（heap768MiB），MySQL2CPU/1GiB（buffer128MiB、限制performance_schema），Redis256MiB，压测端1CPU/256MiB。关闭思考时间的单位列表/工作台各半闭环负载；不代表同等数量的真实人类在线用户，也未涵盖长财务历史或外部服务。

| 最终v9阶段 | 观测值 |
|---|---|
| 1/5/10/25/50并发有界阶梯 | 请求错误均0，Trace合法UUIDv4；高并发延迟仍上升 |
| 10并发600.406秒混合负载 | 11511/11511成功，0错误，19.17请求/秒；总体P50约291ms、P95约1146ms、P99约1328ms |
| 上述工作台5755次 | P95约1209ms，P99约1430ms；20条预警互异、每条数量500与独立SQL一致 |
| 50并发工作台 | P95约5272ms，不能据此声明50并发满足未知响应SLA |
| 资源与日志 | 应用峰值约45.5%、数据库约47.14%内存预算，未OOM；数据库接近2CPU预算，连接池20/DB总21；最终应用ERROR/回拨Trace/权限上下文错误均0 |

保留失败历史，未选取有利片段替换：早期tmpfs基线25并发21.4%错误，聚合后仍0.86%；索引试验旧1GiB DB曾OOM，停止后改为磁盘卷和有界内存配置。新磁盘夹具初始化已有索引导致首次“无索引基线”无效，已排除并记录fixture-correction；仅在自有库明确删索引后重做同物理卷成对试验，无索引50并发108次中43超时，有索引268/268成功、P95约6063ms。v7索引后600秒10955次仍有1次Trace回拨错误；v8短120秒零错误未代替长测；修复Trace后的v9重新完成600秒11511次零错误。新磁盘组与旧tmpfs组不是同物理DB，不混算改善比例。

正式容量证据`.local/r31-capacity-disk/v9-results/summary.json`，早期失败/更正/JFR留在同目录。自有4容器、网络、命名卷/匿名卷已精确清理并回查不存在，16份私密夹具/会话文件已删除，生产镜像/JAR/脱敏日志保留。`.local/r31-capacity-disk/cleanup.json`为清理证据。本次为工程容量基准；没有约定业务SLA/可用性/容量目标，因此非功能正式验收仍不能判PASS。

### 29.5 最终独立回查、清理与剩余工作

最终v9正常管理员16个只读模块接口均HTTP200、R.success/code0及合法UUIDv4；客户无身份10007、工程师无身份401正确拒绝，没有用管理员冒充两H5。证据`.local/r31-final-verification.json`。这次只读检查不能替代写业务联合流程。旧隔离`run_business_flows.py/business_flows.mjs`仍有关闭验证码、Redis取答案、customerNo-only旧认证及无资源上限tmpfs夹具，不适合直接验证v9；本轮检查后未启动它，不能从旧脚本得出v9联合PASS。后续如使用必须先迁移其认证协议与隔离资源配置。

R31已停止新增业务写入，上传后重新按文件前缀、自有用户和合成图片/TXT指纹只读盘点，未找到成功文件，也没有自有维修申请/工单；未提交的浏览器表单不算已创建业务。原采购`909017964387238520`通过正式采购退货`909365389450805255`确认，2件/成本100/应付113恢复0；报价草稿20通过正常接口删除。未SQL修改库存、财务或恢复整库，原采购与退货审计保留。

独立24项净零proof全部0，覆盖库存数量/成本、PSI应收应付、资金未分配/待办/草稿、EOMS净收入/净成本/欠款/活动状态等。8项自有主数据禁用保留，5个自有用户、1角色、2部门精确删除，各保存会话重新查询被拒绝；自有H5在线会话0、活动对象0。临时客户邀请已消费或超过600秒，禁用客户也拒绝使用残留邀请；没有操作验证码/会话缓存。台账`.local/r31-ui-ledger.json`已closed，不允许重放；依据`r31-ui-cleanup-journal.json`、`r31-ui-finalization.json`。

会话辅助进程的真实停止标志已写；本任务自己的正常管理员API会话已POST logout并GET确认失效，凭据文件和含密码的临时登录脚本精确移除、保留文本按已知合成凭据脱敏，原管理员密码不变。证据`.local/r31-session-final-cleanup.json`。不读取浏览器Token，也不通过猜测会话标识撤销其他管理员登录。R29标签核对后的v7/v8/v9三个自有构建容器已删除，JAR/镜像/部署前备份及脱敏证据保留。

| 尚待完成的验收 | 当前判定 |
|---|---|
| 照片上传→客户报修→PC派单→工程师签到/记录/完工→客户确认→财务的完整页面链 | BLOCKED/NOT_RUN：上传通道无法可靠完成，真实GPS未验证；§28的API历史联合结果不能替代 |
| 最终定位期限修复的真实浏览器/设备重测 | NOT_RUN：仅3项受控回归和两H5最终构建通过，不宣称工具长停顿已解决 |
| 移动设备布局、首屏/网络瀑布与页面操作完整覆盖 | NOT_RUN：此前尺寸探针无效；不能由API耗时推出页面加载速度 |
| v9完整新一轮写业务联合回归 | NOT_RUN：本轮完成变更影响测试、真实部分UI、只读模块冒烟和容量；旧联合runner需先升级 |
| 指定公网生产SLA、灾备和真实外部服务 | 未执行：本机dev、隔离prod及内部测试证书不能替代 |

测试规范继续采用仓库分阶段Agent/Skill及§28.8已核验的国家标准框架；不编造国家标准响应阈值，不将未完成项目改为N/A或PASS。最终状态是“修复、构建、容量基准与本轮数据清理完成，完整页面/生产交付验收尚未通过”。

### 29.6 2026-11-07 覆盖空白盘点与补测

本次依据验收矩阵、源码、现存前端用例和上述未完成清单重新盘点。当前工作区另有R32迁移及文件/权限相关修改与自有容器，不能据§29.1旧v9标识假定当前全部源码和运行包相同；本节只声明当前源码实际执行的前端测试及当前本机匿名请求结果，不把既有后端377项当作本次重新执行。保留现有工作区和容器。

已有但尚未纳入§29正式结论的13项前端用例，本次实际执行通过，涵盖客户/工程师上传乱序、失败项删除、上传中禁止提交、移除后迟到上传、表单重复提交、水印预览失败仍保留成功附件，以及鉴权图片切换/卸载的资源释放。连同原48项，补测前基线61/61通过；这些是受控组件/请求测试，不代表真实浏览器上传已完成。

新增7项测试：H5 ID工具3项（空数组项、完整雪花ID、空值/零值一致性），相机文件规范化4项（blob补后缀、PNG MIME与字节保留、大写后缀保持原File、缺文件与隐藏名称）。先运行ID测试复现1项失败：`[null,undefined,'909017964387238520']`被转为`['null','undefined','909017964387238520']`；修复`toBizIdList`复用标量空值规则后，最终前端68/68通过，失败/跳过0。当前未查到`toBizIdList`调用方，判定为工具层潜在缺陷，不能声称已复现某业务页面故障。未改变雪花字符串精度、零值和有效项顺序。

最终客户及工程师H5构建均通过。证据：`.local/r33-biz-id-before.log`保留失败基线，`.local/r33-frontend-baseline.log`、`r33-frontend-final.log`、`r33-h5-customer-build.log`、`r33-h5-engineer-build.log`记录本轮实际结果。新增测试入口为`scripts/quality/h5_biz_id.test.cjs`及`h5_upload_image.test.cjs`。

本机L0实际9/9通过；首次沙箱内网络访问失败，允许本机网络访问后重测通过，不计产品服务故障。新增可重复只读脚本`scripts/quality/anonymous_module_check.py`实际请求18个模块入口：平台身份、PSI基础资料/采购/销售/收付/工作台、报价、EOMS工单/统计、签章统计、文件、H5在线管理及两H5受保护列表。全部返回统一R拒绝，普通接口401、客户H5 10007；证据`.local/anonymous-module-check.json`。此结果仅证明匿名鉴权入口，不证明各接口登录后业务正确，也不构成角色间越权验证。

本次没有创建业务数据、身份或会话，业务写入0，无新增业务清理义务。剩余空白仍包括：最终统一版本的全部写业务联合重跑、真实照片上传/定位及三端完整页面链、移动设备布局与页面加载测量、指定公网目标与外部服务。租户/开放平台/调度仅在当前实际关闭配置已证实时维持原适用性判定，不能笼统宣称“所有功能已测试”。

## 30. R34 照片定位与维修联动补测

2026-11-07按用户授权继续在本机在用API8000补测，使用独立`ITLS-TEST-R34`和`ITLS-TEST-R35`资料；正常公开验证码/SM2登录，借用R32专用角色身份并刷新本任务自己的会话，没有重置密码或扩大角色权限。本轮选择真实HTTP上传及业务API联动，未再次调用曾长时间挂起的浏览器文件选择工具；接口验证与设备页面验证分别记录。

### 30.1 上传及定位缺陷修复

真实客户PNG上传复现失败：`WatermarkUtils`统一输出JPEG，两个Controller却保留输入`.png`名称，文件中心因此正确拒绝扩展名与魔数不符。修复新增`EomsPlatformFileHelper.uploadWatermarkedJpeg`，客户/工程师两入口统一安全basename及`.jpg`，上传显示名/响应名称一致；保留普通文件魔数检查和既有JPEG质量、字体规则，没有关闭校验来接受伪装文件。

另修复定位只判非空的参数边界：经度必须是有限数且在[-180,180]，纬度在[-90,90]；NaN、Infinity、越界、空白、缺单边参数在业务状态变更或文件写入前拒绝，0为合法坐标。照片未提供双方坐标时仍可仅时间水印降级。`Coordinates`与真实PNG/JPG/JPEG编码、文件魔数、名称、Controller前置校验33项定向测试通过。

最终v12 JDK21完整包594项测试，失败/错误/跳过0，1412份源码哈希与构建快照一致；实际部署SHA256 `c32b1a988e4dbb9bcfa5c40a976703a84e57619fd691a8b20423eae47954f4db`。仅替换本机backend，原v11备份保留；R29/31/32包内只读validate/status分别52/1/1项、pending0，无DDL。证据`.local/r34-watermark-fix-summary.json`、`r34-watermark-test.log`、`r34-build-v12-evidence.json`、`r34-deployment-v12.json`。运行环境仍为本机dev，JVM上海时区。

### 30.2 v12真实照片与维修财务完整接口联动

R34在v12执行并登记申请12、工单22、结算20。先确认采购2件，再执行以下同一客户/商品/仓库链：

| 阶段 | 实际断言 |
|---|---|
| 客户PNG照片上传→预览 | 真实multipart上传成功，鉴权预览字节为JPEG；另一客户预览拒绝。3组照片可选坐标不完整/NaN/越界均拒绝 |
| 客户报修→管理端确认转单派单 | 文件关联同一申请；伪造customerId/tenantId未改变真实归属；错误状态客户确认拒绝 |
| 工程师签到→维修照片/记录→签退 | 非负责工程师及缺坐标拒绝；start/finish共8组非法坐标拒绝，前后工单状态保持不变；使用合成0,0完成成功路径。工程师维修中/完成两张真实JPEG上传、鉴权预览成功并关联维修记录，其他工程师写记录拒绝 |
| 客户确认→结算→应收→实际收款 | 跨客户拒绝；并发确认仅1事件、首次时间/原因不被重试覆盖；材料实际FIFO成本50，预估成本999未影响实际成本；结算含材料80+服务20，应收/实际收款100 |
| 冲销→反结算→关闭→采购全退 | 实收正式冲销、结算反确认、工单CLOSED，原采购全退；最终库存数量0、EOMS收入/成本净0；客户确认历史保留 |

最终用例`R34-H5-APPLY-ENGINEER-CUSTOMER-CONFIRM-FINANCE-JOINT`为PASS，记录实际v12 SHA，证据`.local/r34-ledger.json`、`r34-h5-joint-after-v12.log`。成功照片及业务关联作为合成维修审计保留，未把有引用文件删除当作清理要求。原PNG失败及一次二进制拒绝响应被测试脚本误按JSON解析的失败均保留在台账；后者修正为检查HTTP拒绝状态，无额外业务写入。断点续跑只复用精确已确认原采购、核对归属/物料/数量2，不重复采购或覆盖原单。

### 30.3 其他业务链、前端行为及测试自身问题

R35实际执行采购→销售→三笔收款/付款核销→银行排他匹配及解除→销售/采购全退→退款/反退款/作废→银行退款匹配净0；报价并发转工单仅1张→工程师维修→PSI材料出库→结算应收实收→收款冲销/反结算/关闭→采购全退；发票限额/归属、红冲幂等/重复红冲拒绝和工作台回查通过。数据及原/反向审计保留在`.local/r35-ledger.json`和`r35-phase-*.log`。

R35一项红票待办断言因步骤顺序错误而失败：本轮先退货后开票，原用例的前提是先开票后退货。未按产品缺陷处理，也未覆盖失败；新增显式`--returned-before-invoice`分支，必须校验台账退货完成时间早于开票时间，使用独立用例名验证实际无前置红票提醒、随后红冲金额/幂等及终态。没有将其声称为“先开票后退货提醒”本轮通过。

R35复制的阶段脚本还错误传入旧v9部署证据标签，已在closed台账追加`versionAudit`明确原标签无效并保留原文。v12部署前独立捕获的旧JAR SHA与v11一致，可佐证当时为v11，但不把错误标签当每阶段版本证明，也不宣称R35在最终v12重跑。测试入口现对显式部署证据额外读取实际容器JAR SHA，不匹配即在业务阶段前停止；真实当前v12接受、旧v9拒绝2/2通过，证据`.local/r34-version-guard-proof.json`。v12影响的上传/坐标与维修财务链已在R34独立实际重测。

新增5项真实工程师组件操作函数测试：签到/签退定位拒绝不调用写接口且释放loading，成功坐标和字符串ID原样传递，取消不请求定位。前端累计73/73通过、失败/跳过0，`.local/r34-frontend-final.log`；组件测试不等于真实硬件定位。

### 30.4 清理与最终边界

R34与R35分别有21项及24项独立源表净零断言，库存/成本/应收应付/未分配资金/维修收入成本欠款/活动单据与待办均为0；R35另含银行及现金净额。两轮活动自有对象0、台账均closed，拒绝重放。原单/退货/红票/冲销/维修照片及确认记录保留审计，相关自有基础资料禁用，无有效引用的另一单位正常删除。没有SQL写库存账务或整库恢复。

两客户H5正常logout后再查询被拒绝；本轮刷新得到的5个员工会话和管理员会话逐一正常退出或确认已过期，复制的R34/R35身份/token文件、含密码的临时登录脚本及验证码文件精确移除。R32共享账号/部门/角色及其原台账属于另一轮，未由借用轮删除；没有改原管理员密码。证据`.local/r34-session-cleanup.json`及两轮`finalAudit`。

本轮补齐的是实际HTTP照片上传、坐标边界、接口业务联动及反向补偿。真实设备GPS/逆地理外部服务、浏览器相机/上传页面全链、移动布局仍未执行；成功路径使用合成坐标，当前没有外部地图调用，不据此声称物理位置真实性或完整生产交付验收通过。此边界保留，未把API通过改写成页面/硬件通过。

## 31. R36 真实设备与外部地图验证

### 31.1 实际设备与服务前置检查

2026-11-07继续按用户授权验证真实定位、摄像头和外部地图。使用当前本机和IAB实际浏览器，未使用仿真摄像头、固定位置覆盖或伪造定位成功。Windows PnP清单未发现Camera/Sensor设备，仅发现打印扫描Image设备；这不能推断所有网络定位服务都不可用，因此另执行浏览器真实请求。

在仅loopback8766的临时诊断页，通过实际按钮调用浏览器`navigator.geolocation`及`getUserMedia({video:true,audio:false})`；定位直接复用项目当前H5 `utils/geolocation.js`。浏览器是安全上下文、两个API均存在，实际摄像头返回`NotFoundError: Requested device not found`；真实定位未取得坐标，最终由应用层期限返回code3超时。只记录状态/错误，不保存精确坐标或影像。证据`.local/r36-device-browser.json`、`r36-device-browser.png`；这是设备探针，不冒充已登录的报修页面。

只读查询确认`GEO_BAIDU_AK`及自定义逆编码地址均NULL。向官方`https://api.map.baidu.com/reverse_geocoding/v3/?output=json`仅发送一次无AK、无坐标请求，实际HTTP200/业务status101、“AK参数不存在”，说明提供方入口可连通，不能说明逆地理编码成功。未搜索或借用他人密钥，未设置选项，未调用付费成功业务，证据`.local/r36-provider-connectivity.json`。

实际成功路径仍BLOCKED：当前机器/浏览器没有可用摄像头、未获得位置，且没有有效地图AK。浏览器相机拍照→上传页面的成功流程、真实坐标→外部地图地址，均不能改写成PASS；§30的真实multipart上传与合成坐标接口结果继续保留其原适用范围。

### 31.2 本轮发现与修复

地图此前复用legacy `HttpUtils.sendGet`，缺少连接/读取期限，并记录含AK、坐标的完整URL、原始响应和异常信息。现仅地图路径改用专用HttpClient：连接1秒、完整响应异步等待3秒并取消超时请求、不跟随重定向；参数编码、HTTP(S) endpoint结构校验和坐标校验前置。日志仅保留HTTP状态或异常类型，不记录AK、坐标或原始响应。没有将其他legacy HTTP调用宣称为已修复。

地址解析原按返回顺序取住宅POI，现选择有效非负距离中的最近住宅，异常数据降级到格式化地址或坐标。新增17项实际loopback HTTP服务受控测试，覆盖无/空/空白AK零外呼、参数编码、HTTP异常、畸形JSON、无有效地址、请求头/响应体分别延迟及期限取消、日志脱敏和地址选择；这些是实际本地HTTP替身，不等于百度提供方成功验收。测试每例关闭HttpServer及自有线程池。

PC地图配置页另复现3个行为测试失败：缺应用层等待期限、迟到定位仍结算、忙碌状态重复点击。最小修复为10秒应用层定时器、Promise一次结算并清理定时器、同步provider异常结束等待、测试按钮防重复；无AK时仍提前返回，不请求位置。8项真实TS函数行为测试及全前端81项全部通过，失败/跳过0；证据`.local/r36-pc-geo-baseline*.log`、`r36-pc-geo-regression.log`、`r36-frontend-all-tests.log`。PC完整构建包含类型门禁通过，Vite实际4分13秒；仍有既有browser crypto externalized及大chunk告警，未改阈值掩盖，日志`.local/r36-pc-build.log`。浏览器挂起/后台节流会影响定时器调度，不能将受控测试的10秒当所有设备上的精确墙钟保证。

### 31.3 最终运行验证与清理

v13完整JDK21构建611项测试，失败/错误/跳过0，1413份源码哈希与构建快照一致，实际运行SHA256 `6f7f5ecf44d4ab351f82f21cf90398cda615b75d0060df175bfa95bc4f97d9b1`；候选R29/31/32包内只读validate/status分别52/1/1项、pending0，未执行DDL。仅替换本机backend，原v12包备份保留，dev profile与上海JVM时区。地图侧最终定向38项（17地图HTTP+既有21坐标）通过，不把重复执行计为新增38项。证据`.local/r36-build-v13-evidence.json`、`r36-deployment-v13.json`、`r36-map-summary.json`。

通过正常公开验证码/SM2取得本轮自己的管理员API会话，在v13真实调用`POST /eoms/config/baidu-ak/test`：合成0,0且未配置AK时外层R成功、内层success=false并明确提示先配置AK；空坐标由参数校验拒绝。没有读取AK明文或修改配置，没有向地图服务发送真实坐标。随后POST logout并再次确认身份已失效，精确删除本轮session、含密码临时登录脚本及验证码文件；证据`.local/r36-live-map-check.json`。三端本机L0重测9/9通过，不能替代未执行的拍照页面成功链。

临时设备诊断页已关闭，8766仅loopback服务已停止且独立TCP回查端口关闭；测试HTTP替身均已关闭。未创建客户/工单/照片等业务对象，未保存摄像头帧或真实坐标，没有业务补偿或图片删除义务；保留脱敏日志、设备失败截图和复现探针源码。证据`.local/r36-device-cleanup.json`。

最终结论：真实浏览器请求已执行，摄像头缺失和定位超时得到实际失败证据；地图缺AK提示、异常/超时/降级及日志保护已修复验证。真实拍照上传成功、有效位置到百度地址成功仍BLOCKED，前置为可供测试的摄像头/定位终端及有效地图AK。不能靠合成坐标、摄像头仿真、借用他人密钥或本地HTTP替身将其判为通过。

## 32. 逐功能业务流程实测与清理

### 32.1 范围与最终版本

2026-11-07按用户要求逐项执行本机业务接口流程，独立使用`ITLS-TEST-R38`资料，并完成旧R37遗留清理。摄像头、真实GPS和地图AK按用户要求跳过，不记为通过，也不阻塞其他业务测试。目标仍为本机PC5173/API8000、H5端口5174/5175；dev profile不能描述为公网生产。

R38台账44条历史结果，按用例ID去重后42项均PASS；另有普通客户门户流程及两个JPEG回收生命周期的独立证据。用例分版本保留：v14基础资料1项、v15业务31项、v16文件等10项，未将旧用例标签改成最终版本。文件修复未改进销存、维修财务或报价服务，相关业务结果保留原版本；最终文件/门户/签章依赖链在v16实际复测。本节不据此宣称所有路由、全部浏览器页面或公网交付已完整验收。

最终v16运行SHA256 `c5c4d153be7fe4ffe98db9b4b872cbb3faf230dbe0d6eae33f1757d75067a153`，JDK21完整构建663项、失败/错误/跳过0，1417份源文件与候选构建清单一致。仅替换本机backend，v14/v15原包分别保留；包内R29/31/R32只读validate/status分别52/1/1项、pending0，没有执行DDL或全量安装脚本。证据`.local/r38-build-v16-evidence.json`、`r38-deployment-v16.json`。

### 32.2 逐功能实际结果

全部写操作限定合成资料；按顺序等待每个步骤完成，涉及重试/并发的用例只对同一合成记录执行有界请求。真实银行传输、外部正式证书及广播未使用。

| 功能 | 实际执行与断言 | 结果 |
|---|---|---|
| 基础资料、身份与菜单 | 独立客户/供应商、两种商品、分类、两个仓库；正常管理员身份、菜单和接口入口；借用既有专用测试员工，不改原账号/角色 | PASS |
| 采购、库存、销售 | 采购入库10件/1000元；销售5件/650元，实际库存/FIFO成本及单据金额一致 | PASS |
| 收付款与核销 | 分3笔收款300/300/50、付款1000；来源关联、分配、撤销及超额业务校验 | PASS |
| 银行流水 | 排他匹配、解除匹配、退款对应流水；最终6条均匹配、银行净额0，未调用银行网络 | PASS |
| 退货退款与发票 | 先开票后退货；销售/采购全退，退款、反退款、作废；红票提醒与红冲、重复请求仅一条有效红票，待办归零 | PASS |
| 盘盈盘亏、调拨、盘点 | 盘盈5件/成本100；调拨2件、每次成本40，往返数量成本守恒；盘亏补偿归零；零差盘点不改库存，同库/负数/缺货请求正确拒绝 | PASS |
| 原单采购退货 | 两张原采购；原单明细、商品、仓库和累计数量校验，最后全退补偿归零 | PASS |
| 独立报价 | 服务报价金额100，DRAFT→SUBMIT→DRAFT，内容渲染及DOCX/PDF实际下载；草稿正常删除 | PASS |
| 报价兼容维修链 | 报价220并发转换仅1张工单，工程师维修记录及材料，经PSI出库、结算应收、实际收款、冲销和关闭；预估价不替代实际FIFO成本 | PASS |
| 普通客户门户 | 正常一次性访问码+公开验证码登录；测试图上传、报修提交、列表/详情、正常退出及退出后登录态失效；申请和图片正常清理 | PASS |
| 平台文件 | TXT/PDF内容、下载字节/SM3、回收/还原及数量恢复；业务JPEG元数据回收/还原/删除另见32.4 | PASS |
| 公告与消息 | 合成公告草稿；仅专用测试接收人消息、已读/未读及删除状态一致，未真实广播 | PASS |
| 平台入口与工作台 | 三端HTTP入口、路由/选项；20个工作台字段对源表独立只读汇总核验 | PASS |
| 合成签章 | 合成文件、内部自签证书；普通/合并分别有界并发仅1成功，回执verifyStatus=1；实际下载并独立OpenSSL CMS验证完整性，内容改变后验证失败 | PASS |

R38独立门户申请14、照片`909441670372655140`已清理；图片来自生成的测试JPEG，不涉及摄像头或定位。门户证据在`r38-ledger.json`的`portalAuthentication/portalBusinessProof`，各接口用例在`results`、`r38-phase-*.log`，去重统计与最终事实在`.local/r38-final-facts.json`。租户/开放能力/调度未为测试开启，不能把未启用功能算作业务通过。串行少量工作台采样不判性能容量验收PASS。

### 32.3 本轮修复与回归

| 发现 | 修复与验证 |
|---|---|
| 手工表头可为负，非分币金额与尾差可能不守恒 | `DocTaxHelper`拒绝负数及无法精确表示为分币的手工金额；保留零价、合法±0.05尾差和同商品不同价行。旧行为2项基线复现，29项定向通过；v14实际请求6个负价/负表头/非分币分支，独立回查无新增业务记录 |
| 维修用料详情/列表缺少父工单范围限制，写入边界不足 | 公共读取按父工单及可信租户限制；写入锁工单与当前明细，禁止迁移工单、保护审计字段，校验数量/价格精度及商品仓库引用；内部结算`lockByOrder`保持原链。材料17、共享查询4及结算/取消等共73项定向通过；历史停用主数据不隐藏既有材料，实际负责工程师仍可见自己的历史材料 |
| 公共查询对象残留或带入服务端范围标记 | 每次应用时先清8个服务端标记，再按当前身份重建，保留日期及业务租户条件。4项真实MyBatis/H2基线失败→修复通过；真实接口回查范围不再扩大 |
| 前端异步返回覆盖当前单据 | 报价、4个库存/收付款编辑器及Excel导入、签章PDF增加当前编辑版本/身份捕获和迟到结果保护。新增28项实际Vue TS函数行为通过；前端总109/109，完整类型检查/PC构建及Office异步边界通过。既有大chunk/crypto告警保留，未提高阈值隐藏 |
| 本地回收无法移动带业务metadata的文件 | 仅LOCAL使用SDK兼容选项；不清数据库归属元数据，不改变云存储默认策略 |
| 生产文件记录器在还原回调中再次删除记录 | 还原先物理移动、成功后恢复数据库状态；旧半还原状态只有在回收源不存在、原图存在且大小/SM3匹配、缩略图存在/大小匹配（有可信摘要时也匹配摘要）时才恢复，冲突或不匹配不动文件 |

证据分别为`.local/r37-psi-summary.json`、`r37-material-summary.json`、`r37-frontend-summary.json`、`r37-write-boundaries.json`及`r38-file-summary.json`。受控函数/数据库/SDK测试不能写成浏览器页面点击通过。

### 32.4 文件卡点的真实闭环

旧R37客户报修流程最后清理照片时，LocalPlus SDK拒绝metadata移动；v15兼容修复的初次测试使用默认SDK记录器，遗漏生产`FileRecorderImpl`回调，导致实际还原接口虽成功响应，记录仍在回收站。没有直接SQL改状态或删除磁盘文件来掩盖结果。补入真实生产记录器、真实SDK、本地临时磁盘、H2/MyBatis和Spring事务代理后复现，再修正顺序及旧半状态恢复；最终14项文件定向通过。

v16通过正常还原API核验恢复旧照片`909432000635470176`，随后完整执行回收→还原→再次回收→精确删除，原图下载字节及数据库业务metadata一致。新R38门户照片也完成同一生命周期，两个文件最终活动/回收记录均0。证据`.local/r38-recycle-r37-h5Upload.json`、`r38-recycle-r38-portalPhoto.json`。缩略图无可信历史摘要时只记录存在/大小校验，不声称验证其历史哈希；不宣称文件系统与数据库构成通用分布式事务。

旧R37当时的FAIL原文保留，不改成历史PASS；新增恢复及新门户成功证据独立登记。一个功能失败时单独登记待修、继续不受影响模块，修复后回到该功能重测，不无限重复整批业务。

### 32.5 数据与会话清理

R37、R38各有24项独立源表净零断言：库存数量/成本、应收应付、未分配资金、活动工单/结算/实收、维修净收入成本、待办、未登记草稿、银行及现金净额均0。两轮活动自有对象均0、台账均closed，禁止重放。每轮8项有审计引用的基础资料禁用，未引用的另一单位正常删除；确认单据、红票、冲销及合成签章证据按业务审计保留，不SQL删除账务流水或整体恢复数据库。

本轮自己的PC会话正常退出或确认过期，客户门户已正常退出并回查失效；R37/R38复制的token/账号文件、含密码的临时登录脚本及验证码文件精确移除，会话检查辅助进程已停止。借用的R32专用账号/角色及共享构建缓存不由本轮删除，原管理员密码不变。未新增R38容器/网络/卷；测试临时目录/数据库进程结束，日志、候选包和部署前备份保留。

清理证据`.local/r37-ledger.json`、`r38-ledger.json`的`finalAudit`、`r38-session-cleanup.json`、`r38-test-resource-cleanup.json`。清理后本机三端L0冒烟9/9，`git diff --check`通过。本轮结论是上述逐功能接口和正常业务链通过、修复已回归、合成数据清理闭合；不等于所有页面/所有外部服务或国家标准认证通过。

## 33. R39 页面实测与 R40 完整业务联动回归

### 33.1 环境、证据与验收口径

2026-11-07继续执行用户指定的本机在用环境：PC5173、客户H5 5174、工程师H5 5175、API8000，当前为dev profile。仅使用R39/R40合成业务资料及既有R32专用员工；摄像头、真实GPS、地图AK继续跳过。没有开启租户、开放能力或调度，没有修改原管理员密码、权限、验证码策略或在用配置。沿用项目分阶段测试标准框架，页面、函数回归、HTTP业务联动分别记录，不等同国家标准认证。

后端沿用§32最终v16，SHA256 `c5c4d153be7fe4ffe98db9b4b872cbb3faf230dbe0d6eae33f1757d75067a153`；本轮没有修改后端。R40最终联动全部对应该版本，未混入v14/v15结果。此前663项后端构建结果保留原日期和范围，不声称本轮重跑。前端最终源码摘要及统计见`.local/r39-r40-final-facts.json`。

### 33.2 PC 与两端 H5 的真实页面操作

| 页面流程 | 实际操作及结果 | 证据与边界 |
|---|---|---|
| 往来单位 | 空名称校验，创建、编辑、搜索合成客户及联系人，签发本轮访问码 | `r39-ui-ledger.json`；没有改真实客户 |
| 客户门户 | 正常验证码登录；报修必填校验、实际选择并上传测试JPEG、提交、列表及详情照片显示 | 申请15/BX26110707，照片`909448529955520749`；文件上传期间应用自动定位超时后使用时间水印，没有真实GPS成功证据 |
| 工单与工程师端 | PC确认申请、转工单27/GD26110718、派专用工程师；工程师H5任务筛选、详情、维修记录空校验及文本保存 | PC负责开始维修，跳过要求GPS的H5开工/完工动作；管理员工程师入口、个人页及正常退出另实际验证 |
| 结算与客户确认 | PC提交维修后自动生成草稿25/JS26110716，编辑人工费20、确认；客户页面确认前后提示、时间、备注；PC冲销及取消关闭 | 实际浏览器流程仅服务费，没有领料及收款；有材料和实际收款的完整链见33.4，不能混写为浏览器同一条全链 |
| 平台文件 | 测试TXT实际上传、详情42字节/SM3、下载内容一致、回收、还原；列表总数38→39→38→39 | `downloaded-file.txt`及`file-restored.png`；TXT不在预览支持格式内，未判TXT预览通过 |
| 独立报价 | 必填校验、服务100加4%税得104、金额大写及预览；实际下载Word/PDF并解析内容；提交、退草稿、删除 | 43/BJ26110723；`quote-download-proof.json`，DOCX119609字节、PDF31542字节/1页 |
| 印章提取 | 实际选择合成TEST ONLY PNG、提取预览、保存、下载、删除提取记录 | 记录3，输出58602字节；三个关联平台文件登记后通过API回收清理 |
| 签署及验真页面 | 项目空表单校验、签署入口及禁用控件；读取既有R38合成记录详情，实际下载签章PDF/回执并查询回单 | 未完成浏览器签署提交和PDF上传成功校验；HTTP合成签章独立通过，不能替代页面成功路径 |
| 两端移动布局 | 实际390像素视口登录页、失效会话回登录、标志加载失败降级 | `customer-login-390.png`、`engineer-login-390.png`及`h5-mobile-expired.json`：宽度390，无横向溢出、无破损图片；不代表所有移动页面布局通过 |

最终只读巡检覆盖66个菜单叶子中的64个可直接访问页面；另2个需要上下文的公告查看/字典项路由不以无参数直开计PASS。字典页另实际选择工单状态，核对6个枚举值。64页均有非空渲染、无页面错误提示，巡检时间段新增控制台error为0；其中用户列表一次networkidle超时，随后实际DOM已显示内容，单独记录`timeout-observed-dom`，不伪造等待成功。配置11个面板仅只读；代码生成未生成代码、期初建账未调整余额。证据`r39-ui/final-page-health.json`、`remaining-pages.json`、`dict-order-status.txt`。该巡检证明路由可渲染，不代表每个按钮、CRUD或外部服务均已验收。

### 33.3 页面发现的缺陷及修复

| 发现 | 修复、实际复核及测试 |
|---|---|
| 入库/出库VOID状态未明确显示 | 两列表增加红色“已作废”，浏览器核对合成出库CKD26110726 |
| 结算新增表单只有工单文本、缺关联ID，提交被拒绝；关联字段和合计可编辑 | 列表改为提示“工单提交结算后自动生成草稿”，仅编辑现有DRAFT；锁定工单/客户/合计，合计自动计算；增加编辑代次和ID检查，防迟到详情/保存覆盖。实际编辑、确认、冲销通过，5项真实TS函数回归通过 |
| 客户详情把FINISHED直接显示为“客户已确认” | 时间线按实际记录构造，客户确认取独立确认事件，关闭独立显示。浏览器确认前不再误报，确认后显示实际时间和备注；5项进度函数回归通过 |
| H5登录配置图片失效显示破图 | 两端复用SystemLogo，错误时显示LS；移动视口实际显示降级标志，无破图 |
| 验真请求因会话失效报错，却显示“校验失败”；旧请求可覆盖新文件/回单 | 请求未完成改为warning及重试提示，不能当文件完整性失败；请求代次、所选File身份及重复提交保护。5项基线失败后5项回归通过；修复后的成功上传页面仍受工具限制，未记PASS |
| 历史操作日志把框架注解配置提示展示为业务描述 | 精确识别该框架占位，展示“操作描述未记录/模块未记录”；不伪造或改写历史审计。两项函数回归通过，浏览器重载后占位提示0。修改期间短暂HMR缺import错误已修正，最终巡检无新增error |

新增17项行为测试，最终前端总126/126、失败/取消/跳过0；日志`.local/r39-frontend-delivery.log`。最终PC完整类型门禁及构建通过，Vite4分45秒，日志`r39-pc-build-delivery.log`；H5客户/工程师最终构建分别4.79秒/4.65秒通过。保留既有crypto externalized与大chunk告警，没有上调阈值掩盖；构建耗时不是页面加载性能指标。

### 33.4 最终同版本 HTTP 业务联动

R40独立登记43个用例，按ID取最新结果全部PASS；原始历史47条中46 PASS、1 FAIL保留，没有删除失败历史或把补偿重跑计为新增用例。覆盖基础资料、采购/销售/FIFO、分笔收付款/核销、银行匹配与解除、全退退款/反退款、先开票再退货及红冲、库存调拨盘点调整、原采购退货、独立报价导出、报价转维修、维修冲销取消、平台文件、合成公告草稿、专用接收人消息、工作台20字段源表核对，以及合成普通/合并签章。签章同请求仅两路有界并发，各仅1成功；回执verifyStatus=1，下载后独立OpenSSL完整性/篡改校验通过。内部自签测试证书不证明正式外部信任链；银行仅本地账务，不发送真实支付。

另补客户→工程师→库存→结算→实收→反向补偿完整HTTP链：正常门户登录及JPEG上传→报修16→转工单29→派单→PC开工→工程师维修记录/材料→PSI出库1件→提交结算自动生成草稿28→客户确认→人工20加材料80→确认应收100→实收100→收款冲销→结算冲销→关闭→采购2件全退。真实FIFO成本50，不采用请求中的预估999；收入在收款前为0、收款后100、冲销后0，最终库存和维修净成本归零。该R40链没有调用相机、定位或地图。证据`r40-ledger.json`的`h5NoGpsProof`及`r40-h5-joint-*.log`。

原始FAIL为测试角色资料范围不匹配：专用finance部门范围不含管理员建立的本轮工单，收款请求返回应收不存在且没有落款。未扩权；先独立核对CONFIRMED应收100、有效收款0和收入0，再用已授权管理员完成本轮收款及完整冲销。原因记入`h5NoGpsFinanceScopeCorrection`，最终同用例新增PASS，保留原FAIL，未当成已发现产品缺陷。

### 33.5 清理、退出与仍未完成的范围

R39通过正常接口删除联系人、报价草稿、提取记录及其3个登记文件、独立TXT；本轮客户因工单审计引用禁用，申请15、关闭工单27、冲销结算25及引用照片保留审计。11项独立源表净零检查全部0：自有活动联系人/报价/提取/已清理文件、未关闭工单、有效结算/收款、收入/成本/欠款和有效维修出库。保留引用照片不称为未清垃圾，也不强删审计。

R40清理后24项独立源表断言全部0，涵盖库存数量/成本、应收应付、未分配资金、待办、活动工单/结算/维修实收、维修净收入成本、未登记业务草稿、银行与现金净额；活动自有对象0。8项有审计引用的主资料禁用，未引用另一单位正常删除。两轮台账closed，禁止重放；未执行SQL写业务、安装脚本、整库恢复或删除账务流水。证据`r39-ui/cleanup-proof.json`及`r40-ledger.json.finalAudit`。

PC实际正常退出并确认回登录，H5会话已失效，测试标签页关闭；R40自己的管理员及5名员工API会话正常退出或确认已过期，再回查身份均失效。复制的token/密码文件及验证码4件精确删除，原R32共享身份资料不动。Downloads中6份本轮下载副本按精确路径、时间、SHA256与保留证据逐一比对后删除，不清空用户下载目录；脱敏证据保留在`.local`。证据`r39-ui/browser-final.json`、`r40-session-cleanup.json`、`r39-ui/download-cleanup.json`。最终本机三端L0只读冒烟9/9通过，日志`r39-final-service-smoke.log`。

浏览器文件选择工具曾阻塞约13600秒，导致会话过期；此为自动化工具阻塞，不作为产品上传耗时或性能缺陷。之后不再重复该调用；尝试点击上传后寻找原生文件对话框，未出现可操作窗口。因此浏览器PDF成功上传→验真仍BLOCKED，相关错误/迟到请求逻辑仅函数回归通过；浏览器完整签署提交、全部按钮组合、有效外部服务及容量SLA也未验收。摄像头/GPS/地图AK保持用户指定跳过。当前结论为已列业务联动、页面操作和构建通过，数据清理闭合，不能据此宣布所有功能或完整生产交付标准已经通过。

## 34. PDF 上传验真与完整签署接口补测

### 34.1 本轮范围与最终运行版本

2026-11-07按用户新指令跳过浏览器，使用代码检查、受控回归及真实HTTP接口补齐PDF上传验真与签署链。环境仍为本机在用API8000/dev profile，非公网生产；摄像头、真实GPS、地图AK继续跳过。普通公开验证码/SM2登录取得本轮管理员与既有专用limited身份，没有读取浏览器会话、修改密码或关闭验证码。只使用ITLS-TEST-R41/R42合成PDF、测试印章、内部自签证书，无真实合同、真实印章、外部CA/TSA或付费调用。

最终v18运行SHA256为`a5372886a52db9146233b1510844b145de6453198d439280489d6e54db14b7bf`。JDK21完整构建674项，失败/错误/跳过0；1419份当前源文件与实际构建源清单一致。原v16/v17分别备份；仅替换本机backend，保留dev配置与上海JVM时区。包内R29/31/R32只读validate/status分别52/1/1项、pending0，未执行DDL。证据`.local/r41-build-v18-evidence.json`、`r41-source-manifest-v18.json`、`r41-full-build-v18-retry.log`、`r41-deployment-v18.json`。

### 34.2 发现、修复及可复现回归

| 实际问题 | 修复及证据 |
|---|---|
| 上传合成自签PDF时，证书只有序列号/颁发者就被标成“CA校验通过” | 保留数字签名完整性结果，CA区域明确warning“信任链未验证”，返回`trustVerified=false`；提示未验证证书信任链和撤销状态。不把自签证书当外部受信任证书 |
| 未签名PDF被提示为“文件被修改” | 读取实际签名数量，返回`signaturePresent=false`、warning“未检测到数字签名”；不将缺少签名推断为篡改。损坏PDF、空文件、错误扩展名分别失败，正常大写.PDF仍可验证 |
| 签名人取自可编辑的PDF文档作者；解析异常直接回显底层信息 | 签名人改取数字签名元数据，不用文档作者冒充；异常响应改为固定解析/验签提示，当前服务日志只写异常类型。没有声称整个PDF解析库的全部日志均已脱敏 |
| 回单把证书指纹标成文件指纹，并声称自动验签/已完成核验 | 改为签署记录与证书快照，标明证书指纹及SHA-256算法；说明证书指纹不是文件内容哈希、上传已签PDF另行校验、信任链/撤销状态未验证。保留兼容字段`verifyStatus=1`作为既有生成快照状态，不修改历史数据库记录，也不将此编码当独立密码验证或外部信任证明 |
| 较长回单标签换行后与值的提取顺序交错 | 缩短为“证书指纹”，算法放到节标题；实际生成PDF后提取文本核验标签、指纹和说明 |

`SignUploadVerificationTest`新增8项，旧代码6项失败→修复后8项通过，含真实未签名PDF；`SignReceiptMeaningTest`新增3项，旧代码3项失败→最终3项通过，直接生成并解析PDF文本。最终构建包含上述11项。v18初次完整构建曾有1项标签文本失败，日志`r41-full-build-v18-failed.log`保留，修正后完整重跑通过，未把失败历史覆盖为PASS。验真页面已有的5项迟到请求/重复提交函数回归另重跑5/5，日志`r41-pc-verify-state.log`，不计为本轮新增后端测试。

### 34.3 最终同版本真实接口闭环

R42在实际运行v18上全新创建合成资料，执行平台文件上传→测试印章/托管测试证书→项目创建与审核→项目/印章授权→prepare→普通run及合并unified→签后下载→回单查询/下载→multipart上传验真→授权和项目清理。版本在写操作前独立查询实际JAR SHA256，未复用已关闭台账重放业务。

| 场景 | 实际断言 | 结果 |
|---|---|---|
| 普通及合并签署 | 同一合成请求各两路有界并发，各仅1成功；签署记录、文件、回单关联一致，回单状态1；下载实际PDF | PASS |
| 独立完整性检查 | 普通签署PDF提取CMS，由OpenSSL验证内容完整性；修改测试内容后独立验证失败。使用`-noverify`仅校验CMS完整性，不验证外部信任链 | PASS（完整性；外部信任未验证） |
| 三页普通章+骑缝章组合 | 三页合成源PDF，普通章3处、骑缝范围1–3；实际签后PDF24466字节，独立PDF解析确认3页及每页原文本，每页2个印章图像绘制操作；上传验真valid=true、trustVerified=false，回单PDF3794字节 | PASS（图像操作/内容检查，非浏览器视觉验收） |
| 普通已签PDF上传 | 实际multipart、valid=true、signaturePresent=true，当前SHA256等于独立本地摘要，CA为warning | PASS |
| 合并已签PDF大写.PDF上传 | 与普通已签文件同等断言，能关联对应签署记录 | PASS |
| 未签名PDF上传 | valid=false、signaturePresent=false，warning且没有篡改断言 | PASS |
| 空文件/错误扩展名/损坏PDF | 三项分别返回明确失败，未泄露底层异常详情 | PASS |
| 缺少file字段 | 统一响应拒绝，未创建文件记录 | PASS |
| 两份实际回单内容 | 普通/合并JSON关联与签后文件ID一致；下载实际PDF并提取记录ID、证书指纹、正确标签和未验证信任链说明 | PASS |

最终上传用例共7项，不将其中两个成功PDF与签署场景重复计为新签署次数。验真前后`sys_file/sign_file/sign_record`三个表数量均一致，上传验真持久化对象0。R42台账2个流程用例均PASS，分别为普通/合并并发完整性和三页组合链；详细上传7项及回单2份证据在独立JSON。证据`r42-ledger.json`、`r42-sign-final.log`、`r42-multipage-final.log`、`r42-upload-a5372886a52d.json`、`r42-upload-final-retry.log`及实际下载PDF。

另以v16签出的R41普通/合并PDF在最终v18重测，同样7项上传验真及2份回单解析通过，证明本次改动兼容这两份旧签署文件，不扩展为所有历史PDF兼容保证。中间v17上传脚本曾复制台账旧版本标签，原件保留并在`r41-upload-v17-provenance.json`另记版本更正；最终脚本已改为独立核对运行JAR与部署清单，不再依赖旧台账标签。最终兼容证据为`r41-upload-a5372886a52d.json`。

回单独立解析脚本首次缺少中文CMap资源，提取断言失败；补齐项目依赖中的本地CMap后同一批只读检查通过，原失败证据保留。该失败属于测试解析器配置，不误报为产品签署失败，也没有为重测重复签署。这一轮没有进行浏览器中文预览或真实页面点击验收。

### 34.4 清理与交付边界

两轮通过正常项目更新API转为完成状态3并撤销项目用户授权；测试印章已禁用、印章用户授权均0。每轮7项独立只读源表检查均0，共14项：未登记签署记录、启用印章、印章授权、活动项目、项目用户授权、缺失回单、缺失签后文件。两轮台账closed，后续拒绝重放。R41两份、R42三份已签记录及对应源文件、回单、加密托管测试证书按审计引用保留；不强删有效签署链，不将已引用文件当垃圾移除。没有SQL写业务、真实财务变更、整库恢复或外部正式签署。清理证据`r41-r42-cleanup.json`。

两轮共享的是本轮自己取得的2个独立会话，均正常logout并回查失效；6份复制的token/密码/验证码文件精确删除，原R32共享账号资料不动。容器内6个本轮临时签名验证/候选包文件删除并回查不存在；候选构建、原包备份和脱敏证据保留。本轮没有向Downloads新建副本。证据`r41-r42-session-cleanup.json`。最终三端本机只读L0冒烟9/9、`git diff --check`通过。

本轮已补齐上述PDF上传验真、普通/合并及三页骑缝组合的接口与代码回归。浏览器上传/放置/点击提交仍按用户要求跳过，不能将API通过回填为页面通过；真实摄像头/GPS/地图AK继续跳过。外部受信任CA链、撤销服务、可信时间戳、真实合同法律效力与容量SLA未验收，不据此声称全部页面、全部外部服务或国家标准认证已经通过。


## 35. R43 功能缺口补测与业务清理闭环

### 35.1 范围、版本与验收口径

2026-11-07继续检查前轮未覆盖功能，执行正常HTTP业务接口、受控服务测试、代码检查和完整构建。目标为本机在用8000/dev环境及5173/5174/5175，非公网生产。按用户要求跳过浏览器、摄像头、GPS、地图AK；没有将API成功记作页面或硬件通过。沿用项目维护、业务测试、代码检查、分阶段测试Agent/Skill，由同一Agent执行。静态盘点`r43-endpoint-inventory.json`登记427条显式映射，未包含全部继承CRUD，也不代表427条已逐个通过。

最终本机backend为v22，SHA256 `fecf2f4b911c7ae043e03a289184263a926d24a7e3e0a35dd843eca29189e033`。JDK21完整构建716项，失败/错误/跳过0，较前轮674项新增42项；1430份后端源清单在收尾再次逐份哈希比对一致。替换前实际v18已备份。v20/v21是中间构建，v20仅用于固定范围迁移CLI，未作为应用发布。包内R29/R31/R32/R43只读validate/status为52/1/1/1项、pending0，本次JAR部署DDL为0；此前另行执行的一列迁移见35.3。证据`.local/r43-build-v22-evidence.json`、`r43-source-manifest-v22.json`、`r43-full-build-v22.log`、`r43-deployment-v22.json`。

台账保留51条历次结果，按用例ID取最新为32项：31 PASS、1 BLOCKED，含夹具建立与两项清理审计，不当作32个业务模块。初批正常业务发生在v18，金额、分片、文件改名和账号清理等受本轮修复影响的用例在v22重测；每次均独立检查实际运行JAR哈希，台账保留每项真实版本及失败历史，未把所有初批结果重标为v22。证据`r43-ledger.json`。

### 35.2 实际发现与修复

| 问题 | 修复与验证 |
|---|---|
| 销售、采购、维修合同可保存负金额或超过两位小数 | 请求模型增加非负、金额精度与数据库位数上限校验。三域共6项真实接口负例在v22拒绝；曾接受的本轮错误草稿已立即正常删除。`ContractAmountValidationTest`和`RepairContractAmountValidationTest`各8项覆盖负数、过精度、溢出及合法边界 |
| 维修合同模型读写sign_date，数据库缺列导致查询/创建失败 | 增加单独R43 Liquibase补丁、新安装SQL列及固定范围CLI；不改已执行changeset正文，迁移验证见35.3 |
| 本地分片使用相对Servlet目标路径；底层上传失败仍写成功缓存；合并可能提前覆盖目标 | 使用绝对临时路径和先组装后发布；成功才写缓存，核对片数及各片大小，完成时清除全部分片缓存，取消先清理物理分片再清缓存。实际9,660,000字节合成TXT两片逆序上传、断点续传、合并、下载逐字节相同、另建会话取消均通过。`LocalMultipartHandlerTest`4项真实临时磁盘测试、`MultipartCompletionTest`5项服务测试通过 |
| 文件改名接口允许改parentPath，仅改数据库却未移动物理文件，随后下载失败 | 此入口明确只做展示名改名；拒绝改变存储目录并清除冗余同目录输入。3项`FileRenameStorageTest`通过，v22真实目录统计、拒绝移动、正常改名、37字节下载一致通过 |
| 删除用户遗漏UI偏好记录 | 在原用户删除事务中按确切用户ID清理偏好。`UserPreferenceDeletionTest`2项用实际UserService、H2/MyBatis映射与事务复现并修复；受保护用户拒删时偏好保留，其他用户不受影响。v22正常API删除本轮账号后独立查询偏好0 |
| 代码生成菜单存在，当前部署没有对应后端API | `/code/generator/config/psi_goods`返回404“请求URL不存在”，并非仅缺少表配置。PC增加明确不可用提示及失败时禁用批量操作，类型检查/完整构建通过；未恢复缺失后端，因此预览/下载仍BLOCKED |

新增测试共42项：两类金额16、分片9、改名3、用户偏好2、期初确认4、R43迁移CLI8。部分服务测试使用协作者替身，不能替代完整SQL业务链；具体边界见下表。初次维修金额负例曾因缺列异常而看似拒绝，后续审查纠正了证据标准：运行/SQL异常不算业务校验通过，补列后重新复现真实缺陷并完成修复。原失败及初始结果均保留。

### 35.3 维修合同数据库变更验证

新增`patch_eoms_contract_sign_date.sql`、`releases/r43-mysql.yaml`、master include和`R43MigrationCli`/`scripts/migrate-r43.sh`，只允许明确目标的一列可空DATETIME变更；历史未记录签订日期保持NULL，不虚构日期。CLI拒绝既有冲突类型，不运行完整master、种子或安装脚本。已有执行记录的旧插件SQL未改写，新安装`sql/init.sql`同步最终结构。

独立MySQL8.0.42探针无主机映射端口：实际CLI七项通过，包含只读命令无副作用、缺列新增、非空历史行原字段不变且日期NULL、重复执行仅一条历史、目标不符拒绝、冲突DATE类型拒绝、已有精确列MARK_RAN。另有8项CLI受控回归。固定范围应用到本机在用库时原维修合同表有0行，因此不能将现场空表校验冒充真实历史数据迁移证明；非空历史行证据来自隔离探针。应用后维修合同创建、读改、确认和项目绑定通过。独立全新安装检查通过，未向在用库运行全量SQL。证据`r43-migration-probe/evidence.json`、`r43-live-migration.json`、`r43-live-cli-*.log`、`r43-fresh-install.log`。PostgreSQL迁移未执行。

### 35.4 逐功能与业务联动补测

| 功能/流程 | 实测内容与结果 | 证据（均在.local） |
|---|---|---|
| 报价终态 | 独立合成报价DRAFT→SUBMIT→CONFIRM及CANCEL，终态读/渲染金额100不变，编辑和删除被拒；保留合法终态审计 | r43-quote-terminals.log |
| 期初四类 | 库存/应收/应付/预付款草稿创建、列表、读取、编辑、删除通过。在用环境未做无冲销入口的期初确认；另4项受控服务确认验证重复调用不重复创建、库存委托FIFO、应收应付及预付金额，不等同真实库确认 | r43-init-drafts.log、InitConfirmationFlowTest |
| 合同—采购—销售—退货 | 确认采购合同200、销售合同100；采购2×50=100和销售1×80=80关联进度0.5/0.8；再按源明细做完整销退与采退，库存成本及应收应付补偿为0。现有合同进度按已确认原单毛额统计，未扣退货，不把它描述为净退货进度 | r43-contract-chain.log |
| 三域合同—签章项目 | 销售、采购、维修分别建立独立项目，验证合同反向projectId；更新清空对应字段及旧兼容字段后反向解绑，再删除项目核对引用为空。系统约束为一个项目绑定一个合同，不是同时绑定三种合同；本轮不新增外部正式签署 | r43-project-binding-final.log |
| 打印模板 | 内置HTML与DOCX下载有实际单号；自建非默认HTML模板创建、编辑、渲染单号/标记/明细、删除通过；DOCX按ZIP/XML提取核验。未测浏览器打印版式 | r43-print.log、r43-print.docx |
| 文件 | 真实multipart分片恢复/合并/取消、目录大小、改名下载、回收与精确清理通过，未调用浏览器文件选择或摄像头 | r43-multipart-final.log、r43-folders-final-retry.log |
| 商品XLSX导入 | 实际下载模板并生成一行合成商品，INSERT→重解析SKIP→UPDATE售价99→EXIT拒绝重复，剩余会话用正常SKIP消费；4个解析会话全部消费，导入商品正常删除 | r43-goods-import-retry.log |
| 平台维护 | 自建字典/字典项A改B且公开选项缓存刷新；自建部门、无业务权限角色CRUD；自建PC/ACCOUNT客户端读取、时长修改、停用和删除，未改默认客户端或为账号加业务权限 | r43-platform-final.log、r43-client.log |
| 个人资料 | 新建本轮专用账号，普通公开验证码/SM2登录、仅该新账号首次密码初始化、再次正常登录；昵称和UI偏好读改回读、合成PNG头像上传与管理员读取一致、读取空社交绑定列表、退出、删除账号/角色/头像与偏好通过。未发送外部邮件或执行OAuth绑定 | r43-profile-cleanup.log、r43-ledger.json |
| 导出 | 销售合同、采购合同、报价、EOMS仪表盘、统计五份真实XLSX；ZIP/工作簿可解析，统计含5页。前三项按本轮标记筛选；未独立对账全部全局统计金额，也不作为页面验收 | r43-reports.log、r43-export-*.xlsx |
| 代码生成 | 实际404，元数据前后数量0/0，未保存配置、写源码或调用覆盖生成。仅提示缺失服务，预览/下载BLOCKED | r43-ledger.json对应CODEGEN用例 |

测试驱动已保存为`scripts/quality/live_gap_regression.mjs`、`live_profile_flow.mjs`、`make_goods_import.py`及`verify_r43_migration.py`。早期字典URL、部门parentId=0、自制XML命名空间、在一个项目绑定三种合同、目录尾斜杠断言及清理审计未区分逻辑删除等问题属于测试夹具/断言问题，修正后重测，未列为产品缺陷；原失败日志保留。台账已closed，不能在同一运行号重放产生更多不可删除终态记录。

### 35.5 数据、凭据与临时资源清理

业务补偿后21项独立源表零值检查通过，包含库存数量/成本、应收应付、未分配资金、待办、活动工单/结算及未登记进销存财务草稿等。另25项补充查询均0：四类期初活动主/明细、平台维护对象、账号角色关联/偏好、活动或回收中文件、三个活动测试项目及三域合同projectId残留。文件与项目按平台逻辑删除规则核对，未把保留的逻辑删除审计行误判为活动数据。导入4个会话已消费。活动自有对象0，7项有审计引用的主资料正常禁用，未引用另一单位正常删除。

两条终态报价、3份已确认合同、已补偿采购/销售原单及退货单按业务审计规则保留，不声称数据库中全部测试行被物理删除。没有用SQL删业务、强删有效流水、整库恢复或清空公共回收站。改名旧缺陷遗留的一份37字节文件，经确认无活动/回收引用、精确自建目录、大小和已知内容SHA256匹配后删除，再删空目录；没有递归删除或扩展到其他文件。

本轮管理员会话正常logout，专用资料账号会话已失效，再查询身份均拒绝；7份复制的token/密码/验证码文件精确删除，原共享账号资料保留。R43隔离迁移容器及自有匿名卷移除、3份探针私有配置移除，backend内2份本轮候选JAR精确删除，实际运行包/备份/脱敏证据及共用构建缓存保留。证据`r43-ledger.json.finalAudit`、`r43-supplemental-cleanup.json`、`r43-filesystem-cleanup.json`、`r43-session-cleanup.json`、`r43-migration-probe/cleanup.json`。

### 35.6 最终检查与仍未验收项

PC执行`pnpm build`，类型检查及Vite完整构建通过（3分44秒），保留既有sm-crypto浏览器crypto外置和大chunk警告，未通过抬高阈值隐藏警告。此次页面不可用提示仅有代码/构建证据，无浏览器点击证据；两端H5未改动，未重复声称H5本轮重新构建。清理后本机三端L0只读冒烟9/9通过，后端1430文件哈希再次匹配；最终`git diff --check`结果记录在`r43-diff-check.log`。证据`r43-pc-build.log`、`r43-final-service-smoke.log`。

本轮所列API业务联动和修复回归已闭合，但代码生成后端缺失仍BLOCKED。浏览器页面/真实打印、摄像头、GPS、地图AK继续按用户要求跳过；期初在用库真实确认、外部邮件/OAuth、未配置云存储、外部CA/TSA、正式环境容量SLA等没有在本轮验收。未声称所有427映射逐条覆盖、所有模块完全可用或达到国家标准认证/完整生产交付标准。此前签署链和PDF接口验证详见34节，本轮未以无关新数据重复正式签署。


## 36. 移除代码生成与应用管理

### 36.1 最新产品范围与实际清理

2026-11-07用户明确取消代码生成，要求连同应用管理全部清理。本轮R44据此停止此前生成器补齐工作：该临时实现尚未部署，连同独立试验代码一并删除。35节的代码生成BLOCKED属于历史测试记录，本轮处置为“按产品需求移除”，不改记为预览/下载测试通过，也不再列为待补齐功能。目标仍为本机在用5173/8000及两端H5；非公网生产。浏览器、摄像头、GPS、地图AK继续跳过。

已移除代码生成PC页面、配置抽屉、预览弹窗、API封装和类型导出、临时预览工具、新增未发布的生成器后端模块及测试、generator配置YAML及加载项。应用管理对应原`/open/app`开放应用功能，已移除PC列表/编辑/详情及API/类型、`ls-aegis-biz-open`全部实现、父POM与boot依赖、应用管理模块开关及类型、演示环境重置任务中的应用引用。后端子模块从17减为16，父工程加子模块的完整构建反应器为17项。

应用专用API参数签名认证分支、CRUD权限分支和`sa-token-sign`直接依赖同时移除，受保护接口沿用统一登录与权限检查。电子签章业务模块`ls-aegis-biz-sign`继续保留，本轮全量回归包含其现有测试，并真实查询签章可用项目。系统登录客户端配置`sys_client`为PC/H5统一认证所需，保留并前后核对内容一致。根AGENTS已更新产品范围，避免后续将已删除功能重新作为待实现/待测项。

### 36.2 新安装与旧库升级

全新安装`ls-aegis-server/sql/init.sql`已移除应用/生成器专用表、菜单及应用管理开关，同时清除原租户安装段对`sys_app`的两处ALTER，避免缺表导致安装中断。独立无网络、无主机端口MySQL全新安装验证8项通过：完成标志1、非标准排序表0、管理员1、关键菜单1、分类编码列1、下线表0、下线菜单0、下线开关0。脚本`verify_mysql_init.py`加入相应断言，证据`.local/r44-fresh-install.log`。

旧库新增固定范围`releases/r44-mysql.yaml`、`patch_retire_generator_open.sql`、`R44MigrationCli`及`scripts/migrate-r44.sh`；master只追加该release，已执行旧changeset正文未改。旧插件SQL保留用于历史迁移兼容与校验，当前运行包不再含功能实现；历史bootstrap路径最终由R44清理收敛，未直接改写既有历史记录。

迁移只处理3张专用空表`sys_app/gen_config/gen_field_config`、16个已核对身份的固定菜单、其角色/租户套餐授权及`MODULE_OPEN_ENABLED`参数。CLI及release检查表非空、自定义菜单后代和菜单ID被改作他用的情况，遇到冲突停止而不强删；validate/status不初始化迁移历史。只接受明确目标与apply写入确认参数，不运行完整master、全量安装SQL或history同步。

独立MySQL8.0.42探针9项实际CLI验证通过：只读无副作用、非空配置拒删且保留、定制子菜单拒删、菜单身份冲突拒删、3张空表删除、授权移除且登录客户端未变、重复应用仅一条历史、错误目标拒绝、新安装已无对象时仍可完成release。另6项受控CLI测试已纳入完整构建。证据`r44-migration-probe/evidence.json`、`r44-probe-verify.log`及`R44MigrationCliTest`。

在用库执行前确认应用写入口已禁用且3表均0行，随后只执行上述独立release。执行后专用表、菜单、角色授权、套餐授权及应用管理开关5项剩余检查均0。11张共享/业务表记录数保持不变，登录客户端内容及其他角色/套餐授权内容也相同，合计14项前后对比；这不是全业务字段的逐行对账。未创建新业务对象，也未删除财务、库存、签署审计或恢复整库。证据`r44-live-migration.json`、`r44-live-cli-*.log`。

### 36.3 构建、部署与真实接口回归

最终v25 SHA256为`372589145f1842da45df7378190feecf6d25ef0c7b4254f86efeb3b0706f2230`。JDK21运行、Java17编译目标下，完整干净构建**722项测试通过**，失败/错误/跳过均0；1417份后端源清单收尾再次逐份哈希匹配。首次清理后曾暴露残留签名provider成员及演示任务AppMapper引用的编译错误，已修正并完整重跑；旧失败日志保留为`r44-full-build-v23.log`与`r44-full-build-v24.log`，不计为最终通过。最终证据`r44-build-v25-evidence.json`、`r44-full-build-v25.log`、`r44-source-manifest-v25.json`。

PC类型检查及Vite完整构建通过，Vite耗时6分33秒；保留既有sm-crypto浏览器crypto外置和大chunk警告，未调整警告阈值。两端H5源码本轮未修改，未声明重新构建或页面操作通过。证据`r44-pc-build-final.log`。

仅更新本机backend，替换前v22包已备份。候选包内确认不存在biz-open/biz-code及sa-token-sign库；R29/R31/R32/R43旧CLI字节与此前运行包一致。包内五个固定范围只读validate/status均pending0，数量52/1/1/1/1；本次JAR部署本身DDL为0，前述3表下线由另行记录的限定迁移执行。保留dev配置、上海JVM时区及Liquibase自动执行关闭设置。证据`r44-deployment-v25.json`、`r44-deploy-v25.log`、`r44-packaged-*.log`。

本轮通过公开验证码及SM2正常登录取得管理员会话，没有改原密码或停用验证码；该会话在重启后继续执行新运行包接口。最终11项只读核验全部通过：

| 核验 | 实际结果 |
|---|---|
| 登录身份与客户端 | 管理员身份有效、密码未过期；客户端列表规范化内容SHA256与变更前一致 |
| 动态路由与菜单树 | 均不含下线菜单ID、路由或权限字符串 |
| 功能模块配置 | 应用管理开关消失，租户与调度配置保留 |
| 删除接口 | `/code/generator/config`、`/code/generator/preview/psi_goods`、`/open/app`均明确返回404 |
| 核心业务读取 | PSI商品列表、报价列表、维修合同列表、签章可用项目均正常返回 |

证据`r44-baseline.json`、`r44-api-final.json`。上述是已部署版本的只读影响回归，不扩展为本轮重新执行全部写业务联合链；代码生成预览/下载随需求取消而不再验收。

### 36.4 清理与最终结论

R44未创建在用业务测试资料。独立迁移探针的3个合成库随自有容器和匿名卷删除，3份探针私有配置移除；没有移除共用构建容器、中间件、网络或其他轮次账号。管理员正常logout后回查身份失效，3份本轮复制会话/验证码文件精确删除；已放弃的生成器试验目录按3个确切路径清理，容器内候选临时JAR移除或确认不存在，运行包、回退备份和脱敏证据保留。证据`r44-migration-probe/cleanup.json`、`r44-cleanup.json`。

清理后本机三端L0只读冒烟9/9通过，最终`git diff --check`通过；证据`r44-final-service-smoke.log`、`r44-diff-check.log`。代码生成与应用管理现已从当前产品和运行环境移除，保留的统一登录、核心业务及数据库安装路径完成以上验证。历史报告保留原时间点事实；浏览器及硬件按用户要求跳过，未新增全页面、容量SLA或国家标准认证结论。

后续菜单显示复核：用户反馈仍见“开发工具”后，再次只读查询在用库全部开发相关标题、`/code`路径、code组件及9000菜单，无残留记录；3张专用表仍不存在，实际运行JAR仍为上述v25。通过公开公钥响应一致性确认本机5173代理对应8000。前端route store仅内存保存且路由守卫仅首次初始化加载菜单，已打开页面不会因后台菜单变更自动重新取数，因此旧页面内存为当前推断的来源，未进行浏览器观察，不能据此断言用户所见页面已消失。刷新页面或重新登录会重新获取当前菜单。证据`.local/r44-menu-followup.json`。


## 37. 菜单缓存残留与实际运行环境复核

### 37.1 核验范围与发现

2026-11-07用户反馈仍见“开发工具”，要求继续排查实际菜单、缓存与环境。R45继续以本机PC5173/API8000为目标，浏览器、摄像头、GPS和地图AK仍按要求跳过。没有读取或操作用户浏览器缓存文件，也没有推断未提供的远程站点已经更新。

本机实际监听复核为5173 PC、5174客户H5、5175工程师H5及8000后端；所核对80/443/18000无监听。Vite进程为上述三个开发服务，另一个Vite进程是本轮build，不是另一套PC监听服务。`docker/nginx/web`仅留部署说明占位，未发现本机运行的旧Nginx容器。实际GET 5173返回的新main、route store、tabs store均包含本轮修复代码、Cache-Control为no-cache；已删除的代码生成/应用管理Vue及API模块4个地址均404。因此已核实当前5173服务使用本仓库的新代码，不能仅凭文件删除就当作运行环境已更新。证据`.local/r45-runtime-ports.json`、`r45-served-modules.json`、`r45-deleted-module-http.json`。

发现的前端缺口为：动态菜单仅首次初始化拉取，后续导航/恢复焦点不重新取数；sessionStorage中的tabs及keep-alive名称、localStorage中的菜单搜索历史可能恢复旧入口；原退出流程移除已注册路由后未清空菜单store。36节后续所述“旧内存可能保留”现已由以上源码路径进一步定位，但未冒充用户浏览器画面的直接观察。

### 37.2 修复与受控回归

新增统一退役导航策略`src/utils/retired-navigation.ts`，按明确的/code、/open路径边界、旧路由名和固定菜单ID清除下线项，递归处理旧路由树；保留登录客户端配置、业务页签以及/code-review、/open-orders等不同路径。后端接口删除与鉴权仍为真正的功能边界，前端策略用于旧快照兼容，不以隐藏菜单替代后端清理。

启动时定向迁移sessionStorage的tabs和localStorage的layout-search-history两项，保留登录token、账号偏好和业务草稿；页签恢复后再次清除退役项，新增页签/keep-alive拒绝恢复退役路由；搜索历史响应式清除旧入口。旧地址导航回工作总览。退出时清空动态菜单store并使迟到请求失效，防止已退出身份的菜单被旧响应重新装入。

新增`router/menu-sync.ts`：窗口恢复焦点、页面重新可见和导航完成时同步菜单；最多每分钟一次，合并同时触发的请求，不做后台轮询。成功后替换动态路由注册；普通网络失败保留正常菜单并限频重试。事件处理在HMR销毁时解除，避免重复监听。页签store改用已创建的router实例，使在组件外执行缓存清理时不依赖useRouter注入上下文。

| 受控用例 | 验证结果 |
|---|---|
| 递归菜单过滤与相似业务路径 | 只移除退役树，不修改原输入或误删正常路径 |
| 历史tab/cache/search迁移 | 清理精确，token/草稿保留；重复执行不重复写入 |
| 损坏JSON/存储异常 | 不清空其他数据，不阻止启动 |
| 已打开客户端同步 | 移除旧路由注册，正常新菜单可注册 |
| 焦点/导航同时触发及网络失败 | 共享一次请求；失败保留正常菜单并限制重试 |
| 退出后的迟到响应 | 不恢复旧身份菜单或注册路由 |
| 事件生命周期 | 隐藏页面不发请求，销毁时解除所有监听 |
| 持久化恢复与再次添加 | 恢复后旧页签/缓存消失，后续添加不能复活退役项 |

新增8项全部通过，并包含在本轮PC相关回归总计56/56中，不将两组相加成64项。使用实际函数和store逻辑、模拟存储/时钟/路由/事件，属于受控代码回归，不是浏览器端到端测试。初次类型检查误用本地插件不支持的afterHydrate，已按实际依赖改为afterRestore；新恢复夹具最初缺少sessionStorage模拟，修正后重跑8项通过，初始失败日志保留。证据`r45-navigation-tests.log`、`r45-navigation-tests-hydration-fixture-failed.log`、`r45-pc-regression.log`、`r45-typecheck.log`；测试入口`scripts/quality/pc_retired_navigation.test.cjs`。

### 37.3 实际菜单与后台缓存复核

通过普通公开验证码和SM2正常管理员登录，经实际5173/api而非仅直连后端核验。使用平台提供的`DELETE /system/menu/cache`清理角色菜单元数据缓存，未清空Redis、其他用户会话或业务缓存。

清理前路由已无退役项；清理后连续读取两次菜单以覆盖热缓存，两次响应完全相同，并与8000直连路由一致，当前PC路由根节点10个。路由与完整菜单树均不含退役ID、路径、权限或“开发工具/应用管理/代码生成”标题；MODULE选项无应用管理开关。不能因清理前已为0就编造发现了后台残留菜单行；实际修复重点为前端旧导航恢复和更新机制。证据`r45-live-menu-check.json`。

按UTF-8 SQL文件方式只读复查数据库，菜单、3张专用表、应用开关、角色授权、租户套餐授权五项计数均0，临时查询文件随后精确删除。此次没有再次执行功能下线DDL或重放R44迁移。证据`r45-database-retirement-counts.log`。

### 37.4 构建、清理与结论边界

最终PC类型检查及完整Vite构建通过，Vite耗时4分37秒；保留既有sm-crypto和大chunk警告，未提高阈值。生成manifest不含退役页面/API模块入口。当前本机5173继续由Vite提供上述新源模块，dist为构建核验产物，未声称发布到另一个公网Nginx。证据`r45-pc-build.log`、`r45-artifact-evidence.json`。

后端保持36节v25 SHA256 `372589145f1842da45df7378190feecf6d25ef0c7b4254f86efeb3b0706f2230`，1417份后端源清单再次全部匹配。本轮未修改/重新部署后端，也未将前轮722项测试虚记为本轮重跑；两端H5未改动。清理后本机三端L0冒烟9/9及最终git diff --check通过。

R45没有创建在用业务对象；导航存储夹具只在内存测试。本轮管理员正常退出并回查身份失效，3份复制的会话/验证码文件精确删除，正常菜单缓存可由接口重新生成。证据`r45-cleanup.json`、`r45-final-service-smoke.log`、`r45-diff-check.log`。

已完成实际本机服务、菜单响应、后台缓存及前端导航恢复机制的上述检查和修复。新代码启动时自动迁移旧导航快照，之后按事件更新菜单；此前已经执行的旧JavaScript必须取得此次更新才能运行新逻辑。按用户要求未使用浏览器，不能宣称已亲眼确认用户当前标签页的画面或已直接删除其浏览器存储；实际页面验证继续明确为未执行。


## 38. 业务数据全量清理范围盘点

2026-11-07用户要求清除全部业务数据。R46首先只读盘点当前已确认的本机在用`ls-aegis-mysql/ls_aegis`，未转向公网生产、其他数据库或旧轮次隔离容器。按实际表名、全新安装SQL及COUNT(*)核对，未读取业务正文、密码、证书私钥或连接凭据。当前87张业务相关候选表共1950条物理记录，包含历史及逻辑删除行：EOMS17表397条、PSI52表1394条、签章18表159条；其中PSI基础设施锁锚点单独标注，不能将其当普通测试垃圾无协调删除。没有发现源安装SQL未登记而非空的业务候选表。证据`.local/r46-data-reset-inventory.json`。

共享平台表独立列出：用户23、角色13、部门12、菜单355、参数69、登录客户端2、存储配置2、文件108、操作日志5721、公告53、消息25条。以上计数仅为范围盘点，不表示全部归入可删除集合；附件表和物理存储包含跨域共享资料，不能整表或整目录清空。签章候选集合包含印章、证书、项目、签署记录及其审计链，须区分“只重置单据流水”与“连基础资料、印章证书一起重置”。

拟定默认保护范围为系统账号/角色/部门/权限、系统配置/字典/登录客户端/存储设置、数据库结构与Liquibase历史、系统操作安全审计；业务附件须核对引用后单独列出。执行前须明确全量清理是否包括基础主资料、已记账库存/财务流水、已签署记录以及印章证书，之后才建立完整备份并验证恢复，再按最终范围清理与验收。此阶段状态为范围待确认，尚未创建备份、未运行删除/截断/安装SQL、未删除物理文件，实际删除业务记录0。不能据本节声称数据已清空。


### 38.1 用户确认与最终清空结果

用户随后明确确认：不需要备份，直接清空全部业务，包括单据流水、客户商品等基础资料、签署记录、印章证书和关联附件，保留系统账号、权限、配置、字典。本次授权属于在用环境全量业务重置，覆盖此前仅允许按运行号补偿清理的范围；未按旧测试清理规则仅禁用或保留历史业务。**未创建数据库、附件或证书备份**，也未用安装SQL或整库恢复代替清理。

按最终范围清空：PSI除打印模板配置之外的业务/主资料/财务库存及序列、EOMS全部业务与映射、签章印章证书/托管密钥/项目/文件关联/签署/验真回单/业务审计。库存串行锚点只在后端写入停止、整域数据重置的本次维护中一并归零，未作为普通测试清理随意删除。打印模板3条及签章备份调度配置0条按“保留配置”要求保持不变；账户、角色权限、部门、字典参数、登录客户端、存储配置、租户配置、数据库结构、Liquibase历史和系统操作审计保留。

补充盘点后，平台公告、业务通知及阅读记录也归入本次全业务范围，避免保留旧业务消息；系统操作安全日志sys_log仍保留。此项补充清理前后再次对最终保留数据做摘要核对。最终结果如下：

| 清理项 | 实际结果 |
|---|---|
| 首批业务表 | 85表全部0：EOMS删除397条、PSI删除1391条、签章删除159条，共1947条 |
| 公告/通知与阅读记录 | 4表全部0：公告53、通知25、通知读取31、公告读取0，共109条 |
| 最终业务范围 | **89表全部0，累计删除2056条业务记录**，包含历史及逻辑删除行 |
| 附件元数据 | 删除105条业务/非保留上传记录；保留3条系统用户/配置关联元数据 |
| 实际业务文件 | 精确删除77个文件，共6313733字节，主文件/缩略图/回收区及存储中非保留业务上传均核对；不删除整个共享存储根目录或系统加密配置目录 |
| Redis业务状态 | 删除323键：psi:301、eoms:19、sign:3，quote:/multipart:0；再次扫描上述五个前缀均0，包含客户H5业务访问状态及业务编号/导入状态，不使用FLUSHDB |
| 数据保护 | 首批29项保留表/文件记录摘要一致；补充清公告通知时对最终25项保留范围再次摘要比对一致 |
| 备份/表结构 | 未备份；用事务DELETE，不DROP/TRUNCATE、不关闭外键、不重置系统账号密码；表结构和迁移历史保留 |

文件先按sys_file、用户头像、配置图片、打印模板和跨域引用核对，保护系统引用后生成确切路径清单。实际物理存储位于本仓库`docker/data/ls-aegis-server/data/file`；每个待删文件均验证绝对路径位于此根下、非目录/非重解析点、大小与清单一致，再逐个删除，无递归根目录删除。另检查清公告通知后未新增需要删除的共享附件。未读取/输出证书私钥或文件正文，临时附件路径清单在执行完成后删除。证据`.local/r46-database-reset.json`、`r46-notification-cleanup.json`、`r46-physical-cleanup.json`、`r46-redis-cleanup.json`。

后端先停写、执行事务及保留数据摘要核对，再清文件/缓存并恢复；补充消息清理同样在停写窗口进行。重启后独立查询89表仍全0，未被启动逻辑重新填充，证据`r46-final-counts.log`。原管理员使用公开验证码/SM2和原凭据正常登录，经实际5173/api验收22项全部通过：20类业务及消息列表为空、管理员/菜单正常、PSI工作台12项数值指标全0。另核验未读消息total为字符串“0”、抽查已删除的业务附件下载被拒绝。初次未读夹具按JSON数字取值，因平台Long转字符串导致断言失败，按实际契约修正后通过，未误报为残留消息或产品缺陷。证据`r46-api-verification.json`、`r46-additional-checks.json`。

系统用户23、角色13、部门12、菜单355、字典36、字典项136、参数69、登录客户端2、存储配置2等保留；这类数量包含既有逻辑删除行。停写阶段保留摘要全部一致，服务恢复后的正常登录/读取可以追加新的系统审计，不把新增操作日志误当清理失败。源码未修改，后端保持v25，1417份源清单逐份哈希一致；无需因数据维护重跑构建，也未声称本轮重新执行722项后端测试。

本轮自身会话正常退出并回查失效，8份复制的会话/验证码、私有路径清单和临时删除SQL精确移除，容器内临时SQL亦删除。维护脚本运行状态关闭，拒绝重放到后续新增业务；没有为验收新建业务资料。保留统计、不可逆摘要和执行证据，不构造可恢复的业务数据副本。没有操作其他轮次的独立数据库、用户下载目录、历史构建/测试证据副本或既有备份。清理目标是已确认的当前在用数据库及其业务附件存储。

最终三端只读L0冒烟9/9、git diff --check通过，后台服务已恢复。浏览器、摄像头、GPS和地图AK仍按用户要求跳过；接口/数据库核验不替代浏览器画面观察。本节38.1为最新已执行结论，前面的“待确认/未删除”仅保留当时盘点阶段事实。证据`r46-reset-completion.json`、`r46-final-summary.json`、`r46-final-service-smoke.log`、`r46-diff-check.log`。


## 39. V2.0 代码发布与版本统一

2026-11-07用户明确要求将当前整体改动推送Git，命名V2.0。按项目语义版本规范，发布名V2.0，工程版本2.0.0，分支main、注释标签v2.0.0。范围包含平台整体优化、类型与请求状态修复、库存财务/维修/签署一致性、文件上传下载、代码生成与应用管理下线、菜单缓存迁移、固定范围升级工具、Agent/Skill、回归脚本和报告。当前数据库、附件、凭据、本机一次性业务清空SQL和构建产物不在提交范围内。

根VERSION、后端revision与application.version、PC/H5包版本、H5锁文件根版本统一2.0.0；pnpm锁文件没有工程版本字段，未替换依赖自己的版本。两端H5个人页和关于弹窗的版本展示改为读取包版本。README显示V2.0，新增Python缓存/字节码忽略规则，保留运行数据与密钥排除规则。

| 发布前验证 | 结果 |
|---|---|
| 后端完整干净构建 | JDK21运行、Java17编译目标，722项通过，失败/错误/跳过0；17个反应器条目成功 |
| JavaScript回归 | 134项通过，失败/跳过0 |
| PC | 类型检查及Vite生产构建通过，Vite耗时5分钟 |
| 客户/工程师H5 | 两个模式完整构建通过，分别7.36秒/8.76秒 |
| 冒烟脚本自测 | Python unittest 1项含7个响应子场景通过 |
| 本机服务 | 三端只读L0冒烟9/9，未新建业务夹具 |
| 发布内容 | 版本一致性、明确文件清单、凭据材料扫描及暂存diff检查 |

V2.0后端候选SHA256为`a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b`。构建快照1417份源文件一致；暂存门禁又发现4个新增文件EOF空行并规范，其中后端只涉及一份测试文件的末尾空白，已与构建快照按规范化换行比较语句完全相同，其余1416文件和全部生产源码/资源哈希一致，不为纯空白变化重复完整测试。证据`.local/v2-build-v26-evidence.json`、`v2-full-build-v26.log`、`v2-source-manifest-v26.json`、`v2-post-format-source-check.json`。

工具自带pnpm在包版本更新后触发依赖状态自动安装，因全局store注册symlink权限失败；使用现有node_modules运行同一package.json构建脚本npm run build后通过，未更换依赖或锁文件。默认执行环境的本机HTTP权限导致首次冒烟工具2个子场景及L0失败，换到具备本机HTTP权限的执行环境重跑均通过，保留初始日志，不作为产品故障。保留既有sm-crypto和大chunk警告，未提高阈值。证据`v2-js-regression.log`、`v2-pc-build-final.log`、`v2-h5-*-build.log`、`v2-python-tests-final.log`、`v2-final-service-smoke-retry.log`。

本轮仅构建和Git代码发布，不替换当前本机后端运行JAR，不执行数据库迁移或业务清空；新候选包位于忽略目录。浏览器、GPS/摄像头/地图AK、外部CA/TSA和容量SLA的既有未验收边界保持，不因发布大版本改记为通过。版本以main与v2.0.0所指提交定位，最终远端推送另以实际refs核验。


## 40. Git 失效资料清理与本地保留

2026-11-07用户要求以本地为主，删除Git中的无效文件。本轮从v2.0.0提交e2a61cd的干净工作区出发，按项目维护、渐进优化和代码检查规范，先统计跟踪文件并检查引用。未按文件大小、历史日期或静态import数量直接删除应用功能；动态菜单视图、实际使用的地区mock数据、构建/部署脚本、业务测试和不可改写的历史Liquibase资源仍保留。

发现Docs仍跟踪23个日期任务目录、共84份临时工作资料，既有忽略规则不会自动取消历史文件的跟踪。这些目录不属于唯一正式报告或部署SQL，当前源码/构建/部署/测试/Agent入口没有实际文件路径引用。包括23份SQL（含2份较大的历史数据库导出）、30个一次性Python脚本、22份旧笔记，以及2份TSV、2份JSON、2个mjs、2份txt和1份Java源码快照。没有展开或输出数据库导出正文、口令或私钥，只核对文件路径、大小、引用及内容摘要。

通过精确路径清单git rm --cached取消84文件跟踪，保留本地原文件；已逐一确认84份本地文件仍存在且SHA256与清理前一致。新增Docs日期任务目录与Docs内临时SQL忽略规则，84文件均匹配忽略规则，避免后续git add再次带入。当前Git文件数由2523减至2439，当前版本树不再包含3993489字节（约3.81 MiB）历史资料；这不等于Git历史数据库体积减少。Docs中只分发Reports-README.md，继续保留唯一正式报告及本次记录。

本次仅整理Git跟踪和文档，应用版本保持2.0.0；不重打/移动v2.0.0标签，不改写或强制推送历史提交。已有历史提交及v2.0.0标签仍能包含旧任务资料，当前main清理不代表这些文件已从整个Git历史永久抹除。本地归档不复制、不改名、不物理删除。

验证采用本地原文件哈希、索引忽略规则、发布树与原提交的源码子树对象比较以及暂存diff检查。后端、PC、H5、scripts、docker、tests、.agents和agents子树应与原提交逐一相同；不为无源码变化重复耗时构建，也不将上轮722项/134项测试虚记为本轮重跑。本轮仅执行本机只读L0服务冒烟，不写业务库、不创建测试数据、不重启服务。具体校验证据为`.local/git-cleanup-manifest.json`、`git-cleanup-validation.json`、`git-cleanup-service-smoke.log`和`git-cleanup-staged-check.log`。提交范围严格限定为84份归档的索引删除、.gitignore与本报告，远端以本次main整理提交核验。


## 41. 本地冗余工具代码清理与回归

2026-11-07继续按用户要求检查本地无效文件与冗余代码，基线main提交c62a592、工作区干净，工程版本保持2.0.0。先运行repo_inventory.py盘点2439个跟踪文件，再检查工具导出、全仓调用、自动导入配置、桶式导出、动态菜单视图与构建入口。没有将低频能力、未在静态import出现的页面或文件体积大直接当作无效代码。

本轮实际清理如下：

| 项目 | 删除内容与依据 |
|---|---|
| utils/typeof.ts | 整份删除，11个类型判断函数，无模块导入或再导出路径；自动导入未注册此模块，动态路由仅加载views中的Vue页面 |
| utils/index.ts | 删除18个无调用工具函数和SortTree类型：getProperty、getHanByNumber、getRandomInterger、getTypeOf、formatPhone、hidePhone、isEmpty、toCase、randomNum、average、deepClone、isLeapYear、isOdd、isEven、rgbToHex、randomHex、sortTree、fileToBase64；调用核查覆盖PC/H5、构建配置与测试代码 |
| utils/form-field-identity.ts | 删除无调用的patchFormFieldIdentity、watchFormFieldIdentity两个旧兼容包装，保留实际使用及测试的Accessibility实现 |
| mock/_utils.ts | 删除无调用的resultError；地区mock仍使用defineMock、getDelayTime和resultSuccess，全部保留 |
| 本地Python缓存 | 精确删除scripts/quality和tests下3个.pyc，共21869字节；先验证解析路径在当前仓库内、非重解析点，随后删除空缓存目录；原Python源码和测试均保留 |

源码合计删除1份文件及其11个函数，另从3份文件删除21个函数与1个类型，共228行；按TypeScript AST逐条比较，剩余代码语句与基线一致。没有改动依赖或锁文件、后端/H5源码、数据库迁移、部署与业务脚本，没有新建业务夹具、操作业务清空或替换运行JAR。地区数据、动态页面、历史迁移和上轮刻意保留的本地历史归档不作盲删。Git跟踪范围未发现jar/class/pyc/log或dist/target构建产物。

验证结果：现有JavaScript回归134项通过，失败/跳过0；本机PC/两端H5/API只读L0冒烟9/9。PC完整类型检查与Vite生产构建通过（Vite耗时4分41秒），frontend_assets.mjs --check通过，三类Office预览引擎仍为独立入口，受检页面静态闭包未提前加载它们。保留既有sm-crypto浏览器crypto外部化和大chunk警告，未修改阈值。证据位于忽略目录.local中的local-cleanup-manifest.json、local-cleanup-cache-manifest.json、local-cleanup-source-validation.json、local-cleanup-js-tests.log、local-cleanup-service-smoke.log、local-cleanup-pc-build.log、local-cleanup-assets.log和local-cleanup-final-validation.json。

本次删除对象属于未被使用的代码，未量化页面加载速度收益，也不把源代码行数减少等同于浏览器传输体积下降。后端与H5无源码变化，未重复运行其完整构建；浏览器、摄像头、GPS和地图AK继续按用户要求跳过。不会把当前只读冒烟记录为新的完整业务闭环验收。本轮仅推送main清理提交，不移动v2.0.0标签，不改写历史。


## 42. V1 旧产品版本标记核对与运行版本同步

2026-11-07用户澄清要处理的是V1.0.x旧产品版本号，非R测试批次或v构建流水编号。此前旧候选JAR清理仅生成计划、未执行任何删除；该计划已明确标为cancelled/executed=false，历史构建包未删除或重命名。本轮从402c80b干净工作区开始，版本继续使用已发布的V2.0/2.0.0。

全仓跟踪文本检索发现PC config/plugins/app-info.ts启动横幅仍写死LS Aegis Web v1.0.0，现改为直接读取package.json的version。直接执行插件buildStart确认实际输出v2.0.0，apply仍为serve；PC完整类型检查通过。CHANGELOG.md保留真实的1.0.0-SNAPSHOT初始化历史，在顶部补充2.0.0条目并指向唯一正式报告。第三方aieditor、vue-color-kit、picocolors等依赖版本以及openhtmltopdf兼容说明中的1.0.x不是产品版本，未替换为2.0.0。

根VERSION、后端Maven revision、application.version、PC/H5包版本、H5锁文件根版本逐项确认均为2.0.0；两端H5界面已有APP_VERSION读取包版本。本轮没有将历史报告中的旧版本结论改写为新版本，也没有改动迁移批次标识、changeset或依赖版本。证据.local/v2-version-consistency.json、v2-version-banner-check.json、v2-version-typecheck.log。

运行包另发现不一致：本机后端旧包中application.version为1.0.0，内部15个工程模块坐标为1.0.5。与此前已通过722项后端测试及完整构建的V2.0候选逐项比较，15模块共1245项非META-INF生产类/资源完全相同；外层生产类相同，application.yml差异仅application.version，其他外层变化为类路径/分层索引及Maven版本元数据。未重写第三方库版本。旧包SHA256为372589145f1842da45df7378190feecf6d25ef0c7b4254f86efeb3b0706f2230，V2.0包为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b。证据v2-runtime-jar-comparison.json、v2-runtime-module-comparison.json。

先确认当前容器运行状态、自动Liquibase迁移关闭、V2.0候选与既有回退包摘要，再停止同一本机后端容器、替换/app/jars挂载对应的启动JAR并启动；配置、数据卷、账号权限均保留。启动约62秒，启动过程中的前两次冒烟暂时断连，待日志确认Started后重测，最终三端及API只读L0冒烟9/9，容器实际挂载JAR的SHA256与V2.0候选一致。运行包现为2.0.0，不再仅源码显示2.0.0。没有执行数据迁移、创建业务测试数据或清空业务记录。证据v2-version-service-smoke-final.log。

本轮针对serve启动横幅实际执行与完整类型检查验证，没有为版本元数据变化重复PC/H5生产打包或后端722项测试；上一轮PC生产构建结论仍见41节，后端候选完整构建依据见39节。未执行浏览器业务闭环，摄像头/GPS/地图AK继续跳过。提交只包括启动横幅、更新日志及本报告，正常推送main，不移动v2.0.0发布标签。


## 43. Git 云端 V1 标签与本地旧构建包清理

2026-11-07用户进一步明确本意是清除Git云端无效的V1.0文件，并要求复查本地旧文件、构建包及V2.0一致性。本轮以5ba1ebf干净工作区为基线，重新获取远端引用并通过GitHub API盘点Release。核实远端只有main分支；main当前树没有跟踪V1.0命名文件、JAR/WAR/class/pyc、压缩构建包或target/dist产物；GitHub Releases及其附件均为0。云端可见的旧产品版本入口实际为v1.0.0、v1.0.1、v1.0.2、v1.0.3四个标签，不能把本地忽略的旧包数量写成云端已删除文件数量。

四个旧标签对应提交分别为b4ff91353b63f7957caa428866233917872a9a69、a0754dc1e2f430c8c8fa92d6cacd87275019a042、43c127f73a678df9f2417d748e8e739c01e9c23b、c029bab048ac13d715f4c4d5bb62fbb11ecddc42，均经merge-base确认已被main包含，构建/运行/部署入口不依赖这四个tag。记录精确引用对象后核对远端未变，使用一次原子push删除四个云端旧标签，再用期望对象校验删除本地对应标签，避免后续push --tags复活。远端复查四个旧引用均消失；main清理前提交和v2.0.0注释标签对象c47a55b567a698377bf7f7ea4daf95b76ed4b795保持不变。本轮保留V2.0发布入口和Git提交历史，没有强推main、移动V2标签、重写历史或声称旧Git对象已被永久擦除。证据.local/v1-cloud-release-inventory.json、v1-cloud-tag-inventory.json和v1-cloud-tag-cleanup-result.json。

本地按包内application.version或本项目Maven元数据识别真实V1.0.x，核对全部7个容器的挂载与活动进程、宿主Java进程，无运行引用后执行精确文件清理。删除前校验解析绝对路径处于当前仓库、父路径和文件均非重解析点、大小与计划一致；不递归删除共享目录或容器。结果如下：

| 清理对象 | 实际数量 |
|---|---|
| 旧候选与before回退fat JAR | 44份，确认已被当前V2.0取代 |
| 历史构建上下文中的app.jar | 7份，无容器挂载或活动进程引用 |
| 宿主target旧模块JAR与本地旧迁移工具缓存中的项目模块JAR | 31份，项目坐标1.0.x；当前跟踪的迁移工具源码仍保留 |
| 旧boot薄包jar.original | 1份，包内application.version为1.0.0 |
| 总计 | **83份旧项目包，9947639976字节，约9.26 GiB**；另移除24个已空候选目录 |

本地最后再遍历.local与ls-aegis-server中的JAR元数据，旧V1项目JAR为0；仅剩运行包、原V2.0候选v26及本轮干净构建候选v27三份项目包，版本全部2.0.0。删除不涉及第三方缓存依赖、数据卷、密钥、业务数据、历史SQL和有效测试/迁移源码。历史报告、CHANGELOG初始化条目及第三方库1.0.x版本保留原含义，不伪改为产品2.0.0。证据v1-final-cleanup-result.json、v1-final-usage-check.json、v1-final-static-validation.json和v1-final-remaining-packages.json。

| 清理后验证 | 实测结果 |
|---|---|
| 后端干净完整构建 | 从当前源码新建/build-v27快照，排除所有旧target/JAR，JDK21运行、Java17编译；17个反应器条目BUILD SUCCESS，耗时3分1秒 |
| 后端测试 | **722项通过，失败/错误/跳过0**；1417份源码与构建快照逐一摘要一致 |
| 前端JavaScript回归 | **134项通过，失败/跳过0** |
| 前端现有产物依赖检查 | frontend_assets.mjs --check通过，三类Office预览引擎独立加载 |
| 当前V2.0运行环境 | PC/两端H5/API只读L0 **9/9**；运行JAR与原V2.0候选SHA256仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b |
| Git内容 | 应用源码、配置、依赖锁文件和既有测试均未改动；本轮main提交仅更新正式报告，云端标签删除单独实际执行核验 |

本轮后端新候选SHA256为6a7473b8b658be074cdf2de01e3f015883ad4c9e77d63c3cb8a7c952e9f1394c，位于忽略目录.local/backend-v2-v27。本轮未替换或重启当前运行包；新旧V2候选SHA不同不代表源码变动，当前源码快照核对通过，包均为2.0.0。完整证据为v2-build-v27-evidence.json、v2-source-manifest-v27.json、v2-full-build-v27.log、v1-final-js-tests.log、v1-final-service-smoke.log和v1-final-assets.log。

因本轮仅删除未跟踪的旧后端包与Git旧标签，未改PC/H5源码，未重复其生产打包和类型检查；相应最近验证见41、42节，本轮补跑134项前端回归及产物门禁。不把只读冒烟当作新的完整业务闭环；浏览器、摄像头/GPS/地图AK仍按既有范围跳过。未创建业务测试数据，因而无需业务补偿清理。


## 44. 截图旧提交路径的 V2 工程适配

2026-11-07用户用GitHub文件列表截图澄清，关注的是文件右侧release: v1.0.1至v1.0.5的最近修改说明，要求先检查用途，有用的更新V2.0、无用的删除。本轮以9effb73干净工作区出发，范围是截图中仍显示旧提交的7份根文件及.cursor/rules目录；不将旧提交说明当作文件当前版本，也不继续删除版本标签或改写历史。

沿README、生产Dockerfile、scripts/install.sh/deploy.sh、docker/dev-all.cmd及Agent规范核对实际用途，7份根文件均有当前消费者，.cursor/rules中的规范仍指导现有代码维护，没有可凭版本文字直接删除的文件。本轮删除文件数0；保留并实施下列工程更新，不能将此次维护描述成删除有效旧版源码：

| 截图路径 | 当前用途与V2.0实际适配 |
|---|---|
| .cursor/rules | 保留9份规范；更新项目与Git规范、平台一体两份：根VERSION为准，当前2.0.0，缺失不回退V1；普通提交不自动升版，与AGENTS一致；补齐H5/后端/镜像版本核对，明确PC Arco、H5 Vant及已下线产品边界 |
| .dockerignore | 生产根上下文使用；维持.local排除，补排除本机.env及local覆盖、Agent角色资料和Python缓存，保留构建所需的前端.env.production及后端/PC源码 |
| .env.example | 当前生产模板；新增ITLS_VERSION=2.0.0并注明与VERSION一致，只控制项目镜像，不替换第三方版本 |
| .gitattributes | Git实际换行规则；继续保持Linux shell为LF，补充Windows cmd/bat/ps1为CRLF、VERSION为LF，保留Windows PowerShell所需UTF-8 BOM |
| dev.cmd | 保留根开发入口；读取VERSION展示当前版本，校验下游入口并正确返回子进程失败码 |
| docker-compose.yml | 保留生产栈，与本地开发栈分离；四个项目镜像由模糊:prod改为ITLS_VERSION控制、默认2.0.0；数据库/Redis版本、卷、端口及禁用自动迁移保持 |
| pack.bat | 保留Windows打包入口；原样转发参数，支持路径中的空格和&；Batch/Help模式不暂停，传播子进程退出码 |
| pack.ps1 | 保留实际发布工具；读取并校验根VERSION，横幅及deploy-info.json写入2.0.0；包内强制包含VERSION和部署元数据；排除.local、历史Docs、Python缓存和本机环境覆盖；批处理失败不再读键阻塞 |

原pack.ps1未排除.local，可能把旧包和一次性本机测试资料带入部署包；现用合成敏感哨兵文件验证排除，不读取或打包真实凭据。同步修正Dockerfile.prod构建示例及现用MIGRATION-R29.md中的候选镜像示例为ls-aegis-server:2.0.0，仅改使用说明，不改已执行迁移或SQL。产物文件名仍为ls-aegis-deploy.tar.gz，保留既有上传安装命令兼容。

新增release_support.test.cjs的5项行为回归，使用独立合成目录及真实cmd.exe、Windows PowerShell 5.1、tar.exe执行：源码部署包有2.0.0元数据且排除本机资料；缺失/非法VERSION失败且不提示输入；pack.bat Batch返回子进程错误且不pause；dev.cmd展示版本并透传错误；下游入口缺失明确失败。既有134项前端回归及新增5项合计139项通过、失败/跳过0；后续参数原样转发修订后5项再次定向重跑通过，测试路径同时包含空格与&。合成夹具结束时验证绝对路径处于.local下再清理，不操作业务数据。证据.local/v2-support-tests.log、v2-support-all-tests.log。

Docker验证使用.env.example显式渲染生产Compose，四个镜像均为对应项目名:2.0.0，MySQL8.0.33/Redis7.2.8-alpine及关闭自动Liquibase保持。另以纯合成上下文执行真实FROM scratch/COPY构建，5个必要输入保留、8个应排除的本机文件均不出现在输出中；不以手写glob模拟代替Docker结果。不执行生产compose up，不创建或重启服务。证据v2-support-compose-context.json、v2-support-context-build.log。

本机PC/两端H5/API只读L0冒烟9/9，Git换行属性及Windows脚本实际字节核验通过，运行JAR SHA256仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b。后端/PC/H5应用源码子树与基线一致，未重复应用编译及全业务闭环；本轮验证重点是发布支持文件的真实入口、归档和构建上下文。根版本保持2.0.0，不创建/移动v2.0.0标签。证据v2-support-file-audit.json、v2-support-service-smoke.log。

本轮使用release: v2.0.0维护提交记录上述实际变更并正常推送main，目标是截图路径的最新提交说明反映V2.0工程适配；历史提交中的V1文字不改写。云端验收以最新main引用及上述8个路径的最近提交查询为准，记录v2-support-remote-paths.json。浏览器、摄像头/GPS/地图AK继续跳过，未声称执行新的页面业务验收或生产镜像完整部署。


## 45. 项目规范目录迁移为 rules

2026-11-07用户要求将GitHub列表中.cursor/rules改为只叫rules。本轮从7147a50干净工作区出发，将9份规范整体迁移到仓库根rules/；AGENTS.md与README.md入口、目录树、规范内部链接及globs同步更新。逐份与原Git内容按规范化换行比较，除.cursor/rules/到rules/的路径替换及一处Markdown尾部空白清理外，9份规范内容保持一致；当前工程入口和规则自身不再引用旧目录。历史报告中的旧路径保留当时事实。

规则现作为通用项目规范由AGENTS.md显式引用；原Cursor专用自动发现路径不再保留，不声称Cursor仍会自动扫描根rules目录。.gitignore改为忽略整个Cursor缓存目录，去掉旧规则例外；Docker上下文和部署tar继续排除规范资料，补齐新rules路径，避免迁移后意外带入生产包。

沿用真实cmd.exe/Windows PowerShell/tar的5项入口与归档回归全部通过，新增rules合成哨兵确认不进入部署包；Docker scratch实测5个必要输入保留、9个应排除合成文件均不进入输出。证据.local/rules-rename-validation.json、rules-rename-tests.log、rules-rename-compose-context.json与rules-rename-context-build.log。仅目录/链接及打包排除更新，业务源码、运行服务和数据未改变，不重复应用完整构建。版本保持2.0.0，正常推送main，不创建或移动标签；GitHub新路径及旧路径移除另以实际远端目录核验。


## 46. Skills 根目录迁移与历史共同作者盘点

2026-11-07用户要求把.agents/skills改为根skills，同步引用并验证后推送，同时询问GitHub为什么显示cursoragent / Cursor Agent以及能否清除。本轮从8b10487干净工作区开始，将5个项目技能、13份SKILL.md/UI元数据/参考资料原样迁移到skills/；按规范化换行与原Git内容逐一核对一致。AGENTS.md、README.md、两个agents角色入口和tests/AGENTS.md同步引用，README目录树补齐skills。历史报告不批量重写原路径事实。

根skills不是原工具专用的.agents/skills扫描入口，因此明确以AGENTS.md任务路由按需读取，不声称原生技能菜单自动发现保持不变。技能名称、描述、default_prompt和相对参考路径均保留；使用skill-creator的quick_validate.py逐项校验5项全部通过，另核对所有本地参考链接与元数据中的技能调用名。证据.local/skills-rename-validation.json。

更新Git忽略、Docker上下文及pack.ps1排除目录，避免通用skills资料进入运行镜像和部署归档。复用真实cmd.exe/Windows PowerShell/tar的5项入口归档测试全部通过，skills合成哨兵未进入包；Docker scratch真实上下文验证5个必需输入保留、10个应排除文件均未出现在输出。不启停或部署当前服务，不写业务数据；应用源码和当前运行JAR未修改，不重复应用完整构建。证据skills-rename-tests.log、skills-rename-compose-context.json和skills-rename-context-build.log。

历史作者盘点发现8条最早提交含精确脚注Co-authored-by: Cursor <cursoragent@cursor.com>，覆盖v1.0.0至v1.0.5及两条后续安全修复提交。Git实际author/committer沿用leason，Cursor来自提交说明中的共同作者标记，并非本轮迁移添加。GitHub会按该脚注及邮箱关联共同作者，依据见[官方共同作者文档](https://docs.github.com/en/pull-requests/how-tos/commit-changes/creating-a-commit-with-multiple-authors)。近期V2维护提交无该脚注；项目Git规范补充未经用户要求不自动附加工具共同作者署名。证据cursor-coauthor-inventory.json。

从已推送历史彻底移除脚注将改写8条原始消息及其后继提交ID，V2.0注释标签如需跟随净化历史也会更换引用对象；文件源码树不应发生变化。GitHub[修改提交消息说明](https://docs.github.com/en/pull-requests/how-tos/commit-changes/changing-a-commit-message)确认已推送消息改写需要强制推送；本项目rules/项目与Git规范.mdc明确禁止未经用户要求强推main/master。用户当前仅询问可否清除，尚未明确批准历史强制更新，因此本轮只正常提交/推送技能迁移，不修改真实main历史或V2标签。

已准备隔离的本地元数据清理预览：只删除上述8条工具脚注，保留真实author/committer与所有提交的tree，借助只读原对象源在独立预览仓库生成候选main及V2标签；脚本没有push操作。迁移提交推送后运行该预览并记录cursor-coauthor-preview.json供最终确认，预览不等于已清除云端共同作者；不保证GitHub贡献统计即时刷新。真实历史改写需用户对main和v2.0.0两个引用明确授权后才执行。


## 47. 授权清除 Cursor 历史共同作者署名

2026-11-07用户明确允许按46节隔离预览改写main、v2.0.0并带租约强制推送，撤销此前仅预览等待确认的限制。本轮先确认工作区干净、main仍为900148ff8523b197fda4300b8f3fd8f8f3f58af1、V2注释标签仍为c47a55b567a698377bf7f7ea4daf95b76ed4b795，并核对远端精确引用未变化。

逐条重新验证16个候选提交：仅8条原始消息删除精确的Cursor共同作者脚注，后继提交仅更新parent关联；所有tree一致，真实author/committer及时间戳保留。V2注释标签只更换object目标，名称、说明和tagger信息保留。导入预览对象后，对main旧提交和tag旧对象分别设置精确force-with-lease，并在同一次git push --atomic中更新两个远端引用，未使用无租约强推。结果如下：

| 引用 | 授权改写前 | 按预览更新后 |
|---|---|---|
| main（16条历史） | 900148ff8523b197fda4300b8f3fd8f8f3f58af1 | cf2805ba46fac9bef47ed8754a1abbea38e4c822 |
| v2.0.0注释标签对象 | c47a55b567a698377bf7f7ea4daf95b76ed4b795 | 8c4f903b1726adf86b4c2b21f9ffdfa73b591207 |

远端核对成功后，采用带旧对象校验的本地update-ref事务同步main和标签，origin/main同步，工作区保持干净。GitHub API随后核验main全部16条记录的Cursor共同作者脚注为0、实际作者集合仅leason、V2标签与预览对象一致；contributors接口返回仅itlslixinyu（16次贡献），cursoragent已不在列表中，不仅依赖本地git log判断云端显示。证据.local/cursor-coauthor-authorized-validation.json、cursor-coauthor-leased-push.log、cursor-coauthor-history-result.json及cursor-coauthor-github-verification.json；完整旧/新提交对应表仍在cursor-coauthor-preview.json。

本次历史操作未修改任一源码树，运行JAR SHA256仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b，未重启服务或修改业务数据；不为提交元数据重跑应用构建和业务测试。随后以普通文档提交追加本节并正常推送main，不再次改写或移动V2标签。旧报告中的提交ID保留为当时证据，协作者仍持旧历史的副本需要按新远端重新同步，不应把旧分支历史重新推回main。


## 48. GitHub 主页侧栏与贡献 API 差异复核

2026-11-07用户截图显示main为565c5c9时，仓库主页侧栏仍有Contributors 2及cursoragent。此前接口只返回itlslixinyu，不能据此宣称用户可见侧栏已消失；47节的接口核验结果成立，但前次对外“页面已经清除”的表述过度，本节更正验收口径。

重新读取全部远端广告引用，只有HEAD/main与V2.0标签；GitHub默认分支main，非fork、无任何PR及refs/pull残留；当时main全部17条提交的author仅lixinyu@ls.hb.cn，无Cursor共同作者脚注，未发现漏改分支或标签。含匿名的contributors API仍只返回itlslixinyu（17次贡献）。随后请求stats/contributors，首次202计算中，后续200完成且只有itlslixinyu。证据.local/cursor-sidebar-diagnosis.json、cursor-sidebar-statistics-check.json。

直接读取公开GitHub主页，响应为200，未出现cursoragent链接，但HTML中的Contributors仅是占位，未返回人数和用户条目，不能用这一空结果证明浏览器侧栏已清理。证据cursor-sidebar-page-check.json、cursor-sidebar-page-final.json；overviewSidebarClearedVerified明确为false。结合干净历史、接口与用户截图，判断为GitHub侧栏统计/索引残留，尚无直接页面验收证明，不再次改写历史、改默认分支或删除重建仓库来碰运气。

GitHub[官方统计API文档](https://docs.github.com/en/rest/metrics/statistics)说明统计按默认分支SHA缓存，202请求会触发后台计算；[贡献者接口文档](https://docs.github.com/en/rest/repos/repos#list-repository-contributors)亦说明接口数据存在缓存。两者不保证主页组件同步刷新，也不能承诺固定刷新时限。当前已完成支持的统计请求，用户可强制刷新页面复查；若侧栏持续残留，需请GitHub支持核查该仓库的overview contributor索引。

可供用户提交GitHub支持的说明：仓库https://github.com/itlslixinyu/ls-aegis-mcs，默认分支main；已于2026-11-07改写main及V2.0历史移除8条工具共同作者脚注，所有源码树不变；所有当前分支/标签与PR引用已核对；contributors及stats/contributors均仅返回itlslixinyu，但用户主页截图仍显示cursoragent，请重新计算或刷新仓库概览贡献者索引。本轮仅准备此说明，未代表用户发送支持消息。

本轮仅追加澄清报告并正常推送，不修改应用代码、运行服务或业务数据，不把服务可用性测试冒充GitHub页面验收。最终状态为：历史署名清理PASS、API贡献统计PASS、用户可见主页侧栏清除尚待页面复核。


## 49. 本轮架构精简、业务回归与性能审查

用户在语音中重新明确并确认本轮工作：按现行业务流程整理代码架构、本地目录及部署流程，删除确认无用内容、收敛重复实现，重点做整体性能基线及优化；能独立完成的持续执行，必须本人操作的记录待办并推进其他事项。普通改动普通提交并保持版本，较大且完整的已验证批次再升小版本；不修改真实业务数据。本轮基线f5a68bb、工作区干净，使用现有skills工作流，执行台账为.local/overnight-review-state.json。按小批记录实际结果，不将本节未完成事项或旧测试结果计为新通过。

### 49.1 A批：多级菜单响应式状态与失效事件层清理

发现useLevelMenu订阅useRouteListener，但setRouteEmitter在全仓没有生产调用，导致混合/分栏布局一级选中状态始终没有初始化或更新；一级菜单另存初始化快照，权限菜单异步加载或刷新后不能同步。该hook还重复实现useMenu已有的克隆、隐藏过滤和单子页展平逻辑。新增7个状态回归，在原代码上1通过、6失败，复现后修复。

现由混合/分栏布局直接共用useMenu的computed结果，一级选中和二级同级列表都随路由与权限数据变化；支持详情页activeMenu、按完整路径段选择最长前缀，保留隐藏/alwaysShow与源路由不变的行为。归一化点击目标，保留push/replace区别，目录外链用平台安全打开方法，noRedirect叶子回退真实path。删除无生产发射者的useRouteListener.ts及桶式导出，移除两布局一次性getOneLevelMenus调用。未改接口鉴权或业务权限，只复用后端下发的同一菜单树。

验证：新增7项全部通过，全量Node回归146项通过，失败/跳过0；PC完整类型检查和生产构建通过，Vite耗时3分35秒，三类Office异步引擎门禁保持通过。受检首页JS/CSS静态闭包2348963→2348622字节，gzip估算595500→595412字节；这只是小幅产物差异，不冒充实际页面耗时收益。本批主要收益是修复状态与减少重复维护，未宣称完成浏览器画面验收。证据.local/overnight-menu-before.log、overnight-menu-after.log、overnight-menu-all-tests.log、overnight-menu-pc-build.log、overnight-menu-assets-comparison.json。

### 49.2 B批：现行业务测试入口修订与14组闭环

用现有V2.0候选v27在全新独立MySQL/Redis/后端执行14组API基线，最初12通过、2失败：旧脚本要求报价重复转工单报错，而现行实现为幂等返回同工单；客户H5仍按客户编号登录，未使用一次性邀请码。修订后进一步发现客户报修必须先上传现场照片，旧夹具未准备该前置，因此该阶段13通过、1失败。以上均保留失败证据，没有弱化应用要求来凑通过。

现验证报价重试返回同一工单ID且同报价关联工单数为1；客户由管理员为合成客户签发一次性邀请，兑换会话后通过真实H5上传接口提交自生成PNG并取得授权fileId，再报修、派单、工程师处理、客户确认、结算、关单及POST退出。合成图片工厂由签章测试与报修测试共用，上传助手按身份明确传递会话、设置超时并检查HTTP及平台结果。

最终14组PASS、0 FAIL/BLOCKED，包括鉴权身份交替、采购与销售库存/资金、报价独立确认及转工单幂等、维修库存回滚、签章真实PDF密码学核验、客户门户闭环与工作台20字段独立汇总。验证码答案仅从本轮独立Redis夹具读取，不代表浏览器图像交互验收；自签证书不代表外部CA信任链；合成坐标不是GPS实机结果。整个栈仅含合成资料，不访问当前业务库。

清理后独立按本轮标签查询：容器0、网络0、私有环境manifest已移除。证据.local/overnight-business-baseline.log、overnight-business-current-flow.log、overnight-business-current-flow-photo.log、overnight-business-current-flow-results.json和overnight-business-cleanup.json。脚本对齐现行契约后普通提交，产品版本不变。

### 49.3 C批：银行匹配候选查询性能与闭环

原候选逻辑加载同账户全部已确认资金单，在Java过滤金额/日期并按ID逐个再查单据；现将金额差≤0.01及自然日差≤2天先交给SQL筛选，日期右界用第3天零点开区间，候选一次读取所需列，内部候选对象共用于下拉与匹配校验；占用查询只投影业务单ID。保留selectList数据权限映射路径、账户/确认状态/逻辑删除条件、占用排除及手工匹配的后续行锁校验，不改历史SQL或表索引。缺失银行流水日期/金额明确报业务错误。

真实MyBatis/H2夹具为收入/支出各准备1000份无关已确认单据，并验证±0.01金额、±2自然日边界、下一日零点、草稿、其他账户、占用及逻辑删除样本。正确配置逻辑删除后，原实现两项性能断言失败：单据读取3次、返回Java累计1007行；优化后1次、3行，仍只给出同样2份有效候选。另有空日期/金额保护用例，合计3项通过。H2微基准不等同完整租户权限插件验证，未作此泛化宣称。证据.local/overnight-bank-read-baseline.log、overnight-bank-read-after.log。

完整后端17条目构建成功，725项测试全部通过、失败/错误/跳过0，1418份源码与隔离构建快照一致。候选v28 SHA256为8ac9e1b12e39dd98e54d1d9c9c1b21e4da038f58ec3098b9b3207532654af746。进一步新增PSI-BANK-01真实API闭环，创建合成账户/回款/付款，验证重复导入新增0、收入/支出候选、匹配状态、占用排除、无权角色拒绝及解除匹配重放幂等，再执行其他14组，最终15 PASS、0 FAIL/BLOCKED。临时栈自动清理；当前在用后端包未替换。证据v2-build-v28-evidence.json、v2-source-manifest-v28.json、overnight-bank-business.log、overnight-bank-business-results.json。


### 49.4 D批：调度依赖隔离与公共工具精简

构建基线首页index入口含CronDate/Luxon代码，追到公共utils桶直接导入cron-parser；解析器本应只由调度列表和Cron编辑器使用。现抽取utils/cron.ts，由两处调度调用显式加载，共用五次触发时间计算及平台DATETIME_FORMAT；删除旧重复日期格式化/解析实现与空script块。另审计25个公共工具命名导入点、自动导入配置和动态路由范围，移除没有真实消费的trim/formatMoney/max/min/sum及Pos类型，不删除引用相同名称的业务局部函数。证据overnight-common-utils-readers.json。

新增7项Cron回归覆盖五/六字段、通配年份兼容、空白、无效表达式、跨年/闰日及输入时钟不变。完整Node回归153通过、失败/跳过0，PC完整类型检查及真实Vite生产构建通过，耗时3分47秒。七字段的年份处理沿用存量策略，本批仅验证通配年份，不将特定年份限制或实际外部调度执行记为已验证。

新增frontend_module_audit.mjs，以真实Rollup chunk.modules及静态依赖闭包判定：首页Cron/Luxon模块0、调度页7；审计资料只写.local，不进入镜像。Office异步边界仍通过。相比A批完成后的同构建环境：首页JS/CSS闭包2348622→2260581字节，减少88041字节；gzip估算595412→567859，减少27553字节。这里只量化加载资源，未把它换算为浏览器首屏秒数或生产带宽收益。对照证据overnight-cron-assets-comparison.json、overnight-cron-module-audit.json、overnight-cron-tests.log、overnight-cron-all-tests.log和overnight-cron-pc-build.log。项目代码检查Skill补充该实际构建门禁。


### 49.5 F批：部署打包入口收敛与旧配置兼容

核对根生产Compose、四个生产Dockerfile及归档规则，实际部署从源码构建，但打包向导仍提供“使用本地已有产物（更快）”；该选项只检查JAR/dist是否存在，未改变镜像构建方式，且dist被排除出归档，承诺与行为不符。现只提供源码模式1和本地预验证模式3，旧缓存模式2明确提示后迁移为1；非法模式停止，保留-Rebuild/-NoBuild命令行兼容。模式3只是本地预验证，服务器仍构建镜像，不再暗示能直接使用本地产物。

源码部署包额外排除docker/ls-aegis-server/bin，避免把未被生产Dockerfile消费的陈旧JAR混入归档；该目录及本地构建脚本仍可用于开发，不删除有效开发入口。README明确根目录生产Compose与docker目录开发Compose区别，构建脚本说明同步。

实际在含空格及&的临时目录生成/读取tar包，分别验证源码模式1和旧缓存模式2：元数据/缓存均为模式1，VERSION和生产入口在包内，JAR、PC/H5产物、私有配置和测试资料均不在包内；另验证缺版本拒绝、批处理失败透传及开发入口，6项通过。完整Node回归154项通过，失败/跳过0。未触发上传、远程安装或在用服务重建，本批只改部署入口及资料，不重复执行未受影响的Maven构建。证据.local/overnight-pack-tests.log、overnight-pack-all-tests.log。


### 49.6 E批：受控真实接口性能对比与容量边界

新增isolated_capacity.mjs，只接受仓库内前后候选包目录，核对SHA、独立夹具运行号/容器/网络标签及loopback地址；拒绝复用已有环境manifest。先运行15组业务闭环，再在该全新库通过真实API串行创建确认回款，不以SQL伪造财务状态；只读对照限定并发1/5、每方案每档40样本并逐次断言同一候选ID。业务结果补充runId，防止误用上轮资料。未访问在用API或业务库。

首轮工具错误将时间戳前缀当成运行号，已改为显式runId校验，该轮夹具清理完成。随后1000份API造数触发现行统一编号“每日两位日流水，最多99张”限制，停止并清理，保留overnight-capacity-daily-limit.json/log。此限制在平台编号服务中明确实现，不在性能优化中擅自改变全域编码格式；大数据量API造数未完成。调整为80份无关回款及1份有效候选，数据库只读核对81份已确认单，15组业务闭环再次通过。

环境：Windows本机Docker，12 CPU、8223760384字节内存，同库双后端Java21，各-Xms256m/-Xmx768m；每方案预热5次、交替采样。基线v27 SHA6a7473b8b658be074cdf2de01e3f015883ad4c9e77d63c3cb8a7c952e9f1394c，候选v28 SHA8ac9e1b12e39dd98e54d1d9c9c1b21e4da038f58ec3098b9b3207532654af746。

| 只读并发 | 每方案样本 | 基线P50/P95(ms) | 优化后P50/P95(ms) | 错误 |
|---|---|---|---|---|
| 1 | 40 | 44.17 / 54.85 | 28.07 / 33.77 | 0 |
| 5 | 40 | 43.54 / 83.34 | 28.58 / 53.12 | 0 |

160次受测查询结果均一致，测量表明这次候选读取优化在此受控样本下有收益。宿主同时有开发服务及轻量前端构建，未声称严格独占硬件实验、生产最大吞吐或SLA验收。生产容量门槛未约定；99张日编号上限仍是业务容量约束，需将来结合实际日单量决定编码规则升级。本轮没有绕过该上限。

最新runId575bea49fa，测试后manifest移除、按精确标签独立复核容器0/网络0，失败轮也清理完成。证据.local/isolated-capacity-results.json、overnight-isolated-capacity.log和isolated-capacity-setup.log；私有夹具资料不提交。当前在用后端仍未替换。


### 49.7 G批：两端H5按需组件与静态分发复核

H5已有VantResolver，但main仍导入默认Vant并app.use整套注册及全量CSS，真实构建每端保留259个Vant模块。检查模板及动态组件引用后，删除整套注册，沿用现有解析器按模板导入；保留全部现有函数式API导出/全局兼容及Toast/Dialog/Notify显式样式，未删除报修、任务、照片和资料组件。

新增h5_module_audit.mjs，以两种mode实际Vite构建及Rollup模块图核验：未解析van-*组件为0，整套es/index.mjs注册入口不再保留，三类函数式API样式存在；模块由259减至118。客户全部JS/CSS809602→521309字节，gzip估算291130→202190；工程师809603→521310，gzip291136→202191。每端减少288293字节，约35.6%；gzip减少约88940字节。这是全部产物合计，含异步页面，不能称首屏传输减少同等大小或移动设备响应时间已验证。两端构建及新增门禁全部通过，项目检查Skill登记复用命令。

当前PC产物另经临时nginx:1.27.0只读挂载做真实HTTP验证，7/7通过：HTML协商缓存/安全头、JS gzip/Vary/一年哈希缓存、PDF worker MIME/缓存、缺失资源404、SPA回退及HEAD/ETag；受检入口JS368469→实际gzip141035字节。临时容器已清理，不接入在用后端。证据.local/overnight-h5-modules-before.json、overnight-h5-modules-after.json、overnight-h5-gate.log及overnight-nginx-check.log。浏览器画面与真实设备交互仍未新增验收，摄像头/GPS/地图AK继续跳过。


G批补查：原组件门禁仅检查.vue，进一步审查发现router/index.js的404页使用运行时template并含van-button，不能由模板解析器预编译；Vue默认运行时构建也不应依赖运行时模板编译。现迁至异步NotFound.vue，保留返回上一页/无历史回首页行为。门禁扩大到全部src模块，拒绝绕过预编译的运行时模板；双端构建再次通过，未解析组件0，函数式样式齐全。最终客户JS/CSS522128、gzip202808字节，工程师522129、gzip202826字节，相比原始基线均仍减少287474字节；以上替代前述中间产物数字作为最终交付指标。证据.local/overnight-h5-notfound-gate.log、h5-module-audit.json；仍未冒称浏览器页面交互通过。


### 49.8 H批：消息中心订阅生命周期

消息页在onMounted以匿名函数订阅count-refresh，未在卸载时解绑，关闭再打开页面后旧实例继续请求两类未读计数。新增回归实际执行页面TSX逻辑、共用真实mitt总线，模拟4次挂载/刷新/卸载并保留旁路订阅者：原HEAD代码失败，首实例累计6次请求而期望2次；修复以具名refreshCounts订阅，onUnmounted只移除当前函数，回归通过且其他订阅不受影响。缓存页生命周期未另作改变。

完整Node155项通过，失败/跳过0；PC类型检查及生产构建通过，Vite3分41秒。仍有既有大chunk提示及sm-crypto浏览器crypto外部化提示，未隐藏警告或声称无任何构建提示。证据.local/overnight-message-before.log、overnight-message-all-tests.log、overnight-message-pc-build.log。本批不改业务API及数据。


### 49.9 I批：H5镜像依赖锁定与冗余联网移除

生产H5 Dockerfile只复制package.json执行npm install，开发镜像也允许缺锁文件；现均要求package-lock.json并npm ci，避免构建时重新解析依赖。独立H5构建context补排除dist-*、.local、coverage、.env及.env.*.local，保留两端必要的公开mode配置。PC镜像已有frozen-lockfile，不重复改动。

实际检查当前nginx:alpine基础镜像已含Asia/Shanghai时区资料，原Dockerfile却再次apk update/install tzdata；Node构建阶段更没有需要apk安装的包，却仍更新索引。移除两处冗余联网步骤，固定此次已核验的Node20-alpine及nginx-alpine摘要，并在构建时断言时区文件存在。不是关闭TLS校验，也未清理共享镜像/缓存。固定摘要需要后续定期评估更新，本轮未作漏洞扫描认证。

真实Docker构建客户和工程师两种生产镜像均通过，npm ci从锁文件干净安装成功；各在独立无网络临时容器中通过Nginx语法、时区+0800、HTML、入口JS/CSS、SPA路由回退及缺失资源404，12项通过；未连接业务后端、不发送任何外部请求。证据.local/overnight-h5-docker-customer.log、overnight-h5-docker-engineer.log、overnight-h5-image-http.json/log。临时HTTP容器已全部移除；这些是生产静态镜像验证，不等同完整生产Compose上线或登录业务页面验收。


### 49.10 V2.1.0收拢、最终验证与剩余边界

本轮普通修复均独立提交；按用户已确认的“较大完整批次发小版本”规则，将A至I批收拢为2.1.0。同步根VERSION、PC/H5及锁文件根信息、Maven revision、application.version、生产环境模板/四个镜像标签、当前说明；历史V2.0记录及第三方依赖版本保留。新增version_consistency.mjs检查11处产品版本一致，不依赖手工全局替换。原v2.0.0标签不移动。

| 最终2.1.0验证层次 | 实际结果 |
|---|---|
| 后端完整reactor | 17条目成功；725测试，失败/错误/跳过0；1418源码快照一致；2分34秒 |
| Node功能回归 | 155通过，失败/跳过0 |
| PC类型检查/真实生产构建 | 通过，Vite4分04秒；真实模块图首页Cron/Luxon0、调度页7；Office异步门禁通过 |
| 客户/工程师H5 | 两模式真实构建和组件/函数样式门禁通过；各保留118个Vant模块 |
| H5生产镜像 | 两种角色实际npm ci构建成功；最终镜像12项静态Nginx/时区检查通过 |
| PC生产静态HTTP | 临时Nginx实际7/7通过；入口JS368469字节，实际gzip141025字节 |
| 最终候选L2 | 新建独立栈15 PASS、0 FAIL/BLOCKED；覆盖统一身份、PSI库存/资金/银行/汇总、报价独立确认及转工单、EOMS维修结算与失败回滚、签章PDF、客户H5邀请码/照片/报修/确认/退出 |
| 当前在用环境L0 | 9/9只读通过；未登录未写业务；在用JAR哈希仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b |
| 生产配置与实际源码包 | Compose config --quiet通过；实际归档元数据2.1.0/模式1；原打包偏好原样恢复；未上传或远程安装 |

最终后端候选位于.local/backend-v2-v29，SHA256为de6b907713ee25f63f7b2892d22e9c1e463a1c5b30da28f616741d6bbeef3f62，194979917字节；没有覆盖在用JAR。源码包.local/ls-aegis-deploy-v2.1.0.tar.gz为2552374字节、3219条目，SHA256 ae8d3e22bc6cbc45ad53f5b5a70841b27be7244ab19f25e05492a59c7feae6b1；独立逐项检查未含.git/.local/node_modules/target/dist、私有.env、业务数据卷或JAR，保留全量SQL、锁文件和两端公开mode配置。候选与私有证据不提交Git。

最终H5全部JS/CSS客户522128、工程师522129字节，两端gzip估算均202816字节；相比G批原始基线每端减少287474字节，仍是全部产物合计，不冒充首屏节省。后端银行接口的前后量化见49.6，最终2.1只做版本收拢且同逻辑L2重测通过，没有伪造再次压测结果。另记录一次独立dev启动43.031秒及业务测试后资源快照CPU0.75%、内存902.6MiB、120个PIDs；单样本、宿主缓存未清空，不是冷启动对照、峰值内存或生产容量上限。

最终合成栈runId f14881484a已清理，manifest移除，精确标签回查容器0/网络0；本轮专用H5测试镜像清理，未清理共享构建器、其他已有测试容器或在用数据。证据.local/v2-build-v29-evidence.json、v2-source-manifest-v29.json、v21-node-tests.log、v21-pc-build.log、v21-h5-build.log、v21-business-results.json、v21-runtime-sample.json、v21-source-pack.json、v21-cleanup.json及v21-live-readonly-smoke.log。

剩余边界如实登记：本轮是代码/构建、受控接口和静态镜像验证，不是全部管理页面逐按钮浏览器验收；浏览器L3、摄像头、真实GPS、外部地图AK未执行。外部调度关闭，Cron特定年份执行语义及外部调度未新增验证；现行每日99张编号限制保留，大数据量API造数受其约束。正式生产机器的持续负载、最大吞吐、业务SLA和完整Compose上线未验证，不能据本机160次候选查询及单次资源快照宣布整体生产容量达标。当前有效模块/兼容层、动态图标、权限/事务与库存资金链保留，没有按文件大小或模块数量盲目删减。

本轮没有需要用户即时点击才能完成的代码修改；需要浏览器或真实设备配合的验收留待可操作时执行，日单量与容量指标也需结合实际业务确定。本节交付的是经过上述验证的2.1.0源码候选和报告，未声称已持续运行满八小时或已完成公网生产上线。


## 50. 四项顺序优化与联动回归

用户指定依次完成调度年份、财务查询、H5分端构建、PC加载，验证模块联动后推送。基线57dbaf1、工作区干净；复用维护/代码审查/业务测试工作流，不修改在用后端或真实业务数据。分项记录实际结果，后续尚未完成的检查不计为通过。

### 50.1 调度年份预览

原getNextCronTimes直接丢弃第七段，指定2030年却显示2027/2028/2029等执行时间。新增反例基线11项中7通过、4失败；修复后11项全通过。现解析四位年份、范围、列表、起始/间隔及通配步长，校验1970至9999的预览范围、非法步长/逆序区间及多余字段。按允许年份直接建立时间窗口，最多返回5次；有限年份只有1次或已过期时返回真实的较少次数/空列表，不跨年凑数。远期密集表达式直接跳年，不逐秒扫描几十年。输入时钟不修改，旧五/六字段及通配年保持兼容。

编辑器文案同步“最多五次执行时间”。覆盖2030单年、过期年、列表去重、区间/间隔、闰日、年末边界、2099密集表达式及非法输入；PC基线类型检查/完整构建也通过。这里只修复本地执行预览，不改已保存表达式或实际调度任务，不声称已验证外部调度服务的全部Quartz语义。证据.local/r50-cron-before.log、r50-cron-after.log、r50-pc-before.log。


### 50.2 财务核销汇总批量读取

WriteOffHelper.listSummaries原来每个主单selectById、每条明细selectCount检查反核销。现先读取同一资金单的原明细，主单用带DataPermission的selectList分批读取所需列，反核销按原REV_OF标记前缀及方向分批查询，只投影reason；每批最多200个条件，空集合直接返回。保持汇总顺序/金额、任一明细反核销则整组不显示、已删除主单不回显及原标记跨资金单检测范围。写入、撤销、行锁、金额约束与事务代码均未改动。

真实MyBatis/H2回款/付款各验证120及450条明细；旧实现4项性能断言失败，分别127/457次查询；新实现为3/5次，结果一致，覆盖重复冲销、已删除冲销、相似ID、其他方向、跨资金单标记及空历史。扩展现有真实租户/DataPermission拦截器用例到WriteOff主单，验证部门/本人、其他租户/逻辑删除及写入范围，16项定向测试全通过。完整后端17条目成功、733项测试全通过，失败/错误/跳过0，1419份源码快照一致，耗时2分51秒；候选.local/backend-v2-v30，SHA256 203a75922613ae598192c61b5739d6ecb588c9d2914e8b8af902516e13da1f3d。

新增PSI-CASH-SUMMARY-01实际API闭环，采购/销售出入库形成开口，同一资金单分两次核销，检查主单ID/原因/金额，再逐次反核销，核对汇总隐藏、开口余额及未分配金额恢复，并拒绝无权角色。首轮15 PASS/1 FAIL是销售出库夹具缺少已确认销售合同，已补齐现行业务前置，未放宽应用要求；重跑16 PASS、0 FAIL/BLOCKED，签章PDF、报价、EOMS结算/库存回滚、客户H5和工作台独立聚合也通过。

扩展现有隔离性能工具的writeoff-summary场景，不增加在用API目标选项；从真实API创建80份付款核销主单，同库双后端对比v29/v30，12 CPU/8223760384字节Docker内存，Java21各-Xms256m/-Xmx768m；各预热5次、交替采样，只读并发限制1/5，各方案各档40样本。结果如下：

| 只读并发 | 每方案样本 | 基线P50/P95(ms) | 优化后P50/P95(ms) | 错误 |
|---|---|---|---|---|
| 1 | 40 | 399.20 / 547.66 | 36.95 / 50.54 | 0 |
| 5 | 40 | 450.78 / 571.71 | 56.28 / 268.56 | 0 |

160次响应均逐项验证同一批主单及金额。此为本机独立dev栈的付款汇总对照，并发5的尾延迟仍有明显波动；不是生产最大吞吐、持续容量或其他接口的时延承诺。保持每日99张编号规则，合成核销数限制80，450明细边界在H2验证，不伪造更大规模的API造数。

业务栈及性能栈均已清理，性能runId db8cb564c0精确标签回查容器0/网络0、manifest移除。证据.local/r50-finance-before.log、r50-finance-after.log、v2-build-v30-evidence.json、r50-business-attempt1.json、r50-business-results.json、writeoff-capacity-results.json、writeoff-capacity-setup.log及r50-writeoff-capacity.log。测试私有资料不提交，在用后端未替换。


### 50.3 H5分端构建

原两种mode均引用同一份10页路由表，客户包仍包含工程师登录/任务/维修页，工程师包也包含客户报修页，仅运行时守卫拒绝。现将两端页面路由各归入一个模块，由Vite按现有VITE_H5_ROLE配置构建别名#h5-routes；共同路由、404、标题、Pinia身份、GM/接口层继续共用。另一端已知路径保留回本站登录的重定向，不删除跨模块业务接口或重建认证栈。

新增4项真实路由脚本回归，核对两端各5个原页面路径、登录前置、另一端无组件/已知入口重定向、不同部署base及通用404；用真实路由meta验证另一种令牌不能放行。原实现的分端断言失败，修复后4项通过。真实Rollup模块图两端仅含本端views，另一端页面/样式模块0；Vant解析和函数式样式门禁继续通过。

| mode | 全部JS/CSS原始字节：前→后 | gzip估算字节：前→后 | Vant模块：前→后 |
|---|---|---|---|
| customer | 522128 → 429718（减少92410） | 202816 → 167405 | 118 → 118 |
| engineer | 522129 → 458025（减少64104） | 202816 → 179644 | 118 → 103 |

这些是全部产物（包括异步页面）而非首屏传输时延。进一步真实构建两端生产镜像，并在无网络临时容器核对Nginx语法、东八区、HTML/JS/CSS、SPA回退/404，以及本端Auth或Login资源存在、另一端对应资源不存在，全部通过。API联动同时覆盖客户邀请码/照片报修与工程师任务处理，不将API和构建结果冒充真实设备点击。证据.local/r50-h5-before.json、r50-h5-after.json、r50-h5-routes-before.log、r50-h5-routes-after.log、r50-h5-docker-customer.log、r50-h5-docker-engineer.log、r50-h5-image-http.json/log。


### 50.4 PC组件按需加载与动态功能保留

主入口不再安装整套Arco，模板及TSX通过已有组件解析器显式导入，继续使用平台完整定制主题；GiForm将全部25种公开Arco控件类型映射为实际组件对象，group-title仍走既有分组标题分支，不依赖全局a-*字符串。保留Modal/Drawer/Message/Notification的函数式调用、应用上下文、Upload默认name，以及MultipartUpload render回调需要的全局Tooltip。菜单/图标选择器使用后端动态图标，整套图标注册有真实调用，继续保留；Vue/Arco共同vendor分组沿用原循环依赖防护，未盲目拆散。

首轮按需构建门禁仍发现arco-vue.js保留，追到独立FileAudioModal创建第二个Vue应用并安装整套Arco。已移除该整套安装；其ModalContent由同一构建解析器预编译，独立图标注册保留，不删音频预览能力。最终真实Rollup模块图：未解析Arco组件0（除明确保留的全局Tooltip）、整套Arco安装模块0、首页Cron/Luxon0而调度页7；三种Office异步边界仍通过。没有忽略失败门禁或以主入口源码少一次use调用代替模块验证。

新增4项回归使用实际Arco/Vue对象与SSR：完整控件类型映射、无全局组件时输入/数字输入渲染、下拉/日期/开关真实值，以及函数式全局接口/弹窗上下文/动态图标。全部通过；其中SSR是组件级验证，不代表浏览器点击、弹层定位或移动设备验收。完整PC类型检查及最终真实生产构建通过（1分24秒），生成类型更新后再运行类型检查亦通过。全量Node167通过，失败/跳过0。

| 静态JS/CSS闭包 | 优化前原始字节 | 优化后原始字节 | gzip估算：前→后 |
|---|---:|---:|---|
| 首页 | 2260622 | 2136567（减少124055） | 567921 → 523423（减少44498） |
| 工作台 | 3139796 | 3015439（减少124357） | 863164 → 818710（减少44454） |

同一套统计包含CSS和静态导入，工作台含Layout；不是加载秒数或浏览器CPU指标。真实临时Nginx的7项压缩/缓存/worker MIME/SPA/缺失资源/HEAD检查通过，受检入口JS368547字节，实际gzip141026字节。仍保留既有大chunk和sm-crypto浏览器crypto外部化提示，未通过调高阈值或隐藏提示凑通过。证据.local/r50-pc-after.log（首次门禁失败）、r50-pc-final-build.log、r50-pc-module-after.json、r50-pc-assets-before.json、r50-pc-assets-after.json、r50-pc-comparison.json、r50-pc-final-types.log、r50-node-final.log及r50-pc-nginx.log。

### 50.5 联动、清理与交付范围

四项按用户指定顺序完成，并作为独立普通修复提交，产品VERSION仍为2.1.0；不移动既有标签。最终验证为733项后端、167项Node、16组真实API闭环、PC类型/生产构建及模块图、H5双模式构建/角色门禁与双生产静态镜像检查，失败/跳过均为0。性能对照启动时又在新栈重跑16组通过，再执行160次受控汇总查询。原财务夹具缺合同的失败证据和PC全量安装残留门禁失败均保留，修订后完成相应重测。

独立回查本轮3个业务/性能运行号：临时容器0、网络0、manifest移除；两份本轮H5测试镜像及静态HTTP临时容器清理，在用和现存共享服务保留。合成附件随临时后端数据目录销毁，.local中的合成PDF/日志等作为私有证据保留，不是当前业务附件。在用JAR哈希仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b，未被候选替换，真实业务数据未操作；现用PC/双H5/API仅只读冒烟9/9通过。清理证据.local/r50-cleanup.json，业务结果r50-business-results.json，构建快照v2-source-manifest-v30.json。

本轮保证的是上述受测候选与跨模块链路结果，不声明全部页面逐按钮已验收或公网生产上线完成。浏览器L3、真实摄像头/GPS、外部地图AK和外部调度器实际执行未覆盖；日编号99张规则保留，前端年份预览范围不替代外部调度器校验。既有角色权限、客户/商品/仓库关联、库存/资金事务、统一认证和平台部署关系保留。后续正式上线须使用已验证候选及适用环境配置，不能把本机受控延迟对照当作正式生产SLA。


## 51. 全仓结构与冗余审查及受控精简

### 51.1 先审查后修改的范围与基线

用户要求先把整个项目检查完再优化。本轮以23ba09f为基线，开始时工作区干净；先完成全仓静态/构建图/依赖与部署审查，期间生产源码未修改、现用业务未操作。所有2455个Git文件均纳入目录/大小/摘要扫描，2443份文本进一步参与引用分析；其余为二进制资源，不假装已做源代码分析。跟踪文件共11160647字节（含锁文件/报告），Java生产类型1107、RestController85。静态覆盖不等同逐行形式化验证、全部功能已实际点击或全端点已调用。

| 审查区域 | 范围及实际核对 | 审查结论/边界 |
|---|---|---|
| crypto | 10份Java，国密接口/实现、签章消费及已有测试 | 必需，不因类少并入业务域 |
| common | 42份Java，统一身份、业务SPI与依赖树 | 多域共用；AWS等传递依赖不可只按显式import裁剪 |
| mybatis | 10份Java，租户/数据权限映射入口 | 必需；selectMaps权限约束继续保留 |
| redis | 2份Java，缓存前缀/平台消费 | 小模块不是无用模块，保持同一缓存底座 |
| security | 7份Java，Sa-Token拦截、身份上下文清理与现有回归 | 统一鉴权保留，不另造登录栈 |
| starter/compat | 86份Java，旧类型、导出/异常/编号/脱敏与SPI引用 | 找到3份可确认无消费者旧类；大ExcelUtil仍有真实导出调用 |
| rbac | 184份Java，菜单/角色/用户、选项、字典、外部天气及日志 | 平台能力保留；外部调用已有连接/读取超时 |
| file | 52份Java，分片DAO/服务、统一sys_file存储、回收/权限 | @Repository类是扫描入口，不能按零import删除；S3旧SDK有实际第三方调用 |
| notice | 35份Java，消息/公告、未读/已读及WebSocket | 部分未读逻辑拉列表求数量，列入后续计数查询优化候选 |
| tenant | 30份Java，套餐/租户、删除钩子、菜单/缓存 | 有可配置入口，未启用不代表可删；模块暂无自有测试须如实登记 |
| schedule | 37份Java，开关条件、SnailJob调用、禁用回退 | gRPC由调度依赖带入；当前外部调度未实测，不直接剔除 |
| PSI | 359份Java，54份Mapper、库存/资金锁链、发票、导入与工作台 | 部分循环读是锁与状态验证必须保留；导入逐行商品查询/工作台Java聚合有规模增长风险 |
| quote | 25份Java，金额归一化、状态锁、明细/导出及转工单 | 现行独立报价与转工单兼容关系保留 |
| EOMS | 110份Java，报修/派单/结算/回滚、PSI库存SPI、H5附件 | 不直接跨域写库存；大服务拆分只作为后续维护性改造 |
| sign | 103份Java，项目/授权/签章/PDF/回执/文件指针 | 当前SignStorage已使用平台sys_file，不误判为第二套S3存储；PDF两套引擎各有用途 |
| boot | 15份Java及配置/迁移资源，启动依赖闭包、环境/迁移/健康入口 | 16个子模块均在boot传递闭包，无失效Maven模块；不改已执行迁移 |
| PC | 861文件，AST记录前后端工具合计2276条import，真实Vite只写内存审查 | 4120模块解析、2949模块保留、673个chunk；审查后还原生成类型，未借审查改动源码 |
| 双H5 | 47文件，两种mode路由/共享身份和真实构建门禁 | 另一端页面已不进入本端产物；单独构建不拆平台鉴权/接口 |
| SQL | 116个建表名与实体/Mapper/XML/迁移/Compose引用 | _itls_init_done用于安装健康检查，M3映射属历史迁移，CA旧表不擅自DROP |
| 部署/运维 | 根/开发Compose分离、15份shell语法、打包/备份/监控/升级 | shell语法通过不代表逻辑无误；备份假成功已隔离复现，优先修复 |
| 测试/资料 | 67份scripts、84个Java Test文件及现有报告/Agent/Skill | 用例数量不当作覆盖率；一次性live脚本与特殊签章用例未证明全被替代，暂不按名字删除 |

证据.local/r51-inventory.log、r51-static-audit.json、r51-frontend-audit.json、r51-pc-module-graph.json/log、r51-backend-method-audit.json、r51-table-reference-audit.json、r51-dependency-tree.log及r51-shell-syntax.log。结构扫描已覆盖上述区域；动态设备、外部调度、全部页面交互及正式生产容量仍不是本轮静态审查的通过项。

### 51.2 臃肿来源与确认清单

源码11.16MB与本地工作目录大小应分开：不跟随链接统计，PC依赖目录约635.8MB，H5依赖约63.5MB，.local历史候选/日志证据约2.00GB；这些被Git及生产构建context排除，不能把删除它们说成浏览器加载提速。运行JAR194981527字节，398份依赖库合计194401718字节（还包括本项目模块JAR），明显大于源码；大包不等于全部可以删。

真实模块图显示首页代码主要集中于Arco/Vue及公共工具；包归属权重是压缩前模块体，不冒充网络传输字节。Office、富文本、视频、图表等大引擎主要位于异步页面；当前基础首页仍有后续按页拆分主题/图标/公共依赖空间，但需浏览器及动态组件回归，不能盲拆已有Vue/Arco共包的循环依赖防护。

| 类别 | 确认结果 | 本轮动作依据 |
|---|---|---|
| 可精简前端 | GiCodeView生产构建未解析、没有模板/动态注册/源导入消费，仅自动生成类型自引用 | 已下线代码生成后遗留；可连同只服务该组件的6份CodeMirror直接依赖清理 |
| 可精简前端依赖 | @vueuse/components、vue-color-kit、qs、less-loader、sass-loader、vite-plugin-style-import没有实际源码/构建入口消费 | qs变量名实际引用query-string；Vite仍保留less/sass编译器，不删主题能力，共计拟移除12份直接声明 |
| 应保留工具链 | @types、@vue/tsconfig、vue-demi、less/sass/PostCSS等 | 类型、peer及构建工具不要求业务源码显式import，不能按零引用误删 |
| 可精简兼容类 | 旧SysMenu、EscapeUtil、仅被EscapeUtil调用的HTMLFilter | 全仓Java/XML/配置/测试无外部名称消费，无Spring扫描入口；新菜单走RBAC MenuDO |
| 误报/保留 | RedisMultipartUploadDaoImpl、上传digest worker、GiTable导出入口、按钮规范常量、下载工具 | 分别有@Repository、new Worker、类型/文档/测试或公共契约用途；零显式import不等于失效 |
| 完全相同文件 | 两份LICENSE、公开logo/精灵logo、file-close/file-dir SVG | 分发许可或资源名别名有用途，未发现完全相同的整份业务实现，不直接删别名 |
| 后端依赖保留 | ByteBuddy、AWS1/v2、gRPC、验证码/字体/PDF | 依赖树证实ByteBuddy为Crane4j运行依赖；x-file-storage-core仍调用AWS1，不能误删；其余有现行功能消费者 |
| 后端打包候选 | Lombok属于编译用途却进入运行包；Commons Logging与Spring JCL并存并提示冲突 | 可独立验证打包排除，不替换业务存储/扫描框架，不承诺显著降低接口延迟 |
| 查询后续重点 | 工作台对金额/数量拉明细到Java汇总、StockLineImportSupport逐行查商品、部分消息拉列表计数 | 需数据权限、金额、空集合和大样本断言后再改，不以51处粗扫描候选冒充51个已证实缺陷 |
| 维护性后续重点 | 进/销项发票等方向服务存在近似重复；签章/报价/工作台大页面 | 共用校验已有support；不能仅为减少行数合并方向状态机或增加泛化抽象 |
| 运维优先修复 | backup.sh无pipefail，mysqldump失败后gzip成功掩盖状态，并删除旧备份 | 在仅有假docker/合成文件的隔离夹具复现：退出0、报告OK、旧备份被清除；从未调用真实mysqldump |

以上为修改前的审查门槛。后续只先实施确认无用内容与备份失败保护，保留高风险/有功能用途项目；不为“最精简”删业务、账号权限、数据库表、历史迁移、真实附件或共享容器。备份复现证据.local/r51-backup-before.log；完整AWS1第三方消费证据r51-aws-v1-references.json。源码与引用审查不能替代运行时所有反射路径证明，清理后仍必须构建及联动重测。

### 51.3 审查后的第一批精简与修复

2026-11-08完成本批验证。按先审查后修改的顺序，仅处理已确认无用内容和可复现运维缺陷，未合并或删减现有业务域。

| 项目 | 实施内容 | 验证与实际收益 |
|---|---|---|
| 备份失败保护 | backup.sh启用pipefail，数据库/文件归档经临时文件、压缩验证后原子发布；失败退出且不执行旧备份清理；保留策略只匹配项目时间戳命名的普通归档 | 新增实际Bash隔离执行3例：SQL失败、文件打包失败、成功；旧有效备份保护、临时文件清理、外部归档保留均通过。仅假Docker和合成资料，不访问真实数据库或归档 |
| 兼容代码清理 | 删除无消费者SysMenu、EscapeUtil、HTMLFilter共1004行；RBAC现行菜单和其他导出/兼容支持保留 | 新候选源码快照1416文件匹配，17个reactor项目完整构建成功；733项测试，失败/错误/跳过0 |
| 后端运行包 | 排除编译用途Lombok及与Spring JCL重复的Commons Logging；编译和测试依赖未改 | JAR由194981527降至192841782字节，减少2139745字节，运行库398→396；独立候选启动未再出现Commons Logging冲突提示，ByteBuddy/S3等实际运行依赖仍在 |
| 前端失效代码 | 删除GiCodeView及生成类型引用，移除无消费者qs声明 | PC类型检查、真实模块图与生产构建通过，既有表单、上传、动态组件及预览能力保留 |
| 前端依赖 | 删除12项无消费者直接依赖；锁文件仅修剪不可达节点，不升级其余版本 | 锁文件包及snapshot各1090→1007，删除83项；保留项版本、完整元数据和依赖快照逐项结构比较一致。pnpm9.15.4离线frozen-lockfile校验通过 |

后端候选为.local/backend-v2-v31/ls-aegis-server.jar，SHA256 b4a5e8ac9b7da59a86c24a2174dbd92dc067988078be48f39688b375999c1ec7。完整证据v2-build-v31-evidence.json、v2-source-manifest-v31.json、v2-full-build-v31.log及r51-jar-comparison.json。旧候选保留作对照，不覆盖已验证快照。

PC在全新Docker构建阶段使用固定pnpm9.15.4及frozen-lockfile，实际安装959个适用平台依赖（不是锁文件总节点数），安装1分42.1秒，随后类型检查与生产构建通过；宿主机旧node_modules不参与。这是防止“本机残留被删依赖仍让构建成功”的独立验证，不把单次构建耗时当作提速对照。基于该生产镜像再做7项HTTP检查：Nginx语法、HTML/安全头、JS真实gzip/缓存、CSS、SPA回退、缺失资源404、HEAD/ETag，全部通过。

宿主生产dist另经7项实际Nginx检查，含PDF worker MIME，全部通过；入口JS368547字节，实际gzip141026字节，与上轮相同。首页Cron/Luxon0、调度页7，未解析Arco组件0（保留全局Tooltip），整套Arco安装0，Office三种异步入口门禁通过。被删组件原本就不在生产依赖图内，因此本批不宣称首页传输变小或页面响应变快。宿主node_modules未物理裁剪，不声称已释放对应磁盘空间。

双H5重新构建及角色隔离/Vant函数式样式门禁通过：客户全部JS/CSS429718字节、gzip估算167405，工程师458025/179644，与上轮一致。全量Node测试170通过、失败/跳过0（原167加备份3项）。备份测试需要Docker及指定已缓存镜像，缺环境时会明确skip；本轮没有skip。证据r51-backup-after.log、r51-node-tests.log、r51-pc-build.log、r51-pc-assets.json、r51-pc-clean-docker.log、r51-pc-image-http.json、r51-nginx-results.json、r51-h5-audit.json及r51-lock-verification.json。

本机npm全局registry继承值为registry.npmmirror.co且本轮访问失败，另有可用pnpm11与项目锁文件工具不匹配；未修改用户全局配置。仅显式从官方registry取得固定pnpm9.15.4，在独立夹具生成/验证锁文件后用干净生产镜像复验，保留失败日志r51-lock-refresh.log及成功日志r51-lock-frozen.log。没有顺带升级项目依赖。

### 51.4 联动、清理及下一阶段边界

新后端候选在独立MySQL/Redis/后端栈完成16组真实API闭环，16 PASS / 0 FAIL / 0 BLOCKED：统一鉴权与多身份隔离、采购/销售/库存/往来、报价独立确认及转工单幂等、银行流水、维修结算与出库回滚、PDF签署/并发/密码验真、客户H5照片报修、部分/重复核销后的应收应付汇总，以及工作台20项汇总关联。结果为r51-business-results.json，runId 05d041c6eb；使用合成身份及数据，不把接口结果当作真实浏览器点击或外部CA验收。

该业务栈已精确按运行号清理，独立回查临时容器0、网络0、business-env.json已移除；本轮静态HTTP容器和PC验证镜像已删除，宿主备份/锁文件临时夹具已清理。私有.local构建产物、复现日志和合成证据保留且不提交；共享构建容器中的/tmp/r51-backup-probe、/tmp/r51-shell-syntax也是合成审查副本，其容器在本轮末已停止，未为删除这两份副本而启动并重跑该共享容器的旧入口任务。未清理共享镜像、Docker数据卷、其他轮次容器或真实附件。

03:02左右（东八区）只读冒烟首次3/9，API短暂连接中断；随后读取到在用后端/MySQL/Redis启动时间均为2026-11-07T19:02:56Z，先前共享构建容器同时停止、OOM标志为false。未据此断言中断原因，也未重启现用服务。环境恢复后同一脚本9/9通过，首次失败与重测日志分别保留r51-live-smoke.log、r51-live-smoke-recheck.log。在用JAR哈希仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b，未部署本轮候选、未操作真实业务数据。

剩余优化按风险排序登记，不能把“全仓已扫描”写成“全部业务状态已穷举”：

1. 工作台金额/数量聚合迁移到专用SQL之前，先为部门/本人/租户权限、空集合、金额精度和大明细建立对照；不能直接使用缺少数据权限注解的selectMaps替换现有查询。
2. 商品导入批量预取与消息未读计数，分别验证重复编码、无效商品、可见范围及分页语义，再减少逐行查询/整表传输。
3. 大页面按现有表单/列表职责拆分，财务方向服务仅共享稳定校验；保留各自状态和事务，不为缩短文件合并不同业务流程。
4. 公共主题/动态图标、Office/PDF/富文本引擎进一步收敛，需要浏览器动态组件及预览格式矩阵；可选后端依赖按部署能力配置裁剪前，必须明确关闭的功能和联动影响。

本批为普通维护，版本保持2.1.0，现有标签不移动。浏览器交互、摄像头、GPS、地图AK、外部调度器实际执行及正式生产容量/SLA不在本轮通过项内。此次收益是可验证的依赖/失效代码减少和备份可靠性改进；尚未量化新一轮接口延迟、吞吐或启动速度收益。


## 52. 工作台聚合、商品导入与消息计数优化

### 52.1 基线及工作台受权限约束的SQL聚合

2026-11-08按第51节优先级继续，基线32d52be、开始时工作区干净。沿用项目itls-optimize、itls-code-review及itls-business-test；先核对现行查询/实体/Mapper/接收范围，再按工作台、商品导入、消息顺序修改。版本保持2.1.0，未变更库表、迁移、统一身份、前端响应或部署入口。

原工作台把应收/应付、收款草稿、未匹配银行流水以及两个时间窗口的出入库单据/数量投影成实体列表，在Java求和。新增固定表的PsiWorkbenchMapper和内部WorkbenchAggregate投影，数据权限显式绑定单据头h；所有查询同时限制当前租户与逻辑删除。未使用缺少权限注解的selectMaps，也未把全部用户的可见数据预先缓存。

应收/应付继续仅统计OPEN且未核销金额大于0，草稿收款和未匹配流水保留原状态；COUNT包括金额为NULL的单据，SUM忽略NULL并归零，银行负金额不被改成绝对值。待办数量也显式经过数据权限注解。入口提前校验登录和完整权限上下文，异常身份在任何汇总SQL之前拒绝。

出入库分别用一次单据头聚合同时计算本月和近30天笔数/金额，另一次关联明细聚合数量；数量权限沿单据头，同租户关联且头/明细各自排除逻辑删除。头金额不参与一对多数量联表，避免多明细把金额重复相加。移除向Java传输所有头/明细及动态大IN列表；没有对应头时跳过该方向数量查询，空工作台不额外查询明细。库存预警的商品/仓库权限、上限和最近8条待办维持原流程。

| 场景 | 原查询/传输 | 本轮结果 |
|---|---|---|
| 四类资金汇总 | 4次查询，返回全部匹配金额行 | 仍4次查询，各只返回1个聚合投影 |
| 两方向均有单据，月/30天数量 | 2份头列表、4份数量列表，并构造头ID的IN条件 | 2份头聚合、2份数量聚合；不向应用传输逐单/逐行数据 |
| 空出入库窗口 | 跳过数量列表 | 保持跳过数量聚合，所有数量为0 |
| 2万行应收/两方向各2万条明细 | 随行数增长的应用对象与传输 | 实际H2分别执行大样本，资金/头/数量各1份投影，计数和精确金额/数量正确，头金额未被重复累计 |

真实MyBatis MapperProxy、平台DefaultDataPermissionHandler与用户提供器、租户拦截器及SQL解析器共同执行回归，覆盖部门、本人、自定义、多个角色OR、全数据、跨租户、逻辑删除及无匹配；工作台查询禁止缓存旧结果的配置同时经同事务内数据更新重读验证。确定日期为2026-11-05、2026-10-31、2026-03-01，覆盖月/30天重叠、31天月首、跨月窗口、今天结束和明天排除。没有把源码注解存在当成权限实际生效的唯一证据。

### 52.2 商品导入与数据库匹配语义

原StockLineImportSupport每个有效行执行一次商品查询。现收集去重后的原始编号，每批最多200个，通过GoodsMapper.selectImportGoods返回最小字段投影；401行的实际Excel解析回归验证仅3批查询。匹配由数据库连接导入编号派生表完成，返回原导入编号importCode与规范商品编号，不在Java自行小写或去重音来猜测MySQL排序规则。新查询显式限制租户、逻辑删除，并在商品别名g上复用平台数据权限。

保留行顺序、空白行处理、原始重复编号校验、缺商品、数量必须大于0、单价/税率非负、默认税率及缺省13的规则；数量小数与千分位解析保持原实现。文件流以try-with-resources关闭。真实Excel读写单测覆盖401行、200行批次上限、编号空白、重复/不存在、非法数量/负价格/负税率、税率默认和损坏文件。真实SQL另覆盖大小写匹配、部门/租户/删除过滤、空编号集合，以及同一事务里切换数据范围不串用结果；后者实际通过，未虚构缓存泄漏缺陷。

MySQL真实API新增PSI-IMPORT-BATCH-01：入库/出库两条解析入口上传合成XLSX，使用小写编号匹配规范编号，核对商品ID、1.2345数量和2.50单价；重复编号、不存在商品、0数量均拒绝。夹具生成只用Python标准库，不新增Excel测试依赖，不写实际出入库单据。

### 52.3 消息与公告未读数量

消息分类详情原为每个已知类型一次COUNT（当前两类共2次），现一次GROUP BY返回分类投影，按原枚举顺序补零；仅总数仍走一次COUNT。内部类型使用原始整数，历史未知类型不会使已知分类反序列化失败；详细总数继续只累加已知分类，与原实现保持一致。

标记已读前仅查询可见未读ID，指定消息批次时用IN限制该批，不再下载整份消息内容；全部已读仍只读取需要写日志的ID。标记后的WebSocket数量使用COUNT，移除再次加载全部未读消息求size的做法。保留接收者绑定、全员/指定用户、已读时间和个人删除墓碑过滤；ID有序读取，原个人日志幂等写入不变。

还发现/user/message/notice/unread原本拉取全部公告ID后求size，已改为COUNT，与原ID列表共享发布状态、接收范围及已读过滤片段；按通知方式弹窗的ID列表功能继续保留。消息查询的MySQL及PostgreSQL分支均参与Mapper解析检查，公告计数则通过本轮MySQL真实API验证；PostgreSQL未启动实库，不声称已完成该数据库联动。

新增NOTICE-COUNT-01在独立MySQL通过真实API验证：两种身份的全员/指定消息、分类详情和总数、已删除墓碑、夹杂另一接收者ID的标记、重复已读、个人删除、全部已读后为0且另一身份未读不变；公告验证全员/本人/他人及未发布范围，并在阅读后重查数量。数据仅写入带运行号的独立测试栈，不向真实用户发送通知。

### 52.4 依赖复查与构建

重新扫描PC/H5/工具共2278条import及动态入口，并对照第51节依赖树和第三方运行消费者。剩余零显式import候选为vue-demi、类型包、ESLint、Less/Sass、lint-staged等：分别有peer、类型、构建器或package hooks用途，不按源码零import删除。后端Maven依赖声明未变，实际SDK/自动装配用途仍保留；不再裁掉ByteBuddy、AWS/S3、调度RPC或签章/Excel/PDF能力。新旧候选运行库清单均396份，其中381份第三方JAR逐份内容哈希一致，证据r52-runtime-dependencies.json；本轮后端变化为汇总逻辑，不宣称继续减少运行依赖。

H5未配置或引用Autoprefixer，原直接声明的postcss也由Vite带入。移除这2份直接声明后，锁文件非根条目180→170，删除Autoprefixer及9个不可达传递包；PostCSS作为Vite实际依赖保留。保留锁条目的完整元数据/版本逐项比较不变，没有依赖升级。独立夹具使用离线package-lock-only生成后，两份生产镜像均以npm ci重新安装（119个适用平台包）并构建，排除宿主旧node_modules掩盖缺依赖的情况。

双模式真实模块图继续通过Vant组件/函数式样式及角色边界：客户429718字节、gzip估算167405，工程师458025/179644，与前轮一致。这是安装依赖精简，不能宣称删除本来不打入产物的包会降低页面加载时间。双镜像各完成Nginx语法、东八区、HTML/JS/CSS、角色资源边界、SPA回退和缺失资源404检查。证据r52-frontend-audit.json、r52-dependency-review.json、r52-h5-lock.log、r52-h5-audit.json、r52-h5-customer-docker.log、r52-h5-engineer-docker.log及r52-h5-image-http.json；宿主依赖目录未物理裁剪。

### 52.5 最终验证、失败记录与清理

最终候选.local/backend-v2-v34/ls-aegis-server.jar，SHA256 84bd011ed6a262a252f9ae3085a1c2b90eaa04832c2fe7934853cac5205dd403，192846569字节，源码快照1422文件一致。JDK21运行/Java17 release，17个reactor项目完整构建成功，745项后端测试，失败/错误/跳过0；全量Node170通过、失败/跳过0，版本11处一致。已保留v2-build-v34-evidence.json、v2-source-manifest-v34.json、v2-surefire-v34及完整构建日志。

独立候选完成18组真实API闭环，18 PASS / 0 FAIL / 0 BLOCKED，含原16组统一鉴权、库存资金、报价、EOMS结算/回滚、签章密码验真和H5报修，以及新导入/消息两组。工作台20个摘要字段仍以独立MySQL查询逐项核对，不仅验证HTTP成功。结果r52-business-results.json，运行号a14d745157。这些是受测逻辑与隔离闭环，不是浏览器逐按钮、正式生产容量或全部业务状态的穷举证明。

如实保留中途失败：无网络构建容器缺少可解析本机地址，先影响文件模块初始化、后影响TraceId日志测试；只为专用构建容器接入内部网络并修正其主机解析后恢复，未改在用网络或跳过测试。新增夹具最初也有SQL占位符数量及校验器上下文初始化问题，均修正后重测；商品派生表的未类型化参数在H2无法推断，改用不截断编号的CONCAT表达式，并通过真实MySQL上传复验。早期完整/定向失败日志与最终成功记录均保留，不把失败次数算作生产缺陷个数。

业务栈按运行号清理后独立核对容器0、网络0、business-env.json移除；本轮专用构建容器/内部网络、双H5验证镜像/HTTP临时容器及锁文件夹具均按名称与标签核对清理。构建/用例日志和合成证据保留在忽略的.local，不进入Git。在用JAR仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b，未被候选覆盖；真实业务数据、账号权限、配置和附件未操作。最终在用PC/两端H5/API只读冒烟9/9，清理证据r52-final-cleanup.json。

本批可确认的收益是应用侧汇总数据量有界、减少重叠数量查询、401行导入查询401→3、分类消息计数2→1及去掉多余列表传输，另精简H5安装依赖；没有同环境延迟/吞吐对照，不承诺P95或正式SLA提升幅度。浏览器、摄像头、GPS、地图AK及外部调度执行继续未覆盖。下一阶段仍为大页面职责拆分、预览/公共依赖边界及部署能力配置，需按相应功能矩阵继续验证，不能直接删除现有业务。


## 53. 报价拆分、预览会话与公共加载优化

### 53.1 本批边界及大页面职责

2026-11-09从1847980干净工作区开始，按第52节下一阶段处理大页、文件预览和公共依赖。先执行项目盘点及真实PC类型/生产构建/模块图与资源门禁，建立本轮独立基线，再实施改动；沿用itls-optimize、itls-code-review与itls-maintain。没有改后端、H5、SQL、依赖版本、权限配置或产品版本（仍为2.1.0）。

报价原form.vue共1231行，将材料/服务两套近似表格合并为QuoteLines组件，稳定行uid、原行对象、材料选择/新增商品、服务录入、单位、数量单价及小计、锁定/未就绪禁用全部保留；移除通过事件交由父页执行，不在子组件另造报价状态机。QuoteLine定义移到相邻类型文件。保存、提交/确认/取消、路由详情时序、权限指令及导出API继续由原父页控制，未挪到无权限的独立入口。

导出预览拆为QuotePreviewPane，独立持有180ms输入防抖、2.5秒加载兜底和卸载清理；iframe按内容换实例，旧帧事件不能停止当前加载。原边框、表格字号/列宽、行样式、40/60双栏和窄屏预览保留；子预览的复合选择器避免父级通用padding覆盖12px预览间距。父页降至905行（LF口径42508→31250字节），这是维护边界改善，不把父文件变小当作整个功能代码或加载时间同比下降。

签章入口另将既有SignPdfPlacer改为异步组件：未选PDF时不加载定位画布/PDF.js；仍使用原单页、批量、骑缝定位和document-ready/load-error事件、页码/坐标/任务模型。引擎chunk等待有Spin，加载失败复用原PDF错误处理，清理活动文件并提示重新选择重试；项目已切换且没有活动PDF时不清除新的待加载选择。未重写签署流程或更换PDF引擎。

本批未拆驾驶舱42KB页面本体或签章任务编辑区的全部逻辑；优先把确认独立的报价职责与签章重引擎边界落地，后续再按角色交互矩阵拆分其余大页，避免一次重写整个底座。

### 53.2 预览会话、下载鉴权与资源释放

FilePreview新增递增会话标识：onLoading返回当前标识，关闭、替换和卸载会使旧标识失效；onPreview可绑定该标识，旧下载不能重新打开已关闭弹窗。每次渲染绑定本次回调并使用新key，旧rendered/error不能结束新文件的loading或报错。替换文件时释放本组件创建的新标签Blob URL，关闭与卸载继续释放资源。弹窗按受控visible/cancel同步关闭，不再用延迟close事件重复清理，避免旧动画结束干扰新会话。

PlatformFileField对Office/图片/文本/下载分支增加请求代次，旧响应/错误不覆盖新选择，旧finally不清除新请求图标；PDF的arrayBuffer转换也受当前请求约束。关闭预览和卸载会使挂起请求失效，预览转换有await，避免异步异常游离。组件内部主动关闭Office以展示图片/文本时不发出用户关闭通知，防止自行使当前文本请求失效。

文件管理的Office/PDF原来直接把存储URL交给引擎，现复用downloadFile(fileId)鉴权流接口，PDF用ArrayBuffer，其余Office用Blob，原Excel选项与转换钩子保留；错误只反馈当前有效会话，失效的下载不再重开或关闭新文件。长文件ID保持字符串，不改成Number。图片/音频/视频入口及后端文件权限未改。

原附件组件对所有application/json Blob再次一概拒绝，与平台http拦截器重复且会误拦合法JSON文件；已去掉该局部判断，统一使用现有拦截器区分平台R失败与真实文件。既有HTTP Blob回归继续执行，未旁路Authorization、租户头或服务端文件授权。

三套Office薄包装及对应CSS继续按类型加载，PDF worker资源保留；没有把Word/Excel换成无法验收的另一引擎，也不把API或SSR当作真实Office浏览器渲染。

### 53.3 公共依赖与真实构建结果

生产菜单、路由、页签、面包屑、树表及相关管理页不再从xe-utils的CommonJS总入口引入，改用原库browse/mapTree/eachTree/toTreeArray/findTree/groupBy具体模块；同一实现对象的等价性回归通过。开发mock继续可使用原入口。包本身仍有真实用途，未从依赖声明删除；未改Vue/Arco共包防循环依赖、平台主题或后端动态图标注册。

真实Rollup门禁新增总入口检测，最终生产xe-utils/index.js保留模块0；未解析Arco组件0（保留约定Tooltip），整套Arco安装入口0；首页Cron/Luxon0、调度页7。签章页面静态闭包PDF.js模块0，而异步定位组件保留PDF引擎/worker；Office三种异步入口门禁继续通过。受控eager-preview基线脚本同步保留新增会话声明，避免其转换误删状态。

| 静态JS/CSS闭包（含页面Layout） | 原始字节：前→后 | gzip估算字节：前→后 |
|---|---|---|
| index.html | 2136567 → 2102544 | 523423 → 508464 |
| src/views/dashboard/analysis/index.vue | 3015439 → 2981828 | 818710 → 804086 |
| src/views/quote/form.vue | 2321574 → 2288090 | 588826 → 575972 |
| src/views/sign/execute/index.vue | 2692046 → 2271156 | 695542 → 565592 |
| src/views/system/file/index.vue | 3453210 → 3420541 | 953520 → 939238 |

首页gzip减少14959字节，签章入口减少129950字节；签章选中PDF后仍需下载相应异步资源，不能把这部分描述为永久省去。以上为相同配置下manifest静态导入闭包，包含CSS，不包含异步资源；不是页面秒数、CPU、内存或P95。大chunk及sm-crypto浏览器crypto外部化提示仍如实保留，未靠隐藏警告或提高阈值通过。

证据r53-pc-before.log、r53-assets-before.json、r53-pc-delivery.log、r53-assets-final.json、r53-module-final.json。最终类型检查及真实生产构建通过；构建前后863份前端输入哈希一致（r53-source-manifest.json），没有用源码在构建中途变化的混合产物交付。自动生成类型同步，版本11处一致。

### 53.4 回归结果与环境阻塞

新增11项回归全部通过：预览旧渲染事件隔离/关闭/Blob URL释放、附件A/B下载乱序和旧权限错误、文件管理长ID鉴权下载及关闭后响应、工具子模块实现等价、报价服务与锁定材料列插槽/控件/小计、预览防抖/旧帧/卸载定时器，以及签章引擎懒加载与chunk失败反馈。报价列测试使用真实Arco输入/选择/数字/按钮和表格列插槽契约；Arco Table本体依赖mounted，SSR以该契约支架调用列插槽，不能称作真实浏览器表格布局验收。

既有报价A→B详情/权限失败/状态弹窗/导出时序、菜单/页签恢复、签章选择/提交/验真、表单映射及三种预览包装透传等一起执行。全量Node共181项，178 PASS / 0 FAIL / 3 SKIP；跳过的3项是需要可用Docker的备份隔离执行用例，不能将其计入通过。证据r53-node-final-2.log、r53-preview-final.log；测试适配器按新的具体模块导入取同一真实函数，未削弱菜单语义断言。

BLOCKED：Docker初始未运行，定位到当前安装路径后尝试启动；日志明确WSL无法挂载D:\DockerDesktopWSL\main\ext4.vhdx，错误Wsl/Service/CreateInstance/MountDisk/HCS/E_ACCESSDENIED。检查文件/目录权限和WSL服务状态，定向终止Docker发行版重试；确认没有活动WSL发行版后再做一次非破坏性WSL关闭/启动，仍失败。没有提升权限、修改数据盘ACL、重置/注销发行版、删除VHDX或更换Docker数据目录。已停止本轮启动的Docker，恢复其最初未运行状态。证据r53-docker-restart.log、r53-docker-recovery.json、r53-docker-stop.log。

因此本轮干净容器PC镜像构建、Nginx实际HTTP检查、18组候选API联动和备份3例未执行；也没有把第52节745项后端/18组联动旧结果冒充本轮重测。在用PC/两端H5的HTML可访问，但API8000不可连接，代理返回非JSON，只读冒烟3/9（r53-live-smoke.log），不是全部服务正常。浏览器L3及真实DOC/DOCX/XLS/XLSX/PDF渲染矩阵、摄像头/GPS/地图AK、外部调度和生产容量仍未验证。

### 53.5 清理与交付边界

本轮未启动业务造数脚本、未创建测试数据库/容器/网络、未连接或写入真实业务；business-env.json不存在。组件测试只使用内存里的合成Blob/字符串和假下载结果，无业务单据或附件需撤销。Docker不可用，不能对其旧容器清单声称完成守护进程侧回查；历史共享容器/卷未动。本地日志、哈希和构建证据保存在忽略的.local，产物dist不进入Git。

在用JAR哈希仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b，未覆盖/部署候选，未修改真实业务数据、账号、权限、配置、证书或附件。本批按普通代码优化提交，版本和既有标签不变；代码/类型/构建/已执行回归通过，但不能声明跨模块API复测或生产交付验收完成。恢复Docker挂载权限后，须补执行同版本干净镜像/Nginx检查、18组业务闭环及上述3项备份用例，再更新本节阻塞项；浏览器功能矩阵也应单独验收。

## 54. conservative清理规范与阶段1基线

### 54.1 项目规则与配套Skill

2026-11-09按用户本轮要求，先整理项目专用代码清理Agent和Skill集，全部加载后只执行阶段1。准备前Git工作区干净，HEAD ee9c2bc2da4ecb12d8148ea3e16753f627ef66fe，分支main、产品版本2.1.0。现有源码与后端/前端/数据库契约不做改动。

新增agents/code-cleanup/AGENTS.md，并在根AGENTS.md增加任务路由。模式固定conservative，以最前面的范围限制覆盖后续较宽泛的Skill目的：只允许有证据的无用import、普通过时注释、无副作用且无引用的私有/局部变量及固定死分支。类、方法、组件、模块、依赖、配置、资源、脚本和对外字段不删除/移动；私有方法也只列人工复核，配置归档与依赖卸载均不执行。权限/事务/幂等、基座、SM2/SM3/SM4、GB28181、任务/队列/RPC和对外接口一律高风险，禁止自动清理；零字面引用不代替动态消费证据。

| Skill | 本项目流水线边界 |
|---|---|
| java-unused-scan | 结合可用IDEA/Sonar/PMD与Java/XML/反射/框架证据；jdeprscan仅识别废弃API，不冒充未使用类分析；高风险只标记 |
| vue-unused-scan | 现有ESLint及TS/Vue/真实Vite加载图；保留动态路由、字符串组件、权限、worker、CSS与模块副作用，不删props/组件 |
| config-resource-clean | 核对配置绑定、profile、Docker/Compose、脚本、public和资源消费者；conservative只出方案，不归档、不删除 |
| dead-branch-scan | 记录常量来源/全部赋值/控制流证明；配置、权限、运维、灰度和构建mode不当作固定死分支 |
| dependency-clean | 核对编译/运行/peer/types/工具/第三方消费；排除自动装配、Agent/序列化/加密通信，本模式不改pom/package/锁文件 |
| validate-and-rollback | 每批preimage/postimage/hash/限定patch；编译、单测、候选启动与适用联动门禁，失败或关键验证不可执行则精确回滚本批 |
| doc-comment-clean | 只清理有事实证据的普通过时说明；保留许可、安全、协议、国密、工具指令和对外契约说明 |

7个skills/<name>/SKILL.md均已逐个读取并通过skill-creator的quick_validate格式检查。它们是根目录项目级工作流，通过AGENTS显式路由加载，不宣称已全局安装或已执行全部扫描。新总调度Agent已实际启动，仅执行阶段1后停止；未继续扫描或批量修复。

三个清单分类统一为CONFIRMED_REMOVABLE、MANUAL_REVIEW、PROHIBITED_AUTO，配LOW/MEDIUM/HIGH；每项CSV含运行号/ID、Skill、位置/符号、证据、建议/实际动作、前后哈希和验证状态。阶段4先生成方案，阶段5再对白名单低风险项修改。仅工具警告或全文零匹配不能进入自动清理。

回滚限制为本批白名单文件，写回前须匹配postimage，避免覆盖并行修改；禁止reset --hard、git clean或全仓还原。并行冲突需停止覆盖并记录，不能虚报回滚成功。回滚策略已定义，本轮没有业务清理批次，未做故障注入或业务回滚演练。

### 54.2 阶段1实际证据

运行号R54-20261109T022329Z-598160f7。新增只读采集入口scripts/quality/cleanup_baseline.py，已通过Python语法检查并由清理Agent实际执行。证据位于忽略目录.local/conservative-cleanup/R54-20261109T022329Z-598160f7/：baseline.json、tracked-sha256.json、service-smoke.log、phase1-entrypoints.json及service-container-inventory.txt。记录2456份跟踪文件SHA256，7份环境模板仅登记、不读取内容；7个Skill分别记录加载哈希及流水线NOT_RUN_PHASE1。

已核对16个Maven子模块（含父reactor共17项）、根VERSION、PC typecheck/build/lint、双H5 build:customer/build:engineer、后端compile/test/package入口、docker/dev-run-server.ps1和既有Node/API回归入口。这里只锁定命令和适配要求，不代表已运行编译或测试。

| 环境项 | 阶段1实际结果 | 判断边界 |
|---|---|---|
| Java/Javac | 当前PATH为17.0.20.1 | 项目约定JDK21运行；不能因release17就宣称运行要求已满足，也不据此断言整机没有JDK21 |
| Maven | 3.9.16，版本探测成功 | compile/test/package未运行 |
| Node/npm | 24.20.0 / 11.19.0 | 已记录当前可执行版本，未重装依赖 |
| pnpm | 当前PATH为11.25.0 | 项目锁文件工作流为pnpm9；不使用pnpm11改写锁文件 |
| jdeprscan | 输出17.0.20.1、版本命令退出1 | 版本探测非零，不冒充已执行扫描或认定工具完全不可用 |
| PMD/Sonar CLI | 当前PATH不可发现 | 不等于整机未安装，未安装插件或下载新工具补齐 |
| Docker | Server29.9.2只读探测成功 | 这是本轮新事实，不沿用第53节的Docker不可用结论；未启动/修复Docker或修改数据盘 |
| 服务清单 | MySQL/Redis healthy、media运行，匹配清单未见后端 | 只读容器观察，不自动启动/删除或重建服务 |
| 只读服务冒烟 | 3/9，PC/双H5 HTML可访问，API8000不可连接，代理非JSON | startupBaseline FAIL，不等于本批候选曾启动或健康通过 |

最终门禁BLOCKED_FOR_CLEANUP。缺少可用API启动基线，加上JDK/pnpm适配缺口，禁止进入阶段5自动清理。后续静态扫描仍须按阶段顺序及用户指定范围执行，不能跳过分类证据直接修复。

### 54.3 当前范围、清单与未执行项

阶段1已完成环境/基线记录，但启动门禁未通过；阶段2至7的清理流水线为NOT_RUN。未生成实际无用代码候选，不把“尚未扫描”写成“无问题”或“零风险”。Java/Vue/配置/分支/依赖/注释CSV会在对应扫描阶段生成，本轮只定义了字段与分类契约。

本轮工程变更仅是Agent、7个Skill、阶段1采集脚本、根任务路由及本报告；逐文件设置变更清单保存在本运行号目录的setup-changes.csv。该清单是工作流搭建记录，不是“确认可删”候选清单。ls-aegis-server、ls-aegis-web、ls-aegis-h5相对HEAD差异为空，业务代码未改、无类/组件/依赖/配置被删除，无埋点、无数据写入、无清理批次，因此回滚为NOT_APPLICABLE_NO_CLEANUP_BATCH。

未运行应用编译、单元测试、候选启动或业务联动，不引用前轮通过结果替代本轮验证；7项Skill格式验证与采集脚本执行只证明工作流可读取及阶段1可采集，不能称自动删除/回滚流程已经验收。准备文件保留在工作区，本轮没有提交、推送或升版。

## 55. JDK21与pnpm9适配、服务恢复及阶段2静态清单

### 55.1 本轮范围及新的启动基线

2026-11-09按用户要求继续执行：先适配JDK21/pnpm9、恢复8000并复核基线，再进行阶段2全仓静态盘点。沿用conservative及已加载的7个Skill。本节新结果更新第54节的工具链/启动阻塞状态，保留旧失败证据；没有进入阶段3埋点、阶段5自动清理或完整业务验收。

项目本地使用Microsoft OpenJDK21.0.13.0.2+1；ZIP及SHA256来自[Microsoft官方OpenJDK下载页](https://learn.microsoft.com/en-us/java/openjdk/download)，实际校验值c5706a9cf482ef1a0622a86eea5b13d557b07f04554572af061e2d02b58c0f99。pnpm复用本机缓存9.15.4，没有通过pnpm11重装依赖或改锁文件。新增scripts/quality/use-cleanup-toolchain.ps1，运行`. ./scripts/quality/use-cleanup-toolchain.ps1`仅改变当前PowerShell进程；JDK目录、pnpm指针及shim位于忽略目录.local/toolchains。未改系统/用户全局环境，其他机器仍需准备自己的本地工具链，不能把本机绝对路径当成通用安装器。

Docker Server29.9.2可用；使用eclipse-temurin:21-jre-jammy（运行时21.0.13）恢复ls-aegis-mcs-backend，监听127.0.0.1:8000。候选v34 JAR SHA256为84bd011ed6a262a252f9ae3085a1c2b90eaa04832c2fe7934853cac5205dd403，1422份当前后端源输入与该候选源清单一致。忽略目录.local/compose.backend.r54.yml仅将既有Compose的JAR挂载改指向该候选，沿用原环境文件、数据/字体挂载及网络；原target JAR未覆盖，哈希仍为a5a2664c088a4f72da8997d32e357dbfefef4b58ba9a2fc86ffd66c0cd9ab26b。MySQL/Redis/media保留，未重置密钥或业务库，后端保持运行。

真实readiness为/auth/gm/public-key返回success=true/code=0，启动约29.45秒。/actuator/health虽HTTP200，平台返回404/success=false，未配置，**不计为健康通过**。初次恢复及阶段2结束的只读服务冒烟均9/9：API、公钥/拒绝未登录访问、PC与双H5 HTML和代理可访问；不代表登录后全模块联动已重测。恢复证据为.local/r54-backend-restoration.json、r54-backend-source-proof.json、r54-backend-ready.json及r54-backend-smoke.log。

新基线运行号**R54-20261109T025023Z-7466b12c**，证据统一位于.local/conservative-cleanup/该运行号/。HEAD保持ee9c2bc2da4ecb12d8148ea3e16753f627ef66fe，main，产品2.1.0。记录2456份跟踪文件SHA256，7份环境模板不读取内容；工具为Java/Javac21、Maven3.9.16、pnpm9.15.4、Node24.20.0。原始baseline.json门禁为BASELINE_RECORDED_REQUIRES_REVIEW，复核后另写phase1-gate.json为REVIEWED_READY_FOR_PHASE2_SCAN，没有篡改原始基线或授权阶段5。

### 55.2 阶段2实际执行与覆盖限制

按Java→Vue→配置资源→固定分支→依赖→注释顺序执行。新增只读采集/汇总脚本scripts/quality/conservative_vue_scan.mjs、conservative_vite_scan.mjs、conservative_branch_scan.mjs、conservative_inventory.py；不含自动删除或自动修复命令。所有2463份Git跟踪文件进入coverage-inventory.json，逐项区分工具配置输入、词法索引、二进制元数据或敏感内容排除；这不等于2463份文件均经过完整语义分析。缓存、node_modules、dist/target及真实数据卷不作为清理源。

| Skill | 本轮实际证据 | 必须保留的限制 |
|---|---|---|
| java-unused-scan | 复用缓存maven-pmd-plugin3.28.0，日志确认PMD7.17.0；299项规则候选，处理/配置错误均0 | 配置输入恰为1195份跟踪Java，XML仅列100份有警告文件；没有零警告逐文件处理清单，不能声称1195份逐一处理已证明。未配置完整应用/测试auxclasspath，类型解析不完整 |
| vue-unused-scan | ESLint9.3.0对490份PC/H5脚本/Vue采集8条相关诊断；Vite隔离生产默认模式构建通过，4135个模块、2964个有渲染代码模块 | H5借用PC解析器虚拟路径，仅作词法诊断，非H5自身完整lint验收；1份ESLint解析失败。Vite未复制环境文件、write=false，不是实际部署产物，也不能凭未进入图判断无用 |
| config-resource-clean | 598份配置/资源/脚本入口盘点；记录文件名字面消费者、约定入口、动态资源和保护边界 | 无同名引用仅为候选；Spring/profile/MyBatis/Liquibase、glob/public URL、Compose/Docker/脚本和外部部署可能消费。没有证明任何文件可删除 |
| dead-branch-scan | TypeScript AST及Vue SFC解析覆盖500份跟踪前端文件，解析错误0、字面布尔分支0；另登记9处环境/权限等词法复核点 | 没有执行全局常量传播、完整Java控制流或全部运行环境证明；9项不是已证明的死分支，均保留 |
| dependency-clean | 207项Maven/npm声明/管理项盘点；前端结合AST import、文本消费和隔离PC Vite图 | Maven是声明盘点，未执行完整resolved dependency usage分析；CLI/peer/types、样式、自动装配、SPI与第三方间接消费未穷尽。**确认无用依赖0项**，不改manifest/锁文件 |
| doc-comment-clean | 登记5处高风险区域TODO/FIXME等词法候选 | 标记存在不证明过时；没有逐条历史实现/全部消费者证明，确认过时注释0项，不删除安全/协议/工具指令 |

Java使用UnnecessaryImport、UnusedAssignment、UnusedFormalParameter、UnusedLocalVariable、UnusedPrivateField、UnusedPrivateMethod及UnconditionalIfStatement规则。最初独立PMD classpath离线尝试缺commons-codec1.15，随后使用已缓存的正式Maven插件执行成功；没有手工用其他codec版本替换、安装/升级项目依赖或把失败尝试当扫描成功。插件依赖描述仅是包内元数据，不冒充实际加载classpath探针。

ESLint解析缺口为ls-aegis-web/src/views/dashboard/analysis/index.vue:367的泛型箭头函数：Identifier expected。同文件由Vue SFC/TypeScript独立解析通过，且Vite图构建、随后PC类型检查通过，因此保留为ESLint解析器覆盖限制，没有为消除诊断改业务源码；这不补足该文件的ESLint未使用规则分析。现有Vite大chunk、MockJS eval及国密浏览器兼容提示照常留在日志，没有隐藏警告。

### 55.3 三类清单及确认候选

最终清单采用共享CSV字段，含稳定ID、路径/行号、规则、风险、证据/引用、建议与实际动作、前后SHA256及验证状态。**合计976条记录包含工具候选、复核盘点和保护边界，不能称为976个缺陷、死代码或可删文件**。有消费者的其余依赖/资源仍保留在对应完整inventory中，未因不在候选CSV而遗漏盘点。

| Skill | 确认可删 | 待人工复核 | 禁止自动删除 |
|---|---:|---:|---:|
| Java | 4 | 68 | 227 |
| Vue | 0 | 6 | 2 |
| 配置资源 | 0 | 330 | 172 |
| 固定分支 | 0 | 8 | 1 |
| 依赖 | 0 | 36 | 117 |
| 注释 | 0 | 0 | 5 |
| 合计 | **4** | **448** | **524** |

三个最终交付文件为本运行号根目录下confirmed_removable.csv、manual_review.csv、prohibited_auto.csv，各Skill亦有独立CSV，phase2-summary.json汇总实际能力和限制。java-scan/下的原始初分类曾有19个低风险建议；汇总时对国密/鉴权/签章/基座/反射等边界加严后仅保留4项，**以运行号根目录最终清单为准**，保留原始工具证据供追溯。

确认候选仅为以下普通Java import，非static/通配符；PMD命中且标识符在整个文件唯一词法出现为import本身，Javadoc没有引用，独立清理Agent复核未发现高风险越界：

- ls-aegis-server/ls-aegis-biz-psi/src/main/java/com/ls/aegis/biz/psi/support/InvoiceImportSupport.java:21、22：org.w3c.dom.Node、NodeList。
- ls-aegis-server/ls-aegis-biz-psi/src/main/java/com/ls/aegis/biz/psi/support/WriteOffHelper.java:21、22：StockInDO、StockOutDO。

这4项只是后续阶段4/5可考虑的低风险候选，**本轮仍全部未删除**。方法、组件、配置、资源、依赖和对外字段不进入自动删除范围；基座、国密、鉴权/租户、调度/队列、GB28181、协议/外部API及动态消费项继续保护。MANUAL_REVIEW不是删除许可。

### 55.4 本轮校验、变更边界与后续门禁

以下是本轮新执行结果，未借用历史通过记录：

- JDK21执行mvn -o -f ls-aegis-server/pom.xml -pl ls-aegis-boot -am compile -DskipTests，父reactor及16个子模块全部SUCCESS，约1分11秒；编译目标仍沿用既有release17。没有改POM、提升字节码目标或宣称已运行单元测试。
- pnpm9.15.4执行pnpm --dir ls-aegis-web typecheck，退出0。上述隔离PC Vite构建通过、源码哈希一致；它不替代配置真实环境的部署构建，也未新构建双H5产物。
- 新增JS采集脚本node --check、Python AST语法检查通过；classification-validation.json核对976条ID唯一、分类计数一致、actual_action全部NONE_SCAN_ONLY、候选前后哈希与当前文件一致。
- 汇总完成、报告写入前2456份基线哈希全部一致。三个应用目录相对HEAD差异为空；报告更新后仅报告/工作流准备文件变化，不影响应用不变的结论。最终只读冒烟9/9。

本轮没有应用清理patch，没有执行浏览器、摄像头/GPS/地图AK、单元测试全量或登录后业务闭环，不宣称阶段7回归/交付验收完成。没有埋点、业务造数、业务写接口或删除测试数据的操作；恢复服务的正常启动不作为“数据库绝对零写入”的证明。编译仅更新忽略的target缓存，未重新打包/替换运行中的候选JAR。

保留前轮未提交的Agent/7个Skill、基线脚本与文档改动；本轮增加本地工具链适配和4个只读扫描脚本、更新本节。未删除或移动类、方法、组件、依赖、配置、资源、SQL、证书或附件，未提交、推送或升版。无应用清理批次，回滚为NOT_APPLICABLE_NO_CLEANUP_BATCH；没有执行全仓还原或故障回滚演练。

阶段2结论为COMPLETED_WITH_EXPLICIT_COVERAGE_LIMITATIONS，即全仓范围盘点和现有工具扫描已完成，但上述语义/环境缺口仍存在。后续如实施4个import候选，须先限定patch并保存preimage，再执行编译、适用单测、候选启动和回归门禁；失败仅按postimage哈希精确恢复本批，不能把本轮未改源码的基线检查当作删除后的验收。

## 56. 四项import受控清理、单测及候选启动回归

### 56.1 白名单、实际修改及证据

2026-11-09按用户新授权继续conservative清理：本批只处理第55节确认的4个普通import，随后逐项复核剩余清单。沿用代码清理Agent及java-unused-scan、validate-and-rollback、itls-maintain等工作流；保留既有未提交的Agent/Skill、脚本及报告修改，没有扩大删除范围。

应用源码实际仅两文件、4行删除：InvoiceImportSupport.java删除org.w3c.dom.Node/NodeList；WriteOffHelper.java删除StockInDO/StockOutDO。业务方法、事务/权限/幂等、对外接口、数据库、类/组件、依赖、配置和高风险代码不变。与基线比对，其余2325份已记录应用文件哈希一致，Git应用目录差异恰为这两个白名单文件。

运行号R56-import-cleanup，全部私有证据位于.local/conservative-cleanup/R56-import-cleanup/。修改前保存preimage/<原路径>、manifest.json、changes.patch及git-status-before.txt；逐字节验证postimage恰等于预先计划的4行删除，没有附带换行/格式化改动。changes.csv为4条原发现的实际动作与前后哈希，validation.json汇总结果。原阶段2CSV保留不覆盖，避免把历史候选状态改造成“当时已删除”。

### 56.2 首次失败、精确回滚及环境适配

首次执行JDK21下完整后端mvn -o -f ls-aegis-server/pom.xml -pl ls-aegis-boot -am package，单测在既有biz-file的LocalMultipartHandlerTest.servletStyleTransferReceivesAbsoluteDestination失败：WindowsPath.relativize报other has different root。用例将D盘工作目录与C盘JUnit临时目录作相对路径；当时尚未执行PSI本批模块。

按门禁立即核对两文件仍匹配postimage，逐文件恢复preimage并验证原SHA256。rollback-first-attempt.json记录实际恢复成功，不是模拟回滚；未执行reset/clean，也未覆盖其他工作区修改。

恢复后的原源码上验证环境修正：仅用Maven-Djava.io.tmpdir仍不能改变JVM初始化时缓存的临时路径，用例继续失败；将当前构建进程TEMP/TMP及Surefire测试JVM argLine的java.io.tmpdir都指向本运行号D盘test-temp后，LocalMultipartHandlerTest的4项全部通过。只改变当前命令环境和启动参数，未改测试类、业务类、POM或任何项目配置。

确认原用例通过后重新应用同一份4行patch，使用上述同盘临时目录运行完整package，未设置skipTests或跳过失败用例。证据依次为build-test.log、baseline-path-test.log、baseline-path-test-jvm.log、build-test-final.log。后续在Windows跨盘环境复现测试时，需先创建专用D盘临时目录并以测试JVM启动参数指定，不能仅据Maven属性显示的值判断底层临时目录已改变。

### 56.3 本批编译、单测与新候选启动结果

| 校验 | 本轮结果 | 边界 |
|---|---|---|
| JDK21完整后端reactor package | 父工程及16个模块全部SUCCESS，约3分23秒 | 沿用release17；无依赖升级，未跳过单测 |
| 单元测试 | 新鲜Surefire XML统计86套件、745项，0失败/0错误/0跳过 | unit-test-summary.json按最终构建开始后的报告修改时间筛选，不计旧失败报告；含PSI325项 |
| 方法字节码核对 | InvoiceImportSupport与WriteOffHelper两类javap -c -p反汇编均与v34候选完全一致 | 不比较调试行号表，不声称整个JAR逐字节相同；bytecode-validation.json及输出可复核 |
| 新候选启动 | 本批新JAR在临时容器ls-aegis-r56-candidate、127.0.0.1:18000启动，就绪约36.91秒 | 真实/auth/gm/public-key返回success=true/code=0；没有以原8000旧进程代替候选启动 |
| 启动冒烟 | 9/9 | 其中2项直连新候选API（就绪及未登录拒绝）；其余7项为原PC/H5及原8000代理可用性，不能称为前端已切换新候选的完整联动 |
| 最终在用服务 | 8000/PC/双H5只读冒烟9/9 | 保持原8000运行，未自动替换在用JAR；临时18000容器已按精确ID移除并回查不存在 |

候选JAR SHA256为887fcb7f35a5be6f2c30ed7df92ff1f63bcc4972b403bfcdc7990b4c5891b756，保存在本运行号candidate/。启动使用忽略目录中的临时Compose派生文件，沿用已有中间件/环境来源，禁用Liquibase和调度设置保持原值；仅候选名、JAR挂载、端口、日志路径及不自动重启策略作临时隔离，不修改任何跟踪配置。startup-validation.json、candidate-smoke.log、candidate-cleanup.log及final-live-smoke.log记录实际结果。

本批没有改前端源码，因此没有重复前端构建；未执行浏览器、摄像头/GPS/地图AK、登录后的全模块业务闭环或容量测试。既有WriteOffSummaryQueryTest及RefundTodoLifecycleTest使用真实helper和隔离/模拟消费链，连同全部后端单测实际通过；不把单测与L0启动冒烟写成完整L2/L3业务验收，也不将import清理宣称为运行时性能提升。

本轮未执行登录后的业务写接口、未创建合成单据/附件或业务账号，无本轮业务造数需删除；正常启动不等同数据库绝对零写入。临时候选容器已清理，JAR/日志/补丁等证据保留在忽略目录。删除test-temp时虽已做绝对路径边界及重解析点预检，工具自动审批仍拒绝，唯一原因为blocked by policy；未绕过重试删除，测试缓存目录保留，状态见cleanup-status.json，不能写“临时文件已全部清空”。

精确回滚预检命令为python -X utf8 .local/conservative-cleanup/R56-import-cleanup/rollback.py，默认只验证不写入；加--apply才恢复本批两个preimage。当前文件必须匹配本批before或after哈希，否则拒绝覆盖并行修改；预检已通过。本批没有提交、推送或升版，仍保留工作区改动供后续统一处理。

### 56.4 剩余972条逐项证据复核

对第55节的448条MANUAL_REVIEW和524条PROHIBITED_AUTO分别补充逐项证据：review-manual.csv、review-prohibited.csv保留原发现ID及原分类/扫描位置，追加当前源码行/哈希、实际引用/目录消费者/依赖声明证据、保留原因和未解决证明。两份清单合计972条，ID覆盖原清单且无重复遗漏；原始扫描行号与当前源码行号分开，涉及本批两个文件时按删行位置作映射，不把旧哈希冒充当前状态。

| 逐项复核结果 | 数量 | 实际依据与动作 |
|---|---:|---|
| 有目录或依赖消费证据，保留 | 330 | 321个SVG由config/plugins/svg-icon.ts的iconDirs目录、main.ts虚拟注册入口及GiIconSelector的glob消费；5项H2有测试运行用途；4项PDF声明对应解析/渲染链。均未删除 |
| 语义/动态消费证明不足，保留 | 110 | 逐项登记Java规则及当前行、前端解析/回调或H5虚拟解析限制、环境/权限分支、资源/CLI/peer/types等消费者缺口；未升级为可自动清理 |
| 高风险/动态底层边界保护 | 532 | 原524条逐项核对当前源行/哈希或配置资源/依赖声明证据并保留；另8条由人工复核升级，见下文 |

321个SVG有目录级消费，不表示已经逐个证明真实用户运行时选择过每个图标；消费者证据足以否定“无同名文本引用即可删”的依据，但不是每个资源全生命周期可达性的证明。依赖声明盘点也不替代完整resolved dependency usage分析。110条保持具体证据缺口，不声称已穷尽反射、全部环境或外部消费。

classification-corrections.csv记录8条收紧：根POM中的common、rbac、biz-tenant、biz-schedule、biz-sign、Lombok，以及根POM和rbac中的两条biz-notice声明。它们分别涉及基座、鉴权、租户隔离、任务、签章、注解处理器和无法排除队列/外部消费的消息边界；原扫描按根POM路径未充分加严，本轮改分类，不改POM。复核后的两类标签总数为MANUAL_REVIEW440、PROHIBITED_AUTO532；其中MANUAL_REVIEW440含上述330条有消费者的保留项，不能理解为又发现440项无用代码。

remaining-review-summary.json记录范围核对与限制。除原4个import外，新增实际清理0项；未删除/注释私有方法、回调参数、模板变量、配置开关、组件、资源或依赖。完整语义证明仍不足的110项保持保留，后续若继续，应先补对应动态消费/副作用/契约证据再提出新的限定批次，不能把本批745项测试通过沿用为下一批修改后的验收。

## 57. 110项证据不足记录的深入审查与回滚门禁

### 57.1 本轮审查范围与工具证据

2026-11-09按用户要求，由主Agent继续深入审查第56节的110条证据不足记录：Java68、Vue6、配置资源9、分支8、依赖19。沿用conservative及Java/Vue/资源/分支/依赖、验证回滚Skill。逐项结果位于忽略目录.local/conservative-cleanup/R57-deep-review/deep-review-110.csv；110个原发现ID全部覆盖，无重复遗漏。deep-review-summary.json记录分类和实际限制，不能把“已给出审查结论”理解为所有支持环境、外部消费者均已穷尽。

Java使用JDK21 Compiler API，读取上轮Surefire XML的真实java.class.path，在内存中对每份原源码parse/analyze，再逐个移除候选import并比较诊断；只分析、不调用generate，不写项目class或源码。使用release17，明确-proc:none。第一次受限执行不能读取本机Maven缓存，随后通过工具审查获准只读读取缓存，未下载/升级依赖。输入/输出为java-input.tsv、java-compiler-review.tsv及java-compiler.log，可按ID追溯具体缺失符号，而非只靠词法零引用。

| 68条Java记录 | 数量 | 深入结论 |
|---|---:|---|
| 原源码分析0错误，移除import后新增错误 | 62 | 必需import，保留；许多是提供@Test、Mapper/模型等类型的通配符import，原PMD未完整解析类型时产生候选，不能删除 |
| 独立分析缺Lombok构造生成，结合明确源消费及权限边界保护 | 3 | FifoStockHelper使用同包TenantFillHelper；MasterDataReferenceGuard字段/查询分别使用Mapper和UnitDO/GoodsDO/WarehouseDO；均涉及租户/数据权限，保留。-proc:none构造未初始化诊断不代表项目完整编译失败 |
| 实际有调用的私有重载 | 1 | QuoteConversionConcurrencyTest.submit(Runnable)的编译字节码存在明确invokevirtual调用，原PMD漏解lambda重载，保留；quote-concurrency-bytecode.txt可复核 |
| 未发现直接调用、但方法删除不在本模式白名单 | 1 | RepairOrderServiceImpl.deletePhotoFiles仅定位到声明，方法包含物理附件删除委托；没有穷尽反射/生命周期消费，本轮不删除方法 |
| 原/移除后编译诊断均0，确认冗余 | 1 | DocTaxBoundaryTest.java:3的com.ls.aegis.biz.psi.model.entity.*未被实际类型/Javadoc消费，允许作为限定一行候选；本批尝试后的回滚见57.3 |

### 57.2 前端、配置资源、分支及依赖的具体结论

Vue6条已分别检查script/template绑定、实际调用和工具解析：RepairDetail.vue的idx仅在v-for声明出现，key使用record.id，SFC/template编译无错误，确认为未使用的局部模板别名，但未纳入本轮Java补丁。Repairs.vue的customerNo是未消费的computed绑定，其另一次文本出现是属性名；Profile.vue的showFailToast也是未使用named import，但两者处于客户身份/登录退出及角色上下文，保护、不自动修改。form-field-identity.ts的ensureName/ensureLabelAnchor均有真实调用，index只在形参声明出现；本轮保留调用签名和所有调用者，不能将整个函数判为无用。

dashboard/analysis/index.vue:367的解析缺口已定位：仅在本地审查ESLint实例的Vue目标上关闭JSX解析，泛型箭头函数可正确解析，fatal诊断0。没有改仓库ESLint/tsconfig，也没有全局关闭TSX支持；默认仓库lint配置的该缺口仍需后续单独处理。解析后新增makeBarOption:465未使用函数候选，独立登记new-candidates.json（R57-NEW-01），不冒充原110条之一，不在当前方法删除白名单内。

配置资源9条：两份META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports分别指向实际EomsConfiguration/QuoteConfiguration，其@AutoConfiguration/@ComponentScan负责注册业务域，属框架动态消费，升级保护。tsconfig.json是vue-tsc约定入口，pnpm-workspace.yaml是pnpm工作区约定入口，build-artifacts.bat是转发build-artifacts.ps1参数/退出码的可执行Windows包装，保留。H5 Dockerfile.dev未被当前Compose选中；police-badge.svg在现有登录页面的实际使用已改为PNG；PC banner.png/login_h5.jpg未找到当前索引/已有隔离图的消费者。这4份不能声称“已证明必需”，也没有排除外部/人工消费；根据本轮禁止删除配置/资源/脚本的范围保留，不归档或删除。

分支8条：EOMS/PsiFileAccessChecker的4条及权限/角色指令2条实际取决于用户、租户、数据权限和可访问附件计数，不是固定死分支；路由guard的PROD版本检查同时处于鉴权模块，继续保护。main.ts的VITE_BUILD_MOCK来自构建环境，并条件加载mock，不能因当前默认关闭而删分支。当前代码均未改变。

依赖19条均找到具体声明或消费者：EOMS引用报价端口/桥接接口，PSI/EOMS/RBAC引用文件服务与附件权限API；根dependencyManagement为实际boot/子模块提供版本管理；Hutool由SecureUtils实际调用加密API，升级保护。前端主题经main.ts→styles/arco-ui/index.less→主题Less加载；vue-demi是已安装@vueuse/core依赖；lodash/Node类型包服务于实际类型检查和node:配置导入；@vue/tsconfig被extends；ESLint、TypeScript为实际CLI/编译器；Less/Sass服务于已有Less与lang=scss；lint-staged由package.json的pre-commit配置及任务显式消费。未执行Git hook，不宣称已安装/运行该hook。npm-evidence.json保留已安装模块与第三方依赖证据；未改变依赖、锁文件或配置。

110条的复核分类为16条PROHIBITED_AUTO、93条MANUAL_REVIEW保留、1条CONFIRMED_REMOVABLE。93条标签包含已证明必需的import、约定入口及本轮不改的形参等，并不表示93条仍未审查或93项无用代码。资源外部消费者、默认ESLint设置及3份Lombok独立分析限制均逐项保留说明，没有用统一“疑似”模板代替检查。

### 57.3 一行候选的测试、启动阻塞与实际回滚

仅对DocTaxBoundaryTest.java的一行普通实体通配符import实施试验批次；修改前保存cleanup/preimage、manifest.json及changes.patch。保留第56节的全部既有改动，不覆盖两份已清理Java文件。JDK21下离线完整reactor package约2分49秒成功；本轮新Surefire报告统计86套件、745项测试，0失败/0错误/0跳过，含受影响测试。临时路径继续通过当前进程TEMP/TMP和测试JVM argLine限定到D盘，未改配置或跳过用例。

候选启动门禁失败：访问Docker Desktop Linux engine管道时文件不存在，不能创建本批18001候选容器。校验脚本立即核对本批postimage并只恢复这一份test源码preimage，rollback-startup.json验证成功；此前第56节两文件的4条import清理仍与原postimage哈希一致。实际保留新增源码清理**0项**；清单中的一条“确认可删”保留为已验证候选，不能写成已删除交付。候选JAR、补丁、测试缓存和日志留在忽略目录，未创建/写入业务测试单据、账号或附件，也未创建成功的本批测试容器。

尝试一次正常docker desktop start --timeout 55，返回Docker Desktop terminated unexpectedly。宿主日志明确Secrets Engine无法把engine.sock重命名为engine.sock.stale，原因The file cannot be accessed by the system；不是新的Java编译/业务逻辑错误。停止进一步自动恢复，没有删除socket/凭据目录，没有改ACL、重置/注销WSL、删除/迁移数据卷或尝试新建空白Docker环境。

同次Docker日志还记录user-appmanager来源的CustomWslDistroDir设置从D:/DockerDesktopWSL切换到默认AppData/Local/Docker/wsl。这里只记录观察结果，无法据此确定操作来源；本任务未直接写该设置或执行目录迁移。再次恢复Docker前需要核对原数据目录，不能启动后看见空环境就认为旧业务数据已被删除。

最终只读服务冒烟3/9：PC和双H5的HTML可达，API8000及代理不可用。第56节9/9是当时历史结果，不冒充当前健康；本轮应用代码不新增变化，未将测试通过替代启动成功。validation.json状态为审查完成、编译/单测PASS、候选启动BLOCKED、单行清理已精确回滚。未提交、推送或升版；完成Docker环境恢复后才可重放这一行patch并重新验证候选启动。

## 58. 原Docker数据目录恢复与单行import清理验收

### 58.1 原磁盘核对与可逆恢复

2026-11-09按用户明确授权，先核对原数据目录、修复Docker和恢复8000，再重放第57节唯一确认无用的import并重新验收。继续conservative；保留之前工作区修改及所有原数据盘、卷，未扩展到类、方法、组件、依赖或项目配置清理。

目录定位参考[Docker官方WSL后端说明](https://docs.docker.com/desktop/features/wsl/)与[磁盘备份恢复说明](https://docs.docker.com/desktop/settings-and-maintenance/backup-and-restore/)，本机判断以实际文件和容器身份为准。核对结果：

| 对象 | 实际证据 | 判断 |
|---|---|---|
| 原数据盘 | D:/DockerDesktopWSL/disk/docker_data.vhdx，22502440960字节，创建于2026-09-08 | 原Docker业务环境所在盘，保留并重新使用 |
| 默认位置的新数据盘 | AppData/Local/Docker/wsl/disk/docker_data.vhdx，创建于本日21:25，初始约281MB、引擎首次完整启动后1561329664字节 | 新盘，不能用来代表原环境；同样保留，没有覆盖或删除 |
| 原启动盘备份 | D:/DockerDesktopWSL/main_admin_bak/ext4.vhdx仍存在 | 未手工替换、移动或删除 |
| 当前设置 | CustomWslDistroDir曾指向默认C盘位置 | 是本轮恢复时需纠正的目录指向 |

本轮检查时Docker已能在默认C盘位置启动，但尚未连接原数据盘；第57节曾出现的Secrets Engine/socket错误没有通过删除socket或凭据目录处理。先使用正常docker desktop stop等待Docker进程退出，并确认没有运行中的WSL发行版；将settings-store.json修改前状态备份到忽略目录，再通过单字段替换仅恢复CustomWslDistroDir为D:/DockerDesktopWSL。未把新C盘VHDX复制到原D盘，也未进行磁盘替换、压缩、清理或卷初始化。

随后从已确认安装路径隐藏启动Docker Desktop，约21.7秒后Docker Server29.9.2就绪，目录设置持续为D盘。正常启动后Docker自身把docker-desktop注册BasePath更新为D:/DockerDesktopWSL/main；本任务未执行wsl --unregister/--import/--move、发行版重置或手工镜像替换。原数据盘创建时间与大小仍吻合，未对运行中VHDX宣称位级哈希不变。

启动期间原VHDX的ACL确实新增了虚拟机/应用身份条目，原权限条目保留；这是本轮正常启动前后的观测差异，不写成“ACL完全未变”。本任务未执行手动ACL写入、提权修复或凭据内容读取/输出。证据pre-recovery-metadata.json、settings-store.preimage.json、desktop-stop.log、desktop-launch.json、engine-start-result.json、docker-recovery-result.json和wsl-registration-after.json位于.local/conservative-cleanup/R58-docker-recovery/，设置preimage只留证，不作为源码回滚脚本的恢复对象。

### 58.2 原容器、挂载及8000恢复验证

恢复后原容器和镜像重新出现，而非在新空盘中重建：

- 后端完整ID仍为8d9037d2c03bff92be2687830d4e66c526104ff63a20f4fe8bbfe8e95d507d71，与第54/55节恢复记录完全一致。
- MySQL仍为e9d7f366979a、Redis为4d3c2fbb7b3b，均healthy；媒体容器78c5672d758b保留。
- 原mysql:8.0.42、redis:7.2.8、eclipse-temurin:21-jre-jammy及媒体镜像重新可见；原绑定挂载元数据已保存，未修改数据源路径或执行安装SQL。
- 8000恢复，PC/双H5/API只读冒烟9/9。没有以“Docker进程存在”代替原业务环境验证。

恢复前后原卷名集合相同，候选测试结束后再次核对无增减。恢复证据original-containers.txt、original-volumes.txt、original-images.txt、original-mounts.txt、restored-smoke.log。只核对元数据、健康和只读接口，没有读取业务表内容来造出恢复结论，也未进行业务库重建、重置卷、删卷、prune或恢复/回灌SQL。

### 58.3 一行清理的本轮新验收

仅重放DocTaxBoundaryTest.java:3的import com.ls.aegis.biz.psi.model.entity.*删除。修改前确认当前源码仍等于第57节已回滚的preimage，并保存本运行号cleanup/preimage、manifest.json及changes.patch；postimage与此前经Java Compiler API证明可删除的单行补丁完全一致。未追加清理其余109项或新增前端候选。

| 校验 | 本轮实际结果 |
|---|---|
| JDK21离线完整reactor package | 父工程及16个子模块全部SUCCESS，约3分01秒，未skipTests |
| 单元测试 | 新鲜Surefire XML统计86套件、745项，0失败/0错误/0跳过 |
| 本批候选启动 | 临时容器ls-aegis-r58-candidate，127.0.0.1:18002；约39.38秒就绪，/auth/gm/public-key返回success=true/code=0 |
| 候选/在用冒烟 | 9/9：2项直连新候选API，其余7项检查原PC/H5及8000代理；不冒充前端已切换候选的完整业务闭环 |
| 最终8000/PC/H5复测 | 9/9，原8000容器继续运行，未被候选替换 |
| 临时资源清理 | 按精确ID移除候选容器并回查不存在；卷名集合与恢复后原清单相同，未用-v或删除共享资源 |

候选JAR SHA256为887fcb7f35a5be6f2c30ed7df92ff1f63bcc4972b403bfcdc7990b4c5891b756，与第56节候选相同，符合本轮仅改变测试源码import、未改主程序实现的边界。仍保留本轮完整构建和新启动证据，没有直接引用第56/57节旧测试作为本轮通过。真实readiness使用平台接口，不使用未配置的actuator接口。证据build-test.log、unit-test-summary.json、startup-validation.json、candidate-smoke.log、candidate-cleanup.log、final-smoke.log及validation.json。

其余2324份基线应用文件哈希不变；此前两份主源码4条import清理与第56节postimage仍完全一致。本轮新增变更仅一个测试文件、一行删除，累计应用差异为三个文件、5条import；类、方法、组件、依赖、项目配置、数据库契约和国密逻辑未变。未运行浏览器、硬件、容量或登录后的全模块业务闭环，不将745项单测和L0冒烟等同于完整生产验收。

### 58.4 清理结果与当前交付边界

本批全部必需门禁通过，单行import清理保留，解决第57节该候选因环境不可用而回滚的阻塞；第57节历史记录不覆盖。cleanup/changes.csv更新该原发现S2-00184的实际动作、前后哈希和本轮通过状态。精确回滚预检为python -X utf8 .local/conservative-cleanup/R58-docker-recovery/rollback.py，默认不写文件；必要时加--apply仅恢复本批测试文件，哈希不匹配时拒绝覆盖并行修改。预检已通过，无需回滚。

本轮未创建业务单据、账号、签署或附件测试数据；启动应用本身的正常数据库/缓存行为不作为“绝对零写入”承诺。原数据盘、新C盘磁盘、启动盘备份和所有原卷均保留，未执行重置/删除。本轮候选容器已清理，JAR、测试缓存、日志、配置留证和补丁保存在忽略目录，不宣称临时文件全部清空。

当前Docker使用原D盘环境、8000可用、最终冒烟9/9，工程状态保持conservative。版本未变，未提交或推送Git；保留既有Agent/Skill/脚本和报告的工作区修改。剩余审查记录与高风险保护仍以第56/57节为准，不因本批通过而自动解除后续删除限制。

## 59. 前端局部未用绑定清理与双端构建回归

### 59.1 基线、限定范围与实际修改

2026-11-09按用户“继续，不要等”的授权，执行上一轮提出的3项低风险前端候选：S2-00300、S2-00303、S2-00304。沿用conservative、vue-unused-scan及validate-and-rollback，所有角色可以由主Agent执行；本轮未另行扩展删除范围。

开始时8000短时不可用，未先修改代码。首次系统状态查询的自动权限审核超时，未执行；按工具提示重试一次最小只读docker ps成功，确认原后端/MySQL/Redis容器正在启动。等待就绪后只读基线9/9才实施修改。没有再次修改Docker目录、WSL设置或重置任何卷，也没有把工具审核超时写成“不安全”结论。

本批只修改两个文件：

- ls-aegis-h5/src/views/customer/RepairDetail.vue：v-for由(record, idx) in records改为record in records，保留record.id key及全部记录内容。
- ls-aegis-web/src/utils/form-field-identity.ts：移除ensureName/ensureLabelAnchor两个私有函数未读的index形参并同步调用；进一步移除只转传index的私有associateLabel形参及两个forEach回调的索引绑定，避免形成新的未用变量。共清理6处局部绑定，不删除任何函数或方法。

两文件共11行替换，不做全仓格式化；名称生成、序列递增、查询/关联顺序、label锚点和公开导出签名保持不变。身份/退出、权限、租户、国密、组件、依赖、配置、样式及makeBarOption候选均未修改。第58节后端三个文件的5条import清理保留。

证据统一位于.local/conservative-cleanup/R59-frontend-cleanup/：修改前的preimage、逐文件前后SHA256、postimage、changes.patch与baseline-hashes.json；changes.csv按3个原发现记录实际动作及必要调用链联动。应用补丁后逐字节核对与计划postimage一致，不以Git忽略换行差异掩盖额外修改。

### 59.2 行为证据与现有测试

使用PC项目已有TypeScript解析器核对3个私有函数：标识符只存在于声明或直接调用，不作为值传递、注册或观察function.length。修改前后引用计数均为ensureName4、ensureLabelAnchor2、associateLabel2；公开导出patchFormAccessibility(root: ParentNode = document)与watchFormAccessibility签名一致。

对preimage和当前源码分别转译并在隔离VM运行真实ensureName/ensureLabelAnchor，使用合成控件核对既有name/id保留、已有ID冲突避让、不同控件ID唯一、重复修复复用同一锚点、for移除及标记设置，结果完全一致。H5则使用其自身安装的Vue SFC编译器3.5.40，修改前后template编译无错误；编译渲染代码除未使用的(record, idx)回调索引参数外相同。未用PC编译器替代H5项目编译器。

已有前端相关Node回归25份文件、178项全部PASS，0失败/0取消/0跳过。包括全站表单扫描范围/调度、ID碰撞、菜单/路由、请求状态、预览、签署、H5身份/路由/报修等既有覆盖；与本批无关的backup_failure.test.cjs三项备份隔离用例未纳入，不把178称为全项目所有测试。后端源码本批未变，不冒充本轮重新执行了Java745项测试。

修改后的PC文件采用现有ESLint、fix=false核对unused/fatal诊断为0；其余3条既有诊断与preimage按规则/行号/消息比对一致，没有为了通过而格式化整个文件，也不宣称完整lint全部为0。H5通过自身SFC编译/渲染等价证明。证据behavior-equivalence.json、scoped-eslint.json及frontend-tests.log；这些合成DOM/编译测试不等于真实浏览器无障碍或页面交互验收。

### 59.3 构建门禁失败、精确回滚与通过结果

为不读取真实.env、不让插件回写原类型文件，PC/H5均在忽略目录build-input中的输入副本构建，排除环境/证书文件；node_modules只作junction链接，未安装或升级依赖。H5显式设置公开role与BASE_PATH=/，PC用pnpm9执行原typecheck入口，再运行原有生产模块检查脚本的隔离路径副本。

首次PC构建完成，但原fullUtilityRootModules门禁失败：未提供公开VITE_BUILD_MOCK时，Mock动态入口保留在产物图中，并带入xe-utils总入口。立即按postimage核对恢复两份preimage；rollback-first-build.json及first-pc-build.log/first-pc-module-audit.json保留实际失败和回滚证据。未把“Vite构建完成”写成模块门禁通过。

将本轮验证环境明确为VITE_BUILD_MOCK=false，重放完全相同的源码postimage后重测；只设置构建子进程的公开环境值，没有删Mock功能、改仓库配置/环境文件或放宽阈值。178项回归和typecheck对应同一postimage，未因重新应用相同字节而重复整套测试。最终结果：

| 门禁 | 本轮结果 |
|---|---|
| PC类型检查 | pnpm9.15.4、原typecheck入口PASS |
| PC生产构建与模块检查 | PASS，约1分10秒；首页Cron/Luxon模块0，调度页7；Arco未解析/整套安装0，xe-utils总入口0，PDF引擎保持定位组件异步边界 |
| 客户H5生产构建 | PASS，本端页面存在、未包含工程师页面，Vant未解析组件0，函数式API样式存在 |
| 工程师H5生产构建 | PASS，本端页面存在、未包含客户页面，Vant未解析组件0，函数式API样式存在 |
| 构建输入核对 | 原项目905份前端输入与冻结postimage哈希一致；副本中对应输入构建后亦无字节变化，生成类型没有回写原项目 |

保留现有大chunk、国密浏览器crypto外部化等构建提示，没有通过提高阈值或修改高风险逻辑消除警告。以上为非Mock、显式H5分端、base=/的隔离生产构建，不是读取真实环境文件后的公网部署配置验收；本轮没有据单次产物大小宣称页面速度或容量提升。证据pc-typecheck.log、pc-build.log、pc-module-audit.json、h5-build.log、h5-module-audit.json及generated-declarations.json。

### 59.4 三端候选启动、清理和最终状态

在隔离产物目录用对应项目已有Vite preview启动PC、客户H5、工程师H5三个临时loopback候选，随机空闲端口，显式关闭自动打开浏览器。逐端检查HTML、入口JS/CSS实际可访问且不是HTML回退、公钥接口代理success=true/code=0、未登录受保护接口拒绝。三端全部通过；本轮预览/api代理仅指向既有127.0.0.1:8000，不是生产Nginx代理验收。

三个候选HTTP服务已关闭，preview-validation.json确认无监听；最终原5173/5174/5175/8000服务冒烟9/9。未替换在用部署产物、未创建业务单据/账号/照片/签署等数据，无本轮业务造数需补偿；保留本地构建/日志/补丁证据，含node_modules junction的目录不做递归删除。浏览器真实交互、摄像头、GPS、地图AK、容量及登录后完整业务闭环未执行，不以HTTP预览或模拟地理用例代替这些验收。

validation.json为PASS，三个原发现已实际处理；报告追加前其余2454份跟踪输入哈希一致。追加本节只增加正式报告变化，未覆盖之前的Agent/Skill/脚本或后端改动。精确回滚入口python -X utf8 .local/conservative-cleanup/R59-frontend-cleanup/rollback.py默认只预检，加--apply才在哈希匹配时恢复本批两文件，预检通过；本批最终无需回滚。版本未变，未提交或推送Git。

## 60. 保守清理收尾、清单闭环与本地提交

### 60.1 本轮授权、范围与最终清单

2026-11-09用户准备休息，明确授权按前述收尾步骤自动执行：总变更核对、更新清单状态、整理本地提交，随后审查普通注释/TODO及其他候选。继续conservative，不将“自动执行”解释为放开类、方法、组件、依赖、配置或高风险删除。当前轮应用源码没有追加修改，只做只读审查、整理Git及本报告。

将第55节原始976条记录按稳定ID与第56至59节实际动作逐项对账，生成忽略目录.local/conservative-cleanup/R60-closeout/final-findings.csv、closed-findings.csv和closeout-summary.json：

| 最终状态 | 数量 | 含义 |
|---|---:|---|
| 已清理并验证 | 8 | 5条Java import、3条原前端发现；前端必要联动共移除6处局部绑定，不按文本替换次数重复计数 |
| 已审查、保留消费者或模式边界 | 420 | 包含确认必需的import、动态目录资源、工具/运行依赖、约定入口及本模式不删的文件；不等于420项缺陷或420项尚未审查 |
| 禁止自动修改 | 548 | 含原保护项及后续加严的基座/鉴权/租户/调度/国密、自动装配和方法等边界，保持原代码 |
| 合计 | 976 | ID覆盖完整、无重复遗漏 |

R57-NEW-01的makeBarOption是原976条之外的新增函数候选，单独保留，未在本轮删除。当前没有额外普通注释/TODO获得低风险删除证明，不为增加清理数量扩张范围。

清单保留原扫描位置，当前行号对已删除import作偏移映射；实际删除项不再声称有原位置的现行代码。敏感环境/证书记录仍只登记路径，不为计算当前哈希读取其内容。私有CSV、日志、JAR、preimage、Docker设置留证及真实数据目录均未进入Git。

### 60.2 验证证据与阶段退出条件

总复核确认第58节后端完整package、86套件745项单测及候选启动通过；第59节PC类型检查、PC/双H5生产构建和模块门禁、178项前端相关回归、合成DOM/实际H5编译器等价检查、三端隔离预览通过。全部保留源码哈希与各批postimage一致。收尾只变更文档和Git元数据，没有把这些历史结果写成本轮重新执行了整套测试；本轮补做新工作流脚本的Node/Python/PowerShell语法检查及git diff --check，均通过。

本轮最新只读服务冒烟9/9（R60/final-smoke.log），原8000、PC和双H5可用。没有新建业务单据/账号/附件、运行真实备份、调用外部短信/支付/签署或新增压测。之前失败批次的精确回滚及同postimage重放证据继续保留，不删失败日志制造“全部一次通过”。

阶段1/2已完成基线和有覆盖限制的扫描；阶段3运行时埋点为可选、未执行；阶段4分类和限定方案、阶段5低风险白名单、阶段6逐项保留清单、阶段7对应批次校验闭环完成。这里关闭的是本轮保守清理批次，不代表全部业务、浏览器、硬件、外部地图或生产容量验收完成；不解除后续新批次的基线/证据/回滚门禁。

### 60.3 普通注释与TODO进一步审查

对应用源码执行有长度限制的标记扫描并人工核对上下文，过滤业务TODO_URL/return TODOs及嵌入图片内容等非待办标记；实际TODO注释仍为5处，均在签章高风险区域。comment-review.csv记录逐项原位置、现行哈希、实现/调用证据及范围限制，没有删除任何注释或代码。

- SignBackupServiceImpl中4处TODO不是已完成的普通过时注释：手工备份当前仅写任务记录，并设置success、路径和0字节大小，没有实际归档实现；删除方法只删除任务记录，计划入口委托手工方法。全后端Java直接引用扫描只发现该接口/实现及内部委托，当前PC/H5源码也未发现/sign/backup调用；没有据此穷尽数据库任务、反射或外部调用，因此不能判定类可删除，也不能宣称当前线上存在可达的“假成功”接口。这是需要单独核实入口和需求的高风险占位实现，本轮保持不动。
- SignExecuteServiceImpl的TODO处实际返回pdfService.verifySignature(bytes)，旁边保留“签章后哈希变化，应验签”的协议说明。反查哈希的旧思路与当前实现应在国密/签名专项中校订，不作为普通注释批量清理。

这些签章业务TODO与scripts/backup.sh系统备份测试不是同一实现，不能拿后者通过替前者背书。未在真实环境触发备份、删除记录或执行计划任务。对“未接入/动态消费尚未穷尽”与“功能错误已被真实复现”严格区分，留待受保护功能专项处理。

### 60.4 普通本地提交与当前仓库状态

用户已接受“整理提交”的收尾步骤，本轮按以下三批普通提交落地：

| 提交 | 内容 | 报告快照范围 |
|---|---|---|
| 62150012bba34f86e6506f16b78c9eeff7d2a71e | 清理Agent、7个Skill、6个只读/工具链脚本及根任务路由 | 第54至55节 |
| 48111755e0431e2ed2c0208036b971ea930bba91 | 五条无用Java import | 第56至58节 |
| 18b2078c184dbd1cce6b72e294e33772d3c253ec | 两文件前端局部绑定及必要转传链清理 | 第59节 |

每批以明确文件白名单暂存，分阶段报告只在index写入对应快照，工作区完整报告未被截断。第一次报告切片在EOF多出空行，git diff --cached --check阻止提交；仅修正私有中间快照并验证暂存所有权后继续，没有绕过检查。每批提交前核对暂存路径，提交后再次对比实际文件集合和源码哈希，未混入.local、环境文件、证书或数据。

本机Git当时未配置作者身份，最近五次提交的维护者身份一致；本轮仅通过单次命令参数沿用该既有身份，未改全局身份配置。未安装/禁用hook，实际检查未发现活动hook；提交无Cursor/AI共同作者附注。仅追加普通提交，没有amend、rebase、force、标签更改或版本提升，也没有执行push。前三批完成后工作区已核对干净；本节随后单独作为收尾报告提交，具体hash以本地git log中的对应文档提交为准，避免报告自引用hash循环。

commit-plan.json、commit-receipt.json及git-batches.log保存具体白名单、身份来源和命令回执。版本仍为2.1.0；当前轮清理收尾完成，高风险项与完整生产验收限制保留，不生成未经验证的新删除批次。

## 61. 业务工作流Agent与逐功能真实测试首轮

### 61.1 执行范围与可复用入口

2026-11-09按本轮授权新建agents/business-flow/AGENTS.md与skills/itls-workflow-test/SKILL.md，根AGENTS.md加入显式路由。复用原分阶段矩阵和业务流程，不建立第二套平台、账号体系或部署栈；Skill验证通过。校正原验收矩阵仍引用已移除biz-open、代码生成预览下载的过时范围，16个子模块与父reactor共17个项目。

真实测试目标为当前在用API8000，PC5173及两端H5沿用原部署。运行构建由容器挂载和文件SHA256核对：原8000挂载.local/backend-v2-v34/ls-aegis-server.jar，SHA256=84bd011ed6a262a252f9ae3085a1c2b90eaa04832c2fe7934853cac5205dd403。源码基线HEAD为1c87dabbdc53fec1ff3f08e35b553b0a377dc0b8，不将源码HEAD冒充运行包版本。私有运行台账位于.local/business-workflow/ITLS-TEST-R61-AUTH及ITLS-TEST-R61-RBAC；不提交凭据、会话、验证码或日志。

scripts/quality/business_workflow_auth.cjs及business_workflow_client.cjs执行正常加密登录与私有会话，按实际验证码图像完成登录，没有关闭验证码或读取缓存答案。rbac/platform/file/notice/psi/quote/repair脚本按“创建→动作→持久化/关联断言→逆序补偿→独立回查”执行，遇到不确定写响应先按唯一标识回查，不盲目重建。台账按运行包区分初测和后续候选复测。

### 61.2 原8000真实结果与清理

| 功能 | 实际通过内容 | 边界 |
|---|---|---|
| F01认证 | 7项：公钥、未登录拒绝、真实加密登录、当前身份、菜单、退出、旧会话拒绝 | 正常账号接口实测，不等于全部角色/浏览器验收 |
| F02用户/角色 | 7项：专用SELF角色、用户与角色双向关联、昵称修改、重名拒绝、关联角色删除保护、停用、启用 | 未修改原管理员或既有角色权限 |
| F03部门/字典 | 6项：父子部门及树回显、下级保护、编辑保关联、字典项公共查询、编辑后缓存更新、编码不可变 | 新建专用资料，未改系统字典 |
| F04文件 | 5项：有效PDF上传/SM3匹配、原文下载、重命名、回收、原ID还原及内容一致 | 不代表浏览器文件选择或签章验签通过 |
| F05公告/消息 | 4项：草稿创建、用户发布列表不展示草稿、编辑仍为草稿；未读总量与列表/分类型合计一致 | 不广播、不发送短信邮件，不批量标记真实消息 |
| F08进销存资料 | 5项：客户/供应商、联系人增改删、两级分类与商品、引用删除保护、仓库负责人及地址回显 | 未执行库存、资金过账，不能算采购销售财务完整闭环 |
| F09报价 | 7项：字符串ID与客户关联、数量单价税额合计、部分修改保留明细、草稿/提交往返、HTML、Word/PDF导出、独立文档解析 | 三步导出/解析合计按7个用例登记；终态与兼容转工单未执行 |
| F10报修 | 前3项通过：创建→确认→转未接单工单；删除未接单工单恢复申请CONFIRM | 第4项空关闭原因应拒绝但实际成功；第5项因此未执行，等待修复复测 |

原在用包共44个不同业务用例PASS，F10-04存在真实FAIL。各组已通过真实接口补偿和回查：本轮用户/角色、部门/字典/字典项、PDF活动区与回收站、公告草稿、PSI资料、报价及报修/未接单工单均无活动残留；不直接删库或清空回收站。删除后PDF元数据为空、旧下载拒绝；原文与报价PDF均通过pdfinfo解析且各1页，DOCX通过ZIP CRC与document.xml解析。编号序列和合法操作审计保留，不回退共享序号，不宣称数据库物理零痕迹。

租户管理及调度接口实际返回各自“模块已禁用”提示，按本部署记N/A并保持开关，不恢复旧应用管理或代码生成。浏览器、摄像头、真实GPS和地图AK继续跳过；角色隔离、公告发布/定时通知、采购销售/收付款、完整维修结算、签署以及最终全部模块联合验收仍不能由本轮部分结果推导通过。

首轮有两处测试脚本问题保留失败记录后纠正：字典公共接口误写/common而实际为/system/common；删除文件元数据允许成功空值，清理断言改为“不暴露元数据且下载失败”。两处均在修正后独立回查清理通过，不能计为产品缺陷。

### 61.3 已定位修复与验证门禁

F10-04已复现：接口文档注明关闭已确认报修必须填写原因，PC原先仅确认弹框，Service未检查remark，空原因实际将CONFIRM变为CANCEL。本轮修复PC关闭原因输入、空白/500字符限制、请求保留字符串ID和重复提交保护；Service拒绝空白/超长原因并持久化trim后原因，保留原状态限制和权限。接口路径、数据库及国密实现未改。

报价新增真实响应ID为字符串，但PC API声明http.post<number>；修正为string，与现有详情/路由ID契约保持一致。原页面已调用String(newId)，本轮未将类型错误夸大为已发生的雪花ID截断事故。

新增后端3项回归覆盖关闭原因及原状态限制；新增PC4项行为回归，与原报价路由8项共12项全部通过。PC typecheck和生产构建通过（3735模块，Vite构建约81秒）。已有大包及sm-crypto浏览器external警告保留，未为消除警告修改依赖或国密逻辑。

初次检查点曾将后端package、候选启动及真实复测保留为进行中，后续已完成，见61.4。失败留证包括：Maven默认缓存路径误为C:/.m2；Windows沙箱在JDK关闭已读取的依赖JAR时发生toRealPath访问拒绝（JAR本身ZIP校验正常）；Docker只读元数据查询两次自动审批超时。随后使用显式既有Maven缓存和获批构建权限完成验证，未更换依赖、改全局环境、重置Docker或清空原卷。源码preimage、文件白名单、business-fixes.patch和各轮失败日志均保留在忽略目录。

### 61.4 完整门禁、候选验证与在用8000修复

2026-11-10完整reactor17个项目package成功，86个JUnit套件748项全部通过，失败/错误/跳过均0；最终日志backend-validation-final.log，完成时间00:14:44（东八区）。不是仅运行编译或跳过测试的打包。首轮完整单测曾在H5InvitationAuthTest并发结果等待3秒处超时；同一旧测试单独13项复测通过。随后只修正测试同步：等待两个线程就绪并共同抵达原子消费点、设置有界等待，保留并强化两次消费/恰好一次成功/一次会话写入断言。该13项在完整集再次通过；未修改生产H5鉴权、验证码或国密实现，也未将正确性测试当成3秒性能SLA。

候选包SHA256为1650bc7bc20568d96290a65f87e9ff9ae2f44114320f37821d229ccdea337649。18003专用候选实际启动，确认国密公钥指纹与原8000一致；报修5项真实用例及精确补偿全部通过，包含缺失/空白/501字符原因被拒绝且CONFIRM不变、正常关闭原因落库。只移除本轮候选容器，候选会话副本删除，未在此阶段替换原8000。

随后仅重建原ls-aegis-mcs Compose项目的backend，将同一已验证包用于8000，挂载.local/backend-v2-r61/ls-aegis-server.jar。实际容器dcdeea0e562141bf93eef09d7c3ced89c6d5d9d5ced2f7370077f4f4bf4a170f；其余数据、日志、字体挂载逐项与原容器比较不变，公钥指纹相同；没有DDL、原数据库/Redis重置或原卷删除。健康、同5项真实报修复测、PC/双H5只读冒烟9/9通过。旧v34包及原Compose保留作回退；失败自动恢复原包的分支已准备，本次未触发。

新包上重新执行用户/角色、部门/字典、PDF完整生命周期、公告草稿、未读计数、PSI资料、报价/导出与报修闭环，并重新进行正常PC加密登录至退出。合并同一构建的最终不同用例共52项PASS、0项最终FAIL；包括61.5的客户H5六项。每项含多项断言，不将同用例重复尝试相加，也不将原包44项或隔离18组冒充新包新增用例。最终清理台账核对8组业务合成资料补偿通过，PC/H5会话均正常退出，专用Token/邀请私有文件无保留；保留失败尝试及合法审计、编号序列，不宣称数据库物理零痕迹。本节验收时版本为2.1.0，源码、Agent/Skill及报告尚在工作区；后续用户授权升版与推送见第62节。

### 61.5 客户H5子流程、不可撤回过账边界与隔离补测

新增business_workflow_customer.cjs分阶段执行正常邀请、真实验证码、合成水印照片、客户报修、PC确认/带原因关闭、客户回显及退出。最终6个用例通过：F12-C01至C05加PHOTO；退出验证旧H5会话返回10007，客户/申请/活动与回收站照片精确回查为0。上传的是程序生成PNG，经实际服务端水印处理后以客户凭据鉴权预览，JPEG解析640×480；人工查看水印可辨识本轮合成客户、运行号、合成地址和时间。没有调用摄像头、真实GPS或外部地图。首轮脚本没提供必需照片而被拒绝，属于测试前置遗漏；页面和后端本就要求至少一张照片，未为测试放宽校验。修正前置并保留失败记录后重跑成功。

在用库确认采购入库/销售出库后会产生不可直接删除的库存与审计引用。MasterDataReferenceGuard明确保护包含耗尽批次及VOID审计头的引用；StockIn/StockOut公开Controller仅提供草稿增改删与确认，没有通用撤回接口。不能为测试清理删除审计或放开保护。本轮未在8000确认新的采购/销售过账；该部分在用闭环仍待可接受的审计保留测试方案，不能将此部署约束直接宣布为产品缺陷。

为继续验证业务逻辑，额外用同一SHA256包运行既有run_business_flows.py原隔离流程：随机标签da72da09a0、独立MySQL/Redis/后端/网络、仅回环随机端口57710、新建临时数据库及合成身份/测试证书。没有将脚本改指8000，也没有挂载原数据库、原GM数据或真实证书。隔离18组全部PASS、0 FAIL、0 BLOCKED，覆盖正常认证及身份上下文、采购入库/应付付款、销售FIFO/应收收款/缺货回滚、银行匹配与重复解除、现金汇总、报价状态/导出与兼容转单、工程师维修及结算不提前记实收/出库回滚、测试签署/验签、客户H5、工作台、商品导入批处理、消息未读与个人删除。SQL身份/资料夹具和持久化断言只发生在这个临时库；其验证码关闭只属于原隔离测试夹具，不是本机8000登录方法。

脚本finally按运行标签清理，本轮独立Docker复核确认该标签容器0、网络0、business-env.json已移除；原MySQL/Redis仍healthy，8000及媒体服务仍运行。18组是隔离真实API联动回归，不计作在用52项，不等于所有模块最终验收通过。仍未覆盖浏览器PC/双H5页面全量、真实硬件/地图、生产容量、共享库不可撤回过账、全部生产角色边界及生产证书/第三方签署。这些限制继续登记，后续按工作流逐项补足，不能从单测、只读健康或部分联动推导生产交付认证。

## 62. V2.2.0版本同步与Git发布

2026-11-10用户明确要求推送Git，并确认采用小版本V2.2.0。发布包含第60节尚未上传的4个本地清理提交，以及第61节业务流程Agent/Skill、真实测试执行器、报修关闭原因与报价ID修复、并发测试同步和本节版本元数据；普通提交，不改写main或既有标签，不自动添加工具共同作者。

根VERSION统一为2.2.0，PC/H5 package.json、H5 lock根两处、Maven revision、application.version、生产.env.example与四个项目镜像默认标签、README/当前规则/Dockerfile构建示例同步；新增PC更新日志条目。历史报告/旧更新日志及第三方pagehelper/npm依赖2.1.0保持原值，pnpm9锁文件没有项目版本字段，不做全仓字符串替换。version_consistency.mjs核对11处产品版本通过，第三方依赖锁内容经结构化对比未改变。

本次V2.2.0的PC类型检查与生产构建、客户H5/工程师H5生产构建均成功，前端12项行为回归通过；后端17项目离线package成功，JAR内Maven pom.properties与application配置均核对为2.2.0。此次版本打包显式使用-DskipTests，不能写作重新运行748项后端单测；业务源码/测试执行器与第61节验证哈希一致，沿用此前748项完整单测、52项在用实测及18组隔离实测的适用证据，保留原未测边界。构建仍有既有大包及crypto浏览器external提示，未通过修改依赖或国密实现掩盖警告。

V2.2.0后端构建包SHA256=3e8a0b5d2d330e77a00a74c01e9517aaebae0ea0a5de971e379b084a6bc2efe6。该包是本次升版产物；本次Git发布不替换本机在用8000的第61节2.1.0运行包，不能把新版本打包与既有运行包验收混为一体。版本升高也不等于所有生产场景已经验收。

发布按明确35文件白名单暂存，排除.local、数据卷、环境实值、证书、Token、日志、node_modules、target和dist；沿用既有维护者leason身份，版本提交和带注释v2.2.0标签指向同一提交。推送采用main与v2.2.0标签的普通原子推送，拒绝覆盖远端新提交和既有标签；远端最终hash以Git refs与本地发布回执核对为准。私有发布校验、提交白名单、构建日志和推送回执位于.local/releases/v2.2.0，不作为第二份正式报告，也不进入Git。
## 63. EOMS 原生 Android 双端工作流、Agent 与 Skill

2026-11-10基于 `ls-aegis-h5` 和 EOMS 后端源码建立客户服务 Android 与维修工程师 Android 的项目工作流，并按本轮确定的无短信登录方式实现客户自设用户名密码。当前仓库只有客户/工程师 H5 双模式，没有原生 Android 工程；本轮不代表 Android 应用已经开发或可安装。

工作流落在 `Docs/android-apps/WORKFLOW.md`，包括现有业务边界、客户和工程师旅程、建议的 Kotlin/Compose 双应用结构、平台登录与客户一次性激活/自设用户名密码、设备权限与数据安全、A-G 分阶段交付门禁、跨端财务库存验收以及尚待产品决策的事项。客户用户名凭据保存在新增 `eoms_h5_customer_credential` 表，使用平台 PasswordEncoder 摘要；激活/重置需管理员签发的短期邀请码，常规登录经平台 SM2 传输加密，配有图形验证码、IP 限流和账号失败锁定。客户/工程师会话彼此隔离，库存及财务继续由既有后端业务服务处理。

新增 `agents/android-apps/AGENTS.md` 作为实施 Agent 角色，新增 `skills/itls-android/SKILL.md` 与 `skills/itls-android/agents/openai.yaml` 作为显式路由的项目 Skill；根 `AGENTS.md` 与 README 已登记入口。新增 EOMS 客户凭据实体、Mapper、激活/密码登录接口、H5 登录与激活界面及客户凭据表 SQL 全量/增量路径，并更新 PC 发码说明。验收矩阵覆盖客户报修、工程师维修、PC/财务联合、会话和数据安全、设备体验。当前只进行静态文档/差异检查；未执行后端/H5 构建、测试、模拟器/真机或业务闭环验收。上线前需应用增量 Liquibase changeset；既有客户须由管理员发激活码建立账号。
## 64. 网站配置页布局优化

2026-11-10。原后台网站配置以单列显示 11 个字段，编辑操作位于表单底部，图片字段仅显示地址。本轮将现有字段分为平台信息、品牌图片、页脚与备案三组，编辑和保存移到页首，三个图片字段分别显示预览、地址及编辑态上传入口。

本地 Docker 管理端构建和重建成功。在已登录后台中查看了三个分区、现有图片预览和编辑态的三个上传入口，未上传文件或保存设置。本轮未量化性能变化。

同日调整分区为品牌图片、平台信息、页脚与备案。选图后由浏览器转换成 PNG 再上传，Logo 等比缩至长边最多 512 px，网站图标和公安备案图标分别居中补齐为 32 × 32 和 64 × 64 px。原图须可由当前浏览器解码。管理端构建通过，并在浏览器核对了新顺序、输出规格和上传入口；未执行实际上传或保存。

品牌图片预览修复：原文件已正确转换和保存，但静态文件请求未携带平台令牌，返回登录错误。保存前预览使用浏览器中的转换结果。保存后，仅当前品牌配置引用的上传 PNG 可匿名读取，其他上传文件仍走原鉴权。本地前后端镜像构建通过，后台与两端 H5 的代理均返回两张已配置 PNG，内容与磁盘文件一致。未配置的同格式路径仍返回登录错误。未重新上传或改写现有品牌设置。

## 65. H5 登录后入口页调整

2026-11-10。客户端和维修端登录成功后原先直接进入报修/任务列表，空数据时首屏只显示空状态，用户缺少下一步操作入口。本轮将登录后的首个路由改为各端已有的个人中心入口页：客户端进入“我的”，维修端进入“个人中心”。入口页保留现有身份信息、统计和功能按钮，用户点击“我的报修”“在线报修”或“我的任务”等按钮后再进入对应列表，未改变接口、权限或列表筛选。

修改后仅重建客户 H5 和维修端 H5 容器；两个容器健康检查通过。未重新打包 Android APK，因为 APK 继续加载这两个 H5 页面，页面更新可直接生效。未执行真实登录业务闭环，本轮验证范围为路由静态检查、H5 构建和容器健康状态。

## 66. H5 登录成功跳转稳定性修复

2026-11-10。反馈显示登录接口成功后，手机 WebView 仍停留在登录页。原实现依赖前端路由替换，部分 WebView 会保留旧登录文档。本轮在客户端和维修端完成会话写入后改为完整跳转到各自入口页，确保 localStorage 会话先落盘，再加载入口页；入口页中的列表仍需用户点击功能按钮后进入。

客户 H5 和维修 H5 构建通过，两个 Docker 容器健康检查通过。未重新打包 APK，因为 App 内加载的是这两个 H5 页面；本轮未使用真实账号执行登录闭环，仍需在手机上用测试账号确认一次成功登录后的跳转。

## 67. 客户报修与维修任务首屏布局优化

2026-11-10。根据反馈恢复登录后的默认页面：客户进入“我的报修”，维修人员进入“我的任务”。两页增加渐变页头、角色信息卡、快捷操作按钮和有数据/无数据两种明确状态；空列表不再只显示空白区域，客户可直接点击“立即提交报修”，维修人员可重新加载或切换待接单任务。列表、筛选、详情和底部导航仍沿用原有路由与接口。

客户 H5 和维修 H5 生产构建及 Docker 健康检查待本轮部署后记录；本轮未执行真实账号业务闭环。

## 68. 客户与维修端工作台入口

2026-11-10。根据反馈，登录后不再直接进入“我的报修”或“我的任务”列表，而是进入独立的“工作台”。客户工作台提供在线报修、我的报修及待处理/已确认/已转工单/已取消按钮；维修工作台提供待接单、已接单、维修中、待结算、已完成和全部任务按钮。点击按钮后跳转到原列表页并携带状态筛选，原接口、详情页和权限保持不变。

新增客户与维修工作台路由和页面，底部导航保留工作台与个人中心。客户 H5 和维修 H5 构建及容器健康检查待本轮部署后记录，未执行真实账号业务闭环。

## 69. 工作台常驻导航

2026-11-10。反馈从工作台进入列表后看不到工作台入口。本轮将客户端和维修端底部导航统一为“工作台 → 列表 → 我的/个人中心”，并修正当前选中项索引。工作台入口在报修列表、任务列表和个人中心页面持续可见，点击状态按钮进入列表后仍可返回或直接切回工作台。

## 70. 登录会话自动恢复

2026-11-10。正常逻辑调整为首次登录后保存短期会话，后续打开 App 直接恢复到工作台；主动退出、会话过期、后台撤销或清除 App 数据时才重新登录。客户和维修登录页现在启动时检查各自保存的会话令牌，有效时直接跳转工作台，不再每次打开都要求输入账号密码。令牌失效仍由原请求拦截器清理并返回登录页。

## 71. 状态卡片与独立状态页面

2026-11-10。工作台的报修状态和维修任务状态改为与常用功能一致的彩色卡片按钮。客户状态按钮进入独立的报修状态路由，维修状态按钮进入独立的任务状态路由；每个页面显示对应状态标题、说明、筛选后的列表和自己的返回工作台入口，不再让所有状态共用同一个默认列表页面。

## 72. 客户底部导航去重

2026-11-10。客户端底部导航统一为两个入口“工作台”和“我的”，移除列表页与个人中心中重复的“我的报修”底部按钮及个人中心内重复的报修功能菜单，避免两个入口之间产生空白可点击区域和错误跳转。报修入口及状态按钮统一保留在工作台。

## 73. 报修状态页视觉与当前状态导航

2026-11-10。客户报修状态页顶部改为当前状态对应的颜色块，状态名称居中显示并移除重复说明和前置圆点。无数据时使用纵向白色内容框承载空状态提示；底部导航在工作台和我的之间显示当前状态短名称，明确用户当前所在的独立状态页面。

## 74. 报修状态页层级调整

2026-11-10。状态页导航栏不再重复显示状态标题；状态颜色块内居中显示状态名称，下面补充该模块的简短说明。白色内容区从状态块下方连续铺到下方导航上方，所有客户报修状态统一使用该布局。客户 H5 已重新构建部署，容器健康检查通过。

## 75. 客户工作台计数与个人中心重排

2026-11-10。工作台报修状态卡片右侧增加当前状态数量，数量通过客户报修列表接口按状态读取。修复“我的报修”空状态路由误拼为 `/status/` 导致的 404。客户个人中心改为与工作台一致的欢迎卡片风格，账户、关于系统、退出登录固定在页面下方，移除重复报修功能菜单。

## 76. 客户统计方块与全部报修专属页面

2026-11-10。客户个人中心在客户信息卡片下恢复五个统计方块：全部报修、待处理、已确认、已转工单、已取消。工作台“我的报修”改为新的“全部报修”专属页面，沿用状态页的彩色标题、白色内容区、状态标签和底部导航，不再进入旧的公共报修列表路由。

## 77. 工程师端按客户端视觉统一

2026-11-10。工程师工作台状态卡片增加各状态任务数量；工程师个人中心改为与客户工作台一致的欢迎卡片、统计方块和底部账户区，移除重复任务功能菜单。工程师状态列表继续保留原有任务接口和权限逻辑。本轮未改变维修状态机或后端接口。

## 78. 本机统一Compose重建与数据保留验收

2026-11-10。按用户授权，将本机原「MySQL/Redis Compose + 独立后端容器 + 三个 Vite 进程」统一为一个 `ls-aegis` Compose 项目。业务源码基于 `b21f54c`；产品元数据和镜像标签保持 V2.2.0，没有升版、提交或推送。

### 部署与修复

- 新增 `docker-compose.local.yml`，叠加根生产 Compose，在一个项目内管理 MySQL、Redis、后端、PC、客户 H5、工程师 H5 和原媒体占位容器；未建立第二套数据库或认证服务。
- 四个业务镜像均由现有多阶段 Dockerfile 从源码构建，后端 JDK 21/Maven、PC pnpm 9 类型检查及构建、两端 H5 分角色构建均通过。MySQL 8.0.42、Redis 7.2.8 使用原版本基础镜像重建容器，没有升级数据库引擎或清空 Docker。
- 修复根 Compose 的 PC 健康检查：`nginx:1.27.0` 不包含 `wget`，实际包含 `curl`；改为 `curl` 后 PC 健康检查通过。首次等待未通过时保留并恢复了原应用入口，修正后重新切换成功。
- 覆盖配置将原 dev profile 的数据库驱动与根 Compose 的 MySQL JDBC URL 对齐；保留原有本机账号和配置语义，不将此次本机部署宣称为公网生产安全验收。
- 原媒体容器仅执行 `/bin/sleep infinity`、无网络、无数据挂载；在统一 Compose 中原样保留占位行为。它不是已配置的流媒体/GB28181 服务，本轮没有启用或改动相关协议。
- README 与维护 Skill 已补充统一命令、端口、数据目录约束及 Android WebView 测试工程的实际状态。停止旧 Vite 后入口由 Nginx 提供，不再依赖前端开发服务器。

### 数据保留核对

MySQL 继续使用外部卷 `ls-aegis_mysql-data`，Redis 继续使用 `ls-aegis_redis-data`；后端继续挂载原 `docker/data/ls-aegis-server/data` 和日志目录。Redis 配置文件改为只读挂载。Docker Desktop 对 Windows bind 路径的两种等价表示已核对，实际源文件未改变。

迁移前已在被 Git 忽略的 `.local/unified-deploy/` 保存私有数据库逻辑备份（14,274,916 字节）、115 张表行数及数据文件指纹。迁移后除运行访问产生的 `sys_log` 增长外，其余表行数一致；现存数据文件哈希、国密公钥身份及数据库/Redis 数据卷身份均一致。原附件目录继续使用；未重置数据卷、未执行 init.sql、未重放 Liquibase master、未轮换密钥。校验不代表对每条数据库记录做逐字段比对。凭据与备份均未写入 Git。

### 运行与验证结果

| 范围 | 结果 |
|---|---|
| Compose 解析与存储/凭据对照 | 7 个服务配置通过；原凭据、外部数据卷、数据目录和密钥路径一致 |
| 容器状态 | MySQL、Redis、后端、PC、客户 H5、工程师 H5 均 running/healthy；媒体占位容器 running，无业务健康探针 |
| 既有 L0 冒烟 | 9/9 通过 |
| 扩展部署检查 | 882/882 通过：20 项页面/API/身份/鉴权/验证码检查，862 个 JS/MJS/CSS/WASM 静态资源 HTTP 与 MIME 检查 |
| 鉴权语义 | PC/工程师未登录请求被拦截；客户报修列表返回既有 `10007` 令牌无效响应，data 为空；未修改业务错误码 |
| 页面入口 | PC `/system/dept`、客户 `/customer/auth`、工程师 `/engineer/login` 及 25683/25684/25685 别名入口返回 SPA 页面，无 Vite client |
| 版本与源码检查 | 版本一致性 11 处通过，保持 2.2.0；差异格式检查通过 |
| 收尾 | 已删除两个校验身份后的停用旧容器，保留数据卷与镜像；未执行全局 prune |

本轮没有执行登录后的业务写入闭环、浏览器交互或 Android 安装/模拟器测试；后端镜像打包明确使用 `-DskipTests`，不能当作新增单元测试通过证明。验证码检查只产生短期缓存，未创建待清理业务单据。Android 测试工程现有 25684/25685 入口现已对应容器服务，但仍需设备侧 ADB 转发和安装才能启动 App。

从仓库根统一管理（Compose 2.24.4+，需原外部卷和 `docker/.env` 已存在）：

```powershell
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml up -d --build
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml ps
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml logs --tail 100 ls-aegis-server
```

本机入口：PC `http://localhost:5173`，客户 `http://localhost:5174/customer/auth`，工程师 `http://localhost:5175/engineer/login`，API `http://localhost:8000`。兼容入口为 PC 25683、客户 25684、工程师 25685、API 25682；均仅绑定本机。后续启动不应再并行运行旧 Vite/独立后端，也不应使用 `down -v` 或初始化 SQL 处理已有库。

## 79. 统一部署复查与Android双端模拟器启动

2026-11-10。按用户要求重新检查全部服务，并在本机 Android 模拟器实际安装、启动仓库已有的客户/工程师 WebView App；不将其表述为已完成原生 Kotlin 业务客户端。

### 服务与构建

- 统一 `ls-aegis` Compose 下 MySQL、Redis、后端、PC、两端 H5 共六个容器均 healthy；媒体容器仍为 running 的空闲占位，未启用流媒体业务。重新运行 L0 冒烟 9/9 通过。
- 原本机无 Android SDK/模拟器，现将工具、AVD、调试签名、Gradle 缓存及证据集中在被 Git 忽略的 `.local/android/`。从 [Google 官方下载页](https://developer.android.com/studio) 获取命令行工具并核对公布的 SHA-256，再通过 SDK Manager 安装 Platform/Build Tools 35、platform-tools、emulator 和 Android 35 Google APIs x86_64 镜像；未修改系统全局环境或启用 Windows 功能，未重启电脑。
- 实际加速检测返回 WHPX 可用。创建专用 `ITLS_API35` Pixel 5 AVD，序列号 `emulator-5554`，2 核/2048 MB、软件渲染，摄像头及音频关闭。窗口标题确认为 `Android Emulator - ITLS_API35:5554`。
- 修复仓库缺少标准 Gradle Wrapper JAR 的构建缺陷：从校验后的官方 Gradle 8.11.1 生成 Wrapper，补充分发包 SHA-256、Wrapper JAR 的精确 Git 忽略例外、Unix 启动脚本 LF 规则及本机 `local.properties` 忽略规则。未修改 AGP、业务 API 或产品版本。
- JDK 21 + Gradle 8.11.1 首次构建两端 APK 通过；随后用仓库 `gradlew.bat` 重复构建通过（62 个任务 up-to-date），独立 Wrapper 版本检查通过。保留已有 AGP/SDK XML 版本兼容警告，不将其误报为编译失败。

### 模拟器实测

| 检查 | 结果 |
|---|---|
| Android 开机 | `sys.boot_completed=1`；首次开机包管理未就绪时的安装失败，在系统完成启动后重试成功，无清空设备操作 |
| 客户 APK | `customer-app/build/outputs/apk/debug/customer-app-debug.apk`，78,266 字节；安装 Success，`am start -W` Status ok |
| 工程师 APK | `engineer-app/build/outputs/apk/debug/engineer-app-debug.apk`，78,278 字节；安装 Success，`am start -W` Status ok |
| App 会话隔离 | 两个独立 applicationId，启动后各自进程存在；本轮未登录，不声明登录后会话隔离用例已完成 |
| 网络接入 | 两条 ADB reverse 将设备 25684/25685 分别转发到现有客户/工程师 H5 容器，不开放局域网端口 |
| 页面 | 实际截图确认客户账号登录页及工程师登录页均有品牌图、输入框、服务端验证码和登录按钮，没有网络错误页 |
| 崩溃 | 模拟器 crash buffer 为空；截图、界面结构与构建日志保存在 `.local/android/` |
| 当前状态 | 两端共用一台模拟器，工程师 App 在前台，客户 App 在后台；可从 Android 最近任务或桌面切换 |

两端现有调试包版本均为 `0.2.0-real`（versionCode 2），平台产品版本仍为 V2.2.0，本轮没有擅自改版。Android README 已修正只适用于 Mac 的旧说明、移除不存在的预制 APK 路径，并补充本机环境、构建、模拟器及 ADB 启动命令。

本轮仅完成构建、安装、启动、页面渲染和现有部署连通性检查，未输入账号、提交验证码、执行登录后的维修/财务写入或完整业务闭环，未创建业务测试单据。摄像头、GPS、地图 AK 和真机测试继续跳过；数据库、附件和密钥未重置或迁移。所有改动留在本地，未提交或推送 Git。

## 80. ls-aegis-oa目录文档整理工作流与发布准备规范

2026-11-10。用户拟重新维护「ls-aegis-oa 管理平台 1.0」，要求先提供本地目录与全部文档的整理建议、工作流 Agent 和 Skill。本轮仅完成规范生成与初步只读盘点，没有启动批量删除、目录迁移、改版或新仓库发布。

交付：

- `agents/repo-organize/AGENTS.md`：负责基线、文件证据分类、文档重编、批次验证、回退和候选发布调度，由当前 Agent 执行，不默认派生代理。
- `skills/itls-repo-organize/SKILL.md`：区分建议/整理执行/新仓库发布权限；定义文件分类、现有文档规则迁移、运行数据保护与验证方式。
- `skills/itls-repo-organize/references/workflow.md`：阶段 0—7 的产出与退出条件，候选目录树、文件与 Git 边界、批次验证及名称/版本矩阵。
- Skill 的 `agents/openai.yaml` 提供界面元数据；根 `AGENTS.md` 已添加显式路由。

创建前盘点：Git 跟踪的文档类文件 53 个（md/mdc/txt/docx/pdf），`Docs/` 下日期任务目录 23 个，根目录日志 6 个，原报告合集 5,478 行。此统计不代表这些文件全部可删，也不是完整语义审查结果。

建议顺序：锁定现有修改和运行基线 → 全量目录/文档证据盘点 → 设计现行说明与历史归档 → 按批次整理并修正引用 → 验证工程和部署入口 → 实施新名称/版本 → 隔离候选构建与部署 → 按授权发布新仓库。优先整理文档和临时产物，保留源码目录名称，避免一次性重命名 Java 包、构建路径或数据库契约。

候选文档结构使用现有大小写 `Docs/`，规划 guides/business/architecture/operations/testing/releases/archive；尚未创建这些目录。现有 `rules/报告规范.mdc` 仍要求单一正式报告，因此实施分层文档时应先同步规范、Agent/Skill 路由和引用，再迁移内容，避免提前产生平行正式文档。

当前产品仍为 V2.2.0，拟议新仓库 1.0 的工程版本建议为 1.0.0，未执行变更。工作流区分新产品版本、原基座标识、历史记录、第三方版本及数据库迁移标识；不会通过全局字符串替换降版。新仓库地址、历史保留方式及实际发布行为在发布阶段按授权落实，不能默认删除旧 Git 历史或强推。

验证：使用 skill-creator 的 `quick_validate.py` 通过；Skill 的本地链接与界面元数据校验通过；`git diff --check` 通过。仅修改规范与文档，未触及运行目录、业务代码、数据、Docker 服务或 Android 模拟器，无需为此重启服务或执行业务写入。已保留本轮开始前的全部未提交修改；尚未提交或推送 Git。

## 81. 目录文档整理阶段0基线与阶段1完整盘点

2026-11-10，运行号 `20261110_210705`。按用户授权完成阶段0基线锁定和阶段1文件/文档盘点。本轮没有移动、归档或删除任何源文件/运行文件，没有切换版本或修改 Git 远端。当前版本仍为 2.2.0，新项目 `ls-aegis-oa` 的 1.0.0 尚未实施。

### 基线与保护范围

工作区为 `D:/oa.ls.hb.cn/ls-aegis-mcs`，上层工作区仅发现此项目目录。当前分支 main，HEAD 为 `b21f54c5c9be7a4a9e666112255195d9524f1e06`，远端仍为原 `itlslixinyu/ls-aegis-mcs`，未访问或改写远端。

开始时存在15个已修改/新建的现有文件，涉及上一轮部署、Android、工作流规范等。已在被 Git 忽略的 `.local/repo-organize/20261110_210705/` 保存原状态、文件副本、SHA-256 与差异 patch。该基线是此前成果的保护快照，不是 HEAD 干净工作树；不得用 reset/clean 将这些成果当作垃圾清除。

统一 Compose 项目仍为 `ls-aegis`：7个原容器均运行，MySQL/Redis/后端/PC/两端H5共6个 healthy，媒体为空闲占位。记录了容器ID、镜像ID、端口与挂载，不保存环境变量值。MySQL 与 Redis 分别保持外部卷 `ls-aegis_mysql-data`、`ls-aegis_redis-data`；后端原 data、logs 和字体挂载原位保留。没有读取真实 .env、密钥、附件、数据库内容或私有日志正文。

专用 Android 模拟器 `ITLS_API35` / `emulator-5554` 保持连接，ADB 25684/25685 反向转发已记录，SDK/AVD 与相关进程路径列入保护。`.local/android`、`.local/toolchains`、`.local/unified-deploy`（含数据库备份）、`.local/business-workflow`、`.git` 等不作为普通缓存清空。

### 四类清单

| 类别 | 台账条目 | 说明与建议 |
|---|---:|---|
| 保留 | 2,565 | 当前源码、SQL迁移、锁文件、工程规范、有效文档，以及受保护运行/构建目录；“保留”不代表逐函数死代码审查已通过 |
| 归档 | 30 | 23个历史日期任务目录、6个根目录旧Vite日志、1份前端旧版CHANGELOG；均仅候选，未实际移动 |
| 可删 | 2 | `.local/__pycache__` 目录和 `scripts/quality/__pycache__/cleanup_baseline.cpython-313.pyc`；是可重建Python缓存，本轮仍保留原位 |
| 待复核 | 1,731 | 1,651个.local直属私有文件、74个历史构建/探针等目录、6项当前文档或编辑器目录问题；未凭旧命名判无用 |
| 合计 | 4,328 | 4,191个逐文件记录及137个按目录聚合登记的范围，不是磁盘全部物理文件总数 |

全部2,524个Git跟踪路径均逐项登记，工作树盘点没有扫描权限错误。运行数据、SDK/AVD、依赖、构建产物、Git对象及私有历史子树按目录登记，不递归读取其中内容；因此不提供虚假的“全磁盘文件数”或可释放空间估算。

`.local` 的待复核条目已单独分组，不能将1,731项理解为业务缺陷。阶段2优先复核6项当前文档/编辑器问题，再按历史构建、测试证据和私有资料逐组处理。历史JAR、迁移记录、签署/测试证据和密钥配置一律不能仅因日期较早而删除。

### 文档盘点与已发现问题

当前可维护文档56份，基线报告含80个一级编号章节，均生成元数据/标题清单。检查22个可静态识别的本地Markdown文件路径链接，未发现路径缺失；这不是所有锚点、代码块示例、外链和操作命令均有效的证明。当前文档字节级相同分组为0，不据此宣称语义没有重复。

| 文件/范围 | 证据与下一步 |
|---|---|
| `docker/nginx/README.md` | 第10/11行仍使用 `docker compose build/up ... web`，实际服务名为 `ls-aegis-web`；挂载与镜像内配置的区别也需重写 |
| `docker/nginx/web/README.md` | 末尾同样使用旧服务名 `web`；保留其“可选静态挂载点”用途说明，不能整份直接删除 |
| `ls-aegis-server/README.md` | 模块表缺报价/EOMS/签章等现有模块；存量库升级描述需与受限迁移和实际关闭master的部署一致 |
| `scripts/README.md` | 示例含特定服务器/IP/域名，需改成通用占位值，并明确本机统一Compose与服务器安装流程 |
| `Docs/Reports-README.md` | 混合历史结果与当前操作说明；已生成逐章节索引，阶段2再确定内容迁移，不把旧验收提升为新版本验收 |
| `.cursor/` | 当前为空目录；根rules已存在，但仍列待复核，不按编辑器名称直接删除；受保护的`.agents/`原位保留 |
| 三份 `MIGRATION-R29/R31/R32.md` | 当前README或对应迁移脚本仍引用，确认保留，不能按旧轮次名称归档删除 |
| 国密/字体/天气图标/设计说明 | 分别属于基座协议、导出字体和前端资源约定，原位保留；不以“全部文档集中”破坏资源旁说明 |

此阶段完成“盘点与分类”，不是56份文档逐条业务真实性审定。重编、去重与新文档结构实施在阶段2/3执行，当前单一正式报告规则尚未改动。

### 产物与验证

新增可复用只读工具 `scripts/quality/repo_organize_inventory.py`。它要求已有基线和规范运行号，只能将产物写入本项目 `.local/repo-organize/<run-id>/`，不提供删除、移动或Docker操作入口；不跟随符号链接/junction，避免扫描到其他工作区。

本地私有产物包括 `baseline.json`、`preexisting-files/`、`preexisting.patch`、`runtime-before.json`、`project-processes.json`、`inventory.csv`、`keep.csv`、`archive.csv`、`deletable.csv`、`review.csv`、`review-groups.json`、`documents.csv/json`、`document-links.json`、`duplicate-documents.json`、`report-sections.json`。CSV含原路径、目标建议、分类、证据、风险、动作、验证和回退字段。它们不是新GitHub仓库的发布内容，也不是自动执行删除的脚本。

验证覆盖所有跟踪路径，分类总数自洽，保护目录仅聚合登记，文档路径链接检查完成，脚本实际执行通过。既有9项HTTP/API只读冒烟通过；没有执行业务写入或本轮新增业务数据清理，也没有为盘点重编译/重启应用。下一阶段应先审阅当前文档问题和80章报告的内容映射，再实施低风险整理。尚未开始阶段2—7，未提交或推送Git。

收尾验证：用私有合成目录测试了禁止读取敏感内容、保护目录不展开、同名build目录中的Git跟踪源码仍单独登记及输出不泄露合成秘密标记，均通过。此前15个文件内容保持，旧报告正文仅增加内容。对照 `runtime-before.json`/`runtime-after.json`，7个容器的ID、镜像、挂载、端口、健康和重启次数完全一致，AVD身份一致；末次冒烟仍9/9。详细结果保存在 `validation.json` 与运行台账中。报告章节索引在写入本节后包含81章，其中新增本节是本次盘点记录，不属于此前80章历史迁移范围。
