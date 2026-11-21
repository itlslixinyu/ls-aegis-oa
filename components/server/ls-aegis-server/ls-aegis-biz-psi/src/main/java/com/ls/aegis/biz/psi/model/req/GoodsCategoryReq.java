package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品分类请求
 */
@Data
@Schema(description = "商品分类请求")
public class GoodsCategoryReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "上级分类 ID（0=一级分类；选一级 ID 则创建二级）", example = "0")
    @NotNull(message = "上级分类不能为空")
    private Long parentId;

    @Schema(description = "分类编码（创建时忽略，系统自动生成：一级 10/20/30，二级 1001/2001）", example = "10")
    @Length(max = 32, message = "分类编码长度不能超过 {max} 个字符")
    private String code;

    @Schema(description = "分类名称", example = "网络设备")
    @NotBlank(message = "分类名称不能为空")
    @Length(max = 64, message = "分类名称长度不能超过 {max} 个字符")
    private String name;

    @Schema(description = "排序", example = "1")
    @Min(value = 1, message = "排序最小值为 {value}")
    private Integer sort;

    @Schema(description = "状态", example = "1")
    @NotNull(message = "状态不能为空")
    private DisEnableStatusEnum status;

    @Schema(hidden = true)
    private Long tenantId;

    @Schema(hidden = true)
    private Long deptId;
}
