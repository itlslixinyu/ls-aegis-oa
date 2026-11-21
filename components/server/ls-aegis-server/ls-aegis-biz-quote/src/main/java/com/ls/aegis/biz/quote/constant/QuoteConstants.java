package com.ls.aegis.biz.quote.constant;

/**
 * 报价模块常量（与运维报修解耦，不引用 EomsConstants）
 */
public final class QuoteConstants {

    private QuoteConstants() {
    }

    /** 草稿 */
    public static final String QUOTE_STATUS_DRAFT = "DRAFT";
    /** 已提交 */
    public static final String QUOTE_STATUS_SUBMIT = "SUBMIT";
    /** 已确认 */
    public static final String QUOTE_STATUS_CONFIRM = "CONFIRM";
    /** 已取消 */
    public static final String QUOTE_STATUS_CANCEL = "CANCEL";

    /** 列表场景：进行中（草稿 / 已提交 / 已取消） */
    public static final String STATUS_SCOPE_ACTIVE = "ACTIVE";
    /** 列表场景：历史（仅已确认） */
    public static final String STATUS_SCOPE_HISTORY = "HISTORY";

    /** 材料行 */
    public static final String LINE_TYPE_MATERIAL = "MATERIAL";
    /** 服务行 */
    public static final String LINE_TYPE_SERVICE = "SERVICE";
}
