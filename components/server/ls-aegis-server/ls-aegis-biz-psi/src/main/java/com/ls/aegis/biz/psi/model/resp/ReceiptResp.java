package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReceiptResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private String docNo;
    private DocStatusEnum docStatus;
    private LocalDateTime receiptDate;
    private Long unitId;
    private String unitName;
    private Long accountId;
    private String accountName;
    private String payMethod;
    private BigDecimal amount;
    private BigDecimal unallocatedAmount;
    private BigDecimal refundedAmount;
    private Long refundSourceId;
    private Long refundReturnId;
    private String voidReason;
    private LocalDateTime voidTime;
    private String remark;
}
