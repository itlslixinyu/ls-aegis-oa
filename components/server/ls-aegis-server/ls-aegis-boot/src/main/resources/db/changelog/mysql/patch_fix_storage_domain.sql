-- liquibase formatted sql

-- changeset ls:fix_storage_domain_for_nginx
-- comment 修正本地存储访问域名：8000→8080/api，并回写存量头像 URL
SET NAMES utf8mb4;

-- 存储域名：经前端 Nginx /api 反代访问本地文件
UPDATE `sys_storage`
SET `domain` = 'http://localhost:8080/api/file/',
    `update_user` = 1,
    `update_time` = NOW()
WHERE `domain` LIKE '%://localhost:8000/file%'
   OR `domain` LIKE '%://127.0.0.1:8000/file%';

-- 存量头像 URL 同步替换
UPDATE `sys_user`
SET `avatar` = REPLACE(`avatar`, 'http://localhost:8000/file/', 'http://localhost:8080/api/file/')
WHERE `avatar` LIKE 'http://localhost:8000/file/%';

UPDATE `sys_user`
SET `avatar` = REPLACE(`avatar`, 'http://127.0.0.1:8000/file/', 'http://localhost:8080/api/file/')
WHERE `avatar` LIKE 'http://127.0.0.1:8000/file/%';
