package com.ls.aegis.biz.eoms.service.statistics;

import java.util.List;
import java.util.Map;

/**
 * 统计分析 Service 接口
 *
 * @author EOMS
 */
public interface IStatisticsService
{
    /**
     * Dashboard 概览 KPI
     *
     * @return KPI Map
     */
    public Map<String, Object> dashboardOverview();

    /**
     * 工单趋势（近 N 天）
     *
     * @param days 天数
     * @return [{day, count}]
     */
    public List<Map<String, Object>> orderTrend(int days);

    /**
     * 物料消耗 Top N
     *
     * @param limit Top 数量
     * @return [{materialId, name, materialNo, usage}]
     */
    public List<Map<String, Object>> materialTop(int limit);

    /**
     * 利润趋势（近 N 月，收入-成本）
     *
     * @param months 月数
     * @return [{month, income, cost, profit}]
     */
    public List<Map<String, Object>> profitTrend(int months);

    /**
     * 客户分布（按客户类型）
     *
     * @return [{type, count}]
     */
    public List<Map<String, Object>> customerDistribution();

    /**
     * 工程师绩效（工单数 + 已完成数）
     *
     * @param limit Top 数量
     * @return [{engineerId, engineerName, orderCount, completedCount}]
     */
    public List<Map<String, Object>> engineerPerformance(int limit);

    /**
     * 利润明细（按工单维度：收入-成本=利润）
     *
     * @param customerName 客户名称（模糊，可空）
     * @param orderNo      工单号（模糊，可空）
     * @param beginMonth   起始月份（yyyy-MM，可空）
     * @param endMonth     截止月份（yyyy-MM，可空）
     * @return [{orderId, orderNo, customerName, incomeAmount, costAmount, profit, profitRate, settleTime}]
     */
    public List<Map<String, Object>> profitList(String customerName, String orderNo,
                                                String beginMonth, String endMonth);

    /**
     * 利润汇总（总收入、总成本、总利润、利润率）
     *
     * @param customerName 客户名称（模糊，可空）
     * @param orderNo      工单号（模糊，可空）
     * @param beginMonth   起始月份（yyyy-MM，可空）
     * @param endMonth     截止月份（yyyy-MM，可空）
     * @return {totalIncome, totalCost, totalProfit, profitRate, orderCount}
     */
    public Map<String, Object> profitSummary(String customerName, String orderNo,
                                             String beginMonth, String endMonth);

    /**
     * 【P0-1 优化】Dashboard 全部数据一次性返回（7 接口合并为 1）
     * <p>
     * 原首页加载触发 6 次 HTTP 请求：overview + orderTrend + profitTrend +
     * customerDistribution + engineerPerformance + materialTop。每次请求建立独立的
     * DB/Redis 连接，4C4G 以下并发时 Druid 连接池（maxActive 较小）会被瞬间占满。
     * 本接口把 6 组数据合并为单次请求，减少 RTT 和连接池压力 ~85%。
     *
     * @param trendDays   工单趋势近 N 天
     * @param profitMonth 利润趋势近 N 月
     * @param topN        Top N 排行
     * @return {overview, orderTrend, profitTrend, customerDistribution, engineerPerformance, materialTop}
     */
    public Map<String, Object> dashboardAll(int trendDays, int profitMonth, int topN);
}
