# LS·Aegis 雷铄 OA 安全应用构建平台 · 后端

前后端分离后端工程（Maven 多模块）。前端同级目录：`../ls-aegis-web`。

## 模块

| 模块 | 包名 | 说明 |
|------|------|------|
| ls-aegis-crypto | `com.ls.aegis.crypto` | 国密 SM2/SM3/SM4（可配置关闭） |
| ls-aegis-common | `com.ls.aegis.common` | 工具、常量、枚举、API、异常、用户上下文、校验分组 |
| ls-aegis-mybatis | `com.ls.aegis.mybatis` | 实体/Service 基类、MP 配置 |
| ls-aegis-redis | `com.ls.aegis.redis` | 缓存常量、Redis 装配 |
| ls-aegis-security | `com.ls.aegis.security` | Sa-Token、权限拦截、Controller 基类 |
| ls-aegis-starter | `com.ls.aegis.starter` | 核心自动装配（不含 rbac/biz，避免循环依赖） |
| ls-aegis-rbac | `com.ls.aegis.rbac` | 登录认证、用户/角色/菜单/部门/字典/审计 |
| ls-aegis-biz-file | `com.ls.aegis.biz.file` | 文件与存储 |
| ls-aegis-biz-notice | `com.ls.aegis.biz.notice` | 公告与站内消息 |
| ls-aegis-biz-tenant | `com.ls.aegis.biz.tenant` | 租户管理；默认关闭（中间件 `CONTINEW_STARTER_TENANT_ENABLED=false` + 功能模块「租户管理」关） |
| ls-aegis-biz-schedule | `com.ls.aegis.biz.schedule` | 任务调度 |
| ls-aegis-boot | `com.ls.aegis.boot` | 启动入口 `LsAegisApplication` |
| ls-aegis-biz-psi | `com.ls.aegis.biz.psi` | 进销存 |
| ls-aegis-biz-quote | `com.ls.aegis.biz.quote` | 报价、归档、导出和转工单兼容入口 |
| ls-aegis-biz-eoms | `com.ls.aegis.biz.eoms` | 运维报修、工单、维修记录及结算 |
| ls-aegis-biz-sign | `com.ls.aegis.biz.sign` | 项目/文件/印章、签署、回执与验真 |

部署 SQL：全新安装只用 `sql/init.sql`（已含进销存/运维/签章/报价）。已上线库使用经核对的增量迁移，不重跑全量SQL。当前Docker部署关闭完整Liquibase master，受限迁移入口的适用范围见[部署指南](../Docs/operations/deployment.md)，不要直接启用master替代迁移审核。

## 依赖关系（简）

```
ls-aegis-boot → ls-aegis-rbac / 各 ls-aegis-biz-* / ls-aegis-starter
ls-aegis-biz-* → ls-aegis-starter（核心能力）
ls-aegis-rbac → ls-aegis-starter + ls-aegis-biz-file/notice（跨域协作）
ls-aegis-starter → common / mybatis / redis / security / crypto
```

## 编译

```bash
mvn -DskipTests compile
```

## 数据库变更

Liquibase master（`db.changelog-master.yaml`）**当前仅启用 MySQL**；`postgresql/` 下脚本保留供方言切换，不含增量 patch。存量 MySQL 库升级依赖 `mysql/patch_*.sql`。
