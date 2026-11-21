-- liquibase formatted sql

-- changeset ls:quote_form_fields
-- comment 报价单据表单：表头扩展字段与明细行扩展字段
SET NAMES utf8mb4;

-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_quote' AND COLUMN_NAME = 'project_name'
ALTER TABLE `eoms_quote`
  MODIFY COLUMN `customer_id` bigint(20) DEFAULT NULL COMMENT '客户ID，关联 psi_unit.id',
  ADD COLUMN `project_name` varchar(200) DEFAULT NULL COMMENT '项目名称' AFTER `customer_id`,
  ADD COLUMN `quote_unit` varchar(200) DEFAULT NULL COMMENT '报价单位' AFTER `project_name`,
  ADD COLUMN `quote_date` datetime DEFAULT NULL COMMENT '报价时间' AFTER `quote_unit`,
  ADD COLUMN `valid_until` datetime DEFAULT NULL COMMENT '报价有效期' AFTER `quote_date`,
  ADD COLUMN `tax_rate` decimal(5,2) NOT NULL DEFAULT 4.00 COMMENT '增值税综合税率(%)' AFTER `valid_until`,
  ADD COLUMN `material_subtotal` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '材料小计' AFTER `tax_rate`,
  ADD COLUMN `service_subtotal` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '技术服务费小计' AFTER `material_subtotal`,
  ADD COLUMN `terms` text DEFAULT NULL COMMENT '备注/条款' AFTER `remark`;

-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_quote_detail' AND COLUMN_NAME = 'brand_model'
ALTER TABLE `eoms_quote_detail`
  MODIFY COLUMN `item_name` varchar(200) NOT NULL COMMENT '材料名称/服务项目',
  ADD COLUMN `brand_model` varchar(100) DEFAULT NULL COMMENT '品牌/型号' AFTER `item_name`,
  ADD COLUMN `unit` varchar(20) DEFAULT '台' COMMENT '单位' AFTER `brand_model`,
  ADD COLUMN `description` varchar(200) DEFAULT NULL COMMENT '服务说明' AFTER `unit`,
  ADD COLUMN `sort_no` int NOT NULL DEFAULT 0 COMMENT '行序号' AFTER `description`;
