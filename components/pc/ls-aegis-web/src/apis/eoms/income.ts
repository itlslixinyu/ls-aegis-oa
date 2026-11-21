import http from '@/utils/http'

export interface IncomeResp {
  id: string
  incomeNo?: string
  repairOrderId?: string
  amount?: number
  incomeType?: string
  createTime?: string
  remark?: string
}

export function listIncome(query: any) {
  return http.get<PageRes<IncomeResp[]>>('/eoms/finance/income/list', query)
}

export function getIncome(id: string) {
  return http.get<IncomeResp>(`/eoms/finance/income/${id}`)
}

export function addIncome(data: any) {
  return http.post('/eoms/finance/income', data)
}

export function updateIncome(data: any) {
  return http.put('/eoms/finance/income', data)
}

export function deleteIncome(id: string) {
  return http.del(`/eoms/finance/income/${id}`)
}
