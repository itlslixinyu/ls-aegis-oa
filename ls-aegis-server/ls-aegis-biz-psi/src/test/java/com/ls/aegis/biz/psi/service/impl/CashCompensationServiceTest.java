package com.ls.aegis.biz.psi.service.impl;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ls.aegis.biz.psi.controller.PaymentController;
import com.ls.aegis.biz.psi.controller.ReceiptController;
import com.ls.aegis.biz.psi.enums.*;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.req.*;
import com.ls.aegis.biz.psi.model.resp.WriteOffSummaryResp;
import com.ls.aegis.biz.psi.support.*;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.extension.datapermission.annotation.DataPermission;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Stateful mapper boundaries exercise monetary transitions; no datasource/API/Redis access. */
class CashCompensationServiceTest {
    private final ReceiptMapper receipts = mock(ReceiptMapper.class);
    private final PsiPaymentMapper payments = mock(PsiPaymentMapper.class);
    private final StockReturnMapper returns = mock(StockReturnMapper.class);
    private final PsiReceivableMapper receivables = mock(PsiReceivableMapper.class);
    private final PayableMapper payables = mock(PayableMapper.class);
    private final WriteOffItemMapper items = mock(WriteOffItemMapper.class);
    private final CashLedgerMapper ledger = mock(CashLedgerMapper.class);
    private final WriteOffHelper writeOff = mock(WriteOffHelper.class);
    private final DocNoGenerator docNos = mock(DocNoGenerator.class);
    private final IdempotentHelper idem = mock(IdempotentHelper.class);
    private final PsiTodoHelper todos = mock(PsiTodoHelper.class);
    private final CashCompensationService service = new CashCompensationService(receipts, payments, returns, receivables, payables, items, ledger, writeOff, docNos, idem, todos);
    private final Map<Long, ReceiptDO> receiptRows = new HashMap<>();
    private final Map<Long, PaymentDO> paymentRows = new HashMap<>();
    private StockReturnDO returned;
    private final AtomicLong generated = new AtomicLong(900);

    @BeforeAll static void metadata() {
        for (Class<?> type : List.of(ReceiptDO.class, PaymentDO.class, StockReturnDO.class, ReceivableDO.class, PayableDO.class, WriteOffItemDO.class, BankStatementDO.class)) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "cash-test");
            assistant.setCurrentNamespace("cash-test." + type.getSimpleName());
            TableInfoHelper.initTableInfo(assistant, type);
        }
    }
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        UserContext user = new UserContext(); user.setId(99L); user.setDeptId(12L);
        UserContextHolder.setContext(user, false);
        when(idem.execute(anyLong(), anyString(), anyString(), any())).thenAnswer(inv -> ((Supplier<String>)inv.getArgument(3)).get());
        when(docNos.next(anyLong(), anyString())).thenAnswer(inv -> "TEST-REFUND-" + (generated.get() + 1));
        when(ledger.bankMatches(anyLong(), anyString(), anyLong())).thenReturn(0L);
        when(ledger.sumRefundPayments(anyLong(), anyLong())).thenAnswer(inv -> paymentRows.values().stream().filter(r -> Objects.equals(r.getRefundReturnId(), inv.<Long>getArgument(1)) && r.getDocStatus() == DocStatusEnum.CONFIRMED).map(PaymentDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        when(ledger.sumRefundReceipts(anyLong(), anyLong())).thenAnswer(inv -> receiptRows.values().stream().filter(r -> Objects.equals(r.getRefundReturnId(), inv.<Long>getArgument(1)) && r.getDocStatus() == DocStatusEnum.CONFIRMED).map(ReceiptDO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        when(ledger.refundPayments(anyLong(), anyLong(), anyLong())).thenAnswer(inv -> paymentRows.values().stream().filter(r -> Objects.equals(r.getRefundSourceId(), inv.<Long>getArgument(1))).map(r -> history(r)).toList());
        when(ledger.refundReceipts(anyLong(), anyLong(), anyLong())).thenAnswer(inv -> receiptRows.values().stream().filter(r -> Objects.equals(r.getRefundSourceId(), inv.<Long>getArgument(1))).map(r -> history(r)).toList());
        when(writeOff.listSummaries(any(), any())).thenReturn(List.of());
        when(receipts.selectByIdForUpdate(anyLong())).thenAnswer(inv -> copy(receiptRows.get(inv.<Long>getArgument(0)), ReceiptDO.class));
        when(receipts.selectById(anyLong())).thenAnswer(inv -> copy(receiptRows.get(inv.<Long>getArgument(0)), ReceiptDO.class));
        when(payments.selectByIdForUpdate(anyLong())).thenAnswer(inv -> copy(paymentRows.get(inv.<Long>getArgument(0)), PaymentDO.class));
        when(payments.selectById(anyLong())).thenAnswer(inv -> copy(paymentRows.get(inv.<Long>getArgument(0)), PaymentDO.class));
        when(receipts.updateById(any(ReceiptDO.class))).thenAnswer(inv -> { ReceiptDO r = inv.getArgument(0); receiptRows.put(r.getId(), copy(r, ReceiptDO.class)); return 1; });
        when(payments.updateById(any(PaymentDO.class))).thenAnswer(inv -> { PaymentDO r = inv.getArgument(0); paymentRows.put(r.getId(), copy(r, PaymentDO.class)); return 1; });
        when(receipts.insert(any(ReceiptDO.class))).thenAnswer(inv -> { ReceiptDO r = inv.getArgument(0); r.setId(generated.incrementAndGet()); receiptRows.put(r.getId(), copy(r, ReceiptDO.class)); return 1; });
        when(payments.insert(any(PaymentDO.class))).thenAnswer(inv -> { PaymentDO r = inv.getArgument(0); r.setId(generated.incrementAndGet()); paymentRows.put(r.getId(), copy(r, PaymentDO.class)); return 1; });
        when(receipts.selectList(any())).thenAnswer(inv -> receiptRows.values().stream().filter(r -> r.getRefundReturnId() != null && r.getRefundReturnId().equals(500L) && r.getDocStatus() == DocStatusEnum.CONFIRMED).toList());
        when(payments.selectList(any())).thenAnswer(inv -> paymentRows.values().stream().filter(r -> r.getRefundReturnId() != null && r.getRefundReturnId().equals(500L) && r.getDocStatus() == DocStatusEnum.CONFIRMED).toList());
        ReceivableDO ar = new ReceivableDO(); ar.setId(600L); ar.setTenantId(0L); ar.setUnitId(10L); ar.setSourceId(700L); ar.setSourceType(BizSourceTypeEnum.STOCK_OUT);
        when(ledger.receivableForOrigin(0L,700L)).thenReturn(ar); when(ledger.receivableForInvariant(0L,600L)).thenReturn(ar); when(ledger.lockReturnReceivable(0L,700L)).thenReturn(ar);
        PayableDO ap = new PayableDO(); ap.setId(601L); ap.setTenantId(0L); ap.setUnitId(10L); ap.setSourceId(701L); ap.setSourceType(BizSourceTypeEnum.STOCK_IN);
        when(ledger.payableForOrigin(0L,701L)).thenReturn(ap); when(ledger.payableForInvariant(0L,601L)).thenReturn(ap); when(ledger.lockReturnPayable(0L,701L)).thenReturn(ap);
        when(ledger.receiptForInvariant(anyLong(),anyLong())).thenAnswer(inv -> copy(receiptRows.get(inv.<Long>getArgument(1)),ReceiptDO.class));
        when(ledger.paymentForInvariant(anyLong(),anyLong())).thenAnswer(inv -> copy(paymentRows.get(inv.<Long>getArgument(1)),PaymentDO.class));
        when(ledger.returnsForOrigin(anyLong(),anyBoolean(),anyLong())).thenAnswer(inv -> returned != null && returned.getReturnType() == (inv.<Boolean>getArgument(1)?ReturnTypeEnum.SALE_RETURN:ReturnTypeEnum.PURCHASE_RETURN) ? List.of(returned) : List.of());
        when(ledger.setRefundTodo(anyLong(),anyLong(),anyString(),anyBoolean(),anyLong())).thenReturn(1);
        when(ledger.allocationItems(anyLong(), anyBoolean(), nullable(Long.class), nullable(Long.class))).thenReturn(List.of(event(1, WriteOffDirectionEnum.WRITE_OFF, "100"), event(2, WriteOffDirectionEnum.REVERSE, "100")));
        returned = new StockReturnDO(); returned.setId(500L); returned.setTenantId(0L); returned.setUnitId(10L);
        returned.setDocNo("TEST-RETURN"); returned.setDocStatus(DocStatusEnum.CONFIRMED); returned.setReturnType(ReturnTypeEnum.SALE_RETURN);
        returned.setOriginStockOutId(700L); returned.setOriginStockInId(701L); returned.setTaxInclusiveAmount(new BigDecimal("100"));
        when(returns.selectByIdForUpdate(500L)).thenAnswer(inv -> returned);
        when(returns.selectById(500L)).thenAnswer(inv -> returned);
    }
    @AfterEach void cleanup() { UserContextHolder.clearContext(); }

    @Test void salesRefundCreatesOppositeCashAndConservesOriginalCredit() {
        seed(true, "100", "100");
        Long refundId = service.refund(true, 1L, refund("100"));
        ReceiptDO original = receiptRows.get(1L); PaymentDO opposite = paymentRows.get(refundId);
        money("0", original.getUnallocatedAmount()); money("100", original.getRefundedAmount());
        assertEquals(DocStatusEnum.CONFIRMED, opposite.getDocStatus());
        assertEquals(1L, opposite.getRefundSourceId()); assertEquals(500L, opposite.getRefundReturnId());
        assertEquals(original.getUnitId(), opposite.getUnitId()); assertEquals(original.getAccountId(), opposite.getAccountId());
        money("100", opposite.getAmount()); money("0", opposite.getUnallocatedAmount());
        assertNotNull(opposite.getPaymentDate()); assertEquals(99L, opposite.getConfirmUser());
        money("0", original.getAmount().subtract(opposite.getAmount()));
        verify(ledger).setRefundTodo(0L, 500L, "REFUND", true, 99L);
    }

    @Test void purchaseRefundCreatesReceiptWithOriginalAccountAndUnit() {
        seed(false, "100", "100"); returned.setReturnType(ReturnTypeEnum.PURCHASE_RETURN);
        WriteOffItemDO allocated = event(1, WriteOffDirectionEnum.WRITE_OFF, "100"), reversed = event(2, WriteOffDirectionEnum.REVERSE, "100");
        for (WriteOffItemDO row : List.of(allocated, reversed)) { row.setReceiptId(null); row.setPaymentId(1L); row.setOpenId(601L); row.setOpenType(WriteOffOpenTypeEnum.AP); }
        when(ledger.allocationItems(anyLong(), anyBoolean(), nullable(Long.class), nullable(Long.class))).thenReturn(List.of(allocated, reversed));
        Long id = service.refund(false, 1L, refund("25.50"));
        PaymentDO original = paymentRows.get(1L); ReceiptDO opposite = receiptRows.get(id);
        money("74.50", original.getUnallocatedAmount()); money("25.50", original.getRefundedAmount());
        money("25.50", opposite.getAmount()); assertEquals(1L, opposite.getRefundSourceId());
        assertNotNull(opposite.getReceiptDate()); verify(ledger, never()).setRefundTodo(anyLong(), anyLong(), anyString(), eq(true), anyLong());
    }

    @Test void multiplePartialRefundsCannotExceedReturnAmount() {
        seed(true, "150", "150");
        service.refund(true, 1L, refund("60")); service.refund(true, 1L, refund("40"));
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("0.01")));
        money("50", receiptRows.get(1L).getUnallocatedAmount()); money("100", receiptRows.get(1L).getRefundedAmount());
        assertEquals(2, paymentRows.size());
    }

    @Test void reverseRefundRestoresSourceCreditAndRetainsVoidAudit() {
        seed(true, "100", "100"); Long id = service.refund(true, 1L, refund("100"));
        assertEquals(paymentRows.get(id).getDocNo(), service.reverseRefund(false, id, action()));
        ReceiptDO original = receiptRows.get(1L); PaymentDO opposite = paymentRows.get(id);
        money("100", original.getUnallocatedAmount()); money("0", original.getRefundedAmount());
        assertEquals(DocStatusEnum.VOID, opposite.getDocStatus()); assertEquals("synthetic correction", opposite.getVoidReason());
        assertNotNull(opposite.getVoidTime()); assertEquals(99L, opposite.getVoidUser());
        verify(ledger).setRefundTodo(0L, 500L, "REFUND", false, 99L);
        service.reverseRefund(false, id, action());
        money("100", receiptRows.get(1L).getUnallocatedAmount());
    }

    @Test void reversedRefundDoesNotConsumeReturnCapacityAndMayBeRegisteredAgain() {
        seed(true, "100", "100"); Long first = service.refund(true, 1L, refund("100"));
        service.reverseRefund(false, first, action());
        Long second = service.refund(true, 1L, refund("100"));
        assertNotEquals(first, second); assertEquals(DocStatusEnum.VOID, paymentRows.get(first).getDocStatus());
        assertEquals(DocStatusEnum.CONFIRMED, paymentRows.get(second).getDocStatus());
        money("100", receiptRows.get(1L).getRefundedAmount()); money("0", receiptRows.get(1L).getUnallocatedAmount());
    }

    @Test void voidRequiresNoAllocationsRefundsOrBankMatch() {
        seed(true, "100", "90"); assertThrows(BusinessException.class, () -> service.voidCash(true, 1L, action()));
        seed(true, "100", "100"); receiptRows.get(1L).setRefundedAmount(BigDecimal.ONE);
        assertThrows(BusinessException.class, () -> service.voidCash(true, 1L, action()));
        receiptRows.get(1L).setRefundedAmount(BigDecimal.ZERO); when(writeOff.listSummaries(any(), any())).thenReturn(List.of(new WriteOffSummaryResp()));
        assertThrows(BusinessException.class, () -> service.voidCash(true, 1L, action()));
        when(writeOff.listSummaries(any(), any())).thenReturn(List.of()); when(ledger.bankMatches(anyLong(), anyString(), anyLong())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.voidCash(true, 1L, action()));
        assertEquals(DocStatusEnum.CONFIRMED, receiptRows.get(1L).getDocStatus());
    }

    @Test void voidBothNormalDirectionsPreservesAmountAndIsRepeatable() {
        seed(true, "100", "100"); seed(false, "100", "100");
        for (boolean receipt : new boolean[]{true, false}) {
            service.voidCash(receipt, 1L, action()); service.voidCash(receipt, 1L, action());
            var value = receipt ? BeanUtil.copyProperties(receiptRows.get(1L), CashCompensationService.Cash.class) : BeanUtil.copyProperties(paymentRows.get(1L), CashCompensationService.Cash.class);
            assertEquals(DocStatusEnum.VOID, value.getDocStatus()); money("100", value.getAmount()); money("0", value.getUnallocatedAmount());
        }
    }

    @Test void rejectWrongReturnDirectionTenantUnitOrStateBeforeWriting() {
        seed(true, "100", "100"); returned.setReturnType(ReturnTypeEnum.PURCHASE_RETURN);
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("1")));
        returned.setReturnType(ReturnTypeEnum.SALE_RETURN); returned.setTenantId(5L);
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("1")));
        returned.setTenantId(0L); returned.setUnitId(99L);
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("1")));
        returned.setUnitId(10L); returned.setDocStatus(DocStatusEnum.DRAFT);
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("1")));
        assertTrue(paymentRows.isEmpty()); money("100", receiptRows.get(1L).getUnallocatedAmount());
    }

    @Test void rejectUnrelatedCashAndInsufficientCreditAndIllegalAmount() {
        seed(true, "100", "10");
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("11")));
        when(ledger.allocationItems(anyLong(), anyBoolean(), nullable(Long.class), nullable(Long.class))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("1")));
        for (String amount : List.of("0", "-1", "0.001")) assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund(amount)));
        assertTrue(paymentRows.isEmpty());
    }

    @Test void refundRowsCannotBeReusedAsNormalSourceAndMatchedRefundCannotReverse() {
        seed(true, "100", "100"); Long id = service.refund(true, 1L, refund("100"));
        assertThrows(BusinessException.class, () -> service.voidCash(false, id, action()));
        assertThrows(BusinessException.class, () -> service.refund(false, id, refund("1")));
        when(ledger.bankMatches(anyLong(), anyString(), anyLong())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.reverseRefund(false, id, action()));
        money("0", receiptRows.get(1L).getUnallocatedAmount()); assertEquals(DocStatusEnum.CONFIRMED, paymentRows.get(id).getDocStatus());
    }

    @Test void sourceAuthorizationAndTransactionAnnotationsRemainPresent() throws Exception {
        assertThrows(BusinessException.class, () -> service.voidCash(true, 999L, action()));
        for (Class<?> type : List.of(ReceiptMapper.class, PsiPaymentMapper.class, StockReturnMapper.class))
            assertNotNull(type.getMethod("selectByIdForUpdate", Long.class).getAnnotation(DataPermission.class));
        assertEquals(org.springframework.transaction.annotation.Isolation.READ_COMMITTED, CashCompensationService.class.getMethod("refund", boolean.class, Long.class, CashRefundReq.class).getAnnotation(Transactional.class).isolation());
        assertNotNull(CashCompensationService.class.getMethod("voidCash", boolean.class, Long.class, CashActionReq.class).getAnnotation(Transactional.class));
        assertEquals(org.springframework.transaction.annotation.Isolation.READ_COMMITTED, CashCompensationService.class.getMethod("reverseRefund", boolean.class, Long.class, CashActionReq.class).getAnnotation(Transactional.class).isolation());
        assertPermissions(ReceiptController.class, "refund", CashRefundReq.class, "psi:receipt:writeoff", "psi:payment:confirm");
        assertPermissions(PaymentController.class, "refund", CashRefundReq.class, "psi:payment:writeoff", "psi:receipt:confirm");
        assertPermissions(ReceiptController.class, "reverseRefund", CashActionReq.class, "psi:receipt:confirm", "psi:payment:writeoff");
        assertPermissions(PaymentController.class, "reverseRefund", CashActionReq.class, "psi:payment:confirm", "psi:receipt:writeoff");
    }

    @Test void historicalAllocationCyclesDoNotMultiplyRefundAttribution() {
        seed(true, "200", "200"); receiptRows.get(1L).setOperationSequence(4L);
        when(ledger.allocationItems(anyLong(), anyBoolean(), nullable(Long.class), nullable(Long.class))).thenReturn(List.of(event(1, WriteOffDirectionEnum.WRITE_OFF, "10"), event(2, WriteOffDirectionEnum.REVERSE, "10"), event(3, WriteOffDirectionEnum.WRITE_OFF, "10"), event(4, WriteOffDirectionEnum.REVERSE, "10")));
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("10.01")));
        service.refund(true, 1L, refund("10"));
        money("190", receiptRows.get(1L).getUnallocatedAmount());
    }

    @Test void creditFromAnotherOriginCannotFundThisReturnsRefund() {
        seed(true, "200", "200");
        when(ledger.allocationItems(anyLong(), anyBoolean(), nullable(Long.class), nullable(Long.class))).thenReturn(List.of(event(1, WriteOffDirectionEnum.WRITE_OFF, "10"), event(2, WriteOffDirectionEnum.REVERSE, "10")));
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("100")));
        assertTrue(paymentRows.isEmpty()); money("200", receiptRows.get(1L).getUnallocatedAmount());
    }

    @Test void additionalAllocationAfterRefundEstablishesOnlyTheNewRemainingBudget() {
        seed(true, "200", "200"); returned.setTaxInclusiveAmount(new BigDecimal("200"));
        service.refund(true, 1L, refund("100"));
        receiptRows.get(1L).setOperationSequence(5L);
        when(ledger.allocationItems(anyLong(), anyBoolean(), nullable(Long.class), nullable(Long.class))).thenReturn(List.of(event(1, WriteOffDirectionEnum.WRITE_OFF, "100"), event(2, WriteOffDirectionEnum.REVERSE, "100"), event(4, WriteOffDirectionEnum.WRITE_OFF, "100"), event(5, WriteOffDirectionEnum.REVERSE, "100")));
        service.refund(true, 1L, refund("100"));
        money("0", receiptRows.get(1L).getUnallocatedAmount()); money("200", receiptRows.get(1L).getRefundedAmount());
        assertEquals(2, paymentRows.size());
    }

    @Test void missingLegacySequenceRejectsRefundWithoutGuessingHistory() {
        seed(true, "100", "100"); WriteOffItemDO legacy = event(1, WriteOffDirectionEnum.WRITE_OFF, "100"); legacy.setCashSequence(null);
        when(ledger.allocationItems(anyLong(), anyBoolean(), nullable(Long.class), nullable(Long.class))).thenReturn(List.of(legacy));
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("1")));
        assertTrue(paymentRows.isEmpty()); money("100", receiptRows.get(1L).getUnallocatedAmount());
    }

    @Test void hiddenOtherOperatorsRefundStillReducesGlobalReturnCapacity() {
        seed(true, "100", "100"); PaymentDO hidden = new PaymentDO(); hidden.setId(800L); hidden.setTenantId(0L); hidden.setDeptId(999L);
        hidden.setAmount(new BigDecimal("80")); hidden.setRefundReturnId(500L); hidden.setRefundSourceId(2L); hidden.setDocStatus(DocStatusEnum.CONFIRMED);
        paymentRows.put(800L, hidden);
        when(payments.selectList(any())).thenReturn(List.of()); // The operator list cannot see the other department's refund.
        assertThrows(BusinessException.class, () -> service.refund(true, 1L, refund("20.01")));
        service.refund(true, 1L, refund("20"));
        money("80", receiptRows.get(1L).getUnallocatedAmount());
        verify(ledger, atLeastOnce()).sumRefundPayments(0L, 500L);
    }

    @Test void hiddenBankMatchBlocksVoidAndReverseRegardlessOfVisibleBankList() {
        seed(true, "100", "100"); when(ledger.bankMatches(0L, "RECEIPT", 1L)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.voidCash(true, 1L, action()));
        when(ledger.bankMatches(0L, "RECEIPT", 1L)).thenReturn(0L);
        Long id = service.refund(true, 1L, refund("100"));
        when(ledger.bankMatches(0L, "PAYMENT", id)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.reverseRefund(false, id, action()));
        assertEquals(DocStatusEnum.CONFIRMED, paymentRows.get(id).getDocStatus());
    }

    @Test void authorizedSourceTenantControlsGlobalBankInvariantInsteadOfOperatorContext() {
        seed(true, "100", "100"); receiptRows.get(1L).setTenantId(5L);
        when(ledger.bankMatches(5L, "RECEIPT", 1L)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.voidCash(true, 1L, action()));
        assertEquals(DocStatusEnum.CONFIRMED, receiptRows.get(1L).getDocStatus());
        verify(ledger).bankMatches(5L, "RECEIPT", 1L);
    }

    private static WriteOffItemDO event(long sequence, WriteOffDirectionEnum direction, String amount) {
        WriteOffItemDO row = new WriteOffItemDO(); row.setId(1000L + sequence); row.setTenantId(0L); row.setReceiptId(1L); row.setOpenId(600L); row.setOpenType(WriteOffOpenTypeEnum.AR);
        row.setCashSequence(sequence); row.setDirection(direction); row.setAmount(new BigDecimal(amount)); return row;
    }
    private static CashLedgerMapper.RefundHistory history(Object value) {
        CashLedgerMapper.RefundHistory history = BeanUtil.copyProperties(value, CashLedgerMapper.RefundHistory.class);
        history.setDocStatus(value instanceof ReceiptDO r ? r.getDocStatus().name() : ((PaymentDO)value).getDocStatus().name());
        return history;
    }

    private static void assertPermissions(Class<?> type, String method, Class<?> req, String... permissions) throws Exception {
        SaCheckPermission permission = type.getMethod(method, Long.class, req).getAnnotation(SaCheckPermission.class);
        assertNotNull(permission); assertEquals(SaMode.AND, permission.mode()); assertEquals(Set.of(permissions), Set.of(permission.value()));
    }
    private void seed(boolean receipt, String amount, String unallocated) {
        CashCompensationService.Cash cash = new CashCompensationService.Cash();
        cash.setId(1L); cash.setTenantId(0L); cash.setUnitId(10L); cash.setAccountId(20L); cash.setDeptId(12L);
        cash.setAmount(new BigDecimal(amount)); cash.setUnallocatedAmount(new BigDecimal(unallocated)); cash.setRefundedAmount(BigDecimal.ZERO);
        cash.setDocNo("TEST-ORIGINAL"); cash.setDocStatus(DocStatusEnum.CONFIRMED);
        if (receipt) { ReceiptDO row = BeanUtil.copyProperties(cash, ReceiptDO.class); row.setOperationSequence(2L); receiptRows.put(1L, row); }
        else { PaymentDO row = BeanUtil.copyProperties(cash, PaymentDO.class); row.setOperationSequence(2L); paymentRows.put(1L, row); }
    }
    private static CashActionReq action() { CashActionReq r = new CashActionReq(); r.setReason("synthetic correction"); r.setIdempotentKey("test-key-" + UUID.randomUUID()); return r; }
    private static CashRefundReq refund(String amount) { CashRefundReq r = new CashRefundReq(); r.setReason("synthetic correction"); r.setIdempotentKey("test-key-" + UUID.randomUUID()); r.setStockReturnId(500L); r.setAmount(new BigDecimal(amount)); return r; }
    private static <T> T copy(Object row, Class<T> type) { return row == null ? null : BeanUtil.copyProperties(row, type); }
    private static void money(String expected, BigDecimal actual) { assertNotNull(actual); assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
}
