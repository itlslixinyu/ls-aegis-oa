package com.ls.aegis.biz.eoms.finance;

import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.finance.*;
import com.ls.aegis.biz.eoms.mapper.repair.*;
import com.ls.aegis.biz.eoms.service.impl.repair.RepairSettlementServiceImpl;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.common.api.psi.RepairStockOutApi;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SettlementCompensationTest {
    final RepairSettlementMapper settlements=mock(RepairSettlementMapper.class);
    final RepairOrderMapper orders=mock(RepairOrderMapper.class);
    final ReceivableMapper receivables=mock(ReceivableMapper.class);
    final PaymentMapper payments=mock(PaymentMapper.class);
    final CostMapper costs=mock(CostMapper.class);
    final FinanceEventMapper events=mock(FinanceEventMapper.class);
    final IRepairOrderService orderService=mock(IRepairOrderService.class);
    final RepairStockOutApi stock=mock(RepairStockOutApi.class);
    final RepairSettlementServiceImpl service=spy(new RepairSettlementServiceImpl());
    RepairSettlement settlement; Receivable receivable; RepairOrder order; Cost cost;
    @BeforeEach void setup() {
        for(Object[] pair:new Object[][]{{"repairSettlementMapper",settlements},{"repairOrderMapper",orders},{"receivableMapper",receivables},{"paymentMapper",payments},{"costMapper",costs},{"financeEventMapper",events},{"repairOrderService",orderService},{"repairStockOutApi",stock}})
            ReflectionTestUtils.setField(service,(String)pair[0],pair[1]);
        settlement=new RepairSettlement(); settlement.setId(1L);settlement.setRepairOrderId(2L);settlement.setStatus("CONFIRMED");settlement.setTotalCost(new BigDecimal("12.50"));settlement.setTotalIncome(new BigDecimal("50.00"));
        receivable=new Receivable();receivable.setId(3L);receivable.setSettlementId(1L);receivable.setReceivedAmount(BigDecimal.ZERO);
        order=new RepairOrder();order.setId(2L);order.setOrderNo("TEST-ORDER");order.setStatus("FINISHED");
        cost=new Cost();cost.setId(4L);cost.setSettlementId(1L);cost.setRepairOrderId(2L);cost.setAmount(new BigDecimal("12.50"));cost.setCostType("MATERIAL");
        doReturn(settlement).when(service).selectRepairSettlementById(1L);
        when(settlements.lockSettlement(1L)).thenReturn(settlement);
        when(receivables.lockByOrder(2L)).thenReturn(List.of(receivable));
        when(orders.selectRepairOrderById(2L)).thenReturn(order);
        when(costs.lockByOrder(2L)).thenReturn(List.of(cost));
        when(receivables.voidReceivable(eq(3L),nullable(String.class))).thenReturn(1);
        when(settlements.reverseSettlement(eq(1L),nullable(String.class),eq("TEST"))).thenReturn(1);
    }
    @Test void reversalUsesPsiAndPreservesPositiveCostWithExactlyOppositeLinkedEntry() {
        assertEquals(1,service.reverseSettlement(1L,"TEST"));
        var written=ArgumentCaptor.forClass(Cost.class);verify(costs).insertCost(written.capture());
        assertEquals(cost.getId(),written.getValue().getReversalOfId());
        assertEquals(cost.getSettlementId(),written.getValue().getSettlementId());
        assertEquals(0,cost.getAmount().add(written.getValue().getAmount()).compareTo(BigDecimal.ZERO));
        var sequence=inOrder(stock,costs,receivables,orderService,settlements,events);
        sequence.verify(stock).reverseRepairOut(2L,"TEST-ORDER");
        sequence.verify(costs).insertCost(any()); sequence.verify(receivables).voidReceivable(eq(3L),nullable(String.class));
        sequence.verify(orderService).reverseSettlement(eq(2L),nullable(String.class),eq("TEST"));
        sequence.verify(settlements).reverseSettlement(eq(1L),nullable(String.class),eq("TEST"));
        sequence.verify(events).insertEvent(eq("SETTLEMENT"),eq(1L),eq("REVERSE"),eq(2L),eq(new BigDecimal("-50.00")),nullable(String.class),eq("TEST"));
        verify(costs,never()).deleteCostByIds(any());verify(settlements,never()).deleteRepairSettlementById(anyLong());
    }
    @Test void receivedMoneyPreventsAnyStockOrFinancialCompensation() {
        receivable.setReceivedAmount(BigDecimal.ONE);
        assertThrows(ServiceException.class,()->service.reverseSettlement(1L,"TEST"));verifyNoInteractions(stock,events,orderService);verify(costs,never()).insertCost(any());
    }
    @Test void activeReceiptBlocksEvenWhenReceivableAggregateIncorrectlySaysZero() {
        when(payments.countActiveByReceivable(3L)).thenReturn(1);
        assertThrows(ServiceException.class,()->service.reverseSettlement(1L,"TEST"));verifyNoInteractions(stock,events);
    }
    @Test void closedOrderRequiresExplicitWorkflowInsteadOfBeingSilentlyReopened() {
        order.setStatus("CLOSED");assertThrows(ServiceException.class,()->service.reverseSettlement(1L,"TEST"));verifyNoInteractions(stock,events);
    }
    @Test void repeatedReversalDoesNotRepeatStockOrCostCompensation() {
        settlement.setStatus("REVERSED");assertEquals(0,service.reverseSettlement(1L,"TEST"));verifyNoInteractions(stock,receivables,costs,events);
    }
    @Test void historicalUnlinkedReceivableFailsClosed() {
        receivable.setSettlementId(null);assertThrows(ServiceException.class,()->service.reverseSettlement(1L,"TEST"));verifyNoInteractions(stock,events);
    }
    @Test void historicalUnlinkedCostFailsClosed() {
        cost.setSettlementId(null);assertThrows(ServiceException.class,()->service.reverseSettlement(1L,"TEST"));verifyNoInteractions(stock,events);
    }
    @Test void missingOrDuplicateReceivablesAreNotGuessed() {
        when(receivables.lockByOrder(2L)).thenReturn(List.of(receivable,receivable));assertThrows(ServiceException.class,()->service.reverseSettlement(1L,"TEST"));verifyNoInteractions(stock,events);
    }
    @Test void costMismatchBlocksCompensation() {
        cost.setAmount(BigDecimal.ONE);assertThrows(ServiceException.class,()->service.reverseSettlement(1L,"TEST"));verifyNoInteractions(stock,events);
    }
    @Test void psiFailureStopsFinancialMutationAndPropagatesForTransactionRollback() {
        doThrow(new IllegalStateException("FIFO conflict")).when(stock).reverseRepairOut(anyLong(),anyString());
        assertThrows(IllegalStateException.class,()->service.reverseSettlement(1L,"TEST"));verify(costs,never()).insertCost(any());verify(receivables,never()).voidReceivable(anyLong(),nullable(String.class));verifyNoInteractions(events);
    }
    @Test void confirmedAndReversedHeadersCannotBeDeletedOrEditedThroughCrud() {
        for(String status:List.of("CONFIRMED","REVERSED")) {
            settlement.setStatus(status);
            assertThrows(ServiceException.class,()->service.deleteRepairSettlementById(1L));
            assertThrows(ServiceException.class,()->service.updateRepairSettlement(settlement));
        }
        verify(settlements,never()).updateRepairSettlement(any());verify(settlements,never()).deleteRepairSettlementByIds(any());
    }
    @Test void reasonIsMandatoryAndBoundedBeforeAnyQuery() {
        assertThrows(ServiceException.class,()->service.reverseSettlement(1L,""));
        assertThrows(ServiceException.class,()->service.reverseSettlement(1L,"x".repeat(501)));verifyNoInteractions(stock,events);
    }
    @Test void freeWarrantyZeroPaidReceivableStillReversesFifoAndRealCosts(){
        settlement.setTotalIncome(BigDecimal.ZERO);receivable.setStatus("PAID");receivable.setAmount(BigDecimal.ZERO);receivable.setArrearsAmount(BigDecimal.ZERO);
        assertEquals(1,service.reverseSettlement(1L,"TEST"));verify(stock).reverseRepairOut(2L,"TEST-ORDER");verify(receivables).voidReceivable(eq(3L),nullable(String.class));
        var reverse=ArgumentCaptor.forClass(Cost.class);verify(costs).insertCost(reverse.capture());assertEquals(0,new BigDecimal("-12.50").compareTo(reverse.getValue().getAmount()));
    }

}
