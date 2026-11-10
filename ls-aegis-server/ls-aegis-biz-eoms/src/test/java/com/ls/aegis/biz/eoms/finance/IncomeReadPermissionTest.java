package com.ls.aegis.biz.eoms.finance;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.domain.Income;
import com.ls.aegis.biz.eoms.mapper.finance.IncomeMapper;
import com.ls.aegis.biz.eoms.service.impl.finance.IncomeServiceImpl;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DataScopeEnum;
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
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real shared DataScope helper and repository MyBatis SQL over a private in-memory DB. */
public class IncomeReadPermissionTest {
    JdbcTemplate db; IncomeServiceImpl service; UserContext user; MockedStatic<StpUtil> stp; GenericApplicationContext spring;
    top.continew.starter.extension.tenant.autoconfigure.TenantProperties tenantConfiguration;
    @BeforeEach void setup() throws Exception {
        spring=new GenericApplicationContext();var tenant=mock(com.ls.aegis.common.config.TenantExtensionProperties.class);when(tenant.isDefaultTenant()).thenReturn(true);when(tenant.getDefaultTenantId()).thenReturn(0L);spring.registerBean(com.ls.aegis.common.config.TenantExtensionProperties.class,()->tenant);
        tenantConfiguration=new top.continew.starter.extension.tenant.autoconfigure.TenantProperties();tenantConfiguration.setEnabled(true);spring.registerBean(top.continew.starter.extension.tenant.autoconfigure.TenantProperties.class,()->tenantConfiguration);spring.refresh();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(spring);
        user=new UserContext();user.setId(21L);user.setTenantId(7L);user.setDeptId(20L);user.setRoles(Set.of(new RoleContext(1L,"fixture",DataScopeEnum.DEPT)));UserContextHolder.setContext(user,false);
        stp=mockStatic(StpUtil.class);stp.when(StpUtil::isLogin).thenReturn(true);
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");db=new JdbcTemplate(ds);
        db.execute("CREATE ALIAS FIND_IN_SET FOR '"+getClass().getName()+".findInSet'");
        db.execute("create table psi_unit(id bigint primary key,tenant_id bigint,unit_code varchar(20),unit_name varchar(60),deleted int)");
        db.execute("create table eoms_repair_order(id bigint primary key,customer_id bigint,tenant_id bigint,dept_id bigint,create_user bigint,engineer_id bigint,order_no varchar(30),del_flag char(1))");
        db.execute("create table eoms_income(id bigint primary key,repair_order_id bigint,customer_id bigint,income_no varchar(30),amount decimal(12,2),income_type varchar(20),income_date date,payment_id bigint,reversal_of_id bigint,create_by varchar(60),create_time timestamp,update_by varchar(60),update_time timestamp,remark varchar(100),del_flag char(1))");
        db.execute("create table sys_dept(id bigint,ancestors varchar(100))");db.update("insert into sys_dept values(40,'0,20')");
        db.execute("create table sys_role_dept(role_id bigint,dept_id bigint)");db.update("insert into sys_role_dept values(99,30)");
        db.update("insert into psi_unit values(10,7,'OWN','fixture',0),(11,8,'OTHER','fixture',0),(12,7,'DELETED','fixture',1)");
        db.update("insert into eoms_repair_order values(1,10,7,20,21,31,'OWN','0'),(2,10,7,30,22,32,'FOREIGN-DEPT','0'),(3,11,8,20,21,31,'OTHER-TENANT','0'),(4,10,7,40,24,34,'CHILD','0'),(5,10,7,20,21,31,'DELETED','2')");
        db.update("insert into eoms_income(id,repair_order_id,customer_id,income_no,amount,del_flag) values(100,1,10,'OWN',10,'0'),(101,1,10,'REVERSAL',-10,'0'),(200,2,10,'FOREIGN-DEPT',20,'0'),(300,3,11,'OTHER-TENANT',30,'0'),(400,4,10,'CHILD',40,'0'),(500,5,10,'DELETED-PARENT',50,'0'),(600,999,10,'ORPHAN',60,'0'),(700,1,11,'WRONG-CUSTOMER',70,'0'),(800,null,null,'UNATTRIBUTED-LEGACY',80,'0')");
        var cfg=new MybatisConfiguration();cfg.getTypeAliasRegistry().registerAlias("Income",Income.class);cfg.setEnvironment(new Environment("income",new SpringManagedTransactionFactory(),ds));
        for(String name:List.of("EomsDataScopeMapper.xml","IncomeMapper.xml"))try(var in=Files.newInputStream(Path.of("src/main/resources/mapper/eoms/"+name))){new XMLMapperBuilder(in,cfg,name,cfg.getSqlFragments()).parse();}
        var mapper=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(cfg)).getMapper(IncomeMapper.class);service=new IncomeServiceImpl();ReflectionTestUtils.setField(service,"incomeMapper",mapper);
    }
    @AfterEach void cleanup(){if(stp!=null)stp.close();UserContextHolder.clearContext();if(spring!=null)spring.close();}
    public static int findInSet(String id,String list){return list==null?0:Arrays.asList(list.split(",")).indexOf(id)+1;}
    Set<Long> visible(){return new HashSet<>(service.selectIncomeList(new Income()).stream().map(Income::getId).toList());}
    @Test void departmentReadIncludesOriginalAndReversalButRejectsAnotherDepartmentAndTenant(){assertEquals(Set.of(100L,101L),visible());assertNull(service.selectIncomeById(200L));assertNull(service.selectIncomeById(300L));assertEquals(100L,service.selectIncomeById(100L).getId());}
    @Test void selfScopeAndAssignedEngineerFollowTheSameParentOrderVisibility(){user.setRoles(Set.of(new RoleContext(1L,"self",DataScopeEnum.SELF)));assertEquals(Set.of(100L,101L),visible());user.setId(32L);assertEquals(Set.of(200L),visible());user.setId(99L);assertTrue(visible().isEmpty());}
    @Test void departmentChildAndCustomMultiRoleOrSemanticsRemainIntact(){user.setRoles(Set.of(new RoleContext(1L,"child",DataScopeEnum.DEPT_AND_CHILD)));assertEquals(Set.of(100L,101L,400L),visible());user.setRoles(Set.of(new RoleContext(99L,"custom",DataScopeEnum.CUSTOM)));assertEquals(Set.of(200L),visible());user.setRoles(Set.of(new RoleContext(99L,"custom",DataScopeEnum.CUSTOM),new RoleContext(1L,"dept",DataScopeEnum.DEPT)));assertEquals(Set.of(100L,101L,200L),visible());}
    @Test void allScopeIsStillTenantBoundAndOrphanMismatchDeletedRowsAreNotGuessedOrChanged(){user.setRoles(Set.of(new RoleContext(1L,"all",DataScopeEnum.ALL)));assertEquals(Set.of(100L,101L,200L,400L),visible());assertNull(service.selectIncomeById(600L));assertNull(service.selectIncomeById(700L));assertNull(service.selectIncomeById(800L));assertEquals(9,db.queryForObject("select count(*) from eoms_income",Integer.class));}
    @Test void forgedClientScopeCannotOverrideDepartmentAliasOrTrustedTenantEvenForAllScope(){var q=new Income();q.getParams().put("dataScopeFilter",false);q.getParams().put("dsAlias","o) OR 1=1 --");q.getParams().put("dsDeptScope","ALL");q.getParams().put("incomeTenantId",8L);assertEquals(Set.of(100L,101L),new HashSet<>(service.selectIncomeList(q).stream().map(Income::getId).toList()));assertEquals("o",q.getParams().get("dsAlias"));assertEquals(7L,q.getParams().get("incomeTenantId"));user.setRoles(Set.of(new RoleContext(1L,"all",DataScopeEnum.ALL)));q.getParams().put("dsAlias","injected");service.selectIncomeList(q);assertFalse(q.getParams().containsKey("dsAlias"));}
    @Test void exactIdFilterCannotReturnSomeOtherVisibleIncome(){assertNull(service.selectIncomeById(9999L));var q=new Income();q.setId(200L);assertTrue(service.selectIncomeList(q).isEmpty());}
    @Test void missingTenantOrAuthenticationFailsClosed(){user.setTenantId(null);assertThrows(RuntimeException.class,()->service.selectIncomeList(new Income()));user.setTenantId(7L);stp.when(StpUtil::checkLogin).thenThrow(new IllegalStateException("controlled not logged in"));assertThrows(RuntimeException.class,()->service.selectIncomeById(100L));}
    @Test void actualDisabledTenantConfigurationAcceptsRealLoginContextWithoutTenantAndUsesOnlyTrustedDefault(){
        tenantConfiguration.setEnabled(false);user.setTenantId(null);
        db.update("insert into psi_unit values(13,0,'DEFAULT','fixture',0)");db.update("insert into eoms_repair_order values(9,13,0,20,21,31,'DEFAULT','0')");db.update("insert into eoms_income(id,repair_order_id,customer_id,income_no,amount,del_flag) values(900,9,13,'DEFAULT',90,'0')");
        assertFalse(top.continew.starter.extension.tenant.context.TenantContextHolder.isTenantEnabled());assertEquals(Set.of(900L),visible());assertEquals(900L,service.selectIncomeById(900L).getId());assertNull(service.selectIncomeById(100L));
        user.setTenantId(8L);assertEquals(Set.of(900L),visible());
        tenantConfiguration.setEnabled(true);user.setTenantId(null);assertThrows(RuntimeException.class,()->service.selectIncomeList(new Income()));
    }
    @Test void exportControllerReceivesTheSameScopedRowsAsPublicList(){
        var controller=new com.ls.aegis.biz.eoms.controller.finance.IncomeController();ReflectionTestUtils.setField(controller,"incomeService",service);
        try(var page=mockStatic(com.ls.aegis.starter.support.page.PageQueryHelper.class);var excel=mockStatic(com.ls.aegis.starter.support.excel.ExcelExportHelper.class)){
            var response=new org.springframework.mock.web.MockHttpServletResponse();controller.export(response,new Income());
            excel.verify(()->com.ls.aegis.starter.support.excel.ExcelExportHelper.export(org.mockito.ArgumentMatchers.argThat((List<Income> values)->new HashSet<>(values.stream().map(Income::getId).toList()).equals(Set.of(100L,101L))),org.mockito.ArgumentMatchers.eq(com.ls.aegis.biz.eoms.model.resp.IncomeExportResp.class),org.mockito.ArgumentMatchers.eq("利润统计管理数据"),org.mockito.ArgumentMatchers.same(response)));
        }
    }
}
