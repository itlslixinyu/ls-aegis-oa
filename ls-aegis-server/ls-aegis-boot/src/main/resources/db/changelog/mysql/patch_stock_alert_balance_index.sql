-- R31: run through releases/r31-mysql.yaml, which validates any existing same-name index.
-- Online secondary-index construction; unsupported algorithm/lock capability must fail, never fall back to COPY.
ALTER TABLE psi_stock_batch
    ADD INDEX idx_stock_alert_balance (tenant_id, deleted, goods_id, warehouse_id, remain_quantity),
    ALGORITHM=INPLACE, LOCK=NONE;
