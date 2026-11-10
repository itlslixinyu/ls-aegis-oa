-- liquibase formatted sql

-- changeset eoms:contract_sign_date validCheckSum:ANY
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_contract' AND COLUMN_NAME = 'sign_date'
-- comment 维修合同增加签订日，对齐销售/采购合同字段
ALTER TABLE `eoms_contract`
    ADD COLUMN `sign_date` datetime DEFAULT NULL COMMENT '签订日' AFTER `amount`;

-- changeset eoms:contract_status_draft_confirmed validCheckSum:ANY
-- comment 维修合同状态对齐销售/采购：草稿/已确认；存量生效类视为已确认
UPDATE `eoms_contract`
SET `status` = 'CONFIRMED'
WHERE `del_flag` = '0'
  AND `status` IN ('EFFECTIVE', 'EXPIRED', 'TERMINATED', '0');

UPDATE `eoms_contract`
SET `status` = 'DRAFT'
WHERE `del_flag` = '0'
  AND (`status` IS NULL OR `status` = '');

ALTER TABLE `eoms_contract`
    MODIFY COLUMN `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿、CONFIRMED-已确认';

-- changeset eoms:contract_status_dict_align validCheckSum:ANY
-- comment 字典补充草稿/已确认，停用旧生效类状态项
INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 503, '草稿', 'DRAFT', 'default', 1, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_contract_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'DRAFT' AND i.`deleted` = 0);

INSERT INTO `sys_dict_item` (`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
SELECT 504, '已确认', 'CONFIRMED', 'success', 2, NULL, 1, d.id, 1, NOW()
FROM `sys_dict` d WHERE d.`code` = 'eoms_contract_status' AND d.`deleted` = 0
AND NOT EXISTS (SELECT 1 FROM `sys_dict_item` i WHERE i.`dict_id` = d.`id` AND i.`value` = 'CONFIRMED' AND i.`deleted` = 0);

UPDATE `sys_dict_item` i
INNER JOIN `sys_dict` d ON d.`id` = i.`dict_id` AND d.`code` = 'eoms_contract_status' AND d.`deleted` = 0
SET i.`status` = 2
WHERE i.`value` IN ('EFFECTIVE', 'EXPIRED', 'TERMINATED') AND i.`deleted` = 0;
