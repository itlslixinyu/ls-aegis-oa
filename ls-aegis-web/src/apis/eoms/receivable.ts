import http from '@/utils/http'

export interface ReceivableResp {
  id: string
  customerId?: string
  customerName?: string
  repairOrderId?: string
  amount?: number
  receivedAmount?: number
  arrearsAmount?: number
  status?: string
  createTime?: string
  remark?: string
}

export function listReceivable(query: any) {
  return http.get<PageRes<ReceivableResp[]>>('/eoms/finance/receivable/list', query)
}

export function listUnsettledReceivable(customerId: string) {
  return http.get<ReceivableResp[]>('/eoms/finance/receivable/unsettled', { customerId })
}

export function getReceivable(id: string) {
  return http.get<ReceivableResp>(`/eoms/finance/receivable/${id}`)
}

export function addReceivable(data: any) {
  return http.post('/eoms/finance/receivable', data)
}

export function updateReceivable(data: any) {
  return http.put('/eoms/finance/receivable', data)
}

export function deleteReceivable(id: string) {
  return http.del(`/eoms/finance/receivable/${id}`)
}
