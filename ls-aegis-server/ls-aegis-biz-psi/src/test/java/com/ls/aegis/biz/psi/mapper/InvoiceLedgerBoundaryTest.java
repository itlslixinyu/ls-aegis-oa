package com.ls.aegis.biz.psi.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.*;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceLedgerBoundaryTest {
    JdbcTemplate jdbc;InvoiceLedgerMapper mapper;Visible probe;
    public interface Visible{@Select("SELECT count(*) FROM psi_invoice_out i WHERE i.tenant_id=1 AND i.doc_status='CONFIRMED'")long count();}
    @BeforeEach void setup()throws Exception{
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");ds.setDriverClassName("org.h2.Driver");jdbc=new JdbcTemplate(ds);
        for(String side:List.of("in","out")){
            jdbc.execute("CREATE TABLE psi_invoice_"+side+"(id bigint primary key,tenant_id bigint,dept_id bigint,doc_no varchar(64),doc_status varchar(20),red_of_id bigint,deleted int)");
            jdbc.execute("CREATE TABLE psi_invoice_"+side+"_link(id bigint primary key,tenant_id bigint,dept_id bigint,invoice_id bigint,stock_"+side+"_id bigint,amount decimal(18,2),deleted int)");
            jdbc.update("INSERT INTO psi_invoice_"+side+" VALUES(1,1,10,'one','CONFIRMED',null,0),(2,1,20,'two','CONFIRMED',null,0),(3,2,20,'alien','CONFIRMED',null,0),(4,1,20,'draft','DRAFT',null,0),(5,1,20,'red','CONFIRMED',1,0),(6,1,20,'deleted','CONFIRMED',null,1),(7,1,20,'void','VOID',null,0)");
            jdbc.update("INSERT INTO psi_invoice_"+side+"_link VALUES(1,1,10,1,100,30,0),(2,1,20,2,100,40,0),(3,2,20,3,100,900,0),(4,1,20,4,100,900,0),(5,1,20,5,100,-30,0),(6,1,20,6,100,900,0),(7,1,20,7,100,900,0),(8,1,20,2,100,900,1),(9,1,20,3,100,900,0)");
        }
        var config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);var plugins=new MybatisPlusInterceptor();
        plugins.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(999);}}));
        plugins.addInnerInterceptor(new DataPermissionInterceptor(new MultiDataPermissionHandler(){public Expression getSqlSegment(Table t,Expression w,String s){return new EqualsTo(new Column((t.getAlias()==null?t.getName():t.getAlias().getName())+".dept_id"),new LongValue(999));}}));
        config.addInterceptor(plugins);config.addMapper(InvoiceLedgerMapper.class);config.addMapper(Visible.class);
        var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);var session=new SqlSessionTemplate(factory.getObject());mapper=session.getMapper(InvoiceLedgerMapper.class);probe=session.getMapper(Visible.class);
    }
    @AfterEach void cleanup(){jdbc.execute("SHUTDOWN");}
    @Test void authorizedInvariantIncludesOtherDepartmentButNeverAnotherTenant(){
        assertEquals(0,probe.count(),"Negative control: actual plugins must hide ordinary queries");
        assertEquals(0,new BigDecimal("70").compareTo(mapper.outLinked(1L,100L,null)));
        assertEquals(0,new BigDecimal("70").compareTo(mapper.inLinked(1L,100L,null)));
        assertEquals(0,new BigDecimal("900").compareTo(mapper.outLinked(2L,100L,null)));
    }
    @Test void exclusionsAndExactSourceArePreserved(){
        assertEquals(0,new BigDecimal("40").compareTo(mapper.outLinked(1L,100L,1L)));
        assertEquals(0,BigDecimal.ZERO.compareTo(mapper.inLinked(1L,999L,null)));
        assertEquals(0,BigDecimal.ZERO.compareTo(mapper.outLinked(999L,100L,null)));
        assertEquals("red",mapper.outRedNo(1L,1L));assertNull(mapper.outRedNo(2L,1L));
    }
    @Test void voidingBlueRemovesItsCapacityUseWithoutCountingRedAgain(){
        jdbc.update("UPDATE psi_invoice_out SET doc_status='VOID' WHERE id=1");
        assertEquals(0,new BigDecimal("40").compareTo(mapper.outLinked(1L,100L,null)));
    }
}
