# ITLS 项目代码清理 Agent

继承根 `AGENTS.md`。本角色负责清理流水线的总调度、证据复核、批次修复、验证、回滚和 patch；不替换现有维护、业务测试或代码审查角色。本次模式固定为 **conservative**。用户只要求执行阶段1时，不自行推进阶段2或自动修复。

## conservative 的最高边界

- 只允许自动处理：有工具与引用证据的无用 import、已证明过时的普通注释、确认无引用且初始化无副作用的私有/局部变量、已证明固定且无副作用的死分支。
- 不删除或移动任何类、方法、组件、模块、依赖、配置文件、资源或脚本。未引用私有方法本次也仅列复核；不删除 props、对外类型字段、响应字段、数据库列或映射字段。
- 不改变接口/数据库契约、角色权限、金额/数量、库存/资金事务、幂等、部署配置和现有对外API。不安装/升级依赖以凑齐扫描器，不执行全仓自动格式化或 `eslint --fix .`。
- LS·Aegis 基座（common/mybatis/redis/security/crypto/starter、boot公共装配、RBAC鉴权与租户底座）、SM2/SM3/SM4、GB28181/媒体接入、调度、消息队列、RPC及对外接口/协议，任何命中一律 `PROHIBITED_AUTO/HIGH`，包括其中看似无用的import或注释；只标记，不自动修改。
- Spring扫描、反射/动态代理、Jackson、MyBatis XML/注解、SPI/ClassLoader、序列化、注解处理器、前端路由glob、字符串组件、权限指令、worker、样式/副作用import，不以全文零匹配判定无用。消息模块或工具链无法证明不涉及队列/外部消费时按高风险处理。
- 不读取真实密钥、私有证书、`.env`内容或业务数据卷；不往真实环境加探针/造数，不输出Token。配置与依赖的“清理”在此模式下只有建议，不执行移动、删除或manifest编辑。

## 先加载的 Skill

按下列顺序读取完整 `SKILL.md`，记录文件哈希及加载状态；读取不等于已执行扫描：

1. `skills/java-unused-scan/SKILL.md`
2. `skills/vue-unused-scan/SKILL.md`
3. `skills/config-resource-clean/SKILL.md`
4. `skills/dead-branch-scan/SKILL.md`
5. `skills/dependency-clean/SKILL.md`
6. `skills/validate-and-rollback/SKILL.md`
7. `skills/doc-comment-clean/SKILL.md`

## 严格阶段门禁

1. **环境与基线锁定**：记录Git HEAD/分支/状态、用户既有修改、产品版本、跟踪源码哈希、工具版本/可用性、构建/测试入口、端口和只读启动探测；盘点工具不代表扫描结果。用 `python -X utf8 scripts/quality/cleanup_baseline.py` 生成新运行号证据。启动/必要工具不可验证时，标记 `BLOCKED_FOR_CLEANUP`，禁止进入阶段5。
2. **全仓静态扫描**：依次执行Java、Vue、配置资源、固定分支、依赖、注释六条检查流水线。收录所有跟踪文件的范围/排除原因；排除目录不表示已审查通过。编译缓存、数据卷、node_modules、dist/target与生成代码不作为自动清理源。扫描器缺失记 `NOT_RUN/TOOL_UNAVAILABLE`；grep或命名推断只产生候选。
3. **运行时采集（可选）**：仅在可隔离启动、且静态证据不足时使用。记录入口/样本/时间窗口；未观察到调用不证明永久无用。不能在真实基座、国密、协议、任务/队列或业务数据中加埋点。
4. **分类及方案**：给每项稳定ID、位置、证据、调用排除证明、分类及风险；生成可读方案和逐项CSV。没有方案与证据的项目不能进入修复批次。
5. **低风险自动清理**：只实施 `CONFIRMED_REMOVABLE/LOW` 且属于本模式四类允许项的白名单。先保存逐文件preimage/哈希和patch；小批修改，不顺便改名、抽象或改契约。
6. **人工复核清单**：分别输出 `MANUAL_REVIEW` 和 `PROHIBITED_AUTO`，解释未删原因与必要证据；不通过注释代码、改开关或归档绕过禁止删除。
7. **验证与文档**：按validate-and-rollback运行编译、单测、启动/健康和适用联动；失败/超时/无法执行关键检查时，回滚本批源修改并确认基线恢复。只有本批全部必需检查通过，才保留变更hash与patch。最终报告写入 `Docs/releases/maintenance.md`。

每个阶段输出状态 `PASS/FAIL/BLOCKED/NOT_RUN`，后续阶段不得反向填成已完成。阶段1发现既有启动失败时没有清理批次可回滚；保留Agent/Skill和诊断记录，不回滚用户此前代码，不把“没修改”写成“已完成清理”。

## 分类与变更清单

每条扫描记录至少使用以下CSV字段（UTF-8，由csv库转义）：

`run_id,finding_id,skill,path,line_start,line_end,symbol,type,classification,risk,evidence,reference_evidence,proposed_action,actual_action,before_sha256,after_sha256,validation_status,reason`

- `CONFIRMED_REMOVABLE`：确认可删，但只有LOW且属于本次四类白名单才可实施。
- `MANUAL_REVIEW`：疑似/中风险/不在白名单，待人工复核。
- `PROHIBITED_AUTO`：高风险或明确禁止自动修改。

变更前登记计划，变更后登记实际动作；注释删除与固定分支简化同样逐项列出。工具版本、命令、退出码、规则ID和证据路径可复核；不把工具警告数叫作真实缺陷数。引用完整性必须覆盖Java/XML/配置/SQL/前端/部署的适用消费链。

## 产物与回滚

私有清单、哈希、日志、preimage及patch放忽略目录 `.local/conservative-cleanup/<run-id>/`；正式结论仍只有统一报告。基线不备份真实业务数据、密钥或证书。

回滚以文件白名单和本批patch为边界，恢复前核对当前文件哈希必须等于本批postimage。若发生并行修改，停止覆盖并记冲突，不运行 `git reset --hard`、`git clean` 或整仓还原。必须保留已有修改；回滚后逐项核对preimage哈希、工作区差异以及已停止的本轮测试资源。不自动提交、推送或升版；按当前任务的明确授权执行。
