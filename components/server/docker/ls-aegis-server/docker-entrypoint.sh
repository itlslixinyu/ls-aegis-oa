#!/bin/sh
# 生产入口：修正挂载卷权限后降权为 aegis 启动
set -e

# Docker 启动已有容器时，MySQL 可能仍在恢复数据，尚未监听端口。
if [ -n "${DB_HOST:-}" ]; then
  db_port=${DB_PORT:-3306}
  db_deadline=$(($(date +%s) + 120))
  echo "[INFO] Waiting for MySQL at ${DB_HOST}:${db_port}"
  until timeout 2 bash -c 'exec 3<>"/dev/tcp/$1/$2"' bash "$DB_HOST" "$db_port" 2>/dev/null; do
    if [ "$(date +%s)" -ge "$db_deadline" ]; then
      echo "[ERROR] MySQL did not accept connections within 120 seconds" >&2
      exit 1
    fi
    sleep 2
  done
fi

# 已由容器运行时指定非 root 用户时，保持其身份；不递归修改挂载数据的权限。
if [ "$(id -u)" -ne 0 ]; then
  exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar
fi

# root 入口必须成功降权，不能在基础镜像缺少工具时静默回退 root。
if ! command -v setpriv >/dev/null 2>&1; then
  echo "[ERROR] setpriv is required for root entrypoint; refusing to start Java as root" >&2
  exit 1
fi

mkdir -p /app/logs /app/data/file /app/data/gm /app/config
# 宿主机 sudo 创建的目录多为 root，容器内非 root 无法写日志。
chown -R aegis:aegis /app/logs /app/data /app/config 2>/dev/null || true
chmod -R u+rwX /app/logs /app/data /app/config 2>/dev/null || true

exec setpriv --reuid=aegis --regid=aegis --clear-groups --inh-caps=-all -- \
  java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar
