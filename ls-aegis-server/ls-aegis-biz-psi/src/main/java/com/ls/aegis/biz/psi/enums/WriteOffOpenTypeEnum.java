package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 核销开口类型
 */
@Getter
@RequiredArgsConstructor
public enum WriteOffOpenTypeEnum implements BaseEnum<String> {

    AR("AR", "应收"),
    AP("AP", "应付");

    private final String value;
    private final String description;
}
