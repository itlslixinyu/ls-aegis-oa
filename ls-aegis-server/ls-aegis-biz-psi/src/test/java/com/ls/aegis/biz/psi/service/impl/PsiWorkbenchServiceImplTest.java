package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.biz.psi.model.resp.PsiWorkbenchSummaryResp;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DataScopeEnum;
import com.ls.aegis.biz.psi.model.resp.StockAlertQuantityResp;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PsiWorkbenchServiceImplTest {
    private final PsiTodoMapper todo = mock(PsiTodoMapper.class);
    private final GoodsMapper goods = mock(GoodsMapper.class);
    private final StockBatchMapper batches = mock(StockBatchMapper.class);
    private final PsiWorkbenchMapper aggregates = mock(PsiWorkbenchMapper.class);
    private final PsiWorkbenchServiceImpl service = new PsiWorkbenchServiceImpl(todo, goods, batches, aggregates);

    @BeforeEach void emptyAggregates() {
        when(aggregates.selectReceivables(anyLong())).thenReturn(new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate());
        when(aggregates.selectPayables(anyLong())).thenReturn(new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate());
        when(aggregates.selectReceipts(anyLong())).thenReturn(new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate());
        when(aggregates.selectBanks(anyLong())).thenReturn(new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate());
        when(aggregates.selectInHeaders(anyLong(), any(), any(), any(), any())).thenReturn(new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate());
        when(aggregates.selectOutHeaders(anyLong(), any(), any(), any(), any())).thenReturn(new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate());
        when(aggregates.selectInQuantities(anyLong(), any(), any(), any(), any())).thenReturn(new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate());
        when(aggregates.selectOutQuantities(anyLong(), any(), any(), any(), any())).thenReturn(new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate());
    }

    @BeforeEach void stockAlertIdentity() {
        var request=new org.springframework.mock.web.MockHttpServletRequest();var response=new org.springframework.mock.web.MockHttpServletResponse();
        cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.setModelBox(new cn.dev33.satoken.servlet.model.SaRequestForServlet(request),new cn.dev33.satoken.servlet.model.SaResponseForServlet(response),new cn.dev33.satoken.servlet.model.SaStorageForServlet(request));
        cn.dev33.satoken.stp.StpUtil.login(1L);
        var actor=new UserContext();actor.setId(1L);actor.setTenantId(0L);
        actor.setRoles(Set.of(new RoleContext(1L,"super_admin",DataScopeEnum.ALL)));
        UserContextHolder.setContext(actor,false);
    }
    @AfterEach void clearStockAlertIdentity(){cn.dev33.satoken.stp.StpUtil.logout();cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.clearModelBox();UserContextHolder.clearContext();}

    @BeforeAll
    static void initializeLambdaMetadata() {
        for (Class<?> type : List.of(PsiTodoDO.class, ReceivableDO.class, PayableDO.class, GoodsDO.class,
                StockBatchDO.class, StockInDO.class, StockOutDO.class, StockInItemDO.class,
                StockOutItemDO.class, ReceiptDO.class, BankStatementDO.class)) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "quality");
            assistant.setCurrentNamespace("quality." + type.getSimpleName());
            TableInfoHelper.initTableInfo(assistant, type);
        }
    }

    @Test
    void aggregateValuesKeepCountsSignedAmountsAndTenant() {
        var ar = new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate(); ar.setCount(2); ar.setAmount(new BigDecimal("4.00"));
        var bank = new com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate(); bank.setCount(2); bank.setAmount(new BigDecimal("-5.10"));
        when(aggregates.selectReceivables(0L)).thenReturn(ar);
        when(aggregates.selectBanks(0L)).thenReturn(bank);
        var result=service.summary();
        assertEquals(2,result.getOpenReceivableCount());
        assertEquals(new BigDecimal("4.00"),result.getOpenReceivableAmount());
        assertEquals(2,result.getUnmatchedBankCount());
        assertEquals(new BigDecimal("-5.10"),result.getUnmatchedBankAmount());
        verify(aggregates).selectPayables(0L);
        verify(aggregates).selectReceipts(0L);
    }

    @Test
    void malformedIdentityFailsBeforeAnySummaryQuery() {
        UserContextHolder.getContext().setRoles(Set.of());
        clearInvocations(aggregates);
        assertThrows(RuntimeException.class,service::summary);
        verifyNoInteractions(aggregates, todo, goods, batches);
    }

    @Test
    void stockAlertsRestrictBatchesToWatchedGoodsAndSumAcrossBatches() {
        GoodsDO low = new GoodsDO(); low.setId(10L); low.setStockLower(new BigDecimal("6"));
        GoodsDO high = new GoodsDO(); high.setId(20L); high.setStockUpper(new BigDecimal("4"));
        when(goods.selectList(any())).thenReturn(List.of(low, high));
        when(batches.selectAlertQuantities(eq(0L), anyList())).thenAnswer(call -> {
            assertEquals(List.of(10L,20L),call.getArgument(1));
            var a=new StockAlertQuantityResp();a.setGoodsId(10L);a.setRemainQuantity(new BigDecimal("5"));
            var b=new StockAlertQuantityResp();b.setGoodsId(20L);b.setRemainQuantity(new BigDecimal("5"));
            return List.of(a,b);
        });
        var alerts = service.summary().getStockAlerts();
        verify(batches,never()).selectList(any());
        assertEquals(2, alerts.size());
        assertEquals("LOW", alerts.get(0).getAlertType());
        assertEquals("HIGH", alerts.get(1).getAlertType());
        assertEquals(new BigDecimal("5"), alerts.get(0).getStockQty());
        assertEquals(new BigDecimal("5"), alerts.get(1).getStockQty());
    }

    @Test
    void emptyWindowsAndNoWatchedGoodsDoNotQueryItemsOrInventory() {
        PsiWorkbenchSummaryResp result = service.summary();
        assertEquals(0, result.getMonthSaleCount());
        assertEquals(BigDecimal.ZERO, result.getMonthSaleQty());
        assertTrue(result.getStockAlerts().isEmpty());
        verifyNoInteractions(batches);
        verify(aggregates,never()).selectInQuantities(anyLong(), any(), any(), any(), any());
        verify(aggregates,never()).selectOutQuantities(anyLong(), any(), any(), any(), any());
    }

}
