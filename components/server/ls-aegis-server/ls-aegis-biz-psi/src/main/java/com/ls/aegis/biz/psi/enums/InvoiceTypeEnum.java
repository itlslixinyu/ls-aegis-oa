package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 发票类型
 */
@Getter
@RequiredArgsConstructor
public enum InvoiceTypeEnum implements BaseEnum<String> {

    SPECIAL("SPECIAL", "专票"),
    NORMAL("NORMAL", "普票");

    private final String value;
    private final String description;
}
