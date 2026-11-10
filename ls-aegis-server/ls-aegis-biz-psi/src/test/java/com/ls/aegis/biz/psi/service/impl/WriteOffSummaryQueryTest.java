package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.ls.aegis.biz.psi.mapper.CashLedgerMapper;
import com.ls.aegis.biz.psi.mapper.PayableMapper;
import com.ls.aegis.biz.psi.mapper.PsiReceivableMapper;
import com.ls.aegis.biz.psi.mapper.StockInMapper;
import com.ls.aegis.biz.psi.mapper.StockOutMapper;
import com.ls.aegis.biz.psi.mapper.WriteOffItemMapper;
import com.ls.aegis.biz.psi.mapper.WriteOffMapper;
import com.ls.aegis.biz.psi.model.entity.WriteOffDO;
import com.ls.aegis.biz.psi.model.entity.WriteOffItemDO;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.PsiTodoCloseSupport;
import com.ls.aegis.biz.psi.support.WriteOffHelper;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class WriteOffSummaryQueryTest {
    @Intercepts(@Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
    static class Reads implements Interceptor {
        int count;
        @Override public Object intercept(Invocation invocation) throws Throwable {
            count++;
            return invocation.proceed();
        }
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void preservesSummarySemanticsWithBoundedBatchReads(boolean receipt) throws Exception { check(receipt, 120); }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void largeHistoriesSplitPredicatesIntoBoundedBatches(boolean receipt) throws Exception { check(receipt, 450); }

    private void check(boolean receipt, int size) throws Exception {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        var db = new JdbcTemplate(ds);
        var config = new MybatisConfiguration();
        GlobalConfigUtils.setGlobalConfig(config, new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()
            .setLogicDeleteField("deleted").setLogicDeleteValue("1").setLogicNotDeleteValue("0")));
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(), ds));
        var reads = new Reads();
        config.addInterceptor(reads);
        config.addMapper(WriteOffMapper.class);
        config.addMapper(WriteOffItemMapper.class);
        for (Class<?> entity : List.of(WriteOffDO.class, WriteOffItemDO.class)) {
            var table = TableInfoHelper.getTableInfo(entity);
            var columns = new ArrayList<String>();
            columns.add(table.getKeyColumn() + " bigint primary key");
            for (var field : table.getFieldList()) {
                Class<?> type = field.getField().getType();
                String sqlType = type == BigDecimal.class ? "decimal(18,2)" : type == LocalDateTime.class ? "timestamp"
                    : type == Long.class || type == long.class ? "bigint"
                    : type == Integer.class || type == Boolean.class ? "int" : "varchar(1024)";
                columns.add(field.getColumn() + " " + sqlType + (field.isLogicDelete() ? " default 0" : ""));
            }
            db.execute("create table " + table.getTableName() + "(" + String.join(",", columns) + ")");
        }
        String cashColumn = receipt ? "receipt_id" : "payment_id";
        for (int group = 1; group <= 6; group++) {
            db.update("insert into psi_write_off(id,remark,write_off_date,deleted) values(?,?,?,?)",
                group, "主单" + group, "2026-11-08 12:00:00", group == 6 ? 1 : 0);
        }
        for (int i = 1; i <= size; i++) {
            int group = (i - 1) % 6 + 1;
            db.update("insert into psi_write_off_item(id,write_off_id," + cashColumn
                + ",direction,amount,deleted) values(?,?,7,'WRITE_OFF',1.25,0)", i, group);
        }
        // 同一原明细重复冲销、已删除冲销、相似ID及其他资金单，不改变原有判断规则。
        String insert = "insert into psi_write_off_item(id,write_off_id," + cashColumn
            + ",direction,amount,reason,deleted) values(?,999,99,?,1.25,?,?)";
        db.update(insert, 10001, "REVERSE", "REV_OF:1|撤销", 0);
        db.update(insert, 10002, "REVERSE", "REV_OF:1|重复资料", 0);
        db.update(insert, 10003, "REVERSE", "REV_OF:2|已删除", 1);
        db.update(insert, 10004, "REVERSE", "REV_OF:20000|无关", 0);
        db.update(insert, 10005, "WRITE_OFF", "REV_OF:3|方向不同", 0);
        try (var session = new MybatisSqlSessionFactoryBuilder().build(config).openSession()) {
            var helper = new WriteOffHelper(session.getMapper(WriteOffMapper.class), session.getMapper(WriteOffItemMapper.class),
                mock(PsiReceivableMapper.class), mock(PayableMapper.class), mock(StockOutMapper.class), mock(StockInMapper.class),
                mock(DocNoGenerator.class), mock(PsiTodoCloseSupport.class), mock(CashLedgerMapper.class));
            var summaries = helper.listSummaries(receipt ? 7L : null, receipt ? null : 7L);
            assertEquals(List.of(6L, 5L, 4L, 3L, 2L), summaries.stream().map(s -> s.getWriteOffId()).toList());
            for (var summary : summaries) assertEquals(new BigDecimal("1.25").multiply(BigDecimal.valueOf(size / 6)), summary.getAmount());
            assertNull(summaries.get(0).getReason(), "已删除主单不能通过批量读取重新显示");
            assertEquals("主单5", summaries.get(1).getReason());
            System.out.printf("WRITE_OFF_SUMMARY_READS cash=%s items=%d queries=%d%n", cashColumn, size, reads.count);
            assertEquals(2 + (size + 199) / 200, reads.count, "主单与反核销不得逐条查询");
            reads.count = 0;
            assertEquals(List.of(), helper.listSummaries(receipt ? 77L : null, receipt ? null : 77L));
            assertEquals(1, reads.count, "空历史不能形成空IN或全表读取");
        } finally { db.execute("SHUTDOWN"); }
    }
}
