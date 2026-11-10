# 验证入口与证据边界

| 层级 | 入口/检查 | 证明范围 |
|---|---|---|
| 文档静态 | 路径/锚点/命令与源码对照 | 文档可导航，不证明业务成功 |
| 构建 | Maven、PC build、两端H5 build、Android Wrapper | 编译/类型/打包，不证明登录后流程 |
| 单元 | Maven test、已有node --test用例 | 对应隔离断言 |
| 初始化 | python scripts/quality/verify_mysql_init.py | 独立无网络临时MySQL验证init.sql，结束清理 |
| 本机冒烟 | python scripts/quality/service_smoke.py | 默认8000/5173/5174/5175，9项HTTP/API检查，无登录闭环 |
| 隔离业务 | python scripts/quality/run_business_flows.py --jar-dir 候选JAR目录 | 单独库/网络/随机端口与合成数据；不是当前在用库 |
| 在用业务 | 逐功能Agent/Skill台账 | 正常授权身份及可补偿流程；与隔离结果分开 |
| 移动 | APK构建、模拟器/真机分别记录 | H5页面可用不等于App运行；App登录页不等于完整维修流程 |

运行副作用较大的检查前核实脚本的实际ROOT、镜像/端口/卷、临时资源标签和清理逻辑。不得把隔离执行器改向在用8000或复用在用数据库。不要通过输出实际Token/密钥来证明测试。

验收矩阵唯一来源为[模块矩阵](../../skills/itls-staged-test/references/acceptance.md)、[业务断言](../../skills/itls-business-test/references/flows.md)和[标准范围](../../skills/itls-staged-test/references/standards.md)。这些是执行标准，不是认证或已通过声明。

每条结果记录构建指纹、日期、环境、角色、前置、预期/实际、PASS/FAIL/BLOCKED/NOT_RUN及清理证据。库存、应收应付和实收按业务补偿核对，不删除审计日志“清场”。本轮实际结果见[维护记录](../releases/maintenance.md)，旧测试只见[历史原文](../archive/legacy-reports.md)。

摄像头、真实GPS、地图AK继续按用户指定跳过。模拟器、真机、外部服务和性能容量不能混写成已通过。发布候选的验证不自动替换在用2.2.0镜像或宣称正式生产已升级。
