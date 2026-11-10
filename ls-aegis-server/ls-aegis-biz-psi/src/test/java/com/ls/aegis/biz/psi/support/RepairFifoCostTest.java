package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.api.RepairStockOutApiImpl;
import com.ls.aegis.biz.psi.enums.*;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.StockOutReq;
import com.ls.aegis.biz.psi.service.StockOutService;
import com.ls.aegis.common.api.psi.RepairStockOutApi;
import com.ls.aegis.common.context.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepairFifoCostTest {
    final StockOutService service=mock(StockOutService.class);
    final StockOutMapper headers=mock(StockOutMapper.class);
    final RepairStockOutInternalMapper internal=mock(RepairStockOutInternalMapper.class);
    final PsiWarehouseMapper warehouses=mock(PsiWarehouseMapper.class);
    final GoodsMapper goods=mock(GoodsMapper.class);
    final StockOutBatchMapper batches=mock(StockOutBatchMapper.class);
    final FifoStockHelper fifo=mock(FifoStockHelper.class);
    final RepairStockOutApiImpl api=new RepairStockOutApiImpl(service,headers,internal,warehouses,goods,mock(StockOutItemMapper.class),batches,fifo);
    final List<StockOutDO> chain=new ArrayList<>();
    StockOutDO head;StockOutItemDO item;StockOutBatchDO batch;
    @BeforeEach void setup(){
        var actor=new UserContext();actor.setId(99L);actor.setTenantId(0L);UserContextHolder.setContext(actor,false);
        var warehouse=new WarehouseDO();warehouse.setId(20L);warehouse.setTenantId(0L);when(warehouses.selectById(20L)).thenReturn(warehouse);when(warehouses.selectList(any())).thenReturn(List.of(warehouse));
        var product=new GoodsDO();product.setId(5L);product.setTenantId(0L);when(goods.selectById(5L)).thenReturn(product);
        when(internal.lockRepairHeads(eq(0L),eq(10L),anyString())).thenAnswer(inv->new ArrayList<>(chain));
        head=new StockOutDO();head.setId(100L);head.setTenantId(0L);head.setWarehouseId(20L);head.setRepairOrderId(10L);head.setRepairSettlementId(1L);head.setDocStatus(DocStatusEnum.CONFIRMED);head.setCostAmount(new BigDecimal("50"));
        item=new StockOutItemDO();item.setId(200L);item.setTenantId(0L);item.setStockOutId(100L);item.setGoodsId(5L);item.setQuantity(BigDecimal.ONE);item.setCostAmount(new BigDecimal("50"));
        batch=new StockOutBatchDO();batch.setTenantId(0L);batch.setStockOutItemId(200L);batch.setBatchId(300L);batch.setQuantity(BigDecimal.ONE);batch.setCostAmount(new BigDecimal("50"));
        when(internal.selectItems(0L,100L)).thenReturn(List.of(item));when(batches.selectByOutItemDesc(200L)).thenReturn(List.of(batch));
        when(service.create(any(StockOutReq.class))).thenAnswer(inv->{chain.add(head);return 100L;});when(headers.updateById(any(StockOutDO.class))).thenReturn(1);
    }
    @AfterEach void clear(){UserContextHolder.clearContext();}
    RepairStockOutApi.Cmd cmd(){var cmd=new RepairStockOutApi.Cmd();cmd.setOrderId(10L);cmd.setSettlementId(1L);cmd.setOrderNo("TEST-ORDER");var line=new RepairStockOutApi.Item();line.setGoodsId(5L);line.setQuantity(BigDecimal.ONE);line.setWarehouseId(20L);line.setTaxInclusivePrice(new BigDecimal("999"));cmd.setItems(List.of(line));return cmd;}
    @Test void newConfirmationAndIdempotentReplayReturnSameActualFifoCostNotSaleEstimate(){
        assertEquals(0,new BigDecimal("50").compareTo(api.confirmRepairOut(cmd())));assertEquals(0,new BigDecimal("50").compareTo(api.confirmRepairOut(cmd())));
        verify(service,times(1)).create(any());verify(service,times(1)).confirm(eq(100L),any());
    }
    @Test void oldVoidSettlementNeverContributesToNewActualCost(){
        var old=new StockOutDO();old.setId(90L);old.setTenantId(0L);old.setRepairOrderId(10L);old.setRepairSettlementId(99L);old.setDocStatus(DocStatusEnum.VOID);old.setCostAmount(new BigDecimal("999"));chain.add(old);
        assertEquals(0,new BigDecimal("50").compareTo(api.confirmRepairOut(cmd())));
    }
    @Test void activeDifferentSettlementCannotBeReused(){chain.add(head);head.setRepairSettlementId(2L);assertThrows(RuntimeException.class,()->api.confirmRepairOut(cmd()));verifyNoInteractions(service);}
    @Test void wrongTenantItemOrFifoBatchCannotSupplyCost(){
        chain.add(head);item.setTenantId(7L);assertThrows(RuntimeException.class,()->api.confirmRepairOut(cmd()));item.setTenantId(0L);batch.setTenantId(7L);assertThrows(RuntimeException.class,()->api.confirmRepairOut(cmd()));
    }
    @Test void nullNegativeFractionalAndOverflowActualCostsAreRejected(){
        chain.add(head);for(BigDecimal value:Arrays.asList(null,new BigDecimal("-1"),new BigDecimal("0.001"),new BigDecimal("10000000000"))){item.setCostAmount(value);assertThrows(RuntimeException.class,()->api.confirmRepairOut(cmd()));}
    }
    @Test void partialBatchOrHeadCostMismatchCannotReturnAnEstimate(){chain.add(head);batch.setCostAmount(BigDecimal.ONE);assertThrows(RuntimeException.class,()->api.confirmRepairOut(cmd()));batch.setCostAmount(new BigDecimal("50"));head.setCostAmount(BigDecimal.ONE);assertThrows(RuntimeException.class,()->api.confirmRepairOut(cmd()));}
    @Test void noMaterialsReturnsZeroButCannotHideAnExistingActiveOut(){var cmd=cmd();cmd.setItems(List.of());assertEquals(BigDecimal.ZERO,api.confirmRepairOut(cmd));chain.add(head);assertThrows(RuntimeException.class,()->api.confirmRepairOut(cmd));}
}
