--liquibase formatted sql
-- Additive only: keep cash originals, counter-documents, reasons and repair stock links.
--changeset ls:r29_psi_receipt_refunded_amount_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='refunded_amount'
ALTER TABLE psi_receipt ADD COLUMN refunded_amount decimal(18,2) NOT NULL DEFAULT 0.00;

--changeset ls:r29_psi_receipt_refund_source_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='refund_source_id'
ALTER TABLE psi_receipt ADD COLUMN refund_source_id bigint DEFAULT NULL;

--changeset ls:r29_psi_receipt_refund_return_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='refund_return_id'
ALTER TABLE psi_receipt ADD COLUMN refund_return_id bigint DEFAULT NULL;

--changeset ls:r29_psi_receipt_void_reason_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='void_reason'
ALTER TABLE psi_receipt ADD COLUMN void_reason varchar(500) DEFAULT NULL;

--changeset ls:r29_psi_receipt_void_user_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='void_user'
ALTER TABLE psi_receipt ADD COLUMN void_user bigint DEFAULT NULL;

--changeset ls:r29_psi_receipt_void_time_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='void_time'
ALTER TABLE psi_receipt ADD COLUMN void_time datetime DEFAULT NULL;

--changeset ls:r29_psi_payment_refunded_amount_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='refunded_amount'
ALTER TABLE psi_payment ADD COLUMN refunded_amount decimal(18,2) NOT NULL DEFAULT 0.00;

--changeset ls:r29_psi_payment_refund_source_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='refund_source_id'
ALTER TABLE psi_payment ADD COLUMN refund_source_id bigint DEFAULT NULL;

--changeset ls:r29_psi_payment_refund_return_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='refund_return_id'
ALTER TABLE psi_payment ADD COLUMN refund_return_id bigint DEFAULT NULL;

--changeset ls:r29_psi_payment_void_reason_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='void_reason'
ALTER TABLE psi_payment ADD COLUMN void_reason varchar(500) DEFAULT NULL;

--changeset ls:r29_psi_payment_void_user_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='void_user'
ALTER TABLE psi_payment ADD COLUMN void_user bigint DEFAULT NULL;

--changeset ls:r29_psi_payment_void_time_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='void_time'
ALTER TABLE psi_payment ADD COLUMN void_time datetime DEFAULT NULL;

--changeset ls:r29_psi_stock_out_repair_order_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_stock_out' AND column_name='repair_order_id'
ALTER TABLE psi_stock_out ADD COLUMN repair_order_id bigint DEFAULT NULL;

--changeset ls:r29_psi_stock_out_repair_settlement_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_stock_out' AND column_name='repair_settlement_id'
ALTER TABLE psi_stock_out ADD COLUMN repair_settlement_id bigint DEFAULT NULL;

--changeset ls:r29_idx_receipt_refund_return_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND index_name='idx_receipt_refund_return'
CREATE INDEX idx_receipt_refund_return ON psi_receipt (refund_return_id, doc_status);

--changeset ls:r29_idx_payment_refund_return_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='psi_payment' AND index_name='idx_payment_refund_return'
CREATE INDEX idx_payment_refund_return ON psi_payment (refund_return_id, doc_status);

--changeset ls:r29_idx_receipt_refund_source_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND index_name='idx_receipt_refund_source'
CREATE INDEX idx_receipt_refund_source ON psi_receipt (refund_source_id);

--changeset ls:r29_idx_payment_refund_source_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='psi_payment' AND index_name='idx_payment_refund_source'
CREATE INDEX idx_payment_refund_source ON psi_payment (refund_source_id);

--changeset ls:r29_idx_stock_out_repair_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='psi_stock_out' AND index_name='idx_stock_out_repair'
CREATE INDEX idx_stock_out_repair ON psi_stock_out (repair_order_id, doc_status);

--changeset ls:r29_psi_receipt_operation_sequence_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='operation_sequence'
ALTER TABLE psi_receipt ADD COLUMN operation_sequence bigint NOT NULL DEFAULT 0;

--changeset ls:r29_psi_receipt_source_sequence_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='source_sequence'
ALTER TABLE psi_receipt ADD COLUMN source_sequence bigint DEFAULT NULL;

--changeset ls:r29_psi_receipt_void_sequence_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_receipt' AND column_name='void_sequence'
ALTER TABLE psi_receipt ADD COLUMN void_sequence bigint DEFAULT NULL;

--changeset ls:r29_psi_payment_operation_sequence_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='operation_sequence'
ALTER TABLE psi_payment ADD COLUMN operation_sequence bigint NOT NULL DEFAULT 0;

--changeset ls:r29_psi_payment_source_sequence_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='source_sequence'
ALTER TABLE psi_payment ADD COLUMN source_sequence bigint DEFAULT NULL;

--changeset ls:r29_psi_payment_void_sequence_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_payment' AND column_name='void_sequence'
ALTER TABLE psi_payment ADD COLUMN void_sequence bigint DEFAULT NULL;

--changeset ls:r29_psi_write_off_item_cash_sequence_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_write_off_item' AND column_name='cash_sequence'
ALTER TABLE psi_write_off_item ADD COLUMN cash_sequence bigint DEFAULT NULL;

--changeset ls:r29_psi_bank_statement_unmatch_reason_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_bank_statement' AND column_name='unmatch_reason'
ALTER TABLE psi_bank_statement ADD COLUMN unmatch_reason varchar(500) DEFAULT NULL;

--changeset ls:r29_psi_bank_statement_unmatch_user_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_bank_statement' AND column_name='unmatch_user'
ALTER TABLE psi_bank_statement ADD COLUMN unmatch_user bigint DEFAULT NULL;

--changeset ls:r29_psi_bank_statement_unmatch_time_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='psi_bank_statement' AND column_name='unmatch_time'
ALTER TABLE psi_bank_statement ADD COLUMN unmatch_time datetime DEFAULT NULL;

--changeset ls:r29_bank_duplicate_guard_mysql dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM (SELECT tenant_id,match_biz_type,match_biz_id,deleted FROM psi_bank_statement WHERE deleted=0 AND match_biz_type IS NOT NULL AND match_biz_id IS NOT NULL GROUP BY tenant_id,match_biz_type,match_biz_id,deleted HAVING COUNT(*)>1) dup
-- Fail closed rather than automatically merge or delete historical bank associations.
SELECT 1;

--changeset ls:r29_bank_cash_unique_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='psi_bank_statement' AND index_name='uk_bank_cash_match'
CREATE UNIQUE INDEX uk_bank_cash_match ON psi_bank_statement (tenant_id,match_biz_type,match_biz_id,deleted);
