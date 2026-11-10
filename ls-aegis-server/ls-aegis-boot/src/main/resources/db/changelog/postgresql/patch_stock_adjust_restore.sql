--liquibase formatted sql
--changeset ls:r29_stock_adjust_origin_batch_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='psi_stock_adjust_item' AND column_name='origin_batch_id'
ALTER TABLE psi_stock_adjust_item ADD COLUMN origin_batch_id bigint DEFAULT NULL;
