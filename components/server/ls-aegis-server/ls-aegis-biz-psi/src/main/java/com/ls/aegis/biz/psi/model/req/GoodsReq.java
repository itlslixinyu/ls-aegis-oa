package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品请求
 */
@Data
@Schema(description = "商品请求")
public class GoodsReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品编号（系统生成可空，分类码+4位序号）", example = "10010001")
    @Length(max = 32, message = "商品编号长度不能超过 {max} 个字符")
    private String goodsCode;

    @Schema(description = "商品名称", example = "示例商品")
    @NotBlank(message = "商品名称不能为空")
    @Length(max = 128, message = "商品名称长度不能超过 {max} 个字符")
    private String goodsName;

    @Schema(description = "条码")
    @Length(max = 64, message = "条码长度不能超过 {max} 个字符")
    private String barcode;

    @Schema(description = "规格型号")
    @Length(max = 128, message = "规格长度不能超过 {max} 个字符")
    private String spec;

    @Schema(description = "计量单位", example = "个")
    @NotBlank(message = "计量单位不能为空")
    @Length(max = 16, message = "单位长度不能超过 {max} 个字符")
    private String unit;

    @Schema(description = "默认税率", example = "13")
    @NotNull(message = "默认税率不能为空")
    private BigDecimal defaultTaxRate;

    @Schema(description = "分类ID（必须为二级分类）", example = "1")
    @NotNull(message = "请选择二级商品分类")
    private Long categoryId;

    @Schema(description = "品牌ID")
    private Long brandId;

    @Schema(description = "采购参考价")
    private BigDecimal purchasePrice;

    @Schema(description = "销售参考价")
    private BigDecimal salePrice;

    @Schema(description = "最低售价")
    private BigDecimal minSalePrice;

    @Schema(description = "库存上限")
    private BigDecimal stockUpper;

    @Schema(description = "库存下限")
    private BigDecimal stockLower;

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
