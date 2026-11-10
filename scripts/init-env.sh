#!/usr/bin/env bash
# ===========================================================================
# LS・Aegis 生产 .env 初始化：写入完整模板并生成强随机密钥
# 用法：bash scripts/init-env.sh
# ===========================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
if [ ! -f "$PROJECT_ROOT/docker-compose.yml" ] && [ -f "$PWD/docker-compose.yml" ]; then
  PROJECT_ROOT="$PWD"
fi
ENV_FILE="$PROJECT_ROOT/.env"

echo "============================================================"
echo "  LS・Aegis .env 初始化 + 密钥生成"
echo "  PROJECT_ROOT = $PROJECT_ROOT"
echo "============================================================"

if [ -f "$ENV_FILE" ]; then
  echo ""
  echo "[1/3] .env 已存在，重新写入完整模板 ..."
else
  echo ""
  echo "[1/3] .env 不存在，写入默认模板 ..."
fi

cat > "$ENV_FILE" <<'ENVEOF'
# ===========================================================================
# LS・Aegis 生产部署 —— 环境变量（由 init-env.sh 自动生成）
# ===========================================================================

SPRING_PROFILES_ACTIVE=prod

# ===== MySQL（root 与业务账号分离）=====
MYSQL_ROOT_PASSWORD=__TO_BE_REPLACED_MYSQL__
DB_USER=ls_aegis_app
DB_PWD=__TO_BE_REPLACED_MYSQL_APP__

# ===== Redis =====
REDIS_PWD=__TO_BE_REPLACED_REDIS__

# ===== JWT / 签章敏感字段 =====
JWT_SECRET_KEY=__TO_BE_REPLACED_JWT__
TOKEN_SECRET=__TO_BE_REPLACED_TOKEN__
ENCRYPT_AES_PASSWORD=__TO_BE_REPLACED_AES__
ENCRYPT_RSA_PUBLIC_KEY=
ENCRYPT_RSA_PRIVATE_KEY=

# ===== 国密 =====
GM_KEY_STORE_PATH=/app/data/gm/keys.properties
FILE_DIGEST_MIGRATE_TO_SM3=false

# ===== 多租户 / 调度（生产默认关）=====
CONTINEW_STARTER_TENANT_ENABLED=false
SNAIL_JOB_ENABLED=false
SNAIL_JOB_URL=http://127.0.0.1:8001/snail-job
SCHEDULE_TOKEN=
SCHEDULE_USERNAME=schedule
SCHEDULE_PASSWORD=

# ===== 对外访问（install.sh 会在部署后覆盖为三域名）=====
APPLICATION_URL=https://CHANGE_ME_DOMAIN
ITLS_PUBLIC_DOMAIN=CHANGE_ME_DOMAIN
ITLS_PUBLIC_BASE_URL=https://CHANGE_ME_DOMAIN
ITLS_H5_CUSTOMER_DOMAIN=CHANGE_ME_H5
ITLS_H5_ENGINEER_DOMAIN=CHANGE_ME_H5E
ITLS_H5_CUSTOMER_URL=https://CHANGE_ME_H5
ITLS_H5_ENGINEER_URL=https://CHANGE_ME_H5E
ITLS_CORS_ALLOWED_ORIGINS=https://CHANGE_ME_DOMAIN

# ===== JVM（可按机器内存调整）=====
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# Docker 生产：已用 sql/init.sql 初始化时必须关闭，否则与 Liquibase 种子冲突
SPRING_LIQUIBASE_ENABLED=false
MYSQL_USE_SSL=false
ENVEOF
echo "  [OK] 已创建完整模板 $ENV_FILE"

echo ""
echo "[2/3] 使用 openssl 生成强随机密钥 ..."
if ! command -v openssl >/dev/null 2>&1; then
  echo "  openssl 未安装，尝试安装 ..."
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get install -y -qq openssl >/dev/null 2>&1 || apt-get install -y -qq openssl >/dev/null 2>&1 || true
  fi
fi
if ! command -v openssl >/dev/null 2>&1; then
  echo "  [FAIL] 需要 openssl，请先安装后重试"
  exit 1
fi

MYSQL_PWD=$(openssl rand -base64 24 | tr -d '\n=+/' | head -c 28)
APP_DB_PWD=$(openssl rand -base64 24 | tr -d '\n=+/' | head -c 28)
REDIS_PWD=$(openssl rand -base64 24 | tr -d '\n=+/' | head -c 28)
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n=+/' | head -c 48)
TOKEN_SECRET=$(openssl rand -base64 48 | tr -d '\n=+/' | head -c 48)
AES_PWD=$(openssl rand -base64 48 | tr -d '\n=+/' | head -c 48)

echo ""
echo "[3/3] 写入密钥到 $ENV_FILE ..."
replace_any() {
  local var="$1"; local val="$2"; local file="$3"
  # 转义 sed 替换侧特殊字符
  local esc
  esc=$(printf '%s' "$val" | sed -e 's/[\\/&]/\\&/g')
  sed -i -E "s|^${var}=.*|${var}=${esc}|" "$file"
}
replace_any "MYSQL_ROOT_PASSWORD" "$MYSQL_PWD"    "$ENV_FILE"
replace_any "DB_PWD"              "$APP_DB_PWD"    "$ENV_FILE"
replace_any "REDIS_PWD"           "$REDIS_PWD"    "$ENV_FILE"
replace_any "JWT_SECRET_KEY"      "$JWT_SECRET"   "$ENV_FILE"
replace_any "TOKEN_SECRET"        "$TOKEN_SECRET" "$ENV_FILE"
replace_any "ENCRYPT_AES_PASSWORD" "$AES_PWD"     "$ENV_FILE"

echo ""
echo "============================================================"
echo "  密钥已写入：$ENV_FILE"
echo "============================================================"
echo ""
echo "  MYSQL_ROOT_PASSWORD 与 DB_PWD（业务账号）已生成并写入 .env"
echo "  请从服务器本地查看：cat $ENV_FILE"
echo "  （终端不再打印完整明文密码）"
echo ""
echo "  下一步：bash scripts/deploy.sh"
echo "============================================================"
