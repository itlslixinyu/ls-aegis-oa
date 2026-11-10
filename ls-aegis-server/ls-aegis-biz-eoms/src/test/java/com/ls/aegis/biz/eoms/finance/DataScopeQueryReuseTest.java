package com.ls.aegis.biz.eoms.finance;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DataScopeEnum;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Executes the real shared SQL permission fragment over an isolated in-memory database. */
class DataScopeQueryReuseTest {
    UserContext user; MockedStatic<StpUtil> stp; GenericApplicationContext spring; SqlSessionFactory sessions;
    @BeforeEach void setup() throws Exception {
        spring=new GenericApplicationContext();var tenant=mock(com.ls.aegis.common.config.TenantExtensionProperties.class);
        when(tenant.isDefaultTenant()).thenReturn(true);when(tenant.getDefaultTenantId()).thenReturn(0L);
        spring.registerBean(com.ls.aegis.common.config.TenantExtensionProperties.class,()->tenant);spring.refresh();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(spring);
        user=new UserContext();user.setId(21L);user.setTenantId(7L);user.setDeptId(20L);role(DataScopeEnum.DEPT);
        UserContextHolder.setContext(user,false);stp=mockStatic(StpUtil.class);stp.when(StpUtil::isLogin).thenReturn(true);
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");var db=new JdbcTemplate(ds);
        db.execute("create table fixture_order(id bigint,dept_id bigint,create_user bigint,engineer_id bigint)");db.update("insert into fixture_order values(1,20,90,90),(2,30,21,90),(3,30,90,21)");
        db.execute("create table sys_role_dept(role_id bigint,dept_id bigint)");db.update("insert into sys_role_dept values(99,30)");
        var cfg=new MybatisConfiguration();cfg.setEnvironment(new Environment("scope",new SpringManagedTransactionFactory(),ds));
        try(var in=Files.newInputStream(Path.of("src/main/resources/mapper/eoms/EomsDataScopeMapper.xml"))){new XMLMapperBuilder(in,cfg,"shared",cfg.getSqlFragments()).parse();}
        String xml="""
            <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
            <mapper namespace="fixture"><select id="visible" resultType="long">select o.id from fixture_order o where 1=1
            <include refid="com.ls.aegis.biz.eoms.mapper.EomsDataScopeMapper.dataScopeFilter"/> order by o.id</select></mapper>
            """;
        new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),cfg,"fixture",cfg.getSqlFragments()).parse();sessions=new SqlSessionFactoryBuilder().build(cfg);
    }
    @AfterEach void close(){stp.close();UserContextHolder.clearContext();spring.close();}
    void role(DataScopeEnum scope){user.setRoles(Set.of(new RoleContext(10L,"fixture",scope)));}
    List<Long> visible(RepairOrder q){try(var s=sessions.openSession()){return s.selectList("fixture.visible",q);}}
    @Test void forgedSelfEngineerAndCustomScopeCannotExpandDepartmentRows(){
        var q=new RepairOrder();q.getParams().put("dsSelf",true);q.getParams().put("dsIncludeEngineer",true);q.getParams().put("dsCustomRoleIds",Set.of(99L));
        EomsDataScopeHelper.apply(q,true,"o");assertEquals(List.of(1L),visible(q));
    }
    @Test void queryReusedFromSelfToDepartmentDoesNotRetainSelfOrEngineerAccess(){
        var q=new RepairOrder();role(DataScopeEnum.SELF);EomsDataScopeHelper.apply(q,true,"o");assertEquals(List.of(2L,3L),visible(q));
        role(DataScopeEnum.DEPT);EomsDataScopeHelper.apply(q,false,"o");assertEquals(List.of(1L),visible(q));
    }
    @Test void allScopeRemovesPriorFilterButPreservesBusinessFilters(){
        var q=new RepairOrder();q.getParams().put("beginTime","2026-11-07");q.getParams().put("trustedTenant",7L);
        EomsDataScopeHelper.apply(q,false,"o");role(DataScopeEnum.ALL);EomsDataScopeHelper.apply(q,false,"o");
        assertEquals(List.of(1L,2L,3L),visible(q));assertEquals("2026-11-07",q.getParams().get("beginTime"));assertEquals(7L,q.getParams().get("trustedTenant"));
    }
    @Test void h5NoPlatformSessionClearsOldPlatformFlagsWithoutAddingNewScope(){
        var q=new RepairOrder();EomsDataScopeHelper.apply(q,true,"o");stp.when(StpUtil::isLogin).thenReturn(false);
        EomsDataScopeHelper.apply(q,false,"o");assertFalse(q.getParams().containsKey("dataScopeFilter"));assertFalse(q.getParams().containsKey("dsUserId"));
    }
}
