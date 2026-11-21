-- liquibase formatted sql

-- changeset ls:clear_broken_avatar_after_storage_path_fix
-- comment 清空因相对路径未落挂载卷而失效的头像，用户需重新上传
SET NAMES utf8mb4;

UPDATE `sys_user`
SET `avatar` = NULL
WHERE `avatar` LIKE '%/file/user/avatar/%';
