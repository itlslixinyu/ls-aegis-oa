package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 出库类型
 */
@Getter
@RequiredArgsConstructor
public enum StockOutTypeEnum implements BaseEnum<String> {

    SALE("SALE", "销售出库"),
    OTHER("OTHER", "其他出库"),
    REPAIR("REPAIR", "维修出库");

    private final String value;
    private final String description;
}
