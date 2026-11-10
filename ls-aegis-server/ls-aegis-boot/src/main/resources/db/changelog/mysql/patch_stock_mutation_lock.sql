--liquibase formatted sql
--changeset ls:r28_stock_mutation_lock_mysql dbms:mysql
CREATE TABLE IF NOT EXISTS psi_stock_mutation_lock (
    tenant_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    goods_id bigint NOT NULL,
    PRIMARY KEY (tenant_id, warehouse_id, goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存变更串行锚点（包括零库存维度）';
