package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 打印模板请求
 */
@Data
@Schema(description = "打印模板请求")
public class PrintTemplateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(hidden = true)
    private Long tenantId;

    @Schema(hidden = true)
    private Long deptId;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    @Length(max = 128, message = "模板名称长度不能超过 {max} 个字符")
    private String templateName;

    @Schema(description = "业务类型，如 STOCK_OUT")
    @NotBlank(message = "业务类型不能为空")
    @Length(max = 32, message = "业务类型长度不能超过 {max} 个字符")
    private String bizType;

    @Schema(description = "附件/Word 文件ID")
    private Long fileId;

    @Schema(description = "浏览器打印 HTML 模板")
    private String contentHtml;

    @Schema(description = "字段映射说明 JSON")
    @Length(max = 2000, message = "字段映射长度不能超过 {max} 个字符")
    private String fieldMapping;

    @Schema(description = "是否默认（0否 1是）")
    @NotNull(message = "是否默认不能为空")
    private Integer isDefault;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private DisEnableStatusEnum status;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注长度不能超过 {max} 个字符")
    private String remark;
}
