package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 应收开口
 */
@Data
@TableName("psi_receivable")
public class ReceivableDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private Long unitId;
    private BizSourceTypeEnum sourceType;
    private Long sourceId;
    private String sourceNo;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal writtenOffAmount;
    private BigDecimal unwrittenAmount;
    private OpenItemStatusEnum docStatus;
}
