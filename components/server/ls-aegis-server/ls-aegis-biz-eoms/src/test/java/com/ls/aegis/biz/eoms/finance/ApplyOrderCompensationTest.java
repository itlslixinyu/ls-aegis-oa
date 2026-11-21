package com.ls.aegis.biz.eoms.finance;

import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.repair.*;
import com.ls.aegis.biz.eoms.service.impl.repair.*;
import com.ls.aegis.biz.eoms.service.repair.IRepairSettlementService;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplyOrderCompensationTest {
    final RepairOrderMapper orders=mock(RepairOrderMapper.class);
    final RepairApplyMapper applies=mock(RepairApplyMapper.class);
    final RepairRecordMapper records=mock(RepairRecordMapper.class);
    final IRepairSettlementService settlements=mock(IRepairSettlementService.class);
    final EomsPlatformFileHelper files=mock(EomsPlatformFileHelper.class);
    RepairOrderServiceImpl orderService() {
        var s=spy(new RepairOrderServiceImpl());
        ReflectionTestUtils.setField(s,"repairOrderMapper",orders);ReflectionTestUtils.setField(s,"repairApplyMapper",applies);
        ReflectionTestUtils.setField(s,"repairRecordMapper",records);ReflectionTestUtils.setField(s,"repairSettlementService",settlements);
        ReflectionTestUtils.setField(s,"platformFileHelper",files);return s;
    }
    RepairOrder order(String status) {var o=new RepairOrder();o.setId(1L);o.setApplyId(2L);o.setStatus(status);o.setPhotos("shared-apply-photo");return o;}
    @Test void deletingUnacceptedOrderRestoresOriginAndPreservesSharedPhotosAndForeignKey() {
        var service=orderService();var order=order("WAIT_ACCEPT");doReturn(order).when(service).selectRepairOrderById(1L);
        when(orders.lockOrder(1L)).thenReturn(order);when(orders.deleteRepairOrderByIds(any())).thenReturn(1);
        assertEquals(1,service.deleteRepairOrderById(1L));
        var sequence=inOrder(applies,orders);
        sequence.verify(applies).lockApply(2L);sequence.verify(orders).lockOrder(1L);sequence.verify(orders).deleteRepairOrderByIds(any());sequence.verify(applies).restoreAfterOrderDelete(2L);
        verify(orders,never()).clearApplyId(anyLong());verifyNoInteractions(files);
    }
    @Test void executedOrderCannotBeDeletedAsFinancialCompensation() {
        var service=orderService();var order=order("FINISHED");doReturn(order).when(service).selectRepairOrderById(1L);when(orders.lockOrder(1L)).thenReturn(order);
        assertThrows(ServiceException.class,()->service.deleteRepairOrderById(1L));verify(orders,never()).deleteRepairOrderByIds(any());verify(applies,never()).restoreAfterOrderDelete(anyLong());
    }
    @Test void orphanedOrderStatusApplyCanBeArchivedWithoutErasingHistoricalLink() {
        var service=spy(new RepairApplyServiceImpl());
        ReflectionTestUtils.setField(service,"repairOrderMapper",orders);ReflectionTestUtils.setField(service,"repairApplyMapper",applies);
        var apply=new RepairApply();apply.setId(2L);apply.setStatus("ORDER");
        doReturn(apply).when(service).selectRepairApplyById(2L);when(applies.lockApply(2L)).thenReturn(apply);when(applies.selectRepairApplyById(2L)).thenReturn(apply);
        when(applies.deleteRepairApplyByIds(any())).thenReturn(1);
        assertEquals(1,service.deleteRepairApplyById(2L));verify(orders,never()).clearApplyId(anyLong());
    }
    @Test void applyWithAnyActiveOrderCannotBeDeleted() {
        var service=spy(new RepairApplyServiceImpl());ReflectionTestUtils.setField(service,"repairOrderMapper",orders);ReflectionTestUtils.setField(service,"repairApplyMapper",applies);
        var apply=new RepairApply();apply.setId(2L);doReturn(apply).when(service).selectRepairApplyById(2L);when(applies.lockApply(2L)).thenReturn(apply);when(orders.countActiveByApply(2L)).thenReturn(1);
        assertThrows(ServiceException.class,()->service.deleteRepairApplyById(2L));verify(applies,never()).deleteRepairApplyByIds(any());
    }
    @Test void crudCannotAdvanceOrderStateWithoutTheSettlementTransaction() {
        var service=orderService();var current=order("WAIT_SETTLEMENT");doReturn(current).when(service).selectRepairOrderById(1L);when(orders.lockOrder(1L)).thenReturn(current);
        var request=order("FINISHED");assertThrows(ServiceException.class,()->service.updateRepairOrder(request));verify(orders,never()).updateRepairOrder(any());
    }
    @Test void confirmedFinancialOrderCannotBeEditedThroughCrud() {
        var service=orderService();var current=order("FINISHED");doReturn(current).when(service).selectRepairOrderById(1L);when(orders.lockOrder(1L)).thenReturn(current);
        assertThrows(ServiceException.class,()->service.updateRepairOrder(current));verify(orders,never()).updateRepairOrder(any());
    }
    @Test void convertedApplyCannotBeResetThroughCrudToCreateAnotherOrder() {
        var service=spy(new RepairApplyServiceImpl());ReflectionTestUtils.setField(service,"repairApplyMapper",applies);
        var current=new RepairApply();current.setId(2L);current.setStatus("ORDER");doReturn(current).when(service).selectRepairApplyById(2L);when(applies.lockApply(2L)).thenReturn(current);
        var request=new RepairApply();request.setId(2L);request.setStatus("CONFIRM");
        assertThrows(ServiceException.class,()->service.updateRepairApply(request));verify(applies,never()).updateRepairApply(any());
    }
    @Test void closeRequiresNonblankReasonWithinStoredLengthWithoutWriting() {
        var service = new RepairApplyServiceImpl();
        ReflectionTestUtils.setField(service, "repairApplyMapper", applies);
        for (String reason : new String[]{null, "", " \t\n ", "x".repeat(501)}) {
            assertThrows(ServiceException.class, () -> service.closeApply(2L, reason));
        }
        verifyNoInteractions(applies);
    }
    @Test void closePersistsTrimmedReasonOnlyForConfirmedApplication() {
        var service = spy(new RepairApplyServiceImpl());
        ReflectionTestUtils.setField(service, "repairApplyMapper", applies);
        var current = new RepairApply(); current.setId(2L); current.setStatus("CONFIRM");
        doReturn(current).when(service).selectRepairApplyById(2L);
        when(applies.transitionStatus(any(),eq("CONFIRM"))).thenReturn(1);
        assertEquals(1, service.closeApply(2L, "  customer cancelled  "));
        verify(applies).transitionStatus(argThat(a -> a.getId().equals(2L)
            && "CANCEL".equals(a.getStatus()) && "customer cancelled".equals(a.getRemark())),eq("CONFIRM"));
    }
    @Test void concurrentClaimPreventsStaleCancellationWrite() {
        var service=spy(new RepairApplyServiceImpl());ReflectionTestUtils.setField(service,"repairApplyMapper",applies);
        var current=new RepairApply();current.setId(2L);current.setStatus("WAIT");doReturn(current).when(service).selectRepairApplyById(2L);
        when(applies.transitionStatus(any(),eq("WAIT"))).thenReturn(0);
        assertThrows(ServiceException.class,()->service.cancelApply(2L,"stale cancellation"));
        verify(applies,never()).updateRepairApply(any());
    }
    @Test void reasonDoesNotPermitClosingApplicationThatAlreadyBecameOrder() {
        var service = spy(new RepairApplyServiceImpl());
        ReflectionTestUtils.setField(service, "repairApplyMapper", applies);
        var current = new RepairApply(); current.setId(2L); current.setStatus("ORDER");
        doReturn(current).when(service).selectRepairApplyById(2L);
        assertThrows(ServiceException.class, () -> service.closeApply(2L, "valid reason"));
        verify(applies, never()).updateRepairApply(any());
    }
}
