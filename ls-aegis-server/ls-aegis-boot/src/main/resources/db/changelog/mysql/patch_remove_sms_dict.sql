-- liquibase formatted sql

-- changeset ls:remove_sms_dict
-- comment 停用短信：软删除短信厂商字典及字典项
SET NAMES utf8mb4;

UPDATE `sys_dict_item`
SET `status` = 2, `update_user` = 1, `update_time` = NOW()
WHERE `dict_id` = 3 AND `status` = 1;

UPDATE `sys_dict`
SET `deleted` = `id`, `update_user` = 1, `update_time` = NOW()
WHERE `id` = 3 AND `code` = 'sms_supplier' AND `deleted` = 0;
