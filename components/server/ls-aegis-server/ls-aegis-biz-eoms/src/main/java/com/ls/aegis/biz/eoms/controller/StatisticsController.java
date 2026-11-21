package com.ls.aegis.biz.eoms.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.biz.eoms.service.statistics.IStatisticsService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;

/**
 * 统计分析 Controller
 *
 * C-2 性能优化：orderTrend / materialTop 改为 SQL 聚合（GROUP BY），避免全表加载到内存。
 * D-1 数据看板深化：新增利润趋势、客户分布、工程师绩效接口。
 * D-2 修复 orderTrend 时区漏统计问题，并拆分独立统计 API。
 *
 * @author EOMS
 */
@RestController
@RequestMapping("/eoms/statistics")
public class StatisticsController
{
    @Autowired
    private IStatisticsService statisticsService;

    /**
     * 统计列表占位（兼容路由）
     */
    @SaCheckPermission("eoms:statistics:list")
    @GetMapping("/list")
    public R<PageResp<Object>> list(@Valid PageQuery pageQuery)
    {
        return R.ok(PageQueryHelper.toPageResp(new ArrayList<>()));
    }

    /**
     * 工单趋势（近 N 天，默认 7 天）
     */
    @SaCheckPermission("eoms:statistics:list")
    @GetMapping("/orderTrend")
    public R<List<Map<String, Object>>> orderTrend(@RequestParam(defaultValue = "7") int days)
    {
        if (days < 1 || days > 90) days = 7;
        List<Map<String, Object>> list = statisticsService.orderTrend(days);
        return R.ok(list);
    }

    /**
     * 物料消耗 Top（默认 10）
     */
    @SaCheckPermission("eoms:statistics:list")
    @GetMapping("/materialTop")
    public R<List<Map<String, Object>>> materialTop(@RequestParam(defaultValue = "10") int limit)
    {
        if (limit < 1 || limit > 50) limit = 10;
        List<Map<String, Object>> list = statisticsService.materialTop(limit);
        return R.ok(list);
    }

    /**
     * D-1 利润趋势（近 N 月，收入-成本，默认 6 月）
     */
    @SaCheckPermission("eoms:statistics:list")
    @GetMapping("/profitTrend")
    public R<List<Map<String, Object>>> profitTrend(@RequestParam(defaultValue = "6") int months)
    {
        if (months < 1 || months > 24) months = 6;
        List<Map<String, Object>> list = statisticsService.profitTrend(months);
        return R.ok(list);
    }

    /**
     * 利润明细（按工单维度：收入-成本=利润）
     * 支持按客户名称、工单号、月份范围筛选
     * 权限对齐菜单按钮 eoms:profit:query（利润分析菜单 2301 下的查询按钮 3801）
     */
    @SaCheckPermission("eoms:profit:query")
    @GetMapping("/profitList")
    public R<List<Map<String, Object>>> profitList(@RequestParam(required = false) String customerName,
                                                   @RequestParam(required = false) String orderNo,
                                                   @RequestParam(required = false) String beginMonth,
                                                   @RequestParam(required = false) String endMonth)
    {
        PageQueryHelper.startExportLimit();
        List<Map<String, Object>> list = statisticsService.profitList(customerName, orderNo, beginMonth, endMonth);
        return R.ok(list);
    }

    /**
     * 利润汇总（总收入、总成本、总利润、利润率）
     * 权限对齐菜单按钮 eoms:profit:query
     */
    @SaCheckPermission("eoms:profit:query")
    @GetMapping("/profitSummary")
    public R<Map<String, Object>> profitSummary(@RequestParam(required = false) String customerName,
                                                @RequestParam(required = false) String orderNo,
                                                @RequestParam(required = false) String beginMonth,
                                                @RequestParam(required = false) String endMonth)
    {
        Map<String, Object> result = statisticsService.profitSummary(customerName, orderNo, beginMonth, endMonth);
        return R.ok(result);
    }

    /**
     * D-1 客户分布（按客户类型）
     */
    @SaCheckPermission("eoms:statistics:list")
    @GetMapping("/customerDistribution")
    public R<List<Map<String, Object>>> customerDistribution()
    {
        List<Map<String, Object>> list = statisticsService.customerDistribution();
        return R.ok(list);
    }

    /**
     * D-1 工程师绩效（工单数 + 已完成数，默认 Top 10）
     */
    @SaCheckPermission("eoms:statistics:list")
    @GetMapping("/engineerPerformance")
    public R<List<Map<String, Object>>> engineerPerformance(@RequestParam(defaultValue = "10") int limit)
    {
        if (limit < 1 || limit > 50) limit = 10;
        List<Map<String, Object>> list = statisticsService.engineerPerformance(limit);
        return R.ok(list);
    }
}
