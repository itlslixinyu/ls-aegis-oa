-- R31: run through releases/r31-postgresql.yaml, outside a transaction.
-- A failed concurrent build can leave an invalid index; the release rejects it for explicit DBA diagnosis.
CREATE INDEX CONCURRENTLY idx_stock_alert_balance
    ON psi_stock_batch USING btree (tenant_id, deleted, goods_id, warehouse_id, remain_quantity);
