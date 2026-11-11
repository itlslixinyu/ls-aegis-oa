---
name: itls-maintain
description: 开发、修复和运行 LS·Aegis 雷铄 OA 安全应用构建平台 仓库，处理 PC、H5、Spring Boot、MySQL 与 Redis 的项目约定和变更验证。
---

# LS·Aegis 雷铄 OA 安全应用构建平台 开发维护

升版后执行 `node scripts/quality/version_consistency.mjs`，核对根VERSION、PC/H5及锁文件根信息、Maven/application和生产镜像默认标签；普通修复仍按用户授权决定是否升版。

先读根 `AGENTS.md`，定位本次功能域；沿前端 `apis/views` → Controller → Service → Mapper/SQL 跟踪，避免只改页面掩盖服务错误。

日常启动优先复用已有环境。开发依赖使用 `docker/dev-up.ps1`；本机后端使用 `docker/dev-run-server.ps1`，需 JDK 21 + Maven。PC 用锁文件匹配的 pnpm（当前 lockfile 9），H5 用 `npm ci`。先检查端口和 Compose 项目名，不能停止其他 checkout 的服务。

本机若已切换完整容器部署，统一使用 `docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml up -d --build`，不要再同时启动占用 5173/5174/5175 的 Vite 或旧独立后端。该覆盖文件要求 Compose 2.24.4+，保留本机 dev 配置语义，显式复用已有 external MySQL/Redis 卷和附件/国密目录；不是新机器初始化或公网生产配置。禁止通过 `down -v`、卷清理或重跑 init.sql 解决启动问题。迁移前后核对实际挂载、表行数和密钥指纹。

PC 验证用 `pnpm typecheck` 或 `pnpm build`；H5 变更分别 `npm run build:customer`、`npm run build:engineer`。后端从父目录执行 `mvn -pl <变更模块> -am test`；打包使用 `mvn -pl ls-aegis-boot -am package -DskipTests`，必须明确这条命令跳过测试。冷启动可先运行 `python scripts/quality/service_smoke.py`。

保持 UI 与接口契约、国密与权限链一致；涉及库存、结算、报价转工单时同时使用业务测试 Skill。变更 SQL 时检查全量安装与升级路径，不能修改已执行 changeset 解决本地问题。

交付说明改动、验证与未覆盖项，详细记录保存在本地 `.local/maintain/<run-id>/`，Git只更新受影响的现行说明。
