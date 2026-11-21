-- liquibase formatted sql

-- changeset aegis:sign_menu
-- comment P5：写入 sign 平台菜单 30000 段
SET NAMES utf8mb4;

-- 电子签章平台菜单（ID 段 30000–30999）
-- 幂等：先删角色绑定与菜单，再写入；可重复执行
-- 组件路径须与前端 asyncModules 键一致（无前导 /、无 .vue）

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 30000 AND 30999;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 30000 AND 30999;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(30000, '电子签章', 0, 1, '/sign', 'SignRoot', 'Layout', '/sign/dashboard', 'stamp', b'0', b'0', b'0', NULL, 6, 1, 1, NOW()),

(30001, '签章看板', 30000, 2, '/sign/dashboard', 'SignDashboard', 'sign/dashboard/index', NULL, 'dashboard', b'0', b'0', b'0', 'sign:dashboard:view', 1, 1, 1, NOW()),
(30002, '印章管理', 30000, 2, '/sign/seal', 'SignSeal', 'sign/seal/index', NULL, 'stamp', b'0', b'0', b'0', 'sign:seal:list', 2, 1, 1, NOW()),
(30003, '印章提取', 30000, 2, '/sign/seal/extract', 'SignSealExtract', 'sign/seal/extract', NULL, 'scissor', b'0', b'0', b'0', 'sign:seal:extract', 3, 1, 1, NOW()),
(30004, '项目管理', 30000, 2, '/sign/project', 'SignProject', 'sign/project/index', NULL, 'folder', b'0', b'0', b'0', 'sign:project:list', 4, 1, 1, NOW()),
(30005, '签署执行', 30000, 2, '/sign/execute', 'SignExecute', 'sign/execute/index', NULL, 'edit', b'0', b'0', b'0', 'sign:execute:list', 5, 1, 1, NOW()),
(30006, '签署记录', 30000, 2, '/sign/log', 'SignLog', 'sign/log/index', NULL, 'history', b'0', b'0', b'0', 'sign:log:list', 6, 1, 1, NOW()),
(30007, '审计日志', 30000, 2, '/sign/audit/log', 'SignAuditLog', 'sign/audit/log', NULL, 'safe', b'0', b'0', b'0', 'sign:audit:list', 7, 1, 1, NOW()),
(30008, '验真核验', 30000, 2, '/sign/verify', 'SignVerify', 'sign/verify/index', NULL, 'check-circle', b'0', b'0', b'0', 'sign:verify:list', 8, 1, 1, NOW());

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (SELECT `id` FROM `sys_menu` WHERE `id` BETWEEN 30000 AND 30999) m
WHERE r.id IN (1, 3);
