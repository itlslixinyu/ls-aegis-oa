import http from '@/utils/http'

export interface RepairRecordResp {
  id: string
  repairOrderId?: string
  orderNo?: string
  engineerName?: string
  createBy?: string
  faultReason?: string
  checkProcess?: string
  solution?: string
  result?: string
  processingPhotos?: string
  completedPhotos?: string
  /** 预览 URL（后端 fill 后返回） */
  processingPhotoUrls?: string
  completedPhotoUrls?: string
  createTime?: string
  remark?: string
}

export function listRepairRecord(query: any) {
  return http.get<PageRes<RepairRecordResp[]>>('/eoms/repair/record/list', query)
}

export function getRepairRecord(id: string) {
  return http.get<RepairRecordResp>(`/eoms/repair/record/${id}`)
}

export function addRepairRecord(data: any) {
  return http.post('/eoms/repair/record', data)
}

export function updateRepairRecord(data: any) {
  return http.put('/eoms/repair/record', data)
}

export function deleteRepairRecord(id: string) {
  return http.del(`/eoms/repair/record/${id}`)
}
