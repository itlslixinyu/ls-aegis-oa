# Android 双端真实环境测试包

两个 Android 包现在使用原生 WebView 加载项目自己的 H5 页面，登录、验证码、报修、工程师任务和接口请求都来自正在运行的 Docker 平台，不再使用本地模拟数据。客户端和工程师端分别复用 Web 登录页的样式与后端 API。桌面图标使用雷铄科技品牌图，应用名称已移除“模拟”字样。

## 真实 Web 入口

- 客户端：`http://127.0.0.1:25684/customer/auth`
- 工程师端：`http://127.0.0.1:25685/engineer/login`

Docker 端口仅绑定宿主机。手机或模拟器通过 ADB 连接后，在宿主机建立反向端口转发（多设备时为 adb 添加 `-s 设备序列号`）：

```bash
adb reverse tcp:25684 tcp:25684
adb reverse tcp:25685 tcp:25685
```

手机 WebView 访问自己的 `127.0.0.1` 时会经 ADB 转发到宿主机的 H5 容器，再由 H5 Nginx 反代至真实后端。这样无需把服务端口开放到整个局域网。Android 11 及以上也可改用同一 Wi-Fi 下的无线 ADB 配对。

## 安装包

安装包由下方构建命令生成，不随 Git 上传；以 `build/outputs/apk` 中的实际构建产物为准。

构建要求 JDK 21、Android SDK Platform 35 和 Build Tools 35.0.0；工程已包含 Gradle Wrapper：

```bash
./gradlew :customer-app:assembleDebug :engineer-app:assembleDebug
```

Windows 使用 `gradlew.bat`。仓库包含 Gradle 8.11.1 Wrapper JAR 和官方分发包 SHA-256 校验值；本地 SDK 路径通过 `ANDROID_HOME`/`ANDROID_SDK_ROOT` 或被忽略的 `local.properties` 指定。

构建产物：

- `customer-app/build/outputs/apk/debug/customer-app-debug.apk`
- `engineer-app/build/outputs/apk/debug/engineer-app-debug.apk`

## 当前 Windows 本机模拟器

本机 SDK、Gradle 缓存、AVD、调试签名及截图位于仓库 `.local/android/`，不纳入 Git。已经准备 Android 35 / Google APIs x86_64、Pixel 5 配置的 `ITLS_API35`，使用现有 WHPX 加速。先确保统一 Compose 的 25684/25685 入口运行，再从仓库根设置当前进程环境并构建：

```powershell
. ./scripts/quality/use-cleanup-toolchain.ps1
$androidLocal = Join-Path (Get-Location).Path '.local/android'
$env:ANDROID_HOME = Join-Path $androidLocal 'sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = Join-Path $androidLocal 'user'
$env:ANDROID_AVD_HOME = Join-Path $androidLocal 'avd'
$env:GRADLE_USER_HOME = Join-Path $androidLocal 'gradle-cache'
./android-demo/gradlew.bat -p android-demo --no-daemon --max-workers=2 :customer-app:assembleDebug :engineer-app:assembleDebug
```

先用 `$env:ANDROID_HOME/platform-tools/adb.exe devices` 查看设备；专用模拟器 `emulator-5554` 已运行时直接复用，不要重复启动。未运行时执行：

```powershell
Start-Process -FilePath "$env:ANDROID_HOME/emulator/emulator.exe" -ArgumentList @('-avd','ITLS_API35','-port','5554','-memory','2048','-cores','2','-gpu','software','-timezone','Asia/Shanghai','-no-snapshot','-no-boot-anim','-camera-back','none','-camera-front','none','-no-audio') -WindowStyle Hidden
```

等待 `adb -s emulator-5554 shell getprop sys.boot_completed` 返回 `1` 后，再执行安装与启动；设备刚连接时包管理器可能尚未就绪。安装前可用 `adb -s emulator-5554 emu avd name` 核对名称必须为 `ITLS_API35`：

```powershell
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
& $adb -s emulator-5554 reverse tcp:25684 tcp:25684
& $adb -s emulator-5554 reverse tcp:25685 tcp:25685
& $adb -s emulator-5554 install -r android-demo/customer-app/build/outputs/apk/debug/customer-app-debug.apk
& $adb -s emulator-5554 install -r android-demo/engineer-app/build/outputs/apk/debug/engineer-app-debug.apk
& $adb -s emulator-5554 shell am start -W -n com.lsaegis.eoms.customer.demo/.MainActivity
& $adb -s emulator-5554 shell am start -W -n com.lsaegis.eoms.engineer.demo/.MainActivity
```

两个 App 共用一台模拟设备，工程师端最后启动在前台；从 Android 最近任务或桌面切换客户 App。摄像头和音频关闭，无需清空 App 数据或重置数据库。

以上命令复用现有本机 SDK。新机器需先安装 JDK 21、官方 Android 命令行工具，并在 `.local/android/sdk` 使用 `sdkmanager` 安装 `platform-tools`、`emulator`、`platforms;android-35`、`build-tools;35.0.0`、`system-images;android-35;google_apis;x86_64`。使用 `avdmanager` 创建同名 AVD，且将 `ANDROID_USER_HOME` 与 `ANDROID_AVD_HOME` 分别指向 `.local/android/user` 和 `.local/android/avd`。不要把其他设备当作本项目模拟器清理。

登录使用平台中的真实账号。客户首次激活需管理员签发的邀请码；工程师账号由后台管理员分配，图形验证码由后端实时提供。测试业务操作会写入当前 Docker 数据库。

`shared/src/main/assets/index.html` 是旧的离线原型源文件，已从真实环境 APK 的 assets 中移除，不会在新版 App 里运行。
