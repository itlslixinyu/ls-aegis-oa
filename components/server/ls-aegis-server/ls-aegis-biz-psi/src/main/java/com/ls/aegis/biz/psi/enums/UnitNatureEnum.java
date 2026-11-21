package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 往来单位性质
 */
@Getter
@RequiredArgsConstructor
public enum UnitNatureEnum implements BaseEnum<String> {

    ENTERPRISE("ENTERPRISE", "企业"),
    PERSON("PERSON", "个人");

    private final String value;
    private final String description;
}
