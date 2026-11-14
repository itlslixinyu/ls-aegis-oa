package com.ls.aegis.biz.eoms.h5;

import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.biz.eoms.access.EomsFileAccessChecker;
import com.ls.aegis.biz.file.access.*;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DataScopeEnum;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real checker + existing scope helper + tenant interceptor + actual MyBatis SQL on synthetic H2. */
class EomsFileAccessPermissionTest extends H5FileOwnershipSqlTest {
    EomsFileAccessChecker checker; MockedStatic<StpUtil> stp; UserContext user; Set<String> permissions;
    org.springframework.context.support.GenericApplicationContext spring;
    top.continew.starter.extension.tenant.autoconfigure.TenantProperties tenantConfiguration;
    @BeforeEach void prepareChecker() {
        spring=new org.springframework.context.support.GenericApplicationContext();
        var tenant=mock(com.ls.aegis.common.config.TenantExtensionProperties.class);when(tenant.isDefaultTenant()).thenReturn(true);when(tenant.getDefaultTenantId()).thenReturn(0L);
        spring.registerBean(com.ls.aegis.common.config.TenantExtensionProperties.class,()->tenant);
        tenantConfiguration=new top.continew.starter.extension.tenant.autoconfigure.TenantProperties();tenantConfiguration.setEnabled(true);spring.registerBean(top.continew.starter.extension.tenant.autoconfigure.TenantProperties.class,()->tenantConfiguration);spring.refresh();new cn.hutool.extra.spring.SpringUtil().setApplicationContext(spring);
        platformTenant=7L;
        for(String table:List.of("eoms_repair_apply","eoms_repair_order","eoms_contract")) {
            db.execute("alter table "+table+" add dept_id bigint default 20");db.execute("alter table "+table+" add create_user bigint default 11");
        }
        db.execute("alter table eoms_repair_order add engineer_id bigint default 21");
        db.execute("create table sys_role_dept(role_id bigint,dept_id bigint)");db.update("insert into sys_role_dept values(99,30)");
        db.update("insert into eoms_repair_apply(id,customer_id,tenant_id,photos,del_flag,dept_id,create_user) values(4,10,7,'900','0',30,12)");
        checker=new EomsFileAccessChecker();ReflectionTestUtils.setField(checker,"eomsFileAccessMapper",references);
        user=new UserContext();user.setId(21L);user.setTenantId(7L);user.setDeptId(20L);user.setRoles(Set.of(new RoleContext(1L,"fixture",DataScopeEnum.DEPT)));UserContextHolder.setContext(user,false);
        permissions=new HashSet<>();stp=mockStatic(StpUtil.class);stp.when(StpUtil::isLogin).thenReturn(true);stp.when(()->StpUtil.hasPermission(anyString())).thenAnswer(i->permissions.contains(i.getArgument(0)));
    }
    @AfterEach void clearChecker(){if(stp!=null)stp.close();UserContextHolder.clearContext();if(spring!=null)spring.close();}
    FileDO target(long id){var f=new FileDO();f.setId(id);f.setCreateUser(998L);f.setDeleted(0L);return f;}
    boolean allowed(long id){return checker.canAccess(target(id),FileAccessContext.forPlatformUser());}
    @Test void arbitraryPlatformLoginAndUnrelatedFunctionPermissionCannotReadReferencedEomsFile(){assertFalse(allowed(100));permissions.add("eoms:contract:query");assertFalse(allowed(100));assertTrue(allowed(400));}
    @Test void correctPermissionStillNeedsParentDepartmentAndTenantVisibility(){permissions.add("eoms:repair:query");assertTrue(allowed(100));assertFalse(allowed(900));assertFalse(allowed(101));}
    @Test void selfEngineerCanReadOnlyAssignedOrdersAndTheirRecordAttachments(){user.setRoles(Set.of(new RoleContext(1L,"engineer",DataScopeEnum.SELF)));permissions.add("eoms:order:query");assertTrue(allowed(200));assertTrue(allowed(300));assertFalse(allowed(100));user.setId(22L);assertFalse(allowed(200));}
    @Test void recordPermissionDoesNotGrantOrderPhotosButStillRequiresParentVisibility(){permissions.add("eoms:record:query");assertTrue(allowed(300));assertFalse(allowed(200));user.setDeptId(30L);assertFalse(allowed(300));}
    @Test void customAndMultipleRoleOrScopesArePreserved(){permissions.add("eoms:repair:query");user.setRoles(Set.of(new RoleContext(99L,"custom",DataScopeEnum.CUSTOM)));assertTrue(allowed(900));assertFalse(allowed(100));user.setRoles(Set.of(new RoleContext(99L,"custom",DataScopeEnum.CUSTOM),new RoleContext(1L,"dept",DataScopeEnum.DEPT)));assertTrue(allowed(900));assertTrue(allowed(100));}
    @Test void allScopeRemainsTenantBoundAndStaleContextCannotImpersonateAnotherIdentity(){permissions.add("eoms:repair:query");user.setRoles(Set.of(new RoleContext(1L,"all",DataScopeEnum.ALL)));assertTrue(allowed(900));assertFalse(allowed(101));var original=FileAccessContext.forPlatformUser();user.setId(55L);assertFalse(checker.canAccess(target(100),original));stp.when(StpUtil::isLogin).thenReturn(false);assertFalse(allowed(100));}
    @Test void h5CodeOnlyContextFailsClosedAndExactCustomerTenantCannotCrossSameCode(){assertFalse(checker.canAccess(target(100),FileAccessContext.forH5Customer("SAME-CODE")));assertTrue(checker.canAccess(target(100),FileAccessContext.forH5Member(7L,10L,"SAME-CODE",501L,true)));assertFalse(checker.canAccess(target(101),FileAccessContext.forH5Member(7L,10L,"SAME-CODE",501L,true)));assertTrue(checker.canAccess(target(101),FileAccessContext.forH5Member(8L,11L,"SAME-CODE",502L,true)));}
    @Test void completeFileAccessServiceCannotUseGlobalEomsReferenceAsAlternativeToPermissions(){var access=new FileAccessService(List.of(checker));assertFalse(access.canAccess(target(100),FileAccessContext.forPlatformUser()));permissions.add("eoms:repair:query");assertTrue(access.canAccess(target(100),FileAccessContext.forPlatformUser()));var f=target(100);f.setDeleted(1L);assertFalse(checker.canAccess(f,FileAccessContext.forPlatformUser()));}
    @Test void actualDisabledTenantConfigurationSupportsNullLoginTenantWithoutAllowingAnotherTenant(){
        tenantConfiguration.setEnabled(false);platformTenant=0L;user.setTenantId(null);permissions.add("eoms:repair:query");
        db.update("insert into eoms_repair_apply(id,customer_id,tenant_id,photos,del_flag,dept_id,create_user) values(9,10,0,'950','0',20,11)");
        assertFalse(top.continew.starter.extension.tenant.context.TenantContextHolder.isTenantEnabled());assertTrue(allowed(950));assertFalse(allowed(100));
        user.setTenantId(7L);assertTrue(allowed(950));assertFalse(allowed(100));
        tenantConfiguration.setEnabled(true);user.setTenantId(null);assertFalse(allowed(950));
    }
}
