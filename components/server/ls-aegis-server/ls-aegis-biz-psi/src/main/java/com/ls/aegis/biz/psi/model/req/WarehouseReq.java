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
 * 仓库请求
 */
@Data
@Schema(description = "仓库请求")
public class WarehouseReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库编码（系统生成可空）", example = "CK0001")
    @Length(max = 32, message = "仓库编码长度不能超过 {max} 个字符")
    private String warehouseCode;

    @Schema(description = "仓库名称", example = "主仓")
    @NotBlank(message = "仓库名称不能为空")
    @Length(max = 64, message = "仓库名称长度不能超过 {max} 个字符")
    private String warehouseName;

    @Schema(description = "负责人用户ID", example = "1")
    @NotNull(message = "负责人不能为空")
    private Long managerUserId;

    @Schema(description = "负责人姓名（后端根据用户回填，可空）")
    @Length(max = 32, message = "负责人长度不能超过 {max} 个字符")
    private String manager;

    @Schema(description = "地址")
    @Length(max = 256, message = "地址长度不能超过 {max} 个字符")
    private String address;

    @Schema(description = "是否默认仓", example = "true")
    @NotNull(message = "是否默认仓不能为空")
    private Boolean isDefault;

    @Schema(description = "状态", example = "1")
    @NotNull(message = "状态不能为空")
    private DisEnableStatusEnum status;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注长度不能超过 {max} 个字符")
    private String remark;

    @Schema(hidden = true)
    private Long tenantId;

    @Schema(hidden = true)
    private Long deptId;
}
