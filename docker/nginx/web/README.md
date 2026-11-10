# Nginx 前端静态资源（可选本地挂载点）

默认部署路径：**Compose 多阶段构建**（`docker/nginx/Dockerfile`）在镜像内生成静态文件，**无需**本目录有内容，裸 clone 后直接：

```bash
cd docker
docker compose up -d --build
```

本目录仅作可选用途（例如本地覆盖镜像内静态资源时自行挂载），**不要将构建产物提交到 Git**。

服务名为 `ls-aegis-web`，不再使用旧命令中的 `web`。当前本机完整部署和新服务器生产部署使用不同参数，具体命令见[部署指南](../../../Docs/operations/deployment.md)。修改本目录内容不会自动改变镜像内的静态资源。
