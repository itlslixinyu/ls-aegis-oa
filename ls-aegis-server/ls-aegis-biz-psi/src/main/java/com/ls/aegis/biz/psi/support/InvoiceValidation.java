package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.model.req.InvoiceReq;
import top.continew.starter.core.exception.BusinessException;
import java.math.BigDecimal;
import java.util.HashSet;

/** Blue-invoice amounts and allocations are validated before any header/child insert. */
public final class InvoiceValidation {
    private InvoiceValidation() { }
    public static void validate(InvoiceReq req) {
        // The existing UI allows absent tax numbers; persist a blank, never a fabricated number or SQL NULL.
        req.setBuyerTaxNo(cn.hutool.core.util.StrUtil.trimToEmpty(req.getBuyerTaxNo()));
        req.setSellerTaxNo(cn.hutool.core.util.StrUtil.trimToEmpty(req.getSellerTaxNo()));
        money(req.getTaxInclusiveAmount(),true);money(req.getUntaxAmount(),false);money(req.getTaxAmount(),false);
        same(req.getTaxInclusiveAmount(),req.getUntaxAmount().add(req.getTaxAmount()));
        var ids=new HashSet<Long>();
        if(req.getLinks()!=null)for(var link:req.getLinks()){
            if(link==null || link.getStockDocId()==null || !ids.add(link.getStockDocId()))throw new BusinessException("关联原单不能为空或重复");
            money(link.getAmount(),true);
        }
        if(req.getItems()!=null && !req.getItems().isEmpty()){
            BigDecimal total=BigDecimal.ZERO,untax=BigDecimal.ZERO,tax=BigDecimal.ZERO;
            for(var line:req.getItems()){
                if(line==null)throw new BusinessException("发票明细不能为空");
                money(line.getTaxInclusiveAmount(),false);money(line.getUntaxAmount(),false);money(line.getTaxAmount(),false);
                same(line.getTaxInclusiveAmount(),line.getUntaxAmount().add(line.getTaxAmount()));
                if(line.getQuantity()!=null && line.getQuantity().signum()<=0)throw new BusinessException("发票明细数量必须大于零");
                total=total.add(line.getTaxInclusiveAmount());untax=untax.add(line.getUntaxAmount());tax=tax.add(line.getTaxAmount());
            }
            same(req.getTaxInclusiveAmount(),total);same(req.getUntaxAmount(),untax);same(req.getTaxAmount(),tax);
        }
    }
    private static void money(BigDecimal value,boolean positive){
        if(value==null || value.signum()<0 || (positive && value.signum()==0) || value.stripTrailingZeros().scale()>2 || value.precision()-value.scale()>16)
            throw new BusinessException("蓝字发票金额必须非负、最多16位整数及2位小数，含税及关联金额必须大于零");
    }
    private static void same(BigDecimal left,BigDecimal right){
        if(left.subtract(right).abs().compareTo(InvoiceStatusSupport.AMOUNT_TOLERANCE)>0)throw new BusinessException("发票价税合计或明细合计不一致");
    }
}
