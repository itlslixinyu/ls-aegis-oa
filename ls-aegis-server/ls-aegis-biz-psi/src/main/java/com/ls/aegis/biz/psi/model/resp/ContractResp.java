package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ContractResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private String contractNo;
    private String name;
    private LocalDateTime signDate;
    private Long unitId;
    private Long projectId;
    private BigDecimal taxInclusiveAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private DocStatusEnum docStatus;
    /** 附件文件 ID，多个以英文逗号分隔 */
    private String fileId;
    private String remark;
    /** 执行进度 0~1 */
    private BigDecimal progress;
}
