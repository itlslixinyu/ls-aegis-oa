package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import com.ls.aegis.biz.psi.enums.PayStatusEnum;
import com.ls.aegis.biz.psi.enums.StockInTypeEnum;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.PayableMapper;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.mapper.StockInItemMapper;
import com.ls.aegis.biz.psi.mapper.StockInMapper;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.PayableDO;
import com.ls.aegis.biz.psi.model.entity.StockInDO;
import com.ls.aegis.biz.psi.model.entity.StockInItemDO;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.StockInQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockInReq;
import com.ls.aegis.biz.psi.model.resp.StockInResp;
import com.ls.aegis.biz.psi.model.resp.StockLineImportParseResp;
import com.ls.aegis.biz.psi.model.resp.StockLineResp;
import com.ls.aegis.biz.psi.service.StockInService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.biz.psi.support.DocTaxHelper;
import com.ls.aegis.biz.psi.support.FifoStockHelper;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.StockLineImportSupport;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 入库单实现
 */
@Service
@RequiredArgsConstructor
public class StockInServiceImpl implements StockInService {

    private final StockInMapper stockInMapper;
    private final StockInItemMapper stockInItemMapper;
    private final PayableMapper payableMapper;
    private final GoodsMapper goodsMapper;
    private final PsiWarehouseMapper warehouseMapper;
    private final UnitMapper unitMapper;
    private final DocNoGenerator docNoGenerator;
    private final FifoStockHelper fifoStockHelper;
    private final IdempotentHelper idempotentHelper;

    @Override
    public PageResp<StockInResp> page(StockInQuery query, PageQuery pageQuery) {
        QueryWrapper<StockInDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<StockInDO> page = stockInMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<StockInResp> resp = PageResp.build(page, StockInResp.class);
        fillHeaderNames(resp.getList());
        return resp;
    }

    @Override
    public StockInResp get(Long id) {
        StockInDO entity = require(id);
        StockInResp resp = BeanUtil.copyProperties(entity, StockInResp.class);
        List<StockInItemDO> items = stockInItemMapper.selectList(Wrappers.lambdaQuery(StockInItemDO.class)
            .eq(StockInItemDO::getStockInId, id)
            .orderByAsc(StockInItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, StockLineResp.class));
        fillHeaderNames(List.of(resp));
        fillItemGoodsNames(resp.getItems());
        return resp;
    }

    private void fillHeaderNames(List<StockInResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> warehouseIds = list.stream().map(StockInResp::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> unitIds = list.stream().map(StockInResp::getUnitId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> warehouseNames = new HashMap<>();
        Map<Long, String> unitNames = new HashMap<>();
        if (CollUtil.isNotEmpty(warehouseIds)) {
            for (WarehouseDO w : warehouseMapper.selectByIds(warehouseIds)) {
                warehouseNames.put(w.getId(), w.getWarehouseName());
            }
        }
        if (CollUtil.isNotEmpty(unitIds)) {
            for (UnitDO u : unitMapper.selectByIds(unitIds)) {
                unitNames.put(u.getId(), u.getUnitName());
            }
        }
        for (StockInResp row : list) {
            if (row.getWarehouseId() != null) {
                row.setWarehouseName(warehouseNames.get(row.getWarehouseId()));
            }
            if (row.getUnitId() != null) {
                row.setUnitName(unitNames.get(row.getUnitId()));
            }
        }
    }

    private void fillItemGoodsNames(List<StockLineResp> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> goodsIds = items.stream().map(StockLineResp::getGoodsId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollUtil.isEmpty(goodsIds)) {
            return;
        }
        Map<Long, String> goodsNames = new HashMap<>();
        for (GoodsDO g : goodsMapper.selectByIds(goodsIds)) {
            goodsNames.put(g.getId(), g.getGoodsName());
        }
        for (StockLineResp line : items) {
            if (line.getGoodsId() != null) {
                line.setGoodsName(goodsNames.get(line.getGoodsId()));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockInReq req) {
        validateBiz(req);
        DocTaxHelper.HeaderResult tax = DocTaxHelper.applyManualDiff(DocTaxHelper.recalc(req.getItems()), req
            .getTaxInclusiveAmount());
        Long tenantId = TenantFillHelper.currentTenantId();
        StockInDO entity = new StockInDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_STOCK_IN));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setInType(req.getInType());
        entity.setInDate(req.getInDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setUnitId(req.getUnitId());
        entity.setContractId(req.getContractId());
        entity.setTaxInclusiveAmount(tax.taxInclusiveAmount());
        entity.setUntaxAmount(tax.untaxAmount());
        entity.setTaxAmount(tax.taxAmount());
        entity.setTaxDiffAmount(tax.taxDiffAmount());
        entity.setPayStatus(PayStatusEnum.UNPAID);
        entity.setInvoiceStatus(com.ls.aegis.biz.psi.enums.InvoiceStatusEnum.UNINVOICED);
        entity.setRemark(req.getRemark());
        stockInMapper.insert(entity);
        insertItems(tenantId, entity.getId(), tax.lines());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockInReq req, Long id) {
        StockInDO entity = requireLocked(id);
        assertDraft(entity);
        validateBiz(req);
        DocTaxHelper.HeaderResult tax = DocTaxHelper.applyManualDiff(DocTaxHelper.recalc(req.getItems()), req
            .getTaxInclusiveAmount());
        entity.setInType(req.getInType());
        entity.setInDate(req.getInDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setUnitId(req.getUnitId());
        entity.setContractId(req.getContractId());
        entity.setTaxInclusiveAmount(tax.taxInclusiveAmount());
        entity.setUntaxAmount(tax.untaxAmount());
        entity.setTaxAmount(tax.taxAmount());
        entity.setTaxDiffAmount(tax.taxDiffAmount());
        entity.setRemark(req.getRemark());
        stockInMapper.updateById(entity);
        stockInItemMapper.delete(Wrappers.lambdaQuery(StockInItemDO.class).eq(StockInItemDO::getStockInId, id));
        insertItems(entity.getTenantId(), id, tax.lines());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StockInDO entity = requireLocked(id);
        assertDraft(entity);
        stockInItemMapper.delete(Wrappers.lambdaQuery(StockInItemDO.class).eq(StockInItemDO::getStockInId, id));
        stockInMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id, DocConfirmReq req) {
        StockInDO locked = stockInMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("入库单不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "stock-in-confirm-" + id, req.getIdempotentKey(), () -> doConfirm(locked, req
            .getIdempotentKey()));
    }

    private String doConfirm(StockInDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked);
        Long userId = UserContextHolder.getUserId();
        if (!ObjectUtil.equal(locked.getCreateUser(), userId)) {
            throw new BusinessException("仅制单人可确认生效");
        }
        List<StockInItemDO> items = stockInItemMapper.selectList(Wrappers.lambdaQuery(StockInItemDO.class)
            .eq(StockInItemDO::getStockInId, locked.getId())
            .orderByAsc(StockInItemDO::getSort));
        if (CollUtil.isEmpty(items)) {
            throw new BusinessException("入库明细为空");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        fifoStockHelper.lockAll(items.stream().map(item -> new com.ls.aegis.biz.psi.support.StockMutationLock.Key(
            tenantId, locked.getWarehouseId(), item.getGoodsId())).toList());
        for (StockInItemDO item : items) {
            fifoStockHelper.inbound(tenantId, locked.getWarehouseId(), item.getGoodsId(), item.getQuantity(), item
                .getUntaxPrice(), item.getUntaxAmount(), locked.getInDate(), BizSourceTypeEnum.STOCK_IN, locked
                    .getId(), locked.getDocNo());
        }
        if (StockInTypeEnum.PURCHASE.equals(locked.getInType()) && locked.getUnitId() != null) {
            PayableDO payable = new PayableDO();
            payable.setTenantId(tenantId);
            payable.setDeptId(TenantFillHelper.currentDeptId());
            payable.setUnitId(locked.getUnitId());
            payable.setSourceType(BizSourceTypeEnum.STOCK_IN);
            payable.setSourceId(locked.getId());
            payable.setSourceNo(locked.getDocNo());
            payable.setTaxInclusiveAmount(locked.getTaxInclusiveAmount());
            payable.setWrittenOffAmount(BigDecimal.ZERO);
            payable.setUnwrittenAmount(locked.getTaxInclusiveAmount());
            payable.setDocStatus(OpenItemStatusEnum.OPEN);
            payableMapper.insert(payable);
        }
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(userId);
        locked.setConfirmTime(LocalDateTime.now());
        stockInMapper.updateById(locked);
        return locked.getDocNo();
    }

    private void insertItems(Long tenantId, Long stockInId, List<DocTaxHelper.LineResult> lines) {
        for (DocTaxHelper.LineResult line : lines) {
            StockInItemDO item = new StockInItemDO();
            item.setTenantId(tenantId);
            item.setStockInId(stockInId);
            item.setGoodsId(line.goodsId());
            item.setQuantity(line.quantity());
            item.setTaxInclusivePrice(line.taxInclusivePrice());
            item.setTaxRate(line.taxRate());
            item.setUntaxPrice(line.untaxPrice());
            item.setUntaxAmount(line.untaxAmount());
            item.setTaxAmount(line.taxAmount());
            item.setTaxInclusiveAmount(line.taxInclusiveAmount());
            item.setSort(line.sort());
            stockInItemMapper.insert(item);
        }
    }

    private void validateBiz(StockInReq req) {
        if (StockInTypeEnum.PURCHASE.equals(req.getInType()) && req.getUnitId() == null) {
            throw new BusinessException("采购入库必须选择供应商");
        }
    }

    private StockInDO requireLocked(Long id) {
        StockInDO entity = stockInMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private StockInDO require(Long id) {
        StockInDO entity = stockInMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("入库单不存在");
        }
        return entity;
    }

    private void assertDraft(StockInDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) {
        StockLineImportSupport.downloadTemplate(response);
    }

    @Override
    public StockLineImportParseResp parseImportExcel(MultipartFile file) {
        return StockLineImportSupport.parse(file, goodsMapper, TenantFillHelper.currentTenantId());
    }
}
