-- liquibase formatted sql

-- changeset ls:warehouse_manager_user_id
-- comment 仓库负责人绑定系统用户；存量回填为创建人
SET NAMES utf8mb4;

-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_warehouse' AND COLUMN_NAME = 'manager_user_id'
ALTER TABLE `psi_warehouse`
  ADD COLUMN `manager_user_id` bigint(20) DEFAULT NULL COMMENT '负责人用户ID，关联 sys_user.id' AFTER `warehouse_name`,
  ADD INDEX `idx_manager_user` (`manager_user_id`);

UPDATE `psi_warehouse` w
LEFT JOIN `sys_user` u ON u.id = w.create_user
SET w.`manager_user_id` = w.`create_user`,
    w.`manager` = COALESCE(NULLIF(TRIM(w.`manager`), ''), u.nickname, u.username, w.`manager`)
WHERE w.`deleted` = 0 AND w.`manager_user_id` IS NULL;

-- changeset ls:quote_warehouse_id
-- comment 报价单关联出库/备货仓库
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_quote' AND COLUMN_NAME = 'warehouse_id'
ALTER TABLE `eoms_quote`
  ADD COLUMN `warehouse_id` bigint(20) DEFAULT NULL COMMENT '仓库ID，关联 psi_warehouse.id' AFTER `customer_id`,
  ADD KEY `idx_quote_warehouse` (`warehouse_id`);
