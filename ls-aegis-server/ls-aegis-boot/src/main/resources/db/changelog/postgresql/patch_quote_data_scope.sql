--liquibase formatted sql
--changeset ls:r32_quote_data_scope_pg dbms:postgresql
-- No historical ownership guesses or business DML; audited assignment is a separate operator action.
ALTER TABLE eoms_quote
    ADD COLUMN tenant_id BIGINT DEFAULT NULL,
    ADD COLUMN create_user BIGINT DEFAULT NULL,
    ADD COLUMN dept_id BIGINT DEFAULT NULL;
