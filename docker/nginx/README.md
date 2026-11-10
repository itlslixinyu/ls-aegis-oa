# Nginx（前端网关）

- 配置：`conf/nginx.conf`；根生产 Compose 将其构建进镜像，修改后需重建。`docker/docker-compose.yml` 的开发完整栈才额外挂载此配置。
- 镜像：`Dockerfile` 多阶段构建 `ls-aegis-web` → 打入 `/usr/share/nginx/html`
- `web/`：可选本地静态目录（默认不挂载进容器，产物不入库）

本机完整部署从仓库根构建（服务名为 `ls-aegis-web`）：

```bash
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml build ls-aegis-web
docker compose -p ls-aegis --env-file docker/.env -f docker-compose.yml -f docker-compose.local.yml up -d --no-deps ls-aegis-web
```

环境选择和生产入口见[部署指南](../../Docs/operations/deployment.md)。Nginx 镜像内含 curl，PC健康检查使用curl；不能假定Debian镜像含wget。
