package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.BankDirectionEnum;
import com.ls.aegis.biz.psi.enums.MatchStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_bank_statement")
public class BankStatementDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 创建人部门（数据权限） */
    private Long deptId;
    private Long accountId;
    private LocalDateTime stmtDate;
    private BankDirectionEnum direction;
    private BigDecimal amount;
    private String counterparty;
    private String summary;
    private String bankSeqNo;
    private String fingerprint;
    private MatchStatusEnum matchStatus;
    private String matchBizType;
    private Long matchBizId;
    private String unmatchReason;
    private Long unmatchUser;
    private LocalDateTime unmatchTime;
}
