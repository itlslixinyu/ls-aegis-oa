-- liquibase formatted sql

-- changeset psi:143 validCheckSum:ANY
-- comment PSI：入库默认打印模板种子（HTML 空则用内置；Word 可用 render-docx）

INSERT INTO `psi_print_template`
(`tenant_id`, `template_name`, `biz_type`, `file_id`, `content_html`, `field_mapping`, `is_default`, `status`, `remark`, `create_user`, `create_time`, `deleted`)
SELECT 0, '入库单默认模板', 'STOCK_IN', NULL, NULL,
       '{"placeholders":["docNo","inDate","warehouseName","unitName","totalAmount","remark","itemsHtml","itemsText"]}',
       1, 1, '系统预置；可改 HTML 或保持空用内置样式；Word 走 /render-docx', 1, NOW(), 0
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `psi_print_template` WHERE `tenant_id` = 0 AND `biz_type` = 'STOCK_IN' AND `is_default` = 1 AND `deleted` = 0
);
