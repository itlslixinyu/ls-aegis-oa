package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.StockFlowMapper;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.StockFlowDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.StockFlowQuery;
import com.ls.aegis.biz.psi.model.resp.StockFlowResp;
import com.ls.aegis.biz.psi.service.StockFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockFlowServiceImpl implements StockFlowService {

    private final StockFlowMapper stockFlowMapper;
    private final PsiWarehouseMapper warehouseMapper;
    private final GoodsMapper goodsMapper;

    @Override
    public PageResp<StockFlowResp> page(StockFlowQuery query, PageQuery pageQuery) {
        QueryWrapper<StockFlowDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<StockFlowDO> page = stockFlowMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<StockFlowResp> resp = PageResp.build(page, StockFlowResp.class);
        fillNames(resp.getList());
        return resp;
    }

    private void fillNames(List<StockFlowResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> warehouseIds = list.stream().map(StockFlowResp::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> goodsIds = list.stream().map(StockFlowResp::getGoodsId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> warehouseNames = new HashMap<>();
        Map<Long, String> goodsNames = new HashMap<>();
        if (CollUtil.isNotEmpty(warehouseIds)) {
            for (WarehouseDO w : warehouseMapper.selectByIds(warehouseIds)) {
                warehouseNames.put(w.getId(), w.getWarehouseName());
            }
        }
        if (CollUtil.isNotEmpty(goodsIds)) {
            for (GoodsDO g : goodsMapper.selectByIds(goodsIds)) {
                goodsNames.put(g.getId(), g.getGoodsName());
            }
        }
        for (StockFlowResp row : list) {
            if (row.getWarehouseId() != null) {
                row.setWarehouseName(warehouseNames.get(row.getWarehouseId()));
            }
            if (row.getGoodsId() != null) {
                row.setGoodsName(goodsNames.get(row.getGoodsId()));
            }
        }
    }
}
