# 平台架构与工程边界

这是一个monorepo和一套统一平台：PC、客户H5、工程师H5、Android入口共享既有后端身份与业务能力，不独立建立登录或数据库栈。

后端使用JDK21/Spring Boot3，父POM编排16个子模块，由 ls-aegis-boot 启动。分层依赖大体为：boot→RBAC/业务域/starter；starter→common/mybatis/redis/security/crypto。PSI、EOMS、报价和签章是业务域，共用文件、消息等平台能力；具体依赖以各pom为准。

PC为Vue3/Vite/Arco，复用 GiPageLayout/BaseTable/useTable/GiForm；H5为Vue/Vite/Vant，按 customer/engineer 模式分包。不要将PC组件规范硬套H5。业务API分别从 apis/psi、eoms、sign、quote 导入，顶层apis保留平台接口。

后端负责身份、RBAC和数据范围；统一响应R保留成功/错误语义。客户会话错误10007与平台401/403是不同契约，不因整齐而重写。Spring/Jackson/MyBatis/注解/反射相关引用不能仅凭文本未引用判无用。

SM2/SM3/SM4实现与协议保留，真实密钥持久化在运行数据目录。已执行数据库changeset内容和ID不可改写；全新安装SQL与增量升级各司其职。Java包名、Maven artifact和基座标识不随产品展示名做全局替换。

事务、幂等、金额、库存与财务约束见[业务边界](../business/overview.md)。工程源码目录保持原名，部署使用[一套基础Compose及环境覆盖](../operations/deployment.md)。
