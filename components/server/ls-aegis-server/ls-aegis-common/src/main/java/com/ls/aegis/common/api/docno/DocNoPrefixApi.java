package com.ls.aegis.common.api.docno;

/**
 * 单据编号前缀解析（读 {@code sys_option}，由 RBAC 实现）
 */
public interface DocNoPrefixApi {

    /**
     * 解析编号前缀
     *
     * @param optionCode    {@code sys_option.code}（EOMS 类别）
     * @param defaultPrefix 未配置或格式非法时的默认值
     * @return 2～4 位大写字母前缀
     */
    String resolvePrefix(String optionCode, String defaultPrefix);
}
