# 项目文档

本文档仅维护当前版本的使用、开发和运维说明。每个主题保留一个权威入口。

| 主题 | 入口 |
|---|---|
| 开发环境与构建 | [开发指南](guides/development.md) |
| 功能范围与跨模块流程 | [业务流程](business/overview.md) |
| 工程分层、权限与数据边界 | [平台架构](architecture/platform.md) |
| 环境配置与部署 | [部署指南](operations/deployment.md) |
| 数据、密钥与恢复 | [数据保护](operations/data-and-recovery.md) |
| 测试方法与验收范围 | [验证指南](testing/verification.md) |
| 版本变更 | [1.0.0](releases/1.0.0.md) |

专项说明：[Android构建](../android-demo/README.md)、[Android实施工作流](android-apps/WORKFLOW.md)、[数据库迁移](operations/deployment.md#数据库升级)、[国密模块](../ls-aegis-server/ls-aegis-crypto/README.md)、[字体要求](../docker/ls-aegis-server/fonts/README.md)。

工程规范由根[AGENTS.md](../AGENTS.md)路由到 rules/、agents/ 和 skills/。开发流水、逐轮测试结果、临时方案、截图和运行证据仅保存在被忽略的 `.local/`，不纳入项目文档。
