package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
public class OpenItemResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long unitId;
    private String unitName;
    private BizSourceTypeEnum sourceType;
    private Long sourceId;
    private String sourceNo;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal writtenOffAmount;
    private BigDecimal unwrittenAmount;
    private OpenItemStatusEnum docStatus;
}
