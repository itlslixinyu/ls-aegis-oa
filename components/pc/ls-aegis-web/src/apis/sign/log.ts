import http from '@/utils/http'

export interface SignRecordResp {
  recordId?: string
  projectId?: string
  projectName?: string
  sealId?: string
  sealName?: string
  fileName?: string
  signedFileName?: string
  signedFileId?: string
  nickName?: string
  userName?: string
  signMode?: string
  signTime?: string
  clientIp?: string
  ipLocation?: string
  osVersion?: string
  browserVersion?: string
  hasTimestamp?: string
  positions?: string
  status?: string
}

export interface SignLogResp {
  id?: string
  [key: string]: any
}

export interface AuditLogResp {
  actionType?: string
  nickName?: string
  userName?: string
  projectName?: string
  sealName?: string
  clientIp?: string
  ipLocation?: string
  resultCode?: string
  signTime?: string
}

export function listSignRecord(query: any) {
  return http.get<PageRes<SignRecordResp[]>>('/sign/record/list', query)
}

export function getSignRecord(recordId: string) {
  return http.get<SignRecordResp>(`/sign/record/${recordId}`)
}

export function exportSignRecord(query: any) {
  return http.download('/sign/record/export', query)
}

export function listSignLog(query: any) {
  return http.get<PageRes<SignLogResp[]>>('/sign/log/list', query)
}

export function exportSignLog(query: any) {
  return http.download('/sign/log/export', query)
}

export function verifyFingerprint(data: any) {
  return http.post('/sign/verify/fingerprint', data)
}

export function verifyFingerprintUpload(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<any>('/sign/verify/fingerprint/upload', formData)
}

export function listAuditLog(query: any) {
  return http.get<PageRes<AuditLogResp[]>>('/sign/audit/log/list', query)
}
