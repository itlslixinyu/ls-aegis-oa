package com.ls.aegis.rbac.service;

import com.ls.aegis.rbac.model.resp.WeatherNowResp;

/**
 * 运营中枢天气
 */
public interface WeatherService {

    /**
     * 查询实时天气（和风 API Key/JWT 或本地模拟；失败自动回退模拟）
     *
     * @param clientIp 客户端 IP（自动定位用）
     * @param lat      可选纬度（浏览器定位优先）
     * @param lon      可选经度
     * @return 归一化天气
     */
    WeatherNowResp now(String clientIp, Double lat, Double lon);

    /**
     * 测试天气连接（不回退模拟；失败抛业务异常）
     *
     * @param clientIp 客户端 IP
     * @return 实测天气
     */
    WeatherNowResp testConnection(String clientIp);

    /** 清除进程内 JWT 缓存（配置变更 / 一键刷新后调用） */
    void clearTokenCache();
}
