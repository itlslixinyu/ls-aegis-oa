# 业务闭环和断言

以下是由当前源码定位出的验收基线。执行前重新检查 Controller 和 Service；状态值、权限字符串与必填字段以当前实现为准。

| ID | 角色与主链 | 必须验证的结果 | 关键反例 |
|---|---|---|---|
| AUTH-01 | 管理员登录→菜单→受保护接口→退出 | 国密公钥可用；菜单与权限一致；退出后 Token 失效 | 未登录、过期、无权限、跨部门/客户归属 |
| EOMS-AUTH-01 | 管理员签发邀请码→客户激活用户名密码→登出→密码登录→客户报修 | 用户名全局唯一并绑定同租户客户；口令经 SM2 传输加密及 PasswordEncoder 摘要；登录失败通用反馈；会话仅发放给启用客户 | 邀请码过期/重放、用户名冲突、错误客户用户名重置、密码错误锁定、验证码失败、跨租户客户编号碰撞 |
| PSI-01 | 采购→入库→审核→库存/流水→应付→付款/核销 | 明细数量和金额一致；只记一次库存；应付余额按核销变化 | 重复审核、非法反审、负数量、无权限 |
| PSI-02 | 销售→出库→审核→库存成本→应收→收款/核销 | FIFO 成本与库存流水一致；余额可追溯至单据 | 库存不足、重复审核、超额核销、跨仓库越权 |
| PSI-03-WORKBENCH | 统一工作台摘要→独立数据库汇总回查 | 应收/应付/资金笔数与金额、本月/30天单据数量金额与明细数量一致 | 空集合、月份边界、负数/空金额；优化不能扩大数据权限 |
| PSI-CASH-SUMMARY-01 | 合同/出入库→同一资金单多次核销→汇总→逐次反核销 | 回款/付款两侧的主单ID、金额及原因一致；反核销后隐藏对应汇总并恢复未核销/未分配余额 | 无权角色拒绝；销售出库先确认销售合同；不能以读取优化改写财务事务或锁顺序 |
| EOMS-01 | 客户 H5 报修→PC 派单→工程师 H5 接单/开工→维修记录/材料→提交→结算确认 | 客户只见本人报修；工程师只见授权任务；每个状态允许动作正确 | 非工程师派单、抢单并发、跳过维修直接结算 |
| EOMS-02 | 待结算工单→结算确认→PSI 维修出库→应收→实际收款 | 确认是原子事务；REPAIR 出库一次；应收一次；确认本身不产生实收 | 重复/并发确认、库存不足回滚、重复/超额收款 |
| QUOTE-01 | 独立报价草稿→编辑/提交→确认归档→导出；另外回归现有转工单兼容入口 | 报价可独立完成，不强制关联工单；兼容转换关联唯一；终态内容保留；PDF/DOCX 金额一致 | 已确认/已取消继续编辑或再次转换、重复点击 |
| SIGN-01 | 项目/文件/印章准备→可签项目→prepare→run/unified→回执/验证/下载 | 仅授权且可签的项目；文件和印章归属一致；签章及回执可核验 | 无权限项目、终止项目、越权文件、5 秒内重复提交、下载他人回执 |
| X-01 | 报价→EOMS 工单→维修结算→PSI 出库→财务 | 各域使用同一客户/商品/仓库标识；关联可追溯；汇总不重复计数 | 重试、部分失败回滚、前端 ID 精度、刷新后回显 |

## 源码定位

- PSI：`ls-aegis-server/ls-aegis-biz-psi/src/main/java/com/ls/aegis/biz/psi/`，先读对应单据 Controller、Service 和库存支持类；算法测试在同模块 `src/test/java`。
- 派单与状态：`ls-aegis-server/ls-aegis-biz-eoms/src/main/java/com/ls/aegis/biz/eoms/controller/repair/RepairOrderController.java`，API 前缀 `/eoms/repair/order`，动作 `assign/accept/start/submit/close/{id}`。
- 结算：同域 `controller/repair/RepairSettlementController.java` 与 `service/impl/repair/RepairSettlementServiceImpl.java`；确认是 `PUT /eoms/repair/settlement/confirm/{id}`。检查 CAS、事务、PSI 出库、应收和实收的区别。
- 报价：`ls-aegis-server/ls-aegis-biz-quote/src/main/java/com/ls/aegis/biz/quote/controller/QuoteController.java` 与 `service/impl/QuoteServiceImpl.java`；转换是 `PUT /quote/{id}/to-order`。
- 签章：`ls-aegis-server/ls-aegis-biz-sign/src/main/java/com/ls/aegis/biz/sign/controller/SignExecuteController.java`；`/sign/execute/projects`、`prepare`、`run`、`unified`、`receipt/{recordId}`。当前有类级登录校验和权限注解，不能只测按钮。
- 前端：PC `ls-aegis-web/src/views/{psi,eoms,quote,sign}`；H5 `ls-aegis-h5/src/views/{customer,engineer}`。

## 测试数据与证据

准备客户、供应商、商品、仓库、初始库存、工程师、财务和测试印章。记录起始库存/应收/应付/实收；结束时用单据明细复核增量。金额示例可选数量 2、单价 100，但税额、折扣、舍入和库存成本预期必须从真实业务规则计算，不预设税率。

用例缺账号、证书或可撤销测试数据时标记 BLOCKED；未安排执行标记 NOT_RUN；只有真实执行且断言均成立才能标记 PASS。
