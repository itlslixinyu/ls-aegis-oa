package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 支付状态
 */
@Getter
@RequiredArgsConstructor
public enum PayStatusEnum implements BaseEnum<String> {

    UNPAID("UNPAID", "未支付"),
    PARTIAL("PARTIAL", "部分支付"),
    PAID("PAID", "已支付");

    private final String value;
    private final String description;
}
