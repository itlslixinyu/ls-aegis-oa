-- liquibaseFormattedSql
-- changeset ls:patch_security_clear_mail_host
-- comment: 安全加固：清空种子中的外网 SMTP 默认值 smtp.126.com

UPDATE `sys_option` SET `value` = NULL WHERE `code` = 'MAIL_HOST' AND `value` = 'smtp.126.com';
