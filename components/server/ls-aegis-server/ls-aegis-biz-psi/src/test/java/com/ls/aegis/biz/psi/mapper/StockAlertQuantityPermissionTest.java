package com.ls.aegis.biz.psi.mapper;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.resp.*;
import com.ls.aegis.biz.psi.service.impl.PsiWorkbenchServiceImpl;
import com.ls.aegis.common.config.TenantExtensionProperties;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DataScopeEnum;
import com.ls.aegis.mybatis.config.DefaultDataPermissionUserDataProvider;
import net.sf.jsqlparser.expression.*;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import top.continew.starter.extension.datapermission.handler.DefaultDataPermissionHandler;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual platform annotation discovery/user provider, SQL parser, MapperProxy and H2 execution. */
class StockAlertQuantityPermissionTest {
    JdbcTemplate db;GoodsMapper goods;StockBatchMapper batches;UnsafeProbe unsafe;PsiWorkbenchServiceImpl service;
    GenericApplicationContext context;TransactionTemplate tx;SqlCapture captured=new SqlCapture();
    public interface UnsafeProbe {
        // Intentionally lacks @DataPermission: a control that would leak invisible warehouse stock.
        @Select("SELECT SUM(b.remain_quantity) FROM psi_stock_batch b JOIN psi_warehouse w ON w.id=b.warehouse_id AND w.tenant_id=b.tenant_id AND w.deleted=0 WHERE b.goods_id=10 AND b.deleted=0 AND b.remain_quantity>0")
        BigDecimal unscopedQuantity();
    }
    @Intercepts(@Signature(type=StatementHandler.class,method="prepare",args={Connection.class,Integer.class}))
    static class SqlCapture implements Interceptor {
        final List<String> sql=new ArrayList<>();
        public Object intercept(Invocation call)throws Throwable{sql.add(((StatementHandler)call.getTarget()).getBoundSql().getSql());return call.proceed();}
    }
    @BeforeEach void setup()throws Exception{
        context=new GenericApplicationContext();context.registerBean(TenantExtensionProperties.class);context.refresh();new SpringUtil().setApplicationContext(context);
        var request=new org.springframework.mock.web.MockHttpServletRequest();var response=new org.springframework.mock.web.MockHttpServletResponse();
        cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.setModelBox(new cn.dev33.satoken.servlet.model.SaRequestForServlet(request),new cn.dev33.satoken.servlet.model.SaResponseForServlet(response),new cn.dev33.satoken.servlet.model.SaStorageForServlet(request));
        cn.dev33.satoken.stp.StpUtil.login(100L);
        actor(1,100,10,DataScopeEnum.DEPT);
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("CREATE TABLE psi_goods(id BIGINT PRIMARY KEY,tenant_id BIGINT,dept_id BIGINT,create_user BIGINT,goods_code VARCHAR(40),goods_name VARCHAR(80),stock_lower DECIMAL(18,4),stock_upper DECIMAL(18,4),deleted BIGINT DEFAULT 0)");
        db.execute("CREATE TABLE psi_warehouse(id BIGINT PRIMARY KEY,tenant_id BIGINT,dept_id BIGINT,create_user BIGINT,status INT,deleted BIGINT DEFAULT 0)");
        // Real schema has NO dept_id on batches: permission must use the warehouse alias.
        db.execute("CREATE TABLE psi_stock_batch(id BIGINT PRIMARY KEY,tenant_id BIGINT,warehouse_id BIGINT,goods_id BIGINT,create_user BIGINT,remain_quantity DECIMAL(18,4),deleted BIGINT DEFAULT 0)");
        db.execute("CREATE TABLE sys_role_dept(role_id BIGINT,dept_id BIGINT)");
        db.update("INSERT INTO sys_role_dept VALUES(9,20),(99,10)");
        db.update("INSERT INTO psi_warehouse VALUES(1,1,10,100,1,0),(2,1,20,200,1,0),(3,2,10,100,1,0),(4,1,10,100,1,4),(5,1,30,100,1,0),(6,1,10,999,2,0)");
        db.update("INSERT INTO psi_goods VALUES(10,1,10,100,'A','watched',10,50,0),(20,1,20,200,'B','hidden',3,5,0),(30,2,10,100,'C','tenant2',1,NULL,0),(40,1,10,100,'D','deleted',1,NULL,40),(50,1,10,100,'E','empty',1,NULL,0),(60,1,10,100,'F','equal',2,5,0),(70,1,10,100,'G','fraction',NULL,0,0),(80,1,10,100,'H','zero',0,0,0),(90,1,10,100,'I','negative-only',3,NULL,0)");
        db.update("INSERT INTO psi_stock_batch VALUES(1,1,1,10,999,1.0001,0),(2,1,1,10,999,2.0002,0),(3,1,2,10,100,100,0),(4,2,3,10,100,1000,0),(5,1,4,10,100,1000,0),(6,1,5,10,999,0.0004,0),(7,1,6,10,100,0.5,0),(8,1,1,10,100,900,8),(9,1,1,10,100,0,0),(10,1,1,10,100,-99,0),(11,1,999,10,100,10000,0),(12,1,3,10,100,10000,0),(13,2,1,10,100,30000,0),(14,1,1,20,100,500,0),(15,1,2,20,100,2,0),(16,2,3,30,100,0.1234,0),(17,1,1,60,100,2,0),(18,1,1,70,100,0.0001,0),(19,1,1,80,100,0,0),(20,1,1,90,100,-4,0)");
        var configuration=new MybatisConfiguration();configuration.setMapUnderscoreToCamelCase(true);
        var global=new com.baomidou.mybatisplus.core.config.GlobalConfig();
        global.setDbConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig().setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id"));
        global.setSqlInjector(new com.baomidou.mybatisplus.core.injector.DefaultSqlInjector());
        com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils.setGlobalConfig(configuration,global);
        var plugins=new MybatisPlusInterceptor();plugins.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(UserContextHolder.getContext().getTenantId());}}));
        plugins.addInnerInterceptor(new DataPermissionInterceptor(new DefaultDataPermissionHandler(new DefaultDataPermissionUserDataProvider())));
        configuration.addInterceptor(plugins);configuration.addInterceptor(captured);configuration.addMapper(GoodsMapper.class);configuration.addMapper(StockBatchMapper.class);configuration.addMapper(UnsafeProbe.class);
        var factory=new MybatisSqlSessionFactoryBean();factory.setConfiguration(configuration);factory.setGlobalConfig(global);factory.setDataSource(ds);var session=new SqlSessionTemplate(factory.getObject());goods=session.getMapper(GoodsMapper.class);batches=session.getMapper(StockBatchMapper.class);unsafe=session.getMapper(UnsafeProbe.class);
        service=new PsiWorkbenchServiceImpl(mock(PsiTodoMapper.class),goods,batches,mock(PsiWorkbenchMapper.class));
    }
    void actor(long tenant,long user,long dept,DataScopeEnum... scopes){var actor=new UserContext();actor.setTenantId(tenant);actor.setId(user);actor.setDeptId(dept);var roles=new LinkedHashSet<RoleContext>();for(int i=0;i<scopes.length;i++)roles.add(new RoleContext(scopes[i]==DataScopeEnum.CUSTOM?9L:100L+i,"test_scope_"+i,scopes[i]));actor.setRoles(roles);UserContextHolder.setContext(actor,false);}
    @AfterEach void cleanup(){cn.dev33.satoken.stp.StpUtil.logout();cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.clearModelBox();UserContextHolder.clearContext();db.execute("SHUTDOWN");context.close();new SpringUtil().setApplicationContext(null);}
    Map<Long,BigDecimal> sums(Long tenant,Long... ids){return batches.selectAlertQuantities(tenant,Arrays.asList(ids)).stream().collect(Collectors.toMap(StockAlertQuantityResp::getGoodsId,StockAlertQuantityResp::getRemainQuantity));}
    @SuppressWarnings("unchecked") List<PsiStockAlertResp> alerts(){return ReflectionTestUtils.invokeMethod(service,"buildStockAlerts");}
    void equal(String value,BigDecimal actual){assertNotNull(actual);assertEquals(0,new BigDecimal(value).compareTo(actual));}
    @Test void departmentScopeAggregatesOnlyVisibleLiveWarehousesAndPositiveLiveBatches(){equal("3.5003",sums(1L,10L).get(10L));assertTrue(unsafe.unscopedQuantity().compareTo(new BigDecimal("100"))>0,"control lacks DP and must expose warehouse2, unlike production method");String sql=captured.sql.stream().filter(s->s.toLowerCase().contains("group by")).findFirst().orElseThrow();assertTrue(sql.contains("w.dept_id"));assertTrue(sql.contains("b.tenant_id"));assertFalse(sql.contains("b.dept_id"));}
    @Test void selfScopeUsesWarehouseOwnershipInsteadOfBatchCreator(){actor(1,100,10,DataScopeEnum.SELF);equal("3.0007",sums(1L,10L).get(10L));}
    @Test void multipleRolesAreAnOrWithoutLosingThePositiveAndDeletionPredicates(){actor(1,100,10,DataScopeEnum.DEPT,DataScopeEnum.SELF);equal("3.5007",sums(1L,10L).get(10L));}
    @Test void customRoleUsesOnlyItsAssignedDepartmentsForGoodsAndWarehouses(){actor(1,100,10,DataScopeEnum.CUSTOM);equal("100",sums(1L,10L).get(10L));var rows=alerts();assertEquals(List.of(20L),rows.stream().map(PsiStockAlertResp::getGoodsId).toList());equal("2",rows.get(0).getStockQty());}
    @Test void allScopeAndSuperAdminKeepTenantButCanSeeOtherDepartments(){actor(1,100,10,DataScopeEnum.ALL);equal("103.5007",sums(1L,10L).get(10L));var actor=UserContextHolder.getContext();actor.setRoles(Set.of(new RoleContext(1L,"super_admin",DataScopeEnum.ALL)));equal("103.5007",sums(1L,10L).get(10L));assertTrue(sums(2L,30L).isEmpty(),"TenantLine must still reject a different explicit tenant");}
    @Test void tenantSwitchCannotExposeTheOtherTenantsGoodsOrBatches(){actor(2,100,10,DataScopeEnum.DEPT);var rows=alerts();assertEquals(List.of(30L),rows.stream().map(PsiStockAlertResp::getGoodsId).toList());equal("0.1234",rows.get(0).getStockQty());}
    @Test void watchedGoodsPermissionDeletionZeroAndFractionalThresholdsRemainCorrect(){var rows=alerts();var byId=rows.stream().collect(Collectors.toMap(PsiStockAlertResp::getGoodsId,r->r));assertEquals(Set.of(10L,50L,70L,90L),byId.keySet());equal("3.5003",byId.get(10L).getStockQty());equal("0",byId.get(50L).getStockQty());equal("0.0001",byId.get(70L).getStockQty());assertEquals("HIGH",byId.get(70L).getAlertType());equal("0",byId.get(90L).getStockQty());db.update("UPDATE psi_stock_batch SET remain_quantity=1.9999 WHERE id=17");assertTrue(alerts().stream().anyMatch(r->r.getGoodsId().equals(60L)&&r.getAlertType().equals("LOW")));}
    @Test void missingAndEmptyGoodsIdsNeverFallBackToAnUnboundedAggregate(){assertTrue(batches.selectAlertQuantities(1L,List.of()).isEmpty());assertTrue(batches.selectAlertQuantities(1L,null).isEmpty());assertTrue(sums(1L,99999L).isEmpty());}
    @Test void sameSessionQueryDoesNotCacheObsoleteStock(){tx.execute(status->{equal("3.5003",sums(1L,10L).get(10L));db.update("UPDATE psi_stock_batch SET remain_quantity=3.0002 WHERE id=2");equal("4.5003",sums(1L,10L).get(10L));return null;});}
    @Test void invalidScopeFailsBeforeAnyGoodsOrStockSql(){var actor=UserContextHolder.getContext();actor.setRoles(Set.of());captured.sql.clear();assertThrows(RuntimeException.class,()->alerts());assertTrue(captured.sql.isEmpty());}
    @Test void anExpiredLoginCannotUseAnOtherwiseValidThreadLocalScope(){cn.dev33.satoken.stp.StpUtil.logout();captured.sql.clear();assertThrows(cn.dev33.satoken.exception.NotLoginException.class,()->alerts());assertTrue(captured.sql.isEmpty());}
    @Test void aggregateReturnsOneRowForThousandsOfBoundBatchesWithoutChangingExactQuantity(){db.execute("INSERT INTO psi_stock_batch(id,tenant_id,warehouse_id,goods_id,create_user,remain_quantity,deleted) SELECT 100000+X,1,1,10,100,0.0001,0 FROM SYSTEM_RANGE(1,20000)");var rows=batches.selectAlertQuantities(1L,List.of(10L));assertEquals(1,rows.size());equal("5.5003",rows.get(0).getRemainQuantity());assertEquals(20020,db.queryForObject("SELECT COUNT(*) FROM psi_stock_batch",Integer.class));}
    @Test void firstTwentyAlertsAndWatchedGoodsFiveHundredBoundRemainUnchanged(){for(long id=1000;id<1520;id++)db.update("INSERT INTO psi_goods(id,tenant_id,dept_id,create_user,stock_lower,deleted) VALUES(?,1,10,100,1,0)",id);var rows=alerts();assertEquals(20,rows.size());assertEquals(List.of(10L,50L,70L,90L,1000L),rows.stream().limit(5).map(PsiStockAlertResp::getGoodsId).toList());String watched=captured.sql.stream().filter(s->s.contains("FROM psi_goods")).findFirst().orElseThrow();assertTrue(watched.toUpperCase().contains("LIMIT 500"));}
}
