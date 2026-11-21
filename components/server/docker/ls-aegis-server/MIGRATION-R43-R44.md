# R43 / R44受限数据库升级

本页用于已有MySQL 8数据库的受限升级；全新安装使用 `ls-aegis-server/sql/init.sql`。不启动Spring，不执行完整Liquibase master或种子同步。

| 入口 | 固定范围 |
|---|---|
| `scripts/migrate-r43.sh` | EOMS合同sign_date可空日期字段；已有定义必须与固定变更匹配 |
| `scripts/migrate-r44.sh` | 已移除代码生成/应用管理的受限退役；执行前由CLI检查固定范围及保护条件 |

先备份数据库，在隔离副本验证目标版本和回退方案，再由维护人员执行。R44涉及退役清理，不可当作通用清库工具，拒绝条件不得绕过。

1. 准备包含相应CLI的本地镜像、已有数据库网络，以及仅供本次迁移的数据库身份env文件。
2. 明确设置 `MIGRATION_IMAGE`、`MIGRATION_NETWORK`、`MIGRATION_ENV_FILE`；不要打印或提交env内容。
3. 执行 `bash scripts/migrate-r43.sh --help` 或对应R44脚本查看参数。
4. 先运行 `validate` 和 `status`，显式传入 `--target 主机:端口/库名`；必须与env配置的目标一致。
5. 核对范围后，才运行 `apply --target 主机:端口/库名 --acknowledge-write`，完成后再次validate/status。

脚本不构建镜像、不启动依赖、不发布端口、不挂载应用附件或密钥。未实际执行本次升级时，不把status或源码检查记为迁移成功。固定发布资源与历史校验值不得随意改写；SQL、字段和拒绝条件以相应CLI及其固定资源为准。
