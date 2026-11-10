package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 入库类型
 */
@Getter
@RequiredArgsConstructor
public enum StockInTypeEnum implements BaseEnum<String> {

    PURCHASE("PURCHASE", "采购入库"),
    OTHER("OTHER", "其他入库");

    private final String value;
    private final String description;
}
