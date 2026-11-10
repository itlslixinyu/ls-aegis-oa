package com.ls.aegis.rbac.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.common.context.*;
import com.ls.aegis.rbac.mapper.user.*;
import com.ls.aegis.rbac.model.entity.user.*;
import com.ls.aegis.rbac.service.UserPreferenceService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserPreferenceDeletionTest {
    JdbcTemplate db;TransactionTemplate tx;UserServiceImpl service;
    org.springframework.context.support.GenericApplicationContext spring;jakarta.validation.ValidatorFactory validators;
    @BeforeEach void setup() throws Exception {
        validators=jakarta.validation.Validation.buildDefaultValidatorFactory();spring=new org.springframework.context.support.GenericApplicationContext();spring.registerBean(jakarta.validation.Validator.class,validators::getValidator);spring.refresh();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(spring);
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var config=new MybatisConfiguration();config.addMapper(UserMapper.class);config.addMapper(UserPreferenceMapper.class);var f=new MybatisSqlSessionFactoryBean();f.setConfiguration(config);f.setDataSource(ds);var sessions=new SqlSessionTemplate(f.getObject());
        var info=TableInfoHelper.getTableInfo(UserDO.class);var cols=new ArrayList<String>();cols.add("id BIGINT PRIMARY KEY");
        for(var field:info.getFieldList()){var t=field.getPropertyType();String type=t==Long.class?"BIGINT":t==Integer.class?"INT":t==Boolean.class?"BOOLEAN":t==java.time.LocalDateTime.class?"TIMESTAMP":"VARCHAR(2000)";cols.add(field.getColumn()+" "+type);}
        db.execute("CREATE TABLE sys_user("+String.join(",",cols)+")");db.execute("CREATE TABLE sys_user_preference(id BIGINT PRIMARY KEY,user_id BIGINT,ui_json VARCHAR(2000),create_time TIMESTAMP,update_time TIMESTAMP)");
        db.update("INSERT INTO sys_user(id,nickname,is_system,deleted) VALUES(100,'synthetic',false,0),(200,'protected',true,0)");db.update("INSERT INTO sys_user_preference(id,user_id,ui_json) VALUES(1,100,'{}'),(2,200,'{}')");
        var prefs=new UserPreferenceServiceImpl(sessions.getMapper(UserPreferenceMapper.class));var constructor=UserServiceImpl.class.getConstructors()[0];var args=Arrays.stream(constructor.getParameterTypes()).map(t->t==UserPreferenceService.class?prefs:mock(t)).toArray();service=(UserServiceImpl)constructor.newInstance(args);ReflectionTestUtils.setField(service,"baseMapper",sessions.getMapper(UserMapper.class));
        var actor=new UserContext();actor.setId(1L);actor.setUsername("synthetic-admin");UserContextHolder.setContext(actor);
    }
    @AfterEach void cleanup(){UserContextHolder.clearContext();spring.close();validators.close();}
    @Test void normalUserDeleteRemovesOnlyThatUsersPreferences(){tx.execute(s->{service.delete(List.of(100L));return null;});assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=100 AND deleted=0",Integer.class));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM sys_user_preference WHERE user_id=100",Integer.class));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM sys_user_preference WHERE user_id=200",Integer.class));}
    @Test void rejectedProtectedUserDeleteKeepsPreferences(){assertThrows(RuntimeException.class,()->tx.execute(s->{service.delete(List.of(200L));return null;}));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM sys_user_preference WHERE user_id=200",Integer.class));}
}
