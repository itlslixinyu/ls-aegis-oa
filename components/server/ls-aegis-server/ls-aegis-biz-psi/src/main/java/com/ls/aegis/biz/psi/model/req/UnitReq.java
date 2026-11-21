package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.UnitNatureEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 往来单位请求
 */
@Data
@Schema(description = "往来单位请求")
public class UnitReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "单位编号（系统生成可空）", example = "WLDW0001")
    @Length(max = 32, message = "单位编号长度不能超过 {max} 个字符")
    private String unitCode;

    @Schema(description = "单位全称", example = "示例科技有限公司")
    @NotBlank(message = "单位全称不能为空")
    @Length(max = 128, message = "单位全称长度不能超过 {max} 个字符")
    private String unitName;

    @Schema(description = "单位类型", example = "CUSTOMER")
    @NotNull(message = "单位类型不能为空")
    private UnitTypeEnum unitType;

    @Schema(description = "单位性质", example = "ENTERPRISE")
    @NotNull(message = "单位性质不能为空")
    private UnitNatureEnum unitNature;

    @Schema(description = "统一社会信用代码")
    @Length(max = 64, message = "信用代码长度不能超过 {max} 个字符")
    private String creditCode;

    @Schema(description = "地址")
    @Length(max = 256, message = "地址长度不能超过 {max} 个字符")
    private String address;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "等级编码")
    @Length(max = 16, message = "等级编码长度不能超过 {max} 个字符")
    private String levelCode;

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
