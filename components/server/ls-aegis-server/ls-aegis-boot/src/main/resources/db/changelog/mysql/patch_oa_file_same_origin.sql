-- liquibase formatted sql

-- changeset ls:oa_file_same_origin_20261111
-- comment 本地存储使用同源入口，仅修正旧模板默认地址，不覆盖自定义域名。
UPDATE sys_storage SET domain='/api/file/' WHERE type=1 AND domain='http://localhost:8080/api/file/';
