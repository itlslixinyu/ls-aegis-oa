-- liquibaseFormattedSql
-- changeset ls:patch_login_auth_account_email_only
-- comment: 登录仅保留账号密码与邮箱认证，移除客户端 PHONE/SOCIAL

UPDATE `sys_client`
SET `auth_type` = JSON_ARRAY('ACCOUNT', 'EMAIL')
WHERE `client_id` = 'ef51c9a3e9046c4f2ea45142c8a8344a';
