package com.ls.aegis.biz.eoms.h5;
import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.repair.*;
import com.ls.aegis.biz.eoms.service.h5.H5CustomerScope;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class H5MemberScopeTest {
    RepairApplyMapper applies=mock(RepairApplyMapper.class);RepairOrderMapper orders=mock(RepairOrderMapper.class);
    H5CustomerScope scope=new H5CustomerScope(applies,orders);
    H5CustomerInfo session(String role){var s=new H5CustomerInfo(10L,"C","Company");s.setTenantId(7L);s.setAccountId(501L);s.setAccountRole(role);return s;}
    RepairApply apply(Long reporter){var a=new RepairApply();a.setId(1L);a.setTenantId(7L);a.setCustomerId(10L);a.setReporterAccountId(reporter);when(applies.selectRepairApplyById(1L)).thenReturn(a);return a;}
    @Test void memberOwnOnlyAdminCompanyWideAndLegacyUnattributedAdminOnly(){var a=apply(501L);assertSame(a,scope.requireApply(session("MEMBER"),1L));a.setReporterAccountId(502L);assertThrows(RuntimeException.class,()->scope.requireApply(session("MEMBER"),1L));assertSame(a,scope.requireApply(session("ADMIN"),1L));a.setReporterAccountId(null);assertThrows(RuntimeException.class,()->scope.requireApply(session("MEMBER"),1L));assertSame(a,scope.requireApply(session("ADMIN"),1L));}
    @Test void adminCannotCrossCompanyOrTenant(){var a=apply(501L);a.setCustomerId(11L);assertThrows(RuntimeException.class,()->scope.requireApply(session("ADMIN"),1L));a.setCustomerId(10L);a.setTenantId(8L);assertThrows(RuntimeException.class,()->scope.requireApply(session("ADMIN"),1L));}
    @Test void orderPermissionFollowsItsActualApplyNotGuessedOrderId(){apply(502L);var o=new RepairOrder();o.setId(2L);o.setApplyId(1L);o.setTenantId(7L);o.setCustomerId(10L);when(orders.selectRepairOrderById(2L)).thenReturn(o);assertThrows(RuntimeException.class,()->scope.requireOrder(session("MEMBER"),2L));assertSame(o,scope.requireOrder(session("ADMIN"),2L));}
    @Test void companyOnlyLegacyContextCannotActAsMember(){var s=session("MEMBER");s.setAccountId(null);assertThrows(RuntimeException.class,()->scope.requireApply(s,1L));}
}
