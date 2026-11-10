package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import lombok.Data;

import java.io.Serial;

@Data
public class CompanyAccountResp extends BaseResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private String accountName;
    private String bankName;
    private String accountNo;
    private DisEnableStatusEnum status;
    private String remark;
}
