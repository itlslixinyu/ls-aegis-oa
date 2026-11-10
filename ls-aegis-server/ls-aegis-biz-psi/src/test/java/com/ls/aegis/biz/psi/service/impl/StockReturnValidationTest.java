package com.ls.aegis.biz.psi.service.impl;

import com.ls.aegis.biz.psi.enums.*;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.*;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import top.continew.starter.core.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StockReturnValidationTest {
    private final StockReturnMapper returns = mock(StockReturnMapper.class);
    private final StockReturnItemMapper returnItems = mock(StockReturnItemMapper.class);
    private final StockInMapper ins = mock(StockInMapper.class);
    private final StockInItemMapper inItems = mock(StockInItemMapper.class);
    private final StockOutMapper outs = mock(StockOutMapper.class);
    private final StockOutItemMapper outItems = mock(StockOutItemMapper.class);
    private final StockOutBatchMapper consumed = mock(StockOutBatchMapper.class);
    private final IdempotentHelper idempotency = mock(IdempotentHelper.class);
    private final StockReturnServiceImpl service = new StockReturnServiceImpl(returns, returnItems, outs, outItems, consumed, ins, inItems,
        mock(PsiReceivableMapper.class), mock(PayableMapper.class), mock(com.ls.aegis.biz.psi.support.PsiTodoHelper.class),
        mock(com.ls.aegis.biz.psi.support.DocNoGenerator.class), mock(com.ls.aegis.biz.psi.support.FifoStockHelper.class), idempotency,
        mock(com.ls.aegis.biz.psi.support.WriteOffHelper.class), mock(com.ls.aegis.biz.psi.support.InvoiceStatusSupport.class), mock(CashCompensationService.class), mock(CashLedgerMapper.class));
    private StockInDO origin;
    private StockInItemDO originalItem;

    @BeforeEach void source() {
        origin = new StockInDO(); origin.setId(10L); origin.setWarehouseId(100L); origin.setUnitId(200L);
        origin.setDocStatus(DocStatusEnum.CONFIRMED); origin.setInType(StockInTypeEnum.PURCHASE);
        originalItem = new StockInItemDO(); originalItem.setId(11L); originalItem.setStockInId(10L); originalItem.setGoodsId(300L); originalItem.setQuantity(new BigDecimal("10"));
        when(ins.selectById(10L)).thenReturn(origin); when(ins.selectByIdForUpdate(10L)).thenReturn(origin);
        when(inItems.selectById(11L)).thenReturn(originalItem); when(returns.sumReturnedQty(11L)).thenReturn(BigDecimal.ZERO);
    }
    private StockReturnReq request() {
        StockReturnReq req = new StockReturnReq(); req.setReturnType(ReturnTypeEnum.PURCHASE_RETURN); req.setOriginStockInId(10L); req.setUnitId(200L); req.setWarehouseId(100L);
        StockReturnReq.StockReturnItemReq line = new StockReturnReq.StockReturnItemReq(); line.setOriginItemId(11L); line.setGoodsId(300L);
        line.setQuantity(BigDecimal.ONE); line.setTaxInclusivePrice(new BigDecimal("100")); line.setTaxRate(BigDecimal.ZERO); req.setItems(List.of(line)); return req;
    }
    private void rejected(StockReturnReq req) {
        assertThrows(BusinessException.class, () -> service.create(req)); verify(returns, never()).insert(any(StockReturnDO.class));
    }
    @Test void supplierMustMatchOriginalHeader() { StockReturnReq req=request(); req.setUnitId(201L); rejected(req); }
    @Test void sourceItemMustBelongToHeader() { originalItem.setStockInId(20L); rejected(request()); }
    @Test void goodsMustMatchOriginalItem() { StockReturnReq req=request(); req.getItems().get(0).setGoodsId(301L); rejected(req); }
    @Test void negativePriceMustFailBeforeHeaderInsert() { StockReturnReq req=request(); req.getItems().get(0).setTaxInclusivePrice(BigDecimal.ONE.negate()); rejected(req); }
    @Test void repeatedItemCannotHideAggregateOverReturn() { StockReturnReq req=request(); var line=req.getItems().get(0); line.setQuantity(new BigDecimal("6")); req.setItems(List.of(line,line)); rejected(req); }
    @Test void existingConfirmedReturnsReduceAvailableQuantity() { when(returns.sumReturnedQty(11L)).thenReturn(new BigDecimal("10")); rejected(request()); }
    @Test void nonPurchaseOriginCannotUsePurchaseReturn() { origin.setInType(StockInTypeEnum.OTHER); rejected(request()); }
    @Test void confirmationLocksOriginalAndRechecksStaleDraftQuantity() {
        StockReturnDO draft = new StockReturnDO(); draft.setId(500L); draft.setCreateUser(1L); draft.setReturnType(ReturnTypeEnum.PURCHASE_RETURN);
        draft.setDocStatus(DocStatusEnum.DRAFT); draft.setOriginStockInId(10L); draft.setWarehouseId(100L); draft.setUnitId(200L);
        StockReturnItemDO item = new StockReturnItemDO(); item.setOriginItemId(11L); item.setGoodsId(300L); item.setQuantity(new BigDecimal("5")); item.setTaxInclusivePrice(BigDecimal.ONE); item.setTaxRate(BigDecimal.ZERO);
        when(returns.selectByIdForUpdate(500L)).thenReturn(draft); when(returnItems.selectList(any())).thenReturn(List.of(item));
        when(returns.sumReturnedQty(11L)).thenReturn(new BigDecimal("6"));
        when(idempotency.execute(any(), anyString(), anyString(), any())).thenAnswer(call -> ((Supplier<String>)call.getArgument(3)).get());
        try (MockedStatic<UserContextHolder> user=mockStatic(UserContextHolder.class)) {
            user.when(UserContextHolder::getUserId).thenReturn(1L); DocConfirmReq req=new DocConfirmReq(); req.setIdempotentKey("stale");
            assertThrows(BusinessException.class, () -> service.confirm(500L,req));
        }
        verify(ins).selectByIdForUpdate(10L); verify(returns,never()).updateById(any(StockReturnDO.class));
    }
    @Test void threePartialReturnsRestoreExactOriginalCostIncludingCentRemainder() {
        StockOutItemDO original = new StockOutItemDO(); original.setQuantity(new BigDecimal("3")); when(outItems.selectById(12L)).thenReturn(original);
        StockOutBatchDO batch = new StockOutBatchDO(); batch.setBatchId(99L); batch.setQuantity(new BigDecimal("3")); batch.setCostAmount(new BigDecimal("1.00")); batch.setUntaxPrice(new BigDecimal("0.333333"));
        when(consumed.selectByOutItemDesc(12L)).thenReturn(List.of(batch)); BigDecimal total=BigDecimal.ZERO;
        for (int already=0; already<3; already++) {
            when(returns.sumReturnedQty(12L)).thenReturn(BigDecimal.valueOf(already)); StockReturnItemDO line=new StockReturnItemDO(); line.setOriginItemId(12L); line.setQuantity(BigDecimal.ONE);
            List<com.ls.aegis.biz.psi.support.FifoStockHelper.ConsumeLine> restored=ReflectionTestUtils.invokeMethod(service,"buildSaleRestoreLines",line);
            assertNotNull(restored); total=total.add(restored.get(0).costAmount());
        }
        assertEquals(new BigDecimal("1.00"), total);
    }
}
