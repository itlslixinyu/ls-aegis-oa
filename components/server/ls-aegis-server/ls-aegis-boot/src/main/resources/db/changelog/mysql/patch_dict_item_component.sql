-- liquibase formatted sql

-- changeset aegis:patch_dict_item_component_path validCheckSum:ANY
-- comment 字典项菜单 component 指向已有字典页，避免空白路由
UPDATE `sys_menu`
SET `component` = 'system/dict/index'
WHERE `id` = 1140
  AND `component` = 'system/dict/item/index';
