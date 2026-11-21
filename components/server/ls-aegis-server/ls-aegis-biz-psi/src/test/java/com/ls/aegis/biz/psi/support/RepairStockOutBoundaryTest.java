package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.api.RepairStockOutApiImpl;
import com.ls.aegis.biz.psi.enums.*;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.service.StockOutService;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepairStockOutBoundaryTest {
    final StockOutMapper permissionFilteredMapper=mock(StockOutMapper.class);
    final RepairStockOutInternalMapper internal=mock(RepairStockOutInternalMapper.class);
    final StockOutBatchMapper consumed=mock(StockOutBatchMapper.class);
    final FifoStockHelper fifo=mock(FifoStockHelper.class);
    final RepairStockOutApiImpl api=new RepairStockOutApiImpl(mock(StockOutService.class),permissionFilteredMapper,internal,
            mock(PsiWarehouseMapper.class),mock(GoodsMapper.class),mock(StockOutItemMapper.class),consumed,fifo);
    StockOutDO head(long id,long warehouse,Long orderId,String remark) {
        var h=new StockOutDO();h.setId(id);h.setTenantId(0L);h.setWarehouseId(warehouse);h.setRepairOrderId(orderId);h.setRemark(remark);h.setDocStatus(DocStatusEnum.CONFIRMED);h.setDocNo("TEST-OUT-"+id);return h;
    }
    @BeforeEach void identity(){var u=new UserContext();u.setId(200L);u.setTenantId(0L);UserContextHolder.setContext(u,false);}
    @AfterEach void clear(){UserContextHolder.clearContext();}
    void lines(StockOutDO head,long itemId,long goodsId) {
        var item=new StockOutItemDO();item.setId(itemId);item.setTenantId(0L);item.setGoodsId(goodsId);item.setQuantity(BigDecimal.ONE);item.setCostAmount(BigDecimal.TEN);
        when(internal.selectItems(0L,head.getId())).thenReturn(List.of(item));
        var batch=new StockOutBatchDO();batch.setBatchId(itemId+100);batch.setQuantity(BigDecimal.ONE);batch.setUntaxPrice(BigDecimal.TEN);batch.setCostAmount(BigDecimal.TEN);
        when(consumed.selectByOutItemDesc(itemId)).thenReturn(List.of(batch));when(internal.markVoid(0L,head.getId())).thenReturn(1);
    }
    @Test void allWarehousesAreRestoredAcrossCreatorsWithoutUsingOperatorPsiDataScope() {
        var first=head(1,20,99L,null);var second=head(2,10,99L,null);first.setCreateUser(100L);second.setCreateUser(101L);
        when(internal.lockRepairHeads(eq(0L),eq(99L),anyString())).thenReturn(List.of(first,second));lines(first,11,5);lines(second,12,6);
        api.reverseRepairOut(99L,"TEST-ORDER");
        var keys=ArgumentCaptor.forClass(java.util.Collection.class);verify(fifo).lockAll(keys.capture());assertEquals(2,keys.getValue().size());
        var sequence=inOrder(fifo);sequence.verify(fifo).lockAll(any());sequence.verify(fifo).restoreConsumes(eq(0L),eq(20L),eq(5L),anyList(),eq(BizSourceTypeEnum.REPAIR_REVERSE),eq(1L),anyString());sequence.verify(fifo).restoreConsumes(eq(0L),eq(10L),eq(6L),anyList(),eq(BizSourceTypeEnum.REPAIR_REVERSE),eq(2L),anyString());
        verify(internal).markVoid(0L,1L);verify(internal).markVoid(0L,2L);verifyNoInteractions(permissionFilteredMapper);
    }
    @Test void alreadyVoidIsIdempotentAndCannotRestoreTwice() {
        var head=head(1,20,99L,null);head.setDocStatus(DocStatusEnum.VOID);when(internal.lockRepairHeads(eq(0L),eq(99L),anyString())).thenReturn(List.of(head));
        api.reverseRepairOut(99L,"TEST-ORDER");verify(fifo,never()).restoreConsumes(anyLong(),anyLong(),anyLong(),anyList(),any(),anyLong(),anyString());verify(internal,never()).markVoid(anyLong(),anyLong());
    }
    @Test void wrongTenantReturnedFromInternalMapperFailsClosed() {
        var head=head(1,20,99L,null);head.setTenantId(7L);when(internal.lockRepairHeads(eq(0L),eq(99L),anyString())).thenReturn(List.of(head));
        assertThrows(RuntimeException.class,()->api.reverseRepairOut(99L,"TEST-ORDER"));verifyNoInteractions(fifo);
    }
    @Test void legacySuffixAndPercentUnderscoreAreMatchedLiterallyRatherThanBroadLike() {
        String order="TEST_1%";String prefix="工单["+order+"]结算出库";
        var legacy=head(1,20,null,prefix+"-2");var unrelated=head(2,10,null,prefix+"-bogus");
        when(internal.lockRepairHeads(0L,99L,prefix)).thenReturn(List.of(legacy,unrelated));lines(legacy,11,5);
        assertThrows(RuntimeException.class,()->api.reverseRepairOut(99L,order));verifyNoInteractions(fifo);verify(internal,never()).markVoid(anyLong(),anyLong());
    }
    @Test void internalVoidCasFailurePropagatesInsteadOfReportingSuccessfulStockCompensation() {
        var head=head(1,20,99L,null);when(internal.lockRepairHeads(eq(0L),eq(99L),anyString())).thenReturn(List.of(head));lines(head,11,5);when(internal.markVoid(0L,1L)).thenReturn(0);
        assertThrows(RuntimeException.class,()->api.reverseRepairOut(99L,"TEST-ORDER"));verifyNoInteractions(permissionFilteredMapper);
    }
    @Test void cancellationGuardRejectsAnyActiveHeadAcrossOperatorDepartments() {
        var out=head(1,20,99L,null);out.setCreateUser(101L);when(internal.lockRepairHeads(eq(0L),eq(99L),anyString())).thenReturn(List.of(out));
        assertThrows(RuntimeException.class,()->api.assertNoActiveRepairOut(99L,"TEST-ORDER"));verifyNoInteractions(permissionFilteredMapper,fifo);
    }
    @Test void cancellationGuardAllowsOnlyVoidAndRejectsAmbiguousLegacyOrForeignTenant() {
        var out=head(1,20,99L,null);out.setDocStatus(DocStatusEnum.VOID);when(internal.lockRepairHeads(eq(0L),eq(99L),anyString())).thenReturn(List.of(out));
        api.assertNoActiveRepairOut(99L,"TEST-ORDER");out.setTenantId(7L);assertThrows(RuntimeException.class,()->api.assertNoActiveRepairOut(99L,"TEST-ORDER"));
        out.setTenantId(0L);out.setRepairOrderId(null);out.setRemark("工单[TEST-ORDER]结算出库-bad");assertThrows(RuntimeException.class,()->api.assertNoActiveRepairOut(99L,"TEST-ORDER"));verifyNoInteractions(permissionFilteredMapper,fifo);
    }

}
