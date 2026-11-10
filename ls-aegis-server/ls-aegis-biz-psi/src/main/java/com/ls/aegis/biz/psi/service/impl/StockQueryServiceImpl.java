package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.mapper.StockBatchMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.StockQueryQuery;
import com.ls.aegis.biz.psi.model.resp.StockQueryResp;
import com.ls.aegis.biz.psi.service.StockQueryService;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存查询：聚合 psi_stock_batch.remain_* 得到仓×商品实时剩余
 */
@Service
@RequiredArgsConstructor
public class StockQueryServiceImpl implements StockQueryService {

    private final StockBatchMapper stockBatchMapper;
    private final PsiWarehouseMapper warehouseMapper;
    private final GoodsMapper goodsMapper;

    @Override
    public PageResp<StockQueryResp> page(StockQueryQuery query, PageQuery pageQuery) {
        List<Long> warehouseIds = resolveWarehouseIds(query != null ? query.getWarehouseId() : null);
        if (CollUtil.isEmpty(warehouseIds)) {
            Page<StockQueryResp> empty = new Page<>(pageQuery.getPage(), pageQuery.getSize(), 0);
            return PageResp.build(empty, StockQueryResp.class);
        }
        Long tenantId = ObjectUtil.defaultIfNull(UserContextHolder.getTenantId(), 0L);
        Long goodsId = query != null ? query.getGoodsId() : null;
        boolean onlyPositive = query == null || query.getOnlyPositive() == null || Boolean.TRUE.equals(query.getOnlyPositive());
        long page = Math.max(pageQuery.getPage(), 1);
        long size = Math.max(pageQuery.getSize(), 1);
        long offset = (page - 1) * size;

        long total = stockBatchMapper.countBalance(tenantId, warehouseIds, goodsId, onlyPositive);
        List<StockQueryResp> rows = total <= 0
            ? List.of()
            : stockBatchMapper.selectBalancePage(tenantId, warehouseIds, goodsId, onlyPositive, offset, size);
        fillNames(rows);

        Page<StockQueryResp> mpPage = new Page<>(page, size, total);
        mpPage.setRecords(rows);
        return PageResp.build(mpPage, StockQueryResp.class);
    }

    private List<Long> resolveWarehouseIds(Long warehouseId) {
        if (warehouseId != null) {
            assertWarehouseAccess(warehouseId);
            return List.of(warehouseId);
        }
        return listAccessibleWarehouseIds();
    }

    private List<Long> listAccessibleWarehouseIds() {
        var q = Wrappers.lambdaQuery(WarehouseDO.class)
            .eq(WarehouseDO::getStatus, DisEnableStatusEnum.ENABLE)
            .select(WarehouseDO::getId);
        if (!UserContextHolder.isSuperAdmin() && !UserContextHolder.isTenantAdmin()) {
            q.eq(WarehouseDO::getManagerUserId, UserContextHolder.getUserId());
        }
        return warehouseMapper.selectList(q).stream().map(WarehouseDO::getId).toList();
    }

    private void assertWarehouseAccess(Long warehouseId) {
        WarehouseDO wh = warehouseMapper.selectById(warehouseId);
        CheckUtils.throwIfNull(wh, "仓库不存在");
        if (UserContextHolder.isSuperAdmin() || UserContextHolder.isTenantAdmin()) {
            return;
        }
        CheckUtils.throwIf(
            !ObjectUtil.equal(wh.getManagerUserId(), UserContextHolder.getUserId()),
            "只能查看自己负责的仓库");
    }

    private void fillNames(List<StockQueryResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> warehouseIds = list.stream().map(StockQueryResp::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> goodsIds = list.stream().map(StockQueryResp::getGoodsId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WarehouseDO> warehouses = new HashMap<>();
        Map<Long, GoodsDO> goodsMap = new HashMap<>();
        if (CollUtil.isNotEmpty(warehouseIds)) {
            for (WarehouseDO w : warehouseMapper.selectByIds(warehouseIds)) {
                warehouses.put(w.getId(), w);
            }
        }
        if (CollUtil.isNotEmpty(goodsIds)) {
            for (GoodsDO g : goodsMapper.selectByIds(goodsIds)) {
                goodsMap.put(g.getId(), g);
            }
        }
        for (StockQueryResp row : list) {
            row.setId(row.getWarehouseId() + "_" + row.getGoodsId());
            if (row.getRemainQuantity() == null) {
                row.setRemainQuantity(BigDecimal.ZERO);
            }
            if (row.getRemainAmount() == null) {
                row.setRemainAmount(BigDecimal.ZERO);
            }
            WarehouseDO w = warehouses.get(row.getWarehouseId());
            if (w != null) {
                row.setWarehouseName(w.getWarehouseName());
            }
            GoodsDO g = goodsMap.get(row.getGoodsId());
            if (g != null) {
                row.setGoodsCode(g.getGoodsCode());
                row.setGoodsName(g.getGoodsName());
                row.setSpec(g.getSpec());
                row.setUnit(g.getUnit());
            }
        }
    }
}
