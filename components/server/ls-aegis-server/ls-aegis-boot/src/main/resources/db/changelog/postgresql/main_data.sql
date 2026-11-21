-- liquibase formatted sql

-- changeset charles7c:1
-- validCheckSum: ANY
-- comment 初始化表数据
-- 初始化默认菜单
INSERT INTO "sys_menu"
("id", "title", "parent_id", "type", "path", "name", "component", "redirect", "icon", "is_external", "is_cache", "is_hidden", "permission", "sort", "status", "create_user", "create_time")
VALUES
(1000, '系统管理', 0, 1, '/system', 'System', 'Layout', '/system/user', 'settings', false, false, false, NULL, 1, 1, 1, NOW()),
(1010, '用户管理', 1000, 2, '/system/user', 'SystemUser', 'system/user/index', NULL, 'user', false, false, false, NULL, 1, 1, 1, NOW()),
(1011, '列表', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:list', 1, 1, 1, NOW()),
(1012, '详情', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:get', 2, 1, 1, NOW()),
(1013, '新增', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:create', 3, 1, 1, NOW()),
(1014, '修改', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:update', 4, 1, 1, NOW()),
(1015, '删除', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:delete', 5, 1, 1, NOW()),
(1016, '导出', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:export', 6, 1, 1, NOW()),
(1017, '导入', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:import', 7, 1, 1, NOW()),
(1018, '重置密码', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:resetPwd', 8, 1, 1, NOW()),
(1019, '分配角色', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:updateRole', 9, 1, 1, NOW()),

(1030, '角色管理', 1000, 2, '/system/role', 'SystemRole', 'system/role/index', NULL, 'user-management', false, false, false, NULL, 2, 1, 1, NOW()),
(1031, '列表', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:list', 1, 1, 1, NOW()),
(1032, '详情', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:get', 2, 1, 1, NOW()),
(1033, '新增', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:create', 3, 1, 1, NOW()),
(1034, '修改', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:update', 4, 1, 1, NOW()),
(1035, '删除', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:delete', 5, 1, 1, NOW()),
(1036, '修改权限', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:updatePermission', 6, 1, 1, NOW()),
(1037, '分配', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:assign', 7, 1, 1, NOW()),
(1038, '取消分配', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:unassign', 8, 1, 1, NOW()),

(1050, '菜单管理', 1000, 2, '/system/menu', 'SystemMenu', 'system/menu/index', NULL, 'menu', false, false, false, NULL, 3, 1, 1, NOW()),
(1051, '列表', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:list', 1, 1, 1, NOW()),
(1052, '详情', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:get', 2, 1, 1, NOW()),
(1053, '新增', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:create', 3, 1, 1, NOW()),
(1054, '修改', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:update', 4, 1, 1, NOW()),
(1055, '删除', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:delete', 5, 1, 1, NOW()),
(1056, '清除缓存', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:clearCache', 6, 1, 1, NOW()),

(1070, '部门管理', 1000, 2, '/system/dept', 'SystemDept', 'system/dept/index', NULL, 'mind-mapping', false, false, false, NULL, 4, 1, 1, NOW()),
(1071, '列表', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:list', 1, 1, 1, NOW()),
(1072, '详情', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:get', 2, 1, 1, NOW()),
(1073, '新增', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:create', 3, 1, 1, NOW()),
(1074, '修改', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:update', 4, 1, 1, NOW()),
(1075, '删除', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:delete', 5, 1, 1, NOW()),
(1076, '导出', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:export', 6, 1, 1, NOW()),

(1090, '通知公告', 1000, 2, '/system/notice', 'SystemNotice', 'system/notice/index', NULL, 'notification', false, false, false, NULL, 5, 1, 1, NOW()),
(1091, '列表', 1090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:notice:list', 1, 1, 1, NOW()),
(1092, '详情', 1090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:notice:get', 2, 1, 1, NOW()),
(1093, '查看公告', 1090, 2, '/system/notice/view', 'SystemNoticeView', 'system/notice/view/index', NULL, NULL, false, false, true, 'system:notice:view', 3, 1, 1, NOW()),
(1094, '发布公告', 1090, 2, '/system/notice/add', 'SystemNoticeAdd', 'system/notice/add/index', NULL, NULL, false, false, true, 'system:notice:create', 4, 1, 1, NOW()),
(1095, '修改', 1090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:notice:update', 5, 1, 1, NOW()),
(1096, '删除', 1090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:notice:delete', 6, 1, 1, NOW()),

(1110, '文件管理', 1000, 2, '/system/file', 'SystemFile', 'system/file/index', NULL, 'file', false, false, false, NULL, 6, 1, 1, NOW()),
(1111, '列表', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:list', 1, 1, 1, NOW()),
(1112, '详情', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:get', 2, 1, 1, NOW()),
(1113, '上传', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:upload', 3, 1, 1, NOW()),
(1114, '修改', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:update', 4, 1, 1, NOW()),
(1115, '删除', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:delete', 5, 1, 1, NOW()),
(1116, '下载', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:download', 6, 1, 1, NOW()),
(1117, '创建文件夹', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:createDir', 7, 1, 1, NOW()),
(1118, '计算文件夹大小', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:calcDirSize', 8, 1, 1, NOW()),
(1119, '回收站文件列表', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:fileRecycle:list', 9, 1, 1, NOW()),
(1120, '还原回收站文件', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:fileRecycle:restore', 10, 1, 1, NOW()),
(1121, '删除回收站文件', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:fileRecycle:delete', 11, 1, 1, NOW()),
(1122, '清空回收站', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:fileRecycle:clean', 12, 1, 1, NOW()),
(1123, '校验文件', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:check', 13, 1, 1, NOW()),

(1130, '字典管理', 1000, 2, '/system/dict', 'SystemDict', 'system/dict/index', NULL, 'bookmark', false, false, false, NULL, 7, 1, 1, NOW()),
(1131, '列表', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:list', 1, 1, 1, NOW()),
(1132, '详情', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:get', 2, 1, 1, NOW()),
(1133, '新增', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:create', 3, 1, 1, NOW()),
(1134, '修改', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:update', 4, 1, 1, NOW()),
(1135, '删除', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:delete', 5, 1, 1, NOW()),
(1136, '清除缓存', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:clearCache', 6, 1, 1, NOW()),
(1140, '字典项管理', 1000, 2, '/system/dict/item', 'SystemDictItem', 'system/dict/index', NULL, 'bookmark', false, false, true, NULL, 8, 1, 1, NOW()),
(1141, '列表', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:list', 1, 1, 1, NOW()),
(1142, '详情', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:get', 2, 1, 1, NOW()),
(1143, '新增', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:create', 3, 1, 1, NOW()),
(1144, '修改', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:update', 4, 1, 1, NOW()),
(1145, '删除', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:delete', 5, 1, 1, NOW()),

(1150, '系统配置', 1000, 2, '/system/config', 'SystemConfig', 'system/config/index', NULL, 'config', false, false, false, NULL, 999, 1, 1, NOW()),
(1160, '网站配置', 1150, 2, '/system/config?tab=site', 'SystemSiteConfig', 'system/config/site/index', NULL, 'apps', false, false, true, NULL, 1, 1, 1, NOW()),
(1161, '查询', 1160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:siteConfig:get', 1, 1, 1, NOW()),
(1162, '修改', 1160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:siteConfig:update', 2, 1, 1, NOW()),
(1170, '安全配置', 1150, 2, '/system/config?tab=security', 'SystemSecurityConfig', 'system/config/security/index', NULL, 'safe', false, false, true, NULL, 2, 1, 1, NOW()),
(1171, '查询', 1170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:securityConfig:get', 1, 1, 1, NOW()),
(1172, '修改', 1170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:securityConfig:update', 2, 1, 1, NOW()),
(1180, '登录配置', 1150, 2, '/system/config?tab=login', 'SystemLoginConfig', 'system/config/login/index', NULL, 'lock', false, false, true, NULL, 3, 1, 1, NOW()),
(1181, '查询', 1180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:loginConfig:get', 1, 1, 1, NOW()),
(1182, '修改', 1180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:loginConfig:update', 2, 1, 1, NOW()),
(1190, '邮件配置', 1150, 2, '/system/config?tab=mail', 'SystemMailConfig', 'system/config/mail/index', NULL, 'email', false, false, true, NULL, 4, 1, 1, NOW()),
(1191, '查询', 1190, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:mailConfig:get', 1, 1, 1, NOW()),
(1192, '修改', 1190, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:mailConfig:update', 2, 1, 1, NOW()),
(1200, '天气配置', 1150, 2, '/system/config?tab=weather', 'SystemWeatherConfig', 'system/config/weather/index', NULL, 'cloud', false, false, true, NULL, 5, 1, 1, NOW()),
(1201, '查询', 1200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:weatherConfig:get', 1, 1, 1, NOW()),
(1202, '修改', 1200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:weatherConfig:update', 2, 1, 1, NOW()),
(1210, '大屏配置', 1150, 2, '/system/config?tab=dashboard', 'SystemDashboardConfig', 'system/config/dashboard/index', NULL, 'dashboard', false, false, true, NULL, 6, 1, 1, NOW()),
(1211, '查询', 1210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dashboardConfig:get', 1, 1, 1, NOW()),
(1212, '修改', 1210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dashboardConfig:update', 2, 1, 1, NOW()),
(1220, '功能模块', 1150, 2, '/system/config?tab=module', 'SystemModuleConfig', 'system/config/module/index', NULL, 'apps', false, false, true, NULL, 7, 1, 1, NOW()),
(1221, '查询', 1220, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:moduleConfig:get', 1, 1, 1, NOW()),
(1222, '修改', 1220, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:moduleConfig:update', 2, 1, 1, NOW()),
(1230, '存储配置', 1150, 2, '/system/config?tab=storage', 'SystemStorage', 'system/config/storage/index', NULL, 'storage', false, false, true, NULL, 8, 1, 1, NOW()),
(1231, '列表', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:list', 1, 1, 1, NOW()),
(1232, '详情', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:get', 2, 1, 1, NOW()),
(1233, '新增', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:create', 3, 1, 1, NOW()),
(1234, '修改', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:update', 4, 1, 1, NOW()),
(1235, '删除', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:delete', 5, 1, 1, NOW()),
(1236, '修改状态', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:updateStatus', 6, 1, 1, NOW()),
(1237, '设为默认存储', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:setDefault', 7, 1, 1, NOW()),
(1250, '客户端配置', 1150, 2, '/system/config?tab=client', 'SystemClient', 'system/config/client/index', NULL, 'mobile', false, false, true, NULL, 9, 1, 1, NOW()),
(1251, '列表', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:list', 1, 1, 1, NOW()),
(1252, '详情', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:get', 2, 1, 1, NOW()),
(1253, '新增', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:create', 3, 1, 1, NOW()),
(1254, '修改', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:update', 4, 1, 1, NOW()),
(1255, '删除', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:delete', 5, 1, 1, NOW()),

(2000, '系统监控', 0, 1, '/monitor', 'Monitor', 'Layout', '/monitor/online', 'computer', false, false, false, NULL, 2, 1, 1, NOW()),
(2010, '在线用户', 2000, 2, '/monitor/online', 'MonitorOnline', 'monitor/online/index', NULL, 'user', false, false, false, NULL, 1, 1, 1, NOW()),
(2011, '列表', 2010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:online:list', 1, 1, 1, NOW()),
(2012, '强退', 2010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:online:kickout', 2, 1, 1, NOW()),

(2030, '系统日志', 2000, 2, '/monitor/log', 'MonitorLog', 'monitor/log/index', NULL, 'history', false, false, false, NULL, 2, 1, 1, NOW()),
(2031, '列表', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:log:list', 1, 1, 1, NOW()),
(2032, '详情', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:log:get', 2, 1, 1, NOW()),
(2033, '导出', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:log:export', 3, 1, 1, NOW()),
(2034, '签章审计', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sign:audit:list', 4, 1, 1, NOW());

-- 初始化默认部门（一级总公司 / 二级总经办）
INSERT INTO "sys_dept"
("id", "name", "parent_id", "ancestors", "description", "sort", "status", "is_system", "create_user", "create_time")
VALUES
(1, '总公司', 0, '0', '一级部门', 1, 1, true, 1, NOW()),
(2, '总经办', 1, '0,1', '二级部门', 1, 1, false, 1, NOW());

-- 初始化默认角色
INSERT INTO "sys_role"
("id", "name", "code", "data_scope", "description", "sort", "is_system", "create_user", "create_time")
VALUES
(1, '超级管理员', 'super_admin', 1, '系统初始角色', 0, true, 1, NOW()),
(2, '系统管理员', 'sys_admin', 1, NULL, 1, false, 1, NOW()),
(3, '普通角色', 'general', 4, NULL, 2, false, 1, NOW()),
(11, 'EOMS 主管', 'eoms_manager', 2, '运维报修主管：本部门及下级数据', 11, false, 1, NOW()),
(12, 'EOMS 工程师', 'eoms_engineer', 4, '运维工程师：仅本人创建或派给自己的工单', 12, false, 1, NOW()),
(13, 'EOMS 财务', 'eoms_finance', 3, '运维财务：本部门结算/应收/收款', 13, false, 1, NOW());

-- 初始化默认用户（口令哈希 SM3；明文口令禁止写入仓库；pwd_reset_time 置旧以配合密码有效期强制首登改密）
INSERT INTO "sys_user"
("id", "username", "nickname", "password", "gender", "email", "phone", "avatar", "description", "status", "is_system", "pwd_reset_time", "dept_id", "create_user", "create_time")
VALUES
(1, 'admin', '超级管理员', '{sm3}v1$ef4114fbfe4881f4710b626429c97c04$bd43c9a531b4cc2705aaff6d1a2f9563906c254cc3ffe05fbb21c2456bd86d99', 1, NULL, NULL, NULL, '系统初始用户', 1, true, '2000-01-01 00:00:00', 1, 1, NOW()),
(547889293968801822, 'test', '测试用户', '{sm3}v1$cfa6968c5df393377b04ff6e33f6f90c$025649846502c230dbfcff724d5724a2aa0af1c2a97defc84da59c70b9df24c8', 2, NULL, NULL, NULL, '系统测试账号', 1, false, '2000-01-01 00:00:00', 2, 1, NOW());

-- 初始化默认参数
INSERT INTO "sys_option"
("id", "category", "name", "code", "value", "default_value", "description")
VALUES
(1, 'SITE', '系统名称', 'SITE_TITLE', NULL, 'LS-Aegis 雷铄御警安全应用构建平台', '显示在浏览器标题栏和登录页顶栏的产品/项目名称'),
(2, 'SITE', '系统描述', 'SITE_DESCRIPTION', NULL, 'LS Aegis 前后端分离中后台平台', '用于 SEO 的网站元描述'),
(3, 'SITE', '版权声明', 'SITE_COPYRIGHT', NULL, '© 2026 雷铄科技 · LS-Aegis 雷铄御警安全应用构建平台', '显示在页面底部的版权声明文本'),
(4, 'SITE', 'ICP备案号', 'SITE_BEIAN', NULL, '冀ICP备 00000000 号', '工信部 ICP 备案编号（如：冀ICP备00000000号）'),
(5, 'SITE', '系统图标', 'SITE_FAVICON', NULL, '/favicon.ico', '浏览器标签页显示的网站图标（建议 .ico 格式）'),
(6, 'SITE', '系统LOGO', 'SITE_LOGO', NULL, '/logo.svg', '显示在登录页面和系统导航栏的网站图标（建议 .svg 格式）'),
(7, 'SITE', '公司名称', 'SITE_COMPANY', NULL, '雷铄科技', '显示在登录页顶栏的公司名称'),
(8, 'SITE', '公安备案号', 'SITE_BEIAN_GONGAN', NULL, '冀公网安备 00000000000000 号', '显示在登录页底部的公安备案编号'),
(9, 'SITE', '公安备案图标', 'SITE_BEIAN_GONGAN_ICON', NULL, '/beian-gongan.png', '公安备案号左侧图标（建议小尺寸 PNG）'),
(10, 'PASSWORD', '密码错误锁定阈值', 'PASSWORD_ERROR_LOCK_COUNT', NULL, '5', '连续登录失败次数达到该值将锁定账号（0-10次，0表示禁用锁定）'),
(11, 'PASSWORD', '账号锁定时长（分钟）', 'PASSWORD_ERROR_LOCK_MINUTES', NULL, '5', '账号锁定后自动解锁的时间（1-1440分钟，即24小时）'),
(12, 'PASSWORD', '密码有效期（天）', 'PASSWORD_EXPIRATION_DAYS', NULL, '90', '密码强制修改周期（0-999天，0表示永不过期；默认90天，种子账号首登须改密）'),
(13, 'PASSWORD', '密码到期提醒（天）', 'PASSWORD_EXPIRATION_WARNING_DAYS', NULL, '0', '密码过期前的提前提醒天数（0表示不提醒）'),
(14, 'PASSWORD', '历史密码重复校验次数', 'PASSWORD_REPETITION_TIMES', NULL, '3', '禁止使用最近 N 次的历史密码（3-32次）'),
(15, 'PASSWORD', '密码最小长度', 'PASSWORD_MIN_LENGTH', NULL, '8', '密码最小字符长度要求（8-32个字符）'),
(16, 'PASSWORD', '是否允许密码包含用户名', 'PASSWORD_ALLOW_CONTAIN_USERNAME', NULL, '1', '是否允许密码包含正序或倒序的用户名字符'),
(17, 'PASSWORD', '密码是否必须包含特殊字符', 'PASSWORD_REQUIRE_SYMBOLS', NULL, '0', '是否要求密码必须包含特殊字符（如：!@#$%）'),
(18, 'SITE', '系统名称简称', 'SITE_TITLE_SHORT', NULL, '雷铄御警', '显示在左侧栏 Logo 旁的系统简称（必填，最多 8 个字）'),
(19, 'SITE', '公司简称', 'SITE_COMPANY_SHORT', NULL, '雷铄', '公司名称简称（必填，最多 8 个字）'),
(20, 'MAIL', '邮件协议', 'MAIL_PROTOCOL', NULL, 'smtp', '邮件发送协议类型'),
(21, 'MAIL', '服务器地址', 'MAIL_HOST', NULL, NULL, '邮件服务器地址'),
(22, 'MAIL', '服务器端口', 'MAIL_PORT', NULL, '465', '邮件服务器连接端口'),
(23, 'MAIL', '邮箱账号', 'MAIL_USERNAME', NULL, 'support@ls-aegis.local', '发件人邮箱地址'),
(24, 'MAIL', '邮箱密码', 'MAIL_PASSWORD', NULL, NULL, '服务授权密码/客户端专用密码'),
(25, 'MAIL', '启用SSL加密', 'MAIL_SSL_ENABLED', NULL, '1', '是否启用SSL/TLS加密连接'),
(26, 'MAIL', 'SSL端口号', 'MAIL_SSL_PORT', NULL, '465', 'SSL加密连接的备用端口（通常与主端口一致）'),
(27, 'LOGIN', '是否启用验证码', 'LOGIN_CAPTCHA_ENABLED', NULL, '1', NULL),
(28, 'WEATHER', '是否启用', 'WEATHER_ENABLED', NULL, '1', '控制运营中枢顶栏是否展示天气信息。'),
(29, 'WEATHER', '默认城市', 'WEATHER_CITY', NULL, '北京', '固定城市模式下使用；定位未授权或解析失败时作为展示城市。'),
(30, 'WEATHER', '刷新时间间隔（秒）', 'WEATHER_REFRESH_INTERVAL', NULL, '600', '天气信息自动刷新周期，单位秒，建议 60～86400。'),
(31, 'WEATHER', '数据来源', 'WEATHER_PROVIDER', NULL, 'mock', '本地模拟仅用于联调；和风天气支持 API Key 或 JWT，须自行申请凭据并遵守协议与配额，异常时回退本地模拟。'),
(32, 'WEATHER', '城市模式', 'WEATHER_CITY_MODE', NULL, 'auto', '「和风 GeoAPI 定位」在用户授权后使用浏览器位置解析城市；「固定城市」仅使用默认城市。位置信息仅用于天气展示。'),
(33, 'WEATHER', '和风 API Host', 'WEATHER_API_HOST', NULL, '', '填写控制台「设置」中的专属 API 访问地址，请以控制台实际值为准。'),
(34, 'WEATHER', '和风 GeoAPI 地址', 'WEATHER_GEO_HOST', NULL, '', '填写服务商控制台提供的城市查询地址，须与当前账号权限及官方文档一致。'),
(35, 'WEATHER', '和风 API Key', 'WEATHER_API_KEY', NULL, '', '控制台创建「API KEY」类型凭据后的 Key；无 JWT 私钥时填此项即可。仅服务端保存，不下发前端。'),
(36, 'WEATHER', '和风凭据 ID（kid）', 'WEATHER_JWT_KID', NULL, '', '填写和风控制台 JWT 凭据 ID（kid）；仅用 API Key 时可留空。'),
(37, 'WEATHER', '和风项目 ID（sub）', 'WEATHER_JWT_PROJECT_ID', NULL, '', '填写和风控制台项目 ID；仅用 API Key 时可留空。'),
(38, 'WEATHER', '和风 JWT 私钥', 'WEATHER_JWT_PRIVATE_KEY', NULL, '', 'JWT Ed25519 私钥 PEM；仅用 API Key 时可留空。私钥仅服务端保存。'),
(39, 'DASHBOARD', '大屏名称', 'DASHBOARD_TITLE', NULL, 'LS-Aegis 雷铄御警安全应用运营中枢', '运营中枢顶栏主标题'),
(40, 'DASHBOARD', '显示时钟', 'DASHBOARD_SHOW_CLOCK', NULL, '1', '是否在顶栏右侧显示日期时间'),
(41, 'DASHBOARD', '显示运行状态', 'DASHBOARD_SHOW_STATUS', NULL, '1', '是否在工具栏显示系统运行状态指示灯'),
(42, 'DASHBOARD', '显示指标概览', 'DASHBOARD_SHOW_KPI', NULL, '1', '是否显示顶部 KPI 指标卡片'),
(43, 'DASHBOARD', '显示公告', 'DASHBOARD_SHOW_NOTICE', NULL, '1', '是否显示底部最新公告面板'),
(44, 'DASHBOARD', '全屏入口提示', 'DASHBOARD_SHOW_FS_TIP', NULL, '1', '进入大屏时是否短暂提示右下角全屏入口'),
(45, 'DASHBOARD', '显示装饰动效', 'DASHBOARD_SHOW_DECOR', NULL, '1', '是否显示边框装饰与扫描线动效'),
(46, 'DASHBOARD', '默认统计周期', 'DASHBOARD_DEFAULT_DAYS', NULL, '7', '进入大屏时默认统计周期：7 或 30（天）'),
(47, 'DASHBOARD', '数据刷新间隔（秒）', 'DASHBOARD_REFRESH_INTERVAL', NULL, '60', '仪表盘数据自动刷新间隔，单位秒；0 表示不自动刷新'),
(48, 'MODULE', '租户管理', 'MODULE_TENANT_ENABLED', NULL, '0', '默认关闭。开启后显示租户管理菜单与登录租户编码框；关闭则隐藏菜单、拦截相关接口。完整启用还须 CONTINEW_STARTER_TENANT_ENABLED=true。'),
(49, 'MODULE', '应用管理', 'MODULE_OPEN_ENABLED', NULL, '0', '默认关闭。开启后显示能力开放/应用管理菜单；关闭则隐藏菜单并拦截相关接口。'),
(50, 'MODULE', '任务调度', 'MODULE_SCHEDULE_ENABLED', NULL, '0', '默认关闭。开启后显示任务调度侧栏菜单；关闭则隐藏菜单并拦截相关接口。实际执行任务仍须 snail-job.enabled=true 并启动调度中心。');

-- 初始化默认字典
INSERT INTO "sys_dict"
("id", "name", "code", "description", "is_system", "create_user", "create_time")
VALUES
(1, '公告分类', 'notice_type', NULL, true, 1, NOW()),
(2, '客户端类型', 'client_type', NULL, true, 1, NOW());

INSERT INTO "sys_dict_item"
("id", "label", "value", "color", "sort", "description", "status", "dict_id", "create_user", "create_time")
VALUES
(1, '通知公告', '1', 'primary', 1, '日常行政通知、会议通知、放假调休、制度宣贯', 1, 1, 1, NOW()),
(2, '政策文件', '2', 'primary', 2, '新规发布、政策解读、上级下发文件', 1, 1, 1, NOW()),
(6, '新闻动态', '3', 'default', 3, '公司动态、项目进展、行业资讯', 1, 1, 1, NOW()),
(7, '人事公告', '4', 'success', 4, '任免、招聘、入职离职、考勤、绩效考核公示', 1, 1, 1, NOW()),
(8, '财务公示', '5', 'warning', 5, '预算、采购招标、中标结果、费用公示', 1, 1, 1, NOW()),
(9, '安全通告', '6', 'error', 6, '网络安全预警、系统维护、漏洞提示、版本升级', 1, 1, 1, NOW()),
(10, '运维公告', '7', 'warning', 7, '服务器停机、系统更新、功能变更、接口下线', 1, 1, 1, NOW()),
(11, '公示通告', '8', 'default', 8, '结果公示、评选、申诉、公开征求意见', 1, 1, 1, NOW()),
(12, '紧急通告', '9', 'error', 9, '突发事件、应急通知、重要紧急事项', 1, 1, 1, NOW()),
(3, '桌面端', 'PC', 'primary', 1, NULL, 1, 2, 1, NOW()),
(4, '安卓', 'ANDROID', 'success', 2, NULL, 1, 2, 1, NOW()),
(5, '小程序', 'XCX', 'warning', 3, NULL, 1, 2, 1, NOW());

-- 初始化默认用户和角色关联数据
INSERT INTO "sys_user_role"
("id", "user_id", "role_id")
VALUES
(1, 1, 1),
(2, 547889293968801822, 3);

-- 初始化默认存储
INSERT INTO "sys_storage"
("id", "name", "code", "type", "access_key", "secret_key", "endpoint", "bucket_name", "domain", "recycle_bin_enabled", "recycle_bin_path", "description", "is_default", "sort", "status", "create_user", "create_time")
VALUES
(1, '开发环境', 'local_dev', 1, NULL, NULL, NULL, 'data/file/', 'http://localhost:8080/api/file/', true, '.RECYCLE.BIN/', '本地存储', true, 1, 1, 1, NOW()),
(2, '生产环境', 'local_prod', 1, NULL, NULL, NULL, 'data/file/', 'http://localhost:8080/api/file/', true, '.RECYCLE.BIN/', '本地存储', false, 2, 2, 1, NOW());

-- 初始化客户端数据
INSERT INTO "sys_client"
("id", "client_id", "client_type", "auth_type", "active_timeout", "timeout", "status", "create_user", "create_time")
VALUES
(1, 'ef51c9a3e9046c4f2ea45142c8a8344a', 'PC', '["ACCOUNT", "EMAIL"]', 1800, 86400, 1, 1, NOW());

-- 初始化仪表盘公告种子（20 条已发布）
INSERT INTO "sys_notice"
("id", "title", "content", "type", "notice_scope", "notice_users", "notice_methods", "is_timing", "publish_time", "is_top", "status", "create_user", "create_time", "deleted")
VALUES
(910000000000000001, '系统维护窗口通知', '<p>本周六 22:00-24:00 进行系统例行维护，期间登录可能短暂中断，请提前保存工作内容。</p>', '7', 1, NULL, '[1]', false, NOW() - INTERVAL '1 day', true, 3, 1, NOW() - INTERVAL '1 day', 0),
(910000000000000002, '密码策略已更新', '<p>新密码需满足长度与复杂度要求，下次登录时请按提示完成修改。</p>', '6', 1, NULL, '[1]', false, NOW() - INTERVAL '2 day', true, 3, 1, NOW() - INTERVAL '2 day', 0),
(910000000000000003, '国密传输链路启用说明', '<p>平台默认启用 SM2/SM3/SM4 国密能力，浏览器需支持现代加密套件。</p>', '6', 1, NULL, '[1]', false, NOW() - INTERVAL '3 day', false, 3, 1, NOW() - INTERVAL '3 day', 0),
(910000000000000004, '工作总览公告滚动上线', '<p>工作总览与运营数据中枢已支持公告无缝滚动展示，欢迎体验并提出反馈。</p>', '3', 1, NULL, '[1]', false, NOW() - INTERVAL '4 day', false, 3, 1, NOW() - INTERVAL '4 day', 0),
(910000000000000005, '本周五下午例行巡检', '<p>运维将于周五 14:00-16:00 巡检核心服务，如遇异常请联系值班人员。</p>', '7', 1, NULL, '[1]', false, NOW() - INTERVAL '5 day', false, 3, 1, NOW() - INTERVAL '5 day', 0),
(910000000000000006, '关于国庆假期值班安排', '<p>假期值班表已发布，请各部门负责人及时查阅并转发至组内成员。</p>', '1', 1, NULL, '[1]', false, NOW() - INTERVAL '6 day', false, 3, 1, NOW() - INTERVAL '6 day', 0),
(910000000000000007, '文件存储容量扩容完成', '<p>对象存储与本地存储容量已扩容，大文件上传超时问题已优化。</p>', '7', 1, NULL, '[1]', false, NOW() - INTERVAL '7 day', false, 3, 1, NOW() - INTERVAL '7 day', 0),
(910000000000000008, '角色权限模型调整公告', '<p>部分菜单权限码已整理，如发现菜单缺失请联系管理员重新授权。</p>', '1', 1, NULL, '[1]', false, NOW() - INTERVAL '8 day', false, 3, 1, NOW() - INTERVAL '8 day', 0),
(910000000000000009, '登录双因素验证试行', '<p>高权限账号将逐步启用二次验证，请提前绑定可用邮箱。</p>', '6', 1, NULL, '[1]', false, NOW() - INTERVAL '9 day', false, 3, 1, NOW() - INTERVAL '9 day', 0),
(910000000000000010, '运营数据中枢视觉升级', '<p>大屏动效与面板样式已对齐运维大屏规范，刷新页面即可体验。</p>', '3', 1, NULL, '[1]', false, NOW() - INTERVAL '10 day', false, 3, 1, NOW() - INTERVAL '10 day', 0),
(910000000000000011, '接口限流策略说明', '<p>公开接口增加频率限制，异常调用将被短暂熔断，请勿短时间高频请求。</p>', '7', 1, NULL, '[1]', false, NOW() - INTERVAL '11 day', false, 3, 1, NOW() - INTERVAL '11 day', 0),
(910000000000000012, '新员工入职资料清单', '<p>请人事与部门管理员按清单完成账号开通、角色分配与培训确认。</p>', '4', 1, NULL, '[1]', false, NOW() - INTERVAL '12 day', false, 3, 1, NOW() - INTERVAL '12 day', 0),
(910000000000000013, '数据库备份演练通知', '<p>本月备份恢复演练定于周日凌晨执行，演练期间只读查询不受影响。</p>', '7', 1, NULL, '[1]', false, NOW() - INTERVAL '13 day', false, 3, 1, NOW() - INTERVAL '13 day', 0),
(910000000000000014, '浏览器兼容性建议', '<p>推荐使用 Chrome / Edge / 国产信创浏览器最新版本访问管理后台。</p>', '1', 1, NULL, '[1]', false, NOW() - INTERVAL '14 day', false, 3, 1, NOW() - INTERVAL '14 day', 0),
(910000000000000015, '消息中心未读提醒优化', '<p>未读公告与站内信角标刷新频率已优化，减少无效轮询。</p>', '3', 1, NULL, '[1]', false, NOW() - INTERVAL '15 day', false, 3, 1, NOW() - INTERVAL '15 day', 0),
(910000000000000016, '季度安全意识培训', '<p>请全体员工于本月底前完成在线安全培训并提交测验结果。</p>', '6', 1, NULL, '[1]', false, NOW() - INTERVAL '16 day', false, 3, 1, NOW() - INTERVAL '16 day', 0),
(910000000000000017, '定时任务监控告警上线', '<p>调度中心失败任务将同步推送到消息中心，请相关负责人关注。</p>', '7', 1, NULL, '[1]', false, NOW() - INTERVAL '17 day', false, 3, 1, NOW() - INTERVAL '17 day', 0),
(910000000000000018, '客户满意度调研邀请', '<p>本季度调研已开启，欢迎提交使用体验与改进建议。</p>', '8', 1, NULL, '[1]', false, NOW() - INTERVAL '18 day', false, 3, 1, NOW() - INTERVAL '18 day', 0),
(910000000000000019, '日志留存周期调整', '<p>操作日志默认留存周期调整为 180 天，超期数据将按策略归档。</p>', '2', 1, NULL, '[1]', false, NOW() - INTERVAL '19 day', false, 3, 1, NOW() - INTERVAL '19 day', 0),
(910000000000000020, '信创环境适配进展同步', '<p>已完成主流国产 OS 与浏览器兼容验证，问题反馈请提交工单。</p>', '3', 1, NULL, '[1]', false, NOW() - INTERVAL '20 day', false, 3, 1, NOW() - INTERVAL '20 day', 0);
