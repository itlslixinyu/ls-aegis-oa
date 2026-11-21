import http from '@/utils/http'

export interface ContractResp {
  id: string
  contractNo?: string
  contractName?: string
  customerId?: string
  customerName?: string
  amount?: number
  signDate?: string
  status?: string
  startDate?: string
  endDate?: string
  createTime?: string
  remark?: string
  /** 平台 sys_file.id，多个逗号分隔 */
  fileId?: string
  /** 执行进度 0~1 */
  progress?: number
}

export function listContract(query: any) {
  return http.get<PageRes<ContractResp[]>>('/eoms/contract/list', query)
}

export function getContract(id: string) {
  return http.get<ContractResp>(`/eoms/contract/${id}`)
}

export function addContract(data: any) {
  return http.post('/eoms/contract', data)
}

export function updateContract(data: any) {
  return http.put('/eoms/contract', data)
}

export function deleteContract(id: string) {
  return http.del(`/eoms/contract/${id}`)
}

export function confirmContract(id: string) {
  return http.post<string>(`/eoms/contract/${id}/confirm`)
}

export function exportContract(query: any) {
  return http.download('/eoms/contract/export', query)
}
