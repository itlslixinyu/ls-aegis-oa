# Android应用架构

## 双端模块

工程包含customer-app与engineer-app两个独立应用模块，分别承载客户门户和工程师入口。当前采用Android Activity与WebView，业务页面、状态和权限来自对应H5及平台后端。

| 模块 | 页面入口 | 身份来源 |
|---|---|---|
| customer-app | 客户H5 /customer/auth | 邀请激活后的客户会话 |
| engineer-app | 工程师H5 /engineer/login | 平台工程师账户 |

包名保留现有标识以维持应用身份。versionName与工程版本保持一致，versionCode用于Android安装升级。

## 页面与后端关系

```mermaid
flowchart LR
    C[customer-app WebView] --> CH[客户H5]
    E[engineer-app WebView] --> EH[工程师H5]
    CH --> API[统一EOMS / 平台API]
    EH --> API
```

两端使用相同的WebView设置：启用JavaScript与DOM存储，禁止文件和Content访问，使用WebViewClient承接页面导航。不包含独立离线业务页面，也不在Android侧复制库存、结算或签署逻辑。

## 入口与网络

当前入口常量位于各模块MainActivity：客户为 `http://127.0.0.1:25684/customer/auth`，工程师为 `http://127.0.0.1:25685/engineer/login`。设备上的回环地址指向设备本身；采用该配置时，需要ADB反向端口连接宿主H5服务。

```bash
adb reverse tcp:25684 tcp:25684
adb reverse tcp:25685 tcp:25685
```

面向独立设备部署时，应明确设置实际服务入口及HTTPS策略，不能把宿主回环地址当作公网地址。页面可提供的设备能力受WebView与当前Activity实现约束。

## 构建链

工程使用JDK21、Android SDK35和Gradle Wrapper，分别生成两端APK：

```bash
./gradlew :customer-app:assembleDebug :engineer-app:assembleDebug
```

Windows使用gradlew.bat。SDK位置由ANDROID_HOME、ANDROID_SDK_ROOT或本机local.properties提供；SDK、缓存、签名和构建产物独立管理，不随源码提交。
