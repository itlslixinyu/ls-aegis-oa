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
import com.ls.aegis.biz.psi.mapper.InvoiceInItemMapper;
import com.ls.aegis.biz.psi.mapper.InvoiceInLinkMapper;
import com.ls.aegis.biz.psi.mapper.InvoiceInMapper;
import com.ls.aegis.biz.psi.mapper.StockInMapper;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.InvoiceInDO;
import com.ls.aegis.biz.psi.model.entity.InvoiceInItemDO;
import com.ls.aegis.biz.psi.model.entity.InvoiceInLinkDO;
import com.ls.aegis.biz.psi.model.entity.StockInDO;
import com.ls.aegis.biz.psi.model.query.InvoiceQuery;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.InvoiceReq;
import com.ls.aegis.biz.psi.model.resp.InvoiceImportParseResp;
import com.ls.aegis.biz.psi.model.resp.InvoiceResp;
import com.ls.aegis.biz.psi.service.InvoiceInService;
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
public class InvoiceInServiceImpl implements InvoiceInService {

    private final InvoiceInMapper invoiceInMapper;
    private final InvoiceInItemMapper invoiceInItemMapper;
    private final InvoiceInLinkMapper invoiceInLinkMapper;
    private final StockInMapper stockInMapper;
    private final UnitMapper unitMapper;
    private final DocNoGenerator docNoGenerator;
    private final IdempotentHelper idempotentHelper;
    private final InvoiceStatusSupport invoiceStatusSupport;
    private final PsiTodoCloseSupport psiTodoCloseSupport;
    private final com.ls.aegis.biz.psi.support.PsiAttachmentAccessGuard attachments;

    @Override
    public PageResp<InvoiceResp> page(InvoiceQuery query, PageQuery pageQuery) {
        QueryWrapper<InvoiceInDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<InvoiceInDO> page = invoiceInMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<InvoiceResp> resp = PageResp.build(page, InvoiceResp.class);
        fillLinkAmounts(resp.getList());
        return resp;
    }

    @Override
    public InvoiceResp get(Long id) {
        InvoiceInDO entity = require(id);
        InvoiceResp resp = BeanUtil.copyProperties(entity, InvoiceResp.class);
        List<InvoiceInItemDO> items = invoiceInItemMapper.selectList(Wrappers.lambdaQuery(InvoiceInItemDO.class)
            .eq(InvoiceInItemDO::getInvoiceId, id).orderByAsc(InvoiceInItemDO::getSort));
        resp.setItems(BeanUtil.copyToList(items, InvoiceResp.Item.class));
        List<InvoiceInLinkDO> links = invoiceInLinkMapper.selectList(Wrappers.lambdaQuery(InvoiceInLinkDO.class)
            .eq(InvoiceInLinkDO::getInvoiceId, id));
        List<InvoiceResp.Link> linkResps = new ArrayList<>();
        BigDecimal linkSum = BigDecimal.ZERO;
        for (InvoiceInLinkDO link : links) {
            InvoiceResp.Link lr = new InvoiceResp.Link();
            lr.setId(link.getId());
            lr.setStockDocId(link.getStockInId());
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
        InvoiceInDO entity = new InvoiceInDO();
        entity.setTenantId(tenantId);
        entity.setDeptId(TenantFillHelper.currentDeptId());
        entity.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INVOICE_IN));
        entity.setDocStatus(DocStatusEnum.DRAFT);
        fill(entity, req);
        if (invoiceInMapper.insert(entity) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
        saveChildren(tenantId, entity.getId(), req);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(InvoiceReq req, Long id) {
        com.ls.aegis.biz.psi.support.InvoiceValidation.validate(req);
        InvoiceInDO entity = requireLocked(id);
        assertDraft(entity);
        fill(entity, req);
        if (invoiceInMapper.updateById(entity) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
        invoiceInItemMapper.delete(Wrappers.lambdaQuery(InvoiceInItemDO.class).eq(InvoiceInItemDO::getInvoiceId, id));
        invoiceInLinkMapper.delete(Wrappers.lambdaQuery(InvoiceInLinkDO.class).eq(InvoiceInLinkDO::getInvoiceId, id));
        saveChildren(entity.getTenantId(), id, req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        InvoiceInDO entity = requireLocked(id);
        assertDraft(entity);
        invoiceInItemMapper.delete(Wrappers.lambdaQuery(InvoiceInItemDO.class).eq(InvoiceInItemDO::getInvoiceId, id));
        invoiceInLinkMapper.delete(Wrappers.lambdaQuery(InvoiceInLinkDO.class).eq(InvoiceInLinkDO::getInvoiceId, id));
        // 椤讳紶瀹炰綋锛岄€昏緫鍒犻櫎鎵嶈兘濉厖 updateUser锛堜粎浼?id 浼氬弬鏁扮粦瀹氬け璐ワ級
        invoiceInMapper.deleteById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String confirm(Long id, DocConfirmReq req) {
        InvoiceInDO locked = invoiceInMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new BusinessException("进项发票不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(locked.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "invoice-in-confirm-" + id, req.getIdempotentKey(), () -> {
            if (DocStatusEnum.CONFIRMED.equals(locked.getDocStatus())) {
                return locked.getDocNo();
            }
            assertDraft(locked);
            InvoiceReq persisted = BeanUtil.copyProperties(locked, InvoiceReq.class);
            var allItems = invoiceStatusSupport.inItems(locked.getTenantId(),id);
            if (allItems.stream().anyMatch(line -> !java.util.Objects.equals(line.getTenantId(),locked.getTenantId())))
                throw new BusinessException("发票明细租户关联异常，请先对账");
            persisted.setItems(BeanUtil.copyToList(allItems, InvoiceReq.Item.class));
            com.ls.aegis.biz.psi.support.InvoiceValidation.validate(persisted);
            if (!ObjectUtil.equal(locked.getCreateUser(), UserContextHolder.getUserId())) {
                throw new BusinessException("仅制单人可确认生效");
            }
            List<InvoiceInLinkDO> links = invoiceStatusSupport.inLinks(locked.getTenantId(),locked.getId());
            lockLinkedSources(links, locked.getTenantId(), locked.getUnitId());
            BigDecimal linkSum = BigDecimal.ZERO;
            for (InvoiceInLinkDO link : links) {
                linkSum = linkSum.add(nz(link.getAmount()));
            }
            invoiceStatusSupport.assertLinkSumForConfirm(locked.getTaxInclusiveAmount(), linkSum, "发票未关联入库单");
            for (InvoiceInLinkDO link : links) {
                StockInDO stockOut = stockInMapper.selectByIdForUpdate(link.getStockInId());
                if (stockOut == null) {
                    throw new BusinessException("关联入库单不存在");
                }
                invoiceStatusSupport.assertInBalance(stockOut, link.getAmount(), locked.getId());
            }
            locked.setDocStatus(DocStatusEnum.CONFIRMED);
            locked.setIdempotentKey(req.getIdempotentKey());
            locked.setConfirmUser(UserContextHolder.getUserId());
            locked.setConfirmTime(LocalDateTime.now());
            if (invoiceInMapper.updateById(locked) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
            for (InvoiceInLinkDO link : links) {
                invoiceStatusSupport.refreshStockIn(link.getStockInId());
            }
            return locked.getDocNo();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String red(Long id, DocConfirmReq req) {
        InvoiceInDO blue = invoiceInMapper.selectByIdForUpdate(id);
        if (blue == null) {
            throw new BusinessException("蓝字发票不存在");
        }
        Long tenantId = ObjectUtil.defaultIfNull(blue.getTenantId(), 0L);
        return idempotentHelper.execute(tenantId, "invoice-in-red-" + id, req.getIdempotentKey(), () -> {
            if (blue.getRedOfId() != null) throw new BusinessException("红字发票不能再次冲红");
            if (blue.getDocStatus() == DocStatusEnum.VOID) {
                String existing = invoiceStatusSupport.redNo(false,tenantId,id);
                if (existing != null) return existing;
            }
            if (blue.getDocStatus() != DocStatusEnum.CONFIRMED) throw new BusinessException("仅已确认蓝字发票可冲红");
            List<InvoiceInLinkDO> links = invoiceStatusSupport.inLinks(blue.getTenantId(),blue.getId());
            lockLinkedSources(links, tenantId, blue.getUnitId());
            InvoiceInDO red = new InvoiceInDO();
            red.setTenantId(tenantId);
            red.setDeptId(TenantFillHelper.currentDeptId());
            red.setDocNo(docNoGenerator.next(tenantId, PsiConstants.DOC_PREFIX_INVOICE_IN));
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
            if (invoiceInMapper.insert(red) != 1) throw new BusinessException("发票写入失败，请刷新后重试");

            blue.setDocStatus(DocStatusEnum.VOID);
            if (invoiceInMapper.updateById(blue) != 1) throw new BusinessException("发票写入失败，请刷新后重试");


            for (InvoiceInLinkDO link : links) {
                invoiceStatusSupport.refreshStockIn(link.getStockInId());
                psiTodoCloseSupport.completeRedByStockIn(link.getStockInId());
            }
            return red.getDocNo();
        });
    }

    private void lockLinkedSources(List<InvoiceInLinkDO> links, Long tenantId, Long unitId) {
        var seen = new java.util.HashSet<Long>();
        if(links.stream().anyMatch(link -> link.getStockInId()==null || !java.util.Objects.equals(link.getTenantId(),tenantId)))
            throw new BusinessException("发票关联原单或租户异常，请先对账");
        for (InvoiceInLinkDO link : links.stream().sorted(java.util.Comparator.comparing(InvoiceInLinkDO::getStockInId)).toList()) {
            if (!seen.add(link.getStockInId()) || link.getAmount() == null || link.getAmount().signum() <= 0)
                throw new BusinessException("关联原单重复或金额无效");
            StockInDO doc = stockInMapper.selectByIdForUpdate(link.getStockInId());
            if (doc == null || !java.util.Objects.equals(doc.getTenantId(),tenantId) || !java.util.Objects.equals(doc.getUnitId(),unitId))
                throw new BusinessException("发票关联原单不存在、无权访问或往来单位不一致");
        }
    }

    private void fill(InvoiceInDO entity, InvoiceReq req) {
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
                InvoiceInItemDO item = new InvoiceInItemDO();
                item.setTenantId(tenantId);
                item.setInvoiceId(invoiceId);
                item.setGoodsName(line.getGoodsName());
                item.setQuantity(line.getQuantity());
                item.setTaxInclusiveAmount(line.getTaxInclusiveAmount());
                item.setUntaxAmount(line.getUntaxAmount());
                item.setTaxAmount(line.getTaxAmount());
                item.setTaxRate(line.getTaxRate());
                item.setSort(sort++);
                if (invoiceInItemMapper.insert(item) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
            }
        }
        BigDecimal linkSum = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(req.getLinks())) {
            for (InvoiceReq.Link link : req.getLinks()) {
                InvoiceInLinkDO row = new InvoiceInLinkDO();
                row.setTenantId(tenantId);
                row.setInvoiceId(invoiceId);
                row.setStockInId(link.getStockDocId());
                row.setAmount(link.getAmount());
                if (invoiceInLinkMapper.insert(row) != 1) throw new BusinessException("发票写入失败，请刷新后重试");
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
        List<InvoiceInLinkDO> all = invoiceInLinkMapper.selectList(Wrappers.lambdaQuery(InvoiceInLinkDO.class)
            .in(InvoiceInLinkDO::getInvoiceId, ids));
        java.util.Map<Long, BigDecimal> sumMap = new java.util.HashMap<>();
        for (InvoiceInLinkDO link : all) {
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

    private InvoiceInDO requireLocked(Long id) {
        InvoiceInDO entity = invoiceInMapper.selectByIdForUpdate(id);
        if (entity == null) throw new BusinessException("单据不存在或无权访问");
        return entity;
    }

    private InvoiceInDO require(Long id) {
        InvoiceInDO entity = invoiceInMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("进项发票不存在");
        }
        return entity;
    }

    private void assertDraft(InvoiceInDO entity) {
        if (!DocStatusEnum.DRAFT.equals(entity.getDocStatus())) {
            throw new BusinessException("仅草稿状态允许此操作");
        }
    }

    @Override
    public InvoiceImportParseResp parseImportExcel(MultipartFile file) {
        return InvoiceImportSupport.parse(file, unitMapper, TenantFillHelper.currentTenantId(), UnitTypeEnum.SUPPLIER);
    }
}
