package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * 银行对账匹配状态
 */
@Getter
@RequiredArgsConstructor
public enum MatchStatusEnum implements BaseEnum<String> {

    UNMATCHED("UNMATCHED", "未匹配"),
    MATCHED("MATCHED", "已匹配");

    private final String value;
    private final String description;
}
