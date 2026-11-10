--liquibase formatted sql
--changeset ls:r32_quote_data_scope_mysql dbms:mysql
-- Legacy ownership is deliberately unknown. Never derive user IDs from reusable usernames.
-- Apply before the candidate JAR. Any exact pre-existing columns must be validated by the release runner.
ALTER TABLE eoms_quote
    ADD COLUMN tenant_id BIGINT DEFAULT NULL,
    ADD COLUMN create_user BIGINT DEFAULT NULL,
    ADD COLUMN dept_id BIGINT DEFAULT NULL;
