-- liquibase formatted sql

-- changeset eoms:contract_no_prefix_htwx
-- comment 维修合同编号默认前缀 HT → HTWX（与销售 HTXS、采购 HTCG 对齐）
SET NAMES utf8mb4;

UPDATE `sys_option`
SET `default_value` = 'HTWX',
    `value` = CASE
        WHEN `value` IS NULL OR TRIM(`value`) = '' OR UPPER(TRIM(`value`)) = 'HT' THEN NULL
        ELSE `value`
    END,
    `description` = '维修合同自动编号前缀',
    `update_time` = NOW()
WHERE `category` = 'EOMS'
  AND `code` = 'CONTRACT_NO_PREFIX';
