package com.ls.aegis.biz.psi.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;

/** Internal invariant queries after the caller authorizes the exact invoice/stock document. */
@Mapper
@InterceptorIgnore(tenantLine="true",dataPermission="true")
public interface InvoiceLedgerMapper {
    @Select("SELECT l.* FROM psi_invoice_out_link l JOIN psi_invoice_out i ON i.id=l.invoice_id WHERE i.tenant_id=#{tenantId} AND i.id=#{invoiceId} AND i.deleted=0 AND l.deleted=0")
    java.util.List<com.ls.aegis.biz.psi.model.entity.InvoiceOutLinkDO> outLinks(@Param("tenantId") Long tenantId,@Param("invoiceId") Long invoiceId);
    @Select("SELECT l.* FROM psi_invoice_in_link l JOIN psi_invoice_in i ON i.id=l.invoice_id WHERE i.tenant_id=#{tenantId} AND i.id=#{invoiceId} AND i.deleted=0 AND l.deleted=0")
    java.util.List<com.ls.aegis.biz.psi.model.entity.InvoiceInLinkDO> inLinks(@Param("tenantId") Long tenantId,@Param("invoiceId") Long invoiceId);
    @Select("SELECT l.* FROM psi_invoice_out_item l JOIN psi_invoice_out i ON i.id=l.invoice_id WHERE i.tenant_id=#{tenantId} AND i.id=#{invoiceId} AND i.deleted=0 AND l.deleted=0")
    java.util.List<com.ls.aegis.biz.psi.model.entity.InvoiceOutItemDO> outItems(@Param("tenantId") Long tenantId,@Param("invoiceId") Long invoiceId);
    @Select("SELECT l.* FROM psi_invoice_in_item l JOIN psi_invoice_in i ON i.id=l.invoice_id WHERE i.tenant_id=#{tenantId} AND i.id=#{invoiceId} AND i.deleted=0 AND l.deleted=0")
    java.util.List<com.ls.aegis.biz.psi.model.entity.InvoiceInItemDO> inItems(@Param("tenantId") Long tenantId,@Param("invoiceId") Long invoiceId);
    @Select("""
      <script>SELECT COALESCE(SUM(l.amount),0) FROM psi_invoice_out_link l
      JOIN psi_invoice_out i ON i.id=l.invoice_id AND i.tenant_id=l.tenant_id
      WHERE l.tenant_id=#{tenantId} AND l.stock_out_id=#{stockId} AND l.deleted=0
      AND i.deleted=0 AND i.doc_status='CONFIRMED' AND i.red_of_id IS NULL
      <if test="excludeId != null">AND i.id != #{excludeId}</if></script>
      """)
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    BigDecimal outLinked(@Param("tenantId") Long tenantId,@Param("stockId") Long stockId,@Param("excludeId") Long excludeId);
    @Select("""
      <script>SELECT COALESCE(SUM(l.amount),0) FROM psi_invoice_in_link l
      JOIN psi_invoice_in i ON i.id=l.invoice_id AND i.tenant_id=l.tenant_id
      WHERE l.tenant_id=#{tenantId} AND l.stock_in_id=#{stockId} AND l.deleted=0
      AND i.deleted=0 AND i.doc_status='CONFIRMED' AND i.red_of_id IS NULL
      <if test="excludeId != null">AND i.id != #{excludeId}</if></script>
      """)
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    BigDecimal inLinked(@Param("tenantId") Long tenantId,@Param("stockId") Long stockId,@Param("excludeId") Long excludeId);
    @Select("SELECT doc_no FROM psi_invoice_out WHERE tenant_id=#{tenantId} AND red_of_id=#{blueId} AND deleted=0")
    String outRedNo(@Param("tenantId") Long tenantId,@Param("blueId") Long blueId);
    @Select("SELECT doc_no FROM psi_invoice_in WHERE tenant_id=#{tenantId} AND red_of_id=#{blueId} AND deleted=0")
    String inRedNo(@Param("tenantId") Long tenantId,@Param("blueId") Long blueId);
}
