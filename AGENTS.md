# LS·Aegis OA 管理平台 Agent

本文件适用于整个仓库。默认用简体中文沟通。先查看 `git status --short`，保留已有未提交修改。用户要求优化时应落实可验证的改动，避免只生成建议；未明确要求时不提交、不推送、不升版本。

## 项目边界

- 这是一个平台，PSI（进销存）、EOMS（运维）、签章、报价是功能域。统一鉴权、RBAC、国密和响应 `R`，不要拆出第二套登录、UI 或部署栈。
- 产品范围：代码生成与应用管理（原 `/open/app`）已按用户要求移除，不再作为待实现或待测功能；系统登录客户端配置仍为统一认证的组成部分。
- 后端：`ls-aegis-server`，JDK 21，Spring Boot 3，启动模块 `ls-aegis-boot`，父 POM 编排 16 个子模块。
- PC：`ls-aegis-web`，Vue 3 + Arco；列表沿用 `GiPageLayout / BaseTable / useTable`，表单复用 `GiForm`。H5：`ls-aegis-h5`，客户和工程师两种 mode，使用 Vant；不要把 PC 的 Arco 约束错误套到 H5。
- 标准端口：PC 5173、客户 H5 5174、工程师 H5 5175、开发 API 8000。生产 Compose 与 `docker/` 开发 Compose 必须分开。

## 任务路由

- 开发、修复、启动：读 `skills/itls-maintain/SKILL.md`。
- 本地目录/文档整理、新仓库 `ls-aegis-oa` 发布准备：读 `skills/itls-repo-organize/SKILL.md`，角色见 `agents/repo-organize/AGENTS.md`；先盘点与规划，不因生成规范而自动删除数据、改版本或操作远端。
- 减负、性能、页面和流程精简：读 `skills/itls-optimize/SKILL.md`。
- conservative代码清理：加载 `agents/code-cleanup/AGENTS.md` 及其列出的7个Skill；按用户指定阶段执行，不删除类/组件/模块/依赖/配置，高风险只标记，基线启动不可用时禁止自动清理。
- 业务梳理、验收、回归：读 `skills/itls-business-test/SKILL.md`；测试代码还遵守 `tests/AGENTS.md`。
- 按工作流从第一项逐功能真实测试、修复复测与断点恢复：读 `skills/itls-workflow-test/SKILL.md`，角色见 `agents/business-flow/AGENTS.md`；复用现有分阶段矩阵，不恢复已移除功能。
- 代码检查与性能回归：读 `skills/itls-code-review/SKILL.md`；独立检查角色见 `agents/code-review/AGENTS.md`。
- 逐模块及最终联合测试：读 `skills/itls-staged-test/SKILL.md`，角色见 `agents/staged-test/AGENTS.md`。用户已指定本机5173/8000为在用测试目标并授权必要操作；按运行号登记与业务补偿清理，不能将隔离建库脚本重定向到在用实例。
- Android 双端规划、开发与验收：读 `skills/itls-android/SKILL.md`，架构见 `android-demo/README.md`，实施角色见 `agents/android-apps/AGENTS.md`。Android 客户端与工程师端属于 EOMS 的Android WebView入口，复用现有后端 API、客户邀请会话和平台 Sa-Token/RBAC，不新建后端或独立认证底座。
- Skills 位于根 skills/，按本节任务路由显式读取，不依赖工具原生自动发现。Skills 是项目级可复用工作流；不同角色可以由同一个 Agent 执行，无需默认开启多个代理。

## 必须保留的业务约束

- 库存数量和成本通过 PSI 单据服务变更，禁止 EOMS 直接写库存。
- 维修结算生成应收，实际收款才产生实收；重试不能重复出库、重复应收或重复收款。
- 数据权限必须覆盖部门、本人、客户归属和租户（启用时）；按钮隐藏不能代替后端鉴权。
- 请求身份缓存须在鉴权前隔离，并在 preHandle 异常或未放行时清理；不能只依赖 afterCompletion。回归入口包含 `SaExtensionInterceptorTest` 和业务脚本的多账号交替访问用例。
- 防重复写操作复用平台 `IdempotentHelper`；不能把没有执行切面的兼容 `RepeatSubmit` 注解当作有效保护。签章的普通/合并入口均需以同用户同请求并发回归验证。
- ID 按字符串传递，避免雪花 ID 被 JS Number 截断。金额计算、事务边界和状态机不能为减少代码而省略。
- 存量 `com.ls.aegis.compat.*` 只能有边界地迁移，不再扩展另一套框架。
- PC 业务 API 从 `@/apis/psi`、`@/apis/eoms`、`@/apis/sign`、`@/apis/quote` 导入；顶层 `@/apis` 保留平台接口，避免同名财务模型和方法互相覆盖。

## SQL、资料与验证

- `ls-aegis-server/sql/init.sql` 是全新安装唯一 SQL。已上线库新增 Liquibase patch 并登记 master；不改已执行 changeset 正文。种子变更同步源脚本和全量 SQL。
- MySQL 使用 UTF-8 无 BOM、`utf8mb4_general_ci`；含中文 SQL 用文件复制后 `mysql --default-character-set=utf8mb4 < 文件`，不用 PowerShell 管道传 SQL。
- 文档入口为 `Docs/README.md`，按 `rules/报告规范.mdc` 分层维护；只维护必要的现行说明和简明版本变更；开发日志、测试报告及私有台账/草稿放被忽略的 `.local/<任务>/<run-id>/`。Agent/Skill 仍为可执行工程规范。
- 现有详细规则位于 `rules/`，按需读取「平台一体」「项目与Git规范」「SQL规范」「前端统一架构」「报告规范」及表格、按钮规则。
- 修改前建立可复现基线；修改后执行对应检查。报告必须区分静态审查、单测、只读冒烟和真实业务闭环；缺账号或数据时标记阻塞，不能声称全业务已通过。
- 不读取/输出真实密钥，不提交 `.env`、数据卷、证书、日志或测试账户 Token。不要把另一个 checkout 或现存容器当成可清理对象。
