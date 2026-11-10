#!/usr/bin/env bash
# ===========================================================================
# LS・Aegis 服务器一键安装入口（三域名）
#   cd /opt/itls && bash scripts/install.sh
# 腾讯云 ubuntu 账户可直接执行；需要 root 的步骤会自动 sudo
# ===========================================================================

set -euo pipefail

# ---------- 非 root 自动提权（ubuntu 等普通账户）----------
if [ "$(id -u)" -ne 0 ]; then
  if command -v sudo >/dev/null 2>&1; then
    echo "[INFO] 当前用户为 $(whoami)，通过 sudo 提权继续安装 ..."
    exec sudo -E bash "$0" "$@"
  else
    echo "[ERROR] 需要 root 权限，请安装 sudo 后重试，或改用 root 执行"
    exit 1
  fi
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

BOLD=""; CYAN=""; GREEN=""; YELLOW=""; RESET=""
if [ -t 1 ] && command -v tput >/dev/null 2>&1; then
  BOLD=$(tput bold 2>/dev/null || true)
  CYAN=$(tput setaf 6 2>/dev/null || true)
  GREEN=$(tput setaf 2 2>/dev/null || true)
  YELLOW=$(tput setaf 3 2>/dev/null || true)
  RESET=$(tput sgr0 2>/dev/null || true)
fi

is_interactive=0
if [ -t 0 ] && [ -t 1 ]; then is_interactive=1; fi

json_field() {
  local key="$1"
  [ -f "$SCRIPT_DIR/deploy-info.json" ] || return 0
  grep -Eo "\"${key}\"[[:space:]]*:[[:space:]]*\"[^\"]+\"" "$SCRIPT_DIR/deploy-info.json" 2>/dev/null \
    | head -1 | sed -E 's/.*"([^"]+)"[[:space:]]*$/\1/' || true
}

domain_packed=$(json_field Domain)
domain_h5_packed=$(json_field DomainH5)
domain_h5e_packed=$(json_field DomainH5Engineer)
server_packed=$(json_field Server)

printf '\n'
printf "${BOLD}${CYAN}  LS・Aegis 一键安装（服务器端）${RESET}\n"
printf '  项目目录 : %s\n' "$PROJECT_ROOT"
[ -n "${domain_packed:-}" ] && printf '  PC 域名  : %s\n' "$domain_packed"
[ -n "${domain_h5_packed:-}" ] && printf '  H5 客户  : %s\n' "$domain_h5_packed"
[ -n "${domain_h5e_packed:-}" ] && printf '  H5 工程师: %s\n' "$domain_h5e_packed"
[ -n "${server_packed:-}" ] && printf '  目标服务器: %s\n' "$server_packed"
printf '\n'

prompt_default() {
  local p="$1" d="$2" ans
  if [ "$is_interactive" -eq 0 ]; then echo "$d"; return 0; fi
  if [ -n "$d" ]; then p="$p [${d}]"; fi
  printf '  %s : ' "$p"; read -r ans
  if [ -z "${ans:-}" ]; then ans="$d"; fi
  echo "$ans"
}

if [ ! -f "$PROJECT_ROOT/docker-compose.yml" ]; then
  printf "${YELLOW}[WARN]${RESET} 缺少 docker-compose.yml：%s\n" "$PROJECT_ROOT"
  exit 1
fi

for f in deploy.sh init-env.sh backup.sh resource-alert.sh setup-docker-mirror.sh setup-nginx-bt.sh install.sh; do
  target="$SCRIPT_DIR/$f"
  if [ -f "$target" ] && [ ! -x "$target" ]; then chmod +x "$target" 2>/dev/null || true; fi
done

DEFAULT_DOMAIN="${domain_packed:-oa.ls.hb.cn}"
DEFAULT_H5="${domain_h5_packed:-kh.ls.hb.cn}"
DEFAULT_H5E="${domain_h5e_packed:-gc.ls.hb.cn}"
[ "$DEFAULT_DOMAIN" = "CHANGE_ME_DOMAIN" ] && DEFAULT_DOMAIN="oa.ls.hb.cn"

printf -- "${BOLD}${CYAN}--- 安装向导（三域名）---${RESET}\n"
DOMAIN=$(prompt_default "PC 后台域名" "$DEFAULT_DOMAIN")
DOMAIN_H5=$(prompt_default "H5 客户域名" "$DEFAULT_H5")
DOMAIN_H5E=$(prompt_default "H5 工程师域名" "$DEFAULT_H5E")

BASE="https://${DOMAIN}"
H5_URL="https://${DOMAIN_H5}"
H5E_URL="https://${DOMAIN_H5E}"
CORS="${BASE},${H5_URL},${H5E_URL},http://${DOMAIN},http://${DOMAIN_H5},http://${DOMAIN_H5E}"

printf '\n'
printf '  PC           : %s\n' "$BASE"
printf '  H5 客户      : %s\n' "$H5_URL"
printf '  H5 工程师    : %s\n' "$H5E_URL"
printf '  CORS         : %s\n' "$CORS"
printf '\n'

printf -- "${CYAN}-> 调用 scripts/deploy.sh ...${RESET}\n"
cd "$PROJECT_ROOT"
bash "$SCRIPT_DIR/deploy.sh" "$@"
DEPLOY_EXIT=$?

if [ "$DEPLOY_EXIT" -ne 0 ]; then
  printf "\n${YELLOW}[WARN]${RESET} deploy.sh 退出码 %s\n" "$DEPLOY_EXIT"
  printf "       修复后可执行：\n"
  printf "         sudo bash scripts/setup-nginx-bt.sh -d %s --h5 %s --h5e %s\n" "$DOMAIN" "$DOMAIN_H5" "$DOMAIN_H5E"
  exit "$DEPLOY_EXIT"
fi

ENV_FILE="$PROJECT_ROOT/.env"
ensure_env_line() {
  local k="$1" v="$2" esc
  esc=$(printf '%s' "$v" | sed -e 's/[\\&/]/\\&/g')
  if grep -Eiq "^[#[:space:]]*${k}=" "$ENV_FILE" 2>/dev/null; then
    sed -i -E "s|^[#[:space:]]*${k}=.*|${k}=${esc}|I" "$ENV_FILE"
  else
    printf '%s=%s\n' "$k" "$v" >> "$ENV_FILE"
  fi
}
ensure_env_line "ITLS_CORS_ALLOWED_ORIGINS" "$CORS"
ensure_env_line "ITLS_PUBLIC_BASE_URL"      "$BASE"
ensure_env_line "ITLS_PUBLIC_DOMAIN"        "$DOMAIN"
ensure_env_line "ITLS_H5_CUSTOMER_DOMAIN"   "$DOMAIN_H5"
ensure_env_line "ITLS_H5_ENGINEER_DOMAIN"   "$DOMAIN_H5E"
ensure_env_line "ITLS_H5_CUSTOMER_URL"      "$H5_URL"
ensure_env_line "ITLS_H5_ENGINEER_URL"      "$H5E_URL"
ensure_env_line "APPLICATION_URL"           "$BASE"
printf "${GREEN}[OK]${RESET} 三域名已写入 .env\n"

if docker compose ps --format '{{.Service}}' 2>/dev/null | grep -q 'ls-aegis-server'; then
  printf "${CYAN}  [INFO]${RESET}  重启 ls-aegis-server 以加载域名配置 ...\n"
  docker compose up -d ls-aegis-server >/dev/null 2>&1 || true
fi

printf "${BOLD}${GREEN}=== Docker 栈部署完成 ===${RESET}\n"
printf '  PC:        https://%s\n' "$DOMAIN"
printf '  H5 客户:   https://%s\n' "$DOMAIN_H5"
printf '  H5 工程师: https://%s\n' "$DOMAIN_H5E"
printf '\n'

# 有宝塔则自动配三域名反代 + SSL（交互可跳过；非交互/一键远程时直接执行）
if [ -d /www/server/panel ]; then
  DO_NGINX=1
  if [ "$is_interactive" = "1" ]; then
    printf '  检测到宝塔面板。是否立即配置三域名 Nginx + SSL？(Y/n): '
    read -r ans || true
    case "${ans:-Y}" in
      n*|N*) DO_NGINX=0 ;;
    esac
  else
    printf "${CYAN}  [INFO]${RESET}  非交互模式：自动配置宝塔 Nginx + SSL ...\n"
  fi
  if [ "$DO_NGINX" = "1" ]; then
    bash "$SCRIPT_DIR/setup-nginx-bt.sh" -d "$DOMAIN" --h5 "$DOMAIN_H5" --h5e "$DOMAIN_H5E" || true
  fi
else
  printf -- "${BOLD}${CYAN}--- 下一步：Nginx 反代 + SSL ---${RESET}\n"
  printf '  sudo bash scripts/setup-nginx-bt.sh -d %s --h5 %s --h5e %s\n' "$DOMAIN" "$DOMAIN_H5" "$DOMAIN_H5E"
  printf '\n'
fi
