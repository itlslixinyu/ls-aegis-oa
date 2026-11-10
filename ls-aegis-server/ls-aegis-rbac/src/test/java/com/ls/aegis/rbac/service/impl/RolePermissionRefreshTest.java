package com.ls.aegis.rbac.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.common.context.*;
import com.ls.aegis.rbac.mapper.RoleMenuMapper;
import com.ls.aegis.rbac.mapper.RoleMapper;
import com.ls.aegis.rbac.model.entity.RoleDO;
import com.ls.aegis.rbac.model.req.RolePermissionUpdateReq;
import com.ls.aegis.rbac.service.*;
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

class RolePermissionRefreshTest {
    JdbcTemplate db; TransactionTemplate tx; RoleMenuServiceImpl menus; RoleMapper roleMapper;
    org.springframework.context.support.GenericApplicationContext spring;
    jakarta.validation.ValidatorFactory validators;
    @BeforeEach void setup() throws Exception {
        validators=jakarta.validation.Validation.buildDefaultValidatorFactory();spring=new org.springframework.context.support.GenericApplicationContext();
        spring.registerBean(jakarta.validation.Validator.class,validators::getValidator);spring.refresh();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(spring);
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("CREATE TABLE sys_role_menu(role_id BIGINT, menu_id BIGINT, PRIMARY KEY(role_id,menu_id))");
        var configuration=new MybatisConfiguration();configuration.addMapper(RoleMenuMapper.class);configuration.addMapper(RoleMapper.class);
        var factory=new MybatisSqlSessionFactoryBean();factory.setConfiguration(configuration);factory.setDataSource(ds);
        var sessions=new SqlSessionTemplate(factory.getObject());var mapper=sessions.getMapper(RoleMenuMapper.class);roleMapper=sessions.getMapper(RoleMapper.class);
        var table=com.baomidou.mybatisplus.core.metadata.TableInfoHelper.getTableInfo(RoleDO.class);
        db.execute("CREATE TABLE sys_role("+table.getKeyColumn()+" BIGINT PRIMARY KEY,"+table.getFieldList().stream().map(f->f.getColumn()+" VARCHAR(500)").collect(java.util.stream.Collectors.joining(","))+")");
        menus=new RoleMenuServiceImpl();ReflectionTestUtils.setField(menus,"baseMapper",mapper);
    }
    @AfterEach void cleanup(){UserContextHolder.clearContext();spring.close();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(null);validators.close();}
    @Test void clearingLastPermissionsReportsAChangeSoOnlineContextsAreRefreshed(){
        db.update("INSERT INTO sys_role_menu VALUES(7,8),(7,9),(10,8)");
        assertTrue(Boolean.TRUE.equals(tx.execute(s->menus.add(List.of(),7L))));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM sys_role_menu WHERE role_id=7",Integer.class));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM sys_role_menu WHERE role_id=10",Integer.class));
    }
    @Test void unchangedEmptyPermissionsDoNotPretendToBeAnotherChange(){assertTrue(Boolean.FALSE.equals(tx.execute(s->menus.add(List.of(),7L))));}
    @Test void unchangedNonemptyPermissionsRetainLinks(){
        db.update("INSERT INTO sys_role_menu VALUES(7,8)");assertTrue(Boolean.FALSE.equals(tx.execute(s->menus.add(List.of(8L),7L))));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM sys_role_menu",Integer.class));
    }
    @Test void refreshingAnotherUsersPermissionsDoesNotReplaceAdminRequestIdentity(){
        var caller=new UserContext();caller.setId(1L);UserContextHolder.setContext(caller,false);
        var target=new UserContext();target.setId(200L);target.setPermissions(Set.of("quote:query"));
        var session=mock(SaSession.class);when(session.getModel(SaSession.USER,UserContext.class)).thenReturn(target);
        var userRoles=mock(UserRoleService.class);when(userRoles.listUserIdByRoleId(7L)).thenReturn(List.of(200L));
        var service=spy(new RoleServiceImpl(mock(RoleMenuService.class),mock(RoleDeptService.class)));
        ReflectionTestUtils.setField(service,"userRoleService",userRoles);
        doReturn(Set.of()).when(service).listByUserId(200L);doReturn(Set.of()).when(service).listPermissionByUserId(200L);
        try(var stp=mockStatic(StpUtil.class)){
            stp.when(()->StpUtil.getSessionByLoginId(200L,false)).thenReturn(session);
            stp.when(()->StpUtil.getSessionByLoginId(200L)).thenReturn(session);
            service.updateUserContext(7L);
            assertSame(caller,UserContextHolder.getContext());assertTrue(target.getPermissions().isEmpty());
            verify(session).set(SaSession.USER,target);
        }
    }
    @Test void idempotentPermissionSaveAlsoRepairsAStaleOnlineContext(){
        var links=mock(RoleMenuService.class);when(links.add(List.of(),7L)).thenReturn(false);
        db.update("INSERT INTO sys_role(id,name,code,is_system,deleted) VALUES(7,'own','own','false','0')");
        var service=spy(new RoleServiceImpl(links,mock(RoleDeptService.class)));ReflectionTestUtils.setField(service,"baseMapper",roleMapper);
        doNothing().when(service).updateUserContext(7L);
        var req=new RolePermissionUpdateReq();req.setMenuIds(List.of());req.setMenuCheckStrictly(false);
        service.updatePermission(7L,req);verify(service).updateUserContext(7L);
    }
    record RefreshFixture(RoleServiceImpl service,UserContext target,SaSession session,org.mockito.MockedStatic<StpUtil> stp) implements AutoCloseable {
        public void close(){stp.close();}
    }
    RefreshFixture targetFixture(){
        var caller=new UserContext();caller.setId(1L);UserContextHolder.setContext(caller,false);
        var target=new UserContext();target.setId(200L);target.setPermissions(Set.of("quote:query"));
        var session=mock(SaSession.class);when(session.getModel(SaSession.USER,UserContext.class)).thenReturn(target);
        var userRoles=mock(UserRoleService.class);when(userRoles.listUserIdByRoleId(7L)).thenReturn(List.of(200L));
        var service=spy(new RoleServiceImpl(mock(RoleMenuService.class),mock(RoleDeptService.class)));ReflectionTestUtils.setField(service,"userRoleService",userRoles);
        doReturn(Set.of()).when(service).listByUserId(200L);doReturn(Set.of()).when(service).listPermissionByUserId(200L);
        var stp=mockStatic(StpUtil.class);stp.when(()->StpUtil.getSessionByLoginId(200L,false)).thenReturn(session);
        return new RefreshFixture(service,target,session,stp);
    }
    @Test void onlinePermissionsChangeOnlyAfterDatabaseCommit(){
        try(var f=targetFixture()){
            tx.executeWithoutResult(status->{f.service.updateUserContext(7L);verify(f.session,never()).set(anyString(),any());assertTrue(f.target.getPermissions().contains("quote:query"));});
            verify(f.session).set(SaSession.USER,f.target);assertTrue(f.target.getPermissions().isEmpty());
        }
    }
    @Test void rolledBackPermissionChangeDoesNotRewriteOnlineIdentity(){
        try(var f=targetFixture()){
            tx.executeWithoutResult(status->{f.service.updateUserContext(7L);status.setRollbackOnly();});
            verify(f.session,never()).set(anyString(),any());assertTrue(f.target.getPermissions().contains("quote:query"));
        }
    }
}
