package com.ls.aegis.biz.eoms.finance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.repair.*;
import com.ls.aegis.biz.eoms.service.impl.repair.RepairRecordServiceImpl;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.service.h5.H5AttachmentService;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Current repository SQL and real private transactions; visibility is injected at its public service boundary. */
class RepairRecordBoundaryTransactionTest {
    JdbcTemplate db; TransactionTemplate tx; RepairRecordServiceImpl service;
    RepairRecordMapper records; RepairOrderMapper orders;
    IRepairOrderService visibleOrders = mock(IRepairOrderService.class);
    EomsPlatformFileHelper files = mock(EomsPlatformFileHelper.class);
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("create table eoms_repair_order(id bigint primary key,tenant_id bigint,dept_id bigint,create_user bigint,engineer_id bigint,status varchar(32),del_flag char(1))");
        db.execute("create table eoms_repair_record(id bigint auto_increment primary key,repair_order_id bigint,fault_reason varchar(100),check_process varchar(100),solution varchar(100),result varchar(100),processing_photos varchar(200),completed_photos varchar(200),create_by varchar(64),create_user bigint,create_time timestamp,update_by varchar(64),update_time timestamp,remark varchar(100),del_flag char(1),tenant_id bigint,dept_id bigint)");
        db.update("insert into eoms_repair_order values(1,0,20,3,4,'PROCESSING','0'),(2,0,30,5,6,'PROCESSING','0')");
        var cfg=new MybatisConfiguration();cfg.getTypeAliasRegistry().registerAlias("RepairOrder",RepairOrder.class);cfg.getTypeAliasRegistry().registerAlias("RepairRecord",RepairRecord.class);cfg.setEnvironment(new Environment("records",new SpringManagedTransactionFactory(),ds));
        for(String name:List.of("EomsDataScopeMapper.xml","RepairRecordMapper.xml","RepairOrderMapper.xml")) {
            String xml=Files.readString(Path.of("src/main/resources/mapper/eoms/"+name)).replace("sysdate()","CURRENT_TIMESTAMP");
            try(var in=new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))){new XMLMapperBuilder(in,cfg,name,cfg.getSqlFragments()).parse();}
        }
        var sessions=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(cfg));records=sessions.getMapper(RepairRecordMapper.class);orders=sessions.getMapper(RepairOrderMapper.class);
        service=spy(new RepairRecordServiceImpl());ReflectionTestUtils.setField(service,"repairRecordMapper",records);ReflectionTestUtils.setField(service,"repairOrderMapper",orders);ReflectionTestUtils.setField(service,"repairOrderService",visibleOrders);ReflectionTestUtils.setField(service,"platformFileHelper",files);ReflectionTestUtils.setField(service,"h5AttachmentService",mock(H5AttachmentService.class));
        when(visibleOrders.selectRepairOrderById(1L)).thenAnswer(i->orders.lockOrder(1L));
    }
    RepairRecord fresh(){var r=new RepairRecord();r.setRepairOrderId(1L);r.setSolution("fixture");return r;}
    void seed(){db.update("insert into eoms_repair_record(id,repair_order_id,solution,del_flag,tenant_id,dept_id) values(10,1,'original','0',0,20)");var r=new RepairRecord();r.setId(10L);r.setRepairOrderId(1L);r.setTenantId(0L);r.setDeptId(20L);doReturn(r).when(service).selectRepairRecordById(10L);}
    @Test void concurrentSameOrderInsertHasOneWinnerAndOneRecordUsingActualOrderAnchorAndCurrentRecordLocks() throws Exception {
        var barrier=new CyclicBarrier(2);
        when(visibleOrders.selectRepairOrderById(1L)).thenAnswer(i->{var r=new RepairOrder();r.setId(1L);r.setTenantId(0L);r.setDeptId(20L);r.setCreateUser(3L);r.setEngineerId(4L);r.setStatus("PROCESSING");barrier.await(3,TimeUnit.SECONDS);return r;});
        var pool=Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> attempts=new ArrayList<>();for(int i=0;i<2;i++)attempts.add(pool.submit(()->{try{return tx.execute(s->service.insertRepairRecord(fresh()))==1;}catch(ServiceException e){return false;}}));
            int passed=0;for(var task:attempts)if(task.get(10,TimeUnit.SECONDS))passed++;assertEquals(1,passed);assertEquals(1,db.queryForObject("select count(*) from eoms_repair_record where del_flag='0'",Integer.class));
        }finally{pool.shutdownNow();}
    }
    @Test void missingOrInvisibleOrderCannotAcceptARecord(){when(visibleOrders.selectRepairOrderById(1L)).thenReturn(null);assertThrows(ServiceException.class,()->tx.execute(s->service.insertRepairRecord(fresh())));assertEquals(0,db.queryForObject("select count(*) from eoms_repair_record",Integer.class));}
    @Test void recordCannotBeMovedToAnotherOrder(){seed();var update=new RepairRecord();update.setId(10L);update.setRepairOrderId(2L);assertThrows(ServiceException.class,()->tx.execute(s->service.updateRepairRecord(update)));assertEquals(1L,db.queryForObject("select repair_order_id from eoms_repair_record where id=10",Long.class));}
    @Test void closedAndWaitingSettlementStatesRejectInsertUpdateAndDeleteWithoutMutation(){
        seed();for(String status:List.of("WAIT_SETTLEMENT","FINISHED","CLOSED")) {
            db.update("update eoms_repair_order set status=? where id=1",status);var update=new RepairRecord();update.setId(10L);update.setSolution("forged");
            assertThrows(ServiceException.class,()->tx.execute(s->service.insertRepairRecord(fresh())));assertThrows(ServiceException.class,()->tx.execute(s->service.updateRepairRecord(update)));assertThrows(ServiceException.class,()->tx.execute(s->service.deleteRepairRecordByIds(new Long[]{10L})));
        }assertEquals("original",db.queryForObject("select solution from eoms_repair_record where id=10",String.class));assertEquals("0",db.queryForObject("select del_flag from eoms_repair_record where id=10",String.class));
    }
    @Test void invisibleRecordAndMixedBatchAreRejectedBeforeAnyDelete(){seed();doReturn(null).when(service).selectRepairRecordById(11L);assertThrows(ServiceException.class,()->tx.execute(s->service.deleteRepairRecordByIds(new Long[]{10L,11L})));assertEquals("0",db.queryForObject("select del_flag from eoms_repair_record where id=10",String.class));verifyNoInteractions(files);}
    @Test void legitimateUpdateAndDeleteKeepPhotoBytesOutsideTheRecordTransaction(){seed();var update=new RepairRecord();update.setId(10L);update.setSolution("corrected");assertEquals(1,tx.execute(s->service.updateRepairRecord(update)).intValue());assertEquals(1,tx.execute(s->service.deleteRepairRecordByIds(new Long[]{10L})).intValue());verifyNoInteractions(files);assertEquals("2",db.queryForObject("select del_flag from eoms_repair_record where id=10",String.class));}
}
