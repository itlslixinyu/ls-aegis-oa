# 雷铄 OA 飞牛生产版本 1.4.0

此分支对应 2026 年 11 月 21 日的飞牛 Docker 部署，项目名为 `ls-aegis-oa-service`。五份构建输入按镜像分目录保存，构建编号、原镜像 ID 和源码 SHA-256 见 `production-images.json`。客户 H5、工程师 H5 和移动网关使用各自上线时的快照。

| 目录 | 对应服务 |
| --- | --- |
| `components/server` | Java 后端及全新安装 SQL |
| `components/pc` | PC 管理端，包含生产 Dockerfile 使用的前端资源包 |
| `components/customer` | 客户 H5 |
| `components/engineer` | 工程师 H5 |
| `components/gateway` | 移动网关的基础镜像构建输入 |
| `config` | 移动网关 Nginx 和 Redis 配置 |

## 全新环境部署

`compose.yaml` 是参数化的飞牛部署模板。数据库为 MySQL 8.0.42，Redis 为 7.2.8。在空的部署目录使用此模板。已有部署保留其实际挂载路径。

1. 复制 `.env.example` 为 `.env`，填写绑定地址和各服务凭据。开通微信通知、加密或调度的环境，按 Compose 中相应环境变量填写配置。
2. 提供部署方的 `runtime/tls/mobile.crt`、`runtime/tls/mobile.key`、合法授权字体 `runtime/fonts/simhei.ttf` 及两个 H5 服务挂载的微信校验文件。恢复已有加密或签署数据时还需恢复配套密钥。
3. 执行 `docker compose config --quiet`。可先加载清单对应的镜像，或执行 `docker compose build` 从各组件目录构建。随后执行 `docker compose up -d`，使用 `docker compose ps` 核对服务。
4. 新 MySQL 数据目录首次启动时读取 `components/server/ls-aegis-server/sql/init.sql`，包含管理员和必要系统种子。核对业务列表和管理员账户后再开放入口。

PC、客户 H5、工程师 H5 分别绑定 18080、18081、18082。移动 HTTPS 网关绑定 18443、18444。FRP、TLS、微信凭据和运行数据由部署方管理。

## 飞牛在线备份

`scripts/oa-backup.py` 适用于 `/vol1/ls-aegis-oa` 下的飞牛部署。使用 Btrfs 只读快照，将备份写入 `/vol1/ls-aegis-backups`。执行 `sudo python3 scripts/oa-backup.py install` 会做一次备份和数据库恢复演练，成功后启用北京时间每天 02:00 的定时任务。按北京时间保留最近七天，每天一份。
