# R31 库存预警覆盖索引增量

本发布只增加 `psi_stock_batch.idx_stock_alert_balance`，键顺序固定为
`(tenant_id, deleted, goods_id, warehouse_id, remain_quantity)`。它用于库存预警的实时商品汇总，
不改变库存数量、成本、权限、业务数据或种子。无缓存，不替代正式业务和同数据容量复测。

R29 四个补丁、52 项清单和 `R29MigrationCli` 均保持原样；R31 有自己的固定单项发布文件
`db/changelog/releases/r31-mysql.yaml`、SQL、摘要和历史身份。不得向已发布 R29 清单追加本项，
不得通过完整 master、init、`changelogSync` 或清空校验和替代增量。

## MySQL 8 执行

先核对备份、目标库、存储空间和应用负载，并准备只针对目标库的独立临时迁移身份。
应用常驻身份仍只有 CRUD；迁移身份还需本索引的 ALTER/INDEX、元数据读取和 Liquibase
历史/锁表权限。脚本不会创建用户、授予权限、构建镜像、启动依赖或部署应用。

使用已经含本 CLI 的本地候选镜像、数据库既有内网和专用环境文件：

```bash
export MIGRATION_IMAGE=ls-aegis-server:r31-candidate
export MIGRATION_NETWORK=ls-aegis-prod
export MIGRATION_ENV_FILE=/secure/r31-migration-db.env
bash scripts/migrate-r31.sh validate --target ls-aegis-mysql:3306/ls_aegis
bash scripts/migrate-r31.sh status --target ls-aegis-mysql:3306/ls_aegis
bash scripts/migrate-r31.sh apply --target ls-aegis-mysql:3306/ls_aegis --acknowledge-write
bash scripts/migrate-r31.sh status --target ls-aegis-mysql:3306/ls_aegis
```

示例目标必须替换为已核对的实际 `DB_HOST:DB_PORT/DB_NAME`，并与环境文件逐字一致。
环境中 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PWD` 全部必填，`MYSQL_USE_SSL` 为明确布尔值
（缺省 false，沿用 R29 地址解析契约）。不 source 或打印环境文件，不在命令行放密码，
不挂载生产私钥/数据目录，不发布端口；临时容器以 UID/GID 10001 执行后即移除。

`validate/status` 使用只读 JDBC，校验固定发布文件摘要、MySQL8/InnoDB、五列、同名索引定义
以及本项既有历史 checksum。它们不调用 Liquibase 历史初始化，不建历史/锁表，不执行 DDL。
历史中本项已记录但索引丢失、同名索引异列/异序/唯一/非 BTREE/前缀/倒序/不可见、checksum
不符均拒绝，不能把名字存在误认为结构正确。

`apply` 唯一允许的 DDL 是：

```sql
ALTER TABLE psi_stock_batch
    ADD INDEX idx_stock_alert_balance (tenant_id, deleted, goods_id, warehouse_id, remain_quantity),
    ALGORITHM=INPLACE, LOCK=NONE;
```

明确指定 INPLACE/LOCK=NONE，不支持时失败，不隐式回退 COPY。在线创建仍会占用 CPU、IO、
临时/索引空间，并取得短暂元数据锁；LOCK=NONE 不表示零影响。并发长事务可能延长等待，
应由发布窗口和数据库监控控制，不能在在用实例执行无界压测。

release 的全局 HALT 前置先验证既有索引完整定义；只有完全一致的索引才由 changeset 的
`indexExists` 前置标记 MARK_RAN。否则新建并记录 EXECUTED。最终本 release 应 total=1、
recorded=1、pending=0、index=EXACT_EXISTS；重跑无新 DDL、无新索引/历史行。不以 pending=0
替代结构校验。MySQL DDL 可能隐式提交，失败不自动删除索引、清理业务或回退数据库。

新安装的唯一全量文件仍为 `ls-aegis-server/sql/init.sql`，其建表定义已经包含本索引。
新安装后可用本受限入口核验并仅登记此项 MARK_RAN；R29 登记仍由自己的入口独立负责。
已有 PSI 基线的 master 升级清单包含 R31 release；其全局前置要求 PSI 表已存在，
不能把它当作空库建表入口。在用升级不可因此直接执行整个 master。

直接在专用迁移环境调用打包入口也可以：

```bash
java -Dloader.main=com.ls.aegis.boot.tools.R31MigrationCli -cp /app/app.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher status --target DB_HOST:DB_PORT/DB_NAME
```

## PostgreSQL 源码边界

`releases/r31-postgresql.yaml` 提供同等五列 BTREE 定义、有效性/非部分/非表达式/非包含列的
同名校验和独立单项历史；`CREATE INDEX CONCURRENTLY` 明确 `runInTransaction:false`。
失败可能留下 invalid 索引，下一次预检必须拒绝，由 DBA 查明后采用正式后续修复，不能
盲目 MARK_RAN 或自动删除。当前仓库 master 的 PostgreSQL 分支默认注释，PSI 全量基线尚不
完整；本文件不声称 PostgreSQL 全新安装或真实数据库执行通过，也不新增第二套全量初始化。
本次 CLI/脚本仅支持 MySQL8，PG 发布须在已核对 PG 平台基线上使用该独立 release。

## 回退与验收

索引只影响查询计划；业务汇总在无索引时仍正确，但回到旧性能风险。删除索引也属于需审核
的后续结构变更，不删除既有 history 或修改已执行 R31 文本来伪装回退。保留备份和诊断，
需要变更索引时发布新的 changeset。发布后以同数据、同角色、同资源的 EXPLAIN ANALYZE、
返回量、延迟、并发保护和错误率对照确认收益；单测或索引存在本身不等于容量验收 PASS。

本说明是可执行发布契约，不是已执行在用迁移或容量测试的结论。
