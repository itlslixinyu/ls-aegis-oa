package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

@Data
@Schema(description = "仓库响应")
public class WarehouseResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    private String warehouseCode;
    private String warehouseName;
    private Long managerUserId;
    private String manager;
    private String address;
    private Boolean isDefault;
    private DisEnableStatusEnum status;
    private String remark;
}
