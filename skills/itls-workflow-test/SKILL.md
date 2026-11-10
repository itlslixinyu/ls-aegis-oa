---
name: itls-workflow-test
description: 在ITLS指定在用实例按业务工作流逐功能执行真实测试、记录断点和补偿清理、修复后复测；用于持续业务验收，不把静态扫描或单测冒充实际业务通过。
---

# 逐功能业务实测

读根 `AGENTS.md` 和 `agents/business-flow/AGENTS.md`。复用[模块验收矩阵](../itls-staged-test/references/acceptance.md)及[业务流程](../itls-business-test/references/flows.md)，不恢复已移除的代码生成、应用管理或不存在的模块。

## 执行顺序

1. 锁定目标、HEAD/实际运行包、会话角色、服务基线与原有修改。默认本机PC5173、API8000、客户5174、工程师5175；记录这是当前在用环境，不冒充公网部署。基础服务L0先检查，但不把它标为整个平台底座验收。
2. 从F01正常登录→用户信息→菜单→退出开始，再按矩阵依赖逐项推进平台资料、文件、消息、实际启用的调度、PSI基础资料与单据、报价、运维维修/结算、签章。每次只执行可追踪的小步骤。
3. 各功能先从实际前端请求、Controller、Service和状态机确定输入、允许动作、金额/数量/ID关联及预期。只用授权身份和合成对象，不把权限按钮隐藏作为服务端权限证据。
4. 真正调用对应在用接口或页面，分别验证HTTP、平台R、业务状态及关联对象。先做正常流程和普通业务校验；不进行密码猜测、越权利用、无界压测或任意脚本执行。浏览器不能完成时记录L3缺口，继续允许的真实API步骤。
5. 缺陷有真实实际结果和最小触发前置后，定位并修复，运行适用的编译/单测与候选启动，再以同一业务用例复测。不要将后来不同构建的结果拼成同一验收。真实数据、不可补偿副作用或错误率异常时停止新增写入。
6. 按依赖逆序补偿并独立回查本轮对象、附件引用、净库存/余额及待办；没有实际清理证据不得写清理PASS。每模块保留未测项，全部必测模块退出后再做统一平台联合流程。

## 台账与恢复

证据仅写 `.local/business-workflow/<run-id>/`，每步至少有：`case_id,module,build,role,preconditions,action,expected,actual,status,evidence,defect,retest,cleanup`。用PASS/FAIL/BLOCKED/NOT_RUN，N/A要有部署或已移除范围的证据；模块整体结论不能由L0、单测或部分接口通过推导。

写操作前保存运行号和前置快照，响应后立刻登记精确ID与业务补偿路径。恢复断点时先读未清对象和已执行动作，不盲目重放创建/收款/签章。状态或返回异常但可能已落库时，按本轮唯一标识查证后再决定重试。

密码、验证码答案、Token和真实密钥不得进入Git或报告。凭据只从用户已授权来源进入当前进程/私有运行文件，不重置账户或读取真实.env来凑凭据。按所用工具当前要求处理交互确认，不用旧的一般授权绕过工具限制。

`scripts/quality/run_business_flows.py`会创建独立库、账号和隔离服务，不能改指向在用8000实例；隔离候选回归与在用业务实测必须分别标注。摄像头、真实GPS和地图AK继续跳过。最终结论仅留本地运行台账，列明环境、实际通过项、修复、清理和未完成条件；Git只同步受影响的现行说明。

## 已有执行器

正常认证使用`business_workflow_auth.cjs --phase prepare --run-dir .local/business-workflow/<运行号> --username <已授权账号> --client-id <当前PC公开客户端ID>`，查看本次真实验证码后以`--phase authenticate`通过隐藏标准输入提供已授权凭据。默认完成退出；串行续测可显式`--keep-session`，并在本轮结束调用`WorkflowClient.logout()`。不在命令行参数、源码或报告写密码。

位于`scripts/quality/`的执行器均接收已登录的运行目录：

| 执行器 | 当前覆盖 |
|---|---|
| business_workflow_rbac.cjs | 专用SELF角色、用户、关联/重名/删除保护、停用启用 |
| business_workflow_platform.cjs | 部门父子树、字典及缓存更新 |
| business_workflow_file.cjs | 合成PDF上传、SM3、下载、重命名、回收、还原 |
| business_workflow_notice.cjs | 公告草稿，不发布或广播 |
| business_workflow_psi.cjs | 客户/供应商、联系人、分类商品、仓库；无库存过账 |
| business_workflow_quote.cjs | 草稿/提交往返、金额、HTML及Word/PDF导出 |
| business_workflow_repair.cjs | 报修确认→未接单工单→删除补偿→带原因关闭；无结算 |
| business_workflow_customer.cjs | 客户H5正常邀请/验证码登录→报修→PC确认/带原因关闭→客户回显→退出；按prepare/captcha/run分阶段，未覆盖浏览器或维修结算 |

统一客户端校验目录/运行号/会话目标，默认只接受本机8000。候选验证须单独目录、显式`environment=isolated-candidate-shared-db`及18003，并登记运行包哈希；共享数据库的候选不能宣称数据隔离。各执行器创建前落台账，finally执行精确补偿，支持`--cleanup-only`恢复；已有台账禁止盲目重放。先独立核对清理，再以新运行目录或归档后的新尝试开始下一轮。这些执行器覆盖表内子流程，不代替矩阵所有必测项。
