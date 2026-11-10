#!/usr/bin/env bash
# ===========================================================================
# LS・Aegis 一键部署（Linux / 云服务器）
# 1) 检测/安装 Docker  2) 国内镜像  3) down  4) .env  5) compose up  6) 健康轮询
# 用法：
#   bash scripts/deploy.sh
#   bash scripts/deploy.sh --no-build
# ===========================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
if [ -f "$PWD/docker-compose.yml" ]; then
  PROJECT_ROOT="$PWD"
fi
cd "$PROJECT_ROOT"

if [ ! -f "docker-compose.yml" ]; then
  echo "[ERROR] 未找到 docker-compose.yml：$PROJECT_ROOT"
  exit 1
fi

if [ -t 1 ] && command -v tput >/dev/null 2>&1; then
  C_RED=$(tput setaf 1 2>/dev/null || true); C_GREEN=$(tput setaf 2 2>/dev/null || true)
  C_YELLOW=$(tput setaf 3 2>/dev/null || true); C_CYAN=$(tput setaf 6 2>/dev/null || true)
  C_BOLD=$(tput bold 2>/dev/null || true); C_RESET=$(tput sgr0 2>/dev/null || true)
else
  C_RED=""; C_GREEN=""; C_YELLOW=""; C_CYAN=""; C_BOLD=""; C_RESET=""
fi
info()   { echo "${C_CYAN}  [INFO]${C_RESET}  $*"; }
ok()     { echo "${C_GREEN}   [OK] ${C_RESET}  $*"; }
warn()   { echo "${C_YELLOW} [WARN]${C_RESET}  $*"; }
fail()   { echo "${C_RED}  [FAIL]${C_RESET}  $*"; }
section(){ echo; echo "${C_BOLD}=== $* ===${C_RESET}"; }

BUILD_MODE="server"
for arg in "$@"; do
  case "$arg" in
    --local-build) BUILD_MODE="local" ;;
    --no-build)    BUILD_MODE="none"  ;;
    -h|--help)     sed -n '2,12p' "$0"; exit 0 ;;
    *) warn "忽略未知参数: $arg" ;;
  esac
done

echo
echo "${C_BOLD}============================================================"
echo "  LS・Aegis - 一键部署"
echo "  项目目录 : $PROJECT_ROOT"
echo "  构建模式 : $BUILD_MODE"
echo "============================================================${C_RESET}"

section "Pre-Flight  系统资源"
SUDO=""
if [ "$(id -u)" -ne 0 ]; then SUDO="sudo"; fi

# ubuntu 等账户无 docker 组权限时，统一走 sudo docker
docker_cmd() {
  if [ -n "$SUDO" ]; then
    # shellcheck disable=SC2086
    $SUDO docker "$@"
  else
    docker "$@"
  fi
}
compose_cmd() {
  if [ -n "$SUDO" ]; then
    # shellcheck disable=SC2086
    $SUDO docker compose "$@"
  else
    docker compose "$@"
  fi
}

TOTAL_MEM_KB=$(awk '/MemTotal:/ {print $2}' /proc/meminfo 2>/dev/null || echo 0)
AVAIL_MEM_KB=$(awk '/MemAvailable:/ {print $2}' /proc/meminfo 2>/dev/null || echo 0)
TOTAL_MEM_MB=$((TOTAL_MEM_KB / 1024))
AVAIL_MEM_MB=$((AVAIL_MEM_KB / 1024))
TOTAL_CORES=$(nproc 2>/dev/null || echo 1)
SWAP_TOTAL_MB=$(( $(awk '/SwapTotal:/ {print $2}' /proc/meminfo 2>/dev/null || echo 0) / 1024 ))
DISK_AVAIL_MB=$(( $(df -Pk "$PROJECT_ROOT" 2>/dev/null | awk 'NR==2 {print $4}') / 1024 ))

printf "  内存: 总计 %s MB / 可用 %s MB | Swap %s MB | CPU %s | 磁盘剩余 %s MB\n" \
  "$TOTAL_MEM_MB" "$AVAIL_MEM_MB" "$SWAP_TOTAL_MB" "$TOTAL_CORES" "$DISK_AVAIL_MB"

COMPOSE_BUILD_EXTRA=""
if [ "$TOTAL_MEM_MB" -lt 2500 ]; then
  COMPOSE_BUILD_EXTRA="--parallel 1"
  info "小内存 -> 串行构建 --parallel 1"
fi
if [ "$TOTAL_MEM_MB" -lt 4096 ]; then
  export DOCKER_BUILDKIT=1
  export BUILDKIT_PROGRESS=plain
fi
if [ "$DISK_AVAIL_MB" -lt 5120 ]; then
  fail "磁盘可用空间仅 ${DISK_AVAIL_MB} MB，至少需要约 5GB"
  exit 1
fi
ok "资源检查通过"

# ---------- 1/6 Docker ----------
section "1/6  Docker 环境"
PKG_MGR=""
if command -v apt-get >/dev/null 2>&1; then PKG_MGR="apt"
elif command -v yum >/dev/null 2>&1; then PKG_MGR="yum"
elif command -v dnf >/dev/null 2>&1; then PKG_MGR="dnf"
fi

install_docker() {
  info "安装 Docker CE + Compose 插件 ..."
  if [ "$PKG_MGR" = "apt" ] || [ "$PKG_MGR" = "yum" ] || [ "$PKG_MGR" = "dnf" ]; then
    curl -fsSL https://get.docker.com | $SUDO sh -s -- --mirror Aliyun
    if [ "$PKG_MGR" = "apt" ]; then
      $SUDO apt-get install -y -qq docker-compose-plugin >/dev/null 2>&1 || true
    else
      $SUDO $PKG_MGR install -y docker-compose-plugin >/dev/null 2>&1 || true
    fi
  else
    fail "不支持的包管理器，请手工安装 Docker"
    return 1
  fi
  $SUDO systemctl enable --now docker 2>/dev/null || true
  # 将当前登录用户加入 docker 组（下次登录可免 sudo；本次仍用 sudo docker）
  if [ -n "${SUDO_USER:-}" ] && id "$SUDO_USER" >/dev/null 2>&1; then
    $SUDO usermod -aG docker "$SUDO_USER" 2>/dev/null || true
    info "已将用户 $SUDO_USER 加入 docker 组（重新登录后可免 sudo）"
  elif [ -n "$SUDO" ] && [ -n "${USER:-}" ] && [ "$USER" != "root" ]; then
    $SUDO usermod -aG docker "$USER" 2>/dev/null || true
  fi
}

if command -v docker >/dev/null 2>&1; then
  ok "Docker: $(docker --version)"
else
  install_docker || exit 1
fi
if ! compose_cmd version >/dev/null 2>&1; then
  warn "缺少 compose 插件，尝试安装 ..."
  [ "$PKG_MGR" = "apt" ] && $SUDO apt-get install -y -qq docker-compose-plugin >/dev/null 2>&1 || true
fi
if ! compose_cmd version >/dev/null 2>&1; then
  fail "docker compose 不可用（请确认已安装并可用 sudo docker compose）"
  exit 1
fi
ok "Compose: $(compose_cmd version)"

# ---------- 2/6 镜像源 ----------
section "2/6  Docker 国内镜像"
DAEMON_JSON="/etc/docker/daemon.json"
need_restart=0
if [ ! -f "$DAEMON_JSON" ] || ! grep -q "mirror.ccs.tencentyun.com\|registry.cn-hangzhou" "$DAEMON_JSON" 2>/dev/null; then
  info "写入 /etc/docker/daemon.json ..."
  $SUDO mkdir -p /etc/docker
  $SUDO tee "$DAEMON_JSON" >/dev/null <<'JSON'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://registry.cn-hangzhou.aliyuncs.com",
    "https://docker.mirrors.ustc.edu.cn"
  ],
  "dns": ["119.29.29.29", "182.254.116.116", "8.8.8.8"],
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "3" },
  "storage-driver": "overlay2",
  "live-restore": true
}
JSON
  need_restart=1
else
  ok "daemon.json 已配置"
fi
if [ "$need_restart" = "1" ]; then
  $SUDO systemctl daemon-reload >/dev/null 2>&1 || true
  $SUDO systemctl restart docker || $SUDO service docker restart 2>/dev/null || true
  sleep 4
  ok "Docker 已重启"
fi

# ---------- 3/6 down ----------
section "3/6  停止旧栈"
if compose_cmd ps --format '{{.Service}}' 2>/dev/null | grep -q .; then
  info "优雅停止（最多 30s）..."
  compose_cmd down -t 30 >/dev/null
  ok "已停止"
else
  ok "无运行中的容器"
fi

# ---------- 4/6 .env ----------
section "4/6  密钥 (.env)"
ENV_FILE="$PROJECT_ROOT/.env"
NEEDS_GEN=0
if [ ! -f "$ENV_FILE" ]; then
  NEEDS_GEN=1
else
  for v in MYSQL_ROOT_PASSWORD DB_PWD REDIS_PWD JWT_SECRET_KEY TOKEN_SECRET; do
    val=$(grep -E "^${v}=" "$ENV_FILE" 2>/dev/null | head -1 | cut -d= -f2- || true)
    if [ -z "$val" ] || [[ "$val" == *"TO_BE_REPLACED"* ]] || [[ "$val" == *"CHANGE_ME"* ]]; then
      NEEDS_GEN=1
      break
    fi
  done
fi
if [ "$NEEDS_GEN" -eq 1 ]; then
  warn "调用 init-env.sh 生成密钥 ..."
  chmod +x scripts/init-env.sh 2>/dev/null || true
  bash scripts/init-env.sh
  ok ".env 已生成"
else
  ok ".env 已存在且密钥有效"
fi

ENV_ERRORS=""
for v in MYSQL_ROOT_PASSWORD DB_PWD REDIS_PWD JWT_SECRET_KEY TOKEN_SECRET; do
  val=$(grep -E "^${v}=" "$ENV_FILE" 2>/dev/null | head -1 | cut -d= -f2- || true)
  if [ -z "$val" ] || [[ "$val" == *"TO_BE_REPLACED"* ]] || [[ "$val" == *"CHANGE_ME"* ]]; then
    ENV_ERRORS="${ENV_ERRORS}\n  [FAIL] ${v}"
  fi
done
if [ -n "$ENV_ERRORS" ]; then
  fail "密钥校验失败:${ENV_ERRORS}"
  exit 1
fi
ok "密钥校验通过"

# ---------- 5/6 up ----------
section "5/6  启动服务 (mode=$BUILD_MODE)"
mkdir -p docker/data/mysql/data \
  docker/data/redis/data \
  docker/data/ls-aegis-server/config \
  docker/data/ls-aegis-server/data/file \
  docker/data/ls-aegis-server/data/gm \
  docker/data/ls-aegis-server/logs
# 与 Dockerfile.prod 中 aegis UID/GID=10001 对齐，避免日志 Permission denied
$SUDO chown -R 10001:10001 docker/data/ls-aegis-server 2>/dev/null || chown -R 10001:10001 docker/data/ls-aegis-server 2>/dev/null || true

case "$BUILD_MODE" in
  server)
    info "docker compose up -d --build （首次约 10–25 分钟）"
    # shellcheck disable=SC2086
    compose_cmd $COMPOSE_BUILD_EXTRA up -d --build
    ;;
  local|none)
    info "docker compose up -d （不重建镜像）"
    # shellcheck disable=SC2086
    compose_cmd $COMPOSE_BUILD_EXTRA up -d
    ;;
esac

if compose_cmd ps --format '{{.Service}}' 2>/dev/null | grep -qx ls-aegis-mysql; then
  info "等待 MySQL 就绪后初始化业务账号 ..."
  MYSQL_WAIT=0
  while [ "$MYSQL_WAIT" -lt 120 ]; do
    health=$(compose_cmd ps --format '{{.Service}}|{{.Health}}' 2>/dev/null | grep '^ls-aegis-mysql|' | head -1 | cut -d'|' -f2 || true)
    if [ "$health" = "healthy" ]; then
      break
    fi
    sleep 5
    MYSQL_WAIT=$((MYSQL_WAIT + 5))
  done
  chmod +x scripts/ensure-mysql-app-user.sh 2>/dev/null || true
  bash scripts/ensure-mysql-app-user.sh || warn "业务账号初始化失败，请手动执行 scripts/ensure-mysql-app-user.sh"
  compose_cmd up -d ls-aegis-server >/dev/null 2>&1 || true
fi

# ---------- 6/6 health ----------
# 首次空库跑 init.sql 可能数分钟；MySQL health 需等种子导入完成，总等待放宽到 20 分钟
section "6/6  等待健康（最长 1200s，含首次 SQL 初始化）"
MAX_WAIT=1200; INTERVAL=10; ELAPSED=0
SERVICES=(ls-aegis-mysql ls-aegis-redis ls-aegis-server ls-aegis-web ls-aegis-h5 ls-aegis-h5-engineer)

echo "   TIME   SERVICE                   STATE       HEALTH"
echo "   --------------------------------------------------------------"
ALL_HEALTHY=0
while [ "$ELAPSED" -lt "$MAX_WAIT" ]; do
  rows=$(compose_cmd ps --format '{{.Service}}|{{.State}}|{{.Health}}' 2>/dev/null || true)
  ALL_OK=1
  for svc in "${SERVICES[@]}"; do
    line=$(echo "$rows" | grep "^${svc}|" | head -1 || true)
    if [ -z "$line" ]; then
      printf "   %-5ss  %-24s %-11s %s\n" "$ELAPSED" "$svc" "N/A" "missing"
      ALL_OK=0
      continue
    fi
    state=$(echo "$line"  | cut -d'|' -f2)
    health=$(echo "$line" | cut -d'|' -f3)
    health_out="${health:-n/a}"
    printf "   %-5ss  %-24s %-11s %s\n" "$ELAPSED" "$svc" "$state" "$health_out"
    if [ "$state" != "running" ]; then ALL_OK=0; fi
    if [ -n "$health" ] && [ "$health" != "healthy" ]; then ALL_OK=0; fi
  done
  if [ "$ALL_OK" = "1" ]; then ALL_HEALTHY=1; break; fi
  sleep "$INTERVAL"
  ELAPSED=$((ELAPSED + INTERVAL))
  echo "   ..."
done

echo
if [ "$ALL_HEALTHY" = "1" ]; then
  ok "全部服务 healthy"
else
  warn "超时未全部 healthy，请检查: sudo docker compose ps && sudo docker compose logs --tail=80"
fi

echo
echo "${C_BOLD}============================================================"
echo "  本机访问（仅服务器本机，经 Nginx 反代后对外）"
echo "    PC 后台     : http://127.0.0.1:25683"
echo "    后端 API    : http://127.0.0.1:25682"
echo "    H5 客户     : http://127.0.0.1:25684"
echo "    H5 工程师   : http://127.0.0.1:25685"
echo "    MySQL       : 127.0.0.1:25606"
echo "    Redis       : 127.0.0.1:25679"
echo "============================================================${C_RESET}"
echo
echo "  下一步（宝塔）："
echo "    sudo bash scripts/setup-nginx-bt.sh -d 你的域名"
echo

if [ "$ALL_HEALTHY" != "1" ]; then exit 2; fi
exit 0
