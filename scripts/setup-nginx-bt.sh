#!/usr/bin/env bash
# ===========================================================================
# 宝塔 Nginx + SSL（三域名：PC / H5客户 / H5工程师）
# 用法：
#   bash scripts/setup-nginx-bt.sh \
#     -d oa.ls.hb.cn --h5 kh.ls.hb.cn --h5e gc.ls.hb.cn
#   bash scripts/setup-nginx-bt.sh --no-ssl   # 仅 HTTP 反代
# ===========================================================================

set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  if command -v sudo >/dev/null 2>&1; then
    exec sudo bash "$0" "$@"
  else
    echo "[ERROR] 需要 root 权限"
    exit 1
  fi
fi

DOMAIN=""
DOMAIN_H5=""
DOMAIN_H5E=""
DO_SSL=true
while [ $# -gt 0 ]; do
  case "$1" in
    -d|--domain) shift; DOMAIN="${1:-}" ;;
    --h5)        shift; DOMAIN_H5="${1:-}" ;;
    --h5e|--h5-engineer) shift; DOMAIN_H5E="${1:-}" ;;
    --no-ssl)    DO_SSL=false ;;
    -h|--help)   sed -n '2,12p' "$0"; exit 0 ;;
    *) ;;
  esac
  shift || true
done

C_GREEN=""; C_YELLOW=""; C_CYAN=""; C_BOLD=""; C_RESET=""; C_RED=""
if [ -t 1 ] && command -v tput >/dev/null 2>&1; then
  C_RED=$(tput setaf 1 2>/dev/null||true); C_GREEN=$(tput setaf 2 2>/dev/null||true)
  C_YELLOW=$(tput setaf 3 2>/dev/null||true); C_CYAN=$(tput setaf 6 2>/dev/null||true)
  C_BOLD=$(tput bold 2>/dev/null||true); C_RESET=$(tput sgr0 2>/dev/null||true)
fi
info()  { echo "${C_CYAN}  [INFO]${C_RESET}  $*"; }
ok()    { echo "${C_GREEN}   [OK] ${C_RESET}  $*"; }
warn()  { echo "${C_YELLOW} [WARN]${C_RESET}  $*"; }
fail()  { echo "${C_RED}  [FAIL]${C_RESET}  $*"; }
section(){ echo; echo "${C_BOLD}=== $* ===${C_RESET}"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

json_field() {
  local key="$1"
  grep -Eo "\"${key}\"[[:space:]]*:[[:space:]]*\"[^\"]+\"" "$SCRIPT_DIR/deploy-info.json" 2>/dev/null \
    | head -1 | sed -E 's/.*"([^"]+)"[[:space:]]*$/\1/' || true
}

if [ -f "$SCRIPT_DIR/deploy-info.json" ]; then
  [ -z "$DOMAIN" ]     && DOMAIN=$(json_field Domain)
  [ -z "$DOMAIN_H5" ]  && DOMAIN_H5=$(json_field DomainH5)
  [ -z "$DOMAIN_H5E" ] && DOMAIN_H5E=$(json_field DomainH5Engineer)
fi
if [ -f "$PROJECT_ROOT/.env" ]; then
  [ -z "$DOMAIN" ]     && DOMAIN=$(grep -E "^ITLS_PUBLIC_DOMAIN=" "$PROJECT_ROOT/.env" | head -1 | cut -d= -f2-)
  [ -z "$DOMAIN_H5" ]  && DOMAIN_H5=$(grep -E "^ITLS_H5_CUSTOMER_DOMAIN=" "$PROJECT_ROOT/.env" | head -1 | cut -d= -f2-)
  [ -z "$DOMAIN_H5E" ] && DOMAIN_H5E=$(grep -E "^ITLS_H5_ENGINEER_DOMAIN=" "$PROJECT_ROOT/.env" | head -1 | cut -d= -f2-)
fi

is_tty=0; [ -t 0 ] && [ -t 1 ] && is_tty=1
[ -z "$DOMAIN" ]     && DOMAIN="oa.ls.hb.cn"
[ -z "$DOMAIN_H5" ]  && DOMAIN_H5="kh.ls.hb.cn"
[ -z "$DOMAIN_H5E" ] && DOMAIN_H5E="gc.ls.hb.cn"
if [ "$is_tty" = "1" ]; then
  printf '  PC 域名 [%s]: ' "$DOMAIN"; read -r i; [ -n "${i:-}" ] && DOMAIN="$i"
  printf '  H5 客户域名 [%s]: ' "$DOMAIN_H5"; read -r i; [ -n "${i:-}" ] && DOMAIN_H5="$i"
  printf '  H5 工程师域名 [%s]: ' "$DOMAIN_H5E"; read -r i; [ -n "${i:-}" ] && DOMAIN_H5E="$i"
fi

echo
echo "${C_BOLD}============================================================"
echo "  Nginx + SSL（三域名）"
echo "  PC       : $DOMAIN      -> 25683 /api->25682"
echo "  H5 客户  : $DOMAIN_H5   -> 25684"
echo "  H5 工程师: $DOMAIN_H5E  -> 25685"
echo "  SSL      : $DO_SSL"
echo "============================================================${C_RESET}"

section "1/4  检测宝塔"
BT_PANEL="/www/server/panel"
BT_NGINX_CONF_DIR="/www/server/panel/vhost/nginx"
BT_CERT_DIR="/www/server/panel/vhost/cert"
BT_NGINX_BIN="/www/server/nginx/sbin/nginx"

if [ ! -d "$BT_PANEL" ]; then
  fail "未检测到宝塔面板"
  exit 1
fi
ok "宝塔已安装"

if [ -x "$BT_NGINX_BIN" ]; then NGINX_BIN="$BT_NGINX_BIN"
elif command -v nginx >/dev/null 2>&1; then NGINX_BIN="nginx"
else fail "未找到 Nginx"; exit 1; fi
ok "Nginx: $NGINX_BIN"
mkdir -p "$BT_NGINX_CONF_DIR" 2>/dev/null || true

BACKEND_PORT=25682
WEB_PORT=25683
H5_PORT=25684
H5E_PORT=25685

# 在宝塔「网站」列表中登记站点（仅写 conf 不会出现在面板里）
ensure_bt_site() {
  local d="$1"
  local ps="$2"
  local path="/www/wwwroot/${d}"
  mkdir -p "${path}/.well-known/acme-challenge" 2>/dev/null || true

  local BT_PY=""
  for c in /www/server/panel/pyenv/bin/python3 /www/server/panel/pyenv/bin/python \
           /www/server/panel/pyenv/bin/python3.7 python3; do
    if [ -x "$c" ] || command -v "$c" >/dev/null 2>&1; then
      BT_PY="$c"
      break
    fi
  done

  if [ -z "$BT_PY" ]; then
    warn "未找到宝塔 Python，跳过面板建站（仅写 Nginx，网站列表不会显示）"
    return 1
  fi

  local out
  out=$(
    DOMAIN_ARG="$d" PATH_ARG="$path" PS_ARG="$ps" "$BT_PY" - <<'PY' 2>&1
import os, sys
sys.path.insert(0, "/www/server/panel/class")
sys.path.insert(0, "/www/server/panel")
try:
    import public
    from panelSite import panelSite
except Exception as e:
    print("IMPORT_FAIL:" + str(e))
    sys.exit(2)

domain = os.environ["DOMAIN_ARG"]
path = os.environ["PATH_ARG"]
ps = os.environ["PS_ARG"]

try:
    n = public.M("sites").where("name=?", (domain,)).count()
    if n and int(n) > 0:
        print("EXISTS")
        sys.exit(0)
except Exception as e:
    print("DB_CHECK:" + str(e))

get = public.dict_obj()
get.webname = '{"domain":"%s","domainlist":[],"count":0}' % domain
get.path = path
get.type_id = "0"
get.type = "PHP"
get.version = "00"
get.port = "80"
get.ps = ps
get.ftp = "false"
get.sql = "false"
get.codeing = "utf8"
get.set_ssl = "0"
get.force_ssl = "0"
try:
    r = panelSite().AddSite(get)
    print("OK:" + str(r))
except Exception as e:
    print("ADD_FAIL:" + str(e))
    sys.exit(1)
PY
  ) || true

  if echo "$out" | grep -qE 'EXISTS|OK:'; then
    ok "宝塔已登记网站: $d"
    return 0
  fi
  warn "宝塔 API 建站失败 ($d): ${out:-无输出}"
  warn "可在面板「网站 → 添加站点」手工创建同名域名后再重跑本脚本"
  return 1
}

# 宝塔 SSL 部署锚点（面板靠 #error_page 404/404.html; 定位插入证书）
prepare_bt_wellknown() {
  local d="$1"
  mkdir -p "/www/server/panel/vhost/nginx/well-known" \
           "/www/wwwroot/${d}/.well-known/acme-challenge" \
           "$BT_CERT_DIR/$d" 2>/dev/null || true
  # include 目标必须存在，否则 nginx -t 失败
  if [ ! -f "/www/server/panel/vhost/nginx/well-known/${d}.conf" ]; then
    : > "/www/server/panel/vhost/nginx/well-known/${d}.conf"
  fi
}

write_pc_http() {
  local d="$1"
  local conf="$BT_NGINX_CONF_DIR/${d}.conf"
  prepare_bt_wellknown "$d"
  cat > "$conf" <<EOF
# LS・Aegis PC — $d — $(date '+%Y-%m-%d %H:%M:%S')
server {
    listen 80;
    server_name ${d};
    index index.html;
    root /www/wwwroot/${d};
    client_max_body_size 100m;

    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

    #CERT-APPLY-CHECK--START
    include /www/server/panel/vhost/nginx/well-known/${d}.conf;
    #CERT-APPLY-CHECK--END

    #SSL-START SSL相关配置，请勿删除或修改下一行带注释的404规则
    #error_page 404/404.html;
    #SSL-END

    location /.well-known/acme-challenge/ { root /www/wwwroot/${d}; }
    location /api/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT}/;
        proxy_http_version 1.1;
        proxy_set_header Origin "";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 300s;
    }
    location /websocket {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_http_version 1.1;
        proxy_set_header Origin "";
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_read_timeout 3600s;
    }
    location / {
        proxy_pass http://127.0.0.1:${WEB_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
    access_log /www/wwwlogs/${d}.log;
    error_log  /www/wwwlogs/${d}.error.log;
}
EOF
  ok "写入 $conf"
}

write_h5_http() {
  local d="$1"
  local port="$2"
  local conf="$BT_NGINX_CONF_DIR/${d}.conf"
  prepare_bt_wellknown "$d"
  cat > "$conf" <<EOF
# LS・Aegis H5 — $d -> ${port} — $(date '+%Y-%m-%d %H:%M:%S')
server {
    listen 80;
    server_name ${d};
    index index.html;
    root /www/wwwroot/${d};
    client_max_body_size 100m;

    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

    #CERT-APPLY-CHECK--START
    include /www/server/panel/vhost/nginx/well-known/${d}.conf;
    #CERT-APPLY-CHECK--END

    #SSL-START SSL相关配置，请勿删除或修改下一行带注释的404规则
    #error_page 404/404.html;
    #SSL-END

    location /.well-known/acme-challenge/ { root /www/wwwroot/${d}; }
    location / {
        proxy_pass http://127.0.0.1:${port};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
    access_log /www/wwwlogs/${d}.log;
    error_log  /www/wwwlogs/${d}.error.log;
}
EOF
  ok "写入 $conf"
}

write_pc_https() {
  local d="$1"
  local cert="$BT_CERT_DIR/$d/fullchain.pem"
  local key="$BT_CERT_DIR/$d/privkey.pem"
  local conf="$BT_NGINX_CONF_DIR/${d}.conf"
  prepare_bt_wellknown "$d"
  cat > "$conf" <<EOF
# LS・Aegis PC HTTPS — $d
# 保留宝塔 SSL 锚点，便于面板再次部署/续期证书
server {
    listen 80;
    listen 443 ssl http2;
    server_name ${d};
    index index.html;
    root /www/wwwroot/${d};
    client_max_body_size 100m;

    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

    #CERT-APPLY-CHECK--START
    include /www/server/panel/vhost/nginx/well-known/${d}.conf;
    #CERT-APPLY-CHECK--END

    #SSL-START SSL相关配置，请勿删除或修改下一行带注释的404规则
    ssl_certificate     ${cert};
    ssl_certificate_key ${key};
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers EECDH+CHACHA20:EECDH+CHACHA20-draft:EECDH+AES128:RSA+AES128:EECDH+AES256:RSA+AES256:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    error_page 497 https://\$host\$request_uri;
    #error_page 404/404.html;
    #SSL-END

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    location /.well-known/acme-challenge/ { root /www/wwwroot/${d}; }
    location /api/ {
        proxy_pass http://127.0.0.1:${BACKEND_PORT}/;
        proxy_http_version 1.1;
        proxy_set_header Origin "";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 300s;
    }
    location /websocket {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_http_version 1.1;
        proxy_set_header Origin "";
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_read_timeout 3600s;
    }
    location / {
        proxy_pass http://127.0.0.1:${WEB_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
    access_log /www/wwwlogs/${d}.log;
    error_log  /www/wwwlogs/${d}.error.log;
}
EOF
}

write_h5_https() {
  local d="$1"
  local port="$2"
  local cert="$BT_CERT_DIR/$d/fullchain.pem"
  local key="$BT_CERT_DIR/$d/privkey.pem"
  local conf="$BT_NGINX_CONF_DIR/${d}.conf"
  prepare_bt_wellknown "$d"
  cat > "$conf" <<EOF
# LS・Aegis H5 HTTPS — $d
server {
    listen 80;
    listen 443 ssl http2;
    server_name ${d};
    index index.html;
    root /www/wwwroot/${d};
    client_max_body_size 100m;

    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

    #CERT-APPLY-CHECK--START
    include /www/server/panel/vhost/nginx/well-known/${d}.conf;
    #CERT-APPLY-CHECK--END

    #SSL-START SSL相关配置，请勿删除或修改下一行带注释的404规则
    ssl_certificate     ${cert};
    ssl_certificate_key ${key};
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers EECDH+CHACHA20:EECDH+CHACHA20-draft:EECDH+AES128:RSA+AES128:EECDH+AES256:RSA+AES256:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    error_page 497 https://\$host\$request_uri;
    #error_page 404/404.html;
    #SSL-END

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    location /.well-known/acme-challenge/ { root /www/wwwroot/${d}; }
    location / {
        proxy_pass http://127.0.0.1:${port};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
    access_log /www/wwwlogs/${d}.log;
    error_log  /www/wwwlogs/${d}.error.log;
}
EOF
}

try_ssl() {
  local d="$1"
  local cert="$BT_CERT_DIR/$d/fullchain.pem"
  local key="$BT_CERT_DIR/$d/privkey.pem"
  local site="/www/wwwroot/${d}"
  if [ -f "$cert" ] && [ -f "$key" ]; then
    ok "$d 证书已存在"
    return 0
  fi
  local BT_ACME="/www/server/panel/script/acme.sh/acme.sh"
  if [ -x "$BT_ACME" ]; then
    info "acme.sh 签发 $d ..."
    "$BT_ACME" --issue -d "$d" --webroot "$site" 2>&1 || true
    "$BT_ACME" --install-cert -d "$d" --key-file "$key" --fullchain-file "$cert" \
      --reloadcmd "$NGINX_BIN -s reload" 2>&1 || true
  fi
  if [ ! -f "$cert" ] && command -v certbot >/dev/null 2>&1; then
    info "certbot 签发 $d ..."
    certbot certonly --webroot -w "$site" -d "$d" --non-interactive --agree-tos -m admin@"$d" 2>&1 || true
    cp -f "/etc/letsencrypt/live/$d/fullchain.pem" "$cert" 2>/dev/null || true
    cp -f "/etc/letsencrypt/live/$d/privkey.pem" "$key" 2>/dev/null || true
  fi
  if [ -f "$cert" ] && [ -f "$key" ]; then
    ok "$d SSL 就绪"
    return 0
  fi
  warn "$d SSL 自动签发失败，请在宝塔面板手工申请"
  return 1
}

section "2/4  登记宝塔网站并写入 HTTP 反代"
ensure_bt_site "$DOMAIN" "LS・Aegis PC" || true
ensure_bt_site "$DOMAIN_H5" "LS・Aegis H5客户" || true
ensure_bt_site "$DOMAIN_H5E" "LS・Aegis H5工程师" || true
# AddSite 会生成默认 conf；再覆盖为 Docker 反代
write_pc_http "$DOMAIN"
write_h5_http "$DOMAIN_H5" "$H5_PORT"
write_h5_http "$DOMAIN_H5E" "$H5E_PORT"

section "3/4  测试并重载"
if $NGINX_BIN -t 2>&1; then
  $NGINX_BIN -s reload 2>/dev/null || systemctl reload nginx 2>/dev/null || true
  ok "nginx reload"
else
  fail "nginx -t 失败"
  exit 1
fi

section "4/4  SSL"
if [ "$DO_SSL" = "false" ]; then
  warn "已跳过 SSL"
else
  PC_SSL=0; H5_SSL=0; H5E_SSL=0
  try_ssl "$DOMAIN" && PC_SSL=1 || true
  try_ssl "$DOMAIN_H5" && H5_SSL=1 || true
  try_ssl "$DOMAIN_H5E" && H5E_SSL=1 || true
  [ "$PC_SSL" = "1" ] && write_pc_https "$DOMAIN"
  [ "$H5_SSL" = "1" ] && write_h5_https "$DOMAIN_H5" "$H5_PORT"
  [ "$H5E_SSL" = "1" ] && write_h5_https "$DOMAIN_H5E" "$H5E_PORT"
  if $NGINX_BIN -t 2>&1; then
    $NGINX_BIN -s reload 2>/dev/null || true
    ok "最终配置已生效"
  fi
fi

echo
ok "PC:        https://${DOMAIN}"
ok "H5 客户:   https://${DOMAIN_H5}"
ok "H5 工程师: https://${DOMAIN_H5E}"
