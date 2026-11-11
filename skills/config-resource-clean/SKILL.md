---
name: config-resource-clean
description: 盘点LS·Aegis 雷铄 OA 安全应用构建平台环境配置、部署资源、图片及脚本的真实消费者并分类；conservative只输出复核或归档建议，不移动或删除文件。
---

# 配置与资源盘点

读根 `AGENTS.md` 和 `agents/code-cleanup/AGENTS.md`。输入跟踪资源清单、配置入口、Docker/Compose/部署脚本与运行号；不遍历真实数据卷或读取`.env`、私钥/证书内容。

核对Java配置绑定/profile/Liquibase、Vite alias/public URL/动态资源、CSS引用、Nginx、Docker COPY/挂载/healthcheck、开发与生产Compose及打包/升级脚本。环境切换未激活的配置也可能是正式入口。

分别登记历史备份、测试专用、已下线功能和仍启用资源；仅凭文件名旧版本、零字面引用或当前容器未挂载不作可删证明。国密模板、证书目录、生产开关、GB28181/外部通信资源一律PROHIBITED_AUTO；已执行SQL迁移禁止修改。

输出 `config-resource.csv`：共享字段加消费者/环境、归档影响与建议。通常建议归档而非直接删除，但**本次连归档移动也不执行**；不生成会改变加载路径或部署契约的可直接执行删除命令。不把无法访问的环境写为无消费者。
