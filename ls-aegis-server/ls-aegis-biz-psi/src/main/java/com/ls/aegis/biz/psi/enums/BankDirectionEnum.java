package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 银行流水方向
 */
@Getter
@RequiredArgsConstructor
public enum BankDirectionEnum implements BaseEnum<String> {

    IN("IN", "收入"),
    OUT("OUT", "支出");

    private final String value;
    private final String description;
}
