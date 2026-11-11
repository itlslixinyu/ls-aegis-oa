---
name: dependency-clean
description: 复核LS·Aegis 雷铄 OA 安全应用构建平台 Maven/npm依赖的编译、运行、peer和动态消费关系；conservative仅提出依赖方案，不编辑任何manifest或锁文件。
---

# 依赖消费复核

读根 `AGENTS.md` 和 `agents/code-cleanup/AGENTS.md`。输入各pom/package/锁文件、阶段1工具状态、已有依赖树/模块图和运行号。

Maven结合已缓存的dependency:tree、编译/测试/运行范围与第三方调用确认；npm结合PC/H5源码、Vite/plugin、CLI scripts、peer/types、CSS/worker与真实生产模块图。缺工具或缓存不安装/联网升级依赖来完成扫描，只记录证据缺口。

零业务import不是可删证明。注解处理器、Lombok、序列化、SPI/ClassLoader/Agent、自动装配、国密、底层通信、GB28181、调度或队列依赖属于排除/高风险；第三方运行消费必须核对。项目已证实Crane4j使用ByteBuddy，x-file-storage仍消费AWS1，不能按应用显式import误删。

输出 `dependencies.csv` 和依赖方案：保留、人工复核、禁止自动删除及所需验证。未来其他模式的pom/package/lockfile修改只能给预览；**本次不得应用，不提供会被调度器直接执行的卸载/删包命令**，不修改版本、配置或锁文件。方案须区分减少声明、安装体积、产物体积与运行性能。
