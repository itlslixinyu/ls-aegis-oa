package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.mapper.StockBatchMapper;
import com.ls.aegis.biz.psi.mapper.StockFlowMapper;
import com.ls.aegis.biz.psi.model.entity.StockBatchDO;
import com.ls.aegis.biz.psi.model.entity.StockFlowDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.continew.starter.core.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FifoAdjustmentRestoreTest {
    private final StockBatchMapper batches=mock(StockBatchMapper.class);
    private final StockFlowMapper flows=mock(StockFlowMapper.class);
    private final StockMutationLock locks=mock(StockMutationLock.class);
    private final FifoStockHelper service=new FifoStockHelper(batches,flows,mock(DocNoGenerator.class),locks);
    private StockBatchDO batch;
    @BeforeEach void original() {
        batch=new StockBatchDO(); batch.setId(10L); batch.setTenantId(1L); batch.setWarehouseId(2L); batch.setGoodsId(3L);
        batch.setTotalQuantity(new BigDecimal("10")); batch.setRemainQuantity(BigDecimal.ZERO);
        batch.setTotalAmount(new BigDecimal("1000.00")); batch.setRemainAmount(new BigDecimal("0.00")); batch.setUntaxPrice(new BigDecimal("100"));
        when(batches.selectByIdForUpdate(1L,10L)).thenReturn(batch); when(batches.selectAllForUpdate(1L,2L,3L)).thenReturn(List.of(batch));
    }
    private BigDecimal restore(String qty,String price) { return service.restoreAdjustmentBatch(1L,2L,3L,10L,new BigDecimal(qty),new BigDecimal(price),900L,"ADJ-TEST"); }
    @Test void correctsOriginalBatchWithoutCreatingReplacementAndKeepsAdjustmentAudit() {
        assertEquals(new BigDecimal("1000.00"),restore("10","100"));
        assertEquals(0,batch.getRemainQuantity().compareTo(new BigDecimal("10"))); assertEquals(new BigDecimal("1000.00"),batch.getRemainAmount());
        verify(batches,never()).insert(any(StockBatchDO.class)); verify(batches).updateById(batch);
        ArgumentCaptor<StockFlowDO> audit=ArgumentCaptor.forClass(StockFlowDO.class); verify(flows).insert(audit.capture());
        assertEquals(BizSourceTypeEnum.STOCK_ADJUST,audit.getValue().getSourceType()); assertEquals(900L,audit.getValue().getSourceId());
        assertEquals(0,audit.getValue().getBeforeQty().compareTo(BigDecimal.ZERO)); assertEquals(0,audit.getValue().getAfterQty().compareTo(new BigDecimal("10")));
    }
    @Test void cannotRestoreMoreThanOriginalMissingQuantity() { assertThrows(BusinessException.class,()->restore("11","100")); verify(batches,never()).updateById(any(StockBatchDO.class)); }
    @Test void cannotChangeOriginalPrice() { assertThrows(BusinessException.class,()->restore("1","99")); verifyNoInteractions(flows); }
    @Test void cannotRestoreOtherGoodsOrWarehouse() {
        batch.setGoodsId(4L); assertThrows(BusinessException.class,()->restore("1","100"));
        batch.setGoodsId(3L); batch.setWarehouseId(4L); assertThrows(BusinessException.class,()->restore("1","100")); verifyNoInteractions(flows);
    }
    @Test void cannotCrossTenantEvenIfAnIncorrectMapperReturnsForeignBatch() { batch.setTenantId(9L); assertThrows(BusinessException.class,()->restore("1","100")); verifyNoInteractions(flows); }
    @Test void splitCorrectionsRecoverOriginalCostCentRemainderExactly() {
        batch.setTotalQuantity(new BigDecimal("3")); batch.setTotalAmount(new BigDecimal("1.00")); batch.setUntaxPrice(new BigDecimal("0.333333"));
        BigDecimal sum=restore("1","0.333333").add(restore("1","0.333333")).add(restore("1","0.333333"));
        assertEquals(new BigDecimal("1.00"),sum); assertEquals(new BigDecimal("1.00"),batch.getRemainAmount());
        assertThrows(BusinessException.class,()->restore("1","0.333333")); verify(flows,times(3)).insert(any(StockFlowDO.class));
    }
}
