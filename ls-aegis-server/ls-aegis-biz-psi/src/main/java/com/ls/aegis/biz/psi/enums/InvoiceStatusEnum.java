package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 开票状态
 */
@Getter
@RequiredArgsConstructor
public enum InvoiceStatusEnum implements BaseEnum<String> {

    UNINVOICED("UNINVOICED", "未开票"),
    PARTIAL("PARTIAL", "部分开票"),
    INVOICED("INVOICED", "已开票");

    private final String value;
    private final String description;
}
