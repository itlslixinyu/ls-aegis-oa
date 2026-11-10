package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.*;
import com.ls.aegis.biz.psi.support.*;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.config.TenantExtensionProperties;
import com.ls.aegis.common.enums.*;
import net.sf.jsqlparser.expression.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.extension.datapermission.enums.DataScope;
import top.continew.starter.extension.datapermission.handler.DefaultDataPermissionHandler;
import top.continew.starter.extension.datapermission.model.*;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual inherited CRUD delete + actual MyBatis/Spring transactions, tenant and platform DEPT filtering. */
class MasterReferenceDeletionTest {
 org.mockito.MockedStatic<cn.dev33.satoken.stp.StpUtil> login;
 GenericApplicationContext spring; jakarta.validation.ValidatorFactory validators;
 JdbcTemplate jdbc; SqlSessionTemplate session; TransactionTemplate tx; DataSourceTransactionManager txManager;
 UnitServiceImpl units; GoodsServiceImpl goods; WarehouseServiceImpl warehouses;
 MasterDataReferenceMapper referenceMapper;
 static Stream<Arguments> referenceCases(){return Stream.of(
            Arguments.of("WAREHOUSE","psi_stock_batch","warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_stock_flow","warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_stock_in","warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_stock_out","warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_stock_return","warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_stock_transfer","from_warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_stock_transfer","to_warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_init_stock","warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_stock_check","warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","psi_stock_adjust","warehouse_id",true,"deleted"),
            Arguments.of("WAREHOUSE","eoms_repair_material","warehouse_id",false,"del_flag"),
            Arguments.of("WAREHOUSE","eoms_quote","warehouse_id",false,"del_flag"),
            Arguments.of("WAREHOUSE","eoms_m2_warehouse_map","psi_warehouse_id",false,""),
            Arguments.of("GOODS","psi_stock_batch","goods_id",true,"deleted"),
            Arguments.of("GOODS","psi_stock_flow","goods_id",true,"deleted"),
            Arguments.of("GOODS","psi_stock_in_item","goods_id",true,"deleted"),
            Arguments.of("GOODS","psi_stock_out_item","goods_id",true,"deleted"),
            Arguments.of("GOODS","psi_stock_return_item","goods_id",true,"deleted"),
            Arguments.of("GOODS","psi_stock_transfer_item","goods_id",true,"deleted"),
            Arguments.of("GOODS","psi_init_stock_item","goods_id",true,"deleted"),
            Arguments.of("GOODS","psi_stock_check_item","goods_id",true,"deleted"),
            Arguments.of("GOODS","psi_stock_adjust_item","goods_id",true,"deleted"),
            Arguments.of("GOODS","eoms_repair_material","material_id",false,"del_flag"),
            Arguments.of("GOODS","eoms_quote_detail","goods_id",false,"del_flag"),
            Arguments.of("GOODS","eoms_m3_goods_map","psi_goods_id",false,""),
            Arguments.of("UNIT","psi_stock_in","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_stock_out","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_receivable","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_payable","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_stock_return","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_init_receivable_item","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_init_payable_item","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_init_prepay_item","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_receipt","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_payment","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_invoice_out","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_invoice_in","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_contract_sale","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_contract_purchase","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_unit_contact","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_unit_invoice","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_unit_address","unit_id",true,"deleted"),
            Arguments.of("UNIT","psi_unit_pay","unit_id",true,"deleted"),
            Arguments.of("UNIT","eoms_repair_apply","customer_id",true,"del_flag"),
            Arguments.of("UNIT","eoms_repair_order","customer_id",true,"del_flag"),
            Arguments.of("UNIT","eoms_contract","customer_id",true,"del_flag"),
            Arguments.of("UNIT","eoms_income","customer_id",false,"del_flag"),
            Arguments.of("UNIT","eoms_receivable","customer_id",true,"del_flag"),
            Arguments.of("UNIT","eoms_payment","customer_id",true,"del_flag"),
            Arguments.of("UNIT","eoms_quote","customer_id",false,"del_flag"),
            Arguments.of("UNIT","eoms_m2_unit_map","psi_unit_id",false,"")
 );}
 @BeforeEach void setup() throws Exception {
  login=mockStatic(cn.dev33.satoken.stp.StpUtil.class);setActor(true);
  validators=jakarta.validation.Validation.buildDefaultValidatorFactory();spring=new GenericApplicationContext();spring.registerBean(jakarta.validation.Validator.class,validators::getValidator);spring.registerBean(TenantExtensionProperties.class,TenantExtensionProperties::new);spring.refresh();new SpringUtil().setApplicationContext(spring);
  var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000","sa","");ds.setDriverClassName("org.h2.Driver");jdbc=new JdbcTemplate(ds);txManager=new DataSourceTransactionManager(ds);tx=new TransactionTemplate(txManager);
  var c=new MybatisConfiguration();c.setMapUnderscoreToCamelCase(true);c.setDefaultEnumTypeHandler(top.continew.starter.data.handler.CompositeBaseEnumTypeHandler.class);
  GlobalConfigUtils.getGlobalConfig(c).getDbConfig().setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id");
  var chain=new MybatisPlusInterceptor();chain.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(UserContextHolder.getTenantId());}}));
  chain.addInnerInterceptor(new DataPermissionInterceptor(new DefaultDataPermissionHandler(new DataPermissionUserDataProvider(){
   public boolean isFilter(){return !UserContextHolder.getContext().isSuperAdmin();}
   public UserData getUserData(){return new UserData(99L,12L,Set.of(new RoleData(1L,DataScope.DEPT)));}
  })));c.addInterceptor(chain);
  c.addMapper(UnitMapper.class);c.addMapper(GoodsMapper.class);c.addMapper(PsiWarehouseMapper.class);c.addMapper(MasterDataReferenceMapper.class);
  var f=new MybatisSqlSessionFactoryBean();f.setDataSource(ds);f.setConfiguration(c);session=new SqlSessionTemplate(f.getObject());
  for(Class<?> entity:List.of(UnitDO.class,GoodsDO.class,WarehouseDO.class)){
   var table=TableInfoHelper.getTableInfo(entity);var ddl=new StringBuilder("create table "+table.getTableName()+"(id bigint primary key");
   for(var field:table.getFieldList()){String t=field.getPropertyType()==Long.class?"bigint":field.getPropertyType()==LocalDateTime.class?"timestamp":field.getProperty().equals("status")?"int":"varchar(512)";ddl.append(',').append(field.getColumn()).append(' ').append(t);if(field.getProperty().equals("deleted"))ddl.append(" default 0");}jdbc.execute(ddl.append(')').toString());
   jdbc.update("insert into "+table.getTableName()+"(id,tenant_id,dept_id,status,deleted) values(1,7,12,1,0),(2,7,12,1,0),(3,7,20,1,0),(4,8,12,1,0)");
  }
  jdbc.update("update psi_warehouse set manager_user_id=99");
  Map<String,Set<String>> columns=new TreeMap<>();
  referenceCases().forEach(a->{Object[] v=a.get();String table=(String)v[1],col=(String)v[2],soft=(String)v[4];var cols=columns.computeIfAbsent(table,k->new TreeSet<>());cols.add(col+" bigint");if((Boolean)v[3])cols.add("tenant_id bigint");if(!soft.isEmpty())cols.add(soft+(soft.equals("deleted")?" bigint default 0":" varchar(4) default '0'"));cols.add("dept_id bigint default 20");});
  columns.get("psi_stock_batch").add("remain_quantity decimal(18,4) default 0");
  for(var entry:columns.entrySet())jdbc.execute("create table "+entry.getKey()+"(id bigint auto_increment primary key,"+String.join(",",entry.getValue())+")");
  referenceMapper=session.getMapper(MasterDataReferenceMapper.class);var guard=new MasterDataReferenceGuard(referenceMapper,session.getMapper(UnitMapper.class),session.getMapper(GoodsMapper.class),session.getMapper(PsiWarehouseMapper.class));
  units=new UnitServiceImpl(mock(DocNoGenerator.class),guard);goods=new GoodsServiceImpl(mock(DocNoGenerator.class),guard,mock(GoodsCategoryMapper.class));warehouses=new WarehouseServiceImpl(mock(DocNoGenerator.class),guard);
  ReflectionTestUtils.setField(units,"baseMapper",session.getMapper(UnitMapper.class));ReflectionTestUtils.setField(goods,"baseMapper",session.getMapper(GoodsMapper.class));ReflectionTestUtils.setField(warehouses,"baseMapper",session.getMapper(PsiWarehouseMapper.class));
 }
 void setActor(boolean admin){var user=new UserContext();user.setId(99L);user.setDeptId(12L);user.setTenantId(7L);user.setRoleCodes(Set.of(admin?"super_admin":"operator"));user.setRoles(Set.of(new RoleContext(1L,"operator",DataScopeEnum.DEPT)));UserContextHolder.setContext(user,false);}
 @AfterEach void cleanup(){login.close();UserContextHolder.clearContext();jdbc.execute("SHUTDOWN");spring.close();new SpringUtil().setApplicationContext(null);validators.close();}
 void delete(String kind,List<Long> ids){switch(kind){case "UNIT"->units.delete(ids);case "GOODS"->goods.delete(ids);case "WAREHOUSE"->warehouses.delete(ids);default->throw new IllegalArgumentException();}}
 String master(String kind){return switch(kind){case "UNIT"->"psi_unit";case "GOODS"->"psi_goods";case "WAREHOUSE"->"psi_warehouse";default->throw new IllegalArgumentException();};}
 int deleted(String kind,long id){return jdbc.queryForObject("select deleted from "+master(kind)+" where id=?",Integer.class,id);}
 void reference(String table,String col,boolean tenant,String soft,long id,long tid,boolean removed){String cols=col+(tenant?",tenant_id":"")+(soft.isEmpty()?"":","+soft);String marks="?"+(tenant?",?":"")+(soft.isEmpty()?"":",?");var values=new ArrayList<Object>();values.add(id);if(tenant)values.add(tid);if(!soft.isEmpty())values.add(soft.equals("deleted")?(removed?99:0):(removed?"2":"0"));jdbc.update("insert into "+table+"("+cols+") values("+marks+")",values.toArray());}
 @ParameterizedTest @MethodSource("referenceCases")
 void everyActiveDocumentDetailLedgerOrMigrationReferenceBlocksActualCrudDelete(String kind,String table,String column,boolean tenant,String soft){
  reference(table,column,tenant,soft,1,7,false);var ex=assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->delete(kind,List.of(1L))));assertTrue(ex.getMessage().contains("引用"));assertEquals(0,deleted(kind,1));
 }
 @Test void unusedMastersCanStillBeDeletedAndMixedReferencedBatchIsAtomic(){
  reference("psi_receivable","unit_id",true,"deleted",2,7,false);assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->delete("UNIT",List.of(1L,2L))));assertEquals(0,deleted("UNIT",1));assertEquals(0,deleted("UNIT",2));
  tx.executeWithoutResult(t->delete("UNIT",List.of(1L)));tx.executeWithoutResult(t->delete("GOODS",List.of(1L)));tx.executeWithoutResult(t->delete("WAREHOUSE",List.of(1L)));for(String kind:List.of("UNIT","GOODS","WAREHOUSE"))assertEquals(1,deleted(kind,1));
 }
 @Test void anotherTenantOrSoftDeletedDraftReferencesDoNotBlockOwnUnusedMaster(){
  reference("psi_receivable","unit_id",true,"deleted",1,8,false);reference("psi_stock_in","unit_id",true,"deleted",1,7,true);reference("eoms_quote","customer_id",false,"del_flag",1,7,true);
  tx.executeWithoutResult(t->delete("UNIT",List.of(1L)));assertEquals(1,deleted("UNIT",1));
 }
 @Test void hiddenDepartmentReferencesStillCountButHiddenMasterIsNeverAuthorized(){
  setActor(false);reference("psi_payable","unit_id",true,"deleted",1,7,false);
  // DEPT negative control on the same ordinary master mapper, while global references remain complete.
  assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->delete("UNIT",List.of(3L))));assertEquals(0,deleted("UNIT",3));
  assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->delete("UNIT",List.of(4L))));assertEquals(0,deleted("UNIT",4));
  var ex=assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->delete("UNIT",List.of(1L))));assertTrue(ex.getMessage().contains("引用"));
 }
 @Test void exhaustedBatchAndAuditFlowStillBlockDeletion(){reference("psi_stock_batch","warehouse_id",true,"deleted",1,7,false);reference("psi_stock_flow","goods_id",true,"deleted",1,7,false);assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->delete("WAREHOUSE",List.of(1L))));assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->delete("GOODS",List.of(1L))));}
 @Test void referencedMastersCanStillBeDisabledViaActualServiceUpdate(){
  reference("psi_stock_batch","warehouse_id",true,"deleted",1,7,false);reference("psi_stock_flow","goods_id",true,"deleted",1,7,false);reference("psi_receivable","unit_id",true,"deleted",1,7,false);
  var u=new UnitReq();u.setStatus(DisEnableStatusEnum.DISABLE);tx.executeWithoutResult(t->units.update(u,1L));
  var g=new GoodsReq();g.setStatus(DisEnableStatusEnum.DISABLE);tx.executeWithoutResult(t->goods.update(g,1L));
  var w=new WarehouseReq();w.setStatus(DisEnableStatusEnum.DISABLE);tx.executeWithoutResult(t->warehouses.update(w,1L));
  for(String kind:List.of("UNIT","GOODS","WAREHOUSE"))assertEquals(2,jdbc.queryForObject("select status from "+master(kind)+" where id=1",Integer.class));
 }
 @Test void missingIdentityFailsClosed(){UserContextHolder.clearContext();assertThrows(RuntimeException.class,()->tx.executeWithoutResult(t->delete("UNIT",List.of(1L))));assertEquals(0,deleted("UNIT",1));}
 Object transactional(Object service){var factory=new org.springframework.aop.framework.ProxyFactory(service);factory.setProxyTargetClass(true);factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(txManager,new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));return factory.getProxy();}
 @Test void serviceDeletionAnnotationsActuallyOpenSpringTransactionsForAllThreeMasters(){
  var calls=new java.util.concurrent.atomic.AtomicInteger();
  var guard=new MasterDataReferenceGuard(referenceMapper,session.getMapper(UnitMapper.class),session.getMapper(GoodsMapper.class),session.getMapper(PsiWarehouseMapper.class)){
   @Override public void requireDeletable(String kind,List<Long> ids){assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());calls.incrementAndGet();super.requireDeletable(kind,ids);}
  };
  var u=new UnitServiceImpl(mock(DocNoGenerator.class),guard);ReflectionTestUtils.setField(u,"baseMapper",session.getMapper(UnitMapper.class));((UnitServiceImpl)transactional(u)).delete(List.of(1L));
  var g=new GoodsServiceImpl(mock(DocNoGenerator.class),guard,mock(GoodsCategoryMapper.class));ReflectionTestUtils.setField(g,"baseMapper",session.getMapper(GoodsMapper.class));((GoodsServiceImpl)transactional(g)).delete(List.of(1L));
  var w=new WarehouseServiceImpl(mock(DocNoGenerator.class),guard);ReflectionTestUtils.setField(w,"baseMapper",session.getMapper(PsiWarehouseMapper.class));((WarehouseServiceImpl)transactional(w)).delete(List.of(1L));assertEquals(3,calls.get());
 }
 @Test void concurrentDeletesUseOneMasterAnchorAndOnlyOneCanSucceed() throws Exception {
  var proxy=(UnitServiceImpl)transactional(units);var executor=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
  Callable<Boolean> job=()->{setActor(true);try{start.await();try{proxy.delete(List.of(1L));return true;}catch(BusinessException rejected){return false;}}finally{UserContextHolder.clearContext();}};
  try{var a=executor.submit(job);var b=executor.submit(job);start.countDown();assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));assertEquals(1,deleted("UNIT",1));}finally{executor.shutdownNow();}
 }

 void updateMaster(String kind,Long id,Long department){
  switch(kind){
   case "UNIT"->{var r=new UnitReq();r.setStatus(DisEnableStatusEnum.DISABLE);r.setDeptId(department);r.setTenantId(999L);units.update(r,id);}
   case "GOODS"->{var r=new GoodsReq();r.setStatus(DisEnableStatusEnum.DISABLE);r.setDeptId(department);r.setTenantId(999L);goods.update(r,id);}
   case "WAREHOUSE"->{var r=new WarehouseReq();r.setStatus(DisEnableStatusEnum.DISABLE);r.setDeptId(department);r.setTenantId(999L);warehouses.update(r,id);}
   default->throw new IllegalArgumentException();
  }
 }
 @ParameterizedTest @ValueSource(strings={"UNIT","GOODS","WAREHOUSE"})
 void ownMasterUpdateRetainsPersistedOwnership(String kind){setActor(false);tx.executeWithoutResult(t->updateMaster(kind,1L,999L));assertEquals(12L,jdbc.queryForObject("select dept_id from "+master(kind)+" where id=1",Long.class));assertEquals(7L,jdbc.queryForObject("select tenant_id from "+master(kind)+" where id=1",Long.class));}
 @ParameterizedTest @ValueSource(strings={"UNIT","GOODS","WAREHOUSE"})
 void hiddenMasterCannotBeUpdatedByKnownId(String kind){
  setActor(false);
  switch(kind){
   case "UNIT"->{assertTrue(session.getMapper(UnitMapper.class).selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<UnitDO>lambdaQuery().eq(UnitDO::getId,3L)).isEmpty());}
   case "GOODS"->{assertTrue(session.getMapper(GoodsMapper.class).selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<GoodsDO>lambdaQuery().eq(GoodsDO::getId,3L)).isEmpty());}
   case "WAREHOUSE"->{assertTrue(session.getMapper(PsiWarehouseMapper.class).selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<WarehouseDO>lambdaQuery().eq(WarehouseDO::getId,3L)).isEmpty());}
  }
  assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->updateMaster(kind,3L,null)));assertEquals(1,jdbc.queryForObject("select status from "+master(kind)+" where id=3",Integer.class));
 }

 @ParameterizedTest @ValueSource(strings={"UNIT","GOODS","WAREHOUSE"})
 void administratorNormalUpdateAlsoRetainsOwnership(String kind){setActor(true);tx.executeWithoutResult(t->updateMaster(kind,1L,999L));assertEquals(12L,jdbc.queryForObject("select dept_id from "+master(kind)+" where id=1",Long.class));}
 @ParameterizedTest @ValueSource(strings={"UNIT","GOODS","WAREHOUSE"})
 void otherTenantUpdateIsRejected(String kind){setActor(true);assertThrows(BusinessException.class,()->tx.executeWithoutResult(t->updateMaster(kind,4L,null)));assertEquals(1,jdbc.queryForObject("select status from "+master(kind)+" where id=4",Integer.class));}
 @ParameterizedTest @ValueSource(strings={"UNIT","GOODS","WAREHOUSE"})
 void creationOverwritesClientOwnership(String kind){setActor(false);
  switch(kind){
   case "UNIT"->{var r=new UnitReq();r.setUnitCode("TEST");r.setTenantId(999L);r.setDeptId(999L);units.beforeCreate(r);assertEquals(7L,r.getTenantId());assertEquals(12L,r.getDeptId());}
   case "GOODS"->{var r=new GoodsReq();r.setGoodsCode("TEST");r.setTenantId(999L);r.setDeptId(999L);var cm=mock(GoodsCategoryMapper.class);var cat=new GoodsCategoryDO();cat.setParentId(1L);cat.setCode("02");when(cm.selectById(2L)).thenReturn(cat);r.setCategoryId(2L);new GoodsServiceImpl(mock(DocNoGenerator.class),mock(MasterDataReferenceGuard.class),cm).beforeCreate(r);assertEquals(7L,r.getTenantId());assertEquals(12L,r.getDeptId());}
   case "WAREHOUSE"->{var r=new WarehouseReq();r.setWarehouseCode("TEST");r.setTenantId(999L);r.setDeptId(999L);warehouses.beforeCreate(r);assertEquals(7L,r.getTenantId());assertEquals(12L,r.getDeptId());}
  }
 }

}
