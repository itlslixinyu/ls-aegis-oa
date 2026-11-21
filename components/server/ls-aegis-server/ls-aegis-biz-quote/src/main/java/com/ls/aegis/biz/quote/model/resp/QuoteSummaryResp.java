package com.ls.aegis.biz.quote.model.resp;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

/**
 * 报价工作台摘要（工作总览）
 */
@Data
public class QuoteSummaryResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 草稿笔数 */
    private long draftCount;
    /** 本月报价笔数 */
    private long monthCount;
    /** 本月报价含税合计 */
    private BigDecimal monthAmount = BigDecimal.ZERO;
}
