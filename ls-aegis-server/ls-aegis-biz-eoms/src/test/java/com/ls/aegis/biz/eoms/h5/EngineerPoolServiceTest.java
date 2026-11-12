package com.ls.aegis.biz.eoms.h5;

import java.util.Date;
import java.util.List;
import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.biz.eoms.domain.RepairApply;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.mapper.repair.EngineerPoolMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.service.h5.EngineerPoolService;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.common.config.TenantExtensionProperties;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineerPoolServiceTest {
    EngineerPoolMapper pool; RepairOrderMapper orders; EngineerPoolService service;
    RepairApply apply; MockedStatic<StpUtil> stp; MockedStatic<top.continew.starter.extension.tenant.context.TenantContextHolder> tenant;
    @BeforeEach void setup() {
        tenant=mockStatic(top.continew.starter.extension.tenant.context.TenantContextHolder.class);
        stp=mockStatic(StpUtil.class);stp.when(StpUtil::isLogin).thenReturn(true);
        var user=new UserContext();user.setId(21L);user.setUsername("synthetic-engineer");user.setTenantId(0L);user.setDeptId(5L);
        UserContextHolder.setContext(user,false);
        pool=mock(EngineerPoolMapper.class);orders=mock(RepairOrderMapper.class);
        var numbers=mock(EomsNoGenerator.class);when(numbers.generateOrderNo()).thenReturn("SYNTHETIC-GD");
        var tenants=new TenantExtensionProperties();tenants.setDefaultTenantId(0L);
        service=new EngineerPoolService(pool,orders,numbers,tenants);
        when(orders.countEnabledEngineer(0L,21L)).thenReturn(1);
        apply=new RepairApply();apply.setId(9L);apply.setTenantId(0L);apply.setDeptId(7L);apply.setCustomerId(11L);
        apply.setStatus("WAIT");apply.setApplyTime(new Date(System.currentTimeMillis()-660000));
        when(pool.lockApply(0L,9L)).thenReturn(apply);when(pool.lockOrders(0L,9L)).thenReturn(List.of());
        when(orders.insertRepairOrder(any())).thenAnswer(i->{((RepairOrder)i.getArgument(0)).setId(31L);return 1;});
        when(orders.updateRepairOrder(any())).thenReturn(1);when(pool.markConverted(eq(0L),eq(9L),anyString(),any())).thenReturn(1);
    }
    @AfterEach void close(){UserContextHolder.clearContext();stp.close();tenant.close();}
    @Test void overdueUnconfirmedReportCreatesOwnedTaskWithoutAdminConfirmation(){
        assertEquals(31L,service.claim(9L));var c=ArgumentCaptor.forClass(RepairOrder.class);verify(orders).insertRepairOrder(c.capture());
        assertEquals(21L,c.getValue().getEngineerId());assertEquals("ACCEPTED",c.getValue().getStatus());assertNotNull(c.getValue().getAcceptTime());
        assertEquals(7L,c.getValue().getDeptId());assertEquals(0L,c.getValue().getTenantId());assertEquals(11L,c.getValue().getCustomerId());
        verify(pool).markConverted(eq(0L),eq(9L),eq("WAIT"),any());
    }
    @Test void beforeTenMinutesCannotClaim(){apply.setApplyTime(new Date(System.currentTimeMillis()-599000));assertThrows(ServiceException.class,()->service.claim(9L));verify(orders,never()).insertRepairOrder(any());}
    @Test void nonEngineerCannotReadOrClaim(){when(orders.countEnabledEngineer(0L,21L)).thenReturn(0);assertThrows(ServiceException.class,()->service.authorizeEngineer());assertThrows(ServiceException.class,()->service.claim(9L));verifyNoInteractions(pool);}
    @Test void otherTenantOrDeletedReportIsNotFound(){when(pool.lockApply(0L,9L)).thenReturn(null);assertThrows(ServiceException.class,()->service.claim(9L));verify(orders,never()).insertRepairOrder(any());}
    @Test void canceledReportCannotBeClaimed(){apply.setStatus("CANCEL");assertThrows(ServiceException.class,()->service.claim(9L));verify(orders,never()).insertRepairOrder(any());}
    private RepairOrder existing(String status,Long engineer){var o=new RepairOrder();o.setId(32L);o.setStatus(status);o.setEngineerId(engineer);o.setTenantId(0L);when(pool.lockOrders(0L,9L)).thenReturn(List.of(o));return o;}
    @Test void anotherEngineerCannotStealAcceptedTask(){var o=existing("ACCEPTED",22L);o.setAcceptTime(new Date());assertThrows(ServiceException.class,()->service.claim(9L));verify(orders,never()).updateRepairOrder(any());}
    @Test void winningRetryDoesNotCreateOrUpdateAgain(){existing("ACCEPTED",21L);assertEquals(32L,service.claim(9L));verify(orders,never()).updateRepairOrder(any());verify(orders,never()).insertRepairOrder(any());}
    @Test void unacceptedConvertedOrderCanBeClaimedWithoutDuplicateOrder(){apply.setStatus("ORDER");existing("WAIT_ACCEPT",22L);assertEquals(32L,service.claim(9L));verify(orders).updateRepairOrder(any());verify(orders,never()).insertRepairOrder(any());verify(pool,never()).markConverted(any(),any(),any(),any());}
    @Test void inconsistentMultipleOrdersAreNotAutomaticallyChanged(){var o=existing("WAIT_ACCEPT",null);when(pool.lockOrders(0L,9L)).thenReturn(List.of(o,o));assertThrows(ServiceException.class,()->service.claim(9L));verify(orders,never()).updateRepairOrder(any());}
    @Test void failedTransitionFailsWholeClaim(){when(pool.markConverted(any(),any(),any(),any())).thenReturn(0);assertThrows(ServiceException.class,()->service.claim(9L));}
}
