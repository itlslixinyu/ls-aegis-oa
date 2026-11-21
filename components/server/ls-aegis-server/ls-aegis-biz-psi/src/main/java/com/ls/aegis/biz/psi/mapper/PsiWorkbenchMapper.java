package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.resp.WorkbenchAggregate;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Options;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

/** 固定表的只读聚合；权限绑定单据头h，明细同租户关联，不生成大IN列表。 */
@Mapper
public interface PsiWorkbenchMapper {
    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COUNT(*) FROM psi_todo h WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.done=0
        """)
    long countOpenTodos(@Param("tenantId") Long tenantId);

    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COUNT(*) AS count, COALESCE(SUM(h.unwritten_amount),0) AS amount
        FROM psi_receivable h WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.doc_status='OPEN' AND h.unwritten_amount>0
        """)
    WorkbenchAggregate selectReceivables(@Param("tenantId") Long tenantId);

    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COUNT(*) AS count, COALESCE(SUM(h.unwritten_amount),0) AS amount
        FROM psi_payable h WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.doc_status='OPEN' AND h.unwritten_amount>0
        """)
    WorkbenchAggregate selectPayables(@Param("tenantId") Long tenantId);

    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COUNT(*) AS count, COALESCE(SUM(h.amount),0) AS amount
        FROM psi_receipt h WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.doc_status='DRAFT'
        """)
    WorkbenchAggregate selectReceipts(@Param("tenantId") Long tenantId);

    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COUNT(*) AS count, COALESCE(SUM(h.amount),0) AS amount
        FROM psi_bank_statement h WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.match_status='UNMATCHED'
        """)
    WorkbenchAggregate selectBanks(@Param("tenantId") Long tenantId);

    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COALESCE(SUM(CASE WHEN h.confirm_time>=#{monthStart} THEN 1 ELSE 0 END),0) AS monthCount,
        COALESCE(SUM(CASE WHEN h.confirm_time>=#{monthStart} THEN h.tax_inclusive_amount ELSE 0 END),0) AS monthAmount,
        COALESCE(SUM(CASE WHEN h.confirm_time>=#{spanStart} THEN 1 ELSE 0 END),0) AS spanCount,
        COALESCE(SUM(CASE WHEN h.confirm_time>=#{spanStart} THEN h.tax_inclusive_amount ELSE 0 END),0) AS spanAmount
        FROM psi_stock_in h
        WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.doc_status='CONFIRMED'
        AND h.confirm_time>=#{windowStart} AND h.confirm_time<=#{windowEnd}
        """)
    WorkbenchAggregate selectInHeaders(@Param("tenantId") Long tenantId, @Param("monthStart") LocalDateTime monthStart,
        @Param("spanStart") LocalDateTime spanStart, @Param("windowStart") LocalDateTime windowStart,
        @Param("windowEnd") LocalDateTime windowEnd);

    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COALESCE(SUM(CASE WHEN h.confirm_time>=#{monthStart} THEN i.quantity ELSE 0 END),0) AS monthQty,
        COALESCE(SUM(CASE WHEN h.confirm_time>=#{spanStart} THEN i.quantity ELSE 0 END),0) AS spanQty
        FROM psi_stock_in h
        JOIN psi_stock_in_item i ON i.stock_in_id=h.id AND i.tenant_id=h.tenant_id AND i.deleted=0
        WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.doc_status='CONFIRMED'
        AND h.confirm_time>=#{windowStart} AND h.confirm_time<=#{windowEnd}
        """)
    WorkbenchAggregate selectInQuantities(@Param("tenantId") Long tenantId, @Param("monthStart") LocalDateTime monthStart,
        @Param("spanStart") LocalDateTime spanStart, @Param("windowStart") LocalDateTime windowStart,
        @Param("windowEnd") LocalDateTime windowEnd);

    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COALESCE(SUM(CASE WHEN h.confirm_time>=#{monthStart} THEN 1 ELSE 0 END),0) AS monthCount,
        COALESCE(SUM(CASE WHEN h.confirm_time>=#{monthStart} THEN h.tax_inclusive_amount ELSE 0 END),0) AS monthAmount,
        COALESCE(SUM(CASE WHEN h.confirm_time>=#{spanStart} THEN 1 ELSE 0 END),0) AS spanCount,
        COALESCE(SUM(CASE WHEN h.confirm_time>=#{spanStart} THEN h.tax_inclusive_amount ELSE 0 END),0) AS spanAmount
        FROM psi_stock_out h
        WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.doc_status='CONFIRMED'
        AND h.confirm_time>=#{windowStart} AND h.confirm_time<=#{windowEnd}
        """)
    WorkbenchAggregate selectOutHeaders(@Param("tenantId") Long tenantId, @Param("monthStart") LocalDateTime monthStart,
        @Param("spanStart") LocalDateTime spanStart, @Param("windowStart") LocalDateTime windowStart,
        @Param("windowEnd") LocalDateTime windowEnd);

    @DataPermission(tableAlias = "h")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    @Select("""
        SELECT COALESCE(SUM(CASE WHEN h.confirm_time>=#{monthStart} THEN i.quantity ELSE 0 END),0) AS monthQty,
        COALESCE(SUM(CASE WHEN h.confirm_time>=#{spanStart} THEN i.quantity ELSE 0 END),0) AS spanQty
        FROM psi_stock_out h
        JOIN psi_stock_out_item i ON i.stock_out_id=h.id AND i.tenant_id=h.tenant_id AND i.deleted=0
        WHERE h.tenant_id=#{tenantId} AND h.deleted=0 AND h.doc_status='CONFIRMED'
        AND h.confirm_time>=#{windowStart} AND h.confirm_time<=#{windowEnd}
        """)
    WorkbenchAggregate selectOutQuantities(@Param("tenantId") Long tenantId, @Param("monthStart") LocalDateTime monthStart,
        @Param("spanStart") LocalDateTime spanStart, @Param("windowStart") LocalDateTime windowStart,
        @Param("windowEnd") LocalDateTime windowEnd);
}
