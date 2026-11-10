package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 退货类型
 */
@Getter
@RequiredArgsConstructor
public enum ReturnTypeEnum implements BaseEnum<String> {

    SALE_RETURN("SALE_RETURN", "销售退货"),
    PURCHASE_RETURN("PURCHASE_RETURN", "采购退货");

    private final String value;
    private final String description;
}
