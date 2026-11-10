package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 打印模板详情响应
 */
@Data
@Schema(description = "打印模板详情响应")
public class PrintTemplateDetailResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    private String templateName;
    private String bizType;
    private Long fileId;
    private String contentHtml;
    private String fieldMapping;
    private Integer isDefault;
    private DisEnableStatusEnum status;
    private String remark;
}
