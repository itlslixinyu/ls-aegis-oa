# 构建与部署脚本

环境选择、端口及完整部署命令统一维护在[部署指南](../Docs/operations/deployment.md)，数据备份与恢复边界见[数据保护](../Docs/operations/data-and-recovery.md)。本目录不再重复特定服务器地址或提供直接清空已有库的建议。

| 入口 | 用途 |
|---|---|
| build-artifacts.ps1 | 本机Maven/PC/两端H5预构建，后端默认跳过测试；生产源码镜像仍需构建 |
| install.sh / deploy.sh | 新服务器生产安装/更新，需先明确目标和真实.env |
| init-env.sh | 按服务器安装流程准备环境配置；不对在用本机配置重新生成密钥 |
| migrate-r29.sh / migrate-r31.sh / migrate-r32.sh | 只处理对应受限增量补丁，先读各迁移说明 |
| backup.sh | 按脚本约定的生产布局备份，使用前核对数据库与挂载，不默认覆盖本机external卷 |
| resource-alert.sh | 资源检查；安装cron属于单独运维动作 |
| setup-nginx-bt.sh | 服务器网关配置；域名使用自己的部署值，不在本机随意执行 |
| quality/ | 盘点、构建约束、单测和隔离/在用测试入口，详见[验证指南](../Docs/testing/verification.md) |

根目录 pack.ps1/pack.bat 负责源码打包。实际产物以脚本输出为准，部署包、真实.env、pack.config.json和部署元数据不进入Git。脚本名称包含旧轮次不代表无用，先核对调用关系和目标再整理。
