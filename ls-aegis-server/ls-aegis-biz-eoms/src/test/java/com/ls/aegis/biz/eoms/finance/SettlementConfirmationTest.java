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

class SettlementConfirmationTest {
    final RepairSettlementMapper settlements=mock(RepairSettlementMapper.class);
    final RepairOrderMapper orders=mock(RepairOrderMapper.class);
    final RepairMaterialMapper materials=mock(RepairMaterialMapper.class);
    final ReceivableMapper receivables=mock(ReceivableMapper.class);
    final CostMapper costs=mock(CostMapper.class);
    final IRepairOrderService orderService=mock(IRepairOrderService.class);
    final RepairStockOutApi stock=mock(RepairStockOutApi.class);
    final RepairSettlementServiceImpl service=spy(new RepairSettlementServiceImpl());
    RepairOrder order;
    RepairSettlement settlement(long id) {var s=new RepairSettlement();s.setId(id);s.setRepairOrderId(10L);s.setStatus("DRAFT");s.setSettlementNo("TEST-"+id);s.setServiceFee(new BigDecimal("100"));s.setLaborCost(new BigDecimal("20"));return s;}
    @BeforeEach void setup() {
        for(Object[] pair:new Object[][]{{"repairSettlementMapper",settlements},{"repairOrderMapper",orders},{"repairMaterialMapper",materials},{"receivableMapper",receivables},{"costMapper",costs},{"repairOrderService",orderService},{"repairStockOutApi",stock}})
            ReflectionTestUtils.setField(service,(String)pair[0],pair[1]);
        var numbers=mock(com.ls.aegis.biz.eoms.utils.EomsNoGenerator.class);when(numbers.generateReceivableNo()).thenReturn("TEST-YS");ReflectionTestUtils.setField(service,"eomsNoGenerator",numbers);
        order=new RepairOrder();order.setId(10L);order.setCustomerId(20L);order.setStatus("WAIT_SETTLEMENT");order.setOrderNo("TEST-ORDER");
        when(stock.confirmRepairOut(any())).thenReturn(BigDecimal.ZERO);
        when(orders.lockOrder(10L)).thenReturn(order);when(materials.lockByOrder(10L)).thenReturn(List.of());when(receivables.lockByOrder(10L)).thenReturn(List.of());
        for(long id:List.of(1L,2L)) {var s=settlement(id);doReturn(s).when(service).selectRepairSettlementById(id);when(settlements.lockSettlement(id)).thenReturn(s);}
        when(settlements.casConfirmSettlement(anyLong(),anyString(),anyString(),anyString(),any(),any(),any(),any(),any(),any(),any(),any(),nullable(String.class),nullable(String.class))).thenReturn(1);
        doAnswer(inv->{order.setStatus("FINISHED");return 1;}).when(orderService).finishSettlement(eq(10L),nullable(String.class),nullable(String.class));
    }
    @Test void confirmationLinksStockCostAndReceivableToTheExactSettlementRatherThanOrderOnly() {
        assertEquals(1,service.confirmSettlement(1L,null));
        var cmd=ArgumentCaptor.forClass(RepairStockOutApi.Cmd.class);verify(stock).confirmRepairOut(cmd.capture());assertEquals(1L,cmd.getValue().getSettlementId());
        var r=ArgumentCaptor.forClass(Receivable.class);verify(receivables).insertReceivable(r.capture());assertEquals(1L,r.getValue().getSettlementId());assertEquals(0,r.getValue().getAmount().compareTo(new BigDecimal("100")));
        var c=ArgumentCaptor.forClass(Cost.class);verify(costs).insertCost(c.capture());assertEquals(1L,c.getValue().getSettlementId());
    }
    @Test void secondDraftForSameOrderCannotGenerateAnotherStockOutOrReceivable() {
        service.confirmSettlement(1L,null);
        assertThrows(ServiceException.class,()->service.confirmSettlement(2L,null));
        verify(stock,times(1)).confirmRepairOut(any());verify(receivables,times(1)).insertReceivable(any());verify(costs,times(1)).insertCost(any());
    }
    @Test void orderMustBeLockedBeforeReadingMaterialsToPreventSnapshotRaceWithMaterialEdits() {
        service.confirmSettlement(1L,null);
        var sequence=inOrder(orders,materials,stock);sequence.verify(orders).lockOrder(10L);sequence.verify(materials).lockByOrder(10L);sequence.verify(stock).confirmRepairOut(any());
        verify(materials,never()).selectRepairMaterialList(any());
    }
    @Test void activeReceivableConflictFailsRatherThanSilentlyReusingUnrelatedLedger() {
        when(receivables.lockByOrder(10L)).thenReturn(List.of(new Receivable()));
        assertThrows(ServiceException.class,()->service.confirmSettlement(1L,null));verify(receivables,never()).insertReceivable(any());
    }
    @Test void editableMaterialEstimateNeverBecomesAccountingCostAndPsiPrecedesCas() {
        var material=new RepairMaterial();material.setMaterialId(5L);material.setQuantity(BigDecimal.ONE);material.setCostPrice(new BigDecimal("999"));material.setCostAmount(new BigDecimal("999"));material.setSalePrice(new BigDecimal("150"));material.setSaleAmount(new BigDecimal("150"));
        when(materials.lockByOrder(10L)).thenReturn(List.of(material));when(stock.confirmRepairOut(any())).thenReturn(new BigDecimal("50"));
        var s=settlements.lockSettlement(1L);s.setLaborCost(BigDecimal.TEN);
        service.confirmSettlement(1L,null);
        assertEquals(0,new BigDecimal("50").compareTo(s.getMaterialCost()));assertEquals(0,new BigDecimal("60").compareTo(s.getTotalCost()));assertEquals(0,new BigDecimal("190").compareTo(s.getProfitAmount()));
        var recorded=ArgumentCaptor.forClass(Cost.class);verify(costs,times(2)).insertCost(recorded.capture());
        assertTrue(recorded.getAllValues().stream().anyMatch(c->c.getAmount().compareTo(new BigDecimal("50"))==0));
        var sequence=inOrder(stock,settlements,orderService);sequence.verify(stock).confirmRepairOut(any());
        sequence.verify(settlements).casConfirmSettlement(anyLong(),anyString(),anyString(),anyString(),any(),any(),any(),any(),eq(new BigDecimal("50")),any(),eq(new BigDecimal("60")),eq(new BigDecimal("190")),nullable(String.class),nullable(String.class));
        sequence.verify(orderService).finishSettlement(eq(10L),nullable(String.class),nullable(String.class));
    }
    @Test void invalidActualCostsRollBackPsiWritesBeforeAnySettlementOrFinanceWrites() {
        var ds=new org.h2.jdbcx.JdbcDataSource();ds.setURL("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
        var db=new org.springframework.jdbc.core.JdbcTemplate(ds);db.execute("create table fifo_audit(id int primary key, amount decimal(12,2))");
        var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds));
        try {
            for(BigDecimal actual:java.util.Arrays.asList(null,new BigDecimal("-1"),new BigDecimal("10000000000"),new BigDecimal("9999999999.99"),new BigDecimal("0.001"))) {
                doAnswer(inv->{db.update("insert into fifo_audit values(1,50)");return actual;}).when(stock).confirmRepairOut(any());
                assertThrows(ServiceException.class,()->tx.execute(status->service.confirmSettlement(1L,null)));
                assertEquals(0,db.queryForObject("select count(*) from fifo_audit",Integer.class));
                assertEquals("DRAFT",settlements.lockSettlement(1L).getStatus());assertEquals("WAIT_SETTLEMENT",order.getStatus());
            }
            verify(settlements,never()).casConfirmSettlement(anyLong(),anyString(),anyString(),anyString(),any(),any(),any(),any(),any(),any(),any(),any(),nullable(String.class),nullable(String.class));
            verify(orderService,never()).finishSettlement(anyLong(),nullable(String.class),nullable(String.class));verifyNoInteractions(costs);verify(receivables,never()).insertReceivable(any());
        } finally {db.execute("SHUTDOWN");}
    }
    @Test void totalCostOverflowCannotFinishOrBookFinance() {
        when(stock.confirmRepairOut(any())).thenReturn(new BigDecimal("9999999999.99"));
        assertThrows(ServiceException.class,()->service.confirmSettlement(1L,null));verify(orderService,never()).finishSettlement(anyLong(),nullable(String.class),nullable(String.class));
    }

    @Test void legitimateZeroIncomeSettlementCreatesPaidZeroReceivableNotPendingCollection() {
        var s=settlements.lockSettlement(1L);s.setServiceFee(BigDecimal.ZERO);s.setLaborCost(BigDecimal.ZERO);
        assertEquals(1,service.confirmSettlement(1L,null));
        var saved=ArgumentCaptor.forClass(Receivable.class);verify(receivables).insertReceivable(saved.capture());
        assertEquals("PAID",saved.getValue().getStatus());assertEquals(0,saved.getValue().getAmount().signum());assertEquals(0,saved.getValue().getArrearsAmount().signum());
    }

}
