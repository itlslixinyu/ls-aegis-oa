package com.ls.aegis.biz.eoms.service.impl.statistics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ls.aegis.compat.common.core.redis.RedisCache;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.mapper.statistics.StatisticsMapper;
import com.ls.aegis.biz.eoms.service.statistics.IStatisticsService;

/**
 * 统计分析 Service 实现（SQL 聚合 + 月份序列补齐 + Redis 缓存）
 *
 * C-3 缓存策略：
 * 1. Dashboard 概览缓存 30s（高频访问，容忍短暂延迟）
 * 2. 趋势/分布/绩效缓存 60s（变化较慢）
 * 3. null 防护：DB 返回 null 时回退默认值并缓存，避免缓存穿透
 *
 * @author EOMS
 */
@Service
public class StatisticsServiceImpl implements IStatisticsService
{
    @Autowired
    private StatisticsMapper statisticsMapper;

    @Autowired
    private RedisCache redisCache;

    // P0-2 修复：SimpleDateFormat 非线程安全，改为 Java 8 DateTimeFormatter（线程安全、不可变）
    private static final java.time.format.DateTimeFormatter MONTH_FMT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
    private static final java.time.format.DateTimeFormatter DATETIME_FMT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final java.time.format.DateTimeFormatter DATE_FMT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 缓存 key 前缀 */
    private static final String CACHE_PREFIX = "eoms:stats:";
    /** 概览缓存 TTL（秒） */
    private static final int OVERVIEW_TTL = 30;
    /** 趋势/分布缓存 TTL（秒） */
    private static final int TREND_TTL = 60;

    @Override
    public Map<String, Object> dashboardOverview()
    {
        String key = CACHE_PREFIX + "overview:v2";
        Map<String, Object> cached = redisCache.getCacheObject(key);
        if (cached != null)
        {
            return cached;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        String monthStart = formatCal(DATETIME_FMT, cal);
        Map<String, Object> data = statisticsMapper.dashboardOverview(monthStart);
        if (data == null)
        {
            // null 防护：DB 异常或空表时回退默认值
            data = new LinkedHashMap<>();
        }
        // 兜底：确保所有 KPI 字段存在，避免前端取 null
        putIfAbsent(data, "customerTotal", 0);
        putIfAbsent(data, "thisMonthCustomer", 0);
        putIfAbsent(data, "orderTotal", 0);
        putIfAbsent(data, "waitSettlement", 0);
        putIfAbsent(data, "waitRepair", 0);
        putIfAbsent(data, "waitAccept", 0);
        putIfAbsent(data, "processing", 0);
        putIfAbsent(data, "waitApply", 0);
        putIfAbsent(data, "thisMonthOrder", 0);
        putIfAbsent(data, "totalIncome", BigDecimal.ZERO);
        putIfAbsent(data, "monthIncome", BigDecimal.ZERO);
        putIfAbsent(data, "stockAlert", 0);
        putIfAbsent(data, "stockTotal", 0);
        redisCache.setCacheObject(key, data, OVERVIEW_TTL, TimeUnit.SECONDS);
        return data;
    }

    @Override
    public List<Map<String, Object>> orderTrend(int days)
    {
        String key = CACHE_PREFIX + "orderTrend:" + days;
        List<Map<String, Object>> cached = redisCache.getCacheObject(key);
        if (cached != null)
        {
            return cached;
        }
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);
        end.add(Calendar.DAY_OF_MONTH, 1); // 截止为次日 00:00（不含）
        Calendar begin = (Calendar) end.clone();
        begin.add(Calendar.DAY_OF_MONTH, -days);

        String beginDate = formatCal(DATETIME_FMT, begin);
        String endDate = formatCal(DATETIME_FMT, end);

        // SQL 聚合结果 day(yyyy-MM-dd) -> count
        List<Map<String, Object>> rows = statisticsMapper.orderTrend(beginDate, endDate);
        Map<String, Object> hit = new LinkedHashMap<>();
        if (rows != null)
        {
            for (Map<String, Object> r : rows)
            {
                Object day = r.get("day");
                if (day != null)
                {
                    hit.put(day.toString(), r.get("count"));
                }
            }
        }
        // 补齐连续 N 天序列，无数据的日期补 0
        List<Map<String, Object>> list = new ArrayList<>();
        Calendar cur = (Calendar) begin.clone();
        for (int i = 0; i < days; i++)
        {
            String keyDay = formatCal(DATE_FMT, cur);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("day", keyDay);
            Object cnt = hit.get(keyDay);
            m.put("count", cnt != null ? cnt : 0);
            list.add(m);
            cur.add(Calendar.DAY_OF_MONTH, 1);
        }
        redisCache.setCacheObject(key, list, TREND_TTL, TimeUnit.SECONDS);
        return list;
    }

    @Override
    public List<Map<String, Object>> materialTop(int limit)
    {
        String key = CACHE_PREFIX + "materialTop:" + limit;
        List<Map<String, Object>> cached = redisCache.getCacheObject(key);
        if (cached != null)
        {
            return cached;
        }
        List<Map<String, Object>> rows = statisticsMapper.materialTop(limit);
        if (rows == null)
        {
            rows = new ArrayList<>();
        }
        redisCache.setCacheObject(key, rows, TREND_TTL, TimeUnit.SECONDS);
        return rows;
    }

    @Override
    public List<Map<String, Object>> profitTrend(int months)
    {
        String key = CACHE_PREFIX + "profitTrend:" + months;
        List<Map<String, Object>> cached = redisCache.getCacheObject(key);
        if (cached != null)
        {
            return cached;
        }
        Calendar cur = Calendar.getInstance();
        // 起始月：往前 months-1 个月
        cur.add(Calendar.MONTH, -(months - 1));
        cur.set(Calendar.DAY_OF_MONTH, 1);
        cur.set(Calendar.HOUR_OF_DAY, 0);
        cur.set(Calendar.MINUTE, 0);
        cur.set(Calendar.SECOND, 0);
        cur.set(Calendar.MILLISECOND, 0);
        // P1-4 修复：使用完整日期时间格式作为范围查询起点，替代 DATE_FORMAT 函数比较
        String beginMonthStart = formatCal(DATETIME_FMT, cur);

        // 收入、成本各自按月聚合，再合并到连续月份序列
        List<Map<String, Object>> incomeRows = statisticsMapper.incomeTrendByMonth(beginMonthStart);
        List<Map<String, Object>> costRows = statisticsMapper.costTrendByMonth(beginMonthStart);
        Map<String, BigDecimal> incomeMap = toMonthAmountMap(incomeRows);
        Map<String, BigDecimal> costMap = toMonthAmountMap(costRows);

        List<Map<String, Object>> list = new ArrayList<>();
        Calendar m = (Calendar) cur.clone();
        for (int i = 0; i < months; i++)
        {
            String keyMonth = formatCal(MONTH_FMT, m);
            BigDecimal income = incomeMap.getOrDefault(keyMonth, BigDecimal.ZERO);
            BigDecimal cost = costMap.getOrDefault(keyMonth, BigDecimal.ZERO);
            BigDecimal profit = income.subtract(cost);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", keyMonth);
            row.put("income", income);
            row.put("cost", cost);
            row.put("profit", profit);
            list.add(row);
            m.add(Calendar.MONTH, 1);
        }
        redisCache.setCacheObject(key, list, TREND_TTL, TimeUnit.SECONDS);
        return list;
    }

    @Override
    public List<Map<String, Object>> customerDistribution()
    {
        String key = CACHE_PREFIX + "customerDist";
        List<Map<String, Object>> cached = redisCache.getCacheObject(key);
        if (cached != null)
        {
            return cached;
        }
        List<Map<String, Object>> rows = statisticsMapper.customerDistribution();
        if (rows == null)
        {
            rows = new ArrayList<>();
        }
        redisCache.setCacheObject(key, rows, TREND_TTL, TimeUnit.SECONDS);
        return rows;
    }

    @Override
    public List<Map<String, Object>> engineerPerformance(int limit)
    {
        String key = CACHE_PREFIX + "engineerPerf:" + limit;
        List<Map<String, Object>> cached = redisCache.getCacheObject(key);
        if (cached != null)
        {
            return cached;
        }
        List<Map<String, Object>> rows = statisticsMapper.engineerPerformance(limit);
        if (rows == null)
        {
            rows = new ArrayList<>();
        }
        redisCache.setCacheObject(key, rows, TREND_TTL, TimeUnit.SECONDS);
        return rows;
    }

    /**
     * 利润明细（按工单维度：收入-成本=利润）
     * 月份格式 yyyy-MM 转换为 yyyy-MM-01 00:00:00 供 SQL 范围查询
     */
    @Override
    public List<Map<String, Object>> profitList(String customerName, String orderNo,
                                                String beginMonth, String endMonth)
    {
        String beginMonthStart = monthToStart(beginMonth);
        String endMonthNext = monthToNextStart(endMonth);
        List<Map<String, Object>> rows = statisticsMapper.profitList(customerName, orderNo, beginMonthStart, endMonthNext);
        return rows == null ? new ArrayList<>() : rows;
    }

    /**
     * 利润汇总（总收入、总成本、总利润、利润率）
     */
    @Override
    public Map<String, Object> profitSummary(String customerName, String orderNo,
                                             String beginMonth, String endMonth)
    {
        String beginMonthStart = monthToStart(beginMonth);
        String endMonthNext = monthToNextStart(endMonth);
        Map<String, Object> result = statisticsMapper.profitSummary(customerName, orderNo, beginMonthStart, endMonthNext);
        if (result == null)
        {
            result = new HashMap<>();
            result.put("totalIncome", BigDecimal.ZERO);
            result.put("totalCost", BigDecimal.ZERO);
            result.put("totalProfit", BigDecimal.ZERO);
            result.put("profitRate", BigDecimal.ZERO);
            result.put("orderCount", 0);
        }
        return result;
    }

    /**
     * yyyy-MM → yyyy-MM-01 00:00:00（空值返回 null）
     */
    private String monthToStart(String month)
    {
        if (StringUtils.isEmpty(month))
        {
            return null;
        }
        return month + "-01 00:00:00";
    }

    /**
     * yyyy-MM → 下月1号 00:00:00（空值返回 null）
     * 用于 SQL 范围查询的右边界（不含）
     */
    private String monthToNextStart(String month)
    {
        if (StringUtils.isEmpty(month))
        {
            return null;
        }
        try
        {
            java.time.YearMonth ym = java.time.YearMonth.parse(month,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            java.time.LocalDate nextStart = ym.plusMonths(1).atDay(1);
            return nextStart.toString() + " 00:00:00";
        }
        catch (Exception e)
        {
            return null;
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * P0-2 修复：线程安全的日期格式化辅助方法。
     * DateTimeFormatter 不可变且线程安全，但 format() 需要 TemporalAccessor 参数，
     * 此方法封装 Calendar → Instant → ZoneId 转换，保持与原 SimpleDateFormat 一致的时区行为。
     */
    private static String formatCal(java.time.format.DateTimeFormatter fmt, Calendar cal)
    {
        return fmt.format(cal.toInstant().atZone(java.time.ZoneId.systemDefault()));
    }

    private static void putIfAbsent(Map<String, Object> data, String key, Object def)
    {
        if (!data.containsKey(key) || data.get(key) == null)
        {
            data.put(key, def);
        }
    }

    private static Map<String, BigDecimal> toMonthAmountMap(List<Map<String, Object>> rows)
    {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (rows == null) return map;
        for (Map<String, Object> r : rows)
        {
            Object month = r.get("month");
            Object amount = r.get("amount");
            if (month != null)
            {
                BigDecimal val = BigDecimal.ZERO;
                if (amount instanceof BigDecimal) val = (BigDecimal) amount;
                else if (amount != null) val = new BigDecimal(amount.toString());
                map.put(month.toString(), val);
            }
        }
        return map;
    }

    @Override
    public Map<String, Object> dashboardAll(int trendDays, int profitMonth, int topN)
    {
        // 各组数据均已在子方法内单独走 Redis 缓存，这里只是简单聚合；
        // 给聚合结果再加一个 5s 的短缓存，进一步降低 RTT + 连接池压力。
        String aggKey = StringUtils.format("{}all:{}:{}:{}", CACHE_PREFIX, trendDays, profitMonth, topN);
        Map<String, Object> cached = redisCache.getCacheObject(aggKey);
        if (cached != null)
        {
            return cached;
        }
        Map<String, Object> result = new LinkedHashMap<>(8);
        result.put("overview", dashboardOverview());
        result.put("orderTrend", orderTrend(trendDays));
        result.put("profitTrend", profitTrend(profitMonth));
        result.put("customerDistribution", customerDistribution());
        result.put("engineerPerformance", engineerPerformance(topN));
        result.put("materialTop", materialTop(topN));
        redisCache.setCacheObject(aggKey, result, 5, TimeUnit.SECONDS);
        return result;
    }
}
