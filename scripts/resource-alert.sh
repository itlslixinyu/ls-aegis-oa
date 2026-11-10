#!/usr/bin/env bash
# ===========================================================================
# LS・Aegis 资源监控告警
# 用法：
#   bash scripts/resource-alert.sh
#   bash scripts/resource-alert.sh --install-cron
#   bash scripts/resource-alert.sh --uninstall-cron
# ===========================================================================

set -e

SCRIPT_PATH="$(realpath "$0")"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_PATH")")"
ALERT_LOG="$PROJECT_ROOT/backups/alert.log"
DISK_THRESHOLD=85
MEM_THRESHOLD=85

mkdir -p "$(dirname "$ALERT_LOG")"

install_cron() {
  echo "[安装] cron 每小时检查一次 ..."
  CRON_JOB="0 * * * * $SCRIPT_PATH >> $ALERT_LOG 2>&1"
  (crontab -l 2>/dev/null | grep -v "resource-alert.sh"; echo "$CRON_JOB") | crontab -
  echo "  [OK] 已添加"
}

uninstall_cron() {
  echo "[卸载] 移除 cron ..."
  crontab -l 2>/dev/null | grep -v "resource-alert.sh" | crontab - || true
  echo "  [OK] 已移除"
}

check_resources() {
  echo "============================================================"
  echo "  资源检查 - $(date '+%Y-%m-%d %H:%M:%S')"
  echo "============================================================"

  DISK_USAGE=$(df -h / | tail -1 | awk '{print $5}' | sed 's/%//')
  if [ "$DISK_USAGE" -gt "$DISK_THRESHOLD" ]; then
    echo "  [告警] 磁盘使用率: ${DISK_USAGE}%  (阈值 ${DISK_THRESHOLD}%)"
  else
    echo "  [正常] 磁盘使用率: ${DISK_USAGE}%"
  fi

  if command -v free >/dev/null 2>&1; then
    MEM_AVAIL=$(free | awk '/Mem:/ {print $7}')
    MEM_TOTAL=$(free | awk '/Mem:/ {print $2}')
    if [ "${MEM_TOTAL:-0}" -gt 0 ]; then
      MEM_USAGE=$((100 - MEM_AVAIL * 100 / MEM_TOTAL))
      if [ "$MEM_USAGE" -gt "$MEM_THRESHOLD" ]; then
        echo "  [告警] 内存使用率: ${MEM_USAGE}%  (阈值 ${MEM_THRESHOLD}%)"
      else
        echo "  [正常] 内存使用率: ${MEM_USAGE}%"
      fi
    fi
  fi

  if command -v docker >/dev/null 2>&1; then
    echo ""
    echo "  Docker 容器："
    docker ps --format 'table {{.Names}}\t{{.Status}}' 2>/dev/null | head -20 || true
  fi
  echo "============================================================"
}

case "${1:-}" in
  --install-cron)   install_cron ;;
  --uninstall-cron) uninstall_cron ;;
  *)                check_resources ;;
esac
