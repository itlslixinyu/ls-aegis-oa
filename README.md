# LS·Aegis OA 管理平台 1.0

基于 LS·Aegis 统一身份、RBAC 和国密底座的业务平台，包含进销存、运维维修、报价及电子签章。PC、客户H5、工程师H5和Android测试入口共用既有后端。

工程版本以 [VERSION](VERSION) 为准。新仓库候选名称为 `ls-aegis-oa`，名称与版本的实施/验证状态见[发行入口](Docs/releases/README.md)。

| 工程 | 技术与职责 |
|---|---|
| ls-aegis-server | JDK21 / Spring Boot3 / Maven多模块，ls-aegis-boot统一启动 |
| ls-aegis-web | Vue3 / Vite / Arco管理端 |
| ls-aegis-h5 | Vue / Vite / Vant，客户/工程师分端构建 |
| android-demo | 两个Android WebView测试工程；不等同于完整原生客户端 |
| docker / 根Compose | 环境配置、镜像和统一服务编排 |
| scripts / tests | 构建、迁移、验证与业务流程测试 |
| agents / skills / rules | 项目协作角色、工作流与工程规范 |
| Docs | 当前指南、发行记录与历史归档 |

## 使用入口

- [全部文档](Docs/README.md)
- [开发与构建](Docs/guides/development.md)
- [模块与业务联动](Docs/business/overview.md)
- [架构及数据边界](Docs/architecture/platform.md)
- [部署和环境选择](Docs/operations/deployment.md)
- [数据、密钥与恢复](Docs/operations/data-and-recovery.md)
- [验证与未测范围](Docs/testing/verification.md)
- [Android构建及模拟器](android-demo/README.md)

## 当前本机完整部署

从仓库根执行，要求已有配置、外部数据卷和Compose2.24.4+：

```powershell
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml ps
```

PC：`http://localhost:5173`；客户：`http://localhost:5174/customer/auth`；工程师：`http://localhost:5175/engineer/login`；API：`http://localhost:8000`。兼容移动入口为25684/25685。新服务器不能直接照用已有本机数据覆盖配置，具体启动命令和差异只维护在部署指南。

## 业务约束

这是一个平台，功能域不另建数据库和认证栈。维修结算生成应收，实际收款才产生实收；库存由PSI单据服务维护。数据权限、事务、幂等和ID精度不能因整理目录而省略。代码生成和应用管理已经移除。

数据库全新安装只用 `ls-aegis-server/sql/init.sql`；已有库走适用的增量升级。Git不携带业务数据库、附件、密钥、真实环境配置或本机AVD。

## 维护

先读 [AGENTS.md](AGENTS.md)，按任务加载根 skills/ 的对应工作流；不假定编辑器会自动发现。新维护记录写入 [Docs/releases/maintenance.md](Docs/releases/maintenance.md)。旧报告入口 [Reports-README](Docs/Reports-README.md) 只用于追溯历史，不作为当前操作指南。
