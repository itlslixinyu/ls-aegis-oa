package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InvoiceStatusEnum;
import com.ls.aegis.biz.psi.mapper.StockInMapper;
import com.ls.aegis.biz.psi.mapper.StockOutMapper;
import com.ls.aegis.biz.psi.model.entity.StockInDO;
import com.ls.aegis.biz.psi.model.entity.StockOutDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class InvoiceStatusSupport {

    /** 价税/关联金额容差（与业务文档尾差口径一致） */
    public static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.05");

    private final com.ls.aegis.biz.psi.mapper.InvoiceLedgerMapper invoiceLedgerMapper;
    private final StockOutMapper stockOutMapper;
    private final StockInMapper stockInMapper;

    /**
     * 草稿保存：关联合计不得超过发票含税（允许未分摊完）。
     */
    public void assertLinkSumForSave(BigDecimal invoiceAmount, BigDecimal linkSum) {
        BigDecimal total = nz(invoiceAmount);
        BigDecimal linked = nz(linkSum);
        if (linked.compareTo(total.add(AMOUNT_TOLERANCE)) > 0) {
            throw new BusinessException("关联金额不能超过发票含税金额");
        }
    }

    /**
     * 确认生效：关联合计须与发票含税一致（允许 ≤0.05 尾差）。
     */
    public void assertLinkSumForConfirm(BigDecimal invoiceAmount, BigDecimal linkSum, String emptyMsg) {
        BigDecimal total = nz(invoiceAmount);
        BigDecimal linked = nz(linkSum);
        if (linked.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(emptyMsg);
        }
        BigDecimal unallocated = total.subtract(linked);
        if (unallocated.compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessException("尚有未分摊金额 " + unallocated.setScale(2, java.math.RoundingMode.HALF_UP)
                + "，请补关联后再确认");
        }
        if (linked.subtract(total).compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessException("关联金额不能超过发票含税金额");
        }
    }

    public BigDecimal unallocated(BigDecimal invoiceAmount, BigDecimal linkSum) {
        BigDecimal gap = nz(invoiceAmount).subtract(nz(linkSum));
        return gap.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : gap;
    }

    public BigDecimal confirmedOutLinked(Long stockOutId, Long excludeInvoiceId) {
        return confirmedOutLinked(TenantFillHelper.currentTenantId(),stockOutId,excludeInvoiceId);
    }
    public BigDecimal confirmedInLinked(Long stockInId, Long excludeInvoiceId) {
        return confirmedInLinked(TenantFillHelper.currentTenantId(),stockInId,excludeInvoiceId);
    }
    public BigDecimal confirmedOutLinked(Long tenantId, Long stockOutId, Long excludeInvoiceId) {return invoiceLedgerMapper.outLinked(tenantId,stockOutId,excludeInvoiceId);}
    public BigDecimal confirmedInLinked(Long tenantId, Long stockInId, Long excludeInvoiceId) {return invoiceLedgerMapper.inLinked(tenantId,stockInId,excludeInvoiceId);}
    public java.util.List<com.ls.aegis.biz.psi.model.entity.InvoiceOutLinkDO> outLinks(Long tenantId,Long id){return invoiceLedgerMapper.outLinks(tenantId,id);}
    public java.util.List<com.ls.aegis.biz.psi.model.entity.InvoiceInLinkDO> inLinks(Long tenantId,Long id){return invoiceLedgerMapper.inLinks(tenantId,id);}
    public java.util.List<com.ls.aegis.biz.psi.model.entity.InvoiceOutItemDO> outItems(Long tenantId,Long id){return invoiceLedgerMapper.outItems(tenantId,id);}
    public java.util.List<com.ls.aegis.biz.psi.model.entity.InvoiceInItemDO> inItems(Long tenantId,Long id){return invoiceLedgerMapper.inItems(tenantId,id);}
    public String redNo(boolean out, Long tenantId, Long blueId) {
        return out ? invoiceLedgerMapper.outRedNo(tenantId,blueId) : invoiceLedgerMapper.inRedNo(tenantId,blueId);
    }

    public void refreshStockOut(Long stockOutId) {
        StockOutDO doc = stockOutMapper.selectByIdForUpdate(stockOutId);
        if (doc == null) {
            throw new BusinessException("关联出库单不存在或无权访问");
        }
        BigDecimal linked = confirmedOutLinked(doc.getTenantId(),stockOutId, null);
        doc.setInvoiceStatus(toStatus(doc.getTaxInclusiveAmount(), linked));
        if(stockOutMapper.updateById(doc)!=1) throw new BusinessException("出库开票状态更新失败");
    }

    public void refreshStockIn(Long stockInId) {
        StockInDO doc = stockInMapper.selectByIdForUpdate(stockInId);
        if (doc == null) {
            throw new BusinessException("关联入库单不存在或无权访问");
        }
        BigDecimal linked = confirmedInLinked(doc.getTenantId(),stockInId, null);
        doc.setInvoiceStatus(toStatus(doc.getTaxInclusiveAmount(), linked));
        if(stockInMapper.updateById(doc)!=1) throw new BusinessException("入库收票状态更新失败");
    }

    public void assertOutBalance(StockOutDO doc, BigDecimal amount, Long excludeInvoiceId) {
        if (!DocStatusEnum.CONFIRMED.equals(doc.getDocStatus())) {
            throw new BusinessException("仅已确认出库可开票");
        }
        BigDecimal linked = confirmedOutLinked(doc.getTenantId(),doc.getId(), excludeInvoiceId);
        BigDecimal remain = nz(doc.getTaxInclusiveAmount()).subtract(linked);
        if (amount.compareTo(remain) > 0) {
            throw new BusinessException("开票金额超过出库可开票余额，剩余=" + remain);
        }
    }

    public void assertInBalance(StockInDO doc, BigDecimal amount, Long excludeInvoiceId) {
        if (!DocStatusEnum.CONFIRMED.equals(doc.getDocStatus())) {
            throw new BusinessException("仅已确认入库可收票");
        }
        BigDecimal linked = confirmedInLinked(doc.getTenantId(),doc.getId(), excludeInvoiceId);
        BigDecimal remain = nz(doc.getTaxInclusiveAmount()).subtract(linked);
        if (amount.compareTo(remain) > 0) {
            throw new BusinessException("收票金额超过入库可收票余额，剩余=" + remain);
        }
    }

    private InvoiceStatusEnum toStatus(BigDecimal total, BigDecimal linked) {
        if (linked.compareTo(BigDecimal.ZERO) <= 0) {
            return InvoiceStatusEnum.UNINVOICED;
        }
        if (linked.compareTo(nz(total)) >= 0) {
            return InvoiceStatusEnum.INVOICED;
        }
        return InvoiceStatusEnum.PARTIAL;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
