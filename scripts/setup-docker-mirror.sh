#!/usr/bin/env bash
# ===========================================================================
# 配置 Docker 国内镜像加速（可单独执行；deploy.sh 内也会做同类配置）
# 用法：bash scripts/setup-docker-mirror.sh
# ===========================================================================

set -e

echo "============================================================"
echo "  配置 Docker 国内镜像加速"
echo "============================================================"

if [ "$(id -u)" = "0" ]; then SUDO=""; else SUDO="sudo"; fi

$SUDO mkdir -p /etc/docker
$SUDO tee /etc/docker/daemon.json >/dev/null <<'EOF'
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
EOF

echo ""
echo "  [OK] 已写入 /etc/docker/daemon.json"
echo "  重启 Docker daemon ..."
$SUDO systemctl daemon-reload 2>/dev/null || true
$SUDO systemctl restart docker || $SUDO service docker restart 2>/dev/null || true
sleep 3

echo ""
echo "  [OK] 完成。验证：docker info | grep -A5 'Registry Mirrors'"
echo "============================================================"
