package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.biz.psi.mapper.InvoiceOutItemMapper;
import com.ls.aegis.biz.psi.mapper.InvoiceOutLinkMapper;
import com.ls.aegis.biz.psi.mapper.InvoiceOutMapper;
import com.ls.aegis.biz.psi.mapper.StockOutMapper;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.InvoiceOutDO;
import com.ls.aegis.biz.psi.model.entity.InvoiceOutItemDO;
import com.ls.aegis.biz.psi.model.entity.InvoiceOutLinkDO;
import com.ls.aegis.biz.psi.model.entity.StockOutDO;
import com.ls.aegis.biz.psi.model.query.InvoiceQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InvoiceReq;
import com.ls.aegis.biz.psi.model.resp.InvoiceImportParseResp;
import com.ls.aegis.biz.psi.model.resp.InvoiceResp;
import com.ls.aegis.biz.psi.service.InvoiceOutService;
import com.ls.aegis.biz.psi.support.DocNoGenerator;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.biz.psi.support.InvoiceImportSupport;
import com.ls.aegis.biz.psi.support.InvoiceStatusSupport;
import com.ls.aegis.biz.psi.support.PsiTodoCloseSupport;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.context.UserContextHolder;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceOutServiceImpl implements InvoiceOutService {

    private final InvoiceOutMapper invoiceOutMapper;
    private final InvoiceOutItemMapper invoiceOutItemMapper;
    private final InvoiceOutLinkMapper invoiceOutLinkMapper;
    private final StockOutMapper stockOutMapper;
    private final UnitMapper unitMapper;
    private final DocNoGenerator docNoGenerator;
    private final IdempotentHelper idempotentHelper;
    private final InvoiceStatusSupport invoiceStatusSupport;
    private final PsiTodoCloseSupport psiTodoCloseSupport;
    private final com.ls.aegis.biz.psi.support.PsiAttachmentAccessGuard attachments;

    @Override
    public PageResp<InvoiceResp> page(InvoiceQuery query, PageQuery pageQuery) {
        QueryWrapper<InvoiceOutDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<InvoiceOutDO> page = invoiceOutMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<InvoiceResp> resp = PageResp.build(page, InvoiceResp.class);
        fillLinkAmounts(resp.getList());
        return resp;
    }

    @Override
    public InvoiceResp get(Long id) {
        InvoiceOutDO entity = require(id);
        InvoiceResp resp = BeanUtil.copyProperties(entity, InvoiceResp.class);
        List<InvoiceOutItemDO> items = invoiceOutItemMapper.selectList(Wrappers.lambdaQuery(InvoiceOutItemDO.class)
            .eq(InvoiceOutItemDO::getInvoiceId, id).orderByAsc(InvoiceOutItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, InvoiceResp.Item.class));
        List<InvoiceOutLinkDO> links = invoiceOutLinkMapper.selectList(Wrappers.lambdaQuery(InvoiceOutLinkDO.class)
            .eq(InvoiceOutLinkDO::getInvoiceId, id));
        List<InvoiceResp.Link> linkResps = new ArrayList<>();
        BigDecimal linkSum = BigDecimal.ZERO;
        for (InvoiceOutLinkDO link : links) {
            InvoiceResp.Link lr = new InvoiceResp.Link();
            lr.setId(link.getId());
            lr.setStockDocId(link.getStockOutId());
            lr.setAmount(link.getAmount());
            linkResps.add(lr);
            linkSum = linkSum.add(nz(link.getAmount()));
        }
        resp.setLinks(linkResps);
        resp.setLinkedAmount(linkSum);
        resp.setUnallocatedAmount(invoiceStatusSupport.unallocated(entity.getTaxInclusiveAmount(), linkSum));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(InvoiceReq req) {
        com.ls.aegis.biz.psi.support.InvoiceValidation.validate(req);
        Long tenantId = TenantFillHelper.currentTenantId();
        InvoiceOutDO entity = new InvoiceOutDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INVOICE_OUT));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        fill(entity, req);
        if (invoiceOutMapper.insert(entity) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
        saveChildren(tenantId, entity.getId(), req);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InvoiceReq req, Long id) {
        com.ls.aegis.biz.psi.support.InvoiceValidation.validate(req);
        InvoiceOutDO entity = requireLocked(id);
        assertDraft(entity);
        fill(entity, req);
        if (invoiceOutMapper.updateById(entity) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
        invoiceOutItemMapper.delete(Wrappers.lambdaQuery(InvoiceOutItemDO.class).eq(InvoiceOutItemDO::getInvoiceId, id));
        invoiceOutLinkMapper.delete(Wrappers.lambdaQuery(InvoiceOutLinkDO.class).eq(InvoiceOutLinkDO::getInvoiceId, id));
        saveChildren(entity.getTenantId(), id, req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        InvoiceOutDO entity = requireLocked(id);
        assertDraft(entity);
        invoiceOutItemMapper.delete(Wrappers.lambdaQuery(InvoiceOutItemDO.class).eq(InvoiceOutItemDO::getInvoiceId, id));
        invoiceOutLinkMapper.delete(Wrappers.lambdaQuery(InvoiceOutLinkDO.class).eq(InvoiceOutLinkDO::getInvoiceId, id));
        // 椤讳紶瀹炰綋锛岄€昏緫鍒犻櫎鎵嶈兘濉厖 updateUser锛堜粎浼?id 浼氬弬鏁扮粦瀹氬け璐ワ級
        invoiceOutMapper.deleteById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String confirm(Long id, DocConfirmReq req) {
        InvoiceOutDO locked = invoiceOutMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("销项发票不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "invoice-out-confirm-" + id, req.getIdempotentKey(), () -> {
            if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
                return locked.getDocNo();
            }
            assertDraft(locked);
            InvoiceReq persisted = BeanUtil.copyProperties(locked, InvoiceReq.class);
            var allItems = invoiceStatusSupport.outItems(locked.getTenantId(),id);
            if (allItems.stream().anyMatch(line -> !java.util.Objects.equals(line.getTenantId(),locked.getTenantId())))
                throw new BusinessException("发票明细租户关联异常，请先对账");
            persisted.setItems(BeanUtil.copyToList(allItems, InvoiceReq.Item.class));
            com.ls.aegis.biz.psi.support.InvoiceValidation.validate(persisted);
            if (!ObjectUtil.equal(locked.getCreateUser(), UserContextHolder.getUserId())) {
                throw new BusinessException("仅制单人可确认生效");
            }
            List<InvoiceOutLinkDO> links = invoiceStatusSupport.outLinks(locked.getTenantId(),locked.getId());
            lockLinkedSources(links, locked.getTenantId(), locked.getUnitId());
            BigDecimal linkSum = BigDecimal.ZERO;
            for (InvoiceOutLinkDO link : links) {
                linkSum = linkSum.add(nz(link.getAmount()));
            }
            invoiceStatusSupport.assertLinkSumForConfirm(locked.getTaxInclusiveAmount(), linkSum, "发票未关联出库单");
            for (InvoiceOutLinkDO link : links) {
                StockOutDO stockOut = stockOutMapper.selectByIdForUpdate(link.getStockOutId());
                if (stockOut == null) {
                    throw new BusinessException("关联出库单不存在");
                }
                invoiceStatusSupport.assertOutBalance(stockOut, link.getAmount(), locked.getId());
            }
            locked.setDocStatus(DocStatusEnum.CONFIRMED);
            locked.setIdempotentKey(req.getIdempotentKey());
            locked.setConfirmUser(UserContextHolder.getUserId());
            locked.setConfirmTime(LocalDateTime.now());
            if (invoiceOutMapper.updateById(locked) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
            for (InvoiceOutLinkDO link : links) {
                invoiceStatusSupport.refreshStockOut(link.getStockOutId());
            }
            return locked.getDocNo();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String red(Long id, DocConfirmReq req) {
        InvoiceOutDO blue = invoiceOutMapper.selectByIdForUpdate(id);
        if (blue == null) {
            throw new BusinessException("蓝字发票不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(blue.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "invoice-out-red-" + id, req.getIdempotentKey(), () -> {
            if (blue.getRedOfId() != null) throw new BusinessException("红字发票不能再次冲红");
            if (blue.getDocStatus() == DocStatusEnum.VOID) {
                String existing = invoiceStatusSupport.redNo(true,tenantId,id);
                if (existing != null) return existing;
            }
            if (blue.getDocStatus() != DocStatusEnum.CONFIRMED) throw new BusinessException("仅已确认蓝字发票可冲红");
            List<InvoiceOutLinkDO> links = invoiceStatusSupport.outLinks(blue.getTenantId(),blue.getId());
            lockLinkedSources(links, tenantId, blue.getUnitId());
            InvoiceOutDO red = new InvoiceOutDO();
            red.setTenantId(tenantId);
            red.setDeptId(TenantFillHelper.currentDeptId());
            red.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INVOICE_OUT));
            red.setDocStatus(DocStatusEnum.CONFIRMED);
            red.setInvoiceNo(blue.getInvoiceNo().length() <= 62 ? blue.getInvoiceNo() + "-R" : "R-" + blue.getId());
            red.setInvoiceType(blue.getInvoiceType());
            red.setInvoiceDate(LocalDateTime.now());
            red.setUnitId(blue.getUnitId());
            red.setBuyerTaxNo(blue.getBuyerTaxNo());
            red.setSellerTaxNo(blue.getSellerTaxNo());
            red.setTaxInclusiveAmount(blue.getTaxInclusiveAmount().negate());
            red.setUntaxAmount(blue.getUntaxAmount().negate());
            red.setTaxAmount(blue.getTaxAmount().negate());
            red.setRedOfId(blue.getId());
            red.setIdempotentKey(req.getIdempotentKey());
            red.setRemark("冲红：" + blue.getDocNo());
            red.setConfirmUser(UserContextHolder.getUserId());
            red.setConfirmTime(LocalDateTime.now());
            if (invoiceOutMapper.insert(red) != 1) throw new BusinessException("发票写入失败，请刷新后重试");

            blue.setDocStatus(DocStatusEnum.VOID);
            if (invoiceOutMapper.updateById(blue) != 1) throw new BusinessException("发票写入失败，请刷新后重试");


            for (InvoiceOutLinkDO link : links) {
                invoiceStatusSupport.refreshStockOut(link.getStockOutId());
                psiTodoCloseSupport.completeRedByStockOut(link.getStockOutId());
            }
            return red.getDocNo();
        });
    }

    private void lockLinkedSources(List<InvoiceOutLinkDO> links, Long tenantId, Long unitId) {
        var seen = new java.util.HashSet<Long>();
        if(links.stream().anyMatch(link -> link.getStockOutId()==null || !java.util.Objects.equals(link.getTenantId(),tenantId)))
            throw new BusinessException("发票关联原单或租户异常，请先对账");
        for (InvoiceOutLinkDO link : links.stream().sorted(java.util.Comparator.comparing(InvoiceOutLinkDO::getStockOutId)).toList()) {
            if (!seen.add(link.getStockOutId()) || link.getAmount() == null || link.getAmount().signum() <= 0)
                throw new BusinessException("关联原单重复或金额无效");
            StockOutDO doc = stockOutMapper.selectByIdForUpdate(link.getStockOutId());
            if (doc == null || !java.util.Objects.equals(doc.getTenantId(),tenantId) || !java.util.Objects.equals(doc.getUnitId(),unitId))
                throw new BusinessException("发票关联原单不存在、无权访问或往来单位不一致");
        }
    }

    private void fill(InvoiceOutDO entity, InvoiceReq req) {
        entity.setInvoiceNo(req.getInvoiceNo());
        entity.setInvoiceType(req.getInvoiceType());
        entity.setInvoiceDate(req.getInvoiceDate());
        entity.setUnitId(req.getUnitId());
        entity.setBuyerTaxNo(req.getBuyerTaxNo());
        entity.setSellerTaxNo(req.getSellerTaxNo());
        entity.setTaxInclusiveAmount(req.getTaxInclusiveAmount());
        entity.setUntaxAmount(req.getUntaxAmount());
        entity.setTaxAmount(req.getTaxAmount());
        attachments.require(req.getFileId());
        entity.setFileId(req.getFileId());
        entity.setRemark(req.getRemark());
    }

    private void saveChildren(Long tenantId, Long invoiceId, InvoiceReq req) {
        int sort = 1;
        if (CollUtil.isNotEmpty(req.getItems())) {
            for (InvoiceReq.Item line : req.getItems()) {
                InvoiceOutItemDO item = new InvoiceOutItemDO();
                item.setTenantId(tenantId);
                item.setInvoiceId(invoiceId);
                item.setGoodsName(line.getGoodsName());
                item.setQuantity(line.getQuantity());
                item.setTaxInclusiveAmount(line.getTaxInclusiveAmount());
                item.setUntaxAmount(line.getUntaxAmount());
                item.setTaxAmount(line.getTaxAmount());
                item.setTaxRate(line.getTaxRate());
                item.setSort(sort++);
                if (invoiceOutItemMapper.insert(item) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
            }
        }
        BigDecimal linkSum = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(req.getLinks())) {
            for (InvoiceReq.Link link : req.getLinks()) {
                InvoiceOutLinkDO row = new InvoiceOutLinkDO();
                row.setTenantId(tenantId);
                row.setInvoiceId(invoiceId);
                row.setStockOutId(link.getStockDocId());
                row.setAmount(link.getAmount());
                if (invoiceOutLinkMapper.insert(row) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
                linkSum = linkSum.add(nz(link.getAmount()));
            }
        }
        invoiceStatusSupport.assertLinkSumForSave(req.getTaxInclusiveAmount(), linkSum);
    }

    private void fillLinkAmounts(List<InvoiceResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        List<Long> ids = list.stream().map(InvoiceResp::getId).filter(ObjectUtil::isNotNull).toList();
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<InvoiceOutLinkDO> all = invoiceOutLinkMapper.selectList(Wrappers.lambdaQuery(InvoiceOutLinkDO.class)
            .in(InvoiceOutLinkDO::getInvoiceId, ids));
        java.util.Map<Long, BigDecimal> sumMap = new java.util.HashMap<>();
        for (InvoiceOutLinkDO link : all) {
            sumMap.merge(link.getInvoiceId(), nz(link.getAmount()), BigDecimal::add);
        }
        for (InvoiceResp row : list) {
            BigDecimal linked = sumMap.getOrDefault(row.getId(), BigDecimal.ZERO);
            row.setLinkedAmount(linked);
            row.setUnallocatedAmount(invoiceStatusSupport.unallocated(row.getTaxInclusiveAmount(), linked));
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private InvoiceOutDO requireLocked(Long id) {
        InvoiceOutDO entity = invoiceOutMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private InvoiceOutDO require(Long id) {
        InvoiceOutDO entity = invoiceOutMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("销项发票不存在");
        }
        return entity;
    }

    private void assertDraft(InvoiceOutDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }

    @Override
    public InvoiceImportParseResp parseImportExcel(MultipartFile file) {
        return InvoiceImportSupport.parse(file, unitMapper, TenantFillHelper.currentTenantId(), UnitTypeEnum.CUSTOMER);
    }
}
