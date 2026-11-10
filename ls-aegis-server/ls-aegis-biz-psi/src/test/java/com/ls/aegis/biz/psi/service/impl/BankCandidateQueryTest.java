package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.ls.aegis.biz.psi.mapper.BankStatementMapper;
import com.ls.aegis.biz.psi.mapper.CashLedgerMapper;
import com.ls.aegis.biz.psi.mapper.CompanyAccountMapper;
import com.ls.aegis.biz.psi.mapper.PsiPaymentMapper;
import com.ls.aegis.biz.psi.mapper.ReceiptMapper;
import com.ls.aegis.biz.psi.model.entity.BankStatementDO;
import com.ls.aegis.biz.psi.model.entity.PaymentDO;
import com.ls.aegis.biz.psi.model.entity.ReceiptDO;
import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import top.continew.starter.core.exception.BusinessException;

class BankCandidateQueryTest {
    @Intercepts(@Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
    static class Reads implements Interceptor {
        record Read(String mapper, int rows) {}
        final List<Read> queries = new ArrayList<>();

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            Object result = invocation.proceed();
            queries.add(new Read(((MappedStatement)invocation.getArgs()[0]).getId(),
                result instanceof List<?> rows ? rows.size() : 0));
            return result;
        }
    }

    private void candidates(boolean receipt) throws Exception {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        var db = new JdbcTemplate(ds);
        var reads = new Reads();
        var config = new MybatisConfiguration();
        GlobalConfigUtils.setGlobalConfig(config, new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()
            .setLogicDeleteField("deleted").setLogicDeleteValue("1").setLogicNotDeleteValue("0")));
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(), ds));
        config.addInterceptor(reads);
        config.addMapper(BankStatementMapper.class);
        config.addMapper(ReceiptMapper.class);
        config.addMapper(PsiPaymentMapper.class);
        for (Class<?> entity : List.of(BankStatementDO.class, ReceiptDO.class, PaymentDO.class)) {
            var table = TableInfoHelper.getTableInfo(entity);
            var columns = new ArrayList<String>();
            columns.add(table.getKeyColumn() + " bigint primary key");
            for (var field : table.getFieldList()) {
                Class<?> type = field.getField().getType();
                String sqlType = type == BigDecimal.class ? "decimal(18,2)"
                    : type == LocalDateTime.class ? "timestamp"
                    : type == Long.class || type == long.class ? "bigint"
                    : type == Integer.class || type == int.class || type == Boolean.class ? "int"
                    : "varchar(1024)";
                columns.add(field.getColumn() + " " + sqlType + (field.isLogicDelete() ? " default 0" : ""));
            }
            db.execute("create table " + table.getTableName() + "(" + String.join(",", columns) + ")");
        }
        String table = receipt ? "psi_receipt" : "psi_payment";
        String dayColumn = receipt ? "receipt_date" : "payment_date";
        String type = receipt ? "RECEIPT" : "PAYMENT";
        String direction = receipt ? "IN" : "OUT";
        db.update("insert into psi_bank_statement(id,account_id,amount,stmt_date,direction,match_status,tenant_id) "
            + "values(10,7,100,'2026-02-10 12:00:00',?,'UNMATCHED',0)", direction);
        db.update("insert into psi_bank_statement(id,account_id,amount,stmt_date,direction,match_status,match_biz_type,match_biz_id,tenant_id) "
            + "values(11,7,100,'2026-02-10 12:00:00',?,'MATCHED',?,3,0)", direction, type);
        Object[][] sample = {
            {1L, 7L, "CONFIRMED", "99.99", "2026-02-08 00:00:00", 0},
            {2L, 7L, "CONFIRMED", "100.01", "2026-02-12 23:59:59.999", 0},
            {3L, 7L, "CONFIRMED", "100.00", "2026-02-10 12:00:00", 0},
            {4L, 7L, "CONFIRMED", "100.00", "2026-02-13 00:00:00", 0},
            {5L, 7L, "DRAFT", "100.00", "2026-02-10 12:00:00", 0},
            {6L, 8L, "CONFIRMED", "100.00", "2026-02-10 12:00:00", 0},
            {7L, 7L, "CONFIRMED", "100.02", "2026-02-10 12:00:00", 0},
            {8L, 7L, "CONFIRMED", "100.00", "2026-02-10 12:00:00", 1},
        };
        String insert = "insert into " + table + "(id,account_id,doc_status,amount," + dayColumn
            + ",deleted,doc_no,tenant_id) values(?,?,?,?,?,?,?,0)";
        for (Object[] row : sample) db.update(insert, row[0], row[1], row[2], row[3], row[4], row[5], "DOC-" + row[0]);
        var noise = new ArrayList<Object[]>();
        for (long id = 100; id < 1100; id++) noise.add(new Object[] {
            id, 7L, "CONFIRMED", "500.00", "2025-01-01 00:00:00", 0, "OLD-" + id});
        db.batchUpdate(insert, noise);
        try (var session = new MybatisSqlSessionFactoryBuilder().build(config).openSession()) {
            @SuppressWarnings("unchecked")
            ObjectProvider<IGmCrypto> crypto = mock(ObjectProvider.class);
            var service = new BankStatementServiceImpl(session.getMapper(BankStatementMapper.class),
                mock(CompanyAccountMapper.class), session.getMapper(ReceiptMapper.class),
                session.getMapper(PsiPaymentMapper.class), crypto, mock(CashLedgerMapper.class), mock(IdempotentHelper.class));
            var options = service.matchCandidates(10L, type);
            assertEquals(List.of(1L, 2L), options.stream().map(o -> o.getValue()).sorted().toList());
            assertEquals("DOC-1｜¥99.99", options.stream().filter(o -> o.getValue() == 1L).findFirst().orElseThrow().getLabel());
            String prefix = (receipt ? ReceiptMapper.class : PsiPaymentMapper.class).getName() + ".";
            var documentReads = reads.queries.stream().filter(r -> r.mapper().startsWith(prefix)).toList();
            System.out.printf("BANK_MATCH_READS %s queries=%d rows=%d%n", type, documentReads.size(),
                documentReads.stream().mapToInt(Reads.Read::rows).sum());
            assertEquals(1, documentReads.size(), "候选列表不应逐个ID再次读取单据");
            assertEquals(3, documentReads.get(0).rows(), "数据库应先筛选金额和日期，不能把1000份无关单据加载到Java");
            assertFalse(documentReads.stream().anyMatch(r -> r.mapper().endsWith("selectById")));
        } finally {
            db.execute("SHUTDOWN");
        }
    }

    @Test void receiptCandidatesBoundDatabaseRowsAndAvoidPointReads() throws Exception { candidates(true); }
    @Test void paymentCandidatesBoundDatabaseRowsAndAvoidPointReads() throws Exception { candidates(false); }

    @Test void incompleteHistoricalStatementFailsBeforeReadingCandidateDocuments() {
        var bank = mock(BankStatementMapper.class);
        var receipts = mock(ReceiptMapper.class);
        var payments = mock(PsiPaymentMapper.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IGmCrypto> crypto = mock(ObjectProvider.class);
        var service = new BankStatementServiceImpl(bank, mock(CompanyAccountMapper.class), receipts,
            payments, crypto, mock(CashLedgerMapper.class), mock(IdempotentHelper.class));
        for (boolean missingDate : List.of(true, false)) {
            var statement = new BankStatementDO();
            statement.setAmount(missingDate ? new BigDecimal("100.00") : null);
            statement.setStmtDate(missingDate ? null : LocalDateTime.of(2026, 2, 10, 12, 0));
            when(bank.selectById(10L)).thenReturn(statement);
            assertThrows(BusinessException.class, () -> service.matchCandidates(10L, "RECEIPT"));
        }
        verifyNoInteractions(receipts, payments);
    }
}
