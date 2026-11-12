# 图片上传工作流与验收

## 顺序与退出条件

1. 基线：记录HEAD、未提交差异、现有图片入口、API及文件存储规则。保护数据库、附件、密钥及设备会话。
2. 盘点：客户报修、工程师维修中/完成、头像、品牌图片、商品和签章相关图片等逐项确认实际消费者。通用附件中的PDF/Office不套用图片格式限制。
   来源、权限和取消恢复另按[专项工作流](source-permissions.md)逐项执行；相册回传不能代替拍照回传。
3. 契约：为每个入口登记角色、类型/大小/数量、必填性、位置可选性、水印来源、文件ID和业务引用、预览权限、替换/删除及孤立附件处理。
4. 状态：选择→校验→等待/获取可选元数据→上传进度→成功预览或失败。失败可重试/删除；有等待、上传或失败项时不提交。业务提交成功前不丢弃已成功照片。
5. 生命周期：并发有界；重复点击不重复上传；删除/离页后的晚响应不污染列表、不泄漏ObjectURL。取消和断网不能伪装成功；只提交服务端有效文件ID。
6. 文件中心：校验内容、大小与解码限制，验证角色/客户/租户及工单状态，上传归属关联失败不得返回成功；预览/下载复用鉴权。页面取消不能证明服务端没有落盘，未关联文件保留在平台文件中心供授权管理员核对；没有精确ID与引用证明不得自动物理删除。已经关联的文件不能因页面移除而被物理删除。
7. 回归：先单元/组件和原生选图，再专用身份的真实上传、预览、保存报修、派单、维修前后照片、保存记录及各端回显。使用本轮合成对象登记精确ID，按应用业务规则补偿清理。
8. 交付：核对本机运行资源与候选构建一致。原生改动交付同签名APK，真机按用户已有安装偏好处理。文档只写现行契约，执行报告留本地。

## 工程验收矩阵

参考[GB/T 25000.10-2016官方信息](https://openstd.samr.gov.cn/bzgk/std/newGbInfo?hcno=DB14B9415F3D51A9EF321EA62EDDE9A6)的质量模型组织功能适合性、可靠性、易用性、性能及安全性检查。以下是本项目工程用例，不是标准全文复述或国家认证。

| 维度 | 必须验证 |
|---|---|
| 正常 | JPG/PNG、横竖图、小图/手机大图、单选/多选、三种移动上传入口及PC实际入口 |
| 边界 | 空/不可读文件、格式不符、入口限制大小/张数、ID字符串、定位拒绝/超时 |
| 状态 | 失败重试、重复点击、删除后晚响应、离页、成功后提交失败仍保留照片、失败项不得静默跳过 |
| 联动 | 上传归属、报修/维修记录关联、水印、鉴权预览、业务保存后重新进入回显 |
| 性能 | 有界上传并发、真实进度与处理中状态、无跨页残留预览资源，不进行无界生产压测 |
| 清理 | 仅登记的测试身份/文件/业务对象，无真实附件误删，无测试凭据进入Git |

每条结果记录环境、角色、用例、预期、实际、证据与清理，使用PASS/FAIL/BLOCKED/NOT_RUN。静态核对、mock、真实API和真机结果分别标记；未完成项保留在本地状态清单，不能以总体PASS掩盖。

## 可复用入口

- `node --test scripts/quality/photo_upload_session.test.mjs scripts/quality/h5_upload_image.test.cjs scripts/quality/h5_auth_image.test.cjs scripts/quality/pc_upload_lifecycle.test.cjs scripts/quality/pc_image_entries.test.cjs`：格式、队列、重试、取消与PC入口生命周期。
- `node scripts/quality/photo_upload_browser.mjs`：真实Vue/Vant组件，模拟上传、定位拒绝与业务接口；需要本机Chrome，可用`CHROME_PATH`指定。只监听回环地址，产物留在`.local/image-upload/browser-components/`，自动移除临时入口，不替代真实API验证。
- `node scripts/quality/pc_image_browser.mjs`：真实PC图片及附件页面组件，模拟身份及接口，自带合成PNG，阻止夹具访问真实`/api`；结果留`.local/image-upload/pc-components/`，不代表真实账号权限验收。
- `node --test scripts/quality/android_image_policy.test.mjs`：JDK下验证来源边界、剩余名额、图片头与双端受限FileProvider配置。原生交互仍须在设备执行。
- `photo_upload_browser.mjs --native-fixture`可短时提供回环端口5188的真实移动组件夹具，供测试设备反向桥接；接口及定位均为模拟。结束时发送SIGINT/SIGTERM移除临时入口并恢复原设备桥接，禁止用其结果声称真实后端闭环已通过。
- `python -X utf8 scripts/quality/run_business_flows.py`：独立MySQL/Redis/后端闭环，包含客户照片、派单、工程师两组照片、水印预览与维修记录关联；脚本只操作自己标记的隔离环境，禁止改为在用数据库。
