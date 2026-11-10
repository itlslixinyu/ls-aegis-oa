package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.mapper.StockTransferItemMapper;
import com.ls.aegis.biz.psi.mapper.StockTransferMapper;
import com.ls.aegis.biz.psi.model.entity.StockTransferDO;
import com.ls.aegis.biz.psi.model.entity.StockTransferItemDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.StockTransferQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockTransferReq;
import com.ls.aegis.biz.psi.model.resp.StockTransferResp;
import com.ls.aegis.biz.psi.service.StockTransferService;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 仓间调拨：调出 FIFO，调入保留原入库日与成本
 */
@Service
@RequiredArgsConstructor
public class StockTransferServiceImpl implements StockTransferService {

    private final StockTransferMapper stockTransferMapper;
    private final StockTransferItemMapper stockTransferItemMapper;
    private final PsiWarehouseMapper warehouseMapper;
    private final DocNoGenerator docNoGenerator;
    private final FifoStockHelper fifoStockHelper;
    private final IdempotentHelper idempotentHelper;

    @Override
    public PageResp<StockTransferResp> page(StockTransferQuery query, PageQuery pageQuery) {
        QueryWrapper<StockTransferDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<StockTransferDO> page = stockTransferMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery
            .getSize()), wrapper);
        PageResp<StockTransferResp> resp = PageResp.build(page, StockTransferResp.class);
        fillWarehouseNames(resp.getList());
        return resp;
    }

    @Override
    public StockTransferResp get(Long id) {
        StockTransferDO entity = require(id);
        StockTransferResp resp = BeanUtil.copyProperties(entity, StockTransferResp.class);
        List<StockTransferItemDO> items = stockTransferItemMapper.selectList(Wrappers
            .lambdaQuery(StockTransferItemDO.class)
            .eq(StockTransferItemDO::getTransferId, id)
            .orderByAsc(StockTransferItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, StockTransferResp.Item.class));
        fillWarehouseNames(List.of(resp));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockTransferReq req) {
        validate(req);
        Long tenantId = TenantFillHelper.currentTenantId();
        StockTransferDO entity = new StockTransferDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_STOCK_TRANSFER));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setTransferDate(req.getTransferDate());
        entity.setFromWarehouseId(req.getFromWarehouseId());
        entity.setToWarehouseId(req.getToWarehouseId());
        entity.setRemark(req.getRemark());
        stockTransferMapper.insert(entity);
        insertItems(tenantId, entity.getId(), req.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockTransferReq req, Long id) {
        StockTransferDO entity = requireLocked(id);
        assertDraft(entity);
        validate(req);
        entity.setTransferDate(req.getTransferDate());
        entity.setFromWarehouseId(req.getFromWarehouseId());
        entity.setToWarehouseId(req.getToWarehouseId());
        entity.setRemark(req.getRemark());
        stockTransferMapper.updateById(entity);
        stockTransferItemMapper.delete(Wrappers.lambdaQuery(StockTransferItemDO.class)
            .eq(StockTransferItemDO::getTransferId, id));
        insertItems(entity.getTenantId(), id, req.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StockTransferDO entity = requireLocked(id);
        assertDraft(entity);
        stockTransferItemMapper.delete(Wrappers.lambdaQuery(StockTransferItemDO.class)
            .eq(StockTransferItemDO::getTransferId, id));
        stockTransferMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id, DocConfirmReq req) {
        StockTransferDO locked = stockTransferMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("调拨单不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "stock-transfer-confirm-" + id, req.getIdempotentKey(), () -> doConfirm(locked, req
            .getIdempotentKey()));
    }

    private String doConfirm(StockTransferDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked);
        Long userId = UserContextHolder.getUserId();
        if (!ObjectUtil.equal(locked.getCreateUser(), userId)) {
            throw new BusinessException("仅制单人可确认生效");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        List<StockTransferItemDO> items = stockTransferItemMapper.selectList(Wrappers
            .lambdaQuery(StockTransferItemDO.class)
            .eq(StockTransferItemDO::getTransferId, locked.getId())
            .orderByAsc(StockTransferItemDO::getSort));
        fifoStockHelper.lockAll(items.stream().flatMap(item -> java.util.stream.Stream.of(
            new com.ls.aegis.biz.psi.support.StockMutationLock.Key(tenantId, locked.getFromWarehouseId(), item.getGoodsId()),
            new com.ls.aegis.biz.psi.support.StockMutationLock.Key(tenantId, locked.getToWarehouseId(), item.getGoodsId()))).toList());
        for (StockTransferItemDO item : items) {
            List<FifoStockHelper.ConsumeLine> consumes = fifoStockHelper.transferOutbound(tenantId, locked
                .getFromWarehouseId(), item.getGoodsId(), item.getQuantity(), locked.getId(), locked.getDocNo());
            BigDecimal cost = BigDecimal.ZERO;
            for (FifoStockHelper.ConsumeLine c : consumes) {
                fifoStockHelper.transferInbound(tenantId, locked.getToWarehouseId(), item.getGoodsId(), c
                    .quantity(), c.untaxPrice(), c.costAmount(), c.inDate(), c.originInDate() != null
                        ? c.originInDate()
                        : c.inDate(), locked.getId(), locked.getDocNo());
                cost = cost.add(c.costAmount());
            }
            item.setCostAmount(cost);
            stockTransferItemMapper.updateById(item);
        }
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(userId);
        locked.setConfirmTime(LocalDateTime.now());
        stockTransferMapper.updateById(locked);
        return locked.getDocNo();
    }

    private void insertItems(Long tenantId, Long transferId, List<StockTransferReq.Item> lines) {
        int sort = 1;
        for (StockTransferReq.Item line : lines) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("调拨数量必须大于 0");
            }
            StockTransferItemDO item = new StockTransferItemDO();
            item.setTenantId(tenantId);
            item.setTransferId(transferId);
            item.setGoodsId(line.getGoodsId());
            item.setQuantity(line.getQuantity());
            item.setCostAmount(BigDecimal.ZERO);
            item.setSort(sort++);
            stockTransferItemMapper.insert(item);
        }
    }

    private void validate(StockTransferReq req) {
        if (ObjectUtil.equal(req.getFromWarehouseId(), req.getToWarehouseId())) {
            throw new BusinessException("调出仓与调入仓不能相同");
        }
    }

    private void fillWarehouseNames(List<StockTransferResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (StockTransferResp row : list) {
            if (row.getFromWarehouseId() != null) {
                ids.add(row.getFromWarehouseId());
            }
            if (row.getToWarehouseId() != null) {
                ids.add(row.getToWarehouseId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (WarehouseDO w : warehouseMapper.selectByIds(ids)) {
            names.put(w.getId(), w.getWarehouseName());
        }
        for (StockTransferResp row : list) {
            if (row.getFromWarehouseId() != null) {
                row.setFromWarehouseName(names.get(row.getFromWarehouseId()));
            }
            if (row.getToWarehouseId() != null) {
                row.setToWarehouseName(names.get(row.getToWarehouseId()));
            }
        }
    }

    private StockTransferDO requireLocked(Long id) {
        StockTransferDO entity = stockTransferMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private StockTransferDO require(Long id) {
        StockTransferDO entity = stockTransferMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("调拨单不存在");
        }
        return entity;
    }

    private void assertDraft(StockTransferDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }
}
