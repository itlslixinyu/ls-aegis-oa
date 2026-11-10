# R29 MySQL 增量升级

本入口只执行候选 JAR 内固定的四个补丁：`patch_stock_mutation_lock.sql`、`patch_psi_finance_compensation.sql`、`patch_eoms_finance_compensation.sql`、`patch_stock_adjust_restore.sql`，共52个 changeset。它不会启动 Spring，不加载 master、main_table、main_data 或其他种子，不执行 changelogSync，不推断旧库完整基线。原 changeset ID、author、filename 保持不变；既有历史校验和不同或缺失时拒绝继续。

## 前置条件

- MySQL 8，数据库已经按本项目旧版结构或当前 `sql/init.sql` 安装；空库不走增量入口。
- 已验证可恢复的备份，已核对目标环境，安排业务暂停写入的升级窗口。DDL可能隐式提交，失败时不得声称自动回滚。
- 准备包含 `R29MigrationCli` 的新候选生产镜像。脚本不会自动构建、拉起依赖、部署或重启服务；数据库与生产 Compose 网络须已经就绪。
- 单独的迁移环境文件明确配置 DB_HOST、DB_PORT、DB_NAME、DB_USER、DB_PWD；仅包含本次迁移角色的数据库配置，不复制应用JWT/Redis/国密私钥配置。Java在进程内读取凭据，不输出密码，不把密码拼入CLI参数。DB_HOST支持IPv4/容器名/DNS名，MYSQL_USE_SSL沿用环境配置（缺省false）。
- 常驻应用账号通常仅需CRUD权限，不应为R29升级永久增加DDL权限。由DBA临时提供独立migration身份，仅授予目标库所需SELECT、ALTER/CREATE/INDEX及Liquibase历史/锁表DML权限；完成核验后撤销权限或回收该临时账号及凭据文件。不要把迁移身份替换为常驻app身份。
- 脚本使用显式MIGRATION_IMAGE、MIGRATION_NETWORK和MIGRATION_ENV_FILE执行独立非root容器，不调用整个生产Compose，不挂载应用私钥/数据目录，不拉起依赖或发布端口。

## 执行顺序

下面目标仅为生产 Compose 内网示例，操作者须替换为本次明确核对的目标。`--target`必须逐字匹配容器有效DB_HOST:DB_PORT/DB_NAME；没有隐式数据库或root用户默认值。

```bash
export MIGRATION_IMAGE=ls-aegis-server:2.1.0
export MIGRATION_NETWORK=ls-aegis-prod
export MIGRATION_ENV_FILE=/secure/r29-migration-db.env
bash scripts/migrate-r29.sh validate --target ls-aegis-mysql:3306/ls_aegis
bash scripts/migrate-r29.sh status --target ls-aegis-mysql:3306/ls_aegis
bash scripts/migrate-r29.sh apply --target ls-aegis-mysql:3306/ls_aegis --acknowledge-write
bash scripts/migrate-r29.sh status --target ls-aegis-mysql:3306/ls_aegis
```

`validate`检查固定发布输入、MySQL/平台基础表、既有受限历史校验和；`status`另输出受限集合的已登记/待处理数量。两者使用只读连接，缺少DATABASECHANGELOG时也不创建历史表。待处理数量不是必须执行DDL的数量：`apply`会按既有补丁的前置条件将当前已存在字段标记MARK_RAN，仅登记本次受限集合。它不证明其他历史master已执行。

`apply`要求显式`--acknowledge-write`，使用Liquibase锁并保留审计历史；同一版本重跑应pending=0。任一步失败停止发布，保留诊断与历史，先核对结构/重复匹配等原因；不自动删业务数据、改旧changeset、清校验和或盖章完整基线。历史列仅存在但类型/约束错误不属于自动修复范围，须单独对账和结构诊断。

## 新安装和旧库升级

新安装仍以`ls-aegis-server/sql/init.sql`为唯一全量SQL，等待`_itls_init_done`完成并建立业务数据库账号，再运行本受限入口登记/核验R29集合，随后启动候选服务。不要因为新安装已经包含字段就对完整master执行baseline/sync。

旧库不重新导入init，不启用完整master。先备份/停写，运行validate/status，确认目标及范围后apply；成功后status必须pending=0，再按发布流程启动同一候选镜像、检查健康/鉴权与相应业务回归。生产`SPRING_LIQUIBASE_ENABLED=false`保持不变，后续版本须提供自己的固定发布入口，不能往已发布R29清单追加文件。

专用环境文件由Docker传入，不会被本脚本source；妥善限制文件读取权限，完成后回收。脚本要求镜像和网络已存在，不自动拉取镜像或推测目标。直接在已配置独立迁移DB身份的候选JAR环境运行时：

```bash
java -Dloader.main=com.ls.aegis.boot.tools.R29MigrationCli -cp /app/app.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher status --target DB_HOST:DB_PORT/DB_NAME
```

本文件是操作说明，不代表生产发布、完整基线或业务验收已经通过；具体结果由项目正式报告登记。

## 旧账号数据与SM4

正常启动默认不运行旧账号写迁移。R29 schema CLI也不迁移账号内容，不能把SM4解密失败当作明文证据；未知/AES历史值应取得来源明确的明文映射后单独处理，禁止再包装一层SM4。

确有已核对的旧明文数字账号时，维护环境可显式设置`itls.migration.plaintext-account-no-enabled=true`，并提供`itls.migration.plaintext-account-no-allowlist=psi_company_account:101,102;psi_unit_pay:201`（示例ID必须替换）。默认未设置白名单时拒绝运行；每批最多1000行，仅支持已声明为明文的6-24位ASCII数字。疑似Base64块密文、字母/符号及未知格式会中止整批预检，当前可解密SM4保持原值。该限制刻意不覆盖IBAN、长账号或未知编码，须使用审计后的专门映射，不能扩大格式猜测。

此维护动作要求备份及停写窗口；先检查全部白名单，再在同一事务内按原值条件更新，遇到唯一键或并发变化不合并、不删除记录。属性仅供受控维护，完成后移除；不应作为正常部署的默认环境变量。禁止输出原账号、密钥或历史密文。
