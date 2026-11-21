package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.mybatis.base.model.entity.BaseDO;
import net.sf.jsqlparser.expression.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import top.continew.starter.extension.datapermission.enums.DataScope;
import top.continew.starter.extension.datapermission.handler.DefaultDataPermissionHandler;
import top.continew.starter.extension.datapermission.model.*;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Executes injected MP SQL through the actual platform permission handler and tenant plugin. */
class PsiScopedMapperPermissionTest {
 JdbcTemplate jdbc;SqlSessionTemplate session;DataScope scope=DataScope.DEPT;
 @BeforeEach void setup() throws Exception {
  var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");ds.setDriverClassName("org.h2.Driver");jdbc=new JdbcTemplate(ds);
  var c=new MybatisConfiguration();c.setMapUnderscoreToCamelCase(true);c.setDefaultEnumTypeHandler(top.continew.starter.data.handler.CompositeBaseEnumTypeHandler.class);
  GlobalConfigUtils.getGlobalConfig(c).getDbConfig().setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id");
  var chain=new MybatisPlusInterceptor();chain.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(7);}}));
  chain.addInnerInterceptor(new DataPermissionInterceptor(new DefaultDataPermissionHandler(new DataPermissionUserDataProvider(){public boolean isFilter(){return true;}public UserData getUserData(){return new UserData(99L,12L,Set.of(new RoleData(1L,scope)));}})));c.addInterceptor(chain);
  c.addMapper(GoodsMapper.class);c.addMapper(ContractSaleMapper.class);c.addMapper(WriteOffMapper.class);var f=new MybatisSqlSessionFactoryBean();f.setDataSource(ds);f.setConfiguration(c);session=new SqlSessionTemplate(f.getObject());
  for(Class<?> entity:List.of(GoodsDO.class,ContractSaleDO.class,WriteOffDO.class)){
   var table=TableInfoHelper.getTableInfo(entity);var ddl=new StringBuilder("create table "+table.getTableName()+"(id bigint primary key");
   for(var field:table.getFieldList()){String t=field.getPropertyType()==Long.class?"bigint":field.getPropertyType()==LocalDateTime.class?"timestamp":field.getProperty().equals("status")?"int":"varchar(512)";ddl.append(',').append(field.getColumn()).append(' ').append(t);if(field.getProperty().equals("deleted"))ddl.append(" default 0");}jdbc.execute(ddl.append(')').toString());
   jdbc.update("insert into "+table.getTableName()+"(id,tenant_id,dept_id,create_user,deleted,remark) values(1,7,12,99,0,'before'),(2,7,12,100,0,'before'),(3,7,20,99,0,'before'),(4,7,20,100,0,'before'),(5,8,12,99,0,'before'),(6,7,12,99,6,'before')");
  }
 }
 @AfterEach void close(){jdbc.execute("SHUTDOWN");}
 @SuppressWarnings("unchecked") PsiScopedMapper<BaseDO> mapper(String kind){return (PsiScopedMapper<BaseDO>)(Object)(kind.equals("GOODS")?session.getMapper(GoodsMapper.class):kind.equals("WRITEOFF")?session.getMapper(WriteOffMapper.class):session.getMapper(ContractSaleMapper.class));}
 String table(String kind){return kind.equals("GOODS")?"psi_goods":kind.equals("WRITEOFF")?"psi_write_off":"psi_contract_sale";}
 BaseDO update(String kind,long id){if(kind.equals("GOODS")){var x=new GoodsDO();x.setId(id);x.setRemark("changed");return x;}if(kind.equals("WRITEOFF")){var x=new WriteOffDO();x.setId(id);x.setRemark("changed");return x;}var x=new ContractSaleDO();x.setId(id);x.setRemark("changed");return x;}
 List<Long> expected(){return scope==DataScope.DEPT?List.of(1L,2L):List.of(1L,3L);}
 @ParameterizedTest @CsvSource({"GOODS,DEPT","GOODS,SELF","CONTRACT,DEPT","CONTRACT,SELF","WRITEOFF,DEPT","WRITEOFF,SELF"})
 void detailAndBatchAreExactlyListScopeIncludingTenantAndDeleted(String kind,DataScope requested){scope=requested;var m=mapper(kind);var all=List.of(1L,2L,3L,4L,5L,6L);assertEquals(expected(),m.selectList(Wrappers.emptyWrapper()).stream().map(BaseDO::getId).sorted().toList());assertEquals(expected(),m.selectByIds(all).stream().map(BaseDO::getId).sorted().toList());for(Long id:all){if(expected().contains(id))assertNotNull(m.selectById(id));else assertNull(m.selectById(id));}}
 @ParameterizedTest @CsvSource({"GOODS,DEPT","GOODS,SELF","CONTRACT,DEPT","CONTRACT,SELF","WRITEOFF,DEPT","WRITEOFF,SELF"})
 void injectedUpdatesCannotMutateHiddenOtherTenantOrDeletedRows(String kind,DataScope requested){scope=requested;var m=mapper(kind);for(long id=1;id<=6;id++){boolean visible=expected().contains(id);assertEquals(visible?1:0,m.updateById(update(kind,id)),"id="+id);assertEquals(visible?"changed":"before",jdbc.queryForObject("select remark from "+table(kind)+" where id=?",String.class,id));}}
}
