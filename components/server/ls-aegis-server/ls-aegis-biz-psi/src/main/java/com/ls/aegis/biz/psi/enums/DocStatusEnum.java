package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 单据状态
 */
@Getter
@RequiredArgsConstructor
public enum DocStatusEnum implements BaseEnum<String> {

    DRAFT("DRAFT", "草稿"),
    CONFIRMED("CONFIRMED", "已确认"),
    VOID("VOID", "已作废");

    private final String value;
    private final String description;
}
