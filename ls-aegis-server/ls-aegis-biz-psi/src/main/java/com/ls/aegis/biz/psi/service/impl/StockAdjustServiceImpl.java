package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.AdjustTypeEnum;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.mapper.StockAdjustItemMapper;
import com.ls.aegis.biz.psi.mapper.StockAdjustMapper;
import com.ls.aegis.biz.psi.model.entity.StockAdjustDO;
import com.ls.aegis.biz.psi.model.entity.StockAdjustItemDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.StockAdjustQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockAdjustReq;
import com.ls.aegis.biz.psi.model.resp.StockAdjustResp;
import com.ls.aegis.biz.psi.service.StockAdjustService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.FifoStockHelper;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockAdjustServiceImpl implements StockAdjustService {

    private final StockAdjustMapper stockAdjustMapper;
    private final StockAdjustItemMapper stockAdjustItemMapper;
    private final PsiWarehouseMapper warehouseMapper;
    private final DocNoGenerator docNoGenerator;
    private final FifoStockHelper fifoStockHelper;
    private final IdempotentHelper idempotentHelper;

    @Override
    public PageResp<StockAdjustResp> page(StockAdjustQuery query, PageQuery pageQuery) {
        QueryWrapper<StockAdjustDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<StockAdjustDO> page = stockAdjustMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<StockAdjustResp> resp = PageResp.build(page, StockAdjustResp.class);
        fillWarehouseNames(resp.getList());
        return resp;
    }

    @Override
    public StockAdjustResp get(Long id) {
        StockAdjustDO entity = require(id);
        StockAdjustResp resp = BeanUtil.copyProperties(entity, StockAdjustResp.class);
        List<StockAdjustItemDO> items = stockAdjustItemMapper.selectList(Wrappers.lambdaQuery(StockAdjustItemDO.class)
            .eq(StockAdjustItemDO::getAdjustId, id)
            .orderByAsc(StockAdjustItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, StockAdjustResp.Item.class));
        fillWarehouseNames(List.of(resp));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockAdjustReq req) {
        Long tenantId = TenantFillHelper.currentTenantId();
        StockAdjustDO entity = new StockAdjustDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_STOCK_ADJUST));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setAdjustDate(req.getAdjustDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setRemark(req.getRemark());
        stockAdjustMapper.insert(entity);
        saveItems(tenantId, entity.getId(), req.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockAdjustReq req, Long id) {
        StockAdjustDO entity = requireLocked(id);
        assertDraft(entity);
        entity.setAdjustDate(req.getAdjustDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setRemark(req.getRemark());
        stockAdjustMapper.updateById(entity);
        stockAdjustItemMapper.delete(Wrappers.lambdaQuery(StockAdjustItemDO.class).eq(StockAdjustItemDO::getAdjustId, id));
        saveItems(entity.getTenantId(), id, req.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StockAdjustDO entity = requireLocked(id);
        assertDraft(entity);
        stockAdjustItemMapper.delete(Wrappers.lambdaQuery(StockAdjustItemDO.class).eq(StockAdjustItemDO::getAdjustId, id));
        stockAdjustMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id, DocConfirmReq req) {
        StockAdjustDO locked = stockAdjustMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("调整单不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "stock-adjust-confirm-" + id, req.getIdempotentKey(), () -> doConfirm(locked, req
            .getIdempotentKey()));
    }

    private String doConfirm(StockAdjustDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked);
        Long userId = UserContextHolder.getUserId();
        if (!ObjectUtil.equal(locked.getCreateUser(), userId)) {
            throw new BusinessException("仅制单人可确认生效");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        List<StockAdjustItemDO> items = stockAdjustItemMapper.selectList(Wrappers.lambdaQuery(StockAdjustItemDO.class)
            .eq(StockAdjustItemDO::getAdjustId, locked.getId())
            .orderByAsc(StockAdjustItemDO::getSort));
        fifoStockHelper.lockAll(items.stream().map(item -> new com.ls.aegis.biz.psi.support.StockMutationLock.Key(
            tenantId, locked.getWarehouseId(), item.getGoodsId())).toList());
        for (StockAdjustItemDO item : items) {
            if (AdjustTypeEnum.LOSS.equals(item.getAdjustType())) {
                List<FifoStockHelper.ConsumeLine> consumes = fifoStockHelper.adjustOutbound(tenantId, locked
                    .getWarehouseId(), item.getGoodsId(), item.getQuantity(), locked.getId(), locked.getDocNo());
                BigDecimal cost = BigDecimal.ZERO;
                for (FifoStockHelper.ConsumeLine c : consumes) {
                    cost = cost.add(c.costAmount());
                }
                item.setCostAmount(cost);
            } else if (AdjustTypeEnum.PROFIT.equals(item.getAdjustType())) {
                if (item.getUntaxPrice() == null) {
                    throw new BusinessException("盘盈未税单价不能为空");
                }
                BigDecimal amount = item.getQuantity()
                    .multiply(item.getUntaxPrice())
                    .setScale(2, RoundingMode.HALF_UP);
                if (item.getOriginBatchId() != null) {
                    amount = fifoStockHelper.restoreAdjustmentBatch(tenantId, locked.getWarehouseId(), item.getGoodsId(),
                        item.getOriginBatchId(), item.getQuantity(), item.getUntaxPrice(), locked.getId(), locked.getDocNo());
                } else {
                    fifoStockHelper.adjustInbound(tenantId, locked.getWarehouseId(), item.getGoodsId(), item
                        .getQuantity(), item.getUntaxPrice(), amount, locked.getAdjustDate(), locked.getId(), locked.getDocNo());
                }
                item.setCostAmount(amount);
            } else {
                throw new BusinessException("不支持的调整类型");
            }
            stockAdjustItemMapper.updateById(item);
        }
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(userId);
        locked.setConfirmTime(LocalDateTime.now());
        stockAdjustMapper.updateById(locked);
        return locked.getDocNo();
    }

    private void saveItems(Long tenantId, Long adjustId, List<StockAdjustReq.Item> lines) {
        int sort = 0;
        for (StockAdjustReq.Item line : lines) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("调整数量必须大于 0");
            }
            if (AdjustTypeEnum.PROFIT.equals(line.getAdjustType()) && line.getUntaxPrice() == null) {
                throw new BusinessException("盘盈未税单价不能为空");
            }
            if (line.getUntaxPrice() != null && line.getUntaxPrice().signum() < 0) {
                throw new BusinessException("未税单价不能为负数");
            }
            if (line.getOriginBatchId() != null && !AdjustTypeEnum.PROFIT.equals(line.getAdjustType())) {
                throw new BusinessException("仅盘盈纠正可指定原批次");
            }
            StockAdjustItemDO item = new StockAdjustItemDO();
            item.setTenantId(tenantId);
            item.setAdjustId(adjustId);
            item.setGoodsId(line.getGoodsId());
            item.setOriginBatchId(line.getOriginBatchId());
            item.setAdjustType(line.getAdjustType());
            item.setQuantity(line.getQuantity());
            item.setUntaxPrice(line.getUntaxPrice());
            item.setCostAmount(BigDecimal.ZERO);
            item.setSort(sort++);
            stockAdjustItemMapper.insert(item);
        }
    }

    private StockAdjustDO requireLocked(Long id) {
        StockAdjustDO entity = stockAdjustMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private StockAdjustDO require(Long id) {
        StockAdjustDO entity = stockAdjustMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("调整单不存在");
        }
        return entity;
    }

    private void fillWarehouseNames(List<StockAdjustResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> ids = list.stream().map(StockAdjustResp::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (WarehouseDO w : warehouseMapper.selectByIds(ids)) {
            names.put(w.getId(), w.getWarehouseName());
        }
        for (StockAdjustResp row : list) {
            if (row.getWarehouseId() != null) {
                row.setWarehouseName(names.get(row.getWarehouseId()));
            }
        }
    }

    private void assertDraft(StockAdjustDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿可操作");
        }
    }
}
