-- liquibaseFormattedSql
-- changeset ls:patch_gm_seed_password_sm3
-- comment: 整体国密：种子用户口令由 BCrypt 切换为 SM3（admin/admin123、test/test123）

UPDATE `sys_user`
SET `password` = '{sm3}v1$ef4114fbfe4881f4710b626429c97c04$bd43c9a531b4cc2705aaff6d1a2f9563906c254cc3ffe05fbb21c2456bd86d99'
WHERE `id` = 1 AND `username` = 'admin';

UPDATE `sys_user`
SET `password` = '{sm3}v1$cfa6968c5df393377b04ff6e33f6f90c$025649846502c230dbfcff724d5724a2aa0af1c2a97defc84da59c70b9df24c8'
WHERE `id` = 547889293968801822 AND `username` = 'test';
