-- liquibase formatted sql

-- changeset ls:eoms_permission_page_fix
-- comment EOMS 页面权限码与后端 @SaCheckPermission 对齐
SET NAMES utf8mb4;

UPDATE `sys_menu` SET `permission` = 'eoms:repair:list', `update_time` = NOW()
WHERE `id` = 20010 AND `permission` = 'eoms:repair:apply:list';

UPDATE `sys_menu` SET `permission` = 'eoms:order:query', `update_time` = NOW()
WHERE `id` = 20011 AND `permission` = 'eoms:repair:order:list';

UPDATE `sys_menu` SET `permission` = 'eoms:record:list', `update_time` = NOW()
WHERE `id` = 20012 AND `permission` = 'eoms:repair:record:list';

UPDATE `sys_menu` SET `permission` = 'eoms:settlement:query', `update_time` = NOW()
WHERE `id` = 20013 AND `permission` = 'eoms:repair:settlement:list';

UPDATE `sys_menu` SET `permission` = 'eoms:profit:query', `update_time` = NOW()
WHERE `id` = 20042 AND `permission` = 'eoms:finance:profit';

UPDATE `sys_menu` SET `permission` = 'eoms:dashboard:list', `update_time` = NOW()
WHERE `id` = 20070 AND `permission` = 'eoms:dashboard:view';

-- changeset ls:eoms_permission_buttons
-- comment EOMS 各模块按钮权限（20101–20181，含工单 20111–20120），与后端 Controller 对齐
SET NAMES utf8mb4;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20101, '查询', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:list', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20101);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20102, '详情', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:query', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20102);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20103, '新增', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:add', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20103);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20104, '确认', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:confirm', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20104);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20105, '取消', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:cancel', 5, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20105);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20106, '关闭', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:close', 6, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20106);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20107, '转工单', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:assign', 7, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20107);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20108, '删除', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:remove', 8, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20108);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20111, '查询', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:query', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20111);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20112, '新增', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:add', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20112);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20113, '修改', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:update', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20113);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20114, '删除', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:delete', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20114);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20115, '导出', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:export', 5, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20115);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20116, '接单', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:accept', 6, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20116);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20117, '开始维修', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:start', 7, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20117);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20118, '完成维修', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:finish', 8, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20118);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20119, '关闭', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:close', 9, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20119);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20120, '派工', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:assign', 10, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20120);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20121, '查询', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:list', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20121);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20122, '导出', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:export', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20122);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20123, '详情', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:query', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20123);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20124, '新增', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:add', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20124);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20125, '修改', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:update', 5, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20125);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20126, '删除', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:delete', 6, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20126);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20131, '查询', 20013, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:settlement:query', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20131);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20132, '保存', 20013, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:settlement:save', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20132);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20133, '确认', 20013, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:settlement:confirm', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20133);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20141, '导出', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:export', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20141);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20142, '详情', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:query', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20142);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20143, '新增', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:add', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20143);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20144, '修改', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:update', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20144);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20145, '删除', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:delete', 5, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20145);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20151, '查询', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:query', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20151);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20152, '导出', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:export', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20152);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20153, '新增', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:add', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20153);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20154, '修改', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:update', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20154);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20155, '删除', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:delete', 5, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20155);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20161, '查询', 20041, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:payment:query', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20161);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20162, '新增', 20041, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:payment:add', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20162);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20163, '修改', 20041, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:payment:update', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20163);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20164, '删除', 20041, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:payment:delete', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20164);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20171, '查询', 20042, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:profit:query', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20171);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 20181, '导出', 20070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:dashboard:list', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 20181);

-- changeset ls:eoms_permission_role_menu
-- comment 新按钮权限绑定超管(1)与普通角色(3)，并同步已勾选父页面的自定义角色
SET NAMES utf8mb4;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (SELECT `id` FROM `sys_menu` WHERE `id` BETWEEN 20101 AND 20199) m
WHERE r.id IN (1, 3);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, btn.`id`
FROM `sys_role_menu` rm
INNER JOIN `sys_menu` btn ON btn.`parent_id` = rm.`menu_id` AND btn.`id` BETWEEN 20101 AND 20199
WHERE rm.`menu_id` IN (20010, 20011, 20012, 20013, 20032, 20040, 20041, 20042, 20070)
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = btn.`id`
  );
