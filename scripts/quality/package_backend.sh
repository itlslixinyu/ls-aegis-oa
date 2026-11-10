#!/usr/bin/env bash
# 在临时 Maven/JDK21 容器执行：源码挂载 /source（只读），已有依赖缓存挂载 /root/.m2。
# 产物留在容器 /build，不覆盖当前开发服务正在使用的 JAR。
set -euo pipefail
mkdir -p /build
tar -C /source --exclude='target' --exclude='*.jar.original' --exclude='*.jar' -cf - . | tar -C /build -xf -
cd /build
mvn -o -B -pl ls-aegis-boot -am package
