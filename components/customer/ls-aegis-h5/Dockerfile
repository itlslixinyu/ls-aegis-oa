# itls-h5 / Dockerfile（国内源优化版）
# 构建参数 H5_ROLE=customer|engineer → 客户包 / 工程师包
# BASE_PATH 生产路径反代前缀，如 /h5/、/h5-engineer/；本地开发镜像可用 /
ARG H5_ROLE=customer
ARG BASE_PATH=/

# 阶段1: Node 构建（npm registry 已切 npmmirror）
FROM node:20-alpine@sha256:fb4cd12c85ee03686f6af5362a0b0d56d50c58a04632e6c0fb8363f609372293 AS builder
ARG H5_ROLE=customer
ARG BASE_PATH=/

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY . .
ENV BASE_PATH=${BASE_PATH}
RUN npm run build -- --mode ${H5_ROLE} \
 && if [ "${H5_ROLE}" = "engineer" ]; then rm -rf dist && mv dist-engineer dist; fi

# 阶段2: 固定已核验的基础镜像，其自身已含tzdata，无需再次联网安装。
FROM nginx:alpine@sha256:713fc2ebfd92e8217db451477182ee91944b123d2b1ee9fd5a8c6e91ca6c3a11

ENV TZ=Asia/Shanghai
RUN test -f /usr/share/zoneinfo/Asia/Shanghai

RUN rm -f /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:80/ || exit 1
