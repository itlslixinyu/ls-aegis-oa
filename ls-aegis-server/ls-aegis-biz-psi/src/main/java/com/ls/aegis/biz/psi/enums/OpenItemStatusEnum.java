package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 开口台账状态
 */
@Getter
@RequiredArgsConstructor
public enum OpenItemStatusEnum implements BaseEnum<String> {

    OPEN("OPEN", "未结清"),
    CLOSED("CLOSED", "已结清");

    private final String value;
    private final String description;
}
