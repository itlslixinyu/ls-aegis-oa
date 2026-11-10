--liquibase formatted sql
-- Financial compensation is append-only. Never auto-match historical income/cost by amount.
-- Deploy schema before new JAR. Rollback requires preserving these columns and audit events.
--changeset ls:r28_eoms_eoms_payment_status_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_payment' AND column_name='status'
ALTER TABLE eoms_payment ADD COLUMN status varchar(20) NOT NULL DEFAULT 'ACTIVE';

--changeset ls:r28_eoms_eoms_payment_reversed_time_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_payment' AND column_name='reversed_time'
ALTER TABLE eoms_payment ADD COLUMN reversed_time datetime DEFAULT NULL;

--changeset ls:r28_eoms_eoms_payment_reversed_by_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_payment' AND column_name='reversed_by'
ALTER TABLE eoms_payment ADD COLUMN reversed_by varchar(64) DEFAULT NULL;

--changeset ls:r28_eoms_eoms_income_payment_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_income' AND column_name='payment_id'
ALTER TABLE eoms_income ADD COLUMN payment_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_eoms_income_reversal_of_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_income' AND column_name='reversal_of_id'
ALTER TABLE eoms_income ADD COLUMN reversal_of_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_eoms_cost_settlement_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_cost' AND column_name='settlement_id'
ALTER TABLE eoms_cost ADD COLUMN settlement_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_eoms_cost_reversal_of_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_cost' AND column_name='reversal_of_id'
ALTER TABLE eoms_cost ADD COLUMN reversal_of_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_eoms_receivable_settlement_id_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_receivable' AND column_name='settlement_id'
ALTER TABLE eoms_receivable ADD COLUMN settlement_id bigint DEFAULT NULL;

--changeset ls:r28_eoms_uk_income_payment_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_income' AND index_name='uk_income_payment'
CREATE UNIQUE INDEX uk_income_payment ON eoms_income (payment_id);

--changeset ls:r28_eoms_uk_income_reversal_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_income' AND index_name='uk_income_reversal'
CREATE UNIQUE INDEX uk_income_reversal ON eoms_income (reversal_of_id);

--changeset ls:r28_eoms_uk_cost_reversal_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_cost' AND index_name='uk_cost_reversal'
CREATE UNIQUE INDEX uk_cost_reversal ON eoms_cost (reversal_of_id);

--changeset ls:r28_eoms_idx_cost_settlement_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_cost' AND index_name='idx_cost_settlement'
CREATE INDEX idx_cost_settlement ON eoms_cost (settlement_id);

--changeset ls:r28_eoms_idx_receivable_settlement_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_receivable' AND index_name='idx_receivable_settlement'
CREATE INDEX idx_receivable_settlement ON eoms_receivable (settlement_id);

--changeset ls:r28_eoms_idx_payment_status_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_payment' AND index_name='idx_payment_status'
CREATE INDEX idx_payment_status ON eoms_payment (status);

--changeset ls:r28_eoms_finance_event_mysql dbms:mysql
CREATE TABLE IF NOT EXISTS eoms_finance_event (
  id bigint NOT NULL AUTO_INCREMENT,
  aggregate_type varchar(20) NOT NULL,
  aggregate_id bigint NOT NULL,
  action varchar(20) NOT NULL,
  related_id bigint DEFAULT NULL,
  amount decimal(12,2) DEFAULT NULL,
  operator varchar(64) DEFAULT NULL,
  reason varchar(500) NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_finance_event (aggregate_type, aggregate_id, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修财务补偿审计（保留原始单据与正反流水）';

-- Retain every reversed/deleted settlement, but permit only one live settlement per order.
-- Install replacement uniqueness before removing the legacy whole-history constraint.
--changeset ls:r28_eoms_settlement_active_order_column_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_repair_settlement' AND column_name='active_order_id'
ALTER TABLE eoms_repair_settlement ADD COLUMN active_order_id bigint GENERATED ALWAYS AS (CASE WHEN del_flag='0' AND status<>'REVERSED' THEN repair_order_id ELSE NULL END) STORED;

--changeset ls:r28_eoms_settlement_active_order_unique_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_repair_settlement' AND index_name='uk_settlement_active_order'
CREATE UNIQUE INDEX uk_settlement_active_order ON eoms_repair_settlement (active_order_id);

--changeset ls:r28_eoms_settlement_history_order_index_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_repair_settlement' AND index_name='idx_settlement_order_history'
CREATE INDEX idx_settlement_order_history ON eoms_repair_settlement (repair_order_id);

--changeset ls:r28_eoms_settlement_remove_history_unique_mysql dbms:mysql
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_repair_settlement' AND index_name='uk_repair_order_id'
ALTER TABLE eoms_repair_settlement DROP INDEX uk_repair_order_id;
