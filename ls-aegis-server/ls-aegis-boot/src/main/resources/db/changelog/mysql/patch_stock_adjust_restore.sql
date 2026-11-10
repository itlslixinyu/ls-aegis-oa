--liquibase formatted sql
--changeset ls:r29_stock_adjust_origin_batch_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_stock_adjust_item' AND column_name='origin_batch_id'
ALTER TABLE psi_stock_adjust_item ADD COLUMN origin_batch_id bigint DEFAULT NULL COMMENT '显式盘盈纠正的原批次';
