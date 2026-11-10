# 环境与部署

所有命令从仓库根执行，先明确环境。下表三种用途不能混起；真实.env不随仓库发布。

| 环境 | 配置 | 数据与端口 |
|---|---|---|
| 当前Windows完整部署 | 根Compose + docker-compose.local.yml；docker/.env | 原external MySQL/Redis卷；PC5173/客户5174/工程师5175/API8000 |
| 本机源码开发 | docker/docker-compose.yml + docker/docker-compose.dev.yml | 开发依赖，仅MySQL/Redis；应用本机启动 |
| 新服务器生产部署 | 根docker-compose.yml；根.env | 宿主绑定数据目录；服务25682/25683/25684/25685，由网关提供HTTPS |

## 当前已有数据环境

要求Compose2.24.4+；本机覆盖保留dev profile和既有账号，不作为公网生产加固结果。

```powershell
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml ps
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml up -d --build
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml logs --tail 100 ls-aegis-server
```

同一项目管理MySQL/Redis、后端、PC、两端H5及原媒体占位。H5兼容入口25684/25685用于ADB反向转发。MySQL/Redis分别复用 ls-aegis_mysql-data / ls-aegis_redis-data；附件、密钥和日志继续使用原挂载。不要搬迁正在运行的仓库外层目录。Windows字体绑定simhei.ttf；换机器需提供许可允许的TrueType中文字体。

## 新服务器

仅针对新目标准备模板 `.env.example`，填入自己的部署值；配置数据库应用身份和所需权限，不复制本机账号/真实密钥到Git。使用既有入口 `bash scripts/install.sh`。Windows源码打包入口为 `./pack.ps1 -Batch -NoBuild`；服务器从源码多阶段构建，不依赖开发机node_modules/JAR/dist。

端口仅绑定127.0.0.1，公网通过反向代理和HTTPS。根生产Compose MySQL为25606、Redis25679、API25682、PC25683、客户25684、工程师25685。默认媒体服务不在根生产栈，当前本机占位不等于GB28181已接入。

全新空库唯一SQL为 ls-aegis-server/sql/init.sql。已有库不能重跑全量SQL；受限升级按[迁移R29](../../docker/ls-aegis-server/MIGRATION-R29.md)、[R31](../../docker/ls-aegis-server/MIGRATION-R31.md)、[R32](../../docker/ls-aegis-server/MIGRATION-R32.md)的适用范围执行；这些入口不是执行所有新增patch的通用升级器。当前主master关闭，新补丁需逐项核对，不能因文档整理自动执行。

## 验证和运维

构建后确认容器健康，再执行 `python -X utf8 scripts/quality/service_smoke.py`（其默认端口适用于本机完整部署）。检查PC/H5页面、API代理、文件MIME与登录契约，不能只看TCP端口。

备份/恢复见[数据保护](data-and-recovery.md)。资源检查可使用 `scripts/resource-alert.sh`；安装cron会改变系统计划任务，应按部署授权执行。私有运行证据留本地，不输出凭据插值后的Compose配置。
