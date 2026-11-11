---
name: java-unused-scan
description: 对LS·Aegis 雷铄 OA 安全应用构建平台 Java源码执行未使用代码证据扫描与风险分类；conservative仅允许低风险import及无副作用私有变量，不自动删除方法或类。
---

# Java 无用代码扫描

先读根 `AGENTS.md` 与 `agents/code-cleanup/AGENTS.md`，继承其模式、阶段和CSV契约。输入为后端源码根、排除目录和阶段1运行号。

1. 确认可用的IDEA检查、已有Sonar结果、PMD或JDK jdeprscan，记录版本、规则及命令。jdeprscan仅证明废弃API使用，不能用来证明类或变量无引用；缺少适当扫描器时标记未执行，不临时改pom安装扫描插件。
2. 映射17个reactor项目（父工程与16子模块）；基座、国密、boot装配等高风险区域只登记范围/候选，不自动修改。生成源/target排除并登记原因。
3. 对每个候选核对Java调用、MyBatis XML/resultMap、Jackson属性、Spring Bean/注解扫描、AOP、SPI、反射、RPC以及测试/配置。Lombok生成访问器、序列化字段、被框架读取的私有字段不是“无引用”。
4. 只有工具确认且全链路排除动态消费的普通import可列LOW；私有/局部变量还须证明初始化和求值无副作用、无反射/注解契约。静态初始化、工厂调用、锁/资源、事件注册、时序影响不得丢弃。
5. public/protected、类、方法、废弃常量/注解一律只列复核；`@Deprecated`不是可删证明。命中总调度高风险列表时优先标记PROHIBITED_AUTO。

输出 `java-unused.csv` 和工具原始日志，使用共享字段，附规则ID、位置、证据与建议。仅在阶段5生成白名单patch；阶段2不执行IDE或PMD批量修复。自动删除私有方法的能力不适用于本次conservative。
