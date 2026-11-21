package com.ls.aegis.biz.file.service.impl;
import com.ls.aegis.biz.file.access.FileAccessService;
import com.ls.aegis.biz.file.enums.*;
import com.ls.aegis.biz.file.mapper.FileMapper;
import com.ls.aegis.biz.file.mapper.StorageMapper;
import com.ls.aegis.biz.file.config.file.FileRecorderImpl;
import com.ls.aegis.biz.file.util.FileDigestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ls.aegis.biz.file.model.entity.*;
import com.ls.aegis.biz.file.service.StorageService;
import com.ls.aegis.common.context.*;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.dromara.x.file.storage.core.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.interceptor.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real LocalPlus SDK, H2/MyBatis rows, and Spring transaction proxies; no runtime business storage. */
class LocalFileRecycleMetadataTest {
 @TempDir Path root;JdbcTemplate db;FileMapper mapper;StorageDO storage;FileStorageService sdk;FileServiceImpl files;FileRecycleServiceImpl recycle;
 GenericApplicationContext spring;jakarta.validation.ValidatorFactory validators;
 static final String META="{\"itlsEomsH5UploadOwner\":\"0:12345\"}";
 static final String THMETA="{\"testThumb\":\"retained\"}";
 static final byte[] MAIN={1,2,3,4,5},THUMB={9,8,7};
 @BeforeEach void setup() throws Exception {
  var actor=new UserContext();actor.setId(99L);actor.setTenantId(0L);actor.setDeptId(1L);actor.setRoleCodes(Set.of("super_admin"));UserContextHolder.setContext(actor,false);
  validators=jakarta.validation.Validation.buildDefaultValidatorFactory();spring=new GenericApplicationContext();spring.registerBean(jakarta.validation.Validator.class,validators::getValidator);spring.refresh();new SpringUtil().setApplicationContext(spring);
  var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");ds.setDriverClassName("org.h2.Driver");db=new JdbcTemplate(ds);
  var c=new MybatisConfiguration();c.setMapUnderscoreToCamelCase(true);c.setDefaultEnumTypeHandler(top.continew.starter.data.handler.CompositeBaseEnumTypeHandler.class);c.addMapper(FileMapper.class);
  var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(c);factory.setMapperLocations(new ClassPathResource("mapper/FileMapper.xml"));var session=new SqlSessionTemplate(factory.getObject());mapper=session.getMapper(FileMapper.class);
  var ti=TableInfoHelper.getTableInfo(FileDO.class);var ddl=new StringBuilder("create table sys_file(id bigint primary key");for(var f:ti.getFieldList()){String t=f.getPropertyType()==Long.class?"bigint":f.getPropertyType()==LocalDateTime.class?"timestamp":f.getProperty().equals("type")?"int":"varchar(4096)";ddl.append(',').append(f.getColumn()).append(' ').append(t);if(f.getProperty().equals("deleted"))ddl.append(" default 0");}db.execute(ddl.append(')').toString());
  storage=new StorageDO();storage.setId(7L);storage.setType(StorageTypeEnum.LOCAL);storage.setCode("test-local");storage.setRecycleBinEnabled(true);storage.setRecycleBinPath("recycle/");
  var config=new FileStorageProperties.LocalPlusConfig();config.setPlatform("test-local");config.setStoragePath(root.toString()+"/");config.setBasePath("");config.setDomain("http://invalid.test/");sdk=FileStorageServiceBuilder.create(new FileStorageProperties()).useDefault().addFileStorage(FileStorageServiceBuilder.buildLocalPlusFileStorage(List.of(config))).build();
  sdk.setFileRecorder(new FileRecorderImpl(mapper,mock(StorageMapper.class)));
  var ss=mock(StorageService.class);when(ss.getById(7L)).thenReturn(storage);when(ss.listByIds(anyCollection())).thenReturn(List.of(storage));
  files=new FileServiceImpl(new FileAccessService(List.of()),sdk,mock(org.springframework.beans.factory.ObjectProvider.class));ReflectionTestUtils.setField(files,"baseMapper",mapper);ReflectionTestUtils.setField(files,"storageService",ss);
  recycle=new FileRecycleServiceImpl(mapper,ss,sdk);var tx=new DataSourceTransactionManager(ds);files=proxy(files,tx);recycle=proxy(recycle,tx);
  FileDO row=new FileDO();row.setId(123L);row.setName("photo.jpg");row.setOriginalName("合成照片.jpg");row.setParentPath("/eoms/recycle/source");row.setSize((long)MAIN.length);row.setThumbnailName("thumb.jpg");row.setThumbnailSize((long)THUMB.length);row.setStorageId(7L);row.setType(FileTypeEnum.IMAGE);row.setFileDigest(FileDigestUtils.sm3Hex(MAIN,null));row.setMetadata(META);row.setThumbnailMetadata(THMETA);row.setCreateUser(99L);mapper.insert(row);
  Files.createDirectories(root.resolve("eoms/recycle/source"));Files.write(root.resolve("eoms/recycle/source/photo.jpg"),MAIN);Files.write(root.resolve("eoms/recycle/source/thumb.jpg"),THUMB);
 }
 @SuppressWarnings("unchecked") <T>T proxy(T value,DataSourceTransactionManager tx){var p=new ProxyFactory(value);p.setProxyTargetClass(true);p.addAdvice(new TransactionInterceptor(tx,new AnnotationTransactionAttributeSource()));return (T)p.getProxy();}
 @AfterEach void close(){sdk.destroy();db.execute("SHUTDOWN");UserContextHolder.clearContext();spring.close();new SpringUtil().setApplicationContext(null);validators.close();}
 long deleted(){return db.queryForObject("select deleted from sys_file where id=123",Long.class);}
 void metadata(){assertEquals(META,db.queryForObject("select metadata from sys_file where id=123",String.class));assertEquals(THMETA,db.queryForObject("select thumbnail_metadata from sys_file where id=123",String.class));}
 void physical(String prefix,boolean exists) throws Exception{for(String name:List.of("photo.jpg","thumb.jpg")){Path p=root.resolve(prefix+"eoms/recycle/source/"+name);assertEquals(exists,Files.exists(p),p.toString());if(exists)assertArrayEquals(name.startsWith("photo")?MAIN:THUMB,Files.readAllBytes(p));}}
 @Test void baselineRawSdkRejectsPersistedMetadataBeforeTouchingFiles() throws Exception {var info=mapper.selectById(123L).toFileInfo(storage);assertThrows(RuntimeException.class,()->sdk.move(info).setPath("recycle/"+info.getPath()).move());physical("",true);physical("recycle/",false);metadata();}
 @Test void businessDeleteRestoreDeletePurgePreservesMetadataAndBothFiles() throws Exception {
  files.delete(List.of(123L));assertEquals(1,deleted());metadata();physical("",false);physical("recycle/",true);
  recycle.restore(123L);assertEquals(0,deleted());metadata();physical("",true);physical("recycle/",false);
  files.delete(List.of(123L));assertEquals(1,deleted());metadata();physical("",false);physical("recycle/",true);
  recycle.delete(123L);assertEquals(123,deleted());metadata();physical("",false);physical("recycle/",false);
 }
 @Test void physicalMoveFailureRollsBackDatabaseDeletionWithoutDiscardingOwner() throws Exception {
  Files.write(root.resolve("recycle"),new byte[]{1});assertThrows(RuntimeException.class,()->files.delete(List.of(123L)));assertEquals(0,deleted());metadata();physical("",true);
 }
 @Test void restoreFailureRollsBackDatabaseRestoration() throws Exception {
  files.delete(List.of(123L));Files.delete(root.resolve("recycle/eoms/recycle/source/photo.jpg"));
  assertThrows(RuntimeException.class,()->recycle.restore(123L));assertEquals(1,deleted());metadata();assertArrayEquals(THUMB,Files.readAllBytes(root.resolve("recycle/eoms/recycle/source/thumb.jpg")));
 }
 @Test void metadataPolicyIsNotGloballyDisabledForCloudStorage(){
  var cloud=mock(FileStorageService.class);var move=mock(org.dromara.x.file.storage.core.move.MovePretreatment.class,RETURNS_SELF);
  when(cloud.move(any(FileInfo.class))).thenReturn(move);when(cloud.exists(any(FileInfo.class))).thenReturn(true);
  sdk.setFileRecorder(new FileRecorderImpl(mapper,mock(StorageMapper.class)));
  var ss=mock(StorageService.class);storage.setType(StorageTypeEnum.OSS);when(ss.listByIds(anyCollection())).thenReturn(List.of(storage));
  var service=new FileServiceImpl(new FileAccessService(List.of()),cloud,mock(org.springframework.beans.factory.ObjectProvider.class));ReflectionTestUtils.setField(service,"baseMapper",mapper);ReflectionTestUtils.setField(service,"storageService",ss);service.delete(List.of(123L));
  verify(move).setNotSupportMetadataThrowException(false,false);verify(move).move();metadata();
 }

 void simulateOldHalfRestore() {
  files.delete(List.of(123L));
  FileDO row=mapper.selectByIdInRecycleBin(123L);
  mapper.restoreInRecycleBin(123L,99L); // old service activated before SDK callback
  var info=row.toFileInfo(storage);String target=info.getPath();info.setPath("recycle/"+target);
  sdk.move(info).setNotSupportMetadataThrowException(false).setPath(target).move();
  assertEquals(1,deleted());
 }
 @Test void actualRecorderHalfRestoreCanBeVerifiedAndReconciledWithoutMovingFiles() throws Exception {
  simulateOldHalfRestore();physical("",true);physical("recycle/",false);
  recycle.restore(123L);assertEquals(0,deleted());metadata();physical("",true);physical("recycle/",false);
 }
 @ParameterizedTest @ValueSource(strings={"digestMissing","digestWrong","sizeWrong","thumbMissing","thumbSizeWrong","thumbDigestWrong"})
 void halfRestoreRejectsUnverifiableIdentityAndLeavesDatabaseRecycled(String fault) throws Exception {
  simulateOldHalfRestore();
  switch(fault){
   case "digestMissing"->db.update("update sys_file set file_digest=null where id=123");
   case "digestWrong"->db.update("update sys_file set file_digest=? where id=123","0".repeat(64));
   case "sizeWrong"->db.update("update sys_file set size=99 where id=123");
   case "thumbMissing"->Files.delete(root.resolve("eoms/recycle/source/thumb.jpg"));
   case "thumbSizeWrong"->db.update("update sys_file set thumbnail_size=99 where id=123");
   case "thumbDigestWrong"->db.update("update sys_file set thumbnail_metadata=? where id=123","{\"sm3\":\""+"0".repeat(64)+"\"}");
  }
  assertThrows(RuntimeException.class,()->recycle.restore(123L));assertEquals(1,deleted());assertArrayEquals(MAIN,Files.readAllBytes(root.resolve("eoms/recycle/source/photo.jpg")));
 }
 @Test void storedThumbnailSm3IsCheckedWhenPresent() throws Exception {
  simulateOldHalfRestore();String m="{\"sm3\":\""+FileDigestUtils.sm3Hex(THUMB,null)+"\"}";db.update("update sys_file set thumbnail_metadata=? where id=123",m);
  recycle.restore(123L);assertEquals(0,deleted());assertEquals(m,db.queryForObject("select thumbnail_metadata from sys_file where id=123",String.class));physical("",true);
 }
 @Test void duplicateOriginalAndRecycleSourcesAreRejectedWithoutOverwriting() throws Exception {
  files.delete(List.of(123L));Files.createDirectories(root.resolve("eoms/recycle/source"));Files.write(root.resolve("eoms/recycle/source/photo.jpg"),new byte[]{0});
  assertThrows(RuntimeException.class,()->recycle.restore(123L));assertEquals(1,deleted());physical("recycle/",true);assertArrayEquals(new byte[]{0},Files.readAllBytes(root.resolve("eoms/recycle/source/photo.jpg")));metadata();
 }

}
