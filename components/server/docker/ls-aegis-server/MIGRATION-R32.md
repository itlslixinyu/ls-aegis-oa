# R32 报价数据归属增量迁移

本发布固定执行 `releases/r32-mysql.yaml` 内 `ls:r32_quote_data_scope_mysql` 一个 changeset，仅增加 `eoms_quote.tenant_id/create_user/dept_id` 三个有符号 BIGINT、可空、缺省NULL的普通列。不会回填任何旧行，不按可重复用户名推断用户/部门/租户，不运行 master/seed/changelogSync，不修改R29/R31历史。

## 安装与升级边界

新安装使用当前唯一 `sql/init.sql`，已经具有三列；受限CLI在定义精确时可登记MARK_RAN。旧库三列全无时执行原始ALTER；三列全有且定义完全正确时MARK_RAN；仅部分列、错误类型/unsigned/NOT NULL/非空默认值/生成列或invisible等额外定义均拒绝，不能借MARK_RAN盖过漂移。已有本release history却缺列、缺校验和、重复/错误identity或校验和不同也拒绝。其他历史记录保留。

先验证备份可恢复，安排暂停报价写入的升级窗口，再执行此受限迁移，成功后部署对应报价代码。MySQL DDL可能隐式提交；失败保留诊断和历史，不声称自动回滚，不自动删列、删业务数据、清checksum或补齐部分列。历史归属NULL由报价业务权限策略保守处理，仅默认租户0超级管理员可查看未归属旧行，普通用户不可见；本工具不创建新身份体系或授予角色权限。

## 独立临时迁移身份

常驻应用继续仅CRUD，不永久授DDL。迁移环境文件只含明确 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PWD`（可选 `MYSQL_USE_SSL`），使用本次授权目标库的临时身份：对 `eoms_quote` 读取元信息所需权限和ALTER，对Liquibase两张history/lock表所需CREATE及DML。完成后回收该身份和凭据。禁止复制JWT/Redis/SM2私钥等无关应用配置，CLI不打印密码，密码不放参数。

```bash
export MIGRATION_IMAGE=ls-aegis-server:r32-candidate
export MIGRATION_NETWORK=ls-aegis-prod
export MIGRATION_ENV_FILE=/secure/r32-migration-db.env
bash scripts/migrate-r32.sh validate --target ls-aegis-mysql:3306/ls_aegis
bash scripts/migrate-r32.sh status --target ls-aegis-mysql:3306/ls_aegis
bash scripts/migrate-r32.sh apply --target ls-aegis-mysql:3306/ls_aegis --acknowledge-write
bash scripts/migrate-r32.sh validate --target ls-aegis-mysql:3306/ls_aegis
bash scripts/migrate-r32.sh status --target ls-aegis-mysql:3306/ls_aegis
```

示例目标须替换为明确核对的本次目标；`--target`须与进程内环境有效 `host:port/schema` 逐字一致。无默认root身份、无隐式目标。脚本只运行已有本地镜像的非root独立Java进程，不构建/拉起依赖/启动Spring，不挂载应用私钥或数据目录，不发布端口。

`validate/status`使用只读连接，缺少Liquibase history表时也不创建；pending=1仅说明此release尚未登记，不能凭它猜测其他master基线。`apply`是唯一写选项，必须显式确认参数；同版本重复应pending=0，不重跑DDL。固定发布资源按规范UTF-8/LF SHA256校验，任意SQL/release篡改拒绝。

上线前必须独立MySQL8验证：旧表全无三列→执行；新init/精确定义→MARK_RAN；partial/unsigned/notnull/default/generated错型→拒绝；既有history结构缺失/checksum不符→拒绝；错误目标/缺写确认→拒绝；原有种子/旧行归属NULL不变和重复no-op。单元测试不替代这项真实数据库验证。
