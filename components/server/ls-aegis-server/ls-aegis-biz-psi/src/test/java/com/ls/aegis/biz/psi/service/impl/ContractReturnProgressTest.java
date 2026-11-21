package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.ReturnTypeEnum;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.resp.ContractResp;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.PsiAttachmentAccessGuard;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContractReturnProgressTest {
    @BeforeAll static void metadata() {
        for (Class<?> type : List.of(StockReturnDO.class, StockOutDO.class, StockInDO.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), type);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,0,1", "true,25,0.75", "true,100,0", "true,120,0",
        "false,0,1", "false,25,0.75", "false,100,0", "false,120,0"})
    @SuppressWarnings({"rawtypes", "unchecked"})
    void confirmedReturnsReduceProgressAndRemainScopedToOriginalDocuments(boolean sale, String returned, String expected) {
        StockReturnMapper returns = mock(StockReturnMapper.class);
        StockReturnDO returnedDoc = new StockReturnDO();
        returnedDoc.setTaxInclusiveAmount(new BigDecimal(returned));
        when(returns.selectList(any())).thenReturn(List.of(returnedDoc));
        DocNoGenerator numbers = mock(DocNoGenerator.class);
        IdempotentHelper idempotency = mock(IdempotentHelper.class);
        PsiAttachmentAccessGuard attachments = mock(PsiAttachmentAccessGuard.class);
        ContractResp result;
        if (sale) {
            ContractSaleMapper contracts = mock(ContractSaleMapper.class);
            ContractSaleDO contract = new ContractSaleDO();
            contract.setId(1L); contract.setTaxInclusiveAmount(new BigDecimal("100"));
            when(contracts.selectById(1L)).thenReturn(contract);
            StockOutMapper outs = mock(StockOutMapper.class);
            StockOutDO original = new StockOutDO(); original.setId(10L);
            original.setTaxInclusiveAmount(new BigDecimal("100"));
            when(outs.selectList(any())).thenReturn(List.of(original));
            result = new ContractSaleServiceImpl(contracts, outs, returns, numbers, idempotency, attachments).progress(1L);
        } else {
            ContractPurchaseMapper contracts = mock(ContractPurchaseMapper.class);
            ContractPurchaseDO contract = new ContractPurchaseDO();
            contract.setId(1L); contract.setTaxInclusiveAmount(new BigDecimal("100"));
            when(contracts.selectById(1L)).thenReturn(contract);
            StockInMapper ins = mock(StockInMapper.class);
            StockInDO original = new StockInDO(); original.setId(10L);
            original.setTaxInclusiveAmount(new BigDecimal("100"));
            when(ins.selectList(any())).thenReturn(List.of(original));
            result = new ContractPurchaseServiceImpl(contracts, ins, returns, numbers, idempotency, attachments).progress(1L);
        }
        assertEquals(0, new BigDecimal(expected).compareTo(result.getProgress()));
        ArgumentCaptor<Wrapper<StockReturnDO>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(returns).selectList(query.capture());
        String sql = query.getValue().getSqlSegment();
        assertTrue(sql.contains(sale ? "origin_stock_out_id" : "origin_stock_in_id"));
        var params = ((com.baomidou.mybatisplus.core.conditions.AbstractWrapper) query.getValue()).getParamNameValuePairs().values();
        assertTrue(params.contains(10L));
        assertTrue(params.contains(DocStatusEnum.CONFIRMED));
        assertTrue(params.contains(sale ? ReturnTypeEnum.SALE_RETURN : ReturnTypeEnum.PURCHASE_RETURN));
    }
}
