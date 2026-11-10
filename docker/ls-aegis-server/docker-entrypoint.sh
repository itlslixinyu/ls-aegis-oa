#!/bin/sh
# 生产入口：修正挂载卷权限后降权为 aegis 启动
set -e

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
