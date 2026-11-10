package com.ls.aegis.biz.quote.service;

import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.domain.QuoteDetail;
import com.ls.aegis.biz.quote.mapper.QuoteMapper;
import com.ls.aegis.biz.quote.mapper.QuoteDetailMapper;
import com.ls.aegis.biz.quote.service.impl.QuoteServiceImpl;
import com.ls.aegis.starter.support.docno.DocNoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import top.continew.starter.core.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuoteServiceImplTest {
    private QuoteMapper headers;
    private QuoteDetailMapper lines;
    private QuoteServiceImpl service;
    private Quote existing;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        QuoteTestActor.open();
        headers = mock(QuoteMapper.class);
        lines = mock(QuoteDetailMapper.class);
        service = new QuoteServiceImpl(headers, lines, mock(ObjectProvider.class), mock(ObjectProvider.class), mock(DocNoService.class));
        existing = quote("13");
        when(headers.selectQuoteByIdForUpdate(1L)).thenReturn(existing);
        when(headers.updateQuote(any())).thenReturn(1);
        when(lines.selectQuoteDetailList(any())).thenReturn(List.of(line("MATERIAL", "2", "10")));
        when(headers.insertQuote(any())).thenAnswer(inv -> { ((Quote)inv.getArgument(0)).setId(1L); return 1; });
    }

    @AfterEach void clearActor() { QuoteTestActor.close(); }

    @Test void creationUsesStableAuthenticatedOwnershipAndIgnoresClientFields() {
        Quote created = quote("0"); created.setQuoteNo("TEST-OWNER");
        created.setTenantId(99L); created.setCreateUser(999L); created.setDeptId(999L); created.setCreateBy("forged");
        service.insertQuote(created);
        assertEquals(1L, created.getTenantId()); assertEquals(100L, created.getCreateUser()); assertEquals(10L, created.getDeptId());
        assertEquals("reused-display-name", created.getCreateBy());
    }

    @Test void inaccessibleBatchMemberRejectsBeforeAnyDetailOrHeaderDelete() {
        when(headers.selectQuoteByIdForUpdate(2L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.deleteQuoteByIds(new Long[] {1L, 2L}));
        verify(lines, never()).deleteQuoteDetailByQuoteIds(any()); verify(headers, never()).deleteQuoteByIds(any());
    }

    @Test void statusOnlyPreservesTaxAndRecomputesFromStoredRows() {
        Quote patch = quote(null);
        patch.setStatus("SUBMIT");
        patch.setTotalAmount(new BigDecimal("999999"));
        patch.setMaterialSubtotal(new BigDecimal("999999"));
        service.updateQuote(patch);
        money("13", patch.getTaxRate());
        money("20", patch.getMaterialSubtotal());
        money("0", patch.getServiceSubtotal());
        money("22.60", patch.getTotalAmount());
        verify(lines, never()).deleteQuoteDetailByQuoteId(any());
        verify(lines, never()).insertQuoteDetail(any());
    }

    @Test void taxOnlyUsesExistingDetails() {
        Quote patch = quote("6");
        service.updateQuote(patch);
        money("21.20", patch.getTotalAmount());
        verify(lines).selectQuoteDetailList(argThat(q -> q.getQuoteId().equals(1L)));
    }

    @Test void replacementRetainsExistingTaxAndDiscardsAllClientDerivedAmounts() {
        Quote patch = quote(null);
        QuoteDetail material = line("material", "1.25", "3.335");
        material.setAmount(new BigDecimal("9999"));
        patch.setDetails(List.of(material, line("SERVICE", "2.50", "0")));
        service.updateQuote(patch);
        money("3.34", material.getPrice());
        money("4.18", material.getAmount());
        money("4.18", patch.getMaterialSubtotal());
        money("4.72", patch.getTotalAmount());
        verify(lines, times(2)).insertQuoteDetail(any());
    }

    @Test void decimalQuantityIsRoundedBeforeComputingStoredAmount() {
        Quote patch = quote("0");
        QuoteDetail line = line("SERVICE", "1.235", "10.005");
        patch.setDetails(List.of(line));
        service.updateQuote(patch);
        money("1.24", line.getQuantity());
        money("10.01", line.getPrice());
        money("12.41", patch.getTotalAmount());
    }

    @Test void explicitEmptyDetailsClearsAmountsRatherThanTrustingHeader() {
        Quote patch = quote(null);
        patch.setDetails(List.of());
        patch.setTotalAmount(BigDecimal.TEN);
        service.updateQuote(patch);
        money("0", patch.getTotalAmount());
        money("0", patch.getMaterialSubtotal());
        verify(lines).deleteQuoteDetailByQuoteId(1L);
    }

    @Test void creationWithoutRowsAlwaysHasZeroDerivedAmountsAndDefaultTax() {
        Quote created = quote(null);
        created.setQuoteNo("TEST-QUOTE");
        created.setTotalAmount(BigDecimal.TEN);
        service.insertQuote(created);
        money("4", created.getTaxRate());
        money("0", created.getTotalAmount());
        assertEquals("DRAFT", created.getStatus());
    }

    @Test void creationWithEmptyRowsHasZeroAmount() {
        Quote created = quote("0");
        created.setQuoteNo("TEST-QUOTE");
        created.setDetails(List.of());
        created.setTotalAmount(BigDecimal.TEN);
        service.insertQuote(created);
        money("0", created.getTotalAmount());
    }

    @Test void blankOrNullRowsAreRejectedBeforeDeletingExistingDetails() {
        Quote patch = quote(null);
        QuoteDetail blank = line("SERVICE", "1", "1");
        blank.setItemName(" ");
        patch.setDetails(List.of(blank));
        assertThrows(BusinessException.class, () -> service.updateQuote(patch));
        patch.setDetails(java.util.Arrays.asList((QuoteDetail)null));
        assertThrows(BusinessException.class, () -> service.updateQuote(patch));
        verify(lines, never()).deleteQuoteDetailByQuoteId(any());
    }

    @Test void illegalPriceAndQuantityAreRejectedBeforeAnyWrite() {
        for (String[] values : new String[][] {{"0", "1"}, {"-1", "1"}, {"1", "-0.001"}, {"0.001", "1"}, {"100000000", "1"}, {"1", "10000000000"}}) {
            Quote patch = quote(null);
            patch.setDetails(List.of(line("SERVICE", values[0], values[1])));
            assertThrows(BusinessException.class, () -> service.updateQuote(patch), java.util.Arrays.toString(values));
        }
        verify(headers, never()).updateQuote(any());
        verify(lines, never()).deleteQuoteDetailByQuoteId(any());
    }

    @Test void missingNumericFieldsAndUnknownTypeAreRejected() {
        Quote patch = quote(null);
        QuoteDetail line = line("SERVICE", "1", "1");
        line.setPrice(null);
        patch.setDetails(List.of(line));
        assertThrows(BusinessException.class, () -> service.updateQuote(patch));
        line.setPrice(BigDecimal.ZERO);
        line.setQuantity(null);
        assertThrows(BusinessException.class, () -> service.updateQuote(patch));
        line.setQuantity(BigDecimal.ONE);
        line.setLineType("OTHER");
        assertThrows(BusinessException.class, () -> service.updateQuote(patch));
        verify(headers, never()).updateQuote(any());
        verify(lines, never()).deleteQuoteDetailByQuoteId(any());
    }

    @Test void serviceRowCannotRetainGoodsAssociationAndZeroTaxIsValid() {
        Quote patch = quote("0");
        QuoteDetail line = line("service", "1", "10");
        line.setGoodsId(42L);
        patch.setDetails(List.of(line));
        service.updateQuote(patch);
        assertNull(line.getGoodsId());
        assertEquals("SERVICE", line.getLineType());
        money("10", patch.getTotalAmount());
    }

    @Test void emptyDeleteDoesNotIssueInvalidSql() {
        assertEquals(0, service.deleteQuoteByIds(null));
        assertEquals(0, service.deleteQuoteByIds(new Long[0]));
        assertEquals(0, service.deleteQuoteByIds(new Long[] {null}));
        verify(headers, never()).deleteQuoteByIds(any());
    }

    @Test void invalidTaxAndOverflowAreRejectedBeforeAnyWrite() {
        for (String rate : List.of("-0.001", "100.001")) {
            assertThrows(BusinessException.class, () -> service.updateQuote(quote(rate)));
        }
        Quote patch = quote("100");
        patch.setDetails(List.of(line("SERVICE", "1", "9999999999.99")));
        assertThrows(BusinessException.class, () -> service.updateQuote(patch));
        verify(lines, never()).deleteQuoteDetailByQuoteId(any());
        verify(headers, never()).updateQuote(any());
    }

    @Test void clientCannotForgeOrderAssociationOrDeleteFlag() {
        Quote patch = quote(null);
        patch.setRepairOrderId(999L);
        patch.setDelFlag("2");
        service.updateQuote(patch);
        assertNull(patch.getRepairOrderId());
        assertNull(patch.getDelFlag());
    }

    @Test void terminalQuoteCannotBeModifiedOrDeleted() {
        existing.setStatus("CONFIRM");
        assertThrows(BusinessException.class, () -> service.updateQuote(quote(null)));
        assertThrows(BusinessException.class, () -> service.deleteQuoteById(1L));
        verify(headers, never()).updateQuote(any());
        verify(lines, never()).deleteQuoteDetailByQuoteId(any());
    }

    static Quote quote(String tax) {
        Quote q = new Quote(); q.setId(1L); q.setStatus("DRAFT");
        q.setTaxRate(tax == null ? null : new BigDecimal(tax)); return q;
    }
    static QuoteDetail line(String type, String quantity, String price) {
        QuoteDetail l = new QuoteDetail(); l.setItemName("TEST-ITEM"); l.setLineType(type);
        l.setQuantity(new BigDecimal(quantity)); l.setPrice(new BigDecimal(price));
        l.setAmount(l.getQuantity().multiply(l.getPrice()).setScale(2, java.math.RoundingMode.HALF_UP)); return l;
    }
    static void money(String expected, BigDecimal actual) {
        assertNotNull(actual); assertEquals(0, new BigDecimal(expected).compareTo(actual), actual.toPlainString());
    }
}
