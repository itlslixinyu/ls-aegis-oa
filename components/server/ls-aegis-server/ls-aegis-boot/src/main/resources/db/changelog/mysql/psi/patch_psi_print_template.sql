-- liquibase formatted sql

-- changeset psi:13 validCheckSum:ANY
-- comment PSI：打印模板表 + 菜单（P1 浏览器打印简版）

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PSI 打印模板';

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
