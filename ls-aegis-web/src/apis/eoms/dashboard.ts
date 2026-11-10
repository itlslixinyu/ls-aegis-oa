import http from '@/utils/http'

export function getDashboardAll(params?: any) {
  return http.get<any>('/eoms/dashboard/all', params)
}

export function getOverview() {
  return http.get<any>('/eoms/dashboard/overview')
}

/** 统计分析用；仪表盘不再拉趋势 / Top */

export function getOrderTrend(params?: any) {
  return http.get<any>('/eoms/statistics/orderTrend', params)
}

export function getMaterialTop(params?: any) {
  return http.get<any>('/eoms/statistics/materialTop', params)
}

// getProfitTrend 定义在 profit.ts，避免 barrel re-export 冲突

export function getCustomerDistribution() {
  return http.get<any>('/eoms/statistics/customerDistribution')
}

export function getEngineerPerformance(params?: any) {
  return http.get<any>('/eoms/statistics/engineerPerformance', params)
}

export function exportDashboardReport() {
  return http.download('/eoms/report/export/dashboard')
}

export function exportStatisticsReport(params?: any) {
  return http.download('/eoms/report/export/statistics', params)
}
