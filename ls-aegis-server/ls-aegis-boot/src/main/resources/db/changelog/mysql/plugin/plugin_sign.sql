-- liquibase formatted sql

-- changeset aegis:plugin_sign_schema
-- comment P5：sign_* 业务表 DDL（自 sign/init dump 仅提取 sign_ 表）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sign_backup_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务编码',
  `trigger_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发类型(manual/scheduled)',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态(running/success/failed)',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备份文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行消息',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_id`),
  KEY `idx_status` (`status`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备份任务表';

CREATE TABLE IF NOT EXISTS `sign_backup_schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划名称',
  `cycle` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期(daily/weekly/monthly)',
  `run_time` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '执行时间(HH:MM)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态(0启用 1暂停)',
  `trigger_os` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'auto' COMMENT '触发操作系统(auto/windows/linux)',
  `last_run_at` datetime DEFAULT NULL COMMENT '上次执行时间',
  `next_run_at` datetime DEFAULT NULL COMMENT '下次执行时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备份计划表';

CREATE TABLE IF NOT EXISTS `sign_ca_intermediate` (
  `intermediate_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Intermediate CA ID',
  `root_id` bigint NOT NULL COMMENT 'Root CA ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Intermediate CA name',
  `subject_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Subject DN',
  `issuer_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Issuer DN',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Certificate serial number',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SHA256 fingerprint',
  `not_before` datetime NOT NULL COMMENT 'Validity start time',
  `not_after` datetime NOT NULL COMMENT 'Validity end time',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 active 1 disabled 2 revoked)',
  `cert_pem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Certificate PEM',
  `crl_number` bigint DEFAULT NULL COMMENT 'Latest CRL number',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`intermediate_id`),
  UNIQUE KEY `uk_sign_ca_intermediate_serial_no` (`serial_no`),
  KEY `idx_sign_ca_intermediate_root_id` (`root_id`),
  KEY `idx_sign_ca_intermediate_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Private CA intermediate certificate';

CREATE TABLE IF NOT EXISTS `sign_ca_root` (
  `root_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Root CA ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Root CA name',
  `subject_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Subject DN',
  `issuer_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Issuer DN',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Certificate serial number',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SHA256 fingerprint',
  `not_before` datetime NOT NULL COMMENT 'Validity start time',
  `not_after` datetime NOT NULL COMMENT 'Validity end time',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 active 1 disabled 2 revoked)',
  `cert_pem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Certificate PEM',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`root_id`),
  UNIQUE KEY `uk_sign_ca_root_serial_no` (`serial_no`),
  KEY `idx_sign_ca_root_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Private CA root certificate';

CREATE TABLE IF NOT EXISTS `sign_cert` (
  `cert_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Certificate ID',
  `org_id` bigint DEFAULT NULL COMMENT 'Organization ID',
  `project_id` bigint DEFAULT NULL COMMENT 'Project ID',
  `seal_id` bigint DEFAULT NULL COMMENT 'Seal ID',
  `cert_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Certificate type',
  `subject_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Subject DN',
  `issuer_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Issuer DN',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Certificate serial number',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SHA256 fingerprint',
  `not_before` datetime NOT NULL COMMENT 'Validity start time',
  `not_after` datetime NOT NULL COMMENT 'Validity end time',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 active 1 disabled 2 revoked)',
  `cert_pem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Certificate PEM',
  `chain_pem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Certificate chain PEM',
  `key_ref` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Managed key reference',
  `issued_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Issued time',
  `revoked_at` datetime DEFAULT NULL COMMENT 'Revoked time',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`cert_id`),
  UNIQUE KEY `uk_sign_cert_serial_no` (`serial_no`),
  KEY `idx_sign_cert_seal_id` (`seal_id`),
  KEY `idx_sign_cert_status` (`status`),
  KEY `idx_sign_cert_key_ref` (`key_ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Managed signing certificates';

CREATE TABLE IF NOT EXISTS `sign_cert_request` (
  `request_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Request ID',
  `request_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Request type',
  `org_id` bigint DEFAULT NULL COMMENT 'Organization ID',
  `project_id` bigint DEFAULT NULL COMMENT 'Project ID',
  `seal_id` bigint DEFAULT NULL COMMENT 'Seal ID',
  `subject_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Certificate subject name',
  `subject_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Certificate subject code',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 pending 1 approved 2 rejected 3 issued)',
  `approve_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Approver',
  `approve_time` datetime DEFAULT NULL COMMENT 'Approve time',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Reject reason',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`request_id`),
  KEY `idx_sign_cert_request_status` (`status`),
  KEY `idx_sign_cert_request_seal_id` (`seal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Certificate request form';

CREATE TABLE IF NOT EXISTS `sign_cert_revocation` (
  `revoke_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Revocation ID',
  `cert_id` bigint NOT NULL COMMENT 'Certificate ID',
  `revoke_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Revocation reason',
  `revoke_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Revocation time',
  `operator_id` bigint DEFAULT NULL COMMENT 'Operator ID',
  `crl_no` bigint DEFAULT NULL COMMENT 'CRL number',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`revoke_id`),
  KEY `idx_sign_cert_revocation_cert_id` (`cert_id`),
  KEY `idx_sign_cert_revocation_revoke_time` (`revoke_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Certificate revocation records';

CREATE TABLE IF NOT EXISTS `sign_file` (
  `file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `file_mime` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIME类型',
  `file_sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SHA256哈希',
  `file_ext` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件扩展名',
  `owner_id` bigint DEFAULT NULL COMMENT '上传者ID',
  `biz_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务类型(seal/avatar/backup/sign)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`file_id`),
  UNIQUE KEY `uk_sha256` (`file_sha256`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件管理表';

CREATE TABLE IF NOT EXISTS `sign_key_store` (
  `key_ref` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Managed key reference',
  `storage_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SERVER_ENCRYPTED' COMMENT 'Storage type',
  `encrypted_key_blob` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Encrypted private key blob',
  `encrypt_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Encryption version',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 active 1 disabled)',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Created by',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Updated by',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`key_ref`),
  KEY `idx_sign_key_store_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Managed key storage';

CREATE TABLE IF NOT EXISTS `sign_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `seal_id` bigint NOT NULL COMMENT '印章ID',
  `user_id` bigint NOT NULL COMMENT '操作人ID',
  `file_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件SHA256哈希',
  `client_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `ip_location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP归属地',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `output_file_id` bigint DEFAULT NULL COMMENT '输出文件ID',
  `cert_id` bigint DEFAULT NULL COMMENT '证书ID',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '动作类型',
  `result_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果编码',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '追踪ID',
  `sign_time` datetime NOT NULL COMMENT '签章时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_seal_id` (`seal_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sign_time` (`sign_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签章日志表';

CREATE TABLE IF NOT EXISTS `sign_project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称',
  `project_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目编号',
  `manager_ids` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人用户ID(多个用逗号分隔)',
  `manager_names` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人姓名(多个用逗号分隔)',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `start_date` datetime NOT NULL COMMENT '开始日期',
  `end_date` datetime NOT NULL COMMENT '结束日期',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态(0待审核 1未开始 2进行中 3已完成 4已暂停 5已取消)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '项目描述',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_status` (`status`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_manager_id` (`manager_ids`),
  KEY `idx_date_range` (`start_date`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签章项目表';

CREATE TABLE IF NOT EXISTS `sign_project_seal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `seal_id` bigint NOT NULL COMMENT '印章ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_seal` (`project_id`,`seal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目印章关联表';

CREATE TABLE IF NOT EXISTS `sign_project_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '项目角色',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user` (`project_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员关联表';

CREATE TABLE IF NOT EXISTS `sign_record` (
  `record_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '记录ID(UUID)',
  `file_id` bigint NOT NULL COMMENT '源文件ID',
  `seal_id` bigint NOT NULL COMMENT '印章ID',
  `user_id` bigint NOT NULL COMMENT '签章人用户ID',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `sign_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '签章模式(single/continuous/span/fixed)',
  `positions` json NOT NULL COMMENT '签章位置JSON',
  `has_timestamp` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否包含时间戳(0否 1是)',
  `span_config` json DEFAULT NULL COMMENT '跨页配置JSON',
  `client_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端IP',
  `ip_location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP归属地',
  `os_version` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作系统版本',
  `browser_version` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器版本',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'completed' COMMENT '状态(completed/failed)',
  `signed_file_id` bigint DEFAULT NULL COMMENT '已签章文件ID',
  `cert_id` bigint DEFAULT NULL COMMENT '签章证书ID',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证书序列号',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证书SHA256指纹',
  `verify_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '验签状态',
  `sign_time` datetime DEFAULT NULL COMMENT '签章时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `sign_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签章快照SHA256',
  `page_fingerprint` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '页面位置快照SHA256',
  PRIMARY KEY (`record_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_seal_id` (`seal_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_sign_time` (`sign_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签章记录表';

CREATE TABLE IF NOT EXISTS `sign_seal` (
  `seal_id` bigint NOT NULL AUTO_INCREMENT COMMENT '印章ID',
  `seal_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '印章名称',
  `seal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章编号',
  `seal_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章类型',
  `seal_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用范围',
  `enable_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '启用日期',
  `org_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '机构名称',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `seal_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章图片路径',
  `default_size_mm` int DEFAULT NULL COMMENT '默认尺寸(mm)',
  `size_options` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可选尺寸(mm,逗号分隔)',
  `cert_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证书文件路径',
  `key_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '私钥文件路径',
  `cert_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '证书信息JSON',
  `cert_id` bigint DEFAULT NULL COMMENT '绑定证书ID',
  `key_ref` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '托管密钥引用',
  `cert_bind_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'uploaded' COMMENT '证书绑定模式',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态(0启用 1禁用)',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`seal_id`),
  UNIQUE KEY `uk_seal_code` (`seal_code`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='印章信息表';

CREATE TABLE IF NOT EXISTS `sign_seal_extract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `seal_id` bigint DEFAULT NULL COMMENT '印章ID',
  `original_file_id` bigint DEFAULT NULL COMMENT '原始文件ID',
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始文件名',
  `original_file_size` bigint DEFAULT NULL COMMENT '原始文件大小(字节)',
  `extract_file_id` bigint DEFAULT NULL COMMENT '提取文件ID',
  `extract_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提取文件名',
  `extract_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提取文件路径',
  `extract_file_size` bigint NOT NULL COMMENT '提取文件大小(字节)',
  `thumbnail_file_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缩略图路径',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建用户ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_seal_id` (`seal_id`),
  KEY `idx_create_by` (`create_by`),
  KEY `idx_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='印章提取记录表';

CREATE TABLE IF NOT EXISTS `sign_seal_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `seal_id` bigint NOT NULL COMMENT '印章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seal_user` (`seal_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='印章使用授权表';

CREATE TABLE IF NOT EXISTS `sign_verify_receipt` (
  `receipt_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Receipt ID',
  `record_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Sign record ID',
  `origin_file_id` bigint DEFAULT NULL COMMENT 'Original file ID',
  `signed_file_id` bigint DEFAULT NULL COMMENT 'Signed file ID',
  `verify_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT 'Verify status(0 pending 1 pass 2 fail)',
  `cert_id` bigint DEFAULT NULL COMMENT 'Certificate ID',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Certificate serial number',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SHA256 fingerprint',
  `receipt_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Receipt number',
  `receipt_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Receipt file path',
  `verified_at` datetime DEFAULT NULL COMMENT 'Verify time',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`receipt_id`),
  UNIQUE KEY `uk_sign_verify_receipt_no` (`receipt_no`),
  KEY `idx_sign_verify_receipt_record_id` (`record_id`),
  KEY `idx_sign_verify_receipt_cert_id` (`cert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Verify receipt snapshots';
