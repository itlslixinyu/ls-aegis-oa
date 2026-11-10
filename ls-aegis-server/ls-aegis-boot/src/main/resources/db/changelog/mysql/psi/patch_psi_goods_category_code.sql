-- liquibase formatted sql

-- changeset psi:121
-- validCheckSum: ANY
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_goods_category' AND COLUMN_NAME = 'code'
-- comment PSI：商品分类增加层级编码 code 列
ALTER TABLE `psi_goods_category`
    ADD COLUMN `code` varchar(32) DEFAULT NULL COMMENT '分类编码（层级拼接，如 11 / 1111，首位不为0）' AFTER `parent_id`;

-- changeset psi:122
-- validCheckSum: ANY
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_goods_category' AND INDEX_NAME = 'uk_category_code'
-- comment PSI：商品分类编码唯一索引
ALTER TABLE `psi_goods_category`
    ADD UNIQUE INDEX `uk_category_code` (`tenant_id`, `code`, `deleted`);
