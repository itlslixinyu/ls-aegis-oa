# 开发与构建

适用源码开发与隔离构建。当前在用环境已由完整 Compose 承载，启动 Vite/本机后端前先检查端口，避免与容器冲突。

要求 JDK 21、Maven、Node、pnpm 9；H5 使用 npm 和自己的 package-lock。工具版本以锁文件及构建配置为准。本机已有验证工具链可在 PowerShell 执行 `. ./scripts/quality/use-cleanup-toolchain.ps1`，该入口依赖不随Git发布的 `.local/toolchains`，新机器需自行准备工具。

从仓库根执行：

```powershell
pnpm --dir ls-aegis-web install --frozen-lockfile
npm --prefix ls-aegis-h5 ci
mvn -f ls-aegis-server/pom.xml -pl ls-aegis-boot -am package -DskipTests
pnpm --dir ls-aegis-web build
npm --prefix ls-aegis-h5 run build:customer
npm --prefix ls-aegis-h5 run build:engineer
```

`-DskipTests` 跳过测试；打包成功不是单测或业务验收。PC build 包含类型检查，两端H5必须分别构建。

开发需要 MySQL/Redis、真实本机配置和已初始化的库。`docker/dev-up.ps1` 使用开发 Compose；本机后端入口为 `docker/dev-run-server.ps1`。不要将开发依赖配置与已有完整部署混起。前端开发命令分别是 `pnpm --dir ls-aegis-web dev`、`npm --prefix ls-aegis-h5 run dev:customer`、`npm --prefix ls-aegis-h5 run dev:engineer`。

标准端口：PC 5173、客户5174、工程师5175、API8000。数据库与密钥配置模板和实际文件分开，不将真实 `.env` 提交。Android 构建入口见[专用说明](../../android-demo/README.md)。

修改规范按[平台约束](../architecture/platform.md)，验证按[分层检查](../testing/verification.md)。

## 测试JVM配置

Windows开发建议使用Node 22和pnpm 9。可选脚本 `use-cleanup-toolchain.ps1` 只在已有本地工具目录时使用；新机器先安装所需工具，不依赖他人的.local目录。

若测试环境限制Mockito动态自附加，可使用Byte Buddy启动期Agent；只配置测试JVM，不改变生产JVM。依赖已由Maven解析时，可从仓库根执行：

```powershell
$testAgentJar = Join-Path $env:USERPROFILE '.m2/repository/net/bytebuddy/byte-buddy-agent/1.14.19/byte-buddy-agent-1.14.19.jar'
mvn -f ls-aegis-server/pom.xml -pl ls-aegis-boot -am package "-DargLine=-Xmx512m -javaagent:`"$testAgentJar`""
```

Agent版本以当前测试依赖为准；依赖未下载时先按Maven正常解析。启动期Agent机制见[Byte Buddy官方说明](https://github.com/raphw/byte-buddy/blob/master/byte-buddy-agent/src/main/java/net/bytebuddy/agent/ByteBuddyAgent.java)。
