package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 商品档案
 */
@Data
@TableName("psi_goods")
public class GoodsDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private String goodsCode;
    private String goodsName;
    private String barcode;
    private String spec;
    private String unit;
    private BigDecimal defaultTaxRate;
    private Long categoryId;
    private Long brandId;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private BigDecimal minSalePrice;
    private BigDecimal stockUpper;
    private BigDecimal stockLower;
    private DisEnableStatusEnum status;
    private String remark;
}
