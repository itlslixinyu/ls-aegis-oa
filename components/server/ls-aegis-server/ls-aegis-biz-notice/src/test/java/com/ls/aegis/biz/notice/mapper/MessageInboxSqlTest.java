package com.ls.aegis.biz.notice.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.notice.model.query.MessageQuery;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MessageInboxSqlTest {
    private MybatisConfiguration configuration(String db) throws Exception {
        MybatisConfiguration config = new MybatisConfiguration(); config.setDatabaseId(db);
        for (String file : List.of("MessageMapper.xml", "MessageLogMapper.xml")) {
            try (InputStream input = Files.newInputStream(Path.of("src/main/resources/mapper/" + file))) {
                new XMLMapperBuilder(input, config, file, config.getSqlFragments()).parse();
            }
        }
        return config;
    }
    private BoundSql bound(MybatisConfiguration config, String mapper, String method, Object parameters) {
        return config.getMappedStatement("com.ls.aegis.biz.notice.mapper." + mapper + "." + method).getBoundSql(parameters);
    }

    @Test
    void allReadPathsBindCurrentRecipientAndExcludePersonalTombstonesInBothDialects() throws Exception {
        for (String db : List.of("mysql", "pgsql")) {
            MybatisConfiguration config = configuration(db);
            MessageQuery query = new MessageQuery(); query.setUserId(101L);
            for (String method : List.of("selectMessagePage", "selectMessageById", "selectUnreadIdsByUserId", "selectUnreadCountByUserIdAndType", "selectUnreadCountsByType")) {
                Map<String, Object> params = new java.util.HashMap<>();
                params.put("query", query); params.put("id", 1L); params.put("userId", 101L); params.put("type", null); params.put("ids", null);
                BoundSql sql = bound(config, "MessageMapper", method, params);
                assertTrue(sql.getSql().contains("COALESCE(t2.is_deleted, FALSE) = FALSE"), method);
                assertTrue(sql.getSql().contains("t2.user_id ="), method);
                assertTrue(sql.getSql().contains(db.equals("mysql") ? "JSON_CONTAINS" : "jsonb_build_array"), method);
                assertTrue(sql.getParameterMappings().stream().anyMatch(p -> p.getProperty().equals("userId") || p.getProperty().equals("query.userId")), method);
            }
            // 授权查询有意保留个人墓碑：重复删除仍能通过权限校验，且只检查原始接收范围。
            BoundSql access = bound(config, "MessageMapper", "selectAccessibleIds", Map.of("ids", List.of(1L, 2L), "userId", 101L));
            assertFalse(access.getSql().contains("is_deleted"));
            assertTrue(access.getSql().contains("t1.deleted = 0"));
            assertEquals(1L, access.getAdditionalParameter("__frch_id_0"));
            assertEquals(2L, access.getAdditionalParameter("__frch_id_1"));
        }
    }

    @Test
    void deleteAndReadUseCompositeKeyUpsertsPreservingOtherUsersAndDeletedState() throws Exception {
        for (String db : List.of("mysql", "pgsql")) {
            MybatisConfiguration config = configuration(db);
            Map<String, Object> params = Map.of("ids", List.of(1L, 2L), "userId", 101L, "readTime", LocalDateTime.of(2026, 11, 5, 12, 0));
            BoundSql deletion = bound(config, "MessageLogMapper", "markDeleted", params);
            assertTrue(deletion.getSql().contains("INSERT INTO sys_message_log"));
            assertTrue(deletion.getSql().contains(db.equals("mysql") ? "ON DUPLICATE KEY UPDATE is_deleted = TRUE" : "ON CONFLICT (message_id,user_id) DO UPDATE SET is_deleted = TRUE"));
            assertFalse(deletion.getSql().contains("DELETE FROM"));
            assertEquals(2, deletion.getParameterMappings().stream().filter(p -> p.getProperty().equals("userId")).count());
            BoundSql read = bound(config, "MessageLogMapper", "markRead", params);
            assertTrue(read.getSql().contains("is_deleted = FALSE"));
            assertTrue(read.getSql().contains("COALESCE"));
            assertFalse(read.getSql().contains("SET is_deleted = FALSE"));
        }
    }
}
