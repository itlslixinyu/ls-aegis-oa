--liquibase formatted sql
-- Financial compensation is append-only. Never auto-match historical income/cost by amount.
-- Deploy schema before new JAR. Rollback requires preserving these columns and audit events.
--changeset ls:r28_eoms_eoms_payment_status_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='eoms_payment' AND column_name='status'
ALTER TABLE eoms_payment ADD COLUMN status varchar(20) NOT NULL DEFAULT 'ACTIVE';

--changeset ls:r28_eoms_eoms_payment_reversed_time_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='eoms_payment' AND column_name='reversed_time'
ALTER TABLE eoms_payment ADD COLUMN reversed_time timestamp DEFAULT NULL;

--changeset ls:r28_eoms_eoms_payment_reversed_by_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='eoms_payment' AND column_name='reversed_by'
ALTER TABLE eoms_payment ADD COLUMN reversed_by varchar(64) DEFAULT NULL;

--changeset ls:r28_eoms_eoms_income_payment_id_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='eoms_income' AND column_name='payment_id'
ALTER TABLE eoms_income ADD COLUMN payment_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_eoms_income_reversal_of_id_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='eoms_income' AND column_name='reversal_of_id'
ALTER TABLE eoms_income ADD COLUMN reversal_of_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_eoms_cost_settlement_id_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='eoms_cost' AND column_name='settlement_id'
ALTER TABLE eoms_cost ADD COLUMN settlement_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_eoms_cost_reversal_of_id_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='eoms_cost' AND column_name='reversal_of_id'
ALTER TABLE eoms_cost ADD COLUMN reversal_of_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_eoms_receivable_settlement_id_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='eoms_receivable' AND column_name='settlement_id'
ALTER TABLE eoms_receivable ADD COLUMN settlement_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_uk_income_payment_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname=current_schema() AND tablename='eoms_income' AND indexname='uk_income_payment'
CREATE UNIQUE INDEX uk_income_payment ON eoms_income (payment_id);

--changeset ls:r28_eoms_uk_income_reversal_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname=current_schema() AND tablename='eoms_income' AND indexname='uk_income_reversal'
CREATE UNIQUE INDEX uk_income_reversal ON eoms_income (reversal_of_id);

--changeset ls:r28_eoms_uk_cost_reversal_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname=current_schema() AND tablename='eoms_cost' AND indexname='uk_cost_reversal'
CREATE UNIQUE INDEX uk_cost_reversal ON eoms_cost (reversal_of_id);

--changeset ls:r28_eoms_idx_cost_settlement_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname=current_schema() AND tablename='eoms_cost' AND indexname='idx_cost_settlement'
CREATE INDEX idx_cost_settlement ON eoms_cost (settlement_id);

--changeset ls:r28_eoms_idx_receivable_settlement_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname=current_schema() AND tablename='eoms_receivable' AND indexname='idx_receivable_settlement'
CREATE INDEX idx_receivable_settlement ON eoms_receivable (settlement_id);

--changeset ls:r28_eoms_idx_payment_status_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname=current_schema() AND tablename='eoms_payment' AND indexname='idx_payment_status'
CREATE INDEX idx_payment_status ON eoms_payment (status);

--changeset ls:r28_eoms_finance_event_postgresql dbms:postgresql
CREATE TABLE IF NOT EXISTS eoms_finance_event (
  id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  aggregate_type varchar(20) NOT NULL,
  aggregate_id bigint NOT NULL,
  action varchar(20) NOT NULL,
  related_id bigint DEFAULT NULL,
  amount decimal(12,2) DEFAULT NULL,
  operator varchar(64) DEFAULT NULL,
  reason varchar(500) NOT NULL,
  create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_finance_event UNIQUE (aggregate_type, aggregate_id, action)
);

-- Preserve inactive history while enforcing exactly one active settlement per order.
--changeset ls:r28_eoms_settlement_active_order_unique_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname=current_schema() AND tablename='eoms_repair_settlement' AND indexname='uk_settlement_active_order'
CREATE UNIQUE INDEX uk_settlement_active_order ON eoms_repair_settlement (repair_order_id) WHERE del_flag='0' AND status<>'REVERSED';

--changeset ls:r28_eoms_settlement_history_order_index_postgresql dbms:postgresql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname=current_schema() AND tablename='eoms_repair_settlement' AND indexname='idx_settlement_order_history'
CREATE INDEX idx_settlement_order_history ON eoms_repair_settlement (repair_order_id);

--changeset ls:r28_eoms_settlement_remove_history_unique_postgresql dbms:postgresql
ALTER TABLE eoms_repair_settlement DROP CONSTRAINT IF EXISTS uk_repair_order_id;
DROP INDEX IF EXISTS uk_repair_order_id;
