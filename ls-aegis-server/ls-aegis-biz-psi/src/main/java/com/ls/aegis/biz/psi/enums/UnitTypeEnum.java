package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 往来单位类型
 */
@Getter
@RequiredArgsConstructor
public enum UnitTypeEnum implements BaseEnum<String> {

    CUSTOMER("CUSTOMER", "客户"),
    SUPPLIER("SUPPLIER", "供应商"),
    BOTH("BOTH", "双重身份");

    private final String value;
    private final String description;
}
