#!/usr/bin/env bash
# ===========================================================================
# 确保 MySQL 业务账号存在（最小权限，替代应用直连 root）
# 用法：bash scripts/ensure-mysql-app-user.sh
# ===========================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"
CONTAINER="${MYSQL_CONTAINER:-ls-aegis-mysql}"

if [ ! -f "$ENV_FILE" ]; then
  echo "[FAIL] 未找到 .env：$ENV_FILE"
  exit 1
fi

# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

DB_USER="${DB_USER:-ls_aegis_app}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:?请在 .env 设置 MYSQL_ROOT_PASSWORD}"
DB_PWD="${DB_PWD:?请在 .env 设置 DB_PWD}"

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "[WARN] 容器 $CONTAINER 未运行，跳过业务账号初始化"
  exit 0
fi

echo "[INFO] 确保 MySQL 业务账号 ${DB_USER}@% ..."

docker exec "$CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 -e "
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PWD}';
ALTER USER '${DB_USER}'@'%' IDENTIFIED BY '${DB_PWD}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP,
  CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE, CREATE VIEW, SHOW VIEW, TRIGGER
  ON ls_aegis.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
"

echo "[OK] MySQL 业务账号 ${DB_USER} 已就绪"
