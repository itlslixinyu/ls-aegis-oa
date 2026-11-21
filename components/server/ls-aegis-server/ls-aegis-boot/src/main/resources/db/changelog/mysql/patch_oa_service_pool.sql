-- liquibase formatted sql

-- changeset ls:oa_report_address_20261112
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='eoms_repair_apply' AND column_name='report_address'
ALTER TABLE eoms_repair_apply ADD COLUMN report_address varchar(500) DEFAULT NULL COMMENT '客户提交的报修地址，独立于签到签退地址' AFTER customer_id;

-- changeset ls:oa_pool_lookup_20261112
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='eoms_repair_apply' AND index_name='idx_apply_pool'
CREATE INDEX idx_apply_pool ON eoms_repair_apply(tenant_id,status,apply_time,del_flag);
