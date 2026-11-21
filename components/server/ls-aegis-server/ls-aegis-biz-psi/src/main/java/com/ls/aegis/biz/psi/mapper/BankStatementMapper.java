package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.BankStatementDO;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankStatementMapper extends PsiScopedMapper<BankStatementDO> {
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine="true",dataPermission="true")
    @org.apache.ibatis.annotations.Select("""
        SELECT COUNT(*) FROM psi_bank_statement
        WHERE tenant_id=#{tenantId} AND account_id=#{accountId} AND deleted=0
        AND (bank_seq_no IS NULL OR TRIM(bank_seq_no)='')
        AND stmt_date=#{date} AND direction=#{direction} AND amount=#{amount}
        AND COALESCE(TRIM(counterparty),'')=#{counterparty} AND COALESCE(TRIM(summary),'')=#{summary}
        """)
    @org.apache.ibatis.annotations.Options(useCache=false,flushCache=org.apache.ibatis.annotations.Options.FlushCachePolicy.TRUE)
    long countNoSequenceIdentity(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
        @org.apache.ibatis.annotations.Param("accountId") Long accountId,
        @org.apache.ibatis.annotations.Param("date") java.time.LocalDateTime date,
        @org.apache.ibatis.annotations.Param("direction") String direction,
        @org.apache.ibatis.annotations.Param("amount") java.math.BigDecimal amount,
        @org.apache.ibatis.annotations.Param("counterparty") String counterparty,
        @org.apache.ibatis.annotations.Param("summary") String summary);
    @top.continew.starter.extension.datapermission.annotation.DataPermission
    @org.apache.ibatis.annotations.Select("SELECT * FROM psi_bank_statement WHERE id=#{id} AND deleted=0 FOR UPDATE")
    BankStatementDO selectByIdForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
}
