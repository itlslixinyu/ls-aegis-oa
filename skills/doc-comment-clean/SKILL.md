---
name: doc-comment-clean
description: 核实LS·Aegis 雷铄 OA 安全应用构建平台过时普通注释与废弃TODO的事实依据并生成逐项清单；保留许可、安全、国密、协议和对外API说明。
---

# 过时注释核对

读根 `AGENTS.md` 和 `agents/code-cleanup/AGENTS.md`。输入源码、文档/接口实际消费链及运行号。

对普通注释/注释掉的旧业务块/TODO，核对当前实现、调用方和可用历史依据；日期旧、措辞不美观、含TODO或不在一次测试中执行都不是过时证明。已完成TODO必须有对应实现/验证证据。

许可/作者归属、安全边界、数据权限、事务/并发警告、国密算法、GB28181/对接协议、外部API契约及配置操作说明均保留，高风险区域只标记。不能误删eslint/ts-ignore、type-check、SQL/MyBatis注释指令、Javadoc生成标签或编译工具可消费注释。

conservative只允许有证据的LOW普通过时注释删除；对外接口文档、历史迁移说明和其他文件本轮不删除/移动。不能把业务代码改成注释规避模式边界。

输出 `comments.csv`，逐项记录原位置、过时证据、分类、建议及实际动作，候选diff与源码改动遵守同一批次验证/回滚。详细报告仅保存在本地.local/code-cleanup/<run-id>/，不提交开发日志。
