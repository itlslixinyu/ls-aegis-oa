# 开发与构建架构

## 工程与工具链

| 工程 | 工具 | 产物 |
|---|---|---|
| ls-aegis-server | JDK21、Maven | Spring Boot应用JAR |
| ls-aegis-web | Node22、pnpm9、Vite | PC静态资源 |
| ls-aegis-h5 | Node、npm、Vite | 客户和工程师两套静态资源 |
| android-demo | JDK21、Gradle Wrapper、Android SDK35 | 客户和工程师APK |

PC以pnpm-lock.yaml锁定依赖，H5以package-lock.json锁定依赖。不同工程的锁文件和包管理器分别维护，不能混用。后端由父POM统一管理模块和依赖版本。

## 构建链

```text
Maven父工程 → 基础与业务模块 → boot打包 → JAR → 后端运行镜像
PC源码 → 类型检查 → Vite构建 → 静态资源 → Nginx镜像
H5源码 → customer / engineer路由选择 → 分端资源 → 各端Nginx镜像
Android双模块 → Gradle打包 → 各端APK
```

主要构建入口：

```bash
mvn -f ls-aegis-server/pom.xml -pl ls-aegis-boot -am package
pnpm --dir ls-aegis-web install --frozen-lockfile
pnpm --dir ls-aegis-web build
npm --prefix ls-aegis-h5 ci
npm --prefix ls-aegis-h5 run build:customer
npm --prefix ls-aegis-h5 run build:engineer
```

生产Dockerfile采用源码多阶段构建，不依赖开发机的JAR、node_modules或dist。Android构建与服务端镜像分离。

## 配置分层

源码保存默认配置和环境模板；后端通过profile与环境变量注入运行参数，前端通过Vite模式确定公开配置和代理入口。密码、私钥和其他服务端秘密不进入前端环境变量。

开发依赖由docker目录中的开发Compose提供，应用可在宿主机运行。完整容器部署与宿主开发共用默认入口时会发生端口占用，应选择一种应用运行方式。服务编排和存储关系见[部署架构](../operations/deployment.md)。
