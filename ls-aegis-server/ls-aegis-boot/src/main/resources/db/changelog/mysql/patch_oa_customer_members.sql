-- liquibase formatted sql

-- changeset ls:oa_customer_members_accounts
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='eoms_h5_customer_credential' AND COLUMN_NAME='id'
ALTER TABLE eoms_h5_customer_credential
 DROP PRIMARY KEY,
 ADD COLUMN id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
 ADD COLUMN display_name varchar(50) NOT NULL DEFAULT '',
 ADD COLUMN account_role varchar(16) NOT NULL DEFAULT 'MEMBER',
 ADD COLUMN status tinyint NOT NULL DEFAULT 1,
 ADD COLUMN auth_version bigint NOT NULL DEFAULT 1,
 ADD KEY idx_h5_member_customer(tenant_id,customer_id);
UPDATE eoms_h5_customer_credential SET display_name=login_name,account_role='ADMIN';

-- changeset ls:oa_customer_members_reporter
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='eoms_repair_apply' AND COLUMN_NAME='reporter_account_id'
ALTER TABLE eoms_repair_apply ADD COLUMN reporter_account_id bigint NULL,
 ADD COLUMN reporter_name varchar(50) NULL,
 ADD KEY idx_repair_member(tenant_id,customer_id,reporter_account_id,del_flag);
