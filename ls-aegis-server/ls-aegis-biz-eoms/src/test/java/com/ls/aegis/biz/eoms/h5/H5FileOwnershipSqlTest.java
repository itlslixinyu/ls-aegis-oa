package com.ls.aegis.biz.eoms.h5;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ls.aegis.biz.eoms.mapper.file.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class H5FileOwnershipSqlTest {
    long platformTenant = 999L;
    JdbcTemplate db; EomsFileAccessMapper references; H5AttachmentMapper uploads;
    @BeforeEach void setup() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");db=new JdbcTemplate(ds);
        db.execute("CREATE ALIAS FIND_IN_SET FOR '"+getClass().getName()+".findInSet'");
        db.execute("create table psi_unit(id bigint primary key,tenant_id bigint,dept_id bigint,unit_code varchar(40),unit_type varchar(16),status int,deleted int)");
        db.execute("create table eoms_repair_apply(id bigint,customer_id bigint,tenant_id bigint,photos varchar(100),del_flag char(1))");
        db.execute("create table eoms_repair_order(id bigint,customer_id bigint,tenant_id bigint,photos varchar(100),del_flag char(1))");
        db.execute("create table eoms_repair_record(id bigint,repair_order_id bigint,tenant_id bigint,processing_photos varchar(100),completed_photos varchar(100),del_flag char(1))");
        db.execute("create table eoms_contract(id bigint,customer_id bigint,tenant_id bigint,file_id varchar(100),del_flag char(1))");
        db.execute("create table sys_file(id bigint primary key,metadata varchar(2000),deleted bigint)");
        db.update("insert into psi_unit values(10,7,20,'SAME-CODE','CUSTOMER',1,0),(11,8,30,'SAME-CODE','CUSTOMER',1,0),(12,7,20,'DISABLED','CUSTOMER',2,0)");
        db.update("insert into eoms_repair_apply values(1,10,7,'100','0'),(2,11,8,'101','0'),(3,12,7,'102','0')");
        db.update("insert into eoms_repair_order values(5,10,7,'200','0')");
        db.update("insert into eoms_repair_record values(6,5,7,'300','301','0'),(7,5,8,'302','303','0')");
        db.update("insert into eoms_contract values(8,10,7,'400','0')");
        db.update("insert into sys_file values(100,null,0),(101,'{}',1)");
        var cfg=new MybatisConfiguration();cfg.setEnvironment(new Environment("h5",new SpringManagedTransactionFactory(),ds));
        var interceptor=new MybatisPlusInterceptor();interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){
            public net.sf.jsqlparser.expression.Expression getTenantId(){return new net.sf.jsqlparser.expression.LongValue(platformTenant);}
            public boolean ignoreTable(String name){return name.equalsIgnoreCase("sys_file");}
        }));cfg.addInterceptor(interceptor);
        for(String name:List.of("EomsDataScopeMapper.xml","EomsFileAccessMapper.xml"))try(var in=Files.newInputStream(Path.of("src/main/resources/mapper/eoms/"+name))){new XMLMapperBuilder(in,cfg,name,cfg.getSqlFragments()).parse();}
        cfg.addMapper(H5AttachmentMapper.class);
        var sessions=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(cfg));references=sessions.getMapper(EomsFileAccessMapper.class);uploads=sessions.getMapper(H5AttachmentMapper.class);
    }
    public static int findInSet(String id,String csv){return csv==null?0:Arrays.asList(csv.split(",")).indexOf(id)+1;}
    @Test void explicitCustomerTenantWorksEvenWhenPlatformTenantContextIsDifferentAndAllCanonicalReferenceBranchesParse(){
        for(long file:List.of(100L,200L,300L,301L,400L))assertEquals(1,references.countByOwnerAndFileId(7L,10L,file));
        assertEquals(20L,uploads.selectActiveCustomerDept(7L,10L));
    }
    @Test void sameCustomerCodeDifferentTenantDoesNotGrantAccessAndMismatchedLegacyRecordTenantIsRejected(){
        assertEquals(0,references.countByOwnerAndFileId(7L,10L,101L));assertEquals(0,references.countByOwnerAndFileId(8L,10L,100L));assertEquals(0,references.countByOwnerAndFileId(7L,10L,302L));
        assertNull(uploads.selectActiveCustomerDept(8L,10L));assertNull(uploads.selectActiveCustomerDept(7L,12L));assertEquals(0,references.countByOwnerAndFileId(7L,12L,102L));
    }
    @Test void metadataBindingUsesActualOptimisticUpdateIncludingSqlNullAndCannotWriteDeletedFile(){
        assertEquals(1,uploads.bindUpload(100L,null,"{\"owner\":\"10\"}"));assertEquals(0,uploads.bindUpload(100L,null,"{\"owner\":\"11\"}"));
        assertEquals("{\"owner\":\"10\"}",db.queryForObject("select metadata from sys_file where id=100",String.class));assertEquals(0,uploads.bindUpload(101L,"{}","changed"));
    }
}
