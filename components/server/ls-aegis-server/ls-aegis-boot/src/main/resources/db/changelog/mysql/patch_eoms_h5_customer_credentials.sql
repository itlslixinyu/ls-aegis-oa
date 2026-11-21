-- liquibase formatted sql

-- changeset ls:eoms_h5_customer_credentials
-- comment EOMS 客户门户：客户账号密码凭据
CREATE TABLE IF NOT EXISTS `eoms_h5_customer_credential` (
    `tenant_id`     bigint(20)    NOT NULL COMMENT '租户ID',
    `customer_id`   bigint(20)    NOT NULL COMMENT '客户ID，关联 psi_unit.id',
    `login_name`    varchar(32)   NOT NULL COMMENT '客户门户用户名（全局唯一）',
    `password_hash` varchar(255)  NOT NULL COMMENT '平台 PasswordEncoder 生成的口令摘要',
    `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`tenant_id`, `customer_id`),
    UNIQUE KEY `uk_eoms_h5_credential_login` (`login_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='EOMS H5客户门户凭据';
