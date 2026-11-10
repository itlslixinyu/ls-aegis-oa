package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@Schema(description = "商品响应")
public class GoodsResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    private String goodsCode;
    private String goodsName;
    private String barcode;
    private String spec;
    private String unit;
    private BigDecimal defaultTaxRate;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal minSalePrice;
    private BigDecimal stockUpper;
    private BigDecimal stockLower;
    private DisEnableStatusEnum status;
    private String remark;
}
