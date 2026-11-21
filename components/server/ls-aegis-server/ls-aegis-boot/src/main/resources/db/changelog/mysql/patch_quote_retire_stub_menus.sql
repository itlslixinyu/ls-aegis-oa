-- liquibase formatted sql

-- changeset ls:quote_retire_stub_menus
-- comment 下线报价半成品菜单 40002/40003/40004（编辑/历史/设置）；在线编辑走列表弹窗
SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (40002, 40003, 40004);
DELETE FROM `sys_menu` WHERE `id` IN (40002, 40003, 40004);
