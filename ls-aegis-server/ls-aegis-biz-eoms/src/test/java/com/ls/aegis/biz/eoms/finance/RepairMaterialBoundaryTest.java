package com.ls.aegis.biz.eoms.finance;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.repair.*;
import com.ls.aegis.biz.eoms.service.impl.repair.RepairMaterialServiceImpl;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DataScopeEnum;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real repository mapper SQL and transaction/row state assertions on a private in-memory database. */
public class RepairMaterialBoundaryTest {
    MybatisConfiguration configuration;
    JdbcTemplate db; TransactionTemplate tx; RepairMaterialServiceImpl service;
    RepairMaterialMapper materials; RepairOrderMapper orders; UserContext user;
    IRepairOrderService orderVisibility; MockedStatic<StpUtil> stp; GenericApplicationContext spring;
    top.continew.starter.extension.tenant.autoconfigure.TenantProperties tenantConfig;
    @BeforeEach void setup() throws Exception {
        spring=new GenericApplicationContext();spring.registerBean(com.ls.aegis.common.config.TenantExtensionProperties.class,com.ls.aegis.common.config.TenantExtensionProperties::new);
        tenantConfig=new top.continew.starter.extension.tenant.autoconfigure.TenantProperties();tenantConfig.setEnabled(true);spring.registerBean(top.continew.starter.extension.tenant.autoconfigure.TenantProperties.class,()->tenantConfig);spring.refresh();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(spring);
        user=new UserContext();user.setId(21L);user.setUsername("fixture");user.setTenantId(7L);user.setDeptId(20L);user.setRoles(Set.of(new RoleContext(1L,"fixture",DataScopeEnum.DEPT)));UserContextHolder.setContext(user,false);
        stp=mockStatic(StpUtil.class);stp.when(StpUtil::isLogin).thenReturn(true);
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("CREATE ALIAS FIND_IN_SET FOR '"+getClass().getName()+".findInSet'");
        db.execute("create table eoms_repair_order(id bigint primary key,tenant_id bigint,dept_id bigint,create_user bigint,engineer_id bigint,customer_id bigint,order_no varchar(30),status varchar(32),del_flag char(1))");
        db.execute("create table psi_warehouse(id bigint primary key,tenant_id bigint,status int,deleted bigint)");
        db.update("insert into psi_warehouse values(71,7,1,0),(81,8,1,0),(91,7,2,0),(92,7,1,92)");
        db.execute("create table psi_goods(id bigint primary key,tenant_id bigint,goods_code varchar(30),spec varchar(30),status int,deleted bigint)");
        db.execute("create table eoms_repair_material(id bigint auto_increment primary key,repair_order_id bigint,material_id bigint,warehouse_id bigint,material_name varchar(100),quantity decimal(10,2),sale_price decimal(12,2),cost_price decimal(12,2),sale_amount decimal(12,2),cost_amount decimal(12,2),create_by varchar(64),create_time timestamp,update_by varchar(64),update_time timestamp,remark varchar(500),del_flag char(1))");
        db.execute("create table sys_dept(id bigint,ancestors varchar(100))");db.update("insert into sys_dept values(40,'0,20')");db.execute("create table sys_role_dept(role_id bigint,dept_id bigint)");db.update("insert into sys_role_dept values(99,30)");
        db.update("insert into eoms_repair_order values(1,7,20,21,31,10,'OWN','PROCESSING','0'),(2,7,30,22,32,10,'OTHER-DEPT','PROCESSING','0'),(3,8,20,21,31,11,'OTHER-TENANT','PROCESSING','0'),(4,7,40,24,34,10,'CHILD','PROCESSING','0'),(5,7,20,21,31,10,'DELETED','PROCESSING','2'),(6,0,20,21,31,10,'DEFAULT','PROCESSING','0')");
        db.update("insert into psi_goods values(70,7,'G7','spec',1,0),(80,8,'G8','spec',1,0)");
        db.update("insert into eoms_repair_material(id,repair_order_id,material_id,warehouse_id,material_name,quantity,sale_price,cost_price,sale_amount,cost_amount,del_flag) values(100,1,70,71,'own',2,5,3,10,6,'0'),(200,2,70,71,'other',2,5,3,10,6,'0'),(300,3,80,81,'tenant',2,5,3,10,6,'0'),(400,4,70,71,'child',2,5,3,10,6,'0'),(500,5,70,71,'deleted parent',2,5,3,10,6,'0'),(600,6,70,71,'default',2,5,3,10,6,'0'),(700,999,70,71,'orphan',2,5,3,10,6,'0')");
        var cfg=new MybatisConfiguration();configuration=cfg;cfg.getTypeAliasRegistry().registerAlias("RepairMaterial",RepairMaterial.class);cfg.getTypeAliasRegistry().registerAlias("RepairOrder",RepairOrder.class);cfg.setEnvironment(new Environment("materials",new SpringManagedTransactionFactory(),ds));
        for(String name:List.of("EomsDataScopeMapper.xml","RepairMaterialMapper.xml","RepairOrderMapper.xml")){
            String xml=Files.readString(Path.of("src/main/resources/mapper/eoms/"+name)).replace("sysdate()","CURRENT_TIMESTAMP");try(var in=new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))){new XMLMapperBuilder(in,cfg,name,cfg.getSqlFragments()).parse();}
        }
        var sessions=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(cfg));materials=sessions.getMapper(RepairMaterialMapper.class);orders=sessions.getMapper(RepairOrderMapper.class);
        orderVisibility=mock(IRepairOrderService.class);when(orderVisibility.selectRepairOrderById(1L)).thenAnswer(i->orders.lockOrder(1L));when(orderVisibility.selectRepairOrderById(6L)).thenAnswer(i->orders.lockOrder(6L));
        service=new RepairMaterialServiceImpl();ReflectionTestUtils.setField(service,"repairMaterialMapper",materials);ReflectionTestUtils.setField(service,"repairOrderMapper",orders);ReflectionTestUtils.setField(service,"repairOrderService",orderVisibility);
    }
    @AfterEach void clear(){if(stp!=null)stp.close();UserContextHolder.clearContext();top.continew.starter.extension.tenant.context.TenantContextHolder.clear();if(spring!=null)spring.close();}
    public static int findInSet(String id,String csv){return csv==null?0:Arrays.asList(csv.split(",")).indexOf(id)+1;}
    RepairMaterial line(){var m=new RepairMaterial();m.setRepairOrderId(1L);m.setMaterialId(70L);m.setWarehouseId(71L);m.setMaterialName("fixture");m.setQuantity(new BigDecimal("2"));m.setSalePrice(new BigDecimal("5"));m.setCostPrice(new BigDecimal("3"));return m;}
    Set<Long> visible(){return new HashSet<>(service.selectRepairMaterialList(new RepairMaterial()).stream().map(RepairMaterial::getId).toList());}
    @Test void publicReadsRequireParentDepartmentAndTenantAndExcludeDeletedOrOrphanParents(){assertEquals(Set.of(100L),visible());assertNotNull(service.selectRepairMaterialById(100L));assertNull(service.selectRepairMaterialById(200L));assertNull(service.selectRepairMaterialById(300L));assertNull(service.selectRepairMaterialById(700L));}
    @Test void selfEngineerCustomChildAndMultiRoleOrScopesRemainMeaningful(){user.setRoles(Set.of(new RoleContext(1L,"self",DataScopeEnum.SELF)));user.setId(32L);assertEquals(Set.of(200L),visible());user.setId(21L);user.setRoles(Set.of(new RoleContext(99L,"custom",DataScopeEnum.CUSTOM)));assertEquals(Set.of(200L),visible());user.setRoles(Set.of(new RoleContext(1L,"child",DataScopeEnum.DEPT_AND_CHILD)));assertEquals(Set.of(100L,400L),visible());user.setRoles(Set.of(new RoleContext(1L,"dept",DataScopeEnum.DEPT),new RoleContext(99L,"custom",DataScopeEnum.CUSTOM)));assertEquals(Set.of(100L,200L),visible());}
    @Test void allScopeStillCannotCrossTenantAndForgedRequestScopeCannotExpandDepartment(){var q=new RepairMaterial();q.getParams().put("dataScopeFilter",false);q.getParams().put("dsSelf",true);q.getParams().put("dsIncludeEngineer",true);q.getParams().put("dsUserId",22L);q.getParams().put("dsAlias","o) OR 1=1 --");q.getParams().put("materialTenantId",8L);assertEquals(Set.of(100L),new HashSet<>(service.selectRepairMaterialList(q).stream().map(RepairMaterial::getId).toList()));user.setRoles(Set.of(new RoleContext(1L,"all",DataScopeEnum.ALL)));assertEquals(Set.of(100L,200L,400L),visible());}
    @Test void disabledTenantNullContextUsesTrustedDefaultButEnabledMissingFailsClosed(){tenantConfig.setEnabled(false);user.setTenantId(null);assertEquals(Set.of(600L),visible());user.setTenantId(7L);assertEquals(Set.of(600L),visible());tenantConfig.setEnabled(true);user.setTenantId(null);assertThrows(RuntimeException.class,()->visible());}
    @Test void insertRebuildsIdentityDeleteAndAuditFieldsAndRecalculatesClientAmounts(){var m=line();m.setId(999L);m.setDelFlag("2");m.setCreateBy("spoofed");m.setUpdateBy("spoofed");m.setSaleAmount(new BigDecimal("999"));m.setCostAmount(new BigDecimal("999"));assertEquals(1,tx.execute(s->service.insertRepairMaterial(m)).intValue());assertNotEquals(999L,m.getId());assertEquals("0",db.queryForObject("select del_flag from eoms_repair_material where id=?",String.class,m.getId()));assertEquals(new BigDecimal("10.00"),m.getSaleAmount());assertEquals(new BigDecimal("6.00"),m.getCostAmount());assertEquals("fixture",db.queryForObject("select create_by from eoms_repair_material where id=?",String.class,m.getId()));}
    @Test void negativePricesAndExcessPrecisionMustNotReachStorage(){for(int kind=0;kind<5;kind++){var m=line();if(kind==0)m.setSalePrice(new BigDecimal("-0.01"));if(kind==1)m.setCostPrice(new BigDecimal("-0.01"));if(kind==2)m.setSalePrice(new BigDecimal("0.001"));if(kind==3)m.setQuantity(new BigDecimal("0.001"));if(kind==4)m.setQuantity(new BigDecimal("100000000"));assertThrows(ServiceException.class,()->tx.execute(s->service.insertRepairMaterial(m)));}assertEquals(7,db.queryForObject("select count(*) from eoms_repair_material",Integer.class));}
    @Test void nullInputsAndInvalidForeignKeysAndZeroQuantityAreRejected(){assertThrows(ServiceException.class,()->tx.execute(s->service.insertRepairMaterial(null)));for(int kind=0;kind<4;kind++){var m=line();if(kind==0)m.setMaterialId(null);if(kind==1)m.setWarehouseId(null);if(kind==2)m.setQuantity(BigDecimal.ZERO);if(kind==3)m.setMaterialName(" ");assertThrows(ServiceException.class,()->tx.execute(s->service.insertRepairMaterial(m)));}}
    @Test void roundingIsPreservedButCalculatedAmountOverflowIsRejected(){var m=line();m.setQuantity(new BigDecimal("0.25"));m.setSalePrice(new BigDecimal("0.02"));m.setCostPrice(null);assertEquals(1,tx.execute(s->service.insertRepairMaterial(m)).intValue());assertEquals(new BigDecimal("0.01"),m.getSaleAmount());assertEquals(new BigDecimal("0.00"),m.getCostAmount());var huge=line();huge.setQuantity(new BigDecimal("99999999.99"));huge.setSalePrice(new BigDecimal("9999999999.99"));assertThrows(ServiceException.class,()->tx.execute(s->service.insertRepairMaterial(huge)));}
    @Test void updateCannotMigrateParentOrUseDelFlagAndCannotWriteNegativePrice(){var change=line();change.setId(100L);change.setRepairOrderId(2L);assertThrows(ServiceException.class,()->tx.execute(s->service.updateRepairMaterial(change)));assertEquals(1L,db.queryForObject("select repair_order_id from eoms_repair_material where id=100",Long.class));var valid=line();valid.setId(100L);valid.setDelFlag("2");assertEquals(1,tx.execute(s->service.updateRepairMaterial(valid)).intValue());assertEquals("0",db.queryForObject("select del_flag from eoms_repair_material where id=100",String.class));valid.setCostPrice(new BigDecimal("-1"));assertThrows(ServiceException.class,()->tx.execute(s->service.updateRepairMaterial(valid)));}
    @Test void sealedOrderAndInvisibleOrderRejectAllWrites(){db.update("update eoms_repair_order set status='FINISHED' where id=1");assertThrows(ServiceException.class,()->tx.execute(s->service.insertRepairMaterial(line())));var m=line();m.setId(100L);assertThrows(ServiceException.class,()->tx.execute(s->service.updateRepairMaterial(m)));assertThrows(ServiceException.class,()->tx.execute(s->service.deleteRepairMaterialByIds(new Long[]{100L})));db.update("update eoms_repair_order set status='PROCESSING' where id=1");when(orderVisibility.selectRepairOrderById(1L)).thenReturn(null);assertThrows(ServiceException.class,()->tx.execute(s->service.insertRepairMaterial(line())));}
    @Test void mixedBatchRejectsBeforeDeletingVisibleMaterialAndDuplicateIdsAreIdempotent(){assertThrows(ServiceException.class,()->tx.execute(s->service.deleteRepairMaterialByIds(new Long[]{100L,200L})));assertEquals("0",db.queryForObject("select del_flag from eoms_repair_material where id=100",String.class));assertThrows(ServiceException.class,()->tx.execute(s->service.deleteRepairMaterialByIds(new Long[]{100L,null})));assertEquals(1,tx.execute(s->service.deleteRepairMaterialByIds(new Long[]{100L,100L})).intValue());}
    @Test void internalSettlementCompleteLockedSetDoesNotDependOnCallingOperatorsPublicDepartmentScope(){assertEquals(1,tx.execute(s->materials.lockByOrder(2L)).size());assertNull(service.selectRepairMaterialById(200L));}

    @Test void realTenantLineInterceptorUsesExplicitParentScopeWithoutInventingMaterialTenantColumn() {
        var interceptor = new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
            public net.sf.jsqlparser.expression.Expression getTenantId() { return new net.sf.jsqlparser.expression.LongValue(7); }
            public boolean ignoreTable(String name) { return name.equalsIgnoreCase("sys_dept") || name.equalsIgnoreCase("sys_role_dept"); }
        }));
        configuration.addInterceptor(interceptor);
        assertEquals(Set.of(100L), visible());
        assertNull(service.selectRepairMaterialById(300L));
        var line = line();
        assertEquals(1, tx.execute(s -> service.insertRepairMaterial(line)).intValue());
        line.setQuantity(new BigDecimal("3"));
        assertEquals(1, tx.execute(s -> service.updateRepairMaterial(line)).intValue());
        assertEquals(1, tx.execute(s -> service.deleteRepairMaterialById(line.getId())).intValue());
    }

    @Test void materialUpdateWaitsForParentAndRechecksCommittedFinishedState() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        var snapshot = orders.lockOrder(1L);
        when(orderVisibility.selectRepairOrderById(1L)).thenAnswer(i -> { entered.countDown(); return snapshot; });
        var update = line(); update.setId(100L); update.setQuantity(new BigDecimal("4"));
        java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<Throwable>> future = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            tx.execute(s -> {
                orders.lockOrder(1L);
                future.set(executor.submit(() -> inWorker(() -> tx.execute(t -> service.updateRepairMaterial(update)))));
                try { assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new AssertionError(e); }
                db.update("update eoms_repair_order set status='FINISHED' where id=1");
                return null;
            });
            assertInstanceOf(ServiceException.class, future.get().get(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(new BigDecimal("2.00"), db.queryForObject("select quantity from eoms_repair_material where id=100", BigDecimal.class));
        } finally { executor.shutdownNow(); }
    }

    @Test void materialUpdateRechecksCurrentChildAfterConcurrentAuthorizedDeletion() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        var snapshot = orders.lockOrder(1L);
        when(orderVisibility.selectRepairOrderById(1L)).thenAnswer(i -> { entered.countDown(); return snapshot; });
        var update = line(); update.setId(100L);
        java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<Throwable>> future = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            tx.execute(s -> {
                orders.lockOrder(1L);
                future.set(executor.submit(() -> inWorker(() -> tx.execute(t -> service.updateRepairMaterial(update)))));
                try { assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new AssertionError(e); }
                materials.deleteRepairMaterialByIds(new Long[]{100L});
                return null;
            });
            assertInstanceOf(ServiceException.class, future.get().get(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals("2", db.queryForObject("select del_flag from eoms_repair_material where id=100", String.class));
        } finally { executor.shutdownNow(); }
    }

    private Throwable inWorker(Runnable action) {
        UserContextHolder.setContext(user, false);
        try (var ignored = mockStatic(StpUtil.class)) {
            ignored.when(StpUtil::isLogin).thenReturn(true);
            try { action.run(); return null; } catch (Throwable failure) { return failure; }
        } finally { UserContextHolder.clearContext(); }
    }

    @Test void referencesCannotCrossTenantOrUseMissingDisabledDeletedMasterData() {
        for (long id : new long[]{80L,999L}) {
            var m = line(); m.setMaterialId(id);
            assertThrows(ServiceException.class, () -> tx.execute(s -> service.insertRepairMaterial(m)));
        }
        for (long id : new long[]{81L,91L,92L,999L}) {
            var m = line(); m.setWarehouseId(id);
            assertThrows(ServiceException.class, () -> tx.execute(s -> service.insertRepairMaterial(m)));
        }
        db.update("update psi_goods set status=2 where id=70");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.insertRepairMaterial(line())));
        db.update("update psi_goods set status=1,deleted=70 where id=70");
        assertThrows(ServiceException.class, () -> tx.execute(s -> service.insertRepairMaterial(line())));
        assertEquals(7, db.queryForObject("select count(*) from eoms_repair_material", Integer.class));
    }

    @Test void historicalMaterialsRemainReadableAfterReferencedMastersAreDisabledOrDeleted() {
        db.update("update psi_goods set status=2 where id=70");
        db.update("update psi_warehouse set status=2 where id=71");
        assertNotNull(service.selectRepairMaterialById(100L));
        db.update("update psi_goods set deleted=70 where id=70");
        assertEquals(new BigDecimal("6.00"), service.selectRepairMaterialById(100L).getCostAmount());
        assertEquals(Set.of(100L), visible());
    }
}
