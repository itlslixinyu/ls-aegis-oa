---
name: itls-android
description: 规划、实现和验收 LS·Aegis 雷铄 OA 安全应用构建平台 EOMS 客户与维修工程师 Android 双端，覆盖平台认证、维修流程、接口契约、设备能力和后台财务库存闭环。
---

# LS·Aegis 雷铄 OA 安全应用构建平台 Android 双端

先读仓库根 `AGENTS.md`、[双端架构](../../android-demo/README.md) 和 [Android 实施 Agent](../../agents/android-apps/AGENTS.md)。这是项目显式路由的 skill，不假定 IDE 会自动发现它。

## 执行流程

1. **定范围**：检查 `git status --short`；确认本任务属于需求、接口、工程底座、客户端、工程师端、联合验收或发布准备。用户明确要求开发时继续实施，不止给方案。平台与两端App按用户授权同步发布版本，两端versionName与根VERSION一致，versionCode同步递增；运行version_consistency.mjs，不以版本修改代替构建和验收。
2. **查现状**：从 H5 `src/api`、视图、路由和工具层定位，再追踪 EOMS Controller、Service、Mapper 及测试。记录接口当前真实方法、路径、鉴权头、请求/响应、状态条件和失败行为。
3. **建立契约**：逐项映射客户端 `H5AuthController`/`H5RepairController` 与工程师 `EngineerController`，覆盖 `/auth/*` 平台登录和 EOMS API。新增或复用接口时，核对App/H5→HTTPS网关→H5 Nginx/Vite→后端的完整路径和实际HTTP方法，尤其PATCH不能落入SPA静态处理；自助改密使用`/user/profile/password`。不要为方便改变服务端安全约束；如需接口扩展，提出同一 EOMS 后端内的最小契约变更并同步全量安装 SQL/Liquibase 规则（仅当数据结构确需变更）。
4. **分层实现**：维护现有Android Gradle工程及两个独立application，复用既有WebView/H5链路；原生重构或共享core须有明确需求，不作为目录整理的默认动作。锁定工具版本前核对 JDK 21/CI/Android Studio 支持，不升级后端或其他前端依赖来迁就移动端。
5. **落实双会话**：客户首次/重置时用管理员一次性邀请码绑定自设用户名与密码，后续使用用户名密码+图形验证码换取 `h5-token`；密码按平台 SM2 传输协议加密。工程师平台账号走统一登录/权限。安全存储、刷新/退出、失效、账号切换和并发请求行为分别实现，不共享 TokenStore。
6. **完整闭环**：客户提交、查询、确认；工程师接单、开工、记录、照片、完工；随后验证 PC 派单/结算、PSI 出库和应收、真实收款的既有语义。
7. **验证并汇报**：按本仓库已有build/test命令及本Skill的最小验收入口执行；架构边界见 `android-demo/README.md`；用户未要求测试时不自行扩大到高副作用的在用环境写入。区分编译、单测、模拟器、真机和业务闭环，不虚报通过。

## 实施约束

- ID 全程字符串；金额使用十进制定点表示；统一解析 `R.success/code`，保留服务端错误语义。
- 客户/工程师权限由后端验证。对客户、工程师、租户、部门、工单和附件归属都设计越权反例。
- 状态变更操作不离线、不自动重放；网络失败后先查状态再决定是否重试。优先使用服务端现有幂等保护。
- 图片上传遵循 JPEG/PNG 等服务端当前限制、归属绑定、水印和文件访问规则；定位/相机/相册按需申请，拒绝权限有可用的业务回退。
- 不把密钥、真实 Token、个人资料、验证码和附件内容写入日志、提交、崩溃上报或公开存储。
- Android 构建参数、签名、推送和地图供应商均用配置/安全设施；未获决策前不硬编码密钥或引入新供应商。
- 客户端不能直接修改库存与财务流水。结算生成应收，实际收款才形成实收；并发/重试不可重复出库、应收或收款。

## 最小验收入口

- `EOMS-ANDROID-C-01`：首次激活/管理员重置、用户名密码登录、验证码、报修、附件、列表/详情、确认、退出和过期。
- `EOMS-ANDROID-E-01`：统一登录、任务授权、抢单并发、状态机、记录/附件、定位拒绝和完工。
- `EOMS-ANDROID-X-01`：客户→PC 派单→工程师→客户确认→后台结算→PSI 出库/应收→实收。
- `EOMS-ANDROID-SEC-01`：跨应用会话、撤销、越权、附件访问、ID 与本地存储/日志。
- `EOMS-ANDROID-UX-01`：弱网、超时、空/错状态、权限拒绝、小屏和系统字体放大。

当前架构见 `android-demo/README.md`，验收范围按本Skill的最小入口展开。详细验收记录仅保存在本地 `.local/android/<run-id>/`，项目文档只更新有效的使用和构建说明。

H5入口或代理发生变化时，构建对应版本镜像后运行`python -X utf8 scripts/quality/run_business_flows.py --h5-proxy`，让密码恢复闭环经过真实Nginx配置；另执行`service_smoke.py`的未登录PATCH鉴权检查。仅直连API、模拟组件或下载静态资源不能代表手机实际接口链路通过。
