package com.ls.aegis.biz.eoms.controller;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.biz.eoms.model.resp.ReportCustomerDistExportResp;
import com.ls.aegis.biz.eoms.model.resp.ReportEngineerPerfExportResp;
import com.ls.aegis.biz.eoms.model.resp.ReportKpiExportResp;
import com.ls.aegis.biz.eoms.model.resp.ReportMaterialTopExportResp;
import com.ls.aegis.biz.eoms.model.resp.ReportOrderTrendExportResp;
import com.ls.aegis.biz.eoms.model.resp.ReportProfitTrendExportResp;
import com.ls.aegis.biz.eoms.service.statistics.IStatisticsService;
import com.ls.aegis.common.api.system.SysOptionApi;
import top.continew.starter.log.annotation.Log;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;
import com.ls.aegis.starter.support.excel.ExcelSheetWrite;

/**
 * 报表导出（EasyExcel 单/多 Sheet）
 */
@RestController
@RequestMapping("/eoms/report")
public class ReportExportController {

    @Autowired
    private IStatisticsService statisticsService;

    @Autowired
    private SysOptionApi sysOptionApi;

    private String getSystemName() {
        String value = sysOptionApi.getValue("SITE", "SITE_TITLE");
        return (value == null || value.isEmpty()) ? "EOMS" : value;
    }

    @SaCheckPermission("eoms:dashboard:list")
    @Log(module = "Dashboard概览报表-导出")
    @GetMapping("/export/dashboard")
    public void exportDashboard(HttpServletResponse response) {
        Map<String, Object> overview = statisticsService.dashboardOverview();
        List<ReportKpiExportResp> rows = new ArrayList<>();
        ReportKpiExportResp time = new ReportKpiExportResp();
        time.setLabel("导出时间");
        time.setValue(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        rows.add(time);
        for (Map.Entry<String, String> e : buildOverviewLabelMap().entrySet()) {
            ReportKpiExportResp row = new ReportKpiExportResp();
            row.setLabel(e.getValue());
            row.setValue(formatValue(overview == null ? null : overview.get(e.getKey())));
            rows.add(row);
        }
        ExcelExportHelper.export(rows, ReportKpiExportResp.class, getSystemName() + "_Dashboard概览报表", response);
    }

    @SaCheckPermission("eoms:statistics:list")
    @Log(module = "统计分析报表-导出")
    @GetMapping("/export/statistics")
    public void exportStatistics(HttpServletResponse response,
                                 @RequestParam(defaultValue = "7") int days,
                                 @RequestParam(defaultValue = "6") int months,
                                 @RequestParam(defaultValue = "10") int limit) {
        if (days < 1 || days > 90) {
            days = 7;
        }
        if (months < 1 || months > 24) {
            months = 6;
        }
        if (limit < 1 || limit > 50) {
            limit = 10;
        }
        List<Map<String, Object>> orderTrend = statisticsService.orderTrend(days);
        List<Map<String, Object>> profitTrend = statisticsService.profitTrend(months);
        List<Map<String, Object>> materialTop = statisticsService.materialTop(limit);
        List<Map<String, Object>> customerDist = statisticsService.customerDistribution();
        List<Map<String, Object>> engineerPerf = statisticsService.engineerPerformance(limit);

        ExcelExportHelper.exportSheets(getSystemName() + "_统计分析报表", response,
            ExcelSheetWrite.of("工单趋势", ReportOrderTrendExportResp.class, toOrderTrend(orderTrend)),
            ExcelSheetWrite.of("利润趋势", ReportProfitTrendExportResp.class, toProfitTrend(profitTrend)),
            ExcelSheetWrite.of("物料消耗Top", ReportMaterialTopExportResp.class, toMaterialTop(materialTop)),
            ExcelSheetWrite.of("客户分布", ReportCustomerDistExportResp.class, toCustomerDist(customerDist)),
            ExcelSheetWrite.of("工程师绩效", ReportEngineerPerfExportResp.class, toEngineerPerf(engineerPerf)));
    }

    private List<ReportOrderTrendExportResp> toOrderTrend(List<Map<String, Object>> data) {
        List<ReportOrderTrendExportResp> rows = new ArrayList<>();
        for (Map<String, Object> src : emptyIfNull(data)) {
            ReportOrderTrendExportResp row = new ReportOrderTrendExportResp();
            row.setDay(formatValue(src.get("day")));
            row.setCount(formatValue(src.get("count")));
            rows.add(row);
        }
        return rows;
    }

    private List<ReportProfitTrendExportResp> toProfitTrend(List<Map<String, Object>> data) {
        List<ReportProfitTrendExportResp> rows = new ArrayList<>();
        for (Map<String, Object> src : emptyIfNull(data)) {
            ReportProfitTrendExportResp row = new ReportProfitTrendExportResp();
            row.setMonth(formatValue(src.get("month")));
            row.setIncome(formatValue(src.get("income")));
            row.setCost(formatValue(src.get("cost")));
            row.setProfit(formatValue(src.get("profit")));
            rows.add(row);
        }
        return rows;
    }

    private List<ReportMaterialTopExportResp> toMaterialTop(List<Map<String, Object>> data) {
        List<ReportMaterialTopExportResp> rows = new ArrayList<>();
        for (Map<String, Object> src : emptyIfNull(data)) {
            ReportMaterialTopExportResp row = new ReportMaterialTopExportResp();
            row.setMaterialId(formatValue(src.get("materialId")));
            row.setMaterialNo(formatValue(src.get("materialNo")));
            row.setName(formatValue(src.get("name")));
            row.setUsage(formatValue(src.get("usage")));
            rows.add(row);
        }
        return rows;
    }

    private List<ReportCustomerDistExportResp> toCustomerDist(List<Map<String, Object>> data) {
        List<ReportCustomerDistExportResp> rows = new ArrayList<>();
        for (Map<String, Object> src : emptyIfNull(data)) {
            ReportCustomerDistExportResp row = new ReportCustomerDistExportResp();
            row.setType(formatValue(src.get("type")));
            row.setCount(formatValue(src.get("count")));
            rows.add(row);
        }
        return rows;
    }

    private List<ReportEngineerPerfExportResp> toEngineerPerf(List<Map<String, Object>> data) {
        List<ReportEngineerPerfExportResp> rows = new ArrayList<>();
        for (Map<String, Object> src : emptyIfNull(data)) {
            ReportEngineerPerfExportResp row = new ReportEngineerPerfExportResp();
            row.setEngineerId(formatValue(src.get("engineerId")));
            row.setEngineerName(formatValue(src.get("engineerName")));
            row.setOrderCount(formatValue(src.get("orderCount")));
            row.setCompletedCount(formatValue(src.get("completedCount")));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> emptyIfNull(List<Map<String, Object>> data) {
        return data == null ? List.of() : data;
    }

    private String formatValue(Object val) {
        if (val == null) {
            return "";
        }
        if (val instanceof BigDecimal decimal) {
            return decimal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        return val.toString();
    }

    private Map<String, String> buildOverviewLabelMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("customerTotal", "客户总数");
        m.put("thisMonthCustomer", "本月新增客户");
        m.put("orderTotal", "工单总数");
        m.put("thisMonthOrder", "本月工单");
        m.put("waitRepair", "待维修");
        m.put("waitSettlement", "待结算单数");
        m.put("totalIncome", "累计收入(元)");
        m.put("monthIncome", "当月收入(元)");
        m.put("stockTotal", "库存种类");
        m.put("stockAlert", "库存告警");
        return m;
    }
}
