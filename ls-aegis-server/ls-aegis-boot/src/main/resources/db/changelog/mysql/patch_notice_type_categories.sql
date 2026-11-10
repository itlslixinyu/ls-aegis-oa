-- liquibase formatted sql

-- changeset ls:notice_type_categories_v1
-- comment 公告分类默认项：通知公告/政策文件/新闻动态/人事/财务/安全/运维/公示/紧急（颜色按紧急程度分级）
SET NAMES utf8mb4;

-- 更新原有两项（产品新闻→通知公告，企业动态→政策文件）
UPDATE `sys_dict_item`
SET `label` = '通知公告',
    `color` = 'primary',
    `sort` = 1,
    `description` = '日常行政通知、会议通知、放假调休、制度宣贯',
    `status` = 1,
    `update_user` = 1,
    `update_time` = NOW()
WHERE `id` = 1 AND `dict_id` = 1 AND `deleted` = 0;

UPDATE `sys_dict_item`
SET `label` = '政策文件',
    `color` = 'primary',
    `sort` = 2,
    `description` = '新规发布、政策解读、上级下发文件',
    `status` = 1,
    `update_user` = 1,
    `update_time` = NOW()
WHERE `id` = 2 AND `dict_id` = 1 AND `deleted` = 0;

-- 新增分类（固定 id，幂等：已存在则更新）
-- 颜色分级：default资讯 → primary常规 → success人事 → warning关注 → error紧急
INSERT INTO `sys_dict_item`
(`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
VALUES
(6, '新闻动态', '3', 'default', 3, '公司动态、项目进展、行业资讯', 1, 1, 1, NOW()),
(7, '人事公告', '4', 'success', 4, '任免、招聘、入职离职、考勤、绩效考核公示', 1, 1, 1, NOW()),
(8, '财务公示', '5', 'warning', 5, '预算、采购招标、中标结果、费用公示', 1, 1, 1, NOW()),
(9, '安全通告', '6', 'error', 6, '网络安全预警、系统维护、漏洞提示、版本升级', 1, 1, 1, NOW()),
(10, '运维公告', '7', 'warning', 7, '服务器停机、系统更新、功能变更、接口下线', 1, 1, 1, NOW()),
(11, '公示通告', '8', 'default', 8, '结果公示、评选、申诉、公开征求意见', 1, 1, 1, NOW()),
(12, '紧急通告', '9', 'error', 9, '突发事件、应急通知、重要紧急事项', 1, 1, 1, NOW())
ON DUPLICATE KEY UPDATE
    `label` = VALUES(`label`),
    `value` = VALUES(`value`),
    `color` = VALUES(`color`),
    `sort` = VALUES(`sort`),
    `description` = VALUES(`description`),
    `status` = 1,
    `dict_id` = 1,
    `deleted` = 0,
    `update_user` = 1,
    `update_time` = NOW();
