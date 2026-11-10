# 适配 JDK21 运行 ls-aegis-server.jar
# 构建机若无法直连 Docker Hub，可先：
# docker pull docker.m.daocloud.io/library/eclipse-temurin:21-jre-jammy
# docker tag docker.m.daocloud.io/library/eclipse-temurin:21-jre-jammy eclipse-temurin:21-jre-jammy
FROM eclipse-temurin:21-jre-jammy

# 水印等 AWT 场景可用 Noto CJK；PDF 导出须 TrueType(glyf)，见下方 COPY fonts/
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        fontconfig fonts-noto-cjk \
    && fc-cache -fv \
    && rm -rf /var/lib/apt/lists/*

# TrueType 中文字体（如 simhei.ttf），供 openhtmltopdf 导出；勿用 Noto CJK OTF
COPY fonts/ /usr/share/fonts/truetype/aegis/
RUN fc-cache -fv || true

# 将 mvn 产物 ls-aegis-server.jar 放入 docker/ls-aegis-server/bin/
ARG JAR_FILE=./bin/ls-aegis-server.jar
COPY ${JAR_FILE} /app/bin/ls-aegis-server.jar
# WORKDIR 必须为 /app，使种子路径 data/file/ 落在挂载卷 /app/data/file/
WORKDIR /app

ENTRYPOINT ["java", \
            "-jar", \
            "-XX:+UseZGC", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dspring.profiles.active=prod", \
            "/app/bin/ls-aegis-server.jar"]
