# 前端多阶段构建 → Nginx 静态站点
# 构建上下文为仓库根目录（见 docker-compose.yml web.build）
#
# 国内无法直连 Docker Hub 时，可先拉取并 tag：
#   docker pull docker.m.daocloud.io/library/node:20-bookworm-slim
#   docker tag  docker.m.daocloud.io/library/node:20-bookworm-slim node:20-bookworm-slim
#   docker pull docker.m.daocloud.io/library/nginx:1.27.0
#   docker tag  docker.m.daocloud.io/library/nginx:1.27.0 nginx:1.27.0

FROM node:20-bookworm-slim AS web-build

WORKDIR /app

# 国内 npm 镜像；固定 pnpm 版本便于可复现构建
RUN corepack enable && corepack prepare pnpm@9.15.4 --activate \
    && pnpm config set registry https://registry.npmmirror.com

COPY ls-aegis-web/package.json ls-aegis-web/pnpm-lock.yaml ls-aegis-web/.npmrc ./
RUN pnpm install --frozen-lockfile

COPY ls-aegis-web/ ./
# 使用 .env.production；产物输出到 /app/dist
RUN pnpm build

FROM nginx:1.27.0

COPY docker/nginx/conf/nginx.conf /etc/nginx/nginx.conf
COPY --from=web-build /app/dist/ /usr/share/nginx/html/

ENV TZ=Asia/Shanghai
EXPOSE 80
