package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 核销方向
 */
@Getter
@RequiredArgsConstructor
public enum WriteOffDirectionEnum implements BaseEnum<String> {

    WRITE_OFF("WRITE_OFF", "核销"),
    REVERSE("REVERSE", "反核销");

    private final String value;
    private final String description;
}
