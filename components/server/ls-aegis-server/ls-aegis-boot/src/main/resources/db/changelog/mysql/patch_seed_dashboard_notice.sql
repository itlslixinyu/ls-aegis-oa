-- liquibase formatted sql

-- changeset ls:seed_dashboard_notice_20
-- comment 仪表盘公告种子：写入 20 条已发布公告（类型随机分布于 notice_type）
SET NAMES utf8mb4;

DELETE FROM `sys_notice` WHERE `id` BETWEEN 910000000000000001 AND 910000000000000020;

INSERT INTO `sys_notice`
(`id`, `title`, `content`, `type`, `notice_scope`, `notice_users`, `notice_methods`, `is_timing`, `publish_time`, `is_top`, `status`, `create_user`, `create_time`, `deleted`)
VALUES
(910000000000000001, '系统维护窗口通知', '<p>本周六 22:00-24:00 进行系统例行维护，期间登录可能短暂中断，请提前保存工作内容。</p>', '7', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 1 DAY), b'1', 3, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 0),
(910000000000000002, '密码策略已更新', '<p>新密码需满足长度与复杂度要求，下次登录时请按提示完成修改。</p>', '6', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 2 DAY), b'1', 3, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), 0),
(910000000000000003, '国密传输链路启用说明', '<p>平台默认启用 SM2/SM3/SM4 国密能力，浏览器需支持现代加密套件。</p>', '6', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 3 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 0),
(910000000000000004, '工作总览公告滚动上线', '<p>工作总览与运营数据中枢已支持公告无缝滚动展示，欢迎体验并提出反馈。</p>', '3', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 4 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), 0),
(910000000000000005, '本周五下午例行巡检', '<p>运维将于周五 14:00-16:00 巡检核心服务，如遇异常请联系值班人员。</p>', '7', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 5 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 0),
(910000000000000006, '关于国庆假期值班安排', '<p>假期值班表已发布，请各部门负责人及时查阅并转发至组内成员。</p>', '1', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 6 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 0),
(910000000000000007, '文件存储容量扩容完成', '<p>对象存储与本地存储容量已扩容，大文件上传超时问题已优化。</p>', '7', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 7 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), 0),
(910000000000000008, '角色权限模型调整公告', '<p>部分菜单权限码已整理，如发现菜单缺失请联系管理员重新授权。</p>', '1', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 8 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), 0),
(910000000000000009, '登录双因素验证试行', '<p>高权限账号将逐步启用二次验证，请提前绑定可用邮箱。</p>', '6', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 9 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), 0),
(910000000000000010, '运营数据中枢视觉升级', '<p>大屏动效与面板样式已对齐运维大屏规范，刷新页面即可体验。</p>', '3', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 10 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), 0),
(910000000000000011, '接口限流策略说明', '<p>公开接口增加频率限制，异常调用将被短暂熔断，请勿短时间高频请求。</p>', '7', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 11 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 11 DAY), 0),
(910000000000000012, '新员工入职资料清单', '<p>请人事与部门管理员按清单完成账号开通、角色分配与培训确认。</p>', '4', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 12 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), 0),
(910000000000000013, '数据库备份演练通知', '<p>本月备份恢复演练定于周日凌晨执行，演练期间只读查询不受影响。</p>', '7', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 13 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 13 DAY), 0),
(910000000000000014, '浏览器兼容性建议', '<p>推荐使用 Chrome / Edge / 国产信创浏览器最新版本访问管理后台。</p>', '1', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 14 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 14 DAY), 0),
(910000000000000015, '消息中心未读提醒优化', '<p>未读公告与站内信角标刷新频率已优化，减少无效轮询。</p>', '3', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 15 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), 0),
(910000000000000016, '季度安全意识培训', '<p>请全体员工于本月底前完成在线安全培训并提交测验结果。</p>', '6', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 16 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 16 DAY), 0),
(910000000000000017, '定时任务监控告警上线', '<p>调度中心失败任务将同步推送到消息中心，请相关负责人关注。</p>', '7', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 17 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 17 DAY), 0),
(910000000000000018, '客户满意度调研邀请', '<p>本季度调研已开启，欢迎提交使用体验与改进建议。</p>', '8', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 18 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 18 DAY), 0),
(910000000000000019, '日志留存周期调整', '<p>操作日志默认留存周期调整为 180 天，超期数据将按策略归档。</p>', '2', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 19 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 19 DAY), 0),
(910000000000000020, '信创环境适配进展同步', '<p>已完成主流国产 OS 与浏览器兼容验证，问题反馈请提交工单。</p>', '3', 1, NULL, '[1]', b'0', DATE_SUB(NOW(), INTERVAL 20 DAY), b'0', 3, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), 0);
