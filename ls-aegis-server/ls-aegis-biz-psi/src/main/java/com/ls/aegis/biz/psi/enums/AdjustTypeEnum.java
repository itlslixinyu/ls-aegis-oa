package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 库存调整类型
 */
@Getter
@RequiredArgsConstructor
public enum AdjustTypeEnum implements BaseEnum<String> {

    PROFIT("PROFIT", "盘盈"),
    LOSS("LOSS", "盘亏");

    private final String value;
    private final String description;
}
