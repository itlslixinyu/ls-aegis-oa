import http from '@/utils/http'

export interface RepairSettlementResp {
  id: string
  settlementNo?: string
  repairOrderId?: string
  orderId?: string
  orderNo?: string
  customerName?: string
  /** 人工费（对应后端 laborAmount） */
  laborAmount?: number
  /** 材料费/材料收入（对应后端 materialAmount） */
  materialAmount?: number
  /** 合计收入（对应后端 totalIncome） */
  totalIncome?: number
  serviceFee?: number
  status?: string
  createTime?: string
  remark?: string
}

export function listSettlement(query: any) {
  return http.get<PageRes<RepairSettlementResp[]>>('/eoms/repair/settlement/list', query)
}

export function getSettlement(id: string) {
  return http.get<RepairSettlementResp>(`/eoms/repair/settlement/${id}`)
}

export function addSettlement(data: any) {
  return http.post('/eoms/repair/settlement', data)
}

export function updateSettlement(data: any) {
  return http.put('/eoms/repair/settlement', data)
}

export function deleteSettlement(id: string) {
  return http.del(`/eoms/repair/settlement/${id}`)
}

export function confirmSettlement(id: string) {
  return http.put(`/eoms/repair/settlement/confirm/${id}`)
}

export function getSettlementDetail(id: string) {
  return http.get<any>(`/eoms/repair/settlement/detail/${id}`)
}

export function listSettlementMaterial(query: any) {
  return http.get<PageRes<any[]>>('/eoms/repair/material/list', query)
}

export function addSettlementMaterial(data: any) {
  return http.post('/eoms/repair/material', data)
}

export function updateSettlementMaterial(data: any) {
  return http.put('/eoms/repair/material', data)
}

export function deleteSettlementMaterial(ids: string) {
  return http.del(`/eoms/repair/material/${ids}`)
}

export function reverseSettlement(id: string, reason: string) {
  return http.put(`/eoms/repair/settlement/reverse/${id}?reason=${encodeURIComponent(reason)}`)
}
