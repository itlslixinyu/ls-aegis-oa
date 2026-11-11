# 架构与业务文档

本目录描述当前产品的工程组成、业务关系与运行边界。产品展示名称为 LS·Aegis 雷铄 OA 安全应用构建平台，技术模块名称沿用 LS·Aegis 底座。

| 文档 | 内容 |
|---|---|
| [平台架构](architecture/platform.md) | 多端入口、模块分层、公共能力与数据访问 |
| [业务流程](business/overview.md) | 功能域职责、状态流转及跨模块协作 |
| [开发与构建](guides/development.md) | 工程依赖、构建链和配置来源 |
| [部署架构](operations/deployment.md) | 环境分层、网络路由、服务依赖与数据库升级 |
| [数据与密钥](operations/data-and-recovery.md) | 持久化边界、备份关系与恢复顺序 |
| [接口与数据一致性](architecture/contracts.md) | 身份、授权、响应、幂等及文件访问契约 |
| [Android应用架构](../android-demo/README.md) | 双端模块、WebView入口和平台对接 |
| [版本说明](releases/1.0.0.md) | 当前版本范围与升级要求 |

工程协作规范由根[AGENTS.md](../AGENTS.md)路由；数据库受限迁移和国密模块的专项说明在相关源码目录就近维护。
