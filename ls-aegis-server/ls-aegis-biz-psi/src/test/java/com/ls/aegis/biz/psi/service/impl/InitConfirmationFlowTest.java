package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ls.aegis.biz.psi.enums.*;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.support.*;
import com.ls.aegis.common.context.*;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Controlled service-level flow; mapper/fifo doubles do not claim live opening-balance confirmation. */
@ExtendWith(MockitoExtension.class)
class InitConfirmationFlowTest {
    @Mock InitStockMapper stocks;@Mock InitStockItemMapper stockItems;
    @Mock InitReceivableMapper ars;@Mock InitReceivableItemMapper arItems;
    @Mock InitPayableMapper aps;@Mock InitPayableItemMapper apItems;
    @Mock InitPrepayMapper prepays;@Mock InitPrepayItemMapper prepayItems;
    @Mock PsiReceivableMapper receivables;@Mock PayableMapper payables;@Mock PsiPaymentMapper payments;
    @Mock DocNoGenerator numbers;@Mock FifoStockHelper fifo;@Mock IdempotentHelper idempotency;
    @InjectMocks InitDocServiceImpl service;
    BigDecimal amount=new BigDecimal("50.00");
    DocConfirmReq req(){var r=new DocConfirmReq();r.setIdempotentKey("synthetic-key");return r;}
    @BeforeEach void setup(){var actor=new UserContext();actor.setId(1L);actor.setDeptId(1L);actor.setTenantId(0L);UserContextHolder.setContext(actor);when(idempotency.execute(anyLong(),anyString(),anyString(),any())).thenAnswer(i->((Supplier<?>)i.getArgument(3)).get());}
    @AfterEach void clear(){UserContextHolder.clearContext();}
    @Test @SuppressWarnings("unchecked") void stockConfirmationUsesFifoOnce(){
        var doc=new InitStockDO();doc.setId(1L);doc.setTenantId(0L);doc.setCreateUser(1L);doc.setWarehouseId(2L);doc.setDocNo("INIT-STOCK");doc.setInitDate(LocalDateTime.now());doc.setDocStatus(DocStatusEnum.DRAFT);when(stocks.selectByIdForUpdate(1L)).thenReturn(doc);
        var line=new InitStockItemDO();line.setGoodsId(3L);line.setQuantity(new BigDecimal("2"));line.setUntaxPrice(new BigDecimal("25"));when(stockItems.selectList(any(Wrapper.class))).thenReturn(List.of(line));
        assertEquals("INIT-STOCK",service.confirm("STOCK",1L,req()));service.confirm("STOCK",1L,req());
        verify(fifo,times(1)).inbound(eq(0L),eq(2L),eq(3L),eq(new BigDecimal("2")),eq(new BigDecimal("25")),eq(amount),eq(doc.getInitDate()),eq(BizSourceTypeEnum.INIT_STOCK),eq(1L),eq("INIT-STOCK"));assertEquals(DocStatusEnum.CONFIRMED,doc.getDocStatus());
    }
    @Test @SuppressWarnings("unchecked") void receivableConfirmationCreatesOnlyOpenAmount(){
        var doc=new InitReceivableDO();doc.setId(1L);doc.setCreateUser(1L);doc.setDocNo("INIT-AR");doc.setDocStatus(DocStatusEnum.DRAFT);when(ars.selectByIdForUpdate(1L)).thenReturn(doc);var line=new InitReceivableItemDO();line.setUnitId(2L);line.setTaxInclusiveAmount(amount);when(arItems.selectList(any(Wrapper.class))).thenReturn(List.of(line));
        service.confirm("AR",1L,req());service.confirm("AR",1L,req());var c=ArgumentCaptor.forClass(ReceivableDO.class);verify(receivables,times(1)).insert(c.capture());assertEquals(amount,c.getValue().getUnwrittenAmount());assertEquals(BigDecimal.ZERO,c.getValue().getWrittenOffAmount());assertEquals(BizSourceTypeEnum.INIT_AR,c.getValue().getSourceType());verifyNoInteractions(payments);
    }
    @Test @SuppressWarnings("unchecked") void payableConfirmationCreatesOnlyOpenAmount(){
        var doc=new InitPayableDO();doc.setId(1L);doc.setCreateUser(1L);doc.setDocNo("INIT-AP");doc.setDocStatus(DocStatusEnum.DRAFT);when(aps.selectByIdForUpdate(1L)).thenReturn(doc);var line=new InitPayableItemDO();line.setUnitId(2L);line.setTaxInclusiveAmount(amount);when(apItems.selectList(any(Wrapper.class))).thenReturn(List.of(line));
        service.confirm("AP",1L,req());service.confirm("AP",1L,req());var c=ArgumentCaptor.forClass(PayableDO.class);verify(payables,times(1)).insert(c.capture());assertEquals(amount,c.getValue().getUnwrittenAmount());assertEquals(BigDecimal.ZERO,c.getValue().getWrittenOffAmount());assertEquals(BizSourceTypeEnum.INIT_AP,c.getValue().getSourceType());verifyNoInteractions(payments);
    }
    @Test @SuppressWarnings("unchecked") void prepaymentConfirmationCreatesOneOpeningCredit(){
        var doc=new InitPrepayDO();doc.setId(1L);doc.setCreateUser(1L);doc.setDocNo("INIT-PREPAY");doc.setDocStatus(DocStatusEnum.DRAFT);when(prepays.selectByIdForUpdate(1L)).thenReturn(doc);var line=new InitPrepayItemDO();line.setUnitId(2L);line.setTaxInclusiveAmount(amount);when(prepayItems.selectList(any(Wrapper.class))).thenReturn(List.of(line));when(numbers.next(anyLong(),anyString())).thenReturn("TEST-PAYMENT");
        service.confirm("PREPAY",1L,req());service.confirm("PREPAY",1L,req());var c=ArgumentCaptor.forClass(PaymentDO.class);verify(payments,times(1)).insert(c.capture());assertEquals(amount,c.getValue().getAmount());assertEquals(amount,c.getValue().getUnallocatedAmount());assertEquals("INIT_PREPAY",c.getValue().getPayMethod());assertEquals(DocStatusEnum.CONFIRMED,c.getValue().getDocStatus());
    }
}
