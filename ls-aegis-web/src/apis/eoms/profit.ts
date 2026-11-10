import http from '@/utils/http'

export interface ProfitItemResp {
  orderId?: string
  orderNo?: string
  customerName?: string
  income?: number
  cost?: number
  profit?: number
  month?: string
}

export interface ProfitSummaryResp {
  totalIncome?: number
  totalCost?: number
  totalProfit?: number
  profitRate?: number
  orderCount?: number
}

export function getProfitList(query: any) {
  return http.get<PageRes<ProfitItemResp[]>>('/eoms/statistics/profitList', query)
}

export function getProfitSummary(query: any) {
  return http.get<ProfitSummaryResp>('/eoms/statistics/profitSummary', query)
}

export function getProfitTrend(params?: any) {
  return http.get<any>('/eoms/statistics/profitTrend', params)
}
