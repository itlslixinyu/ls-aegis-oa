import http from '@/utils/http'

export interface RepairOrderResp {
  id: string
  orderNo?: string
  customerId?: string
  customerName?: string
  engineerId?: string
  engineerName?: string
  address?: string
  status?: string
  createTime?: string
  remark?: string
}

export interface EngineerOption {
  id: string
  nickname?: string
  username?: string
}

export function listRepairOrder(query: any) {
  return http.get<PageRes<RepairOrderResp[]>>('/eoms/repair/order/list', query)
}

export function listEngineerOptions() {
  return http.get<EngineerOption[]>('/eoms/repair/order/engineerOptions')
}

export function getRepairOrder(id: string) {
  return http.get<RepairOrderResp>(`/eoms/repair/order/${id}`)
}

export function addRepairOrder(data: any) {
  return http.post('/eoms/repair/order', data)
}

export function updateRepairOrder(data: any) {
  return http.put('/eoms/repair/order', data)
}

export function deleteRepairOrder(id: string) {
  return http.del(`/eoms/repair/order/${id}`)
}

export function assignEngineer(id: string, engineerId: string) {
  return http.put(`/eoms/repair/order/assign/${id}`, undefined, { params: { engineerId } })
}

export function acceptOrder(id: string) {
  return http.put(`/eoms/repair/order/accept/${id}`)
}

export function startOrder(id: string) {
  return http.put(`/eoms/repair/order/start/${id}`)
}

export function submitOrder(id: string) {
  return http.put(`/eoms/repair/order/submit/${id}`)
}

export function closeOrder(id: string) {
  return http.put(`/eoms/repair/order/close/${id}`)
}

export function cancelReversedOrder(id: string, reason: string) {
  return http.put(`/eoms/repair/order/cancel/${id}`, undefined, { params: { reason } })
}

export function listRepairMaterial(query: any) {
  return http.get<PageRes<any[]>>('/eoms/repair/material/list', query)
}
