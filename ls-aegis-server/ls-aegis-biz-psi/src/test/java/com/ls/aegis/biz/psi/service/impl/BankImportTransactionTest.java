package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.biz.psi.enums.BankDirectionEnum;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.BankImportReq;
import com.ls.aegis.common.context.*;
import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.mybatis.config.MyBatisPlusMetaObjectHandler;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual MyBatis SQL and two Spring transactions, with a real account-row lock and real unique indexes. */
class BankImportTransactionTest {
    JdbcTemplate jdbc;TransactionTemplate tx;BankStatementServiceImpl service;
    @BeforeEach @SuppressWarnings("unchecked") void setup()throws Exception{
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000","sa","");ds.setDriverClassName("org.h2.Driver");jdbc=new JdbcTemplate(ds);
        tx=new TransactionTemplate(new DataSourceTransactionManager(ds));tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        var config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);
        var global=GlobalConfigUtils.getGlobalConfig(config);global.getDbConfig().setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id");global.setMetaObjectHandler(new MyBatisPlusMetaObjectHandler());
        config.addMapper(BankStatementMapper.class);var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);
        var mapper=new SqlSessionTemplate(factory.getObject()).getMapper(BankStatementMapper.class);
        var table=TableInfoHelper.getTableInfo(BankStatementDO.class);var ddl=new StringBuilder("CREATE TABLE psi_bank_statement(id bigint primary key");
        for(var field:table.getFieldList()){
            Class<?> type=field.getPropertyType();String sqlType=type==Long.class?"bigint":type==BigDecimal.class?"decimal(18,2)":type==LocalDateTime.class?"timestamp":"varchar(512)";
            ddl.append(',').append(field.getColumn()).append(' ').append(sqlType);if(field.getProperty().equals("deleted"))ddl.append(" DEFAULT 0");
        }
        ddl.append(",UNIQUE(tenant_id,account_id,fingerprint,deleted),UNIQUE(tenant_id,account_id,bank_seq_no,deleted))");jdbc.execute(ddl.toString());
        jdbc.execute("CREATE TABLE account_anchor(id bigint primary key)");jdbc.update("INSERT INTO account_anchor VALUES(1)");
        var accounts=mock(CompanyAccountMapper.class);
        when(accounts.selectByIdForUpdate(1L)).thenAnswer(inv->{jdbc.queryForObject("SELECT id FROM account_anchor WHERE id=1 FOR UPDATE",Long.class);var a=new CompanyAccountDO();a.setId(1L);a.setTenantId(0L);return a;});
        service=new BankStatementServiceImpl(mapper,accounts,mock(ReceiptMapper.class),mock(PsiPaymentMapper.class),mock(ObjectProvider.class),mock(CashLedgerMapper.class),mock(IdempotentHelper.class));
    }
    @AfterEach void close(){UserContextHolder.clearContext();jdbc.execute("SHUTDOWN");}
    private void user(){var u=new UserContext();u.setId(99L);u.setDeptId(12L);u.setTenantId(0L);UserContextHolder.setContext(u,false);}
    private BankImportReq request(String seq,String amount){
        var line=new BankImportReq.Item();line.setStmtDate(LocalDateTime.of(2026,11,6,1,0));line.setDirection(BankDirectionEnum.IN);line.setAmount(new BigDecimal(amount));line.setCounterparty("synthetic");line.setSummary("same economic fields");line.setBankSeqNo(seq);
        var req=new BankImportReq();req.setAccountId(1L);req.setItems(List.of(line));return req;
    }
    private int run(BankImportReq req){user();try{return tx.execute(status->service.importStatements(req));}finally{UserContextHolder.clearContext();}}
    @Test void distinctSequencesRetainBothButConcurrentSameSequenceInsertsOnlyOne()throws Exception{
        var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        try{
            var a=pool.submit(()->{gate.await();return run(request("seq-1","300.00"));});var b=pool.submit(()->{gate.await();return run(request("seq-1","300.00"));});gate.countDown();
            assertEquals(1,a.get(10,TimeUnit.SECONDS)+b.get(10,TimeUnit.SECONDS));assertEquals(1,run(request("seq-2","300.00")));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM psi_bank_statement",Integer.class));
        }finally{pool.shutdownNow();}
    }
    @Test void noSequenceNormalizesScaleAndRecognizesUnmodifiedLegacyFingerprint(){
        assertEquals(1,run(request(null,"300.0")));
        jdbc.update("UPDATE psi_bank_statement SET fingerprint='historical-different-hash',dept_id=999");
        assertEquals(0,run(request(null,"300.00")));
        assertEquals("historical-different-hash",jdbc.queryForObject("SELECT fingerprint FROM psi_bank_statement",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM psi_bank_statement",Integer.class));
    }
    @Test void noSequenceConcurrentReplaysAlsoHaveSinglePersistedIdentity()throws Exception{
        var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        try{var a=pool.submit(()->{gate.await();return run(request(null,"300.0"));});var b=pool.submit(()->{gate.await();return run(request(null,"300.00"));});gate.countDown();assertEquals(1,a.get(10,TimeUnit.SECONDS)+b.get(10,TimeUnit.SECONDS));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM psi_bank_statement",Integer.class));}
        finally{pool.shutdownNow();}
    }
}
