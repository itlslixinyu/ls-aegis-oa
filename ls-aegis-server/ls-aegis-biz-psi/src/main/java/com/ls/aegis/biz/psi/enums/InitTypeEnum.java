package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 期初类型
 */
@Getter
@RequiredArgsConstructor
public enum InitTypeEnum implements BaseEnum<String> {

    STOCK("STOCK", "期初库存"),
    AR("AR", "应收期初"),
    AP("AP", "应付期初"),
    PREPAY("PREPAY", "预付期初");

    private final String value;
    private final String description;
}
