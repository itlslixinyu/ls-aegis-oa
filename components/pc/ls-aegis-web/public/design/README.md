# 设计字典（Design Dictionary）

本目录存放前端可直接打开的设计对照页（静态 HTML，经 Vite `public` 原样发布）。

## 按钮样式字典

| 项 | 路径 |
|---|---|
| 文件 | `button-dictionary.html` |
| 开发态 | 启动 `pnpm dev` 后访问 `/design/button-dictionary.html` |
| 生产态 | 站点根路径 `/design/button-dictionary.html` |
| 代码常量 | `src/constant/button-role.ts` |
| Agent 规则 | `.cursor/rules/按钮样式规范.mdc` |

用途：按「七类业务角色 + 修饰 + 规范外」选型按钮皮肤与文案，避免按 Arco type×status 穷举。

运行时全局皮肤（业务页勿再 import）：

- `src/styles/arco-ui/a-button.less`
- `src/styles/arco-ui/a-link.less`

本地也可直接用浏览器打开本文件，无需启动前端。
