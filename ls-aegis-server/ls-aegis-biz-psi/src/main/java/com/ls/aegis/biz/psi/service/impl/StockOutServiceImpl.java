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
import com.ls.aegis.biz.psi.enums.InvoiceStatusEnum;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import com.ls.aegis.biz.psi.enums.StockOutTypeEnum;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.ContractSaleMapper;
import com.ls.aegis.biz.psi.mapper.PsiReceivableMapper;
import com.ls.aegis.biz.psi.mapper.StockOutBatchMapper;
import com.ls.aegis.biz.psi.mapper.StockOutItemMapper;
import com.ls.aegis.biz.psi.mapper.StockOutMapper;
import com.ls.aegis.biz.psi.model.entity.ContractSaleDO;
import com.ls.aegis.biz.psi.model.entity.ReceivableDO;
import com.ls.aegis.biz.psi.model.entity.StockOutBatchDO;
import com.ls.aegis.biz.psi.model.entity.StockOutDO;
import com.ls.aegis.biz.psi.model.entity.StockOutItemDO;
import com.ls.aegis.biz.psi.model.query.StockOutQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.StockOutReq;
import com.ls.aegis.biz.psi.model.resp.StockLineImportParseResp;
import com.ls.aegis.biz.psi.model.resp.StockLineResp;
import com.ls.aegis.biz.psi.model.resp.StockOutResp;
import com.ls.aegis.biz.psi.service.StockOutService;
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
 * 出库单实现（确认时 FIFO）
 */
@Service
@RequiredArgsConstructor
public class StockOutServiceImpl implements StockOutService {

    private final StockOutMapper stockOutMapper;
    private final StockOutItemMapper stockOutItemMapper;
    private final StockOutBatchMapper stockOutBatchMapper;
    private final PsiReceivableMapper receivableMapper;
    private final ContractSaleMapper contractSaleMapper;
    private final GoodsMapper goodsMapper;
    private final DocNoGenerator docNoGenerator;
    private final FifoStockHelper fifoStockHelper;
    private final IdempotentHelper idempotentHelper;

    @Override
    public PageResp<StockOutResp> page(StockOutQuery query, PageQuery pageQuery) {
        QueryWrapper<StockOutDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<StockOutDO> page = stockOutMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<StockOutResp> resp = PageResp.build(page, StockOutResp.class);
        fillContractNames(resp.getList());
        return resp;
    }

    @Override
    public StockOutResp get(Long id) {
        StockOutDO entity = require(id);
        StockOutResp resp = BeanUtil.copyProperties(entity, StockOutResp.class);
        List<StockOutItemDO> items = stockOutItemMapper.selectList(Wrappers.lambdaQuery(StockOutItemDO.class)
            .eq(StockOutItemDO::getStockOutId, id)
            .orderByAsc(StockOutItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, StockLineResp.class));
        fillContractNames(List.of(resp));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StockOutReq req) {
        validateBiz(req);
        DocTaxHelper.HeaderResult tax = DocTaxHelper.applyManualDiff(DocTaxHelper.recalc(req.getItems()), req
            .getTaxInclusiveAmount());
        Long tenantId = TenantFillHelper.currentTenantId();
        StockOutDO entity = new StockOutDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_STOCK_OUT));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        entity.setOutType(req.getOutType());
        entity.setOutDate(req.getOutDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setUnitId(req.getUnitId());
        entity.setContractId(req.getContractId());
        entity.setTaxInclusiveAmount(tax.taxInclusiveAmount());
        entity.setUntaxAmount(tax.untaxAmount());
        entity.setTaxAmount(tax.taxAmount());
        entity.setTaxDiffAmount(tax.taxDiffAmount());
        entity.setCostAmount(BigDecimal.ZERO);
        entity.setInvoiceStatus(InvoiceStatusEnum.UNINVOICED);
        entity.setPayStatus(com.ls.aegis.biz.psi.enums.PayStatusEnum.UNPAID);
        entity.setRemark(req.getRemark());
        stockOutMapper.insert(entity);
        insertItems(tenantId, entity.getId(), tax.lines());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StockOutReq req, Long id) {
        StockOutDO entity = requireLocked(id);
        assertDraft(entity);
        validateBiz(req);
        DocTaxHelper.HeaderResult tax = DocTaxHelper.applyManualDiff(DocTaxHelper.recalc(req.getItems()), req
            .getTaxInclusiveAmount());
        entity.setOutType(req.getOutType());
        entity.setOutDate(req.getOutDate());
        entity.setWarehouseId(req.getWarehouseId());
        entity.setUnitId(req.getUnitId());
        entity.setContractId(req.getContractId());
        entity.setTaxInclusiveAmount(tax.taxInclusiveAmount());
        entity.setUntaxAmount(tax.untaxAmount());
        entity.setTaxAmount(tax.taxAmount());
        entity.setTaxDiffAmount(tax.taxDiffAmount());
        entity.setRemark(req.getRemark());
        stockOutMapper.updateById(entity);
        stockOutItemMapper.delete(Wrappers.lambdaQuery(StockOutItemDO.class).eq(StockOutItemDO::getStockOutId, id));
        insertItems(entity.getTenantId(), id, tax.lines());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StockOutDO entity = requireLocked(id);
        assertDraft(entity);
        stockOutItemMapper.delete(Wrappers.lambdaQuery(StockOutItemDO.class).eq(StockOutItemDO::getStockOutId, id));
        stockOutMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id, DocConfirmReq req) {
        StockOutDO locked = stockOutMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("出库单不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "stock-out-confirm-" + id, req.getIdempotentKey(), () -> doConfirm(locked, req
            .getIdempotentKey()));
    }

    private String doConfirm(StockOutDO locked, String idempotentKey) {
        if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
            return locked.getDocNo();
        }
        assertDraft(locked);
        Long userId = UserContextHolder.getUserId();
        if (!StockOutTypeEnum.REPAIR.equals(locked.getOutType()) && !ObjectUtil.equal(locked.getCreateUser(), userId)) {
            throw new BusinessException("仅制单人可确认生效");
        }
        List<StockOutItemDO> items = stockOutItemMapper.selectList(Wrappers.lambdaQuery(StockOutItemDO.class)
            .eq(StockOutItemDO::getStockOutId, locked.getId())
            .orderByAsc(StockOutItemDO::getSort));
        if (CollUtil.isEmpty(items)) {
            throw new BusinessException("出库明细为空");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        BigDecimal totalCost = BigDecimal.ZERO;
        fifoStockHelper.lockAll(items.stream().map(item -> new com.ls.aegis.biz.psi.support.StockMutationLock.Key(
            tenantId, locked.getWarehouseId(), item.getGoodsId())).toList());
        for (StockOutItemDO item : items) {
            List<FifoStockHelper.ConsumeLine> consumes = fifoStockHelper.outbound(tenantId, locked
                .getWarehouseId(), item.getGoodsId(), item.getQuantity(), BizSourceTypeEnum.STOCK_OUT, locked
                    .getId(), locked.getDocNo());
            BigDecimal itemCost = BigDecimal.ZERO;
            for (FifoStockHelper.ConsumeLine c : consumes) {
                StockOutBatchDO outBatch = new StockOutBatchDO();
                outBatch.setTenantId(tenantId);
                outBatch.setStockOutItemId(item.getId());
                outBatch.setBatchId(c.batchId());
                outBatch.setQuantity(c.quantity());
                outBatch.setUntaxPrice(c.untaxPrice());
                outBatch.setCostAmount(c.costAmount());
                stockOutBatchMapper.insert(outBatch);
                itemCost = itemCost.add(c.costAmount());
            }
            item.setCostAmount(itemCost);
            stockOutItemMapper.updateById(item);
            totalCost = totalCost.add(itemCost);
        }
        if (StockOutTypeEnum.SALE.equals(locked.getOutType()) && locked.getUnitId() != null) {
            ReceivableDO ar = new ReceivableDO();
            ar.setTenantId(tenantId);
            ar.setDeptId(TenantFillHelper.currentDeptId());
            ar.setUnitId(locked.getUnitId());
            ar.setSourceType(BizSourceTypeEnum.STOCK_OUT);
            ar.setSourceId(locked.getId());
            ar.setSourceNo(locked.getDocNo());
            ar.setTaxInclusiveAmount(locked.getTaxInclusiveAmount());
            ar.setWrittenOffAmount(BigDecimal.ZERO);
            ar.setUnwrittenAmount(locked.getTaxInclusiveAmount());
            ar.setDocStatus(OpenItemStatusEnum.OPEN);
            receivableMapper.insert(ar);
        }
        locked.setCostAmount(totalCost);
        locked.setDocStatus(DocStatusEnum.CONFIRMED);
        locked.setIdempotentKey(idempotentKey);
        locked.setConfirmUser(userId);
        locked.setConfirmTime(LocalDateTime.now());
        stockOutMapper.updateById(locked);
        return locked.getDocNo();
    }

    private void insertItems(Long tenantId, Long stockOutId, List<DocTaxHelper.LineResult> lines) {
        for (DocTaxHelper.LineResult line : lines) {
            StockOutItemDO item = new StockOutItemDO();
            item.setTenantId(tenantId);
            item.setStockOutId(stockOutId);
            item.setGoodsId(line.goodsId());
            item.setQuantity(line.quantity());
            item.setTaxInclusivePrice(line.taxInclusivePrice());
            item.setTaxRate(line.taxRate());
            item.setUntaxPrice(line.untaxPrice());
            item.setUntaxAmount(line.untaxAmount());
            item.setTaxAmount(line.taxAmount());
            item.setTaxInclusiveAmount(line.taxInclusiveAmount());
            item.setCostAmount(BigDecimal.ZERO);
            item.setSort(line.sort());
            stockOutItemMapper.insert(item);
        }
    }

    private void validateBiz(StockOutReq req) {
        if (!StockOutTypeEnum.SALE.equals(req.getOutType())) {
            req.setContractId(null);
            return;
        }
        if (req.getUnitId() == null) {
            throw new BusinessException("销售出库必须选择客户");
        }
        if (req.getContractId() == null) {
            throw new BusinessException("销售出库必须绑定销售合同");
        }
        ContractSaleDO contract = contractSaleMapper.selectById(req.getContractId());
        if (contract == null) {
            throw new BusinessException("销售合同不存在");
        }
        if (!DocStatusEnum.CONFIRMED.equals(contract.getDocStatus())) {
            throw new BusinessException("只能绑定已确认的销售合同");
        }
        if (!ObjectUtil.equal(contract.getUnitId(), req.getUnitId())) {
            throw new BusinessException("销售合同与客户不匹配");
        }
    }

    private void fillContractNames(List<StockOutResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> contractIds = list.stream()
            .map(StockOutResp::getContractId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (contractIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>();
        for (ContractSaleDO c : contractSaleMapper.selectByIds(contractIds)) {
            nameMap.put(c.getId(), c.getName());
        }
        for (StockOutResp row : list) {
            if (row.getContractId() != null) {
                row.setContractName(nameMap.get(row.getContractId()));
            }
        }
    }

    private StockOutDO requireLocked(Long id) {
        StockOutDO entity = stockOutMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private StockOutDO require(Long id) {
        StockOutDO entity = stockOutMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("出库单不存在");
        }
        return entity;
    }

    private void assertDraft(StockOutDO entity) {
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
