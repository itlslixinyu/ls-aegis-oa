package com.ls.aegis.biz.psi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.continew.starter.core.enums.BaseEnum;

/**
 * PSI 主数据导入策略（与用户模块语义对齐，不依赖 rbac）
 */
@Getter
@RequiredArgsConstructor
public enum PsiImportPolicyEnum implements BaseEnum<Integer> {

    SKIP(1, "跳过该行"),
    UPDATE(2, "修改数据"),
    EXIT(3, "停止导入");

    private final Integer value;
    private final String description;
}
