package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.vo.DashboardStats;

/**
 * 仪表盘服务接口
 */
public interface ISignDashboardService {

    /**
     * 获取仪表盘统计数据
     */
    DashboardStats getStats();
}
