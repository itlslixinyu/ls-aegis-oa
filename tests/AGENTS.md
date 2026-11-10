# 业务流程测试 Agent

继承根 `AGENTS.md`，先读取 `skills/itls-business-test/SKILL.md`。

- 从实际 Controller、Service 状态机和前端操作建立测试，不能按菜单名称猜流程。
- 当前用户要求默认在已确认的在用实例逐模块测试，按 `skills/itls-staged-test/SKILL.md` 执行；隔离夹具脚本仍只用于其原独立环境。使用专用数据前缀 `ITLS-TEST-<run-id>`，记录精确ID并经业务撤销/冲销清理，不能改数据库制造“通过”。
- 无登录态时只执行只读冒烟和未登录拦截。登录后的流程测试需要管理员、主管、工程师、客户、财务等不同角色；不重置既有管理员密码来获取测试入口。
- 通过必须同时具有预期状态、关联记录、金额/数量、权限和持久化证据。失败需记录可复现步骤及最小输入；Token、密码、手机号等不得写入报告。
- `scripts/quality/service_smoke.py` 仅验证可用性与未登录拦截；`scripts/quality/verify_mysql_init.py` 使用独立无端口临时容器验证全新安装。两者均不能代替完整业务验收。
- 正式结果更新 `Docs/releases/maintenance.md`，每条用例标记 PASS / FAIL / BLOCKED / NOT_RUN，附环境、时间、实际结果及证据位置。
- 登录后的 API 回归入口为 `python -X utf8 scripts/quality/run_business_flows.py`。身份隔离用例必须交替使用不同角色并先触发权限拒绝，覆盖 preHandle 异常时 Spring 不回调本拦截器 afterCompletion 的分支，防止线程复用串用身份。
