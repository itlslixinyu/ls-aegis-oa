-- liquibase formatted sql

-- changeset ls:create_sys_user_preference
-- comment 用户界面偏好表（账号级 UI 配置）
-- preconditions onFail:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_preference'
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_user_preference` (
    `id`          bigint(20)   AUTO_INCREMENT COMMENT 'ID',
    `user_id`     bigint(20)   NOT NULL       COMMENT '用户ID',
    `ui_json`     text         NOT NULL       COMMENT '界面配置JSON',
    `create_time` datetime     NOT NULL       COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL   COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_id`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户界面偏好表';
