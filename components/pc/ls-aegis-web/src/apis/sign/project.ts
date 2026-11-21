import http from '@/utils/http'

export interface ProjectResp {
  projectId: string
  projectName?: string
  projectCode?: string
  managerIds?: string[]
  managerNames?: string
  deptId?: string
  deptName?: string
  startDate?: string
  endDate?: string
  status?: string
  description?: string
  contractType?: string
  contractId?: string
  contractNo?: string
  contractName?: string
  partnerName?: string
  saleContractId?: string
  saleContractNo?: string
  saleContractName?: string
  salePartnerName?: string
  purchaseContractId?: string
  purchaseContractNo?: string
  purchaseContractName?: string
  purchasePartnerName?: string
  repairContractId?: string
  repairContractNo?: string
  repairContractName?: string
  repairPartnerName?: string
  /** 平台 sys_file.id，逗号分隔 */
  otherFileIds?: string
  sealIds?: Array<string>
  createTime?: string
}

export interface ProjectContractOption {
  id: string
  contractNo?: string
  contractName?: string
  partnerName?: string
  /** 合同附件 sys_file.id，逗号分隔 */
  fileId?: string
}

export function listProject(query: any) {
  return http.get<PageRes<ProjectResp[]>>('/sign/project/list', query)
}

export function listProjectContracts(query: {
  contractType: string
  keyword?: string
  excludeProjectId?: string
}) {
  return http.get<ProjectContractOption[]>('/sign/project/contracts', query)
}

export function getProject(projectId: string) {
  return http.get<ProjectResp>(`/sign/project/${projectId}`)
}

export function addProject(data: any) {
  return http.post('/sign/project', data)
}

export function updateProject(data: any) {
  return http.put('/sign/project', data)
}

export function delProject(projectIds: string | Array<string>) {
  const ids = Array.isArray(projectIds) ? projectIds.join(',') : projectIds
  return http.del(`/sign/project/${ids}`)
}

export function reviewProject(projectId: string, status: string) {
  return http.put(`/sign/project/review/${projectId}`, undefined, { params: { status } })
}

export function exportProject(query: any) {
  return http.download('/sign/project/export', query)
}
