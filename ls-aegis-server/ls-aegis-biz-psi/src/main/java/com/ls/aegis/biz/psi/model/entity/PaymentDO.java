package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_payment")
public class PaymentDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 创建人部门（数据权限） */
    private Long deptId;
    private String docNo;
    private DocStatusEnum docStatus;
    private Long unitId;
    private Long accountId;
    private BigDecimal amount;
    private BigDecimal unallocatedAmount;
    private BigDecimal refundedAmount;
    private Long refundSourceId;
    private Long refundReturnId;
    private Long operationSequence;
    private Long sourceSequence;
    private Long voidSequence;
    private String voidReason;
    private Long voidUser;
    private LocalDateTime voidTime;
    private String payMethod;
    private LocalDateTime paymentDate;
    private String idempotentKey;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
