# LS·Aegis 雷铄 OA 安全应用构建平台 · PC 管理端

Vue3 + Arco Design 前端工程，与后端 `ls-aegis-server` 同级，不放在后端源码内。

## 常用命令

```bash
pnpm bootstrap
pnpm dev
```

开发默认对接 Docker 后端：见 `.env.development`（`VITE_API_BASE_URL`）。

## Mock

本地 mock **默认关闭**。需要时在 `.env.development` 设：

```bash
VITE_LOCAL_MOCK=true
```

生产包 mock 由 `VITE_BUILD_MOCK` 控制（默认 `false`）。

## 中国地图数据

- ECharts 几何：`src/assets/map/china.json`（仅仪表盘地理页加载）
- 后端省份对照：`ls-aegis-rbac/.../china-provinces.json`（仪表盘访问聚合，非地图几何）

## 按钮样式字典

管理后台按钮按「七类角色 + 修饰」选型，权威对照页：

- 静态页：[`public/design/button-dictionary.html`](./public/design/button-dictionary.html)（开发态 `/design/button-dictionary.html`）
- 常量：`src/constant/button-role.ts`
- 说明：[`public/design/README.md`](./public/design/README.md)
