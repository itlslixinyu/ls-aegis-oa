package com.ls.aegis.biz.psi.support;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.psi.mapper.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StockLockSqlTest {
    MybatisConfiguration config(String dialect)throws Exception{
        var c=new MybatisConfiguration();c.setMapUnderscoreToCamelCase(true);c.setDatabaseId(dialect);
        for(String name:List.of("StockMutationLockMapper","RepairStockOutInternalMapper"))try(InputStream in=Files.newInputStream(Path.of("src/main/resources/mapper/"+name+".xml"))){new XMLMapperBuilder(in,c,name,c.getSqlFragments()).parse();}return c;
    }
    @Test void anchorUsesDialectAppropriateUpsertAndLockIsExplicitCompositeKey()throws Exception{
        for(String dialect:List.of("mysql","pgsql","postgresql")){
            var c=config(dialect);var p=Map.of("tenantId",0L,"warehouseId",10L,"goodsId",20L);
            String upsert=c.getMappedStatement(StockMutationLockMapper.class.getName()+".ensureAnchor").getBoundSql(p).getSql().replaceAll("\\s+"," ");
            assertTrue(upsert.contains(dialect.equals("mysql")?"on duplicate key update":"on conflict (tenant_id, warehouse_id, goods_id) do nothing"));
            String lock=c.getMappedStatement(StockMutationLockMapper.class.getName()+".lockAnchor").getBoundSql(p).getSql().replaceAll("\\s+"," ");
            assertTrue(lock.contains("tenant_id=? and warehouse_id=? and goods_id=?"));assertTrue(lock.contains("for update"));
        }
    }
    @Test void internalRepairReadsOtherCreatorInAuthorizedTenantAndCannotUpdateDifferentTenant()throws Exception{
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");var db=new JdbcTemplate(ds);
        try{
            db.execute("create table psi_stock_out(id bigint primary key,tenant_id bigint,out_type varchar(20),doc_status varchar(20),repair_order_id bigint,remark varchar(500),warehouse_id bigint,create_user bigint,deleted bigint default 0,update_time timestamp)");
            db.update("insert into psi_stock_out(id,tenant_id,out_type,doc_status,repair_order_id,warehouse_id,create_user) values(1,0,'REPAIR','CONFIRMED',99,10,100),(2,0,'REPAIR','CONFIRMED',99,20,101),(3,7,'REPAIR','CONFIRMED',99,10,100),(4,0,'SALE','CONFIRMED',99,10,100)");
            var c=config("mysql");c.setEnvironment(new Environment("test",new SpringManagedTransactionFactory(),ds));var mapper=new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(c)).getMapper(RepairStockOutInternalMapper.class);
            new TransactionTemplate(new DataSourceTransactionManager(ds)).executeWithoutResult(status->{
                var heads=mapper.lockRepairHeads(0L,99L,"工单[TEST]结算出库");assertEquals(Set.of(1L,2L),heads.stream().map(h->h.getId()).collect(java.util.stream.Collectors.toSet()));
                assertEquals(0,mapper.markVoid(7L,1L));assertEquals(1,mapper.markVoid(0L,1L));assertEquals(0,mapper.markVoid(0L,1L));
            });
            assertEquals("VOID",db.queryForObject("select doc_status from psi_stock_out where id=1",String.class));
        }finally{db.execute("SHUTDOWN");}
    }
    @Test void internalMappersDeliberatelyBypassCallerPsiScopeWithMandatoryTenantInEveryStatement(){
        for(Class<?> mapper:List.of(StockMutationLockMapper.class,RepairStockOutInternalMapper.class)){
            InterceptorIgnore ignore=mapper.getAnnotation(InterceptorIgnore.class);assertNotNull(ignore);assertEquals("true",ignore.dataPermission());assertEquals("true",ignore.tenantLine());
        }
    }
    @Test void fifoBalanceSnapshotUsesLockingRowsInsteadOfPlainAggregateSnapshot()throws Exception{
        String statement=StockBatchMapper.class.getMethod("selectAllForUpdate",Long.class,Long.class,Long.class)
            .getAnnotation(org.apache.ibatis.annotations.Select.class).value()[0].replaceAll("\\s+"," ").toLowerCase();
        assertTrue(statement.contains("tenant_id=#{tenantid}"));assertTrue(statement.contains("warehouse_id=#{warehouseid}"));
        assertTrue(statement.contains("goods_id=#{goodsid}"));assertTrue(statement.contains("for update"));assertFalse(statement.contains("sum("));
    }
}
