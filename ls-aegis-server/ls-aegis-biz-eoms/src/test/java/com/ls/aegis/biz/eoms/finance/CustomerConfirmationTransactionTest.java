package com.ls.aegis.biz.eoms.finance;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.controller.h5.H5RepairController;
import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.interceptor.H5TokenInterceptor;
import com.ls.aegis.biz.eoms.mapper.repair.CustomerConfirmationMapper;
import com.ls.aegis.biz.eoms.model.resp.CustomerConfirmationResp;
import com.ls.aegis.biz.eoms.service.h5.CustomerConfirmationService;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.common.context.*;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual annotation SQL and Spring transactions, never live data or tokens. */
class CustomerConfirmationTransactionTest {
    static org.springframework.context.support.GenericApplicationContext responseContext;
    @BeforeAll static void platformResponseFactory(){
        responseContext=new org.springframework.context.support.GenericApplicationContext();
        responseContext.registerBean(com.feiniaojin.gracefulresponse.api.ResponseStatusFactory.class,()->new com.feiniaojin.gracefulresponse.api.ResponseStatusFactory(){
            public com.feiniaojin.gracefulresponse.data.ResponseStatus defaultSuccess(){return new com.feiniaojin.gracefulresponse.defaults.DefaultResponseStatus("0","ok");}
            public com.feiniaojin.gracefulresponse.data.ResponseStatus defaultError(){return new com.feiniaojin.gracefulresponse.defaults.DefaultResponseStatus("1","error");}
            public com.feiniaojin.gracefulresponse.data.ResponseStatus newInstance(String code,String message){return new com.feiniaojin.gracefulresponse.defaults.DefaultResponseStatus(code,message);}
        });responseContext.refresh();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(responseContext);
    }
    @AfterAll static void closeResponseContext(){responseContext.close();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(null);}
    JdbcTemplate db;DataSourceTransactionManager manager;CustomerConfirmationMapper mapper;CustomerConfirmationService service;
    final IRepairOrderService orderService=mock(IRepairOrderService.class);
    @BeforeEach void setup(){
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");db=new JdbcTemplate(ds);manager=new DataSourceTransactionManager(ds);
        db.execute("create table eoms_repair_order(id bigint primary key,tenant_id bigint,customer_id bigint,status varchar(30),del_flag char default '0')");
        db.execute("create table eoms_finance_event(id bigint auto_increment primary key,aggregate_type varchar(20),aggregate_id bigint,action varchar(20),related_id bigint,amount decimal(12,2),operator varchar(64),reason varchar(500),create_time timestamp,unique(aggregate_type,aggregate_id,action))");
        db.update("insert into eoms_repair_order(id,tenant_id,customer_id,status) values(10,0,100,'WAIT_SETTLEMENT'),(11,7,100,'WAIT_SETTLEMENT'),(12,0,200,'WAIT_SETTLEMENT')");
        var c=new MybatisConfiguration();c.setEnvironment(new Environment("test",new SpringManagedTransactionFactory(),ds));c.addMapper(CustomerConfirmationMapper.class);
        mapper=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(c)).getMapper(CustomerConfirmationMapper.class);service=proxy(mapper);
        var user=new UserContext();user.setId(99L);user.setTenantId(0L);UserContextHolder.setContext(user,false);
    }
    CustomerConfirmationService proxy(CustomerConfirmationMapper calls){var target=new CustomerConfirmationService(calls,orderService);var factory=new ProxyFactory(target);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));return (CustomerConfirmationService)factory.getProxy();}
    @AfterEach void clear(){UserContextHolder.clearContext();db.execute("SHUTDOWN");}
    int events(){return db.queryForObject("select count(*) from eoms_finance_event",Integer.class);}
    @ParameterizedTest @ValueSource(strings={"WAIT_SETTLEMENT","FINISHED"}) void eligibleCustomerConfirmationPersistsExactIdentityWithoutChangingBusinessState(String status){
        db.update("update eoms_repair_order set status=? where id=10",status);assertFalse(service.forCustomer(0L,100L,10L).isConfirmed());var result=service.confirm(0L,100L,10L,"accepted repair");
        assertTrue(result.isConfirmed());assertEquals("10",result.getOrderId());assertEquals("100",result.getCustomerId());assertNotNull(result.getConfirmedAt());assertEquals("accepted repair",result.getReason());
        assertEquals(status,db.queryForObject("select status from eoms_repair_order where id=10",String.class));assertEquals(1,events());assertNull(db.queryForObject("select amount from eoms_finance_event",java.math.BigDecimal.class));assertEquals("CUSTOMER:100",db.queryForObject("select operator from eoms_finance_event",String.class));assertEquals(100L,db.queryForObject("select related_id from eoms_finance_event",Long.class));
    }
    @ParameterizedTest @ValueSource(strings={"WAIT_ACCEPT","ACCEPTED","PROCESSING","CLOSED","REVERSED"}) void illegalStateCannotProduceAConfirmation(String status){db.update("update eoms_repair_order set status=? where id=10",status);assertThrows(ServiceException.class,()->service.confirm(0L,100L,10L,"invalid state"));assertEquals(0,events());}
    @Test void customerAndTenantOwnershipIsRequiredForBothMutationAndQuery(){
        assertThrows(ServiceException.class,()->service.confirm(0L,200L,10L,"wrong customer"));assertThrows(ServiceException.class,()->service.confirm(7L,100L,10L,"wrong tenant"));assertThrows(ServiceException.class,()->service.confirm(0L,100L,11L,"foreign order"));assertThrows(ServiceException.class,()->service.confirm(0L,100L,12L,"other customer"));
        assertThrows(ServiceException.class,()->service.forCustomer(0L,200L,10L));assertThrows(ServiceException.class,()->service.forCustomer(7L,100L,10L));assertEquals(0,events());
    }
    @Test void replayKeepsOriginalReasonIdentityAndTimeAndCannotBypassCurrentOwnership(){
        var first=service.confirm(0L,100L,10L,"first reason");var replay=service.confirm(0L,100L,10L,"replacement must not overwrite");assertEquals(first,replay);assertEquals(1,events());
        assertThrows(ServiceException.class,()->service.confirm(0L,200L,10L,"cannot replay another customer's confirmation"));
        db.update("update eoms_repair_order set status='CLOSED' where id=10");assertThrows(ServiceException.class,()->service.confirm(0L,100L,10L,"closed cannot confirm"));assertEquals(first,service.forCustomer(0L,100L,10L));assertEquals(1,events());
    }
    @Test void twoConcurrentConfirmationsSerializeOnOwnedOrderAndProduceExactlyOneEvent()throws Exception{
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{List<Future<CustomerConfirmationResp>> jobs=new ArrayList<>();for(String reason:List.of("first contender","second contender"))jobs.add(pool.submit(()->{start.await();return service.confirm(0L,100L,10L,reason);}));start.countDown();assertEquals(jobs.get(0).get(10,TimeUnit.SECONDS),jobs.get(1).get(10,TimeUnit.SECONDS));assertEquals(1,events());assertEquals("WAIT_SETTLEMENT",db.queryForObject("select status from eoms_repair_order where id=10",String.class));}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void failureAfterActualInsertRollsBackTheAuditAndLeavesOrderUnchanged(){
        var calls=mock(CustomerConfirmationMapper.class,org.mockito.AdditionalAnswers.delegatesTo(mapper));var reads=new AtomicInteger();doAnswer(inv->{if(reads.incrementAndGet()==2)throw new ServiceException("synthetic readback failure");return mapper.confirmation(inv.getArgument(0),inv.getArgument(1),inv.getArgument(2));}).when(calls).confirmation(anyLong(),anyLong(),anyLong());
        assertThrows(ServiceException.class,()->proxy(calls).confirm(0L,100L,10L,"must rollback"));assertEquals(0,events());assertEquals("WAIT_SETTLEMENT",db.queryForObject("select status from eoms_repair_order where id=10",String.class));assertTrue(service.confirm(0L,100L,10L,"retry after rollback").isConfirmed());
    }
    @Test void malformedAuditIdentityIsRejectedRatherThanReturnedOrReplaced(){db.update("insert into eoms_finance_event(aggregate_type,aggregate_id,action,related_id,operator,reason,create_time) values('ORDER',10,'CUSTOMER_CONFIRM',200,'CUSTOMER:200','legacy mismatch',CURRENT_TIMESTAMP)");assertThrows(ServiceException.class,()->service.confirm(0L,100L,10L,"must not replace"));assertThrows(ServiceException.class,()->service.forCustomer(0L,100L,10L));assertEquals("legacy mismatch",db.queryForObject("select reason from eoms_finance_event",String.class));assertEquals(1,events());}
    @Test void invalidSessionAndOversizedReasonAreRejectedWithoutAnyAudit(){assertThrows(ServiceException.class,()->service.confirm(null,100L,10L,"no tenant"));assertThrows(ServiceException.class,()->service.confirm(0L,null,10L,"no customer"));assertThrows(ServiceException.class,()->service.confirm(0L,100L,10L,"x".repeat(501)));assertEquals(0,events());}
    @Test void staffReadRequiresVisibleOrderAndOwnTenant(){assertThrows(ServiceException.class,()->service.forStaff(10L));var visible=new RepairOrder();visible.setId(10L);visible.setTenantId(0L);visible.setCustomerId(100L);when(orderService.selectRepairOrderById(10L)).thenReturn(visible);service.confirm(0L,100L,10L,"visible acknowledgement");assertTrue(service.forStaff(10L).isConfirmed());visible.setTenantId(7L);assertThrows(ServiceException.class,()->service.forStaff(10L));assertEquals(1,events());}
    MockHttpServletRequest request(Long tenant,Long customer){var req=new MockHttpServletRequest();var info=new H5CustomerInfo();info.setTenantId(tenant);info.setCustomerId(customer);req.setAttribute(H5TokenInterceptor.ATTR_CUSTOMER_INFO,info);req.setAttribute(H5TokenInterceptor.ATTR_TENANT_ID,tenant);req.setAttribute(H5TokenInterceptor.ATTR_CUSTOMER_ID,customer);return req;}
    @Test void realH5ControllerUsesSessionIdentityAndCannotHonorForgedBodyCustomerOrTenant(){var controller=new H5RepairController();ReflectionTestUtils.setField(controller,"customerConfirmationService",service);var body=new HashMap<String,Object>();body.put("orderId","10");body.put("remark","controller acknowledgement");body.put("customerId","200");body.put("tenantId","7");var result=controller.confirm(request(0L,100L),body);assertTrue(result.getData().isConfirmed());assertEquals("100",result.getData().getCustomerId());assertEquals(1,events());assertThrows(ServiceException.class,()->controller.confirm(request(7L,100L),body));var invalid=request(0L,100L);invalid.removeAttribute(H5TokenInterceptor.ATTR_TENANT_ID);assertThrows(ServiceException.class,()->controller.confirm(invalid,body));assertEquals(1,events());}
    @Test void h5ApplicationTenantComesFromTheSameAuthenticatedSessionRatherThanBodyOrPlatformDefault(){
        var controller=new H5RepairController();var applies=mock(com.ls.aegis.biz.eoms.service.repair.IRepairApplyService.class);ReflectionTestUtils.setField(controller,"repairApplyService",applies);
        var attachmentMapper=mock(com.ls.aegis.biz.eoms.mapper.file.H5AttachmentMapper.class);when(attachmentMapper.selectActiveCustomerDept(7L,100L)).thenReturn(20L);
        var uploadedFiles=mock(com.ls.aegis.biz.file.service.FileService.class);var photo=new com.ls.aegis.biz.file.model.entity.FileDO();photo.setId(900L);photo.setDeleted(0L);photo.setMetadata("{\"itlsEomsH5UploadOwner\":{\"tenantId\":\"7\",\"customerId\":\"100\"}}");when(uploadedFiles.getById(900L)).thenReturn(photo);
        ReflectionTestUtils.setField(controller,"h5AttachmentService",new com.ls.aegis.biz.eoms.service.h5.H5AttachmentService(uploadedFiles,attachmentMapper,mock(com.ls.aegis.biz.eoms.mapper.file.EomsFileAccessMapper.class)));
        var apply=new RepairApply();apply.setCustomerId(200L);apply.setTenantId(0L);apply.setContactName("synthetic");apply.setContactPhone("15500000000");apply.setFaultType("TEST");apply.setFaultDesc("synthetic request");apply.setPhotos("900");when(applies.insertRepairApply(any())).thenReturn(1);
        controller.apply(request(7L,100L),apply);var saved=org.mockito.ArgumentCaptor.forClass(RepairApply.class);verify(applies).insertRepairApply(saved.capture());assertEquals(7L,saved.getValue().getTenantId());assertEquals(100L,saved.getValue().getCustomerId());
    }

}
