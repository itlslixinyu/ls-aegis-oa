-- liquibase formatted sql

-- changeset ls:c7_dict_sign_status
-- comment C7：签章启用状态 / 签章记录状态走 sys_dict
SET NAMES utf8mb4;

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 306, '签章印章状态', 'sign_seal_status', '0启用 1禁用', b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'sign_seal_status' AND `deleted` = 0);

INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
SELECT 307, '签章记录状态', 'sign_record_status', NULL, b'1', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'sign_record_status' AND `deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 660, '启用', '0', 'success', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '0' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 661, '禁用', '1', 'error', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_seal_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = '1' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 662, '成功', 'completed', 'success', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_record_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'completed' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 663, '失败', 'failed', 'error', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'sign_record_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'failed' AND i.`deleted` = 0);
