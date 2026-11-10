package com.ls.aegis.common.api.system;

/**
 * 读取 {@code sys_option}（由 RBAC 实现，业务模块勿直接依赖 OptionService）
 */
public interface SysOptionApi {

    /**
     * 按类别 + 编码取值；库中 value 为空时回落 default_value。
     *
     * @return 未配置时返回 {@code null}
     */
    String getValue(String category, String code);

    /**
     * 取值，空白时返回指定默认值
     */
    String getValue(String category, String code, String defaultValue);
}
