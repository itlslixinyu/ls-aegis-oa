package com.ls.aegis.starter.support.docno;

/**
 * 平台编号 Redis Key 前缀与日流水规则
 */
public final class DocNoConstants {

    private DocNoConstants() {
    }

    /** 单据日期：年取后两位，如 2026-08-19 → {@code 260819} */
    public static final String DATE_PATTERN = "yyMMdd";
    /** 日流水位数：当天从 01 递增，最多 99 */
    public static final int DAILY_SEQ_WIDTH = 2;

    /** PSI 单据日序号：{@code psi:seq:{tenant}:{prefix}:{yyMMdd}} */
    public static final String PSI_DOC_SEQ_PREFIX = "psi:seq:";
    /** PSI 主数据全局序号：{@code psi:code:{tenant}:{scope}} */
    public static final String PSI_CODE_SEQ_PREFIX = "psi:code:";
    /** EOMS 日序号 / 物料序号：{@code eoms:no:...} */
    public static final String EOMS_NO_PREFIX = "eoms:no:";
    /** 签章日序号：{@code sign:seq:{prefix}:{yyMMdd}} */
    public static final String SIGN_DOC_SEQ_PREFIX = "sign:seq:";
}
