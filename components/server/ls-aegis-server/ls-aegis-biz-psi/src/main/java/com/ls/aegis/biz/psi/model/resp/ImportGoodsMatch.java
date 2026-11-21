package com.ls.aegis.biz.psi.model.resp;

import java.math.BigDecimal;
import lombok.Data;

/** 保留数据库排序规则匹配到的原始导入编号。 */
@Data
public class ImportGoodsMatch {
    private String importCode;
    private Long id;
    private String goodsCode;
    private String goodsName;
    private BigDecimal defaultTaxRate;
}
