-- liquibase formatted sql

-- changeset psi:5 validCheckSum:ANY
-- comment PSI 目录/菜单图标校正：全部使用 assets/icons 中存在的 SVG，避免空白图标
UPDATE `sys_menu` SET `icon` = 'storage' WHERE `id` = 10000;
UPDATE `sys_menu` SET `icon` = 'folder' WHERE `id` = 10001;
UPDATE `sys_menu` SET `icon` = 'archive' WHERE `id` = 10002;
UPDATE `sys_menu` SET `icon` = 'stamp' WHERE `id` = 10003;
UPDATE `sys_menu` SET `icon` = 'menu-file' WHERE `id` = 10004;
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
