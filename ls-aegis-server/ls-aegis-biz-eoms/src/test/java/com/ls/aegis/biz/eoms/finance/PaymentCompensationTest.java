package com.ls.aegis.biz.eoms.finance;

import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.finance.*;
import com.ls.aegis.biz.eoms.service.impl.finance.PaymentServiceImpl;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentCompensationTest {
    final PaymentMapper payments = mock(PaymentMapper.class);
    final ReceivableMapper receivables = mock(ReceivableMapper.class);
    final IncomeMapper incomes = mock(IncomeMapper.class);
    final FinanceEventMapper events = mock(FinanceEventMapper.class);
    final EomsNoGenerator numbers = mock(EomsNoGenerator.class);
    final PaymentServiceImpl service = spy(new PaymentServiceImpl());
    Payment payment;
    Income income;

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"paymentMapper",payments);
        ReflectionTestUtils.setField(service,"receivableMapper",receivables);
        ReflectionTestUtils.setField(service,"incomeMapper",incomes);
        ReflectionTestUtils.setField(service,"financeEventMapper",events);
        ReflectionTestUtils.setField(service,"eomsNoGenerator",numbers);
        payment=new Payment(); payment.setId(1L); payment.setReceivableId(20L);
        payment.setAmount(new BigDecimal("37.50")); payment.setStatus("ACTIVE");
        income=new Income(); income.setId(30L); income.setPaymentId(1L);
        income.setAmount(payment.getAmount()); income.setCustomerId(40L); income.setRepairOrderId(50L); income.setIncomeType("REPAIR");
        doReturn(payment).when(service).selectPaymentById(1L);
        when(payments.lockPayment(1L)).thenReturn(payment);
        when(receivables.lockReceivable(20L)).thenReturn(new Receivable());
        when(incomes.selectOriginalByPaymentId(1L)).thenReturn(income);
        when(receivables.rollbackReceivableOnPaymentDelete(eq(20L),eq(payment.getAmount()),nullable(String.class))).thenReturn(1);
        when(payments.reversePayment(eq(1L),nullable(String.class))).thenReturn(1);
        when(numbers.generateIncomeNo()).thenReturn("TEST-REV-1");
    }
    @Test void receiptReversalRestoresReceivableAndAppendsExactlyOneNegativeIncomeWithoutDeletingOriginal() {
        assertEquals(1,service.deletePaymentByIds(new Long[]{1L,1L}));
        ArgumentCaptor<Income> written=ArgumentCaptor.forClass(Income.class);
        verify(incomes).insertIncome(written.capture());
        Income reversal=written.getValue();
        assertEquals(0,income.getAmount().add(reversal.getAmount()).compareTo(BigDecimal.ZERO));
        assertEquals(income.getId(),reversal.getReversalOfId());
        assertNull(reversal.getPaymentId()); // Unique payment linkage remains on original receipt income.
        assertEquals(income.getRepairOrderId(),reversal.getRepairOrderId());
        verify(events).insertEvent(eq("PAYMENT"),eq(1L),eq("REVERSE"),eq(30L),eq(new BigDecimal("-37.50")),nullable(String.class),anyString());
        verify(payments,never()).deletePaymentByIds(any());
        verify(incomes,never()).deleteIncomeByIds(any());
    }
    @Test void repeatReversalIsIdempotentAndCannotProduceSecondNegativeIncome() {
        doAnswer(inv->{payment.setStatus("REVERSED");return 1;}).when(payments).reversePayment(eq(1L),nullable(String.class));
        assertEquals(1,service.deletePaymentById(1L));
        assertEquals(0,service.deletePaymentById(1L));
        verify(incomes,times(1)).insertIncome(any());
        verify(receivables,times(1)).rollbackReceivableOnPaymentDelete(anyLong(),any(),nullable(String.class));
    }
    @Test void foreignOrMissingBatchMemberRejectsWholeBatchBeforeAnyFinancialWrite() {
        doReturn(null).when(service).selectPaymentById(2L);
        assertThrows(ServiceException.class,()->service.deletePaymentByIds(new Long[]{1L,2L}));
        verifyNoInteractions(incomes,receivables,events);
    }
    @Test void legacyUnlinkedIncomeFailsClosedRatherThanReversingOtherIncomeOnSameOrder() {
        when(incomes.selectOriginalByPaymentId(1L)).thenReturn(null);
        assertThrows(ServiceException.class,()->service.deletePaymentById(1L));
        verify(incomes,never()).selectIncomeList(any());
        verify(incomes,never()).insertIncome(any());
        verify(receivables,never()).rollbackReceivableOnPaymentDelete(anyLong(),any(),nullable(String.class));
        verifyNoInteractions(events);
    }
    @Test void amountMismatchCannotBeCompensated() {
        income.setAmount(BigDecimal.ONE);
        assertThrows(ServiceException.class,()->service.deletePaymentById(1L));
        verify(incomes,never()).insertIncome(any());
        verify(payments,never()).reversePayment(anyLong(),nullable(String.class));
    }
    @Test void receivableConflictStopsBeforeIncomeOrReceiptMutation() {
        when(receivables.rollbackReceivableOnPaymentDelete(anyLong(),any(),nullable(String.class))).thenReturn(0);
        assertThrows(ServiceException.class,()->service.deletePaymentById(1L));
        verify(incomes,never()).insertIncome(any()); verifyNoInteractions(events);
    }
    @Test void generatedReceiptCannotBeEditedThroughCrud() {
        assertThrows(ServiceException.class,()->service.updatePayment(payment));
        verify(payments,never()).updatePayment(any());
    }
    @Test void incomeInsertionFailurePropagatesForTransactionRollback() {
        doThrow(new IllegalStateException("database failure")).when(incomes).insertIncome(any());
        assertThrows(IllegalStateException.class,()->service.deletePaymentById(1L));
        verify(payments,never()).reversePayment(anyLong(),nullable(String.class)); verifyNoInteractions(events);
    }
}
