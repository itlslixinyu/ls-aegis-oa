--liquibase formatted sql
--changeset ls:r43_eoms_contract_sign_date_mysql dbms:mysql
-- Historical signing dates are unknown; do not derive them from creation/start dates.
ALTER TABLE eoms_contract ADD COLUMN sign_date DATETIME DEFAULT NULL COMMENT '签订日期；历史未记录时为空';
