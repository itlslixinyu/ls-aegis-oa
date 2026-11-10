#!/usr/bin/env bash
# ===========================================================================
# LS・Aegis 备份：MySQL + 本地文件目录（无 MinIO）
# 用法：bash scripts/backup.sh
# cron 示例：0 2 * * * bash /opt/itls/scripts/backup.sh
# ===========================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
if [ ! -f "$PROJECT_ROOT/docker-compose.yml" ] && [ -f "$PWD/docker-compose.yml" ]; then
  PROJECT_ROOT="$PWD"
fi
cd "$PROJECT_ROOT"

BACKUP_DIR="$PROJECT_ROOT/backups"
DATE=$(date +%Y%m%d_%H%M%S)
MYSQL_CONTAINER="ls-aegis-mysql"
DB_NAME="ls_aegis"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi
MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-${DB_PWD:-}}"

SUDO=""
if [ "$(id -u)" -ne 0 ]; then SUDO="sudo"; fi
docker_cmd() {
  if [ -n "$SUDO" ]; then $SUDO docker "$@"; else docker "$@"; fi
}

mkdir -p "$BACKUP_DIR"
MYSQL_TMP=""
FILE_TMP=""
cleanup_temp() {
  if [ -n "$MYSQL_TMP" ]; then rm -f -- "$MYSQL_TMP"; fi
  if [ -n "$FILE_TMP" ]; then rm -f -- "$FILE_TMP"; fi
}
trap cleanup_temp EXIT

echo "============================================================"
echo "  LS・Aegis 备份"
echo "  时间: $DATE"
echo "  输出: $BACKUP_DIR"
echo "============================================================"

echo ""
echo "[1/3] 备份 MySQL ($DB_NAME) ..."
if [ -z "$MYSQL_PWD" ]; then
  echo "  [FAIL] 未设置 MYSQL_ROOT_PASSWORD / DB_PWD"
  exit 1
fi
MYSQL_BACKUP="$BACKUP_DIR/${DB_NAME}_${DATE}.sql.gz"
if docker_cmd ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  MYSQL_TMP=$(mktemp "$BACKUP_DIR/.${DB_NAME}_${DATE}.XXXXXX")
  if ! docker_cmd exec -e "MYSQL_PWD=$MYSQL_PWD" "$MYSQL_CONTAINER" mysqldump -uroot \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --default-character-set=utf8mb4 \
    --set-gtid-purged=OFF \
    "$DB_NAME" | gzip > "$MYSQL_TMP"; then
    echo "  [FAIL] MySQL 导出失败，保留旧备份，不发布未完成文件"
    exit 1
  fi
  gzip -t "$MYSQL_TMP"
  mv -- "$MYSQL_TMP" "$MYSQL_BACKUP"
  MYSQL_TMP=""
  echo "  [OK] $MYSQL_BACKUP ($(du -h "$MYSQL_BACKUP" | cut -f1))"
else
  echo "  [FAIL] 容器 $MYSQL_CONTAINER 未运行"
  exit 1
fi

echo ""
echo "[2/3] 备份本地文件目录 docker/data/ls-aegis-server ..."
FILE_BACKUP="$BACKUP_DIR/files_${DATE}.tar.gz"
DATA_DIR="$PROJECT_ROOT/docker/data/ls-aegis-server"
if [ -d "$DATA_DIR" ]; then
  FILE_TMP=$(mktemp "$BACKUP_DIR/.files_${DATE}.XXXXXX")
  tar czf "$FILE_TMP" -C "$PROJECT_ROOT/docker/data" ls-aegis-server
  gzip -t "$FILE_TMP"
  mv -- "$FILE_TMP" "$FILE_BACKUP"
  FILE_TMP=""
  echo "  [OK] $FILE_BACKUP ($(du -h "$FILE_BACKUP" | cut -f1))"
else
  echo "  [SKIP] 目录不存在: $DATA_DIR"
fi

echo ""
echo "[3/3] 清理 7 天前旧备份 ..."
while IFS= read -r -d '' old_backup; do
  backup_name=$(basename "$old_backup")
  if [[ "$backup_name" =~ ^${DB_NAME}_[0-9]{8}_[0-9]{6}\.sql\.gz$ \
     || "$backup_name" =~ ^files_[0-9]{8}_[0-9]{6}\.tar\.gz$ ]]; then
    rm -f -- "$old_backup"
  fi
done < <(find "$BACKUP_DIR" -maxdepth 1 -type f -mtime +7 -print0)
REMAINING=$(find "$BACKUP_DIR" -maxdepth 1 -type f -name "*.gz" | wc -l)
echo "  [OK] 目录保留 $REMAINING 个归档，仅清理本项目命名的过期备份"

echo ""
echo "============================================================"
echo "  备份完成"
ls -lh "$BACKUP_DIR"/*"${DATE}"* 2>/dev/null || true
echo "============================================================"
