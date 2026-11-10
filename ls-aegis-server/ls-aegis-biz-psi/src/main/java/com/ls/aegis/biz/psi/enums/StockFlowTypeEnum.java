package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 库存流水类型
 */
@Getter
@RequiredArgsConstructor
public enum StockFlowTypeEnum implements BaseEnum<String> {

    IN("IN", "入库"),
    OUT("OUT", "出库"),
    TRANSFER_IN("TRANSFER_IN", "调拨入"),
    TRANSFER_OUT("TRANSFER_OUT", "调拨出"),
    PROFIT("PROFIT", "盘盈"),
    LOSS("LOSS", "盘亏"),
    ADJUST("ADJUST", "调整");

    private final String value;
    private final String description;
}
