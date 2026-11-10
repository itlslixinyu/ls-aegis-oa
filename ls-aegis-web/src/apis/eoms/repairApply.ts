import http from '@/utils/http'

export interface RepairApplyResp {
  id: string
  /** 报修编号（后端字段 repairNo / repair_no） */
  repairNo?: string
  customerId?: string
  customerName?: string
  contactName?: string
  contactPhone?: string
  address?: string
  faultType?: string
  faultDesc?: string
  photos?: string
  photoUrls?: string
  status?: string
  applyTime?: string
  createTime?: string
  remark?: string
}

export function listRepairApply(query: any) {
  return http.get<PageRes<RepairApplyResp[]>>('/eoms/repair/apply/list', query)
}

export function getRepairApply(id: string) {
  return http.get<RepairApplyResp>(`/eoms/repair/apply/${id}`)
}

export function addRepairApply(data: any) {
  return http.post('/eoms/repair/apply', data)
}

export function confirmRepairApply(id: string) {
  return http.put(`/eoms/repair/apply/confirm/${id}`)
}

export function cancelRepairApply(id: string) {
  return http.put(`/eoms/repair/apply/cancel/${id}`)
}

export function closeRepairApply(id: string, reason: string) {
  return http.put(`/eoms/repair/apply/close/${id}?remark=${encodeURIComponent(reason)}`)
}

export function toOrderRepairApply(id: string, data?: any) {
  return http.put(`/eoms/repair/apply/toOrder/${id}`, data)
}
