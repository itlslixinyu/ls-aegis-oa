package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.BankDirectionEnum;
import com.ls.aegis.biz.psi.enums.MatchStatusEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BankStatementResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long accountId;
    private String accountName;
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
}
