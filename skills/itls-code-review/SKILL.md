---
name: itls-code-review
description: 检查 ITLS 性能优化和跨域改动中的真实缺陷，验证请求并发、加载依赖、查询规模及统一平台的数据边界。
---

# ITLS 代码与性能检查

H5组件加载改动后执行 `node scripts/quality/h5_module_audit.mjs --check`，真实构建客户/工程师两种模式；检查模板组件已解析、未保留Vant整套注册入口、Toast/Dialog/Notify函数式样式齐全，且不包含另一角色的业务页面。产物体积变化不等同页面交互验收。

读根 `AGENTS.md` 并保留已有修改。适用于用户要求代码检查、继续优化，或回归公共底座与跨域改动；普通单点修复不必扩展成全库审计。

沿真实调用链检查触发条件、返回顺序和最终状态。重点判断：旧请求是否覆盖新选择（包括 A→B→A）、卸载/缓存页恢复是否留下请求或定时器、菜单或权限加载失败是否影响会话、查询是否把大批明细加载到 Java/浏览器后只做求和。

检查模块关联时沿用平台服务：报价使用 PSI 商品，维修出库经 RepairStockOutApi，应收与实收分离，文件/签章沿用统一授权。优化聚合、缓存或批量查询时核对租户、部门、逻辑删除、金额精度和空集合语义；不能只用耗时降低证明正确。

当前 DataPermissionMapper 的 selectList 显式标有 DataPermission，而 selectMaps 未覆盖该注解；不要为做 SQL 汇总直接替换查询方法。使用新聚合方法前单独验证数据权限和租户拦截。

前端行为回归入口：`node --test scripts/quality/frontend_performance.test.cjs`。构建后用 `node scripts/quality/frontend_assets.mjs --check` 核对 JS/CSS 静态依赖与异步预览边界。需要资源对照时读这两个脚本及 `frontend_preview_baseline.mjs`，区分传输体积、渲染耗时与构建耗时。

核对调度依赖加载边界时，先运行PC类型检查，再执行 `node scripts/quality/frontend_module_audit.mjs --check`。该命令使用现有Vite配置执行真实生产构建，并把模块图写入.local；验证首页不加载Cron/Luxon而调度页仍可用，不以源码import数量或压缩体积代替实际模块图。

PC按需组件改动还须通过 `node --test scripts/quality/pc_arco_loading.test.cjs`，保留GiForm完整公开控件映射、函数式弹窗上下文、上传配置及动态菜单图标。模块图门禁检查未解析a-*组件（仅允许显式全局Tooltip）和整套Arco安装入口，包含独立音频预览应用，不能只删除main中的安装调用。财务只读汇总可用既有隔离对比工具的 `--scenario=writeoff-summary`；显式指定前后候选包，只对合成栈造数，样本和并发受脚本限制，结束后核对清理。

静态分发配置变更时，用 `python -X utf8 scripts/quality/verify_nginx.py` 在临时容器验证实际响应，不以配置文字或离线 gzip 估算代替 HTTP 验证。检查依赖 Docker、生产 dist 和指定 Nginx 镜像，不触及生产部署。

后端变更按实际模块执行 Maven 测试；涉及库存/财务/签章时继续按 itls-business-test 执行独立环境用例。缺少运行权限时完成可运行的检查，明确未执行项，不以旧日志替代当前回归。

发现必须能给出文件位置、触发场景、实际影响和证据；先验证再修复。针对并发、状态、金额或查询边界新增有区分力的回归，不写只匹配源码文字的测试。普通格式问题不冒充业务缺陷。正式结果追加到 `Docs/releases/maintenance.md`。
