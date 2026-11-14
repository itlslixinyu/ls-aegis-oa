-- LS-Aegis MySQL 全新安装初始化脚本（唯一部署 SQL）
-- 由 Liquibase mysql changelog 合并生成：表结构 + 种子 + 插件
-- 导入前请确保库已创建且客户端使用 utf8mb4
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========== main_table.sql ==========

CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id`          bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `title`       varchar(30)  NOT NULL                    COMMENT '标题',
    `parent_id`   bigint(20)   NOT NULL DEFAULT 0          COMMENT '上级菜单ID',
    `type`        tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '类型（1：目录；2：菜单；3：按钮）',
    `path`        varchar(255) DEFAULT NULL                COMMENT '路由地址',
    `name`        varchar(50)  DEFAULT NULL                COMMENT '组件名称',
    `component`   varchar(255) DEFAULT NULL                COMMENT '组件路径',
    `redirect`    varchar(255) DEFAULT NULL                COMMENT '重定向地址',
    `icon`        varchar(50)  DEFAULT NULL                COMMENT '图标',
    `is_external` bit(1)       DEFAULT b'0'                COMMENT '是否外链',
    `is_cache`    bit(1)       DEFAULT b'0'                COMMENT '是否缓存',
    `is_hidden`   bit(1)       DEFAULT b'0'                COMMENT '是否隐藏',
    `permission`  varchar(100) DEFAULT NULL                COMMENT '权限标识',
    `sort`        int          NOT NULL DEFAULT 999        COMMENT '排序',
    `status`      tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `create_user` bigint(20)   NOT NULL                    COMMENT '创建人',
    `create_time` datetime     NOT NULL                    COMMENT '创建时间',
    `update_user` bigint(20)   DEFAULT NULL                COMMENT '修改人',
    `update_time` datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`     bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_title_parent_id`(`title`, `parent_id`, `deleted`),
    INDEX `idx_parent_id`(`parent_id`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id`          bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `name`        varchar(30)  NOT NULL                    COMMENT '名称',
    `parent_id`   bigint(20)   NOT NULL DEFAULT 0          COMMENT '上级部门ID',
    `ancestors`   varchar(512) NOT NULL DEFAULT ''         COMMENT '祖级列表',
    `description` varchar(200) DEFAULT NULL                COMMENT '描述',
    `sort`        int          NOT NULL DEFAULT 999        COMMENT '排序',
    `status`      tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `is_system`   bit(1)       NOT NULL DEFAULT b'0'       COMMENT '是否为系统内置数据',
    `create_user` bigint(20)   NOT NULL                    COMMENT '创建人',
    `create_time` datetime     NOT NULL                    COMMENT '创建时间',
    `update_user` bigint(20)   DEFAULT NULL                COMMENT '修改人',
    `update_time` datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`     bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_name_parent_id`(`name`, `parent_id`, `deleted`),
    INDEX `idx_parent_id`(`parent_id`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`                  bigint(20)   AUTO_INCREMENT        COMMENT 'ID',
    `name`                varchar(30)  NOT NULL              COMMENT '名称',
    `code`                varchar(30)  NOT NULL              COMMENT '编码',
    `data_scope`          tinyint(1)   NOT NULL DEFAULT 4    COMMENT '数据权限（1：全部数据权限；2：本部门及以下数据权限；3：本部门数据权限；4：仅本人数据权限；5：自定义数据权限）',
    `description`         varchar(200) DEFAULT NULL          COMMENT '描述',
    `sort`                int          NOT NULL DEFAULT 999  COMMENT '排序',
    `is_system`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否为系统内置数据',
    `menu_check_strictly` bit(1)       DEFAULT b'1'          COMMENT '菜单选择是否父子节点关联',
    `dept_check_strictly` bit(1)       DEFAULT b'1'          COMMENT '部门选择是否父子节点关联',
    `create_user`         bigint(20)   NOT NULL              COMMENT '创建人',
    `create_time`         datetime     NOT NULL              COMMENT '创建时间',
    `update_user`         bigint(20)   DEFAULT NULL          COMMENT '修改人',
    `update_time`         datetime     DEFAULT NULL          COMMENT '修改时间',
    `deleted`             bigint(20)   NOT NULL DEFAULT 0    COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_name`(`name`, `deleted`),
    UNIQUE INDEX `uk_code`(`code`, `deleted`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`             bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `username`       varchar(64)  NOT NULL                    COMMENT '用户名',
    `nickname`       varchar(30)  NOT NULL                    COMMENT '昵称',
    `password`       varchar(255) DEFAULT NULL                COMMENT '密码',
    `gender`         tinyint(1)   UNSIGNED NOT NULL DEFAULT 0 COMMENT '性别（0：未知；1：男；2：女）',
    `email`          varchar(255) DEFAULT NULL                COMMENT '邮箱',
    `phone`          varchar(255) DEFAULT NULL                COMMENT '手机号码',
    `avatar`         longtext     DEFAULT NULL                COMMENT '头像',
    `description`    varchar(200) DEFAULT NULL                COMMENT '描述',
    `status`         tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `is_system`      bit(1)       NOT NULL DEFAULT b'0'       COMMENT '是否为系统内置数据',
    `pwd_reset_time` datetime     DEFAULT NULL                COMMENT '最后一次修改密码时间',
    `dept_id`        bigint(20)   NOT NULL                    COMMENT '部门ID',
    `create_user`    bigint(20)   DEFAULT NULL                COMMENT '创建人',
    `create_time`    datetime     NOT NULL                    COMMENT '创建时间',
    `update_user`    bigint(20)   DEFAULT NULL                COMMENT '修改人',
    `update_time`    datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`        bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_username`(`username`, `deleted`),
    UNIQUE INDEX `uk_email`(`email`, `deleted`),
    UNIQUE INDEX `uk_phone`(`phone`, `deleted`),
    INDEX `idx_dept_id`(`dept_id`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `sys_user_password_history` (
    `id`          bigint(20)   AUTO_INCREMENT COMMENT 'ID',
    `user_id`     bigint(20)   NOT NULL       COMMENT '用户ID',
    `password`    varchar(255) NOT NULL       COMMENT '密码',
    `create_time` datetime     NOT NULL       COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户历史密码表';

CREATE TABLE IF NOT EXISTS `sys_user_social` (
    `id`              bigint(20)   AUTO_INCREMENT     COMMENT 'ID',
    `source`          varchar(255) NOT NULL           COMMENT '来源',
    `open_id`         varchar(255) NOT NULL           COMMENT '开放ID',
    `user_id`         bigint(20)   NOT NULL           COMMENT '用户ID',
    `meta_json`       text         DEFAULT NULL       COMMENT '附加信息',
    `last_login_time` datetime     DEFAULT NULL       COMMENT '最后登录时间',
    `create_time`     datetime     NOT NULL           COMMENT '创建时间',
    `update_time`     datetime     DEFAULT NULL       COMMENT '修改时间',
    `deleted`         bigint(20)   NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_source_open_id`(`source`, `open_id`, `deleted`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户社会化关联表';

CREATE TABLE IF NOT EXISTS `sys_user_preference` (
    `id`          bigint(20)   AUTO_INCREMENT COMMENT 'ID',
    `user_id`     bigint(20)   NOT NULL       COMMENT '用户ID',
    `ui_json`     text         NOT NULL       COMMENT '界面配置JSON',
    `create_time` datetime     NOT NULL       COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL   COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_id`(`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户界面偏好表';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`      bigint(20) AUTO_INCREMENT COMMENT 'ID',
    `user_id` bigint(20) NOT NULL       COMMENT '用户ID',
    `role_id` bigint(20) NOT NULL       COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_id_role_id`(`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户和角色关联表';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色和菜单关联表';

CREATE TABLE IF NOT EXISTS `sys_role_dept` (
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
    PRIMARY KEY (`role_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色和部门关联表';

CREATE TABLE IF NOT EXISTS `sys_option` (
    `id`            bigint(20)   AUTO_INCREMENT COMMENT 'ID',
    `category`      varchar(50)  NOT NULL       COMMENT '类别',
    `name`          varchar(50)  NOT NULL       COMMENT '名称',
    `code`          varchar(100) NOT NULL       COMMENT '键',
    `value`         longtext     DEFAULT NULL   COMMENT '值',
    `default_value` longtext     DEFAULT NULL   COMMENT '默认值',
    `description`   varchar(200) DEFAULT NULL   COMMENT '描述',
    `update_user`   bigint(20)   DEFAULT NULL   COMMENT '修改人',
    `update_time`   datetime     DEFAULT NULL   COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_category_code`(`category`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数表';

CREATE TABLE IF NOT EXISTS `sys_dict` (
    `id`          bigint(20)   AUTO_INCREMENT        COMMENT 'ID',
    `name`        varchar(30)  NOT NULL              COMMENT '名称',
    `code`        varchar(30)  NOT NULL              COMMENT '编码',
    `description` varchar(200) DEFAULT NULL          COMMENT '描述',
    `is_system`   bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否为系统内置数据',
    `create_user` bigint(20)   NOT NULL              COMMENT '创建人',
    `create_time` datetime     NOT NULL              COMMENT '创建时间',
    `update_user` bigint(20)   DEFAULT NULL          COMMENT '修改人',
    `update_time` datetime     DEFAULT NULL          COMMENT '修改时间',
    `deleted`     bigint(20)   NOT NULL DEFAULT 0    COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_name`(`name`, `deleted`),
    UNIQUE INDEX `uk_code`(`code`, `deleted`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典表';

CREATE TABLE IF NOT EXISTS `sys_dict_item` (
    `id`          bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `label`       varchar(30)  NOT NULL                    COMMENT '标签',
    `value`       varchar(30)  NOT NULL                    COMMENT '值',
    `color`       varchar(30)  DEFAULT NULL                COMMENT '标签颜色',
    `sort`        int          NOT NULL DEFAULT 999        COMMENT '排序',
    `description` varchar(200) DEFAULT NULL                COMMENT '描述',
    `status`      tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `dict_id`     bigint(20)   NOT NULL                    COMMENT '字典ID',
    `create_user` bigint(20)   NOT NULL                    COMMENT '创建人',
    `create_time` datetime     NOT NULL                    COMMENT '创建时间',
    `update_user` bigint(20)   DEFAULT NULL                COMMENT '修改人',
    `update_time` datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`     bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_value_dict_id`(`value`, `dict_id`, `deleted`),
    INDEX `idx_dict_id`(`dict_id`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典项表';

CREATE TABLE IF NOT EXISTS `sys_log` (
    `id`               bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `trace_id`         varchar(255) DEFAULT NULL                COMMENT '链路ID',
    `description`      varchar(255) NOT NULL                    COMMENT '日志描述',
    `module`           varchar(100) NOT NULL                    COMMENT '所属模块',
    `request_url`      varchar(512) NOT NULL                    COMMENT '请求URL',
    `request_method`   varchar(10)  NOT NULL                    COMMENT '请求方式',
    `request_headers`  text         DEFAULT NULL                COMMENT '请求头',
    `request_body`     text         DEFAULT NULL                COMMENT '请求体',
    `status_code`      int          NOT NULL                    COMMENT '状态码',
    `response_headers` text         DEFAULT NULL                COMMENT '响应头',
    `response_body`    mediumtext   DEFAULT NULL                COMMENT '响应体',
    `time_taken`       bigint(20)   NOT NULL                    COMMENT '耗时（ms）',
    `ip`               varchar(100) DEFAULT NULL                COMMENT 'IP',
    `address`          varchar(255) DEFAULT NULL                COMMENT 'IP归属地',
    `browser`          varchar(100) DEFAULT NULL                COMMENT '浏览器',
    `os`               varchar(100) DEFAULT NULL                COMMENT '操作系统',
    `status`           tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：成功；2：失败）',
    `error_msg`        text         DEFAULT NULL                COMMENT '错误信息',
    `create_user`      bigint(20)   DEFAULT NULL                COMMENT '创建人',
    `create_time`      datetime     NOT NULL                    COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_module`(`module`),
    INDEX `idx_ip`(`ip`),
    INDEX `idx_address`(`address`),
    INDEX `idx_create_time`(`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统日志表';

CREATE TABLE IF NOT EXISTS `sys_message` (
    `id`          bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `title`       varchar(50)  NOT NULL                    COMMENT '标题',
    `content`     text         DEFAULT NULL                COMMENT '内容',
    `type`        tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '类型（1：系统消息；2：安全消息）',
    `path`        varchar(255) DEFAULT NULL                COMMENT '跳转路径',
    `scope`       tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '通知范围（1：所有人；2：指定用户）',
    `users`       json         DEFAULT NULL                COMMENT '通知用户',
    `create_time` datetime     NOT NULL                    COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`     bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息表';

CREATE TABLE IF NOT EXISTS `sys_message_log` (
    `message_id` bigint(20) NOT NULL     COMMENT '消息ID',
    `user_id`    bigint(20) NOT NULL     COMMENT '用户ID',
    `read_time`  datetime   DEFAULT NULL COMMENT '读取时间',
    `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '个人收件箱已删除',
    PRIMARY KEY (`message_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息日志表';

CREATE TABLE IF NOT EXISTS `sys_notice` (
    `id`             bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `title`          varchar(150) NOT NULL                    COMMENT '标题',
    `content`        mediumtext   NOT NULL                    COMMENT '内容',
    `type`           varchar(30)  NOT NULL                    COMMENT '分类',
    `notice_scope`   tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '通知范围（1：所有人；2：指定用户）',
    `notice_users`   json         DEFAULT NULL                COMMENT '通知用户',
    `notice_methods` json         DEFAULT NULL                COMMENT '通知方式（1：系统消息；2：登录弹窗）',
    `is_timing`      bit(1)       NOT NULL DEFAULT b'0'       COMMENT '是否定时',
    `publish_time`   datetime     DEFAULT NULL                COMMENT '发布时间',
    `is_top`         bit(1)       NOT NULL DEFAULT b'0'       COMMENT '是否置顶',
    `status`         tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：草稿；2：待发布；3：已发布）',
    `create_user`    bigint(20)   NOT NULL                    COMMENT '创建人',
    `create_time`    datetime     NOT NULL                    COMMENT '创建时间',
    `update_user`    bigint(20)   DEFAULT NULL                COMMENT '修改人',
    `update_time`    datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`        bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告表';

CREATE TABLE IF NOT EXISTS `sys_notice_log` (
    `notice_id` bigint(20) NOT NULL     COMMENT '公告ID',
    `user_id`   bigint(20) NOT NULL     COMMENT '用户ID',
    `read_time` datetime   DEFAULT NULL COMMENT '读取时间',
    PRIMARY KEY (`notice_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告日志表';

CREATE TABLE IF NOT EXISTS `sys_storage` (
    `id`                  bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `name`                varchar(100) NOT NULL                    COMMENT '名称',
    `code`                varchar(30)  NOT NULL                    COMMENT '编码',
    `type`                tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '类型（1：本地存储；2：对象存储）',
    `access_key`          varchar(255) DEFAULT NULL                COMMENT 'Access Key',
    `secret_key`          varchar(255) DEFAULT NULL                COMMENT 'Secret Key',
    `endpoint`            varchar(255) DEFAULT NULL                COMMENT 'Endpoint',
    `bucket_name`         varchar(255) NOT NULL                    COMMENT 'Bucket',
    `domain`              varchar(255) DEFAULT NULL                COMMENT '域名',
    `recycle_bin_enabled` bit(1)       NOT NULL DEFAULT b'1'       COMMENT '启用回收站',
    `recycle_bin_path`    varchar(255) DEFAULT NULL                COMMENT '回收站路径',
    `description`         varchar(200) DEFAULT NULL                COMMENT '描述',
    `is_default`          bit(1)       NOT NULL DEFAULT b'0'       COMMENT '是否为默认存储',
    `sort`                int          NOT NULL DEFAULT 999        COMMENT '排序',
    `status`              tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `create_user`         bigint(20)   NOT NULL                    COMMENT '创建人',
    `create_time`         datetime     NOT NULL                    COMMENT '创建时间',
    `update_user`         bigint(20)   DEFAULT NULL                COMMENT '修改人',
    `update_time`         datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`             bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_code`(`code`, `deleted`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='存储表';

CREATE TABLE IF NOT EXISTS `sys_file` (
    `id`                 bigint(20)    AUTO_INCREMENT              COMMENT 'ID',
    `name`               varchar(255)  NOT NULL                    COMMENT '名称',
    `original_name`      varchar(255)  NOT NULL                    COMMENT '原始名称',
    `size`               bigint(20)    DEFAULT NULL                COMMENT '大小（字节）',
    `parent_path`        varchar(512)  NOT NULL DEFAULT '/'        COMMENT '上级目录',
    `path`               varchar(512)  NOT NULL                    COMMENT '路径',
    `extension`          varchar(32)   DEFAULT NULL                COMMENT '扩展名',
    `content_type`       varchar(255)  DEFAULT NULL                COMMENT '内容类型',
    `type`               tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '类型（0: 目录；1：其他；2：图片；3：文档；4：视频；5：音频）',
    `file_digest`        varchar(256)  DEFAULT NULL                COMMENT '文件指纹（统一 SM3）',
    `metadata`           text          DEFAULT NULL                COMMENT '元数据',
    `thumbnail_name`     varchar(255)  DEFAULT NULL                COMMENT '缩略图名称',
    `thumbnail_size`     bigint(20)    DEFAULT NULL                COMMENT '缩略图大小（字节)',
    `thumbnail_metadata` text          DEFAULT NULL                COMMENT '缩略图元数据',
    `storage_id`         bigint(20)    NOT NULL                    COMMENT '存储ID',
    `create_user`        bigint(20)    NOT NULL                    COMMENT '创建人',
    `create_time`        datetime      NOT NULL                    COMMENT '创建时间',
    `update_user`        bigint(20)    DEFAULT NULL                COMMENT '修改人',
    `update_time`        datetime      DEFAULT NULL                COMMENT '修改时间',
    `deleted`            bigint(20)    NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_type`(`type`),
    INDEX `idx_file_digest`(`file_digest`),
    INDEX `idx_storage_id`(`storage_id`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件表';

CREATE TABLE IF NOT EXISTS `sys_client` (
    `id`             bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `client_id`      varchar(50)  NOT NULL                    COMMENT '客户端ID',
    `client_type`    varchar(50)  NOT NULL                    COMMENT '客户端类型',
    `auth_type`      json         NOT NULL                    COMMENT '认证类型',
    `active_timeout` bigint(20)   DEFAULT -1                  COMMENT 'Token最低活跃频率（单位：秒，-1：不限制，永不冻结）',
    `timeout`        bigint(20)   DEFAULT 2592000             COMMENT 'Token有效期（单位：秒，-1：永不过期）',
    `status`         tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `create_user`    bigint(20)   NOT NULL                    COMMENT '创建人',
    `create_time`    datetime     NOT NULL                    COMMENT '创建时间',
    `update_user`    bigint(20)   DEFAULT NULL                COMMENT '修改人',
    `update_time`    datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`        bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_client_id`(`client_id`, `deleted`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户端表';

-- ========== main_data.sql ==========

-- validCheckSum: ANY
-- 初始化默认菜单
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(1000, '系统管理', 0, 1, '/system', 'System', 'Layout', '/system/user', 'settings', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()),
(1010, '用户管理', 1000, 2, '/system/user', 'SystemUser', 'system/user/index', NULL, 'user', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()),
(1011, '列表', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:list', 1, 1, 1, NOW()),
(1012, '详情', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:get', 2, 1, 1, NOW()),
(1013, '新增', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:create', 3, 1, 1, NOW()),
(1014, '修改', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:update', 4, 1, 1, NOW()),
(1015, '删除', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:delete', 5, 1, 1, NOW()),
(1016, '导出', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:export', 6, 1, 1, NOW()),
(1017, '导入', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:import', 7, 1, 1, NOW()),
(1018, '重置密码', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:resetPwd', 8, 1, 1, NOW()),
(1019, '分配角色', 1010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:user:updateRole', 9, 1, 1, NOW()),

(1030, '角色管理', 1000, 2, '/system/role', 'SystemRole', 'system/role/index', NULL, 'user-management', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()),
(1031, '列表', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:list', 1, 1, 1, NOW()),
(1032, '详情', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:get', 2, 1, 1, NOW()),
(1033, '新增', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:create', 3, 1, 1, NOW()),
(1034, '修改', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:update', 4, 1, 1, NOW()),
(1035, '删除', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:delete', 5, 1, 1, NOW()),
(1036, '修改权限', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:updatePermission', 6, 1, 1, NOW()),
(1037, '分配', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:assign', 7, 1, 1, NOW()),
(1038, '取消分配', 1030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:role:unassign', 8, 1, 1, NOW()),

(1050, '菜单管理', 1000, 2, '/system/menu', 'SystemMenu', 'system/menu/index', NULL, 'menu', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()),
(1051, '列表', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:list', 1, 1, 1, NOW()),
(1052, '详情', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:get', 2, 1, 1, NOW()),
(1053, '新增', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:create', 3, 1, 1, NOW()),
(1054, '修改', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:update', 4, 1, 1, NOW()),
(1055, '删除', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:delete', 5, 1, 1, NOW()),
(1056, '清除缓存', 1050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:menu:clearCache', 6, 1, 1, NOW()),

(1070, '部门管理', 1000, 2, '/system/dept', 'SystemDept', 'system/dept/index', NULL, 'mind-mapping', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()),
(1071, '列表', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:list', 1, 1, 1, NOW()),
(1072, '详情', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:get', 2, 1, 1, NOW()),
(1073, '新增', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:create', 3, 1, 1, NOW()),
(1074, '修改', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:update', 4, 1, 1, NOW()),
(1075, '删除', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:delete', 5, 1, 1, NOW()),
(1076, '导出', 1070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dept:export', 6, 1, 1, NOW()),

(1090, '通知公告', 1000, 2, '/system/notice', 'SystemNotice', 'system/notice/index', NULL, 'notification', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()),
(1091, '列表', 1090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:notice:list', 1, 1, 1, NOW()),
(1092, '详情', 1090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:notice:get', 2, 1, 1, NOW()),
(1093, '查看公告', 1090, 2, '/system/notice/view', 'SystemNoticeView', 'system/notice/view/index', NULL, NULL, b'0', b'0', b'1', 'system:notice:view', 3, 1, 1, NOW()),
(1094, '发布公告', 1090, 2, '/system/notice/add', 'SystemNoticeAdd', 'system/notice/add/index', NULL, NULL, b'0', b'0', b'1', 'system:notice:create', 4, 1, 1, NOW()),
(1095, '修改', 1090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:notice:update', 5, 1, 1, NOW()),
(1096, '删除', 1090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:notice:delete', 6, 1, 1, NOW()),

(1110, '文件管理', 1000, 2, '/system/file', 'SystemFile', 'system/file/index', NULL, 'file', b'0', b'0', b'0', NULL, 6, 1, 1, NOW()),
(1111, '列表', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:list', 1, 1, 1, NOW()),
(1112, '详情', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:get', 2, 1, 1, NOW()),
(1113, '上传', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:upload', 3, 1, 1, NOW()),
(1114, '修改', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:update', 4, 1, 1, NOW()),
(1115, '删除', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:delete', 5, 1, 1, NOW()),
(1116, '下载', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:download', 6, 1, 1, NOW()),
(1117, '创建文件夹', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:createDir', 7, 1, 1, NOW()),
(1118, '计算文件夹大小', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:calcDirSize', 8, 1, 1, NOW()),
(1119, '回收站文件列表', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:fileRecycle:list', 9, 1, 1, NOW()),
(1120, '还原回收站文件', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:fileRecycle:restore', 10, 1, 1, NOW()),
(1121, '删除回收站文件', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:fileRecycle:delete', 11, 1, 1, NOW()),
(1122, '清空回收站', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:fileRecycle:clean', 12, 1, 1, NOW()),
(1123, '校验文件', 1110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:file:check', 13, 1, 1, NOW()),

(1130, '字典管理', 1000, 2, '/system/dict', 'SystemDict', 'system/dict/index', NULL, 'bookmark', b'0', b'0', b'0', NULL, 7, 1, 1, NOW()),
(1131, '列表', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:list', 1, 1, 1, NOW()),
(1132, '详情', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:get', 2, 1, 1, NOW()),
(1133, '新增', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:create', 3, 1, 1, NOW()),
(1134, '修改', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:update', 4, 1, 1, NOW()),
(1135, '删除', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:delete', 5, 1, 1, NOW()),
(1136, '清除缓存', 1130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dict:clearCache', 6, 1, 1, NOW()),
(1140, '字典项管理', 1000, 2, '/system/dict/item', 'SystemDictItem', 'system/dict/index', NULL, 'bookmark', b'0', b'0', b'1', NULL, 8, 1, 1, NOW()),
(1141, '列表', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:list', 1, 1, 1, NOW()),
(1142, '详情', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:get', 2, 1, 1, NOW()),
(1143, '新增', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:create', 3, 1, 1, NOW()),
(1144, '修改', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:update', 4, 1, 1, NOW()),
(1145, '删除', 1140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dictItem:delete', 5, 1, 1, NOW()),

(1150, '系统配置', 1000, 2, '/system/config', 'SystemConfig', 'system/config/index', NULL, 'config', b'0', b'0', b'0', NULL, 999, 1, 1, NOW()),
(1160, '网站配置', 1150, 2, '/system/config?tab=site', 'SystemSiteConfig', 'system/config/site/index', NULL, 'apps', b'0', b'0', b'1', NULL, 1, 1, 1, NOW()),
(1161, '查询', 1160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:siteConfig:get', 1, 1, 1, NOW()),
(1162, '修改', 1160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:siteConfig:update', 2, 1, 1, NOW()),
(1170, '安全配置', 1150, 2, '/system/config?tab=security', 'SystemSecurityConfig', 'system/config/security/index', NULL, 'safe', b'0', b'0', b'1', NULL, 2, 1, 1, NOW()),
(1171, '查询', 1170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:securityConfig:get', 1, 1, 1, NOW()),
(1172, '修改', 1170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:securityConfig:update', 2, 1, 1, NOW()),
(1180, '登录配置', 1150, 2, '/system/config?tab=login', 'SystemLoginConfig', 'system/config/login/index', NULL, 'lock', b'0', b'0', b'1', NULL, 3, 1, 1, NOW()),
(1181, '查询', 1180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:loginConfig:get', 1, 1, 1, NOW()),
(1182, '修改', 1180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:loginConfig:update', 2, 1, 1, NOW()),
(1190, '邮件配置', 1150, 2, '/system/config?tab=mail', 'SystemMailConfig', 'system/config/mail/index', NULL, 'email', b'0', b'0', b'1', NULL, 4, 1, 1, NOW()),
(1191, '查询', 1190, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:mailConfig:get', 1, 1, 1, NOW()),
(1192, '修改', 1190, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:mailConfig:update', 2, 1, 1, NOW()),
(1200, '天气配置', 1150, 2, '/system/config?tab=weather', 'SystemWeatherConfig', 'system/config/weather/index', NULL, 'cloud', b'0', b'0', b'1', NULL, 5, 1, 1, NOW()),
(1201, '查询', 1200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:weatherConfig:get', 1, 1, 1, NOW()),
(1202, '修改', 1200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:weatherConfig:update', 2, 1, 1, NOW()),
(1210, '大屏配置', 1150, 2, '/system/config?tab=dashboard', 'SystemDashboardConfig', 'system/config/dashboard/index', NULL, 'dashboard', b'0', b'0', b'1', NULL, 6, 1, 1, NOW()),
(1211, '查询', 1210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dashboardConfig:get', 1, 1, 1, NOW()),
(1212, '修改', 1210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:dashboardConfig:update', 2, 1, 1, NOW()),
(1220, '功能模块', 1150, 2, '/system/config?tab=module', 'SystemModuleConfig', 'system/config/module/index', NULL, 'apps', b'0', b'0', b'1', NULL, 7, 1, 1, NOW()),
(1221, '查询', 1220, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:moduleConfig:get', 1, 1, 1, NOW()),
(1222, '修改', 1220, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:moduleConfig:update', 2, 1, 1, NOW()),
(1230, '存储配置', 1150, 2, '/system/config?tab=storage', 'SystemStorage', 'system/config/storage/index', NULL, 'storage', b'0', b'0', b'1', NULL, 8, 1, 1, NOW()),
(1231, '列表', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:list', 1, 1, 1, NOW()),
(1232, '详情', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:get', 2, 1, 1, NOW()),
(1233, '新增', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:create', 3, 1, 1, NOW()),
(1234, '修改', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:update', 4, 1, 1, NOW()),
(1235, '删除', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:delete', 5, 1, 1, NOW()),
(1236, '修改状态', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:updateStatus', 6, 1, 1, NOW()),
(1237, '设为默认存储', 1230, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:storage:setDefault', 7, 1, 1, NOW()),
(1250, '客户端配置', 1150, 2, '/system/config?tab=client', 'SystemClient', 'system/config/client/index', NULL, 'mobile', b'0', b'0', b'1', NULL, 9, 1, 1, NOW()),
(1251, '列表', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:list', 1, 1, 1, NOW()),
(1252, '详情', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:get', 2, 1, 1, NOW()),
(1253, '新增', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:create', 3, 1, 1, NOW()),
(1254, '修改', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:update', 4, 1, 1, NOW()),
(1255, '删除', 1250, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:client:delete', 5, 1, 1, NOW()),
(1260, '运维报修', 1150, 2, '/system/config?tab=eoms', 'SystemEomsConfig', 'system/config/eoms/index', NULL, 'tool', b'0', b'0', b'1', NULL, 10, 1, 1, NOW()),
(1261, '查询', 1260, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:eomsConfig:get', 1, 1, 1, NOW()),
(1262, '修改', 1260, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:eomsConfig:update', 2, 1, 1, NOW()),
(1270, 'H5客户授权', 1150, 2, '/system/config?tab=h5Auth', 'SystemH5AuthConfig', 'system/config/h5-auth/index', NULL, 'mobile', b'0', b'0', b'1', NULL, 11, 1, 1, NOW()),
(1271, '查询配置', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:get', 1, 1, 1, NOW()),
(1272, '修改配置', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:update', 2, 1, 1, NOW()),
(1273, '在线列表', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:list', 3, 1, 1, NOW()),
(1274, '强退令牌', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:kickout', 4, 1, 1, NOW()),

(2000, '系统监控', 0, 1, '/monitor', 'Monitor', 'Layout', '/monitor/online', 'computer', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()),
(2010, '在线用户', 2000, 2, '/monitor/online', 'MonitorOnline', 'monitor/online/index', NULL, 'user', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()),
(2011, '列表', 2010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:online:list', 1, 1, 1, NOW()),
(2012, '强退', 2010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:online:kickout', 2, 1, 1, NOW()),

(2030, '系统日志', 2000, 2, '/monitor/log', 'MonitorLog', 'monitor/log/index', NULL, 'history', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()),
(2031, '列表', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:log:list', 1, 1, 1, NOW()),
(2032, '详情', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:log:get', 2, 1, 1, NOW()),
(2033, '导出', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'monitor:log:export', 3, 1, 1, NOW()),
(2034, '签章审计', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sign:audit:list', 4, 1, 1, NOW());

-- 初始化默认部门（一级总公司 / 二级总经办）
INSERT INTO `sys_dept`
(`id`, `name`, `parent_id`, `ancestors`, `description`, `sort`, `status`, `is_system`, `create_user`, `create_time`)
VALUES
(1, '总公司', 0, '0', '一级部门', 1, 1, b'1', 1, NOW());

-- 初始化默认角色
INSERT INTO `sys_role`
(`id`, `name`, `code`, `data_scope`, `description`, `sort`, `is_system`, `create_user`, `create_time`)
VALUES
(1, '超级管理员', 'super_admin', 1, '系统初始角色', 0, b'1', 1, NOW()),
(2, '系统管理员', 'sys_admin', 1, NULL, 1, b'0', 1, NOW()),
(3, '普通角色', 'general', 4, NULL, 2, b'0', 1, NOW()),
(11, 'EOMS 主管', 'eoms_manager', 2, '运维报修主管：本部门及下级数据', 11, b'0', 1, NOW()),
(12, 'EOMS 工程师', 'eoms_engineer', 4, '运维工程师：仅本人创建或派给自己的工单', 12, b'0', 1, NOW()),
(13, 'EOMS 财务', 'eoms_finance', 3, '运维财务：本部门结算/应收/收款', 13, b'0', 1, NOW());

-- 初始化默认用户（口令哈希 SM3；明文口令禁止写入仓库；pwd_reset_time 置旧以配合密码有效期强制首登改密）
INSERT INTO `sys_user`
(`id`, `username`, `nickname`, `password`, `gender`, `email`, `phone`, `avatar`, `description`, `status`, `is_system`, `pwd_reset_time`, `dept_id`, `create_user`, `create_time`)
VALUES
(1, 'admin', '超级管理员', '{sm3}v1$ef4114fbfe4881f4710b626429c97c04$bd43c9a531b4cc2705aaff6d1a2f9563906c254cc3ffe05fbb21c2456bd86d99', 1, NULL, NULL, NULL, '系统初始用户', 1, b'1', '2000-01-01 00:00:00', 1, 1, NOW());

-- 初始化默认参数
INSERT INTO `sys_option`
(`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
VALUES
(1, 'SITE', '系统名称', 'SITE_TITLE', NULL, 'LS·Aegis 雷铄 OA 安全应用构建平台', '显示在浏览器标题栏和登录页顶栏的产品/项目名称'),
(2, 'SITE', '系统描述', 'SITE_DESCRIPTION', NULL, 'LS·Aegis 雷铄 OA 安全应用构建平台', '用于 SEO 的网站元描述'),
(3, 'SITE', '版权声明', 'SITE_COPYRIGHT', NULL, 'LS·Aegis 雷铄 OA 安全应用构建平台', '显示在页面底部的版权声明文本'),
(4, 'SITE', 'ICP备案号', 'SITE_BEIAN', NULL, '冀ICP备 00000000 号', '工信部 ICP 备案编号（如：冀ICP备00000000号）'),
(5, 'SITE', '系统图标', 'SITE_FAVICON', NULL, '/favicon.png', '浏览器标签页显示的网站图标（建议 .ico 格式）'),
(6, 'SITE', '系统LOGO', 'SITE_LOGO', NULL, '/brand-logo.png', '显示在登录页面和系统导航栏的网站图标（建议 .svg 格式）'),
(7, 'SITE', '公司名称', 'SITE_COMPANY', NULL, '雷铄科技', '显示在登录页顶栏的公司名称'),
(8, 'SITE', '公安备案号', 'SITE_BEIAN_GONGAN', NULL, '冀公网安备 00000000000000 号', '显示在登录页底部的公安备案编号'),
(9, 'SITE', '公安备案图标', 'SITE_BEIAN_GONGAN_ICON', NULL, '/beian-gongan.png', '公安备案号左侧图标（建议小尺寸 PNG）'),
(10, 'PASSWORD', '密码错误锁定阈值', 'PASSWORD_ERROR_LOCK_COUNT', NULL, '5', '连续登录失败次数达到该值将锁定账号（0-10次，0表示禁用锁定）'),
(11, 'PASSWORD', '账号锁定时长（分钟）', 'PASSWORD_ERROR_LOCK_MINUTES', NULL, '5', '账号锁定后自动解锁的时间（1-1440分钟，即24小时）'),
(12, 'PASSWORD', '密码有效期（天）', 'PASSWORD_EXPIRATION_DAYS', NULL, '90', '密码强制修改周期（0-999天，0表示永不过期；默认90天，种子账号首登须改密）'),
(13, 'PASSWORD', '密码到期提醒（天）', 'PASSWORD_EXPIRATION_WARNING_DAYS', NULL, '0', '密码过期前的提前提醒天数（0表示不提醒）'),
(14, 'PASSWORD', '历史密码重复校验次数', 'PASSWORD_REPETITION_TIMES', NULL, '3', '禁止使用最近 N 次的历史密码（3-32次）'),
(15, 'PASSWORD', '密码最小长度', 'PASSWORD_MIN_LENGTH', NULL, '8', '密码最小字符长度要求（8-32个字符）'),
(16, 'PASSWORD', '是否允许密码包含用户名', 'PASSWORD_ALLOW_CONTAIN_USERNAME', NULL, '1', '是否允许密码包含正序或倒序的用户名字符'),
(17, 'PASSWORD', '密码是否必须包含特殊字符', 'PASSWORD_REQUIRE_SYMBOLS', NULL, '0', '是否要求密码必须包含特殊字符（如：!@#$%）'),
(18, 'SITE', '系统名称简称', 'SITE_TITLE_SHORT', NULL, 'ls·aegis·oa', '显示在左侧栏 Logo 旁的系统简称（必填，最多 32 个字符）'),
(19, 'SITE', '公司简称', 'SITE_COMPANY_SHORT', NULL, '雷铄', '公司名称简称（必填，最多 8 个字）'),
(20, 'MAIL', '邮件协议', 'MAIL_PROTOCOL', NULL, 'smtp', '邮件发送协议类型'),
(21, 'MAIL', '服务器地址', 'MAIL_HOST', NULL, NULL, '邮件服务器地址'),
(22, 'MAIL', '服务器端口', 'MAIL_PORT', NULL, '465', '邮件服务器连接端口'),
(23, 'MAIL', '邮箱账号', 'MAIL_USERNAME', NULL, 'support@ls-aegis.local', '发件人邮箱地址'),
(24, 'MAIL', '邮箱密码', 'MAIL_PASSWORD', NULL, NULL, '服务授权密码/客户端专用密码'),
(25, 'MAIL', '启用SSL加密', 'MAIL_SSL_ENABLED', NULL, '1', '是否启用SSL/TLS加密连接'),
(26, 'MAIL', 'SSL端口号', 'MAIL_SSL_PORT', NULL, '465', 'SSL加密连接的备用端口（通常与主端口一致）'),
(27, 'LOGIN', '是否启用验证码', 'LOGIN_CAPTCHA_ENABLED', NULL, '1', NULL),
(28, 'WEATHER', '是否启用', 'WEATHER_ENABLED', NULL, '1', '控制运营中枢顶栏是否展示天气信息。'),
(29, 'WEATHER', '默认城市', 'WEATHER_CITY', NULL, '北京', '固定城市模式下使用；定位未授权或解析失败时作为展示城市。'),
(30, 'WEATHER', '刷新时间间隔（秒）', 'WEATHER_REFRESH_INTERVAL', NULL, '600', '天气信息自动刷新周期，单位秒，建议 60～86400。'),
(31, 'WEATHER', '数据来源', 'WEATHER_PROVIDER', NULL, 'mock', '本地模拟仅用于联调；和风天气支持 API Key 或 JWT，须自行申请凭据并遵守协议与配额，异常时回退本地模拟。'),
(32, 'WEATHER', '城市模式', 'WEATHER_CITY_MODE', NULL, 'auto', '「和风 GeoAPI 定位」在用户授权后使用浏览器位置解析城市；「固定城市」仅使用默认城市。位置信息仅用于天气展示。'),
(33, 'WEATHER', '和风 API Host', 'WEATHER_API_HOST', NULL, '', '填写控制台「设置」中的专属 API 访问地址，请以控制台实际值为准。'),
(34, 'WEATHER', '和风 GeoAPI 地址', 'WEATHER_GEO_HOST', NULL, '', '填写服务商控制台提供的城市查询地址，须与当前账号权限及官方文档一致。'),
(35, 'WEATHER', '和风 API Key', 'WEATHER_API_KEY', NULL, '', '控制台创建「API KEY」类型凭据后的 Key；无 JWT 私钥时填此项即可。仅服务端保存，不下发前端。'),
(36, 'WEATHER', '和风凭据 ID（kid）', 'WEATHER_JWT_KID', NULL, '', '填写和风控制台 JWT 凭据 ID（kid）；仅用 API Key 时可留空。'),
(37, 'WEATHER', '和风项目 ID（sub）', 'WEATHER_JWT_PROJECT_ID', NULL, '', '填写和风控制台项目 ID；仅用 API Key 时可留空。'),
(38, 'WEATHER', '和风 JWT 私钥', 'WEATHER_JWT_PRIVATE_KEY', NULL, '', 'JWT Ed25519 私钥 PEM；仅用 API Key 时可留空。私钥仅服务端保存。'),
(39, 'DASHBOARD', '大屏名称', 'DASHBOARD_TITLE', NULL, 'LS-Aegis 雷铄御警安全应用运营中枢', '运营中枢顶栏主标题'),
(40, 'DASHBOARD', '显示时钟', 'DASHBOARD_SHOW_CLOCK', NULL, '1', '是否在顶栏右侧显示日期时间'),
(41, 'DASHBOARD', '显示运行状态', 'DASHBOARD_SHOW_STATUS', NULL, '1', '是否在工具栏显示系统运行状态指示灯'),
(42, 'DASHBOARD', '显示指标概览', 'DASHBOARD_SHOW_KPI', NULL, '1', '是否显示顶部 KPI 指标卡片'),
(43, 'DASHBOARD', '显示公告', 'DASHBOARD_SHOW_NOTICE', NULL, '1', '是否显示底部最新公告面板'),
(44, 'DASHBOARD', '全屏入口提示', 'DASHBOARD_SHOW_FS_TIP', NULL, '1', '进入大屏时是否短暂提示右下角全屏入口'),
(45, 'DASHBOARD', '显示装饰动效', 'DASHBOARD_SHOW_DECOR', NULL, '1', '是否显示边框装饰与扫描线动效'),
(46, 'DASHBOARD', '默认统计周期', 'DASHBOARD_DEFAULT_DAYS', NULL, '7', '进入大屏时默认统计周期：7 或 30（天）'),
(47, 'DASHBOARD', '数据刷新间隔（秒）', 'DASHBOARD_REFRESH_INTERVAL', NULL, '60', '仪表盘数据自动刷新间隔，单位秒；0 表示不自动刷新'),
(48, 'MODULE', '租户管理', 'MODULE_TENANT_ENABLED', NULL, '0', '默认关闭。开启后显示租户管理菜单与登录租户编码框；关闭则隐藏菜单、拦截相关接口。完整启用还须 CONTINEW_STARTER_TENANT_ENABLED=true。'),
(50, 'MODULE', '任务调度', 'MODULE_SCHEDULE_ENABLED', NULL, '0', '默认关闭。开启后显示任务调度侧栏菜单；关闭则隐藏菜单并拦截相关接口。实际执行任务仍须 snail-job.enabled=true 并启动调度中心。'),
(51, 'EOMS', '客户编号前缀', 'CUSTOMER_NO_PREFIX', NULL, 'KH', '客户档案自动编号前缀（2～4 位大写字母）'),
(52, 'EOMS', '报修编号前缀', 'REPAIR_NO_PREFIX', NULL, 'BX', '报修申请自动编号前缀'),
(53, 'EOMS', '工单编号前缀', 'ORDER_NO_PREFIX', NULL, 'GD', '维修工单自动编号前缀'),
(54, 'EOMS', '报价编号前缀', 'QUOTE_NO_PREFIX', NULL, 'BJ', '报价单自动编号前缀'),
(55, 'EOMS', '合同编号前缀', 'CONTRACT_NO_PREFIX', NULL, 'HTWX', '维修合同自动编号前缀'),
(56, 'EOMS', '采购编号前缀', 'PURCHASE_NO_PREFIX', NULL, 'CG', '采购单自动编号前缀'),
(57, 'EOMS', '收入编号前缀', 'INCOME_NO_PREFIX', NULL, 'SR', '收入单自动编号前缀'),
(58, 'EOMS', '收款编号前缀', 'PAYMENT_NO_PREFIX', NULL, 'SK', '收款单自动编号前缀'),
(59, 'EOMS', '应收编号前缀', 'RECEIVABLE_NO_PREFIX', NULL, 'YS', '应收单自动编号前缀'),
(60, 'EOMS', '物料编号前缀', 'MATERIAL_NO_PREFIX', NULL, 'WL', '物料档案自动编号前缀（分类层级编码另算）'),
(61, 'EOMS', '物料分类编号前缀', 'CATEGORY_NO_PREFIX', NULL, 'WL', '物料分类自动编号前缀'),
(62, 'EOMS', '结算编号前缀', 'SETTLEMENT_NO_PREFIX', NULL, 'JS', '维修结算单自动编号前缀'),
(63, 'EOMS', '仓库编号前缀', 'WAREHOUSE_NO_PREFIX', NULL, 'CK', '仓库档案自动编号前缀'),
(64, 'EOMS', '供应商编号前缀', 'SUPPLIER_NO_PREFIX', NULL, 'GYS', '供应商档案自动编号前缀'),
(65, 'EOMS', '入库编号前缀', 'INBOUND_NO_PREFIX', NULL, 'RK', '入库单自动编号前缀'),
(66, 'EOMS', '百度地图 AK', 'GEO_BAIDU_AK', NULL, NULL, '逆地理编码 Web 服务 AK；未填时现场定位降级为坐标字符串'),
(67, 'EOMS', '百度逆地理编码地址', 'GEO_BAIDU_REGEO_URL', NULL, 'https://api.map.baidu.com/reverse_geocoding/v3/', '可换成内网代理域名；须与申请 AK 的服务端类型匹配'),
(68, 'EOMS', '水印图最长边（像素）', 'WATERMARK_MAX_SIDE', NULL, '1280', '现场照烧录水印前缩放上限，建议 640～4096'),
(69, 'EOMS', 'H5令牌有效期（分钟）', 'H5_TOKEN_EXPIRE_MINUTES', NULL, '120', '客户 H5 身份令牌 Redis TTL，建议 30～480；国密环境下令牌由 SM3 生成'),
(70, 'EOMS', 'H5令牌活跃续期', 'H5_TOKEN_AUTO_REFRESH', NULL, '1', '1=每次请求续期；0=固定 TTL 到期失效');

-- 初始化默认字典
INSERT INTO `sys_dict`
(`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
VALUES
(1, '公告分类', 'notice_type', NULL, b'1', 1, NOW()),
(2, '客户端类型', 'client_type', NULL, b'1', 1, NOW());

INSERT INTO `sys_dict_item`
(`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
VALUES
(1, '通知公告', '1', 'primary', 1, '日常行政通知、会议通知、放假调休、制度宣贯', 1, 1, 1, NOW()),
(2, '政策文件', '2', 'primary', 2, '新规发布、政策解读、上级下发文件', 1, 1, 1, NOW()),
(6, '新闻动态', '3', 'default', 3, '公司动态、项目进展、行业资讯', 1, 1, 1, NOW()),
(7, '人事公告', '4', 'success', 4, '任免、招聘、入职离职、考勤、绩效考核公示', 1, 1, 1, NOW()),
(8, '财务公示', '5', 'warning', 5, '预算、采购招标、中标结果、费用公示', 1, 1, 1, NOW()),
(9, '安全通告', '6', 'error', 6, '网络安全预警、系统维护、漏洞提示、版本升级', 1, 1, 1, NOW()),
(10, '运维公告', '7', 'warning', 7, '服务器停机、系统更新、功能变更、接口下线', 1, 1, 1, NOW()),
(11, '公示通告', '8', 'default', 8, '结果公示、评选、申诉、公开征求意见', 1, 1, 1, NOW()),
(12, '紧急通告', '9', 'error', 9, '突发事件、应急通知、重要紧急事项', 1, 1, 1, NOW()),
(3, '桌面端', 'PC', 'primary', 1, NULL, 1, 2, 1, NOW()),
(4, '安卓', 'ANDROID', 'success', 2, NULL, 1, 2, 1, NOW()),
(5, '小程序', 'XCX', 'warning', 3, NULL, 1, 2, 1, NOW());

-- 初始化默认用户和角色关联数据
INSERT INTO `sys_user_role`
(`id`, `user_id`, `role_id`)
VALUES
(1, 1, 1);

-- 初始化默认存储
INSERT INTO `sys_storage`
(`id`, `name`, `code`, `type`, `access_key`, `secret_key`, `endpoint`, `bucket_name`, `domain`, `recycle_bin_enabled`, `recycle_bin_path`, `description`, `is_default`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(1, '开发环境', 'local_dev', 1, NULL, NULL, NULL, 'data/file/', '/api/file/', b'1', '.RECYCLE.BIN/', '本地存储', b'1', 1, 1, 1, NOW()),
(2, '生产环境', 'local_prod', 1, NULL, NULL, NULL, 'data/file/', '/api/file/', b'1', '.RECYCLE.BIN/', '本地存储', b'0', 2, 2, 1, NOW());

-- 初始化客户端数据
INSERT INTO `sys_client`
(`id`, `client_id`, `client_type`, `auth_type`, `active_timeout`, `timeout`, `status`, `create_user`, `create_time`)
VALUES
(1, 'ef51c9a3e9046c4f2ea45142c8a8344a', 'PC', '["ACCOUNT", "EMAIL"]', 1800, 86400, 1, 1, NOW());

-- ========== plugin\plugin_tenant.sql ==========

-- 初始化表结构
CREATE TABLE IF NOT EXISTS `tenant` (
    `id`             bigint(20)   AUTO_INCREMENT              COMMENT 'ID',
    `name`           varchar(30)  NOT NULL                    COMMENT '名称',
    `code`           varchar(30)  NOT NULL                    COMMENT '编码',
    `domain`         varchar(255) DEFAULT NULL                COMMENT '域名',
    `expire_time`    datetime     DEFAULT NULL                COMMENT '过期时间',
    `description`    varchar(200) DEFAULT NULL                COMMENT '描述',
    `status`         tinyint(1)   UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `admin_user`     bigint(20)   DEFAULT NULL                COMMENT '管理员用户',
    `admin_username` varchar(64)  DEFAULT NULL                COMMENT '管理员用户名',
    `package_id`     bigint(20)   NOT NULL                    COMMENT '套餐ID',
    `create_user`    bigint(20)   NOT NULL                    COMMENT '创建人',
    `create_time`    datetime     NOT NULL                    COMMENT '创建时间',
    `update_user`    bigint(20)   DEFAULT NULL                COMMENT '修改人',
    `update_time`    datetime     DEFAULT NULL                COMMENT '修改时间',
    `deleted`        bigint(20)   NOT NULL DEFAULT 0          COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_code`(`code`, `deleted`),
    INDEX `idx_admin_user`(`admin_user`),
    INDEX `idx_package_id`(`package_id`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户表';

CREATE TABLE IF NOT EXISTS `tenant_package` (
    `id`                  bigint(20)    AUTO_INCREMENT              COMMENT 'ID',
    `name`                varchar(30)   NOT NULL                    COMMENT '名称',
    `sort`                int           NOT NULL DEFAULT 999        COMMENT '排序',
    `menu_check_strictly` bit(1)        DEFAULT b'1'                COMMENT '菜单选择是否父子节点关联',
    `description`         varchar(200)  DEFAULT NULL                COMMENT '描述',
    `status`              tinyint       UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `create_user`         bigint(20)    NOT NULL                    COMMENT '创建人',
    `create_time`         datetime      NOT NULL                    COMMENT '创建时间',
    `update_user`         bigint(20)    DEFAULT NULL                COMMENT '修改人',
    `update_time`         datetime      DEFAULT NULL                COMMENT '修改时间',
    `deleted`             bigint(20)   NOT NULL DEFAULT 0           COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_create_user`(`create_user`),
    INDEX `idx_update_user`(`update_user`),
    INDEX `idx_deleted`(`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户套餐表';

CREATE TABLE IF NOT EXISTS `tenant_package_menu` (
    `package_id` bigint(20) NOT NULL COMMENT '套餐ID',
    `menu_id`    bigint(20) NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`package_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户套餐和菜单关联表';

-- 为已有表增加租户字段
ALTER TABLE `sys_dept`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_role`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_user`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_user_password_history`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_user_social`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_user_role`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_role_menu`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_role_dept`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_log`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_message`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_message_log`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_notice`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_notice_log`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);
ALTER TABLE `sys_file`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 调整唯一索引
ALTER TABLE `sys_dept`
    DROP INDEX `uk_name_parent_id`,
    ADD UNIQUE INDEX `uk_name_parent_id` (`name`, `parent_id`, `deleted`, `tenant_id`);
ALTER TABLE `sys_role`
    DROP INDEX `uk_name`,
    DROP INDEX `uk_code`,
    ADD UNIQUE INDEX `uk_name` (`name`, `deleted`, `tenant_id`),
    ADD UNIQUE INDEX `uk_code` (`code`, `deleted`, `tenant_id`);
ALTER TABLE `sys_user`
    DROP INDEX `uk_username`,
    DROP INDEX `uk_email`,
    DROP INDEX `uk_phone`,
    ADD UNIQUE INDEX `uk_username` (`username`, `deleted`, `tenant_id`),
    ADD UNIQUE INDEX `uk_email` (`email`, `deleted`, `tenant_id`),
    ADD UNIQUE INDEX `uk_phone` (`phone`, `deleted`, `tenant_id`);
ALTER TABLE `sys_user_social`
    DROP INDEX `uk_source_open_id`,
    ADD UNIQUE INDEX `uk_source_open_id` (`source`, `open_id`, `deleted`, `tenant_id`);

-- 初始化默认菜单
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(3000, '租户管理', 0, 1, '/tenant', 'Tenant', 'Layout', '/tenant/management', 'user-group', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()),

(3010, '租户列表', 3000, 2, '/tenant/management', 'TenantManagement', 'tenant/management/index', NULL, 'user-group', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()),
(3011, '列表', 3010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:management:list', 1, 1, 1, NOW()),
(3012, '详情', 3010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:management:get', 2, 1, 1, NOW()),
(3013, '新增', 3010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:management:create', 3, 1, 1, NOW()),
(3014, '修改', 3010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:management:update', 4, 1, 1, NOW()),
(3015, '删除', 3010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:management:delete', 5, 1, 1, NOW()),
(3016, '修改租户管理员密码', 3010, 3, NULL, NULL, NULL, NULL, NULL, b'0', b'0', b'0', 'tenant:management:updateAdminUserPwd', 6, 1, 1, NOW()),

(3020, '套餐管理', 3000, 2, '/tenant/package', 'TenantPackage', 'tenant/package/index', NULL, 'project', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()),
(3021, '列表', 3020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:package:list', 1, 1, 1, NOW()),
(3022, '详情', 3020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:package:get', 2, 1, 1, NOW()),
(3023, '新增', 3020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:package:create', 3, 1, 1, NOW()),
(3024, '修改', 3020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:package:update', 4, 1, 1, NOW()),
(3025, '删除', 3020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'tenant:package:delete', 5, 1, 1, NOW());

-- ========== plugin\plugin_schedule.sql ==========

-- 初始化默认菜单
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(8000, '任务调度', 0, 1, '/schedule', 'Schedule', 'Layout', '/schedule/job', 'schedule', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()),
(8010, '任务管理', 8000, 2, '/schedule/job', 'ScheduleJob', 'schedule/job/index', NULL, 'select-all', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()),
(8011, '列表', 8010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:job:list', 1, 1, 1, NOW()),
(8012, '详情', 8010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:job:get', 2, 1, 1, NOW()),
(8013, '新增', 8010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:job:create', 3, 1, 1, NOW()),
(8014, '修改', 8010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:job:update', 4, 1, 1, NOW()),
(8015, '删除', 8010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:job:delete', 5, 1, 1, NOW()),
(8016, '执行', 8010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:job:trigger', 6, 1, 1, NOW()),

(8020, '任务日志', 8000, 2, '/schedule/log', 'ScheduleLog', 'schedule/log/index', NULL, 'find-replace', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()),
(8021, '列表', 8020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:log:list', 1, 1, 1, NOW()),
(8022, '停止', 8020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:log:stop', 3, 1, 1, NOW()),
(8023, '重试', 8020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'schedule:log:retry', 4, 1, 1, NOW());

-- ========== 报价菜单 40000 段（平台一体）==========
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 40000 AND 40999;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 40000 AND 40999;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(40000, '报价管理', 0, 1, '/quote', 'QuoteRoot', 'Layout', '/quote/index', 'quote', b'0', b'0', b'0', NULL, 7, 1, 1, NOW()),
(40001, '报价列表', 40000, 2, '/quote/index', 'QuoteIndex', 'quote/index', NULL, 'list', b'0', b'0', b'0', 'quote:list', 1, 1, 1, NOW()),
(40010, '查询', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:query', 1, 1, 1, NOW()),
(40011, '新增', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:add', 2, 1, 1, NOW()),
(40012, '修改', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:update', 3, 1, 1, NOW()),
(40013, '删除', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:delete', 4, 1, 1, NOW()),
(40014, '导出', 40001, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'quote:export', 5, 1, 1, NOW());

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (SELECT `id` FROM `sys_menu` WHERE `id` BETWEEN 40000 AND 40999) m
WHERE r.id IN (1, 3);
-- ========== 运维报修菜单 20000 段 ==========
-- EOMS 平台菜单（ID 段 20000–20999）
-- 幂等：先删角色绑定与菜单，再写入；可重复执行
-- 组件路径须与前端 asyncModules 键一致（无前导 /、无 .vue）

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 20000 AND 20999;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 20000 AND 20999;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
-- 顶层 Layout
(20000, '运维报修', 0, 1, '/eoms', 'EomsRoot', 'Layout', '/eoms/repair/apply', 'tool', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()),

-- 客户/供应商/仓库已并入进销存 psi_unit / psi_warehouse（M2），不再写 20001 / 20030 / 20031

-- 报修
(20010, '报修申请', 20000, 2, '/eoms/repair/apply', 'EomsRepairApply', 'eoms/repair/apply', NULL, 'file', b'0', b'0', b'0', 'eoms:repair:list', 2, 1, 1, NOW()),
(20011, '报修工单', 20000, 2, '/eoms/repair/order', 'EomsRepairOrder', 'eoms/repair/order', NULL, 'list', b'0', b'0', b'0', 'eoms:order:query', 3, 1, 1, NOW()),
(20012, '维修记录', 20000, 2, '/eoms/repair/record', 'EomsRepairRecord', 'eoms/repair/record', NULL, 'history', b'0', b'0', b'0', 'eoms:record:list', 4, 1, 1, NOW()),
(20013, '维修结算', 20000, 2, '/eoms/repair/settlement', 'EomsRepairSettlement', 'eoms/repair/settlement', NULL, 'stamp', b'0', b'0', b'0', 'eoms:settlement:query', 5, 1, 1, NOW()),

-- 报修申请按钮（RepairApplyController）
(20101, '查询', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:list', 1, 1, 1, NOW()),
(20102, '详情', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:query', 2, 1, 1, NOW()),
(20103, '新增', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:add', 3, 1, 1, NOW()),
(20104, '确认', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:confirm', 4, 1, 1, NOW()),
(20105, '取消', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:cancel', 5, 1, 1, NOW()),
(20106, '关闭', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:close', 6, 1, 1, NOW()),
(20107, '转工单', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:assign', 7, 1, 1, NOW()),
(20108, '删除', 20010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:remove', 8, 1, 1, NOW()),

-- 报修工单按钮（RepairOrderController / EngineerController）
(20111, '查询', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:query', 1, 1, 1, NOW()),
(20112, '新增', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:add', 2, 1, 1, NOW()),
(20113, '修改', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:update', 3, 1, 1, NOW()),
(20114, '删除', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:delete', 4, 1, 1, NOW()),
(20115, '导出', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:export', 5, 1, 1, NOW()),
(20116, '接单', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:accept', 6, 1, 1, NOW()),
(20117, '开始维修', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:start', 7, 1, 1, NOW()),
(20118, '完成维修', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:finish', 8, 1, 1, NOW()),
(20119, '关闭', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:order:close', 9, 1, 1, NOW()),
(20120, '派工', 20011, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:repair:assign', 10, 1, 1, NOW()),

-- 维修记录按钮（RepairRecordController）
(20121, '查询', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:list', 1, 1, 1, NOW()),
(20122, '导出', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:export', 2, 1, 1, NOW()),
(20123, '详情', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:query', 3, 1, 1, NOW()),
(20124, '新增', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:add', 4, 1, 1, NOW()),
(20125, '修改', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:update', 5, 1, 1, NOW()),
(20126, '删除', 20012, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:record:delete', 6, 1, 1, NOW()),

-- 维修结算按钮（RepairSettlementController）
(20131, '查询', 20013, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:settlement:query', 1, 1, 1, NOW()),
(20132, '保存', 20013, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:settlement:save', 2, 1, 1, NOW()),
(20133, '确认', 20013, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:settlement:confirm', 3, 1, 1, NOW()),

-- 物料/库存/采购已并入进销存 psi_goods / psi_stock_*（M3），不再写 20020～20024

-- 主数据 / 业务（仓库/供应商走 /psi/warehouse、/psi/unit）
(20032, '运维合同', 20000, 2, '/eoms/contract', 'EomsContract', 'eoms/contract/index', NULL, 'file', b'0', b'0', b'0', 'eoms:contract:list', 13, 1, 1, NOW()),
-- 报价已迁平台 quote 菜单（40000 段），勿再写入 20033

-- 运维合同按钮（ContractController）
(20141, '导出', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:export', 1, 1, 1, NOW()),
(20142, '详情', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:query', 2, 1, 1, NOW()),
(20143, '新增', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:add', 3, 1, 1, NOW()),
(20144, '修改', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:update', 4, 1, 1, NOW()),
(20145, '删除', 20032, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:contract:delete', 5, 1, 1, NOW()),

-- 财务
(20040, '维修应收', 20000, 2, '/eoms/finance/receivable', 'EomsFinanceReceivable', 'eoms/finance/receivable', NULL, 'stamp', b'0', b'0', b'0', 'eoms:receivable:list', 15, 1, 1, NOW()),
(20041, '维修收款', 20000, 2, '/eoms/finance/receipt', 'EomsFinanceReceipt', 'eoms/finance/receipt', NULL, 'common', b'0', b'0', b'0', 'eoms:payment:list', 16, 1, 1, NOW()),
(20042, '利润分析', 20000, 2, '/eoms/finance/profit', 'EomsFinanceProfit', 'eoms/finance/profit', NULL, 'profit-model-alt', b'0', b'0', b'0', 'eoms:profit:query', 17, 1, 1, NOW()),

-- 维修应收按钮（ReceivableController）
(20151, '查询', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:query', 1, 1, 1, NOW()),
(20152, '导出', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:export', 2, 1, 1, NOW()),
(20153, '新增', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:add', 3, 1, 1, NOW()),
(20154, '修改', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:update', 4, 1, 1, NOW()),
(20155, '删除', 20040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:receivable:delete', 5, 1, 1, NOW()),

-- 维修收款按钮（PaymentController）
(20161, '查询', 20041, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:payment:query', 1, 1, 1, NOW()),
(20162, '新增', 20041, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:payment:add', 2, 1, 1, NOW()),
(20163, '修改', 20041, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:payment:update', 3, 1, 1, NOW()),
(20164, '删除', 20041, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:payment:delete', 4, 1, 1, NOW()),

-- 利润分析按钮（StatisticsController）
(20171, '查询', 20042, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:profit:query', 1, 1, 1, NOW()),

-- 统计 / 配置 / 看板
(20050, '统计分析', 20000, 2, '/eoms/statistics', 'EomsStatistics', 'eoms/statistics/index', NULL, 'dashboard', b'0', b'0', b'0', 'eoms:statistics:list', 18, 1, 1, NOW()),
(20070, '运维看板', 20000, 2, '/eoms/dashboard', 'EomsDashboard', 'eoms/dashboard', NULL, 'dashboard', b'0', b'0', b'0', 'eoms:dashboard:list', 0, 1, 1, NOW()),

-- 运维看板按钮（EomsDashboardController / ReportExportController）
(20181, '导出', 20070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'eoms:dashboard:list', 1, 1, 1, NOW());

-- 绑定超管(1)与普通角色(3)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (SELECT `id` FROM `sys_menu` WHERE `id` BETWEEN 20000 AND 20999) m
WHERE r.id IN (1, 3);


-- ========== 电子签章菜单 30000 段 ==========
-- 电子签章平台菜单（ID 段 30000–30999）
-- 幂等：先删角色绑定与菜单，再写入；可重复执行
-- 组件路径须与前端 asyncModules 键一致（无前导 /、无 .vue）

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 30000 AND 30999;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 30000 AND 30999;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(30000, '电子签章', 0, 1, '/sign', 'SignRoot', 'Layout', '/sign/dashboard', 'stamp', b'0', b'0', b'0', NULL, 6, 1, 1, NOW()),

(30001, '签章看板', 30000, 2, '/sign/dashboard', 'SignDashboard', 'sign/dashboard/index', NULL, 'dashboard', b'0', b'0', b'0', 'sign:dashboard:view', 1, 1, 1, NOW()),
(30002, '印章管理', 30000, 2, '/sign/seal', 'SignSeal', 'sign/seal/index', NULL, 'stamp', b'0', b'0', b'0', 'sign:seal:list', 2, 1, 1, NOW()),
(30003, '印章提取', 30000, 2, '/sign/seal/extract', 'SignSealExtract', 'sign/seal/extract', NULL, 'scissor', b'0', b'0', b'0', 'sign:seal:extract', 3, 1, 1, NOW()),
(30004, '项目管理', 30000, 2, '/sign/project', 'SignProject', 'sign/project/index', NULL, 'folder', b'0', b'0', b'0', 'sign:project:list', 4, 1, 1, NOW()),
(30005, '签署执行', 30000, 2, '/sign/execute', 'SignExecute', 'sign/execute/index', NULL, 'edit', b'0', b'0', b'0', 'sign:execute:list', 5, 1, 1, NOW()),
(30006, '签署记录', 30000, 2, '/sign/log', 'SignLog', 'sign/log/index', NULL, 'history', b'0', b'0', b'0', 'sign:log:list', 6, 1, 1, NOW()),
(30008, '验真核验', 30000, 2, '/sign/verify', 'SignVerify', 'sign/verify/index', NULL, 'check-circle', b'0', b'0', b'0', 'sign:verify:list', 8, 1, 1, NOW());

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (SELECT `id` FROM `sys_menu` WHERE `id` BETWEEN 30000 AND 30999) m
WHERE r.id IN (1, 3);


-- ========== P5：eoms 业务表 DDL（对齐 liquibase plugin_eoms.sql）==========
SET NAMES utf8mb4;

-- =====================================================
-- ITLS-EOMS 维修服务管理系统 数据库初始化脚本
-- 版本：V1.1
-- 说明：包含 22 张业务表 DDL + 字典 + 菜单 + 角色初始化
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- =====================================================


-- =====================================================
-- 【T2-1】22 张 eoms_ 业务表 DDL
-- 所有表含完整公共字段（日志/流水/附件表按规范省略 del_flag）
-- =====================================================

-- ---------- 5.1 客户模块（M2 已并入 psi_unit / psi_unit_contact）----------
-- 业务表 customer_id 语义：psi_unit.id。禁止再建 eoms_customer / eoms_supplier / eoms_warehouse。

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

-- ---------- 5.2 报修模块 ----------

-- 报修申请表
CREATE TABLE IF NOT EXISTS `eoms_repair_apply` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_no       varchar(32)     NOT NULL                COMMENT '报修编号，唯一',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  report_address  varchar(500)    DEFAULT NULL            COMMENT '客户提交的报修地址，独立于签到签退地址',
  contact_name    varchar(50)     DEFAULT NULL            COMMENT '联系人姓名',
  contact_phone   varchar(30)     DEFAULT NULL            COMMENT '联系电话',
  fault_type      varchar(32)     DEFAULT NULL            COMMENT '故障类型',
  fault_desc      text            DEFAULT NULL            COMMENT '故障描述',
  photos          varchar(1000)   DEFAULT NULL            COMMENT '现场照片URL（多个逗号分隔，带水印）',
  urgent_level    varchar(32)     DEFAULT 'NORMAL'        COMMENT '紧急程度',
  contract_id     bigint(20)      DEFAULT NULL            COMMENT '关联合同ID',
  status          varchar(32)     NOT NULL DEFAULT 'WAIT' COMMENT '状态：WAIT-待处理、CONFIRM-已确认、ORDER-已转工单、CANCEL-已取消',
  apply_time      datetime        DEFAULT NULL            COMMENT '报修时间',
  source          varchar(20)     NOT NULL DEFAULT 'H5'   COMMENT '报修来源：H5-客户H5、ADMIN-后台登记',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  tenant_id       bigint(20)      NOT NULL DEFAULT 0      COMMENT '租户ID',
  create_user     bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人ID',
  dept_id         bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人部门',
  PRIMARY KEY (id),
  UNIQUE KEY uk_repair_no (repair_no),
  KEY idx_customer_id (customer_id),
  KEY idx_status (status),
  KEY idx_urgent_level (urgent_level),
  KEY idx_apply_time (apply_time),
  KEY idx_source (source),
  KEY idx_apply_pool (tenant_id,status,apply_time,del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报修申请表';

-- ---------- 5.3 维修模块 ----------

-- 维修订单表
CREATE TABLE IF NOT EXISTS `eoms_repair_order` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键，工单ID',
  order_no        varchar(32)     NOT NULL                COMMENT '维修单号，唯一',
  apply_id        bigint(20)      DEFAULT NULL            COMMENT '关联报修申请ID',
  quote_id        bigint(20)      DEFAULT NULL            COMMENT '关联报价单ID',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  engineer_id     bigint(20)      DEFAULT NULL            COMMENT '工程师ID，关联sys_user.user_id',
  status          varchar(32)     NOT NULL DEFAULT 'WAIT_ACCEPT' COMMENT '状态：WAIT_ACCEPT-待接单、ACCEPTED-已接单、PROCESSING-维修中、WAIT_SETTLEMENT-待结算、FINISHED-已完成、CLOSED-已关闭',
  accept_time     datetime        DEFAULT NULL            COMMENT '接单时间',
  start_time      datetime        DEFAULT NULL            COMMENT '开始维修时间',
  finish_time     datetime        DEFAULT NULL            COMMENT '完成时间',
  address         varchar(500)    DEFAULT NULL            COMMENT '维修地址',
  photos          varchar(1000)   DEFAULT NULL            COMMENT '现场照片URL（多个逗号分隔，带水印）',
  sign_in_longitude  varchar(32)  DEFAULT NULL            COMMENT '签到经度（开始维修时定位）',
  sign_in_latitude   varchar(32)  DEFAULT NULL            COMMENT '签到纬度（开始维修时定位）',
  sign_in_address    varchar(500) DEFAULT NULL            COMMENT '签到地址（逆地理编码中文地址）',
  sign_out_longitude varchar(32)  DEFAULT NULL            COMMENT '签退经度（完成维修时定位）',
  sign_out_latitude  varchar(32)  DEFAULT NULL            COMMENT '签退纬度（完成维修时定位）',
  sign_out_address   varchar(500) DEFAULT NULL            COMMENT '签退地址（逆地理编码中文地址）',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  tenant_id       bigint(20)      NOT NULL DEFAULT 0      COMMENT '租户ID',
  create_user     bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人ID',
  dept_id         bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人部门',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_apply_id (apply_id),
  KEY idx_customer_id (customer_id),
  KEY idx_engineer_id (engineer_id),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修订单表';

-- 维修记录表
CREATE TABLE IF NOT EXISTS `eoms_repair_record` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_order_id bigint(20)      NOT NULL                COMMENT '维修工单ID',
  fault_reason    text            DEFAULT NULL            COMMENT '故障原因',
  check_process   text            DEFAULT NULL            COMMENT '检测过程',
  solution        text            DEFAULT NULL            COMMENT '解决方案',
  result          text            DEFAULT NULL            COMMENT '维修结果',
  processing_photos text          DEFAULT NULL            COMMENT '维修中照片URL（逗号分隔，带水印）',
  completed_photos  text          DEFAULT NULL            COMMENT '维修完成照片URL（逗号分隔，带水印）',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  tenant_id       bigint(20)      NOT NULL DEFAULT 0      COMMENT '租户ID',
  create_user     bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人ID',
  dept_id         bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人部门',
  PRIMARY KEY (id),
  KEY idx_repair_order_id (repair_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修记录表';

-- 维修结算表
CREATE TABLE IF NOT EXISTS `eoms_repair_settlement` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  settlement_no   varchar(32)     DEFAULT NULL            COMMENT '结算单号（JS前缀）',
  repair_order_id bigint(20)      NOT NULL                COMMENT '维修工单ID',
  service_fee     decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '服务费',
  material_amount decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '材料收入总额',
  labor_amount    decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '人工收入总额',
  other_amount    decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '其它收入',
  total_income    decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '总收入',
  material_cost   decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '材料成本总额',
  labor_cost      decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '人工成本总额',
  other_cost      decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '其它成本',
  total_cost      decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '总成本',
  profit_amount   decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '利润额 = 总收入 - 总成本',
  status          varchar(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿、CONFIRMED-已确认',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  tenant_id       bigint(20)      NOT NULL DEFAULT 0      COMMENT '租户ID',
  create_user     bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人ID',
  dept_id         bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人部门',
  PRIMARY KEY (id),
  UNIQUE KEY uk_settlement_no (settlement_no),
  active_order_id bigint GENERATED ALWAYS AS (CASE WHEN del_flag='0' AND status<>'REVERSED' THEN repair_order_id ELSE NULL END) STORED,
  UNIQUE KEY uk_settlement_active_order (active_order_id),
  KEY idx_settlement_order_history (repair_order_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修结算表';

-- 维修材料使用表
CREATE TABLE IF NOT EXISTS `eoms_repair_material` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_order_id bigint(20)      NOT NULL                COMMENT '维修工单ID',
  material_id     bigint(20)      NOT NULL                COMMENT '物料ID，关联 psi_goods.id',
  warehouse_id    bigint(20)      DEFAULT NULL            COMMENT '出库仓库ID，关联 psi_warehouse.id',
  material_name   varchar(100)    NOT NULL                COMMENT '物料名称（快照）',
  quantity        decimal(10,2)   NOT NULL                COMMENT '使用数量',
  sale_price      decimal(12,2)   NOT NULL                COMMENT '销售单价（快照）',
  cost_price      decimal(12,2)   NOT NULL                COMMENT '成本单价（快照）',
  sale_amount     decimal(12,2)   NOT NULL                COMMENT '销售金额',
  cost_amount     decimal(12,2)   NOT NULL                COMMENT '成本金额',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  KEY idx_repair_order_id (repair_order_id),
  KEY idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修材料使用表';

-- ---------- 5.4 报价模块 ----------

-- 报价单表
CREATE TABLE IF NOT EXISTS `eoms_quote` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id       bigint(20)      DEFAULT NULL COMMENT '认证租户；存量未知归属不自动推断',
  create_user     bigint(20)      DEFAULT NULL COMMENT '认证创建人ID；不按用户名授权',
  dept_id         bigint(20)      DEFAULT NULL COMMENT '创建时认证部门ID',
  quote_no        varchar(32)     NOT NULL                COMMENT '报价编号，唯一',
  customer_id     bigint(20)      DEFAULT NULL            COMMENT '客户ID，关联 psi_unit.id',
  warehouse_id    bigint(20)      DEFAULT NULL            COMMENT '仓库ID，关联 psi_warehouse.id',
  project_name    varchar(200)    DEFAULT NULL            COMMENT '项目名称',
  quote_unit      varchar(200)    DEFAULT NULL            COMMENT '报价单位',
  quote_date      datetime        DEFAULT NULL            COMMENT '报价时间',
  valid_until     datetime        DEFAULT NULL            COMMENT '报价有效期',
  tax_rate        decimal(5,2)    NOT NULL DEFAULT 4.00   COMMENT '增值税综合税率(%)',
  material_subtotal decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '材料小计',
  service_subtotal decimal(12,2)  NOT NULL DEFAULT 0.00   COMMENT '技术服务费小计',
  repair_order_id bigint(20)      DEFAULT NULL            COMMENT '关联维修工单ID',
  total_amount    decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '报价总金额(含税)',
  status          varchar(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿、SUBMIT-已提交、CONFIRM-已确认、CANCEL-已取消',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  terms           text            DEFAULT NULL            COMMENT '备注/条款',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_quote_no (quote_no),
  KEY idx_customer_id (customer_id),
  KEY idx_quote_warehouse (warehouse_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报价单表';

-- 报价明细表
CREATE TABLE IF NOT EXISTS `eoms_quote_detail` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  quote_id        bigint(20)      NOT NULL                COMMENT '报价单ID',
  line_type       varchar(16)     NOT NULL DEFAULT 'SERVICE' COMMENT 'MATERIAL材料 / SERVICE服务',
  goods_id        bigint(20)      DEFAULT NULL            COMMENT '材料行关联 psi_goods.id',
  item_name       varchar(200)    NOT NULL                COMMENT '材料名称/服务项目',
  brand_model     varchar(100)    DEFAULT NULL            COMMENT '品牌/型号',
  unit            varchar(20)     DEFAULT '台'            COMMENT '单位',
  description     varchar(200)    DEFAULT NULL            COMMENT '服务说明',
  sort_no         int             NOT NULL DEFAULT 0      COMMENT '行序号',
  quantity        decimal(10,2)   NOT NULL                COMMENT '数量/工作量',
  price           decimal(12,2)   NOT NULL                COMMENT '单价',
  amount          decimal(12,2)   NOT NULL                COMMENT '金额',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  KEY idx_quote_id (quote_id),
  KEY idx_qd_goods (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报价明细表';

-- ---------- 5.5 合同模块 ----------

-- 合同表
CREATE TABLE IF NOT EXISTS `eoms_contract` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  contract_no     varchar(32)     NOT NULL                COMMENT '合同编号，唯一',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  project_id      bigint(20)      DEFAULT NULL            COMMENT '所属项目',
  contract_name   varchar(200)    NOT NULL                COMMENT '合同名称',
  contract_type   varchar(20)     DEFAULT NULL            COMMENT '合同类型',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '合同金额',
  sign_date       datetime        DEFAULT NULL            COMMENT '签订日期；历史未记录时为空',
  start_date      date            DEFAULT NULL            COMMENT '开始日期',
  end_date        date            DEFAULT NULL            COMMENT '结束日期',
  status          varchar(20)     NOT NULL DEFAULT 'EFFECTIVE' COMMENT '状态：EFFECTIVE-生效中、EXPIRED-已到期、TERMINATED-已终止',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  file_id         varchar(500)   DEFAULT NULL            COMMENT '附件文件ID，多个逗号分隔',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  tenant_id       bigint(20)      NOT NULL DEFAULT 0      COMMENT '租户ID',
  create_user     bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人ID',
  dept_id         bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人部门',
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_no (contract_no),
  KEY idx_customer_id (customer_id),
  KEY idx_ec_project (project_id),
  KEY idx_ec_file (file_id),
  KEY idx_status (status),
  KEY idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合同表';

-- ---------- 5.6 物料库存模块（M3 已并入 psi_goods / psi_stock_*）----------
-- 业务表 material_id 语义：psi_goods.id。禁止再建 eoms_material* / eoms_purchase / eoms_inbound。
-- 工单用料 eoms_repair_material.warehouse_id 语义：psi_warehouse.id；结算出库走 PSI stock-out（REPAIR）。

-- ---------- 5.7 财务模块 ----------

-- 收入记录表
CREATE TABLE IF NOT EXISTS `eoms_income` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  income_no       varchar(32)     NOT NULL                COMMENT '收入编号',
  repair_order_id bigint(20)      NOT NULL                COMMENT '关联维修工单ID',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '收入金额',
  income_type     varchar(20)     DEFAULT NULL            COMMENT '收入类型',
  income_date     date            DEFAULT NULL            COMMENT '收入日期',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  payment_id bigint DEFAULT NULL,
  reversal_of_id bigint DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_income_payment (payment_id),
  UNIQUE KEY uk_income_reversal (reversal_of_id),

  UNIQUE KEY uk_income_no (income_no),
  KEY idx_repair_order_id (repair_order_id),
  KEY idx_customer_id (customer_id),
  KEY idx_income_date (income_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收入记录表';

-- 成本记录表
CREATE TABLE IF NOT EXISTS `eoms_cost` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_order_id bigint(20)      NOT NULL                COMMENT '关联维修工单ID',
  cost_type       varchar(20)     NOT NULL                COMMENT '成本类型：MATERIAL-材料、LABOR-人工、OTHER-其它',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '成本金额',
  description     varchar(500)    DEFAULT NULL            COMMENT '成本说明',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  settlement_id bigint DEFAULT NULL,
  reversal_of_id bigint DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cost_reversal (reversal_of_id),
  KEY idx_cost_settlement (settlement_id),

  KEY idx_repair_order_id (repair_order_id),
  KEY idx_cost_type (cost_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成本记录表';

-- 应收账款表
CREATE TABLE IF NOT EXISTS `eoms_receivable` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  receivable_no   varchar(32)     DEFAULT NULL            COMMENT '应收单号（YS前缀）',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  repair_order_id bigint(20)      NOT NULL                COMMENT '关联维修工单ID',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '应收金额',
  received_amount decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '已收金额',
  arrears_amount  decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '欠款金额 = 应收 - 已收',
  status          varchar(20)     NOT NULL DEFAULT 'UNPAID' COMMENT '状态：UNPAID-未收款、PARTIAL-部分收款、PAID-已收款',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  tenant_id       bigint(20)      NOT NULL DEFAULT 0      COMMENT '租户ID',
  create_user     bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人ID',
  dept_id         bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人部门',
  settlement_id bigint DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_receivable_settlement (settlement_id),

  UNIQUE KEY uk_receivable_no (receivable_no),
  KEY idx_customer_id (customer_id),
  KEY idx_repair_order_id (repair_order_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修应收账款';

-- 收款记录表（维修收款，勿与进销存 psi_payment 付款混淆）
CREATE TABLE IF NOT EXISTS `eoms_payment` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  receivable_id   bigint(20)      NOT NULL                COMMENT '关联应收单ID',
  payment_no      varchar(32)     DEFAULT NULL            COMMENT '收款单号（SK前缀）',
  batch_id        varchar(64)     DEFAULT NULL            COMMENT '批量核销批次ID',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '收款金额',
  payment_method  varchar(20)     DEFAULT NULL            COMMENT '收款方式：现金、转账、微信、支付宝',
  payment_time    datetime        DEFAULT NULL            COMMENT '收款时间',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  tenant_id       bigint(20)      NOT NULL DEFAULT 0      COMMENT '租户ID',
  create_user     bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人ID',
  dept_id         bigint(20)      NOT NULL DEFAULT 0      COMMENT '创建人部门',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  reversed_time datetime DEFAULT NULL,
  reversed_by varchar(64) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_payment_status (status),

  KEY idx_customer_id (customer_id),
  KEY idx_receivable_id (receivable_id),
  KEY idx_payment_time (payment_time),
  KEY idx_payment_no (payment_no),
  KEY idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修收款记录';

CREATE TABLE IF NOT EXISTS eoms_finance_event (
  id bigint NOT NULL AUTO_INCREMENT,
  aggregate_type varchar(20) NOT NULL,
  aggregate_id bigint NOT NULL,
  action varchar(20) NOT NULL,
  related_id bigint DEFAULT NULL,
  amount decimal(12,2) DEFAULT NULL,
  operator varchar(64) DEFAULT NULL,
  reason varchar(500) NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_finance_event (aggregate_type, aggregate_id, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修财务补偿审计（保留原始单据与正反流水）';

-- ---------- 5.8 基础能力（M1 已并入平台）----------
-- eoms_config → sys_option（category=EOMS）
-- eoms_attachment → sys_file；合同附件走 eoms_contract.file_id
-- eoms_operation_log → sys_log（continew @Log）

-- =====================================================

-- ========== P5：sign 业务表 DDL（对齐 liquibase plugin_sign.sql）==========
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sign_backup_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务编码',
  `trigger_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发类型(manual/scheduled)',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态(running/success/failed)',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备份文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行消息',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_id`),
  KEY `idx_status` (`status`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='备份任务表';

CREATE TABLE IF NOT EXISTS `sign_backup_schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计划名称',
  `cycle` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '周期(daily/weekly/monthly)',
  `run_time` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行时间(HH:MM)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0启用 1暂停)',
  `trigger_os` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'auto' COMMENT '触发操作系统(auto/windows/linux)',
  `last_run_at` datetime DEFAULT NULL COMMENT '上次执行时间',
  `next_run_at` datetime DEFAULT NULL COMMENT '下次执行时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='备份计划表';

CREATE TABLE IF NOT EXISTS `sign_ca_intermediate` (
  `intermediate_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Intermediate CA ID',
  `root_id` bigint NOT NULL COMMENT 'Root CA ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Intermediate CA name',
  `subject_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Subject DN',
  `issuer_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Issuer DN',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Certificate serial number',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHA256 fingerprint',
  `not_before` datetime NOT NULL COMMENT 'Validity start time',
  `not_after` datetime NOT NULL COMMENT 'Validity end time',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 active 1 disabled 2 revoked)',
  `cert_pem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Certificate PEM',
  `crl_number` bigint DEFAULT NULL COMMENT 'Latest CRL number',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`intermediate_id`),
  UNIQUE KEY `uk_sign_ca_intermediate_serial_no` (`serial_no`),
  KEY `idx_sign_ca_intermediate_root_id` (`root_id`),
  KEY `idx_sign_ca_intermediate_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Private CA intermediate certificate';

CREATE TABLE IF NOT EXISTS `sign_ca_root` (
  `root_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Root CA ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Root CA name',
  `subject_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Subject DN',
  `issuer_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Issuer DN',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Certificate serial number',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHA256 fingerprint',
  `not_before` datetime NOT NULL COMMENT 'Validity start time',
  `not_after` datetime NOT NULL COMMENT 'Validity end time',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 active 1 disabled 2 revoked)',
  `cert_pem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Certificate PEM',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`root_id`),
  UNIQUE KEY `uk_sign_ca_root_serial_no` (`serial_no`),
  KEY `idx_sign_ca_root_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Private CA root certificate';

CREATE TABLE IF NOT EXISTS `sign_cert` (
  `cert_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Certificate ID',
  `org_id` bigint DEFAULT NULL COMMENT 'Organization ID',
  `project_id` bigint DEFAULT NULL COMMENT 'Project ID',
  `seal_id` bigint DEFAULT NULL COMMENT 'Seal ID',
  `cert_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Certificate type',
  `subject_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Subject DN',
  `issuer_dn` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Issuer DN',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Certificate serial number',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHA256 fingerprint',
  `not_before` datetime NOT NULL COMMENT 'Validity start time',
  `not_after` datetime NOT NULL COMMENT 'Validity end time',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 active 1 disabled 2 revoked)',
  `cert_pem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Certificate PEM',
  `chain_pem` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'Certificate chain PEM',
  `key_ref` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Managed key reference',
  `issued_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Issued time',
  `revoked_at` datetime DEFAULT NULL COMMENT 'Revoked time',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`cert_id`),
  UNIQUE KEY `uk_sign_cert_serial_no` (`serial_no`),
  KEY `idx_sign_cert_seal_id` (`seal_id`),
  KEY `idx_sign_cert_status` (`status`),
  KEY `idx_sign_cert_key_ref` (`key_ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Managed signing certificates';

CREATE TABLE IF NOT EXISTS `sign_cert_request` (
  `request_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Request ID',
  `request_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Request type',
  `org_id` bigint DEFAULT NULL COMMENT 'Organization ID',
  `project_id` bigint DEFAULT NULL COMMENT 'Project ID',
  `seal_id` bigint DEFAULT NULL COMMENT 'Seal ID',
  `subject_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Certificate subject name',
  `subject_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Certificate subject code',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 pending 1 approved 2 rejected 3 issued)',
  `approve_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Approver',
  `approve_time` datetime DEFAULT NULL COMMENT 'Approve time',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Reject reason',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`request_id`),
  KEY `idx_sign_cert_request_status` (`status`),
  KEY `idx_sign_cert_request_seal_id` (`seal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Certificate request form';

CREATE TABLE IF NOT EXISTS `sign_cert_revocation` (
  `revoke_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Revocation ID',
  `cert_id` bigint NOT NULL COMMENT 'Certificate ID',
  `revoke_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Revocation reason',
  `revoke_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Revocation time',
  `operator_id` bigint DEFAULT NULL COMMENT 'Operator ID',
  `crl_no` bigint DEFAULT NULL COMMENT 'CRL number',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`revoke_id`),
  KEY `idx_sign_cert_revocation_cert_id` (`cert_id`),
  KEY `idx_sign_cert_revocation_revoke_time` (`revoke_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Certificate revocation records';

CREATE TABLE IF NOT EXISTS `sign_file` (
  `file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件存储路径',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `file_mime` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'MIME类型',
  `file_sha256` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHA256哈希',
  `file_ext` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件扩展名',
  `owner_id` bigint DEFAULT NULL COMMENT '上传者ID',
  `biz_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务类型(seal/avatar/backup/sign)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`file_id`),
  UNIQUE KEY `uk_sha256` (`file_sha256`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件管理表';

CREATE TABLE IF NOT EXISTS `sign_key_store` (
  `key_ref` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Managed key reference',
  `storage_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SERVER_ENCRYPTED' COMMENT 'Storage type',
  `encrypted_key_blob` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Encrypted private key blob',
  `encrypt_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Encryption version',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT 'Status(0 active 1 disabled)',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Created by',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Updated by',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`key_ref`),
  KEY `idx_sign_key_store_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Managed key storage';

CREATE TABLE IF NOT EXISTS `sign_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `seal_id` bigint NOT NULL COMMENT '印章ID',
  `user_id` bigint NOT NULL COMMENT '操作人ID',
  `file_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件SHA256哈希',
  `client_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端IP',
  `ip_location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP归属地',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `output_file_id` bigint DEFAULT NULL COMMENT '输出文件ID',
  `cert_id` bigint DEFAULT NULL COMMENT '证书ID',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作类型',
  `result_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结果编码',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '追踪ID',
  `sign_time` datetime NOT NULL COMMENT '签章时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID',
  `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID',
  `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '部门ID',
  PRIMARY KEY (`log_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_seal_id` (`seal_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sign_time` (`sign_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='签章日志表';

CREATE TABLE IF NOT EXISTS `sign_project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目名称',
  `project_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目编号',
  `manager_ids` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人用户ID(多个用逗号分隔)',
  `manager_names` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人姓名(多个用逗号分隔)',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `start_date` datetime NOT NULL COMMENT '开始日期',
  `end_date` datetime NOT NULL COMMENT '结束日期',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0待审核 1未开始 2进行中 3已完成 4已暂停 5已取消)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '项目描述',
  `contract_type` varchar(16) DEFAULT NULL COMMENT '兼容旧筛：曾绑定类型 SALE/PURCHASE/REPAIR',
  `contract_id` bigint DEFAULT NULL COMMENT '兼容旧筛：曾绑定合同ID',
  `contract_no` varchar(64) DEFAULT NULL COMMENT '合同编号汇总快照',
  `contract_name` varchar(200) DEFAULT NULL COMMENT '合同名称汇总快照',
  `partner_name` varchar(200) DEFAULT NULL COMMENT '往来单位汇总快照',
  `sale_contract_id` bigint DEFAULT NULL COMMENT '销售合同ID',
  `sale_contract_no` varchar(64) DEFAULT NULL COMMENT '销售合同编号快照',
  `sale_contract_name` varchar(200) DEFAULT NULL COMMENT '销售合同名称快照',
  `sale_partner_name` varchar(200) DEFAULT NULL COMMENT '销售往来单位快照',
  `purchase_contract_id` bigint DEFAULT NULL COMMENT '采购合同ID',
  `purchase_contract_no` varchar(64) DEFAULT NULL COMMENT '采购合同编号快照',
  `purchase_contract_name` varchar(200) DEFAULT NULL COMMENT '采购合同名称快照',
  `purchase_partner_name` varchar(200) DEFAULT NULL COMMENT '采购往来单位快照',
  `repair_contract_id` bigint DEFAULT NULL COMMENT '维修合同ID',
  `repair_contract_no` varchar(64) DEFAULT NULL COMMENT '维修合同编号快照',
  `repair_contract_name` varchar(200) DEFAULT NULL COMMENT '维修合同名称快照',
  `repair_partner_name` varchar(200) DEFAULT NULL COMMENT '维修往来单位快照',
  `other_file_ids` varchar(1000) DEFAULT NULL COMMENT '其他文件：平台sys_file.id，逗号分隔',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID',
  `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_status` (`status`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_manager_id` (`manager_ids`),
  KEY `idx_date_range` (`start_date`,`end_date`),
  KEY `idx_sign_project_contract` (`contract_type`,`contract_id`),
  KEY `idx_sign_project_sale_contract` (`sale_contract_id`),
  KEY `idx_sign_project_purchase_contract` (`purchase_contract_id`),
  KEY `idx_sign_project_repair_contract` (`repair_contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='签章项目表';

CREATE TABLE IF NOT EXISTS `sign_project_seal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `seal_id` bigint NOT NULL COMMENT '印章ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_seal` (`project_id`,`seal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目印章关联表';

CREATE TABLE IF NOT EXISTS `sign_project_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目角色',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user` (`project_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目成员关联表';

CREATE TABLE IF NOT EXISTS `sign_record` (
  `record_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '记录ID(UUID)',
  `file_id` bigint NOT NULL COMMENT '源文件ID',
  `seal_id` bigint NOT NULL COMMENT '印章ID',
  `user_id` bigint NOT NULL COMMENT '签章人用户ID',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `sign_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '签章模式(single/continuous/span/fixed)',
  `positions` json NOT NULL COMMENT '签章位置JSON',
  `has_timestamp` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否包含时间戳(0否 1是)',
  `span_config` json DEFAULT NULL COMMENT '跨页配置JSON',
  `client_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户端IP',
  `ip_location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP归属地',
  `os_version` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作系统版本',
  `browser_version` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '浏览器版本',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'completed' COMMENT '状态(completed/failed)',
  `signed_file_id` bigint DEFAULT NULL COMMENT '已签章文件ID',
  `cert_id` bigint DEFAULT NULL COMMENT '签章证书ID',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '证书序列号',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '证书SHA256指纹',
  `verify_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '验签状态',
  `sign_time` datetime DEFAULT NULL COMMENT '签章时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `sign_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签章快照SHA256',
  `page_fingerprint` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '页面位置快照SHA256',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户ID',
  `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT '创建人ID',
  `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '部门ID',
  PRIMARY KEY (`record_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_seal_id` (`seal_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_sign_time` (`sign_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='签章记录表';

CREATE TABLE IF NOT EXISTS `sign_seal` (
  `seal_id` bigint NOT NULL AUTO_INCREMENT COMMENT '印章ID',
  `seal_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '印章名称',
  `seal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '印章编号',
  `seal_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '印章类型',
  `seal_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '使用范围',
  `enable_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '启用日期',
  `org_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '机构名称',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `seal_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '印章图片路径',
  `default_size_mm` int DEFAULT NULL COMMENT '默认尺寸(mm)',
  `size_options` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '可选尺寸(mm,逗号分隔)',
  `cert_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '证书文件路径',
  `key_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '私钥文件路径',
  `cert_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '证书信息JSON',
  `cert_id` bigint DEFAULT NULL COMMENT '绑定证书ID',
  `key_ref` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '托管密钥引用',
  `cert_bind_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'uploaded' COMMENT '证书绑定模式',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态(0启用 1禁用)',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`seal_id`),
  UNIQUE KEY `uk_seal_code` (`seal_code`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='印章信息表';

CREATE TABLE IF NOT EXISTS `sign_seal_extract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `seal_id` bigint DEFAULT NULL COMMENT '印章ID',
  `original_file_id` bigint DEFAULT NULL COMMENT '原始文件ID',
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始文件名',
  `original_file_size` bigint DEFAULT NULL COMMENT '原始文件大小(字节)',
  `extract_file_id` bigint DEFAULT NULL COMMENT '提取文件ID',
  `extract_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提取文件名',
  `extract_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提取文件路径',
  `extract_file_size` bigint NOT NULL COMMENT '提取文件大小(字节)',
  `thumbnail_file_path` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '缩略图路径',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建用户ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_seal_id` (`seal_id`),
  KEY `idx_create_by` (`create_by`),
  KEY `idx_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='印章提取记录表';

CREATE TABLE IF NOT EXISTS `sign_seal_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `seal_id` bigint NOT NULL COMMENT '印章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seal_user` (`seal_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='印章使用授权表';

CREATE TABLE IF NOT EXISTS `sign_verify_receipt` (
  `receipt_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Receipt ID',
  `record_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Sign record ID',
  `origin_file_id` bigint DEFAULT NULL COMMENT 'Original file ID',
  `signed_file_id` bigint DEFAULT NULL COMMENT 'Signed file ID',
  `verify_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT 'Verify status(0 pending 1 pass 2 fail)',
  `cert_id` bigint DEFAULT NULL COMMENT 'Certificate ID',
  `serial_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Certificate serial number',
  `fingerprint_sha256` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'SHA256 fingerprint',
  `receipt_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Receipt number',
  `receipt_file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Receipt file path',
  `verified_at` datetime DEFAULT NULL COMMENT 'Verify time',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Remark',
  PRIMARY KEY (`receipt_id`),
  UNIQUE KEY `uk_sign_verify_receipt_no` (`receipt_no`),
  KEY `idx_sign_verify_receipt_record_id` (`record_id`),
  KEY `idx_sign_verify_receipt_cert_id` (`cert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Verify receipt snapshots';

-- ========== P5：psi 业务表/字典/菜单种子（plugin_psi + patches 终态折叠）==========
-- validCheckSum: ANY

-- ---------- 主数据 ----------
CREATE TABLE IF NOT EXISTS `psi_unit` (
    `id`           bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `unit_code`    varchar(32)   NOT NULL COMMENT '单位编号',
    `unit_name`    varchar(128)  NOT NULL COMMENT '单位全称',
    `unit_type`    varchar(16)   NOT NULL COMMENT 'CUSTOMER/SUPPLIER/BOTH',
    `unit_nature`  varchar(16)   NOT NULL DEFAULT 'ENTERPRISE' COMMENT 'ENTERPRISE/PERSON',
    `credit_code`  varchar(64)   DEFAULT NULL COMMENT '统一社会信用代码',
    `address`      varchar(256)  DEFAULT NULL COMMENT '地址',
    `category_id`  bigint(20)    DEFAULT NULL COMMENT '分类ID',
    `level_code`   varchar(16)   DEFAULT NULL COMMENT '等级',
    `status`       tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `remark`       varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`  bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`  datetime      NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_unit_code` (`tenant_id`, `unit_code`, `deleted`),
    INDEX `idx_unit_name` (`tenant_id`, `unit_name`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位';

CREATE TABLE IF NOT EXISTS `psi_unit_contact` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `contact_name`  varchar(64)   NOT NULL COMMENT '联系人',
    `mobile`        varchar(32)   DEFAULT NULL COMMENT '手机',
    `phone`         varchar(32)   DEFAULT NULL COMMENT '电话',
    `email`         varchar(128)  DEFAULT NULL COMMENT '邮箱',
    `position`      varchar(64)   DEFAULT NULL COMMENT '职务',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位联系人';

CREATE TABLE IF NOT EXISTS `psi_unit_invoice` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `title`         varchar(128)  NOT NULL COMMENT '发票抬头',
    `tax_no`        varchar(64)   DEFAULT NULL COMMENT '税号',
    `bank_name`     varchar(128)  DEFAULT NULL COMMENT '开户行',
    `account_no`    varchar(64)   DEFAULT NULL COMMENT '账号',
    `address`       varchar(256)  DEFAULT NULL COMMENT '地址',
    `phone`         varchar(32)   DEFAULT NULL COMMENT '电话',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位开票信息';

CREATE TABLE IF NOT EXISTS `psi_unit_address` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `contact_name`  varchar(64)   DEFAULT NULL COMMENT '收货人',
    `mobile`        varchar(32)   DEFAULT NULL COMMENT '手机',
    `region`        varchar(128)  DEFAULT NULL COMMENT '省市区',
    `address`       varchar(256)  NOT NULL COMMENT '详细地址',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位收货地址';

CREATE TABLE IF NOT EXISTS `psi_unit_pay` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `account_name`  varchar(128)  NOT NULL COMMENT '户名',
    `bank_name`     varchar(128)  DEFAULT NULL COMMENT '开户行',
    `account_no`    varchar(64)   NOT NULL COMMENT '账号',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位收款信息';

CREATE TABLE IF NOT EXISTS `psi_print_template` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `template_name`  varchar(128)  NOT NULL COMMENT '模板名称',
    `biz_type`       varchar(32)   NOT NULL COMMENT '业务类型 STOCK_OUT 等',
    `file_id`        bigint(20)    DEFAULT NULL COMMENT '附件/Word 文件ID',
    `content_html`   mediumtext    DEFAULT NULL COMMENT '浏览器打印 HTML 模板',
    `field_mapping`  varchar(2000) DEFAULT NULL COMMENT '字段映射说明 JSON',
    `is_default`     tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `status`         tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_biz_type` (`tenant_id`, `biz_type`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='PSI 打印模板';

CREATE TABLE IF NOT EXISTS `psi_goods_category` (
    `id`           bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `parent_id`    bigint(20)    NOT NULL DEFAULT 0 COMMENT '父级ID',
    `code`         varchar(32)   DEFAULT NULL COMMENT '分类编码（一级 10/20/30，下级 1001/2001）',
    `name`         varchar(64)   NOT NULL COMMENT '分类名称',
    `sort`         int           NOT NULL DEFAULT 999 COMMENT '排序',
    `status`       tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `create_user`  bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`  datetime      NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_category_code` (`tenant_id`, `code`, `deleted`),
    INDEX `idx_parent` (`tenant_id`, `parent_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品分类';

CREATE TABLE IF NOT EXISTS `psi_goods_brand` (
    `id`           bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `name`         varchar(64)   NOT NULL COMMENT '品牌名称',
    `status`       tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `create_user`  bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`  datetime      NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_brand_name` (`tenant_id`, `name`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品品牌';

CREATE TABLE IF NOT EXISTS `psi_goods` (
    `id`                bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`         bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `goods_code`        varchar(32)    NOT NULL COMMENT '商品编号',
    `goods_name`        varchar(128)   NOT NULL COMMENT '商品名称',
    `barcode`           varchar(64)    DEFAULT NULL COMMENT '条码',
    `spec`              varchar(128)   DEFAULT NULL COMMENT '规格型号',
    `unit`              varchar(16)    NOT NULL DEFAULT '个' COMMENT '计量单位',
    `default_tax_rate`  decimal(5,2)   NOT NULL DEFAULT 13.00 COMMENT '默认税率',
    `category_id`       bigint(20)     DEFAULT NULL COMMENT '分类ID',
    `brand_id`          bigint(20)     DEFAULT NULL COMMENT '品牌ID',
    `purchase_price`    decimal(18,2)  DEFAULT NULL COMMENT '采购参考价',
    `sale_price`        decimal(18,2)  DEFAULT NULL COMMENT '销售参考价',
    `min_sale_price`    decimal(18,2)  DEFAULT NULL COMMENT '最低售价',
    `stock_upper`       decimal(18,4)  DEFAULT NULL COMMENT '库存上限',
    `stock_lower`       decimal(18,4)  DEFAULT NULL COMMENT '库存下限',
    `status`            tinyint(1)     UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `remark`            varchar(500)   DEFAULT NULL COMMENT '备注',
    `create_user`       bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`       datetime       NOT NULL COMMENT '创建时间',
    `update_user`       bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`       datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`           bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_goods_code` (`tenant_id`, `goods_code`, `deleted`),
    INDEX `idx_goods_name` (`tenant_id`, `goods_name`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品档案';

CREATE TABLE IF NOT EXISTS `psi_warehouse` (
    `id`              bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `warehouse_code`  varchar(32)   NOT NULL COMMENT '仓库编码',
    `warehouse_name`  varchar(64)   NOT NULL COMMENT '仓库名称',
    `manager_user_id` bigint(20)    DEFAULT NULL COMMENT '负责人用户ID，关联 sys_user.id',
    `manager`         varchar(32)   DEFAULT NULL COMMENT '负责人姓名（冗余展示）',
    `address`         varchar(256)  DEFAULT NULL COMMENT '地址',
    `is_default`      tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认仓（0否1是）',
    `status`          tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `remark`          varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`     bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`     datetime      NOT NULL COMMENT '创建时间',
    `update_user`     bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`     datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`         bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_warehouse_code` (`tenant_id`, `warehouse_code`, `deleted`),
    INDEX `idx_manager_user` (`manager_user_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='仓库';

CREATE TABLE IF NOT EXISTS `psi_doc_seq` (
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)   NOT NULL DEFAULT 0 COMMENT '租户ID',
    `prefix`       varchar(16)  NOT NULL COMMENT '单号前缀',
    `biz_date`     varchar(8)   NOT NULL COMMENT '业务日yyyyMMdd',
    `current_val`  int          NOT NULL DEFAULT 0 COMMENT '当前序号',
    `update_time`  datetime     DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_doc_seq` (`tenant_id`, `prefix`, `biz_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='单据号序列';

-- ---------- 库存核心（P0 表结构先行） ----------
CREATE TABLE IF NOT EXISTS `psi_stock_batch` (
    `id`               bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`        bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `warehouse_id`     bigint(20)     NOT NULL COMMENT '仓库ID',
    `goods_id`         bigint(20)     NOT NULL COMMENT '商品ID',
    `batch_no`         varchar(64)    NOT NULL COMMENT '批次号',
    `source_type`      varchar(32)    NOT NULL COMMENT '来源类型',
    `source_id`        bigint(20)     DEFAULT NULL COMMENT '来源单据ID',
    `source_no`        varchar(64)    DEFAULT NULL COMMENT '来源单据号',
    `in_date`          datetime       NOT NULL COMMENT 'FIFO排序入库日',
    `origin_in_date`   datetime       NOT NULL COMMENT '最初入库日',
    `total_quantity`   decimal(18,4)  NOT NULL COMMENT '入库总数量',
    `remain_quantity`  decimal(18,4)  NOT NULL COMMENT '剩余数量',
    `untax_price`      decimal(18,6)  NOT NULL COMMENT '未税单位成本',
    `total_amount`     decimal(18,2)  NOT NULL COMMENT '入库总金额',
    `remain_amount`    decimal(18,2)  NOT NULL COMMENT '剩余金额',
    `create_user`      bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`      datetime       NOT NULL COMMENT '创建时间',
    `update_user`      bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`      datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`          bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_fifo` (`tenant_id`, `warehouse_id`, `goods_id`, `remain_quantity`, `in_date`, `id`),
    INDEX `idx_stock_alert_balance` (`tenant_id`, `deleted`, `goods_id`, `warehouse_id`, `remain_quantity`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存批次';

CREATE TABLE IF NOT EXISTS `psi_stock_flow` (
    `id`               bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`        bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`          bigint(20)     NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `warehouse_id`     bigint(20)     NOT NULL COMMENT '仓库ID',
    `goods_id`         bigint(20)     NOT NULL COMMENT '商品ID',
    `flow_type`        varchar(32)    NOT NULL COMMENT '流水类型',
    `before_qty`       decimal(18,4)  NOT NULL COMMENT '变动前数量',
    `change_qty`       decimal(18,4)  NOT NULL COMMENT '变动数量',
    `after_qty`        decimal(18,4)  NOT NULL COMMENT '变动后数量',
    `before_amount`    decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '变动前金额',
    `change_amount`    decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '变动金额',
    `after_amount`     decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '变动后金额',
    `source_type`      varchar(32)    DEFAULT NULL COMMENT '来源类型',
    `source_id`        bigint(20)     DEFAULT NULL COMMENT '来源单据ID',
    `source_no`        varchar(64)    DEFAULT NULL COMMENT '来源单据号',
    `confirm_user`     bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`     datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`      bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`      datetime       NOT NULL COMMENT '创建时间',
    `update_user`      bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`      datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`          bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_wh_goods` (`tenant_id`, `warehouse_id`, `goods_id`),
    INDEX `idx_source` (`tenant_id`, `source_no`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存流水';

CREATE TABLE IF NOT EXISTS `psi_stock_in` (
    `id`                bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`         bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`            varchar(32)    NOT NULL COMMENT '入库单号',
    `doc_status`        varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `in_type`           varchar(16)    NOT NULL COMMENT 'PURCHASE/OTHER',
    `in_date`           datetime       NOT NULL COMMENT '入库日期',
    `warehouse_id`      bigint(20)     NOT NULL COMMENT '仓库ID',
    `unit_id`           bigint(20)     DEFAULT NULL COMMENT '供应商ID',
    `contract_id`       bigint(20)     DEFAULT NULL COMMENT '采购合同ID',
    `tax_inclusive_amount` decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '含税总金额',
    `untax_amount`      decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '未税总金额',
    `tax_amount`        decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '税额总金额',
    `tax_diff_amount`   decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '价税尾差',
    `pay_status`        varchar(16)    NOT NULL DEFAULT 'UNPAID' COMMENT '支付状态',
    `invoice_status`    varchar(16)    NOT NULL DEFAULT 'UNINVOICED' COMMENT '开票状态',
    `idempotent_key`    varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `remark`            varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`      bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`      datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`       bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`       datetime       NOT NULL COMMENT '创建时间',
    `update_user`       bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`       datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`           bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_stock_in_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_stock_in_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购入库单';

CREATE TABLE IF NOT EXISTS `psi_stock_in_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `stock_in_id`          bigint(20)     NOT NULL COMMENT '入库单ID',
    `goods_id`             bigint(20)     NOT NULL COMMENT '商品ID',
    `quantity`             decimal(18,4)  NOT NULL COMMENT '数量',
    `tax_inclusive_price`  decimal(18,6)  NOT NULL COMMENT '含税单价',
    `tax_rate`             decimal(5,2)   NOT NULL COMMENT '税率',
    `untax_price`          decimal(18,6)  NOT NULL COMMENT '未税单价',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_stock_in` (`stock_in_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购入库明细';

CREATE TABLE IF NOT EXISTS `psi_stock_out` (
  `repair_order_id` bigint DEFAULT NULL,
  `repair_settlement_id` bigint DEFAULT NULL,
  KEY `idx_stock_out_repair` (`repair_order_id`, `doc_status`),
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`               varchar(32)    NOT NULL COMMENT '出库单号',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `out_type`             varchar(16)    NOT NULL COMMENT 'SALE/OTHER/REPAIR',
    `out_date`             datetime       NOT NULL COMMENT '出库日期',
    `warehouse_id`         bigint(20)     NOT NULL COMMENT '仓库ID',
    `unit_id`              bigint(20)     DEFAULT NULL COMMENT '客户ID',
    `contract_id`          bigint(20)     DEFAULT NULL COMMENT '销售合同ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '含税总金额',
    `untax_amount`         decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '未税总金额',
    `tax_amount`           decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '税额总金额',
    `tax_diff_amount`      decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '价税尾差',
    `cost_amount`          decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '出库总成本',
    `invoice_status`       varchar(16)    NOT NULL DEFAULT 'UNINVOICED' COMMENT '开票状态',
    `idempotent_key`       varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`         bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`         datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_stock_out_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_stock_out_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销售出库单';

CREATE TABLE IF NOT EXISTS `psi_stock_out_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `stock_out_id`         bigint(20)     NOT NULL COMMENT '出库单ID',
    `goods_id`             bigint(20)     NOT NULL COMMENT '商品ID',
    `quantity`             decimal(18,4)  NOT NULL COMMENT '数量',
    `tax_inclusive_price`  decimal(18,6)  NOT NULL COMMENT '含税单价',
    `tax_rate`             decimal(5,2)   NOT NULL COMMENT '税率',
    `untax_price`          decimal(18,6)  NOT NULL COMMENT '未税单价',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `cost_amount`          decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '出库成本快照',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_stock_out` (`stock_out_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销售出库明细';

CREATE TABLE IF NOT EXISTS `psi_stock_out_batch` (
    `id`               bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`        bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `stock_out_item_id` bigint(20)    NOT NULL COMMENT '出库明细ID',
    `batch_id`         bigint(20)     NOT NULL COMMENT '批次ID',
    `quantity`         decimal(18,4)  NOT NULL COMMENT '消耗数量',
    `untax_price`      decimal(18,6)  NOT NULL COMMENT '未税单位成本快照',
    `cost_amount`      decimal(18,2)  NOT NULL COMMENT '消耗成本',
    `create_user`      bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`      datetime       NOT NULL COMMENT '创建时间',
    `update_user`      bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`      datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`          bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_out_item` (`stock_out_item_id`),
    INDEX `idx_batch` (`batch_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出库批次消耗明细';

CREATE TABLE IF NOT EXISTS `psi_receivable` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `unit_id`              bigint(20)     NOT NULL COMMENT '客户ID',
    `source_type`          varchar(32)    NOT NULL COMMENT '来源类型',
    `source_id`            bigint(20)     NOT NULL COMMENT '来源单据ID',
    `source_no`            varchar(64)    NOT NULL COMMENT '来源单据号',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税总额',
    `written_off_amount`   decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '已核销额',
    `unwritten_amount`     decimal(18,2)  NOT NULL COMMENT '未核销额',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_ar_source` (`tenant_id`, `source_type`, `source_id`, `deleted`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应收开口台账';

CREATE TABLE IF NOT EXISTS `psi_payable` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `unit_id`              bigint(20)     NOT NULL COMMENT '供应商ID',
    `source_type`          varchar(32)    NOT NULL COMMENT '来源类型',
    `source_id`            bigint(20)     NOT NULL COMMENT '来源单据ID',
    `source_no`            varchar(64)    NOT NULL COMMENT '来源单据号',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税总额',
    `written_off_amount`   decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '已核销额',
    `unwritten_amount`     decimal(18,2)  NOT NULL COMMENT '未核销额',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_ap_source` (`tenant_id`, `source_type`, `source_id`, `deleted`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应付开口台账';

-- ---------- 字典 ----------
INSERT INTO `sys_dict`
(`id`, `name`, `code`, `description`, `is_system`, `create_user`, `create_time`)
VALUES
(100, 'PSI税率', 'psi_tax_rate', '进销存税率', b'1', 1, NOW()),
(101, 'PSI往来单位类型', 'psi_unit_type', NULL, b'1', 1, NOW()),
(102, 'PSI单据状态', 'psi_doc_status', NULL, b'1', 1, NOW()),
(103, 'PSI入库类型', 'psi_stock_in_type', NULL, b'1', 1, NOW()),
(104, 'PSI出库类型', 'psi_stock_out_type', NULL, b'1', 1, NOW()),
(105, 'PSI来源类型', 'psi_source_type', '库存流水/应收应付来源', b'1', 1, NOW()),
(106, 'PSI流水类型', 'psi_flow_type', '库存流水变动类型', b'1', 1, NOW()),
(107, 'PSI发票类型', 'psi_invoice_type', '专票/普票', b'1', 1, NOW()),
(108, 'PSI退货类型', 'psi_return_type', '销售退货/采购退货', b'1', 1, NOW()),
(109, 'PSI打印业务类型', 'psi_print_biz_type', '打印模板业务类型', b'1', 1, NOW()),
(113, 'PSI开口状态', 'psi_open_status', '应收应付结清状态', b'1', 1, NOW()),
(200, 'EOMS报修状态', 'eoms_apply_status', NULL, b'1', 1, NOW()),
(201, 'EOMS工单状态', 'eoms_order_status', NULL, b'1', 1, NOW()),
(202, 'EOMS紧急程度', 'eoms_urgent_level', NULL, b'1', 1, NOW()),
(203, 'EOMS故障类型', 'eoms_fault_type', NULL, b'1', 1, NOW()),
(204, 'EOMS报价状态', 'eoms_quote_status', NULL, b'1', 1, NOW()),
(205, 'EOMS应收状态', 'eoms_receivable_status', NULL, b'1', 1, NOW()),
(206, 'EOMS结算状态', 'eoms_settle_status', NULL, b'1', 1, NOW()),
(207, 'EOMS客户类型', 'eoms_customer_type', NULL, b'1', 1, NOW()),
(208, 'EOMS仓库类型', 'eoms_warehouse_type', NULL, b'1', 1, NOW()),
-- (209) eoms_inbound_status 已随 M3 下线，见末尾 DELETE
(210, 'EOMS合同状态', 'eoms_contract_status', NULL, b'1', 1, NOW()),
(211, 'EOMS收款方式', 'eoms_payment_method', NULL, b'1', 1, NOW()),
(212, 'EOMS启用状态', 'eoms_enable_status', '0正常 1停用', b'1', 1, NOW()),
(213, 'EOMS报修来源', 'eoms_apply_source', NULL, b'1', 1, NOW()),
(300, '签章印章类型', 'sign_seal_category', NULL, b'1', 1, NOW()),
(301, '签章项目状态', 'sign_project_status', NULL, b'1', 1, NOW()),
(302, '签章合同类型', 'sign_contract_type', NULL, b'1', 1, NOW()),
(303, '签章方式', 'sign_mode', NULL, b'1', 1, NOW()),
(304, '签章审计动作', 'sign_audit_action', NULL, b'1', 1, NOW()),
(305, '签章提取类型', 'sign_seal_extract_category', NULL, b'1', 1, NOW()),
(306, '签章印章状态', 'sign_seal_status', '0启用 1禁用', b'1', 1, NOW()),
(307, '签章记录状态', 'sign_record_status', NULL, b'1', 1, NOW());

INSERT INTO `sys_dict_item`
(`id`, `label`, `value`, `color`, `sort`, `description`, `status`, `dict_id`, `create_user`, `create_time`)
VALUES
(100, '13%', '13', 'primary', 1, NULL, 1, 100, 1, NOW()),
(101, '9%', '9', 'primary', 2, NULL, 1, 100, 1, NOW()),
(102, '6%', '6', 'primary', 3, NULL, 1, 100, 1, NOW()),
(103, '3%', '3', 'primary', 4, NULL, 1, 100, 1, NOW()),
(104, '1%', '1', 'primary', 5, NULL, 1, 100, 1, NOW()),
(110, '客户', 'CUSTOMER', 'primary', 1, NULL, 1, 101, 1, NOW()),
(111, '供应商', 'SUPPLIER', 'success', 2, NULL, 1, 101, 1, NOW()),
(112, '双重身份', 'BOTH', 'warning', 3, NULL, 1, 101, 1, NOW()),
(120, '草稿', 'DRAFT', 'default', 1, NULL, 1, 102, 1, NOW()),
(121, '已确认', 'CONFIRMED', 'success', 2, NULL, 1, 102, 1, NOW()),
(122, '已作废', 'VOID', 'error', 3, NULL, 1, 102, 1, NOW()),
(130, '采购入库', 'PURCHASE', 'primary', 1, NULL, 1, 103, 1, NOW()),
(131, '其他入库', 'OTHER', 'warning', 2, NULL, 1, 103, 1, NOW()),
(140, '销售出库', 'SALE', 'primary', 1, NULL, 1, 104, 1, NOW()),
(141, '其他出库', 'OTHER', 'warning', 2, NULL, 1, 104, 1, NOW()),
(142, '维修出库', 'REPAIR', 'success', 3, 'EOMS 维修结算出库', 1, 104, 1, NOW()),
(150, '采购入库', 'STOCK_IN', 'primary', 1, NULL, 1, 105, 1, NOW()),
(151, '销售出库', 'STOCK_OUT', 'success', 2, NULL, 1, 105, 1, NOW()),
(152, '销售退货', 'SALE_RETURN', 'warning', 3, NULL, 1, 105, 1, NOW()),
(153, '采购退货', 'PURCHASE_RETURN', 'warning', 4, NULL, 1, 105, 1, NOW()),
(154, '应收期初', 'INIT_AR', 'default', 5, NULL, 1, 105, 1, NOW()),
(155, '应付期初', 'INIT_AP', 'default', 6, NULL, 1, 105, 1, NOW()),
(156, '期初库存', 'INIT_STOCK', 'default', 7, NULL, 1, 105, 1, NOW()),
(157, '预付期初', 'INIT_PREPAY', 'default', 8, NULL, 1, 105, 1, NOW()),
(158, '仓间调拨', 'TRANSFER', 'primary', 9, NULL, 1, 105, 1, NOW()),
(159, '盘点', 'STOCK_CHECK', 'warning', 10, NULL, 1, 105, 1, NOW()),
(160, '库存调整', 'STOCK_ADJUST', 'warning', 11, NULL, 1, 105, 1, NOW()),
(161, '回款', 'RECEIPT', 'success', 12, NULL, 1, 105, 1, NOW()),
(162, '付款', 'PAYMENT', 'error', 13, NULL, 1, 105, 1, NOW()),
(163, '销项发票', 'INVOICE_OUT', 'primary', 14, NULL, 1, 105, 1, NOW()),
(164, '进项发票', 'INVOICE_IN', 'primary', 15, NULL, 1, 105, 1, NOW()),
(165, '销售合同', 'CONTRACT_SALE', 'primary', 16, NULL, 1, 105, 1, NOW()),
(166, '采购合同', 'CONTRACT_PURCHASE', 'success', 17, NULL, 1, 105, 1, NOW()),
(170, '入库', 'IN', 'success', 1, NULL, 1, 106, 1, NOW()),
(171, '出库', 'OUT', 'error', 2, NULL, 1, 106, 1, NOW()),
(172, '调拨入', 'TRANSFER_IN', 'primary', 3, NULL, 1, 106, 1, NOW()),
(173, '调拨出', 'TRANSFER_OUT', 'warning', 4, NULL, 1, 106, 1, NOW()),
(174, '盘盈', 'PROFIT', 'success', 5, NULL, 1, 106, 1, NOW()),
(175, '盘亏', 'LOSS', 'error', 6, NULL, 1, 106, 1, NOW()),
(176, '调整', 'ADJUST', 'warning', 7, NULL, 1, 106, 1, NOW()),
(180, '专票', 'SPECIAL', 'primary', 1, NULL, 1, 107, 1, NOW()),
(181, '普票', 'NORMAL', 'default', 2, NULL, 1, 107, 1, NOW()),
(182, '销售退货', 'SALE_RETURN', 'warning', 1, NULL, 1, 108, 1, NOW()),
(183, '采购退货', 'PURCHASE_RETURN', 'primary', 2, NULL, 1, 108, 1, NOW()),
(184, '入库单', 'STOCK_IN', 'primary', 1, NULL, 1, 109, 1, NOW()),
(185, '出库单', 'STOCK_OUT', 'success', 2, NULL, 1, 109, 1, NOW()),
(186, '未结清', 'OPEN', 'warning', 1, NULL, 1, 113, 1, NOW()),
(187, '已结清', 'CLOSED', 'success', 2, NULL, 1, 113, 1, NOW()),
(400, '待处理', 'WAIT', 'warning', 1, NULL, 1, 200, 1, NOW()),
(401, '已确认', 'CONFIRM', 'primary', 2, NULL, 1, 200, 1, NOW()),
(402, '已转工单', 'ORDER', 'success', 3, NULL, 1, 200, 1, NOW()),
(403, '已取消', 'CANCEL', 'default', 4, NULL, 1, 200, 1, NOW()),
(410, '待接单', 'WAIT_ACCEPT', 'warning', 1, NULL, 1, 201, 1, NOW()),
(411, '已接单', 'ACCEPTED', 'primary', 2, NULL, 1, 201, 1, NOW()),
(412, '维修中', 'PROCESSING', 'success', 3, NULL, 1, 201, 1, NOW()),
(413, '待结算', 'WAIT_SETTLEMENT', 'warning', 4, NULL, 1, 201, 1, NOW()),
(414, '已完成', 'FINISHED', 'success', 5, NULL, 1, 201, 1, NOW()),
(415, '已关闭', 'CLOSED', 'default', 6, NULL, 1, 201, 1, NOW()),
(420, '一般', 'NORMAL', 'success', 1, NULL, 1, 202, 1, NOW()),
(421, '紧急', 'URGENT', 'warning', 2, NULL, 1, 202, 1, NOW()),
(422, '特急', 'VERY_URGENT', 'error', 3, NULL, 1, 202, 1, NOW()),
(430, '电脑设备故障', '电脑设备故障', 'primary', 1, NULL, 1, 203, 1, NOW()),
(431, '监控设备故障', '监控设备故障', 'primary', 2, NULL, 1, 203, 1, NOW()),
(432, '办公设备故障', '办公设备故障', 'primary', 3, NULL, 1, 203, 1, NOW()),
(433, '通用基础故障', '通用基础故障', 'default', 4, NULL, 1, 203, 1, NOW()),
(440, '草稿', 'DRAFT', 'default', 1, NULL, 1, 204, 1, NOW()),
(441, '已提交', 'SUBMIT', 'primary', 2, NULL, 1, 204, 1, NOW()),
(442, '已确认', 'CONFIRM', 'success', 3, NULL, 1, 204, 1, NOW()),
(443, '已取消', 'CANCEL', 'error', 4, NULL, 1, 204, 1, NOW()),
(450, '未收款', 'UNPAID', 'warning', 1, NULL, 1, 205, 1, NOW()),
(451, '部分收款', 'PARTIAL', 'primary', 2, NULL, 1, 205, 1, NOW()),
(452, '已结清', 'PAID', 'success', 3, NULL, 1, 205, 1, NOW()),
(460, '草稿', 'DRAFT', 'default', 1, NULL, 1, 206, 1, NOW()),
(461, '已确认', 'CONFIRMED', 'success', 2, NULL, 1, 206, 1, NOW()),
(470, '企业客户', 'ENTERPRISE', 'primary', 1, NULL, 1, 207, 1, NOW()),
(471, '个人客户', 'PERSON', 'success', 2, NULL, 1, 207, 1, NOW()),
(480, '主仓', 'MAIN', 'primary', 1, NULL, 1, 208, 1, NOW()),
(481, '备件仓', 'SPARE', 'warning', 2, NULL, 1, 208, 1, NOW()),
-- (490)(491) eoms_inbound_status 字典项已随 M3 下线
(500, '生效中', 'EFFECTIVE', 'success', 1, NULL, 1, 210, 1, NOW()),
(501, '已到期', 'EXPIRED', 'warning', 2, NULL, 1, 210, 1, NOW()),
(502, '已终止', 'TERMINATED', 'error', 3, NULL, 1, 210, 1, NOW()),
(510, '现金', '现金', 'success', 1, NULL, 1, 211, 1, NOW()),
(511, '转账', '转账', 'primary', 2, NULL, 1, 211, 1, NOW()),
(512, '微信', '微信', 'success', 3, NULL, 1, 211, 1, NOW()),
(513, '支付宝', '支付宝', 'primary', 4, NULL, 1, 211, 1, NOW()),
(520, '正常', '0', 'success', 1, NULL, 1, 212, 1, NOW()),
(521, '停用', '1', 'error', 2, NULL, 1, 212, 1, NOW()),
(530, '客户H5', 'H5', 'primary', 1, NULL, 1, 213, 1, NOW()),
(531, '后台登记', 'ADMIN', 'default', 2, NULL, 1, 213, 1, NOW()),
(600, '法定代表人章', 'legal_rep', 'error', 1, NULL, 1, 300, 1, NOW()),
(601, '合同章', 'contract', 'primary', 2, NULL, 1, 300, 1, NOW()),
(602, '财务章', 'finance', 'warning', 3, NULL, 1, 300, 1, NOW()),
(603, '公司公章', 'company', 'success', 4, NULL, 1, 300, 1, NOW()),
(610, '待审核', '0', 'warning', 1, NULL, 1, 301, 1, NOW()),
(611, '未开始', '1', 'default', 2, NULL, 1, 301, 1, NOW()),
(612, '进行中', '2', 'primary', 3, NULL, 1, 301, 1, NOW()),
(613, '已完成', '3', 'success', 4, NULL, 1, 301, 1, NOW()),
(614, '已暂停', '4', 'warning', 5, NULL, 1, 301, 1, NOW()),
(615, '已取消', '5', 'error', 6, NULL, 1, 301, 1, NOW()),
(620, '销售合同', 'SALE', 'primary', 1, NULL, 1, 302, 1, NOW()),
(621, '采购合同', 'PURCHASE', 'success', 2, NULL, 1, 302, 1, NOW()),
(622, '维修合同', 'REPAIR', 'warning', 3, NULL, 1, 302, 1, NOW()),
(630, '单次签章', 'single', 'primary', 1, NULL, 1, 303, 1, NOW()),
(631, '连续签章', 'continuous', 'success', 2, NULL, 1, 303, 1, NOW()),
(632, '跨页签章', 'span', 'warning', 3, NULL, 1, 303, 1, NOW()),
(633, '固定位置', 'fixed', 'default', 4, NULL, 1, 303, 1, NOW()),
(634, '合并签章', 'unified', 'primary', 5, NULL, 1, 303, 1, NOW()),
(640, '登录', 'LOGIN', 'primary', 1, NULL, 1, 304, 1, NOW()),
(641, '登出', 'LOGOUT', 'default', 2, NULL, 1, 304, 1, NOW()),
(642, '新增签章', 'SIGN_ADD', 'success', 3, NULL, 1, 304, 1, NOW()),
(643, '删除签章', 'SIGN_DELETE', 'error', 4, NULL, 1, 304, 1, NOW()),
(644, '下载签章', 'SIGN_DOWNLOAD', 'primary', 5, NULL, 1, 304, 1, NOW()),
(645, '授权', 'AUTH', 'warning', 6, NULL, 1, 304, 1, NOW()),
(646, '指纹校验', 'FINGERPRINT', 'success', 7, NULL, 1, 304, 1, NOW()),
(650, '公章', '公章', 'success', 1, NULL, 1, 305, 1, NOW()),
(651, '合同章', '合同章', 'primary', 2, NULL, 1, 305, 1, NOW()),
(652, '财务章', '财务章', 'warning', 3, NULL, 1, 305, 1, NOW()),
(653, '法人章', '法人章', 'error', 4, NULL, 1, 305, 1, NOW()),
(660, '启用', '0', 'success', 1, NULL, 1, 306, 1, NOW()),
(661, '禁用', '1', 'error', 2, NULL, 1, 306, 1, NOW()),
(662, '成功', 'completed', 'success', 1, NULL, 1, 307, 1, NOW()),
(663, '失败', 'failed', 'error', 2, NULL, 1, 307, 1, NOW());

-- ---------- 菜单（10000 段）：两层展示（四大模块为顶层 Layout，页面为第二层） ----------
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
VALUES
(10001, '基础数据', 0, 1, '/psi/master', 'PsiMaster', 'Layout', '/psi/unit', 'folder', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()),
(10002, '库存业务', 0, 1, '/psi/stock', 'PsiStock', 'Layout', '/psi/goods-category', 'archive', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()),
(10003, '财务管理', 0, 1, '/psi/finance', 'PsiFinance', 'Layout', '/psi/receipt', 'stamp', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()),
(10004, '合同管理', 0, 1, '/psi/contract', 'PsiContract', 'Layout', '/psi/contract-sale', 'file', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()),
(10010, '往来单位', 10001, 2, '/psi/unit', 'PsiUnit', 'psi/unit/index', NULL, 'user-group', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()),
(10011, '列表', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:list', 1, 1, 1, NOW()),
(10012, '详情', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:get', 2, 1, 1, NOW()),
(10013, '新增', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:create', 3, 1, 1, NOW()),
(10014, '修改', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:update', 4, 1, 1, NOW()),
(10015, '删除', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:delete', 5, 1, 1, NOW()),
(10016, '导出', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:export', 6, 1, 1, NOW()),
(10017, '导入', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:import', 7, 1, 1, NOW()),
(10020, '商品档案', 10002, 2, '/psi/goods', 'PsiGoods', 'psi/goods/index', NULL, 'tags', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()),
(10021, '列表', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:list', 1, 1, 1, NOW()),
(10022, '详情', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:get', 2, 1, 1, NOW()),
(10023, '新增', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:create', 3, 1, 1, NOW()),
(10024, '修改', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:update', 4, 1, 1, NOW()),
(10025, '删除', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:delete', 5, 1, 1, NOW()),
(10026, '导出', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:export', 6, 1, 1, NOW()),
(10027, '导入', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:import', 7, 1, 1, NOW()),
(10310, '商品分类', 10002, 2, '/psi/goods-category', 'PsiGoodsCategory', 'psi/goods-category/index', NULL, 'mind-mapping', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()),
(10311, '列表', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:list', 1, 1, 1, NOW()),
(10312, '详情', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:get', 2, 1, 1, NOW()),
(10313, '新增', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:create', 3, 1, 1, NOW()),
(10314, '修改', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:update', 4, 1, 1, NOW()),
(10315, '删除', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:delete', 5, 1, 1, NOW()),
(10316, '导出', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:export', 6, 1, 1, NOW()),
(10317, '导入', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:import', 7, 1, 1, NOW()),
(10030, '仓库管理', 10002, 2, '/psi/warehouse', 'PsiWarehouse', 'psi/warehouse/index', NULL, 'location', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()),
(10031, '列表', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:list', 1, 1, 1, NOW()),
(10032, '详情', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:get', 2, 1, 1, NOW()),
(10033, '新增', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:create', 3, 1, 1, NOW()),
(10034, '修改', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:update', 4, 1, 1, NOW()),
(10035, '删除', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:delete', 5, 1, 1, NOW()),
(10036, '导出', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:export', 6, 1, 1, NOW()),
(10037, '导入', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:import', 7, 1, 1, NOW()),
(10210, '库存查询', 10002, 2, '/psi/stock-query', 'PsiStockQuery', 'psi/stock-query/index', NULL, 'search', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()),
(10211, '列表', 10210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockQuery:list', 1, 1, 1, NOW()),
(10080, '期初建账', 10001, 2, '/psi/init', 'PsiInit', 'psi/init/index', NULL, 'calendar', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()),
(10081, '列表', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:list', 1, 1, 1, NOW()),
(10082, '确认', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:confirm', 2, 1, 1, NOW()),
(10083, '导入', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:import', 3, 1, 1, NOW());

-- ----- patch_psi_return_transfer.sql -----
-- validCheckSum: ANY

CREATE TABLE IF NOT EXISTS `psi_stock_return` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`               varchar(32)    NOT NULL COMMENT '退货单号',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `return_type`          varchar(32)    NOT NULL COMMENT 'SALE_RETURN/PURCHASE_RETURN',
    `return_date`          datetime       NOT NULL COMMENT '退货日期',
    `warehouse_id`         bigint(20)     NOT NULL COMMENT '仓库ID',
    `unit_id`              bigint(20)     DEFAULT NULL COMMENT '往来单位ID',
    `origin_stock_out_id`  bigint(20)     DEFAULT NULL COMMENT '原出库单ID',
    `origin_stock_in_id`   bigint(20)     DEFAULT NULL COMMENT '原入库单ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '含税总金额',
    `untax_amount`         decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '未税总金额',
    `tax_amount`           decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '税额',
    `cost_amount`          decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本金额',
    `idempotent_key`       varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`         bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`         datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_return_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_return_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_origin_out` (`origin_stock_out_id`),
    INDEX `idx_origin_in` (`origin_stock_in_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退货单';

CREATE TABLE IF NOT EXISTS `psi_stock_return_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `return_id`            bigint(20)     NOT NULL COMMENT '退货单ID',
    `origin_item_id`       bigint(20)     NOT NULL COMMENT '原出入库明细ID',
    `goods_id`             bigint(20)     NOT NULL COMMENT '商品ID',
    `quantity`             decimal(18,4)  NOT NULL COMMENT '退货数量',
    `tax_inclusive_price`  decimal(18,6)  NOT NULL COMMENT '含税单价',
    `tax_rate`             decimal(5,2)   NOT NULL COMMENT '税率',
    `untax_price`          decimal(18,6)  NOT NULL COMMENT '未税单价',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `cost_amount`          decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_return` (`return_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退货明细';

CREATE TABLE IF NOT EXISTS `psi_stock_transfer` (
    `id`                 bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`          bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`             varchar(32)   NOT NULL COMMENT '调拨单号',
    `doc_status`         varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `transfer_date`      datetime      NOT NULL COMMENT '调拨日期',
    `from_warehouse_id`  bigint(20)    NOT NULL COMMENT '调出仓',
    `to_warehouse_id`    bigint(20)    NOT NULL COMMENT '调入仓',
    `idempotent_key`     varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`             varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`       bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`       datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`        bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`        datetime      NOT NULL COMMENT '创建时间',
    `update_user`        bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`        datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`            bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_transfer_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_transfer_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='仓间调拨单';

CREATE TABLE IF NOT EXISTS `psi_stock_transfer_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `transfer_id`  bigint(20)     NOT NULL COMMENT '调拨单ID',
    `goods_id`     bigint(20)     NOT NULL COMMENT '商品ID',
    `quantity`     decimal(18,4)  NOT NULL COMMENT '数量',
    `cost_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_transfer` (`transfer_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='调拨明细';

CREATE TABLE IF NOT EXISTS `psi_todo` (
    `id`           bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `todo_type`    varchar(32)   NOT NULL COMMENT '待办类型',
    `title`        varchar(200)  NOT NULL COMMENT '标题',
    `biz_type`     varchar(32)   DEFAULT NULL COMMENT '业务类型',
    `biz_id`       bigint(20)    DEFAULT NULL COMMENT '业务ID',
    `biz_no`       varchar(64)   DEFAULT NULL COMMENT '业务单号',
    `done`         tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否完成',
    `create_user`  bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`  datetime      NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_todo_open` (`tenant_id`, `done`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='PSI待办';

INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10060, '退货管理', 10002, 2, '/psi/stock-return', 'PsiStockReturn', 'psi/stock-return/index', NULL, 'reply', b'0', b'0', b'0', NULL, 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10060);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10061, '列表', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:list', 1, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10061);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10062, '详情', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:get', 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10062);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10063, '新增', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:create', 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10063);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10064, '修改', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:update', 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10064);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10065, '删除', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:delete', 5, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10065);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10066, '确认', 10060, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockReturn:confirm', 6, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10066);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10070, '仓间调拨', 10002, 2, '/psi/stock-transfer', 'PsiStockTransfer', 'psi/stock-transfer/index', NULL, 'swap', b'0', b'0', b'0', NULL, 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10070);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10071, '列表', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:list', 1, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10071);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10072, '详情', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:get', 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10072);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10073, '新增', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:create', 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10073);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10074, '修改', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:update', 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10074);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10075, '删除', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:delete', 5, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10075);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10076, '确认', 10070, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockTransfer:confirm', 6, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10076);

-- ----- patch_psi_remaining_modules.sql -----
-- validCheckSum: ANY

-- ========== 期初 ==========
CREATE TABLE IF NOT EXISTS `psi_init_stock` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`        varchar(32)   NOT NULL COMMENT '单号',
    `doc_status`    varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `init_date`     datetime      NOT NULL COMMENT '期初日期',
    `warehouse_id`  bigint(20)    NOT NULL COMMENT '仓库ID',
    `idempotent_key` varchar(64)  DEFAULT NULL COMMENT '幂等键',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`  bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`  datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_init_stock_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_init_stock_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存期初';

CREATE TABLE IF NOT EXISTS `psi_init_stock_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `init_id`      bigint(20)     NOT NULL COMMENT '期初单ID',
    `goods_id`     bigint(20)     NOT NULL COMMENT '商品ID',
    `quantity`     decimal(18,4)  NOT NULL COMMENT '数量',
    `untax_price`  decimal(18,6)  NOT NULL COMMENT '未税成本单价',
    `untax_amount` decimal(18,2)  NOT NULL COMMENT '未税金额',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_init` (`init_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存期初明细';

CREATE TABLE IF NOT EXISTS `psi_init_receivable` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`         varchar(32)   NOT NULL COMMENT '单号',
    `doc_status`     varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `init_date`      datetime      NOT NULL COMMENT '期初日期',
    `idempotent_key` varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`   bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`   datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_init_ar_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_init_ar_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应收期初';

CREATE TABLE IF NOT EXISTS `psi_init_receivable_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `init_id`              bigint(20)     NOT NULL COMMENT '期初单ID',
    `unit_id`              bigint(20)     NOT NULL COMMENT '客户ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_init` (`init_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应收期初明细';

CREATE TABLE IF NOT EXISTS `psi_init_payable` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`         varchar(32)   NOT NULL COMMENT '单号',
    `doc_status`     varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `init_date`      datetime      NOT NULL COMMENT '期初日期',
    `idempotent_key` varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`   bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`   datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_init_ap_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_init_ap_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应付期初';

CREATE TABLE IF NOT EXISTS `psi_init_payable_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `init_id`              bigint(20)     NOT NULL COMMENT '期初单ID',
    `unit_id`              bigint(20)     NOT NULL COMMENT '供应商ID',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_init` (`init_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应付期初明细';

CREATE TABLE IF NOT EXISTS `psi_init_prepay` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`         varchar(32)   NOT NULL COMMENT '单号',
    `doc_status`     varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `init_date`      datetime      NOT NULL COMMENT '期初日期',
    `idempotent_key` varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`   bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`   datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_init_pre_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_init_pre_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预付期初';

CREATE TABLE IF NOT EXISTS `psi_init_prepay_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `init_id`      bigint(20)     NOT NULL COMMENT '期初单ID',
    `unit_id`      bigint(20)     NOT NULL COMMENT '供应商ID',
    `amount`       decimal(18,2)  NOT NULL COMMENT '预付金额',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_init` (`init_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预付期初明细';

-- ========== 盘点 / 调整 ==========
CREATE TABLE IF NOT EXISTS `psi_stock_check` (
    `id`                bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`         bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`            varchar(32)   NOT NULL COMMENT '盘点单号',
    `doc_status`        varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `check_date`        datetime      NOT NULL COMMENT '盘点日期',
    `warehouse_id`      bigint(20)    NOT NULL COMMENT '仓库ID',
    `snapshot_version`  bigint(20)    NOT NULL DEFAULT 0 COMMENT '账面快照版本',
    `idempotent_key`    varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`            varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`      bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`      datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`       bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`       datetime      NOT NULL COMMENT '创建时间',
    `update_user`       bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`       datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`           bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_check_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_check_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存盘点';

CREATE TABLE IF NOT EXISTS `psi_stock_check_item` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `check_id`     bigint(20)     NOT NULL COMMENT '盘点单ID',
    `goods_id`     bigint(20)     NOT NULL COMMENT '商品ID',
    `book_qty`     decimal(18,4)  NOT NULL DEFAULT 0 COMMENT '账面数量',
    `check_qty`    decimal(18,4)  NOT NULL COMMENT '实盘数量',
    `diff_qty`     decimal(18,4)  NOT NULL COMMENT '差异数量',
    `untax_price`  decimal(18,6)  DEFAULT NULL COMMENT '盘盈未税单价',
    `cost_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_check` (`check_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存盘点明细';

CREATE TABLE IF NOT EXISTS `psi_stock_adjust` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`         varchar(32)   NOT NULL COMMENT '调整单号',
    `doc_status`     varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `adjust_date`    datetime      NOT NULL COMMENT '调整日期',
    `warehouse_id`   bigint(20)    NOT NULL COMMENT '仓库ID',
    `idempotent_key` varchar(64)   DEFAULT NULL COMMENT '幂等键',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `confirm_user`   bigint(20)    DEFAULT NULL COMMENT '确认人',
    `confirm_time`   datetime      DEFAULT NULL COMMENT '确认时间',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_adjust_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_adjust_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存调整';

CREATE TABLE IF NOT EXISTS `psi_stock_adjust_item` (
    `origin_batch_id` bigint DEFAULT NULL COMMENT '显式盘盈纠正的原批次',
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `adjust_id`    bigint(20)     NOT NULL COMMENT '调整单ID',
    `goods_id`     bigint(20)     NOT NULL COMMENT '商品ID',
    `adjust_type`  varchar(16)    NOT NULL COMMENT 'PROFIT/LOSS',
    `quantity`     decimal(18,4)  NOT NULL COMMENT '数量',
    `untax_price`  decimal(18,6)  DEFAULT NULL COMMENT '盘盈未税单价',
    `cost_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '成本',
    `sort`         int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_adjust` (`adjust_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存调整明细';

-- ========== 银行账户 / 收付 / 核销 ==========
CREATE TABLE IF NOT EXISTS `psi_company_account` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `account_name`  varchar(128)  NOT NULL COMMENT '户名',
    `bank_name`     varchar(128)  NOT NULL COMMENT '开户行',
    `account_no`    varchar(64)   NOT NULL COMMENT '账号',
    `status`        tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_account_no` (`tenant_id`, `account_no`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公司银行账户';

CREATE TABLE IF NOT EXISTS `psi_receipt` (
  `operation_sequence` bigint NOT NULL DEFAULT 0,
  `source_sequence` bigint DEFAULT NULL,
  `void_sequence` bigint DEFAULT NULL,
  `refunded_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `refund_source_id` bigint DEFAULT NULL,
  `refund_return_id` bigint DEFAULT NULL,
  `void_reason` varchar(500) DEFAULT NULL,
  `void_user` bigint DEFAULT NULL,
  `void_time` datetime DEFAULT NULL,
  KEY `idx_receipt_refund_return` (`refund_return_id`, `doc_status`),
  KEY `idx_receipt_refund_source` (`refund_source_id`),
    `id`                  bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`           bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`              varchar(32)    NOT NULL COMMENT '回款单号',
    `doc_status`          varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `receipt_date`        datetime       NOT NULL COMMENT '回款日期',
    `unit_id`             bigint(20)     NOT NULL COMMENT '客户ID',
    `account_id`          bigint(20)     DEFAULT NULL COMMENT '收款账户',
    `pay_method`          varchar(32)    DEFAULT NULL COMMENT '收款方式',
    `amount`              decimal(18,2)  NOT NULL COMMENT '金额',
    `unallocated_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '未分配金额',
    `idempotent_key`      varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `remark`              varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`        bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`        datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`         bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`         datetime       NOT NULL COMMENT '创建时间',
    `update_user`         bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`         datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`             bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_receipt_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_receipt_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_unit` (`unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='回款登记';

CREATE TABLE IF NOT EXISTS `psi_payment` (
  `operation_sequence` bigint NOT NULL DEFAULT 0,
  `source_sequence` bigint DEFAULT NULL,
  `void_sequence` bigint DEFAULT NULL,
  `refunded_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `refund_source_id` bigint DEFAULT NULL,
  `refund_return_id` bigint DEFAULT NULL,
  `void_reason` varchar(500) DEFAULT NULL,
  `void_user` bigint DEFAULT NULL,
  `void_time` datetime DEFAULT NULL,
  KEY `idx_payment_refund_return` (`refund_return_id`, `doc_status`),
  KEY `idx_payment_refund_source` (`refund_source_id`),
    `id`                  bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`           bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`              varchar(32)    NOT NULL COMMENT '付款单号',
    `doc_status`          varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `payment_date`        datetime       NOT NULL COMMENT '付款日期',
    `unit_id`             bigint(20)     NOT NULL COMMENT '供应商ID',
    `account_id`          bigint(20)     DEFAULT NULL COMMENT '付款账户',
    `pay_method`          varchar(32)    DEFAULT NULL COMMENT '付款方式',
    `amount`              decimal(18,2)  NOT NULL COMMENT '金额',
    `unallocated_amount`  decimal(18,2)  NOT NULL DEFAULT 0.00 COMMENT '未分配金额',
    `idempotent_key`      varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `remark`              varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`        bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`        datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`         bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`         datetime       NOT NULL COMMENT '创建时间',
    `update_user`         bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`         datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`             bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_payment_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_payment_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_unit` (`unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='付款登记';

CREATE TABLE IF NOT EXISTS `psi_write_off` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`        varchar(32)   NOT NULL COMMENT '核销单号',
    `write_off_date` datetime     NOT NULL COMMENT '核销日期',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_writeoff_no` (`tenant_id`, `doc_no`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='核销主表';

CREATE TABLE IF NOT EXISTS `psi_write_off_item` (
  `cash_sequence` bigint DEFAULT NULL,
    `id`            bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `write_off_id`  bigint(20)     NOT NULL COMMENT '核销主表ID',
    `receipt_id`    bigint(20)     DEFAULT NULL COMMENT '回款ID',
    `payment_id`    bigint(20)     DEFAULT NULL COMMENT '付款ID',
    `open_type`     varchar(8)     NOT NULL COMMENT 'AR/AP',
    `open_id`       bigint(20)     NOT NULL COMMENT '开口ID',
    `direction`     varchar(16)    NOT NULL COMMENT 'WRITE_OFF/REVERSE',
    `amount`        decimal(18,2)  NOT NULL COMMENT '金额',
    `reason`        varchar(500)   DEFAULT NULL COMMENT '原因',
    `create_user`   bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`   datetime       NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_writeoff` (`write_off_id`),
    INDEX `idx_receipt` (`receipt_id`),
    INDEX `idx_payment` (`payment_id`),
    INDEX `idx_open` (`open_type`, `open_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='核销明细';

-- ========== 发票 ==========
CREATE TABLE IF NOT EXISTS `psi_invoice_out` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`               varchar(32)    NOT NULL COMMENT '系统单号',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `invoice_no`           varchar(64)    NOT NULL COMMENT '发票号码',
    `invoice_type`         varchar(16)    NOT NULL COMMENT 'SPECIAL/NORMAL',
    `invoice_date`         datetime       NOT NULL COMMENT '开票日期',
    `unit_id`              bigint(20)     NOT NULL COMMENT '客户ID',
    `buyer_tax_no`         varchar(64)    NOT NULL COMMENT '购方税号',
    `seller_tax_no`        varchar(64)    NOT NULL COMMENT '销方税号',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `red_of_id`            bigint(20)     DEFAULT NULL COMMENT '冲红蓝字发票ID',
    `idempotent_key`       varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `file_id`              bigint(20)     DEFAULT NULL COMMENT '附件',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`         bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`         datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_inv_out_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_inv_out_biz` (`tenant_id`, `invoice_no`, `buyer_tax_no`, `seller_tax_no`, `invoice_type`, `deleted`),
    UNIQUE INDEX `uk_inv_out_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销售开票';

CREATE TABLE IF NOT EXISTS `psi_invoice_out_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `invoice_id`           bigint(20)     NOT NULL COMMENT '发票ID',
    `goods_name`           varchar(128)   NOT NULL COMMENT '货物或应税劳务',
    `quantity`             decimal(18,4)  DEFAULT NULL COMMENT '数量',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `tax_rate`             decimal(5,2)   NOT NULL COMMENT '税率',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销项发票明细';

CREATE TABLE IF NOT EXISTS `psi_invoice_out_link` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `invoice_id`   bigint(20)     NOT NULL COMMENT '发票ID',
    `stock_out_id` bigint(20)     NOT NULL COMMENT '出库单ID',
    `amount`       decimal(18,2)  NOT NULL COMMENT '关联含税金额',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_stock_out` (`stock_out_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销项发票关联出库';

CREATE TABLE IF NOT EXISTS `psi_invoice_in` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `doc_no`               varchar(32)    NOT NULL COMMENT '系统单号',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
    `invoice_no`           varchar(64)    NOT NULL COMMENT '发票号码',
    `invoice_type`         varchar(16)    NOT NULL COMMENT 'SPECIAL/NORMAL/NO_INVOICE',
    `invoice_date`         datetime       NOT NULL COMMENT '收票日期',
    `unit_id`              bigint(20)     NOT NULL COMMENT '供应商ID',
    `buyer_tax_no`         varchar(64)    NOT NULL COMMENT '购方税号',
    `seller_tax_no`        varchar(64)    NOT NULL COMMENT '销方税号',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `red_of_id`            bigint(20)     DEFAULT NULL COMMENT '冲红蓝字发票ID',
    `idempotent_key`       varchar(64)    DEFAULT NULL COMMENT '幂等键',
    `file_id`              bigint(20)     DEFAULT NULL COMMENT '附件',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `confirm_user`         bigint(20)     DEFAULT NULL COMMENT '确认人',
    `confirm_time`         datetime       DEFAULT NULL COMMENT '确认时间',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_inv_in_no` (`tenant_id`, `doc_no`, `deleted`),
    UNIQUE INDEX `uk_inv_in_biz` (`tenant_id`, `invoice_no`, `buyer_tax_no`, `seller_tax_no`, `invoice_type`, `deleted`),
    UNIQUE INDEX `uk_inv_in_idem` (`tenant_id`, `idempotent_key`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购收票';

CREATE TABLE IF NOT EXISTS `psi_invoice_in_item` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `invoice_id`           bigint(20)     NOT NULL COMMENT '发票ID',
    `goods_name`           varchar(128)   NOT NULL COMMENT '货物或应税劳务',
    `quantity`             decimal(18,4)  DEFAULT NULL COMMENT '数量',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税金额',
    `tax_rate`             decimal(5,2)   NOT NULL COMMENT '税率',
    `untax_amount`         decimal(18,2)  NOT NULL COMMENT '未税金额',
    `tax_amount`           decimal(18,2)  NOT NULL COMMENT '税额',
    `sort`                 int            NOT NULL DEFAULT 0 COMMENT '排序',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='进项发票明细';

CREATE TABLE IF NOT EXISTS `psi_invoice_in_link` (
    `id`           bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`    bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `invoice_id`   bigint(20)     NOT NULL COMMENT '发票ID',
    `stock_in_id`  bigint(20)     NOT NULL COMMENT '入库单ID',
    `amount`       decimal(18,2)  NOT NULL COMMENT '关联含税金额',
    `create_user`  bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`  datetime       NOT NULL COMMENT '创建时间',
    `update_user`  bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`  datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_invoice` (`invoice_id`),
    INDEX `idx_stock_in` (`stock_in_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='进项发票关联入库';

-- ========== 银行对账 ==========
CREATE TABLE IF NOT EXISTS `psi_bank_statement` (
  `unmatch_reason` varchar(500) DEFAULT NULL,
  `unmatch_user` bigint DEFAULT NULL,
  `unmatch_time` datetime DEFAULT NULL,
  UNIQUE KEY `uk_bank_cash_match` (`tenant_id`,`match_biz_type`,`match_biz_id`,`deleted`),
    `id`             bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`        bigint(20)     NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `account_id`     bigint(20)     NOT NULL COMMENT '账户ID',
    `stmt_date`      datetime       NOT NULL COMMENT '流水日期',
    `direction`      varchar(8)     NOT NULL COMMENT 'IN/OUT',
    `amount`         decimal(18,2)  NOT NULL COMMENT '金额',
    `counterparty`   varchar(128)   DEFAULT NULL COMMENT '对方户名',
    `summary`        varchar(256)   DEFAULT NULL COMMENT '摘要',
    `bank_seq_no`    varchar(64)    DEFAULT NULL COMMENT '银行流水号',
    `fingerprint`    varchar(64)    NOT NULL COMMENT '去重指纹SM3',
    `match_status`   varchar(16)    NOT NULL DEFAULT 'UNMATCHED' COMMENT '匹配状态',
    `match_biz_type` varchar(32)    DEFAULT NULL COMMENT '匹配业务类型',
    `match_biz_id`   bigint(20)     DEFAULT NULL COMMENT '匹配业务ID',
    `create_user`    bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`    datetime       NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_bank_fp` (`tenant_id`, `account_id`, `fingerprint`, `deleted`),
    UNIQUE INDEX `uk_bank_seq` (`tenant_id`, `account_id`, `bank_seq_no`, `deleted`),
    INDEX `idx_match` (`match_status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='银行流水';

-- ========== 合同 ==========
CREATE TABLE IF NOT EXISTS `psi_contract_sale` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `contract_no`          varchar(32)    NOT NULL COMMENT '合同编号',
    `contract_name`        varchar(128)   NOT NULL COMMENT '合同名称',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `sign_date`            datetime       DEFAULT NULL COMMENT '签订日',
    `unit_id`              bigint(20)     NOT NULL COMMENT '客户ID',
    `project_id`           bigint(20)     DEFAULT NULL COMMENT '所属项目',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税总额',
    `start_date`           datetime       DEFAULT NULL COMMENT '开始日',
    `end_date`             datetime       DEFAULT NULL COMMENT '结束日',
    `file_id`              varchar(500)   DEFAULT NULL COMMENT '附件文件ID，多个逗号分隔',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_cs_no` (`tenant_id`, `contract_no`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销售合同';

CREATE TABLE IF NOT EXISTS `psi_contract_purchase` (
    `id`                   bigint(20)     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`            bigint(20)     NOT NULL DEFAULT 0 COMMENT '租户ID',
    `dept_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '创建人部门',
    `contract_no`          varchar(32)    NOT NULL COMMENT '合同编号',
    `contract_name`        varchar(128)   NOT NULL COMMENT '合同名称',
    `doc_status`           varchar(16)    NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `sign_date`            datetime       DEFAULT NULL COMMENT '签订日',
    `unit_id`              bigint(20)     NOT NULL COMMENT '供应商ID',
    `project_id`           bigint(20)     DEFAULT NULL COMMENT '所属项目',
    `tax_inclusive_amount` decimal(18,2)  NOT NULL COMMENT '含税总额',
    `start_date`           datetime       DEFAULT NULL COMMENT '开始日',
    `end_date`             datetime       DEFAULT NULL COMMENT '结束日',
    `file_id`              varchar(500)   DEFAULT NULL COMMENT '附件文件ID，多个逗号分隔',
    `remark`               varchar(500)   DEFAULT NULL COMMENT '备注',
    `create_user`          bigint(20)     NOT NULL COMMENT '创建人',
    `create_time`          datetime       NOT NULL COMMENT '创建时间',
    `update_user`          bigint(20)     DEFAULT NULL COMMENT '修改人',
    `update_time`          datetime       DEFAULT NULL COMMENT '修改时间',
    `deleted`              bigint(20)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_cp_no` (`tenant_id`, `contract_no`, `deleted`),
    INDEX `idx_cp_project` (`project_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购合同';

-- ----- patch_psi_unit_sub.sql -----
CREATE TABLE IF NOT EXISTS `psi_unit_contact` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `contact_name`  varchar(64)   NOT NULL COMMENT '联系人',
    `mobile`        varchar(32)   DEFAULT NULL COMMENT '手机',
    `phone`         varchar(32)   DEFAULT NULL COMMENT '电话',
    `email`         varchar(128)  DEFAULT NULL COMMENT '邮箱',
    `position`      varchar(64)   DEFAULT NULL COMMENT '职务',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位联系人';

CREATE TABLE IF NOT EXISTS `psi_unit_invoice` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `title`         varchar(128)  NOT NULL COMMENT '发票抬头',
    `tax_no`        varchar(64)   DEFAULT NULL COMMENT '税号',
    `bank_name`     varchar(128)  DEFAULT NULL COMMENT '开户行',
    `account_no`    varchar(64)   DEFAULT NULL COMMENT '账号',
    `address`       varchar(256)  DEFAULT NULL COMMENT '地址',
    `phone`         varchar(32)   DEFAULT NULL COMMENT '电话',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位开票信息';

CREATE TABLE IF NOT EXISTS `psi_unit_address` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `contact_name`  varchar(64)   DEFAULT NULL COMMENT '收货人',
    `mobile`        varchar(32)   DEFAULT NULL COMMENT '手机',
    `region`        varchar(128)  DEFAULT NULL COMMENT '省市区',
    `address`       varchar(256)  NOT NULL COMMENT '详细地址',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位收货地址';

CREATE TABLE IF NOT EXISTS `psi_unit_pay` (
    `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`     bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `unit_id`       bigint(20)    NOT NULL COMMENT '往来单位ID',
    `account_name`  varchar(128)  NOT NULL COMMENT '户名',
    `bank_name`     varchar(128)  DEFAULT NULL COMMENT '开户行',
    `account_no`    varchar(64)   NOT NULL COMMENT '账号',
    `is_default`    tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `remark`        varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`   bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`   datetime      NOT NULL COMMENT '创建时间',
    `update_user`   bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`   datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`       bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_unit` (`tenant_id`, `unit_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='往来单位收款信息';

-- ----- patch_psi_print_template.sql -----
CREATE TABLE IF NOT EXISTS `psi_print_template` (
    `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `tenant_id`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '租户ID',
    `template_name`  varchar(128)  NOT NULL COMMENT '模板名称',
    `biz_type`       varchar(32)   NOT NULL COMMENT '业务类型 STOCK_OUT 等',
    `file_id`        bigint(20)    DEFAULT NULL COMMENT '附件/Word 文件ID',
    `content_html`   mediumtext    DEFAULT NULL COMMENT '浏览器打印 HTML 模板',
    `field_mapping`  varchar(2000) DEFAULT NULL COMMENT '字段映射说明 JSON',
    `is_default`     tinyint(1)    UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否默认（0否1是）',
    `status`         tinyint(1)    UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态（1：启用；2：禁用）',
    `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_user`    bigint(20)    NOT NULL COMMENT '创建人',
    `create_time`    datetime      NOT NULL COMMENT '创建时间',
    `update_user`    bigint(20)    DEFAULT NULL COMMENT '修改人',
    `update_time`    datetime      DEFAULT NULL COMMENT '修改时间',
    `deleted`        bigint(20)    NOT NULL DEFAULT 0 COMMENT '是否已删除（0：否；id：是）',
    PRIMARY KEY (`id`),
    INDEX `idx_biz_type` (`tenant_id`, `biz_type`, `deleted`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='PSI 打印模板';

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10200, '打印模板', 10001, 2, '/psi/print-template', 'PsiPrintTemplate', 'psi/print-template/index', NULL, 'printer', b'0', b'0', b'0', NULL, 9, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10200);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10201, '列表', 10200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:printTemplate:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10201);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10202, '详情', 10200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:printTemplate:get', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10202);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10203, '新增', 10200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:printTemplate:create', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10203);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10204, '修改', 10200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:printTemplate:update', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10204);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10205, '删除', 10200, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:printTemplate:delete', 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10205);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10200 AS id UNION ALL SELECT 10201 UNION ALL SELECT 10202
    UNION ALL SELECT 10203 UNION ALL SELECT 10204 UNION ALL SELECT 10205
) m
WHERE r.id IN (1, 3);

-- 默认出库打印模板（content_html 空则后端用内置 HTML）
INSERT INTO `psi_print_template`
(`tenant_id`, `template_name`, `biz_type`, `file_id`, `content_html`, `field_mapping`, `is_default`, `status`, `remark`, `create_user`, `create_time`, `deleted`)
SELECT 0, '出库单默认模板', 'STOCK_OUT', NULL, NULL,
       '{"placeholders":["docNo","outDate","warehouseName","unitName","totalAmount","remark","itemsHtml"]}',
       1, 1, '系统预置；可改 HTML 或保持空用内置样式', 1, NOW(), 0
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `psi_print_template` WHERE `tenant_id` = 0 AND `biz_type` = 'STOCK_OUT' AND `is_default` = 1 AND `deleted` = 0
);

-- ----- patch_psi_stock_menu.sql -----
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10040, '采购入库', 10002, 2, '/psi/stock-in', 'PsiStockIn', 'psi/stock-in/index', NULL, 'download', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10040);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10041, '列表', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:list', 1, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10041);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10042, '详情', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:get', 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10042);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10043, '新增', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:create', 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10043);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10044, '修改', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:update', 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10044);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10045, '删除', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:delete', 5, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10045);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10046, '确认', 10040, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockIn:confirm', 6, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10046);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10050, '销售出库', 10002, 2, '/psi/stock-out', 'PsiStockOut', 'psi/stock-out/index', NULL, 'upload', b'0', b'0', b'0', NULL, 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10050);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10051, '列表', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:list', 1, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10051);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10052, '详情', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:get', 2, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10052);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10053, '新增', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:create', 3, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10053);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10054, '修改', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:update', 4, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10054);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10055, '删除', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:delete', 5, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10055);
INSERT INTO `sys_menu` (`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10056, '确认', 10050, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockOut:confirm', 6, 1, 1, NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10056);

-- ----- patch_psi_menu_modules.sql -----
-- validCheckSum: ANY
-- 兼容已执行旧种子的环境库：目录缺失则补、父级纠偏、叶子菜单幂等插入

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10001, '基础数据', 0, 1, '/psi/master', 'PsiMaster', 'Layout', '/psi/unit', 'folder', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10001);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10002, '库存业务', 0, 1, '/psi/stock', 'PsiStock', 'Layout', '/psi/stock-in', 'archive', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10002);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10003, '财务管理', 0, 1, '/psi/finance', 'PsiFinance', 'Layout', '/psi/receipt', 'stamp', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10003);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10004, '合同管理', 0, 1, '/psi/contract', 'PsiContract', 'Layout', '/psi/contract-sale', 'file', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10004);

UPDATE `sys_menu` SET `parent_id` = 10001, `sort` = 1 WHERE `id` = 10010;
UPDATE `sys_menu` SET `parent_id` = 10001, `sort` = 2 WHERE `id` = 10020;
UPDATE `sys_menu` SET `parent_id` = 10001, `sort` = 3 WHERE `id` = 10030;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 1 WHERE `id` = 10040;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 2 WHERE `id` = 10050;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 3 WHERE `id` = 10060;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 4 WHERE `id` = 10070;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10080, '期初建账', 10001, 2, '/psi/init', 'PsiInit', 'psi/init/index', NULL, 'calendar', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10080);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10081, '列表', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10081);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10082, '确认', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:confirm', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10082);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10090, '库存盘点', 10002, 2, '/psi/stock-check', 'PsiStockCheck', 'psi/stock-check/index', NULL, 'select-all', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10090);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10091, '列表', 10090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:check:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10091);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10092, '新增', 10090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:check:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10092);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10093, '确认', 10090, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:check:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10093);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10100, '库存调整', 10002, 2, '/psi/stock-adjust', 'PsiStockAdjust', 'psi/stock-adjust/index', NULL, 'tool', b'0', b'0', b'0', NULL, 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10100);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10101, '列表', 10100, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:adjust:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10101);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10102, '新增', 10100, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:adjust:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10102);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10103, '确认', 10100, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:adjust:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10103);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10110, '销售开票', 10003, 2, '/psi/invoice-out', 'PsiInvoiceOut', 'psi/invoice-out/index', NULL, 'export', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10110);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10111, '列表', 10110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-out:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10111);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10112, '新增', 10110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-out:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10112);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10113, '确认', 10110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-out:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10113);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10114, '冲红', 10110, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-out:red', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10114);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10120, '采购收票', 10003, 2, '/psi/invoice-in', 'PsiInvoiceIn', 'psi/invoice-in/index', NULL, 'import', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10120);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10121, '列表', 10120, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-in:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10121);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10122, '新增', 10120, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-in:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10122);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10123, '确认', 10120, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-in:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10123);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10124, '冲红', 10120, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:invoice-in:red', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10124);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10130, '回款登记', 10003, 2, '/psi/receipt', 'PsiReceipt', 'psi/receipt/index', NULL, 'share-alt', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10130);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10131, '列表', 10130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receipt:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10131);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10132, '新增', 10130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receipt:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10132);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10133, '确认', 10130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receipt:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10133);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10134, '核销', 10130, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receipt:writeoff', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10134);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10140, '付款登记', 10003, 2, '/psi/payment', 'PsiPayment', 'psi/payment/index', NULL, 'share-internal', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10140);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10141, '列表', 10140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payment:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10141);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10142, '新增', 10140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payment:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10142);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10143, '确认', 10140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payment:confirm', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10143);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10144, '核销', 10140, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payment:writeoff', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10144);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10150, '银行账户', 10003, 2, '/psi/account', 'PsiAccount', 'psi/account/index', NULL, 'safe', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10150);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10151, '列表', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10151);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10160, '银行对账', 10003, 2, '/psi/bank', 'PsiBank', 'psi/bank/index', NULL, 'find-replace', b'0', b'0', b'0', NULL, 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10160);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10161, '列表', 10160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:bank:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10161);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10162, '导入', 10160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:bank:import', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10162);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10163, '匹配', 10160, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:bank:match', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10163);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10170, '销售合同', 10004, 2, '/psi/contract-sale', 'PsiContractSale', 'psi/contract-sale/index', NULL, 'drive-file', b'0', b'0', b'0', NULL, 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10170);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10171, '列表', 10170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contractSale:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10171);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10172, '新增', 10170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contractSale:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10172);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10173, '修改', 10170, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contractSale:update', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10173);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10180, '采购合同', 10004, 2, '/psi/contract-purchase', 'PsiContractPurchase', 'psi/contract-purchase/index', NULL, 'bookmark', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10180);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10181, '列表', 10180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contractPurchase:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10181);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10182, '新增', 10180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contractPurchase:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10182);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10183, '修改', 10180, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:contractPurchase:update', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10183);

-- ----- patch_psi_menu_icons.sql -----
UPDATE `sys_menu` SET `icon` = 'storage' WHERE `id` = 10000;
UPDATE `sys_menu` SET `icon` = 'folder' WHERE `id` = 10001;
UPDATE `sys_menu` SET `icon` = 'archive' WHERE `id` = 10002;
UPDATE `sys_menu` SET `icon` = 'stamp' WHERE `id` = 10003;
UPDATE `sys_menu` SET `icon` = 'file' WHERE `id` = 10004;
UPDATE `sys_menu` SET `icon` = 'user-group' WHERE `id` = 10010;
UPDATE `sys_menu` SET `icon` = 'tags' WHERE `id` = 10020;
UPDATE `sys_menu` SET `icon` = 'location' WHERE `id` = 10030;
UPDATE `sys_menu` SET `icon` = 'download' WHERE `id` = 10040;
UPDATE `sys_menu` SET `icon` = 'upload' WHERE `id` = 10050;
UPDATE `sys_menu` SET `icon` = 'reply' WHERE `id` = 10060;
UPDATE `sys_menu` SET `icon` = 'swap' WHERE `id` = 10070;
UPDATE `sys_menu` SET `icon` = 'calendar' WHERE `id` = 10080;
UPDATE `sys_menu` SET `icon` = 'select-all' WHERE `id` = 10090;
UPDATE `sys_menu` SET `icon` = 'tool' WHERE `id` = 10100;
UPDATE `sys_menu` SET `icon` = 'export' WHERE `id` = 10110;
UPDATE `sys_menu` SET `icon` = 'import' WHERE `id` = 10120;
UPDATE `sys_menu` SET `icon` = 'share-alt' WHERE `id` = 10130;
UPDATE `sys_menu` SET `icon` = 'share-internal' WHERE `id` = 10140;
UPDATE `sys_menu` SET `icon` = 'safe' WHERE `id` = 10150;
UPDATE `sys_menu` SET `icon` = 'find-replace' WHERE `id` = 10160;
UPDATE `sys_menu` SET `icon` = 'drive-file' WHERE `id` = 10170;
UPDATE `sys_menu` SET `icon` = 'bookmark' WHERE `id` = 10180;

-- 系统公告详情/发布页缺图标一并补齐
UPDATE `sys_menu` SET `icon` = 'eye' WHERE `id` = 1093 AND (`icon` IS NULL OR `icon` = '');
UPDATE `sys_menu` SET `icon` = 'edit' WHERE `id` = 1094 AND (`icon` IS NULL OR `icon` = '');

-- ----- patch_psi_open_ledger_menu.sql -----
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10125, '应收台账', 10003, 2, '/psi/receivable', 'PsiReceivable', 'psi/receivable/index', NULL, 'list', b'0', b'0', b'0', NULL, 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10125);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10126, '列表', 10125, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:receivable:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10126);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10127, '应付台账', 10003, 2, '/psi/payable', 'PsiPayable', 'psi/payable/index', NULL, 'ordered-list', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10127);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10128, '列表', 10127, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:payable:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10128);

-- 银行账户增删改权限
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10152, '新增', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:create', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10152);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10153, '修改', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:update', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10153);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10154, '删除', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:delete', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10154);

-- 回款/付款菜单排序后移（应收应付插入 3/4，原回款等顺延）
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 10130 AND `sort` < 5;
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 10140 AND `sort` < 6;
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 10150 AND `sort` < 7;
UPDATE `sys_menu` SET `sort` = 8 WHERE `id` = 10160 AND `sort` < 8;

-- ----- patch_psi_flow_todo_menu.sql -----
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10190, '库存流水', 10002, 2, '/psi/stock-flow', 'PsiStockFlow', 'psi/stock-flow/index', NULL, 'storage', b'0', b'0', b'0', NULL, 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10190);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10191, '列表', 10190, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockFlow:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10191);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10195, '业务待办', 10002, 2, '/psi/todo', 'PsiTodo', 'psi/todo/index', NULL, 'calendar', b'0', b'0', b'0', NULL, 8, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10195);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10196, '列表', 10195, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:todo:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10196);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10197, '完成', 10195, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:todo:done', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10197);

-- ----- patch_psi_workbench_menu.sql -----
-- validCheckSum: ANY

-- 不再插入 /psi/workbench 菜单；经营概览见仪表盘工作台 PsiBusinessPanel
SELECT 1;

-- ----- patch_psi_role_menu_bind.sql -----
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10185 AS id UNION ALL SELECT 10186
    UNION ALL SELECT 10190 UNION ALL SELECT 10191
    UNION ALL SELECT 10195 UNION ALL SELECT 10196 UNION ALL SELECT 10197
) m
WHERE r.id IN (1, 3);

-- ----- patch_psi_stock_out_pay_status.sql -----
-- 幂等：CREATE 可能已含列（全量 init）则跳过
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_out' AND COLUMN_NAME = 'pay_status') = 0,
    'ALTER TABLE `psi_stock_out` ADD COLUMN `pay_status` varchar(16) NOT NULL DEFAULT ''UNPAID'' COMMENT ''回款状态'' AFTER `invoice_status`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- patch_psi_stock_in_invoice_status.sql -----
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_in' AND COLUMN_NAME = 'invoice_status') = 0,
    'ALTER TABLE `psi_stock_in` ADD COLUMN `invoice_status` varchar(16) NOT NULL DEFAULT ''UNINVOICED'' COMMENT ''开票状态'' AFTER `pay_status`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- patch_psi_contract_confirm.sql -----
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_sale' AND COLUMN_NAME = 'confirm_user'
ALTER TABLE `psi_contract_sale`
    ADD COLUMN `confirm_user` bigint(20) DEFAULT NULL COMMENT '确认人' AFTER `remark`,
    ADD COLUMN `confirm_time` datetime DEFAULT NULL COMMENT '确认时间' AFTER `confirm_user`;

-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_purchase' AND COLUMN_NAME = 'confirm_user'
ALTER TABLE `psi_contract_purchase`
    ADD COLUMN `confirm_user` bigint(20) DEFAULT NULL COMMENT '确认人' AFTER `remark`,
    ADD COLUMN `confirm_time` datetime DEFAULT NULL COMMENT '确认时间' AFTER `confirm_user`;

-- ----- patch_psi_account_no_sm4.sql -----
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_company_account' AND COLUMN_NAME = 'account_no' AND CHARACTER_MAXIMUM_LENGTH < 255
ALTER TABLE `psi_company_account`
    MODIFY COLUMN `account_no` varchar(255) NOT NULL COMMENT '账号（库内 SM4）';

-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_unit_pay' AND COLUMN_NAME = 'account_no' AND CHARACTER_MAXIMUM_LENGTH < 255
ALTER TABLE `psi_unit_pay`
    MODIFY COLUMN `account_no` varchar(255) NOT NULL COMMENT '账号（库内 SM4）';

-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_unit_invoice' AND COLUMN_NAME = 'account_no' AND CHARACTER_MAXIMUM_LENGTH < 255
ALTER TABLE `psi_unit_invoice`
    MODIFY COLUMN `account_no` varchar(255) DEFAULT NULL COMMENT '账号（库内 SM4）';

-- ----- patch_psi_data_permission_pilot.sql -----
-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_in' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_stock_in` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_stock_in.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_stock_in` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_receipt' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_receipt` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_receipt.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_receipt` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_sale' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_contract_sale` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_contract_sale.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_contract_sale` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- ----- patch_psi_goods_category_menu.sql -----
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10310, '商品分类', 10001, 2, '/psi/goods-category', 'PsiGoodsCategory', 'psi/goods-category/index', NULL, 'mind-mapping', b'0', b'0', b'0', NULL, 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10310);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10311, '列表', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10311);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10312, '详情', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:get', 2, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10312);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10313, '新增', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:create', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10313);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10314, '修改', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:update', 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10314);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10315, '删除', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:delete', 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10315);

-- 商品档案排序让到分类之后
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 10020 AND `sort` < 3;
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 10030 AND `sort` < 4;
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 10080 AND `sort` < 5;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10310 AS id UNION ALL SELECT 10311 UNION ALL SELECT 10312
    UNION ALL SELECT 10313 UNION ALL SELECT 10314 UNION ALL SELECT 10315
) m
WHERE r.id IN (1, 3);

-- ----- patch_psi_goods_category_code.sql -----
-- 全新安装的 psi_goods_category 已包含 code 和 uk_category_code。
-- Liquibase 的列/索引存在性前置条件不会由 mysql 客户端执行，不能重复拼接 ALTER。
-- 已上线库继续使用原 changeset psi:121 / psi:122 的前置条件升级。

-- ----- patch_psi_data_permission_rollout.sql -----
-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_out' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_stock_out` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_stock_out.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_stock_out` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_return' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_stock_return` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_stock_return.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_stock_return` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_transfer' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_stock_transfer` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_stock_transfer.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_stock_transfer` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_check' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_stock_check` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_stock_check.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_stock_check` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_adjust' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_stock_adjust` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_stock_adjust.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_stock_adjust` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_purchase' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_contract_purchase` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_contract_purchase.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_contract_purchase` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_payment' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_payment` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_payment.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_payment` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_invoice_out' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_invoice_out` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_invoice_out.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_invoice_out` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_invoice_in' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_invoice_in` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_invoice_in.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_invoice_in` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_write_off' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_write_off` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_write_off.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_write_off` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_init_stock' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_init_stock` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_init_stock.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_init_stock` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_init_receivable' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_init_receivable` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_init_receivable.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_init_receivable` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_init_payable' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_init_payable` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_init_payable.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_init_payable` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_init_prepay' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_init_prepay` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_init_prepay.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_init_prepay` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_receivable' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_receivable` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_receivable.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_receivable` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_payable' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_payable` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_payable.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_payable` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_unit' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_unit` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_unit.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_unit` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_goods' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_goods` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_goods.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_goods` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_warehouse' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_warehouse` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_warehouse.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_warehouse` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_company_account' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_company_account` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_company_account.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_company_account` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- ----- patch_psi_data_permission_remaining.sql -----
-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_bank_statement' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_bank_statement` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_bank_statement.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_bank_statement` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_todo' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_todo` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_todo.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_todo` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_stock_flow' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_stock_flow` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_stock_flow.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_stock_flow` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_print_template' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_print_template` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_print_template.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_print_template` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- 幂等：仅当缺列时 ADD dept_id（Docker 全量 init 时 CREATE 已含列则跳过）
SET @sql := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_goods_category' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `psi_goods_category` ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `tenant_id`',
    'SELECT ''skip psi_goods_category.dept_id'''
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `psi_goods_category` t LEFT JOIN `sys_user` u ON u.id = t.create_user SET t.dept_id = COALESCE(u.dept_id, 0) WHERE t.deleted = 0;

-- ----- patch_psi_print_stock_in.sql -----
INSERT INTO `psi_print_template`
(`tenant_id`, `template_name`, `biz_type`, `file_id`, `content_html`, `field_mapping`, `is_default`, `status`, `remark`, `create_user`, `create_time`, `deleted`)
SELECT 0, '入库单默认模板', 'STOCK_IN', NULL, NULL,
       '{"placeholders":["docNo","inDate","warehouseName","unitName","totalAmount","remark","itemsHtml","itemsText"]}',
       1, 1, '系统预置；可改 HTML 或保持空用内置样式；Word 走 /render-docx', 1, NOW(), 0
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `psi_print_template` WHERE `tenant_id` = 0 AND `biz_type` = 'STOCK_IN' AND `is_default` = 1 AND `deleted` = 0
);

-- ----- patch_psi_master_import_export_menu.sql -----
-- validCheckSum: ANY

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10016, '导出', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:export', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10016);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10017, '导入', 10010, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:unit:import', 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10017);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10026, '导出', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:export', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10026);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10027, '导入', 10020, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goods:import', 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10027);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10036, '导出', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:export', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10036);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10037, '导入', 10030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:warehouse:import', 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10037);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10083, '导入', 10080, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:init:import', 3, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10083);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10016 AS id UNION ALL SELECT 10017
    UNION ALL SELECT 10026 UNION ALL SELECT 10027
    UNION ALL SELECT 10036 UNION ALL SELECT 10037
    UNION ALL SELECT 10083
) m
WHERE r.id IN (1, 3);

-- ----- patch_psi_menu_flatten_two_level.sql -----
-- 四大模块升为顶层目录（需 Layout 承载页面壳）
UPDATE `sys_menu`
SET `parent_id` = 0,
    `component` = 'Layout',
    `sort` = 2,
    `redirect` = '/psi/workbench',
    `update_time` = NOW()
WHERE `id` = 10001;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `component` = 'Layout',
    `sort` = 3,
    `redirect` = '/psi/stock-in',
    `update_time` = NOW()
WHERE `id` = 10002;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `component` = 'Layout',
    `sort` = 4,
    `redirect` = '/psi/receipt',
    `update_time` = NOW()
WHERE `id` = 10003;

UPDATE `sys_menu`
SET `parent_id` = 0,
    `component` = 'Layout',
    `sort` = 5,
    `redirect` = '/psi/contract-sale',
    `update_time` = NOW()
WHERE `id` = 10004;

-- 原挂在 PSI业务 下的工作台，改挂基础数据
UPDATE `sys_menu`
SET `parent_id` = 10001,
    `sort` = 0,
    `update_time` = NOW()
WHERE `id` = 10185;

-- 角色若只勾了「PSI业务」目录，补绑四大模块，避免升层后丢入口
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.`role_id`, m.`id`
FROM `sys_role_menu` rm
CROSS JOIN (
    SELECT 10001 AS id UNION ALL SELECT 10002 UNION ALL SELECT 10003 UNION ALL SELECT 10004
) m
WHERE rm.`menu_id` = 10000;

DELETE FROM `sys_role_menu` WHERE `menu_id` = 10000;
DELETE FROM `sys_menu` WHERE `id` = 10000;

-- ----- patch_psi_p2_import_export_menu.sql -----
-- validCheckSum: ANY

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10316, '导出', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:export', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10316);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10317, '导入', 10310, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:goodsCategory:import', 7, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10317);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10155, '导出', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:export', 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10155);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10156, '导入', 10150, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:account:import', 6, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10156);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10316 AS id UNION ALL SELECT 10317
    UNION ALL SELECT 10155 UNION ALL SELECT 10156
) m
WHERE r.id IN (1, 3);

-- ----- patch_psi_workbench_merge_dashboard.sql -----
UPDATE `sys_menu`
SET `redirect` = '/psi/unit',
    `update_time` = NOW()
WHERE `id` = 10001
  AND (`redirect` IS NULL OR `redirect` = '/psi/workbench');

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (10185, 10186);
DELETE FROM `sys_menu` WHERE `id` IN (10185, 10186);

-- ----- patch_psi_menu_sidebar_order.sql -----
UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 10004;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 10002;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 10003;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 10001;
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 1000;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 2000;

-- ----- patch_quote_retire_stub_menus.sql -----
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (40002, 40003, 40004);
DELETE FROM `sys_menu` WHERE `id` IN (40002, 40003, 40004);

-- ----- patch_contract_menu_icon_mono.sql -----
UPDATE `sys_menu` SET `icon` = 'file', `update_time` = NOW() WHERE `id` = 10004 AND `title` = '合同管理';
UPDATE `sys_menu` SET `icon` = 'file', `update_time` = NOW() WHERE `id` = 20032;

-- ----- patch_menu_sidebar_tree.sql -----
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (40002, 40003, 40004);
DELETE FROM `sys_menu` WHERE `id` IN (40002, 40003, 40004);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 30100, '项目管理', 0, 1, '/sign/project', 'ProjectRoot', 'Layout', '/sign/project/index', 'project', b'0', b'0', b'0', NULL, 4, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 30100);

UPDATE `sys_menu`
SET `parent_id` = 30100, `title` = '项目列表', `path` = '/sign/project/index', `sort` = 1, `update_time` = NOW()
WHERE `id` = 30004 AND `deleted` = 0;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 30100 FROM `sys_role_menu` rm WHERE rm.`menu_id` = 30004;

UPDATE `sys_menu`
SET `parent_id` = 10004, `title` = '维修合同', `sort` = 3, `is_hidden` = b'0', `update_time` = NOW()
WHERE `id` = 20032 AND `deleted` = 0;

UPDATE `sys_menu`
SET `parent_id` = 10003, `title` = '利润分析', `sort` = 7, `is_hidden` = b'0', `update_time` = NOW()
WHERE `id` = 20042 AND `deleted` = 0;

UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 10170 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 10180 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 10110 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 10120 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 10125 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 10127 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 10130 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10140 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 8, `update_time` = NOW() WHERE `id` = 10150 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 9, `update_time` = NOW() WHERE `id` = 10160 AND `deleted` = 0;

UPDATE `sys_menu` SET `is_hidden` = b'1', `update_time` = NOW()
WHERE `id` IN (
    20040, 20041, 20050,
    20070, 30001
) AND `deleted` = 0;

UPDATE `sys_menu` SET `redirect` = '/eoms/repair/apply', `update_time` = NOW() WHERE `id` = 20000 AND `deleted` = 0;
UPDATE `sys_menu` SET `redirect` = '/sign/seal/extract', `update_time` = NOW() WHERE `id` = 30000 AND `deleted` = 0;

UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 20010 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 20011 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 20012 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 20013 AND `deleted` = 0;

UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 30003 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 30002 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 30005 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 30006 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 30008 AND `deleted` = 0;

UPDATE `sys_menu` SET `title` = '租户列表', `update_time` = NOW() WHERE `id` = 3010 AND `deleted` = 0;

UPDATE `sys_menu` SET `permission` = 'psi:contractSale:list', `update_time` = NOW() WHERE `id` = 10171 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractSale:create', `update_time` = NOW() WHERE `id` = 10172 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractSale:update', `update_time` = NOW() WHERE `id` = 10173 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractPurchase:list', `update_time` = NOW() WHERE `id` = 10181 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractPurchase:create', `update_time` = NOW() WHERE `id` = 10182 AND `deleted` = 0;
UPDATE `sys_menu` SET `permission` = 'psi:contractPurchase:update', `update_time` = NOW() WHERE `id` = 10183 AND `deleted` = 0;

UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 40000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3, `update_time` = NOW() WHERE `id` = 10004 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4, `update_time` = NOW() WHERE `id` = 30100 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 10002 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10003 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 7, `update_time` = NOW() WHERE `id` = 20000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 8, `update_time` = NOW() WHERE `id` = 30000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 9, `update_time` = NOW() WHERE `id` = 10001 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 10, `update_time` = NOW() WHERE `id` = 1000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 11, `update_time` = NOW() WHERE `id` = 2000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 12, `update_time` = NOW() WHERE `id` = 3000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 14, `update_time` = NOW() WHERE `id` = 8000 AND `deleted` = 0;

-- ----- patch_sign_audit_to_monitor_log.sql -----
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 2034, '签章审计', 2030, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sign:audit:list', 4, 1, 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 2034);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 2034
FROM `sys_role` r
WHERE r.id IN (1, 3);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 2034
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (30007, 2031)
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 2034
  );

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, m.`id`
FROM `sys_role_menu` rm
CROSS JOIN (SELECT 2000 AS `id` UNION ALL SELECT 2030) m
WHERE rm.`menu_id` = 30007
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = m.`id`
  );

DELETE FROM `sys_role_menu` WHERE `menu_id` = 30007;
DELETE FROM `sys_menu` WHERE `id` = 30007;

-- ----- patch_project_contract_bind.sql -----
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_sale' AND COLUMN_NAME = 'project_id') = 0,
    'ALTER TABLE `psi_contract_sale` ADD COLUMN `project_id` bigint(20) DEFAULT NULL COMMENT ''所属项目'' AFTER `unit_id`, ADD INDEX `idx_cs_project` (`project_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'psi_contract_purchase' AND COLUMN_NAME = 'project_id') = 0,
    'ALTER TABLE `psi_contract_purchase` ADD COLUMN `project_id` bigint(20) DEFAULT NULL COMMENT ''所属项目'' AFTER `unit_id`, ADD INDEX `idx_cp_project` (`project_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_contract' AND COLUMN_NAME = 'project_id') = 0,
    'ALTER TABLE `eoms_contract` ADD COLUMN `project_id` bigint(20) DEFAULT NULL COMMENT ''所属项目'' AFTER `customer_id`, ADD INDEX `idx_ec_project` (`project_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- patch_sign_project_contract_bind.sql -----
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'contract_type') = 0,
    'ALTER TABLE `sign_project` ADD COLUMN `contract_type` varchar(16) DEFAULT NULL COMMENT ''绑定合同类型：SALE销售/PURCHASE采购/REPAIR维修'' AFTER `description`, ADD COLUMN `contract_id` bigint DEFAULT NULL COMMENT ''绑定合同ID'' AFTER `contract_type`, ADD COLUMN `contract_no` varchar(64) DEFAULT NULL COMMENT ''合同编号快照'' AFTER `contract_id`, ADD COLUMN `contract_name` varchar(200) DEFAULT NULL COMMENT ''合同名称快照'' AFTER `contract_no`, ADD COLUMN `partner_name` varchar(200) DEFAULT NULL COMMENT ''往来单位快照'' AFTER `contract_name`, ADD KEY `idx_sign_project_contract` (`contract_type`, `contract_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- patch_sign_project_multi_contract.sql -----
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'sale_contract_id') = 0,
    'ALTER TABLE `sign_project` ADD COLUMN `sale_contract_id` bigint DEFAULT NULL COMMENT ''销售合同ID'' AFTER `partner_name`, ADD COLUMN `sale_contract_no` varchar(64) DEFAULT NULL COMMENT ''销售合同编号快照'' AFTER `sale_contract_id`, ADD COLUMN `sale_contract_name` varchar(200) DEFAULT NULL COMMENT ''销售合同名称快照'' AFTER `sale_contract_no`, ADD COLUMN `sale_partner_name` varchar(200) DEFAULT NULL COMMENT ''销售往来单位快照'' AFTER `sale_contract_name`, ADD COLUMN `purchase_contract_id` bigint DEFAULT NULL COMMENT ''采购合同ID'' AFTER `sale_partner_name`, ADD COLUMN `purchase_contract_no` varchar(64) DEFAULT NULL COMMENT ''采购合同编号快照'' AFTER `purchase_contract_id`, ADD COLUMN `purchase_contract_name` varchar(200) DEFAULT NULL COMMENT ''采购合同名称快照'' AFTER `purchase_contract_no`, ADD COLUMN `purchase_partner_name` varchar(200) DEFAULT NULL COMMENT ''采购往来单位快照'' AFTER `purchase_contract_name`, ADD COLUMN `repair_contract_id` bigint DEFAULT NULL COMMENT ''维修合同ID'' AFTER `purchase_partner_name`, ADD COLUMN `repair_contract_no` varchar(64) DEFAULT NULL COMMENT ''维修合同编号快照'' AFTER `repair_contract_id`, ADD COLUMN `repair_contract_name` varchar(200) DEFAULT NULL COMMENT ''维修合同名称快照'' AFTER `repair_contract_no`, ADD COLUMN `repair_partner_name` varchar(200) DEFAULT NULL COMMENT ''维修往来单位快照'' AFTER `repair_contract_name`, ADD KEY `idx_sign_project_sale_contract` (`sale_contract_id`), ADD KEY `idx_sign_project_purchase_contract` (`purchase_contract_id`), ADD KEY `idx_sign_project_repair_contract` (`repair_contract_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `sign_project`
SET `sale_contract_id` = `contract_id`,
    `sale_contract_no` = `contract_no`,
    `sale_contract_name` = `contract_name`,
    `sale_partner_name` = `partner_name`
WHERE `del_flag` = '0' AND `contract_type` = 'SALE' AND `contract_id` IS NOT NULL
  AND `sale_contract_id` IS NULL;

UPDATE `sign_project`
SET `purchase_contract_id` = `contract_id`,
    `purchase_contract_no` = `contract_no`,
    `purchase_contract_name` = `contract_name`,
    `purchase_partner_name` = `partner_name`
WHERE `del_flag` = '0' AND `contract_type` = 'PURCHASE' AND `contract_id` IS NOT NULL
  AND `purchase_contract_id` IS NULL;

UPDATE `sign_project`
SET `repair_contract_id` = `contract_id`,
    `repair_contract_no` = `contract_no`,
    `repair_contract_name` = `contract_name`,
    `repair_partner_name` = `partner_name`
WHERE `del_flag` = '0' AND `contract_type` = 'REPAIR' AND `contract_id` IS NOT NULL
  AND `repair_contract_id` IS NULL;

-- ----- patch_sign_project_other_files.sql -----
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'other_file_ids') = 0,
    'ALTER TABLE `sign_project` ADD COLUMN `other_file_ids` varchar(1000) DEFAULT NULL COMMENT ''其他文件：平台sys_file.id，逗号分隔'' AFTER `repair_partner_name`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- patch_menu_project_quote_paths.sql -----
UPDATE `sys_menu`
SET `path` = '/sign/project', `redirect` = '/sign/project/index', `update_time` = NOW()
WHERE `id` = 30100 AND `deleted` = 0;

UPDATE `sys_menu`
SET `path` = '/sign/project/index', `update_time` = NOW()
WHERE `id` = 30004 AND `deleted` = 0;

-- ----- patch_m1_eoms_config_to_option.sql -----
INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 66, 'EOMS', '百度地图 AK', 'GEO_BAIDU_AK', NULL, NULL, '逆地理编码 Web 服务 AK；未填时现场定位降级为坐标字符串'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'GEO_BAIDU_AK');
INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 67, 'EOMS', '百度逆地理编码地址', 'GEO_BAIDU_REGEO_URL', NULL, 'https://api.map.baidu.com/reverse_geocoding/v3/', '可换成内网代理域名；须与申请 AK 的服务端类型匹配'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'GEO_BAIDU_REGEO_URL');
INSERT INTO `sys_option` (`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 68, 'EOMS', '水印图最长边（像素）', 'WATERMARK_MAX_SIDE', NULL, '1280', '现场照烧录水印前缩放上限，建议 640～4096'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'WATERMARK_MAX_SIDE');

DELETE FROM `sys_role_menu` WHERE `menu_id` = 20060;
DELETE FROM `sys_menu` WHERE `id` = 20060;

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1260, '运维报修', 1150, 2, CONCAT('/system/config', CHAR(63), 'tab=eoms'), 'SystemEomsConfig', 'system/config/eoms/index', NULL, 'tool', b'0', b'0', b'1', NULL, 10, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1260);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1261, '查询', 1260, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:eomsConfig:get', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1261);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1262, '修改', 1260, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:eomsConfig:update', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1262);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1260
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1220
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1260);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1261
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1221
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1261);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1262
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 1222
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1262);

DROP TABLE IF EXISTS `eoms_config`;

-- ----- patch_m1_eoms_attachment_to_sys_file.sql -----
-- 合同 file_id 已写入上方 CREATE；此处仅下线菜单并归档旧表
DELETE FROM `sys_role_menu` WHERE `menu_id` = 20061;
DELETE FROM `sys_menu` WHERE `id` = 20061;
DROP TABLE IF EXISTS `eoms_attachment`;

-- ----- patch_m1_eoms_operation_log.sql -----
DELETE FROM `sys_role_menu` WHERE `menu_id` = 20062;
DELETE FROM `sys_menu` WHERE `id` = 20062;
DROP TABLE IF EXISTS `eoms_operation_log`;


-- ----- patch_psi_collation_general.sql -----
-- 已上线库由 Liquibase 将 psi_* 转为 utf8mb4_general_ci；上方 CREATE 已带 COLLATE，全新安装无需再 CONVERT。
-- ----- patch_m2_unit_warehouse.sql -----
CREATE TABLE IF NOT EXISTS `eoms_m2_unit_map` (
  `id`            bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_kind`   varchar(16)   NOT NULL COMMENT 'CUSTOMER/SUPPLIER',
  `source_id`     bigint(20)    NOT NULL COMMENT '原表主键',
  `source_code`   varchar(64)   DEFAULT NULL COMMENT '原编号',
  `source_name`   varchar(256)  DEFAULT NULL COMMENT '原名称',
  `psi_unit_id`   bigint(20)    DEFAULT NULL COMMENT '目标 psi_unit.id',
  `action`        varchar(32)   NOT NULL COMMENT 'INSERTED/MAPPED_CODE/CONFLICT_NAME',
  `note`          varchar(500)  DEFAULT NULL COMMENT '对账说明',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source_kind`, `source_id`),
  KEY `idx_psi_unit` (`psi_unit_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='M2往来单位迁移对账';

CREATE TABLE IF NOT EXISTS `eoms_m2_warehouse_map` (
  `id`                 bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`          bigint(20)    NOT NULL COMMENT '原 eoms_warehouse.id',
  `source_code`        varchar(64)   DEFAULT NULL COMMENT '原仓库编号',
  `source_name`        varchar(256)  DEFAULT NULL COMMENT '原仓库名称',
  `psi_warehouse_id`   bigint(20)    DEFAULT NULL COMMENT '目标 psi_warehouse.id',
  `action`             varchar(32)   NOT NULL COMMENT 'INSERTED/MAPPED_CODE/CONFLICT_NAME',
  `note`               varchar(500)  DEFAULT NULL COMMENT '对账说明',
  `create_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source_id`),
  KEY `idx_psi_wh` (`psi_warehouse_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='M2仓库迁移对账';

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10011
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20001, 20031, 20010, 20011, 20032, 20040, 20041, 20023, 20024, 40001, 40011);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10031
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20030, 20022, 20023, 20024);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10001
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10010, 10030);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10010
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20010, 20011, 20032, 20040, 20041, 40001, 10011);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10030
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20022, 20023, 20024, 10031);

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (20001, 20002, 20003, 20004, 20005, 20030, 20031);
DELETE FROM `sys_menu` WHERE `id` IN (20001, 20002, 20003, 20004, 20005, 20030, 20031);

UPDATE `sys_menu`
SET `redirect` = '/eoms/repair/apply', `update_time` = NOW()
WHERE `id` = 20000 AND `deleted` = 0;

DROP TABLE IF EXISTS `eoms_customer`;
DROP TABLE IF EXISTS `eoms_customer_contact`;
DROP TABLE IF EXISTS `eoms_supplier`;
DROP TABLE IF EXISTS `eoms_warehouse`;

-- ----- patch_m3_material_stock.sql -----
CREATE TABLE IF NOT EXISTS `eoms_m3_category_map` (
  `id`                bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`         bigint(20)    NOT NULL COMMENT '原 eoms_material_category.id',
  `source_code`       varchar(64)   DEFAULT NULL COMMENT '原分类编码',
  `source_name`       varchar(256)  DEFAULT NULL COMMENT '原分类名称',
  `psi_category_id`   bigint(20)    DEFAULT NULL COMMENT '目标 psi_goods_category.id',
  `action`            varchar(32)   NOT NULL COMMENT 'INSERTED/MAPPED_CODE/CONFLICT_NAME',
  `note`              varchar(500)  DEFAULT NULL COMMENT '对账说明',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source_id`),
  KEY `idx_psi_cat` (`psi_category_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='M3物料分类迁移对账';

CREATE TABLE IF NOT EXISTS `eoms_m3_goods_map` (
  `id`             bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id`      bigint(20)    NOT NULL COMMENT '原 eoms_material.id',
  `source_code`    varchar(64)   DEFAULT NULL COMMENT '原物料编号',
  `source_name`    varchar(256)  DEFAULT NULL COMMENT '原物料名称',
  `psi_goods_id`   bigint(20)    DEFAULT NULL COMMENT '目标 psi_goods.id',
  `action`         varchar(32)   NOT NULL COMMENT 'INSERTED/MAPPED_CODE/CONFLICT_NAME',
  `note`           varchar(500)  DEFAULT NULL COMMENT '对账说明',
  `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source` (`source_id`),
  KEY `idx_psi_goods` (`psi_goods_id`),
  KEY `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='M3物料商品迁移对账';

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10021
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20020, 20011, 20013, 20022);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10020
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20020, 20021, 10021);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10310
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20021, 20020);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10040
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20023, 20024);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10050
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20022, 20011, 20013);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10001
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10020, 10310, 10021);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10002
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10040, 10050);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10041
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10040, 20023, 20024);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10051
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10050, 20022, 20011, 20013);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10031
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (20011, 20013, 20020, 20022, 20023, 20024);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 10030
FROM `sys_role_menu` rm
WHERE rm.`menu_id` IN (10031, 20011, 20013);

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (20020, 20021, 20022, 20023, 20024);
DELETE FROM `sys_menu` WHERE `id` IN (20020, 20021, 20022, 20023, 20024);

-- ----- patch_m3_role_grant_fix.sql -----
-- 不依赖已删 20020～20024：按维修/已有 PSI 菜单链式补授 + 种子角色显式补授
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, m.`id`
FROM `sys_role_menu` rm
CROSS JOIN (
  SELECT 10001 AS `id` UNION ALL SELECT 10002
  UNION ALL SELECT 10020 UNION ALL SELECT 10021 UNION ALL SELECT 10310
  UNION ALL SELECT 10040 UNION ALL SELECT 10041
  UNION ALL SELECT 10050 UNION ALL SELECT 10051
  UNION ALL SELECT 10030 UNION ALL SELECT 10031
) m
WHERE rm.`menu_id` IN (20011, 20013, 10020, 10021, 10050, 10051, 10030, 10031, 10040, 10041, 10310);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`id`, m.`id`
FROM `sys_role` r
CROSS JOIN (
  SELECT 10001 AS `id` UNION ALL SELECT 10002
  UNION ALL SELECT 10020 UNION ALL SELECT 10021 UNION ALL SELECT 10310
  UNION ALL SELECT 10040 UNION ALL SELECT 10041
  UNION ALL SELECT 10050 UNION ALL SELECT 10051
  UNION ALL SELECT 10030 UNION ALL SELECT 10031
) m
WHERE r.`id` IN (1, 2, 3);

DELETE FROM `sys_dict_item`
WHERE `dict_id` IN (SELECT `id` FROM (SELECT `id` FROM `sys_dict` WHERE `code` = 'eoms_inbound_status') t);
DELETE FROM `sys_dict` WHERE `code` = 'eoms_inbound_status';

DROP TABLE IF EXISTS `eoms_material_record`;
DROP TABLE IF EXISTS `eoms_material_stock`;
DROP TABLE IF EXISTS `eoms_inbound_item`;
DROP TABLE IF EXISTS `eoms_inbound`;
DROP TABLE IF EXISTS `eoms_purchase`;
DROP TABLE IF EXISTS `eoms_material`;
DROP TABLE IF EXISTS `eoms_material_category`;

-- ----- patch_r_docs.sql -----
UPDATE `sys_menu` SET `title` = '维修应收' WHERE `id` = 20040;
UPDATE `sys_menu`
SET `title` = '维修收款',
    `path` = '/eoms/finance/receipt',
    `name` = 'EomsFinanceReceipt',
    `component` = 'eoms/finance/receipt'
WHERE `id` = 20041;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_quote_detail' AND COLUMN_NAME = 'line_type') = 0,
    'ALTER TABLE `eoms_quote_detail` ADD COLUMN `line_type` varchar(16) NOT NULL DEFAULT ''SERVICE'' COMMENT ''MATERIAL材料 / SERVICE服务'' AFTER `quote_id`, ADD COLUMN `goods_id` bigint(20) DEFAULT NULL COMMENT ''材料行关联 psi_goods.id'' AFTER `line_type`, ADD KEY `idx_qd_goods` (`goods_id`)',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- patch_menu_icon_align.sql -----
UPDATE `sys_menu` SET `icon` = 'profit-model-alt', `update_time` = NOW() WHERE `id` = 20042 AND `deleted` = 0;
UPDATE `sys_menu` SET `icon` = 'quote', `update_time` = NOW() WHERE `id` = 40000 AND `deleted` = 0;
UPDATE `sys_menu` SET `icon` = 'insert-chart', `update_time` = NOW() WHERE `id` = 10003 AND `deleted` = 0;
UPDATE `sys_menu` SET `icon` = 'project', `update_time` = NOW() WHERE `id` = 30100 AND `deleted` = 0;
UPDATE `sys_menu` SET `icon` = 'gift', `update_time` = NOW() WHERE `id` = 3020 AND `deleted` = 0;
UPDATE `sys_menu` SET `icon` = 'drive-file', `update_time` = NOW() WHERE `id` = 10180 AND `deleted` = 0;
UPDATE `sys_menu` SET `icon` = 'customer-service', `update_time` = NOW() WHERE `id` = 20010 AND `deleted` = 0;
UPDATE `sys_menu` SET `icon` = 'check-square', `update_time` = NOW() WHERE `id` = 20013 AND `deleted` = 0;

-- ----- patch_h5_auth_manage.sql -----
INSERT INTO `sys_option`
(`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 69, 'EOMS', 'H5令牌有效期（分钟）', 'H5_TOKEN_EXPIRE_MINUTES', NULL, '120', '客户 H5 身份令牌 Redis TTL，建议 30～480；国密环境下令牌由 SM3 生成'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'H5_TOKEN_EXPIRE_MINUTES');
INSERT INTO `sys_option`
(`id`, `category`, `name`, `code`, `value`, `default_value`, `description`)
SELECT 70, 'EOMS', 'H5令牌活跃续期', 'H5_TOKEN_AUTO_REFRESH', NULL, '1', '1=每次请求续期；0=固定 TTL 到期失效'
WHERE NOT EXISTS (SELECT 1 FROM `sys_option` WHERE `code` = 'H5_TOKEN_AUTO_REFRESH');
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1270, 'H5客户授权', 1150, 2, CONCAT('/system/config', CHAR(63), 'tab=h5Auth'), 'SystemH5AuthConfig', 'system/config/h5-auth/index', NULL, 'mobile', b'0', b'0', b'1', NULL, 11, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1270);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1271, '查询配置', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:get', 1, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1271);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1272, '修改配置', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:update', 2, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1272);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1273, '在线列表', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:list', 3, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1273);
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 1274, '强退令牌', 1270, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'system:h5Auth:kickout', 4, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 1274);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1270 FROM `sys_role_menu` rm WHERE rm.`menu_id` = 1260
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1270);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1271 FROM `sys_role_menu` rm WHERE rm.`menu_id` = 1261
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1271);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1272 FROM `sys_role_menu` rm WHERE rm.`menu_id` = 1262
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1272);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1273 FROM `sys_role_menu` rm WHERE rm.`menu_id` = 1261
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1273);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, 1274 FROM `sys_role_menu` rm WHERE rm.`menu_id` = 1262
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` x WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 1274);

-- ----- patch_eoms_data_permission_m1.sql -----
-- EOMS 七张核心表补 tenant_id / create_user / dept_id，并按 create_by→username 回填
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_apply' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `eoms_repair_apply`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `del_flag`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `eoms_repair_apply` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0'
  AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_apply' AND COLUMN_NAME = 'dept_id');

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_order' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `eoms_repair_order`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `del_flag`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `eoms_repair_order` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0'
  AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_order' AND COLUMN_NAME = 'dept_id');

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_record' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `eoms_repair_record`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `del_flag`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `eoms_repair_record` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0'
  AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_record' AND COLUMN_NAME = 'dept_id');

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_settlement' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `eoms_repair_settlement`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `del_flag`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `eoms_repair_settlement` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0'
  AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_repair_settlement' AND COLUMN_NAME = 'dept_id');

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_contract' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `eoms_contract`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `del_flag`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `eoms_contract` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0'
  AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_contract' AND COLUMN_NAME = 'dept_id');

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_receivable' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `eoms_receivable`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `del_flag`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `eoms_receivable` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0'
  AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_receivable' AND COLUMN_NAME = 'dept_id');

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_payment' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `eoms_payment`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `del_flag`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人部门'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `eoms_payment` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0),
    t.dept_id = COALESCE(u.dept_id, 0)
WHERE t.del_flag = '0'
  AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_payment' AND COLUMN_NAME = 'dept_id');

-- ----- patch_sign_data_permission_m4.sql -----
-- Sign 项目补 create_user/tenant_id；日志/记录补 tenant_id/create_user/dept_id
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'create_user') = 0,
    'ALTER TABLE `sign_project`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `del_flag`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `sign_project` t
    LEFT JOIN `sys_user` u ON u.username = t.create_by AND u.deleted = 0
SET t.create_user = COALESCE(u.id, 0)
WHERE t.del_flag = '0'
  AND EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_project' AND COLUMN_NAME = 'create_user');

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_log' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `sign_log`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `create_time`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''部门ID'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `sign_log` t
    LEFT JOIN `sys_user` u ON u.id = t.user_id AND u.deleted = 0
    LEFT JOIN `sign_project` p ON p.project_id = t.project_id
SET t.create_user = COALESCE(t.user_id, 0),
    t.dept_id = COALESCE(NULLIF(p.dept_id, 0), u.dept_id, 0)
WHERE EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_log' AND COLUMN_NAME = 'dept_id');

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_record' AND COLUMN_NAME = 'dept_id') = 0,
    'ALTER TABLE `sign_record`
      ADD COLUMN `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''租户ID'' AFTER `create_time`,
      ADD COLUMN `create_user` bigint(20) NOT NULL DEFAULT 0 COMMENT ''创建人ID'' AFTER `tenant_id`,
      ADD COLUMN `dept_id` bigint(20) NOT NULL DEFAULT 0 COMMENT ''部门ID'' AFTER `create_user`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE `sign_record` t
    LEFT JOIN `sys_user` u ON u.id = t.user_id AND u.deleted = 0
    LEFT JOIN `sign_project` p ON p.project_id = t.project_id
SET t.create_user = COALESCE(t.user_id, 0),
    t.dept_id = COALESCE(NULLIF(p.dept_id, 0), u.dept_id, 0)
WHERE EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sign_record' AND COLUMN_NAME = 'dept_id');

-- ----- patch_quote_form_fields.sql -----
SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_quote' AND COLUMN_NAME = 'project_name') = 0,
    'ALTER TABLE `eoms_quote`
      MODIFY COLUMN `customer_id` bigint(20) DEFAULT NULL COMMENT ''客户ID，关联 psi_unit.id'',
      ADD COLUMN `project_name` varchar(200) DEFAULT NULL COMMENT ''项目名称'' AFTER `customer_id`,
      ADD COLUMN `quote_unit` varchar(200) DEFAULT NULL COMMENT ''报价单位'' AFTER `project_name`,
      ADD COLUMN `quote_date` datetime DEFAULT NULL COMMENT ''报价时间'' AFTER `quote_unit`,
      ADD COLUMN `valid_until` datetime DEFAULT NULL COMMENT ''报价有效期'' AFTER `quote_date`,
      ADD COLUMN `tax_rate` decimal(5,2) NOT NULL DEFAULT 4.00 COMMENT ''增值税综合税率(%)'' AFTER `valid_until`,
      ADD COLUMN `material_subtotal` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT ''材料小计'' AFTER `tax_rate`,
      ADD COLUMN `service_subtotal` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT ''技术服务费小计'' AFTER `material_subtotal`,
      ADD COLUMN `terms` text DEFAULT NULL COMMENT ''备注/条款'' AFTER `remark`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'eoms_quote_detail' AND COLUMN_NAME = 'brand_model') = 0,
    'ALTER TABLE `eoms_quote_detail`
      MODIFY COLUMN `item_name` varchar(200) NOT NULL COMMENT ''材料名称/服务项目'',
      ADD COLUMN `brand_model` varchar(100) DEFAULT NULL COMMENT ''品牌/型号'' AFTER `item_name`,
      ADD COLUMN `unit` varchar(20) DEFAULT ''台'' COMMENT ''单位'' AFTER `brand_model`,
      ADD COLUMN `description` varchar(200) DEFAULT NULL COMMENT ''服务说明'' AFTER `unit`,
      ADD COLUMN `sort_no` int NOT NULL DEFAULT 0 COMMENT ''行序号'' AFTER `description`',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----- patch_eoms_role_data_scope.sql -----
-- EOMS 角色默认 data_scope 种子（幂等）
INSERT INTO `sys_role`
(`id`, `name`, `code`, `data_scope`, `description`, `sort`, `is_system`, `create_user`, `create_time`)
SELECT 11, 'EOMS 主管', 'eoms_manager', 2, '运维报修主管：本部门及下级数据', 11, b'0', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 11 OR `code` = 'eoms_manager');

INSERT INTO `sys_role`
(`id`, `name`, `code`, `data_scope`, `description`, `sort`, `is_system`, `create_user`, `create_time`)
SELECT 12, 'EOMS 工程师', 'eoms_engineer', 4, '运维工程师：仅本人创建或派给自己的工单', 12, b'0', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 12 OR `code` = 'eoms_engineer');

INSERT INTO `sys_role`
(`id`, `name`, `code`, `data_scope`, `description`, `sort`, `is_system`, `create_user`, `create_time`)
SELECT 13, 'EOMS 财务', 'eoms_finance', 3, '运维财务：本部门结算/应收/收款', 13, b'0', 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 13 OR `code` = 'eoms_finance');

UPDATE `sys_role` SET `data_scope` = 2, `name` = 'EOMS 主管', `description` = '运维报修主管：本部门及下级数据'
WHERE `code` = 'eoms_manager' AND `data_scope` NOT IN (1);

UPDATE `sys_role` SET `data_scope` = 4, `name` = 'EOMS 工程师', `description` = '运维工程师：仅本人创建或派给自己的工单'
WHERE `code` = 'eoms_engineer' AND `data_scope` NOT IN (1);

UPDATE `sys_role` SET `data_scope` = 3, `name` = 'EOMS 财务', `description` = '运维财务：本部门结算/应收/收款'
WHERE `code` = 'eoms_finance' AND `data_scope` NOT IN (1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT `id` FROM `sys_menu`
    WHERE `id` IN (20000, 20010, 20011, 20012, 20013, 20032, 20040, 20041, 20042, 20050, 20070)
       OR (`id` BETWEEN 20101 AND 20199)
       OR (`id` BETWEEN 20111 AND 20120)
) m
WHERE r.`code` = 'eoms_manager';

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT `id` FROM `sys_menu`
    WHERE `id` IN (20000, 20011, 20012)
       OR (`id` BETWEEN 20111 AND 20120)
       OR (`id` BETWEEN 20121 AND 20129)
) m
WHERE r.`code` = 'eoms_engineer';

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT `id` FROM `sys_menu`
    WHERE `id` IN (20000, 20013, 20040, 20041, 20042)
       OR (`id` BETWEEN 20131 AND 20139)
       OR (`id` BETWEEN 20151 AND 20159)
       OR (`id` BETWEEN 20161 AND 20169)
       OR (`id` BETWEEN 20171 AND 20179)
) m
WHERE r.`code` = 'eoms_finance';

-- ----- patch_psi_stock_query_menu.sql / patch_psi_stock_query_menu_place.sql -----
INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10210, '库存查询', 10001, 2, '/psi/stock-query', 'PsiStockQuery', 'psi/stock-query/index', NULL, 'search', b'0', b'0', b'0', NULL, 5, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10210);

INSERT INTO `sys_menu`
(`id`, `title`, `parent_id`, `type`, `path`, `name`, `component`, `redirect`, `icon`, `is_external`, `is_cache`, `is_hidden`, `permission`, `sort`, `status`, `create_user`, `create_time`)
SELECT 10211, '列表', 10210, 3, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'psi:stockQuery:list', 1, 1, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 10211);

UPDATE `sys_menu`
SET `parent_id` = 10001,
    `sort` = 5,
    `title` = '库存查询',
    `path` = '/psi/stock-query',
    `name` = 'PsiStockQuery',
    `component` = 'psi/stock-query/index',
    `icon` = 'search',
    `update_time` = NOW()
WHERE `id` = 10210;

UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10080 AND (`sort` IS NULL OR `sort` <> 6);

-- 若曾把库存流水/待办为腾位置改过 sort，恢复库存业务内顺序：盘点5 调整6 流水7 待办8
UPDATE `sys_menu` SET `sort` = 7, `update_time` = NOW() WHERE `id` = 10190 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 8, `update_time` = NOW() WHERE `id` = 10195 AND `parent_id` = 10002;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r
CROSS JOIN (
    SELECT 10210 AS id UNION ALL SELECT 10211
) m
WHERE r.id IN (1, 3);

-- ----- patch_psi_menu_move_master_to_stock.sql -----
-- 商品分类/商品档案/仓库管理/库存查询 → 库存业务
UPDATE `sys_menu`
SET `redirect` = '/psi/goods-category',
    `update_time` = NOW()
WHERE `id` = 10002;

UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 1, `update_time` = NOW() WHERE `id` = 10310;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 2, `update_time` = NOW() WHERE `id` = 10020;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 3, `update_time` = NOW() WHERE `id` = 10030;
UPDATE `sys_menu` SET `parent_id` = 10002, `sort` = 4, `update_time` = NOW() WHERE `id` = 10210;

UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 10040 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 10050 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 7, `update_time` = NOW() WHERE `id` = 10060 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 8, `update_time` = NOW() WHERE `id` = 10070 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 9, `update_time` = NOW() WHERE `id` = 10090 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 10, `update_time` = NOW() WHERE `id` = 10100 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 11, `update_time` = NOW() WHERE `id` = 10190 AND `parent_id` = 10002;
UPDATE `sys_menu` SET `sort` = 12, `update_time` = NOW() WHERE `id` = 10195 AND `parent_id` = 10002;

UPDATE `sys_menu` SET `sort` = 1, `update_time` = NOW() WHERE `id` = 10010 AND `parent_id` = 10001;
UPDATE `sys_menu` SET `sort` = 2, `update_time` = NOW() WHERE `id` = 10080 AND `parent_id` = 10001;


-- ----- 一级菜单最终排序（与 patch_menu_sidebar_tree 一致；仪表盘为前端固定第 1）-----
UPDATE `sys_menu` SET `sort` = 2,  `update_time` = NOW() WHERE `id` = 40000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 3,  `update_time` = NOW() WHERE `id` = 10004 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 4,  `update_time` = NOW() WHERE `id` = 30100 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 5,  `update_time` = NOW() WHERE `id` = 10002 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 6,  `update_time` = NOW() WHERE `id` = 10003 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 7,  `update_time` = NOW() WHERE `id` = 20000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 8,  `update_time` = NOW() WHERE `id` = 30000 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 9,  `update_time` = NOW() WHERE `id` = 10001 AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 10, `update_time` = NOW() WHERE `id` = 1000  AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 11, `update_time` = NOW() WHERE `id` = 2000  AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 12, `update_time` = NOW() WHERE `id` = 3000  AND `deleted` = 0;
UPDATE `sys_menu` SET `sort` = 14, `update_time` = NOW() WHERE `id` = 8000  AND `deleted` = 0;



-- 库存串行锚点：业务事务按 tenant/warehouse/goods 排序锁定，不能作为可清理测试数据删除。
CREATE TABLE IF NOT EXISTS psi_stock_mutation_lock (
    tenant_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    goods_id bigint NOT NULL,
    PRIMARY KEY (tenant_id, warehouse_id, goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存变更串行锚点（包括零库存维度）';

-- 正式默认品牌，不引用上传目录或在用库配置。
UPDATE sys_option SET default_value='/brand-logo.png' WHERE code='SITE_LOGO';
UPDATE sys_option SET default_value='/favicon.png' WHERE code='SITE_FAVICON';

-- 同公司多成员账号与报修提交人；历史无提交人的记录仅公司管理员可见。

ALTER TABLE eoms_h5_customer_credential
 DROP PRIMARY KEY,
 ADD COLUMN id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
 ADD COLUMN display_name varchar(50) NOT NULL DEFAULT '',
 ADD COLUMN account_role varchar(16) NOT NULL DEFAULT 'MEMBER',
 ADD COLUMN status tinyint NOT NULL DEFAULT 1,
 ADD COLUMN auth_version bigint NOT NULL DEFAULT 1,
 ADD KEY idx_h5_member_customer(tenant_id,customer_id);
UPDATE eoms_h5_customer_credential SET display_name=login_name,account_role='ADMIN';

ALTER TABLE eoms_repair_apply ADD COLUMN reporter_account_id bigint NULL,
 ADD COLUMN reporter_name varchar(50) NULL,
 ADD KEY idx_repair_member(tenant_id,customer_id,reporter_account_id,del_flag);

-- 部署健康检查标记：仅当本文件整份执行完才出现，供 docker-compose MySQL healthcheck 使用
CREATE TABLE IF NOT EXISTS `_itls_init_done` (
  `id` tinyint NOT NULL PRIMARY KEY,
  `finished_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='init.sql 导入完成标记';
REPLACE INTO `_itls_init_done` (`id`, `finished_at`) VALUES (1, NOW());

SET FOREIGN_KEY_CHECKS = 1;
