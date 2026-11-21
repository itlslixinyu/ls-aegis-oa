package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import com.ls.aegis.biz.psi.mapper.PayableMapper;
import com.ls.aegis.biz.psi.mapper.PsiReceivableMapper;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.PayableDO;
import com.ls.aegis.biz.psi.model.entity.ReceivableDO;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.model.query.OpenItemQuery;
import com.ls.aegis.biz.psi.model.resp.OpenItemOptionResp;
import com.ls.aegis.biz.psi.model.resp.OpenItemResp;
import com.ls.aegis.biz.psi.service.OpenItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OpenItemServiceImpl implements OpenItemService {

    private final PsiReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;
    private final UnitMapper unitMapper;

    @Override
    public PageResp<OpenItemResp> pageReceivable(OpenItemQuery query, PageQuery pageQuery) {
        QueryWrapper<ReceivableDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<ReceivableDO> page = receivableMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<OpenItemResp> resp = PageResp.build(page, OpenItemResp.class);
        fillUnitNames(resp.getList());
        return resp;
    }

    @Override
    public PageResp<OpenItemResp> pagePayable(OpenItemQuery query, PageQuery pageQuery) {
        QueryWrapper<PayableDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<PayableDO> page = payableMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<OpenItemResp> resp = PageResp.build(page, OpenItemResp.class);
        fillUnitNames(resp.getList());
        return resp;
    }

    @Override
    public List<OpenItemOptionResp> openReceivables(Long unitId) {
        var q = Wrappers.lambdaQuery(ReceivableDO.class)
            .eq(ReceivableDO::getDocStatus, OpenItemStatusEnum.OPEN)
            .gt(ReceivableDO::getUnwrittenAmount, BigDecimal.ZERO)
            .orderByAsc(ReceivableDO::getId);
        if (unitId != null) {
            q.eq(ReceivableDO::getUnitId, unitId);
        }
        return receivableMapper.selectList(q).stream().map(this::toArOption).toList();
    }

    @Override
    public List<OpenItemOptionResp> openPayables(Long unitId) {
        var q = Wrappers.lambdaQuery(PayableDO.class)
            .eq(PayableDO::getDocStatus, OpenItemStatusEnum.OPEN)
            .gt(PayableDO::getUnwrittenAmount, BigDecimal.ZERO)
            .orderByAsc(PayableDO::getId);
        if (unitId != null) {
            q.eq(PayableDO::getUnitId, unitId);
        }
        return payableMapper.selectList(q).stream().map(this::toApOption).toList();
    }

    private OpenItemOptionResp toArOption(ReceivableDO ar) {
        String type = ar.getSourceType() == null ? "" : ar.getSourceType().getValue();
        String typeDesc = ar.getSourceType() == null ? "" : ar.getSourceType().getDescription();
        String label = (StrUtil.isNotBlank(typeDesc) ? typeDesc + "｜" : "")
            + StrUtil.blankToDefault(ar.getSourceNo(), String.valueOf(ar.getId()))
            + "｜未核销 " + nz(ar.getUnwrittenAmount()).toPlainString();
        return new OpenItemOptionResp(ar.getId(), label, nz(ar.getUnwrittenAmount()), ar.getSourceNo(), type);
    }

    private OpenItemOptionResp toApOption(PayableDO ap) {
        String type = ap.getSourceType() == null ? "" : ap.getSourceType().getValue();
        String typeDesc = ap.getSourceType() == null ? "" : ap.getSourceType().getDescription();
        String label = (StrUtil.isNotBlank(typeDesc) ? typeDesc + "｜" : "")
            + StrUtil.blankToDefault(ap.getSourceNo(), String.valueOf(ap.getId()))
            + "｜未核销 " + nz(ap.getUnwrittenAmount()).toPlainString();
        return new OpenItemOptionResp(ap.getId(), label, nz(ap.getUnwrittenAmount()), ap.getSourceNo(), type);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void fillUnitNames(List<OpenItemResp> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> unitIds = list.stream().map(OpenItemResp::getUnitId).filter(Objects::nonNull).distinct().toList();
        if (unitIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>();
        for (UnitDO unit : unitMapper.selectByIds(unitIds)) {
            nameMap.put(unit.getId(), unit.getUnitName());
        }
        for (OpenItemResp item : list) {
            item.setUnitName(nameMap.get(item.getUnitId()));
        }
    }
}
