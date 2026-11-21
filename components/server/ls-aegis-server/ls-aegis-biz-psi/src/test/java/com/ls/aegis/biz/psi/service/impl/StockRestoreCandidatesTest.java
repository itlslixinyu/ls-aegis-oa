package com.ls.aegis.biz.psi.service.impl;

import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.common.context.UserContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.context.support.GenericApplicationContext;
import cn.hutool.extra.spring.SpringUtil;
import org.mockito.MockedStatic;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockRestoreCandidatesTest {
    private static GenericApplicationContext context;
    @BeforeAll static void validationContext() {
        context = new GenericApplicationContext();
        context.registerBean(jakarta.validation.Validator.class, () -> mock(jakarta.validation.Validator.class));
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }
    @AfterAll static void closeContext() { context.close(); new SpringUtil().setApplicationContext(null); }
    private final StockBatchMapper batches = mock(StockBatchMapper.class);
    private final PsiWarehouseMapper warehouses = mock(PsiWarehouseMapper.class);
    private final GoodsMapper goods = mock(GoodsMapper.class);
    private final StockQueryServiceImpl service = new StockQueryServiceImpl(batches, warehouses, goods);

    @Test void restrictsCandidatesToTheAuthorizedWarehouseGoodsAndTenant() {
        try (MockedStatic<UserContextHolder> user = mockStatic(UserContextHolder.class)) {
            user.when(UserContextHolder::getUserId).thenReturn(7L);
            user.when(UserContextHolder::getTenantId).thenReturn(9L);
            WarehouseDO warehouse = new WarehouseDO(); warehouse.setManagerUserId(7L);
            when(warehouses.selectById(2L)).thenReturn(warehouse);
            when(goods.selectById(3L)).thenReturn(new GoodsDO());
            when(batches.selectRestoreCandidates(9L,2L,3L)).thenReturn(List.of());
            assertTrue(service.restoreBatches(2L,3L).isEmpty());
            verify(batches).selectRestoreCandidates(9L,2L,3L);
        }
    }
    @Test void rejectsUnmanagedWarehouseBeforeReadingAnyBatch() {
        try (MockedStatic<UserContextHolder> user = mockStatic(UserContextHolder.class)) {
            user.when(UserContextHolder::getUserId).thenReturn(7L);
            WarehouseDO warehouse = new WarehouseDO(); warehouse.setManagerUserId(8L);
            when(warehouses.selectById(2L)).thenReturn(warehouse);
            assertThrows(RuntimeException.class, () -> service.restoreBatches(2L,3L));
            verifyNoInteractions(batches, goods);
        }
    }
    @Test void rejectsUnavailableGoodsBeforeReadingAnyBatch() {
        try (MockedStatic<UserContextHolder> user = mockStatic(UserContextHolder.class)) {
            user.when(UserContextHolder::isSuperAdmin).thenReturn(true);
            when(warehouses.selectById(2L)).thenReturn(new WarehouseDO());
            assertThrows(RuntimeException.class, () -> service.restoreBatches(2L,3L));
            verifyNoInteractions(batches);
        }
    }
}
