package com.ls.aegis.biz.eoms.mapper.statistics;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 统计分析 Mapper 接口（SQL 聚合查询，替代 Java 内存统计）
 *
 * 性能优化要点：
 * 1. 全部使用 COUNT/SUM/GROUP BY 聚合，避免全表加载到内存
 * 2. WHERE 条件使用范围查询（create_time >= ?），确保走索引
 * 3. Dashboard 概览使用标量子查询合并为单次 SQL，减少 DB 往返
 *
 * @author EOMS
 */
public interface StatisticsMapper
{
    /**
     * Dashboard 概览（单次 SQL 标量子查询，返回全部 KPI）
     *
     * @param monthStart 当月1号 00:00:00（yyyy-MM-dd HH:mm:ss）
     * @return KPI Map
     */
    public Map<String, Object> dashboardOverview(@Param("monthStart") String monthStart);

    /**
     * 工单趋势（按天聚合，近 N 天）
     *
     * @param beginDate 起始日期（yyyy-MM-dd 00:00:00）
     * @param endDate   截止日期（yyyy-MM-dd 00:00:00，不含）
     * @return [{day, count}]
     */
    public List<Map<String, Object>> orderTrend(@Param("beginDate") String beginDate,
                                                @Param("endDate") String endDate);

    /**
     * 物料消耗 Top N（OUT 出库聚合）
     *
     * @param limit Top 数量
     * @return [{materialId, name, materialNo, usage}]
     */
    public List<Map<String, Object>> materialTop(@Param("limit") int limit);

    /**
     * 收入按月聚合（利润趋势用）
     *
     * @param beginMonthStart 起始月初（yyyy-MM-01 00:00:00，范围查询走索引）
     * @return [{month, amount}]
     */
    public List<Map<String, Object>> incomeTrendByMonth(@Param("beginMonthStart") String beginMonthStart);

    /**
     * 成本按月聚合（利润趋势用）
     *
     * @param beginMonthStart 起始月初（yyyy-MM-01 00:00:00，范围查询走索引）
     * @return [{month, amount}]
     */
    public List<Map<String, Object>> costTrendByMonth(@Param("beginMonthStart") String beginMonthStart);

    /**
     * 客户分布（按客户类型聚合）
     *
     * @return [{type, count}]
     */
    public List<Map<String, Object>> customerDistribution();

    /**
     * 工程师绩效（工单数 + 已完成数，按工程师聚合）
     *
     * @param limit Top 数量
     * @return [{engineerId, engineerName, orderCount, completedCount}]
     */
    public List<Map<String, Object>> engineerPerformance(@Param("limit") int limit);

    /**
     * 利润明细（按工单维度聚合：收入-成本=利润）
     * <p>
     * 收入来源：eoms_income（客户实际付款，按 repair_order_id 聚合）
     * 成本来源：eoms_cost（材料+人工+其它，按 repair_order_id 聚合）
     * 利润 = 收入 - 成本；利润率 = 利润 / 收入 * 100%
     * 只返回有收入或成本的工单，支持按客户名称、工单号、月份筛选
     * </p>
     *
     * @param customerName 客户名称（模糊，可空）
     * @param orderNo      工单号（模糊，可空）
     * @param beginMonth   起始月份（yyyy-MM-01 00:00:00，可空）
     * @param endMonthNext 截止月份下月1号（yyyy-MM-01 00:00:00，可空，范围不含）
     * @return [{orderId, orderNo, customerName, incomeAmount, costAmount, profit, profitRate, settleTime}]
     */
    public List<Map<String, Object>> profitList(@Param("customerName") String customerName,
                                                @Param("orderNo") String orderNo,
                                                @Param("beginMonth") String beginMonth,
                                                @Param("endMonthNext") String endMonthNext);

    /**
     * 利润汇总（总收入、总成本、总利润、利润率）
     * <p>
     * 与 profitList 同源同筛选条件，聚合为单行汇总数据。
     * </p>
     *
     * @param customerName 客户名称（模糊，可空）
     * @param orderNo      工单号（模糊，可空）
     * @param beginMonth   起始月份（yyyy-MM-01 00:00:00，可空）
     * @param endMonthNext 截止月份下月1号（yyyy-MM-01 00:00:00，可空，范围不含）
     * @return {totalIncome, totalCost, totalProfit, profitRate}
     */
    public Map<String, Object> profitSummary(@Param("customerName") String customerName,
                                             @Param("orderNo") String orderNo,
                                             @Param("beginMonth") String beginMonth,
                                             @Param("endMonthNext") String endMonthNext);
}
