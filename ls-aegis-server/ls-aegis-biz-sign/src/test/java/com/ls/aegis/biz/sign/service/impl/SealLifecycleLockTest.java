package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.domain.*;
import com.ls.aegis.biz.sign.domain.vo.*;
import com.ls.aegis.biz.sign.mapper.*;
import com.ls.aegis.common.context.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SealLifecycleLockTest {
    JdbcTemplate db; TransactionTemplate tx; SignSealMapper seals; SignRecordMapper records; SignSealServiceImpl service;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("CREATE TABLE sign_seal(seal_id BIGINT PRIMARY KEY,seal_name VARCHAR(50),status VARCHAR(1),del_flag VARCHAR(1),create_by VARCHAR(50))");
        db.execute("CREATE TABLE sign_record(record_id VARCHAR(32) PRIMARY KEY,seal_id BIGINT)");
        db.execute("INSERT INTO sign_seal VALUES(1,'test','0','0','admin')");
        var f=new SqlSessionFactoryBean();f.setDataSource(ds);f.setMapperLocations(new ClassPathResource("mapper/sign/SignDataScopeMapper.xml"),new ClassPathResource("mapper/sign/SignSealMapper.xml"),new ClassPathResource("mapper/sign/SignRecordMapper.xml"));
        var sessions=new SqlSessionTemplate(f.getObject());seals=sessions.getMapper(SignSealMapper.class);records=sessions.getMapper(SignRecordMapper.class);
        service=new SignSealServiceImpl();ReflectionTestUtils.setField(service,"sealMapper",seals);ReflectionTestUtils.setField(service,"signRecordMapper",records);
        actor();
    }
    void actor(){var a=new UserContext();a.setId(1L);a.setUsername("admin");UserContextHolder.setContext(a);}
    @AfterEach void cleanup(){UserContextHolder.clearContext();}
    @Test void deletedSealIsNotReturnedByExecutionLock(){
        db.update("UPDATE sign_seal SET del_flag='2' WHERE seal_id=1");
        tx.execute(s->{assertNull(seals.selectSealByIdForUpdate(1L));return null;});
    }
    @Test void usedSealCannotBeDeletedAndUnusedSealCan(){
        db.update("INSERT INTO sign_record VALUES('r1',1)");
        assertThrows(RuntimeException.class,()->tx.execute(s->service.deleteSealByIds(new Long[]{1L})));
        assertEquals("0",db.queryForObject("SELECT del_flag FROM sign_seal WHERE seal_id=1",String.class));
        db.update("DELETE FROM sign_record");
        assertEquals(1,tx.execute(s->service.deleteSealByIds(new Long[]{1L})).intValue());
    }
    @Test void signingAnchorMakesConcurrentDeleteObserveCommittedAudit() throws Exception {
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var attempted=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var writer=pool.submit(()->tx.execute(s->{assertNotNull(seals.selectSealByIdForUpdate(1L));locked.countDown();await(release);db.update("INSERT INTO sign_record VALUES('r2',1)");return true;}));
            assertTrue(locked.await(3,TimeUnit.SECONDS));
            var deleter=pool.submit(()->{actor();try{return tx.execute(s->{attempted.countDown();return service.deleteSealByIds(new Long[]{1L});});}finally{UserContextHolder.clearContext();}});
            assertTrue(attempted.await(3,TimeUnit.SECONDS));release.countDown();assertTrue(writer.get(6,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->deleter.get(6,TimeUnit.SECONDS));
            assertEquals("0",db.queryForObject("SELECT del_flag FROM sign_seal WHERE seal_id=1",String.class));
            assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM sign_record",Integer.class));
        } finally {release.countDown();pool.shutdownNow();}
    }
    static void await(CountDownLatch latch){try{if(!latch.await(4,TimeUnit.SECONDS))throw new IllegalStateException("test gate timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
    @Test void executionValidationUsesLockedReadAndRejectsDeletedRows() {
        var execute=new SignExecuteServiceImpl();var sealMapper=mock(SignSealMapper.class);var projects=mock(SignProjectMapper.class);
        var project=new SignProject();project.setStatus(SignProject.STATUS_IN_PROGRESS);when(projects.selectProjectById(5L)).thenReturn(project);
        ReflectionTestUtils.setField(execute,"projectMapper",projects);ReflectionTestUtils.setField(execute,"sealMapper",sealMapper);
        ReflectionTestUtils.setField(execute,"requestNormalizer",mock(SignRequestNormalizer.class));
        var request=new SignRequest();request.setProjectId(5L);request.setSealId(1L);request.setSignMode("single");
        var pos=new SignPosition();pos.setPage(1);pos.setX(1f);pos.setY(1f);pos.setWidth(10f);pos.setHeight(10f);request.setPositions(List.of(pos));
        var deleted=new SignSeal();deleted.setSealId(1L);deleted.setStatus("0");deleted.setDelFlag("2");when(sealMapper.selectSealByIdForUpdate(1L)).thenReturn(deleted);
        var error=assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(execute,"validateExecutableRequest",request,1L,true));
        assertTrue(error.getMessage().contains("印章不可用"));verify(sealMapper).selectSealByIdForUpdate(1L);verify(sealMapper,never()).selectSealById(anyLong());
    }
}
