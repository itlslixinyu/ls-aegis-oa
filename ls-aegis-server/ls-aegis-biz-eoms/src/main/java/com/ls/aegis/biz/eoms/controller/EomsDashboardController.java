package com.ls.aegis.biz.eoms.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.biz.eoms.service.statistics.IStatisticsService;
import top.continew.starter.web.model.R;

/**
 * Dashboard 概览 Controller
 *
 * C9：页面只展示 KPI（{@code /overview}）。趋势 / Top 图表在 {@code /eoms/statistics}。
 * {@code /all} 保留给导出或兼容调用，不要再接到仪表盘页。
 */
@RestController
@RequestMapping("/eoms/dashboard")
public class EomsDashboardController
{
    @Autowired
    private IStatisticsService statisticsService;

    @SaCheckPermission("eoms:dashboard:list")
    @GetMapping("/overview")
    public R<Map<String, Object>> getOverview()
    {
        Map<String, Object> data = statisticsService.dashboardOverview();
        return R.ok(data);
    }

    /**
     * 一次性返回 KPI + 图表（兼容旧调用）。新仪表盘只用 {@link #getOverview}。
     */
    @SaCheckPermission("eoms:dashboard:list")
    @GetMapping("/all")
    public R<Map<String, Object>> getAll(@RequestParam(defaultValue = "7") int trendDays,
                                         @RequestParam(defaultValue = "2") int profitMonth,
                                         @RequestParam(defaultValue = "10") int topN)
    {
        return R.ok(statisticsService.dashboardAll(trendDays, profitMonth, topN));
    }
}
