---
name: itls-business-test
description: 梳理并验证 ITLS 的报价、进销存、运维报修、财务和签章业务闭环，覆盖状态流转、库存金额、角色权限与幂等反例。
---

# ITLS 业务流程测试

读根 `AGENTS.md` 和 [流程用例](references/flows.md)。先确定环境、真实 API、状态机、角色、测试数据和可执行范围。没有账户或前置数据时完成静态梳理与只读冒烟，把依赖项标记 BLOCKED，继续独立可执行的检查。

`python scripts/quality/service_smoke.py` 是 L0 服务基线；`python scripts/quality/verify_mysql_init.py` 是隔离的全新安装验证。L1 是核心算法与状态机单测；L2 是登录后的跨模块 API 闭环；L3 是 PC/H5 实际操作、反馈、列表回显和导出核验。不能用较低层通过替代较高层。

已有后端启动 JAR 和 PC 依赖时，使用 `python -X utf8 scripts/quality/run_business_flows.py` 执行 L2。它会创建带唯一标签的独立 MySQL/Redis/后端、专用账号和合成文件，验证后清理；运行资料保存在忽略的 `.local/`。PDF 测试需要项目字体目录或 Windows 系统中的 `simhei.ttf`。`--keep` 仅用于失败复核，之后用 `--cleanup` 清理；不要把当前开发库改成测试库。测试证书为隔离环境自签，不证明外部信任链。H5 验证码由专用 Redis 读取以测试 API，不代表图像交互通过。

更新后端时可用 `--jar-dir <候选包目录>` 先验证候选包，文件名为 ls-aegis-server.jar，不覆盖正在使用的开发 JAR。PSI-03-WORKBENCH 用独立 MySQL 只读聚合核对 20 个摘要字段；日期按测试后端 Asia/Shanghai 口径，不能误用数据库容器的 UTC 日期。月份边界还需结合 PsiWorkbenchServiceImplTest 的确定日期反例，不把当前日期的一次 API 测试当作全部日期覆盖。

执行 L2/L3 时使用专用测试账号和本轮数据前缀，记录角色、关联 ID、前后快照与响应；每一步同时检查 HTTP 与 `R.success/code`。对写操作重试、并发提交、跨部门/跨客户访问、非法状态、库存不足、超额收款、过期登录分别验证。断言数据库与 UI 最终一致，不打印敏感字段。

测试域外依赖（短信、正式签章证书、付款服务）使用已配置测试服务或明确标记阻塞，不发送真实业务通知。清理仅针对本轮对象，已记账数据走应用支持的撤销/冲销，禁止直接删财务和库存流水。

结果格式：用例 ID、环境与时间、角色、前置、操作、预期、实际、证据、PASS/FAIL/BLOCKED/NOT_RUN、清理结果。正式结果写 `Docs/releases/maintenance.md`；静态代码支持的预期不能填成实际执行结果。
