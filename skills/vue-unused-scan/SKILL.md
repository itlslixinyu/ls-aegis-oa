---
name: vue-unused-scan
description: 检查LS·Aegis 雷铄 OA 安全应用构建平台 PC及双H5的未使用声明和加载入口，结合ESLint与真实Vite模块图分类，保留动态组件、权限和副作用。
---

# Vue 未使用项扫描

读根 `AGENTS.md`、总调度 `agents/code-cleanup/AGENTS.md`。输入PC/H5源码、各自Vite/tsconfig及运行号；PC为Arco/Gi，H5为Vant两种mode，不混用规则。

1. 使用项目现有ESLint配置和已安装规则，先只读输出JSON及退出码；检查Unused Imports规则是否确实存在，未安装记缺失，不升级ESLint或添加插件。必要时以TypeScript/Vue编译器补充引用候选，不能冒充ESLint结论。
2. 检查script与template、defineExpose/emits/slots、defineProps、动态import、import.meta.glob、后端字符串组件/路由、权限指令、worker URL及注册表。未进某次产物不证明组件永久无用。
3. 引用真实构建图时用项目已有模块/资源门禁；记录其可能写入的dist及生成类型，不把构建副产物当作清理改动。PC用frontend_module_audit/frontend_assets，H5变化用h5_module_audit双模式。
4. conservative只接受LOW普通import和无副作用局部声明。watch/computed/ref初始化可能注册副作用或承担生命周期，必须逐项证明；不删除props、组件、样式文件或公开类型。样式动态class、主题、teleport、权限条件均列复核。
5. 被注释的大块旧template仅在证明已失效且不含安全/协议说明后，交doc-comment-clean处理。权限、运维/灰度、构建环境条件不自动消除。

输出 `vue-unused.csv`，附可运行的只读扫描命令、逐文件修复预览。自动修复命令只允许明确的文件/规则白名单，并由阶段5调度；禁止全仓`--fix`。公开API与组件/模块本次不删除。
