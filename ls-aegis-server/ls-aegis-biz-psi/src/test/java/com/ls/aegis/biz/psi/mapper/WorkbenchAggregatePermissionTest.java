package com.ls.aegis.biz.psi.mapper;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.common.config.TenantExtensionProperties;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.context.RoleContext;
import com.ls.aegis.common.enums.DataScopeEnum;
import com.ls.aegis.mybatis.config.DefaultDataPermissionUserDataProvider;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.continew.starter.extension.datapermission.handler.DefaultDataPermissionHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchAggregatePermissionTest {
    JdbcTemplate db;
    PsiWorkbenchMapper mapper;
    GoodsMapper goods;
    GenericApplicationContext context;
    TransactionTemplate tx;
    StockAlertQuantityPermissionTest.SqlCapture captured=new StockAlertQuantityPermissionTest.SqlCapture();
    LocalDate today=LocalDate.of(2026,11,5);
    @BeforeEach void setup() throws Exception {
        context=new GenericApplicationContext();context.registerBean(TenantExtensionProperties.class);context.refresh();new SpringUtil().setApplicationContext(context);
        var request=new org.springframework.mock.web.MockHttpServletRequest();var response=new org.springframework.mock.web.MockHttpServletResponse();
        cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.setModelBox(new cn.dev33.satoken.servlet.model.SaRequestForServlet(request),new cn.dev33.satoken.servlet.model.SaResponseForServlet(response),new cn.dev33.satoken.servlet.model.SaStorageForServlet(request));
        cn.dev33.satoken.stp.StpUtil.login(100L);
        actor(1,100,10,DataScopeEnum.DEPT);
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("CREATE TABLE sys_role_dept(role_id BIGINT,dept_id BIGINT)");
        db.update("INSERT INTO sys_role_dept VALUES(9,20)");
        for(String table:List.of("psi_receivable","psi_payable","psi_receipt","psi_bank_statement","psi_todo","psi_stock_in","psi_stock_out")) {
            db.execute("CREATE TABLE "+table+"(id BIGINT PRIMARY KEY,tenant_id BIGINT,dept_id BIGINT,create_user BIGINT,deleted BIGINT DEFAULT 0,doc_status VARCHAR(30),match_status VARCHAR(30),done INT DEFAULT 0,amount DECIMAL(22,4),unwritten_amount DECIMAL(22,4),tax_inclusive_amount DECIMAL(22,4),confirm_time DATETIME)");
        }
        for(String direction:List.of("in","out")) db.execute("CREATE TABLE psi_stock_"+direction+"_item(id BIGINT PRIMARY KEY,tenant_id BIGINT,stock_"+direction+"_id BIGINT,deleted BIGINT DEFAULT 0,quantity DECIMAL(22,4))");
        db.execute("CREATE TABLE psi_goods(id BIGINT PRIMARY KEY,tenant_id BIGINT,dept_id BIGINT,create_user BIGINT,deleted BIGINT DEFAULT 0,goods_code VARCHAR_IGNORECASE(40),goods_name VARCHAR(50),default_tax_rate DECIMAL(10,4))");
        var configuration=new MybatisConfiguration();configuration.setMapUnderscoreToCamelCase(true);
        var global=new com.baomidou.mybatisplus.core.config.GlobalConfig();
        global.setDbConfig(new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig().setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id"));
        global.setSqlInjector(new com.baomidou.mybatisplus.core.injector.DefaultSqlInjector());
        com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils.setGlobalConfig(configuration,global);
        var plugins=new MybatisPlusInterceptor();plugins.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(UserContextHolder.getContext().getTenantId());}}));
        plugins.addInnerInterceptor(new DataPermissionInterceptor(new DefaultDataPermissionHandler(new DefaultDataPermissionUserDataProvider())));
        configuration.addInterceptor(plugins);configuration.addInterceptor(captured);configuration.addMapper(PsiWorkbenchMapper.class);configuration.addMapper(GoodsMapper.class);
        var factory=new MybatisSqlSessionFactoryBean();factory.setConfiguration(configuration);factory.setGlobalConfig(global);factory.setDataSource(ds);var session=new SqlSessionTemplate(factory.getObject());mapper=session.getMapper(PsiWorkbenchMapper.class); goods=session.getMapper(GoodsMapper.class);
    }
    void actor(long tenant,long user,long dept,DataScopeEnum... scopes) {
        var actor=new UserContext();actor.setTenantId(tenant);actor.setId(user);actor.setDeptId(dept);
        var roles=new LinkedHashSet<RoleContext>();
        for(int i=0;i<scopes.length;i++)roles.add(new RoleContext(scopes[i]==DataScopeEnum.CUSTOM?9L:100L+i,"scope_"+i,scopes[i]));
        actor.setRoles(roles);UserContextHolder.setContext(actor,false);
    }
    @AfterEach void cleanup(){cn.dev33.satoken.stp.StpUtil.logout();cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.clearModelBox();UserContextHolder.clearContext();db.execute("SHUTDOWN");context.close();new SpringUtil().setApplicationContext(null);}
    void equal(String expected,BigDecimal value){assertNotNull(value);assertEquals(0,new BigDecimal(expected).compareTo(value));}
    void row(String table,long id,long tenant,long dept,long user,long deleted,String status,String amount) {
        db.update("INSERT INTO "+table+"(id,tenant_id,dept_id,create_user,deleted,doc_status,match_status,amount,unwritten_amount,tax_inclusive_amount,confirm_time) VALUES(?,?,?,?,?,?,?,?,?,?,?)",id,tenant,dept,user,deleted,status,status,amount,amount,amount,today.atStartOfDay());
    }
    void funds() {
        for(String t:List.of("psi_receivable","psi_payable","psi_receipt","psi_bank_statement","psi_todo")){
            String status=t.equals("psi_receipt")?"DRAFT":t.equals("psi_bank_statement")?"UNMATCHED":"OPEN";
            row(t,1,1,10,100,0,status,"1.0001");row(t,2,1,20,200,0,status,"100");
            row(t,3,2,10,100,0,status,"1000");row(t,4,1,10,100,4,status,"900");
            row(t,5,1,30,100,0,status,"2.0002");
        }
    }
    void assertFunds(String sum,long count){
        for(var result:List.of(mapper.selectReceivables(1L),mapper.selectPayables(1L),mapper.selectReceipts(1L),mapper.selectBanks(1L))){assertEquals(count,result.getCount());equal(sum,result.getAmount());}
        assertEquals(count,mapper.countOpenTodos(1L));
    }
    @Test void departmentSelfCustomAndOrScopesApplyToEveryAggregate(){
        funds();assertFunds("1.0001",1);
        actor(1,100,10,DataScopeEnum.SELF);assertFunds("3.0003",2);
        actor(1,100,10,DataScopeEnum.CUSTOM);assertFunds("100",1);
        actor(1,100,10,DataScopeEnum.DEPT,DataScopeEnum.SELF);assertFunds("3.0003",2);
        actor(1,100,10,DataScopeEnum.ALL);assertFunds("103.0003",3);
        actor(2,100,10,DataScopeEnum.DEPT);assertEquals(1,mapper.selectReceivables(2L).getCount());equal("1000",mapper.selectReceivables(2L).getAmount());
        assertEquals(0,mapper.selectReceivables(1L).getCount());
    }
    @Test void emptyNullNegativeStatusAndDeletedRowsKeepExactSemantics(){
        equal("0",mapper.selectReceipts(1L).getAmount());
        checkStock(0,"0","0");
        row("psi_receipt",1,1,10,100,0,"DRAFT",null);row("psi_receipt",2,1,10,100,0,"DRAFT","-5.1234");row("psi_receipt",3,1,10,100,0,"CONFIRMED","999");
        var result=mapper.selectReceipts(1L);assertEquals(2,result.getCount());equal("-5.1234",result.getAmount());
        row("psi_receivable",1,1,10,100,0,"OPEN","0");row("psi_receivable",2,1,10,100,0,"OPEN","-10");row("psi_receivable",3,1,10,100,0,"CLOSED","10");
        assertEquals(0,mapper.selectReceivables(1L).getCount());
    }
    void stock(long id,long tenant,long dept,long user,long deleted,String status,String amount,LocalDateTime date){
        for(String d:List.of("in","out")){
            row("psi_stock_"+d,id,tenant,dept,user,deleted,status,amount);
            db.update("UPDATE psi_stock_"+d+" SET confirm_time=? WHERE id=?",date,id);
            db.update("INSERT INTO psi_stock_"+d+"_item VALUES(?,?,?,0,1.0001),(?,?,?,0,2.0002)",id*10,tenant,id,id*10+1,tenant,id);
        }
    }
    void checkStock(long count,String amount,String qty){
        var month=today.withDayOfMonth(1).atStartOfDay();var span=today.minusDays(29).atStartOfDay();var start=month.isBefore(span)?month:span;var end=today.atTime(LocalTime.MAX);
        for(var h:List.of(mapper.selectInHeaders(1L,month,span,start,end),mapper.selectOutHeaders(1L,month,span,start,end))){assertEquals(count,h.getMonthCount());equal(amount,h.getMonthAmount());}
        for(var q:List.of(mapper.selectInQuantities(1L,month,span,start,end),mapper.selectOutQuantities(1L,month,span,start,end)))equal(qty,q.getMonthQty());
    }
    @Test void stockJoinsUseHeaderPermissionAndDoNotMultiplyHeaderMoney(){
        stock(1,1,10,100,0,"CONFIRMED","10.1234",today.atStartOfDay());stock(2,1,20,200,0,"CONFIRMED","100",today.atStartOfDay());stock(3,2,10,100,0,"CONFIRMED","1000",today.atStartOfDay());stock(4,1,10,100,4,"CONFIRMED","900",today.atStartOfDay());stock(5,1,10,100,0,"DRAFT","900",today.atStartOfDay());
        for(String d:List.of("in","out")) db.update("INSERT INTO psi_stock_"+d+"_item VALUES(900,1,1,900,800),(901,2,1,0,900)");
        checkStock(1,"10.1234","3.0003");
        actor(1,100,10,DataScopeEnum.CUSTOM);checkStock(1,"100","3.0003");
        actor(1,100,10,DataScopeEnum.SELF);checkStock(1,"10.1234","3.0003");
        assertTrue(captured.sql.stream().anyMatch(s->s.contains("h.create_user")));
    }
    @Test void monthThirtyDayAndEndOfTodayBoundariesAreInclusive(){
        for(LocalDate date:List.of(LocalDate.of(2026,11,5),LocalDate.of(2026,10,31),LocalDate.of(2026,3,1))){
            today=date;for(String d:List.of("in","out")){db.execute("DELETE FROM psi_stock_"+d+"_item");db.execute("DELETE FROM psi_stock_"+d);}
            var month=today.withDayOfMonth(1).atStartOfDay();var span=today.minusDays(29).atStartOfDay();var start=month.isBefore(span)?month:span;var end=today.atTime(LocalTime.MAX);
            stock(1,1,10,100,0,"CONFIRMED","1",month);stock(2,1,10,100,0,"CONFIRMED","2",span);stock(3,1,10,100,0,"CONFIRMED","4",today.atTime(23,59,59));stock(4,1,10,100,0,"CONFIRMED","800",today.plusDays(1).atStartOfDay());stock(5,1,10,100,0,"CONFIRMED","900",start.minusSeconds(1));
            var h=mapper.selectInHeaders(1L,month,span,start,end);var q=mapper.selectInQuantities(1L,month,span,start,end);
            long monthCount=span.isBefore(month)?2:3;long spanCount=month.isBefore(span)?2:3;
            assertEquals(monthCount,h.getMonthCount());assertEquals(spanCount,h.getSpanCount());equal(monthCount==2?"5":"7",h.getMonthAmount());equal(spanCount==2?"6":"7",h.getSpanAmount());equal(new BigDecimal("3.0003").multiply(BigDecimal.valueOf(monthCount)).toPlainString(),q.getMonthQty());
        }
    }
    @Test void twentyThousandRowsReturnOneProjectionAndSameSessionDoesNotCacheOldTotals(){
        db.execute("INSERT INTO psi_receivable(id,tenant_id,dept_id,create_user,doc_status,unwritten_amount) SELECT X,1,10,100,'OPEN',0.0001 FROM SYSTEM_RANGE(1,20000)");
        tx.execute(status->{var r=mapper.selectReceivables(1L);assertEquals(20000,r.getCount());equal("2",r.getAmount());db.update("UPDATE psi_receivable SET unwritten_amount=1 WHERE id=1");equal("2.9999",mapper.selectReceivables(1L).getAmount());return null;});
    }
    @Test void twentyThousandDetailsKeepOneHeaderAndOneQuantityProjection(){
        stock(1,1,10,100,0,"CONFIRMED","10.1234",today.atStartOfDay());
        for(String d:List.of("in","out")) db.execute("INSERT INTO psi_stock_"+d+"_item SELECT 100000+X,1,1,0,0.0001 FROM SYSTEM_RANGE(1,20000)");
        captured.sql.clear();checkStock(1,"10.1234","5.0003");
        assertEquals(4,captured.sql.size());
        assertTrue(captured.sql.stream().noneMatch(sql->sql.toUpperCase().contains(" IN (")));
    }
    @Test void importBatchPreservesCaseMatchingTenantDepartmentAndOriginalCode(){
        db.update("INSERT INTO psi_goods VALUES(1,1,10,100,0,'AbC','visible',13),(2,1,20,200,0,'OTHER','hidden',9),(3,2,10,100,0,'FOREIGN','tenant',9),(4,1,10,100,4,'DELETED','deleted',9)");
        var matches=goods.selectImportGoods(1L,List.of("abc","OTHER","FOREIGN","DELETED","missing"));assertEquals(1,matches.size());assertEquals("abc",matches.get(0).getImportCode());assertEquals("AbC",matches.get(0).getGoodsCode());assertEquals(1L,matches.get(0).getId());
        assertTrue(goods.selectImportGoods(1L,List.of()).isEmpty());assertTrue(goods.selectImportGoods(1L,null).isEmpty());
        actor(1,100,10,DataScopeEnum.CUSTOM);assertEquals("OTHER",goods.selectImportGoods(1L,List.of("abc","OTHER")).get(0).getGoodsCode());
    }
    @Test void importBatchCannotReuseAnotherScopeInsideTheSameSession(){
        db.update("INSERT INTO psi_goods VALUES(1,1,10,100,0,'A','first',13),(2,1,20,200,0,'B','second',9)");
        tx.execute(status->{
            assertEquals(List.of("A"),goods.selectImportGoods(1L,List.of("A","B")).stream().map(row->row.getGoodsCode()).toList());
            actor(1,100,10,DataScopeEnum.CUSTOM);
            assertEquals(List.of("B"),goods.selectImportGoods(1L,List.of("A","B")).stream().map(row->row.getGoodsCode()).toList());
            return null;
        });
    }
}
