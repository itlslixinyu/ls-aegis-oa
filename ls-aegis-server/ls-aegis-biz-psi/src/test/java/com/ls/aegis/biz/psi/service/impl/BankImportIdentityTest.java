package com.ls.aegis.biz.psi.service.impl;

import com.ls.aegis.biz.psi.enums.BankDirectionEnum;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.BankStatementDO;
import com.ls.aegis.biz.psi.model.entity.CompanyAccountDO;
import com.ls.aegis.biz.psi.model.req.BankImportReq;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import top.continew.starter.core.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BankImportIdentityTest {
    private final BankStatementMapper bank = mock(BankStatementMapper.class);
    private final CompanyAccountMapper accounts = mock(CompanyAccountMapper.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<IGmCrypto> crypto = mock(ObjectProvider.class);
    private final BankStatementServiceImpl service = new BankStatementServiceImpl(bank, accounts,
        mock(ReceiptMapper.class), mock(PsiPaymentMapper.class), crypto, mock(CashLedgerMapper.class), mock(IdempotentHelper.class));

    private BankImportReq.Item line(String seq) {
        var line = new BankImportReq.Item(); line.setBankSeqNo(seq); line.setStmtDate(LocalDateTime.of(2026,11,6,1,0));
        line.setDirection(BankDirectionEnum.IN); line.setAmount(new BigDecimal("300.00")); line.setSummary("synthetic"); return line;
    }
    private BankImportReq request(BankImportReq.Item... lines) {
        var account = new CompanyAccountDO(); account.setId(1L); account.setTenantId(TenantFillHelper.currentTenantId());
        when(accounts.selectByIdForUpdate(1L)).thenReturn(account);
        var req = new BankImportReq(); req.setAccountId(1L); req.setItems(List.of(lines)); return req;
    }
    @Test void distinctSequenceRetainsEconomicallyIdenticalTransactions() {
        var req=request(line("bank-1"),line("bank-2"));
        assertEquals(2,service.importStatements(req));
        var captured=ArgumentCaptor.forClass(BankStatementDO.class); verify(bank,times(2)).insert(captured.capture());
        assertNotEquals(captured.getAllValues().get(0).getFingerprint(), captured.getAllValues().get(1).getFingerprint());
        verify(accounts).selectByIdForUpdate(1L);
    }
    @Test void noSequenceKeepsContentIdentityForReplay() {
        var req=request(line(null),line("  "));
        req.getItems().get(0).setAmount(new BigDecimal("300.0"));
        assertEquals(2,service.importStatements(req)); // mock storage; inspect persisted identities
        var captured=ArgumentCaptor.forClass(BankStatementDO.class); verify(bank,times(2)).insert(captured.capture());
        assertEquals(captured.getAllValues().get(0).getFingerprint(), captured.getAllValues().get(1).getFingerprint());
        assertNull(captured.getAllValues().get(1).getBankSeqNo());
    }
    @Test void historicalNoSequenceRowIsRecognizedEvenWhenItsStoredHashAlgorithmOrScaleDiffers() {
        var req=request(line(null));
        when(bank.countNoSequenceIdentity(anyLong(),eq(1L),any(),eq("IN"),any(),anyString(),eq("synthetic"))).thenReturn(1L);
        assertEquals(0,service.importStatements(req));verify(bank,never()).insert(any(BankStatementDO.class));
    }
    @Test void unauthorizedAccountFailsBeforeAnyInsert() {
        var req=new BankImportReq(); req.setAccountId(2L); req.setItems(List.of(line("a")));
        assertThrows(BusinessException.class,()->service.importStatements(req)); verifyNoInteractions(bank);
    }
    @Test void invalidMoneyCannotBeImported() {
        for (String amount: List.of("0","-1","1.001")) {
            var line=line("invalid");line.setAmount(new BigDecimal(amount));var req=request(line);
            assertThrows(BusinessException.class,()->service.importStatements(req));
        }
        verify(bank,never()).insert(any(BankStatementDO.class));
    }
}
