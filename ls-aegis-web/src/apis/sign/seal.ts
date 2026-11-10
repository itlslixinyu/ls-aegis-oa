import http from '@/utils/http'

export interface SealResp {
  sealId: string
  sealName?: string
  sealCode?: string
  sealCategory?: string
  categoryLabel?: string
  deptId?: string
  deptName?: string
  status?: string
  enableDate?: string
  remark?: string
  sealFilePath?: string
  certId?: string
  certStatus?: string
  serialNo?: string
  notBefore?: string
  notAfter?: string
  keyRef?: string
  createTime?: string
}

export interface ExtractRecordResp {
  id: string
  originalFileName?: string
  extractFileName?: string
  extractFileDisplayName?: string
  createTime?: string
  createBy?: string
}

export function listSeal(query: any) {
  return http.get<PageRes<SealResp[]>>('/sign/seal/list', query)
}

export function getSeal(sealId: string) {
  return http.get<SealResp>(`/sign/seal/${sealId}`)
}

export function addSeal(formData: FormData) {
  return http.post('/sign/seal', formData)
}

export function updateSeal(data: FormData | Record<string, any>) {
  const isFormData = typeof FormData !== 'undefined' && data instanceof FormData
  return isFormData
    ? http.post('/sign/seal/update', data)
    : http.put('/sign/seal', data)
}

export function delSeal(sealIds: string | Array<string>) {
  const ids = Array.isArray(sealIds) ? sealIds.join(',') : sealIds
  return http.del(`/sign/seal/${ids}`)
}

export function exportSeal(query: any) {
  return http.download('/sign/seal/export', query)
}

/** 获取当前用户可用印章 */
export function availableSeals(projectId?: string) {
  return http.get<SealResp[]>('/sign/seal/available', { projectId })
}

/** 印章提取-预览（不上传 MinIO，不创建 DB 记录） */
export function extractSealPreview(formData: FormData) {
  return http.post<{ extractImageBase64?: string }>('/sign/seal/extract/preview', formData)
}

/** 印章提取-保存 */
export function extractSealSave(formData: FormData) {
  return http.post<{ recordId?: string }>('/sign/seal/extract/save', formData)
}

export function getExtractList(query: any) {
  return http.get<PageRes<ExtractRecordResp[]>>('/sign/seal/extract/list', query)
}

export function saveExtractFileName(id: string, fileName: string) {
  return http.put(`/sign/seal/extract/${id}`, { extractFileName: fileName })
}

export function deleteExtractRecord(ids: string | Array<string>) {
  const idStr = Array.isArray(ids) ? ids.join(',') : ids
  return http.del(`/sign/seal/extract/${idStr}`)
}

export function bindSealAuth(sealId: string, userIds: Array<string>) {
  return http.post(`/sign/seal/${sealId}/auth/bind`, userIds)
}

export function getNextSealCode() {
  return http.get<string>('/sign/seal/code/next')
}

export function bindSealCert(data: { sealId: string }) {
  return http.post('/sign/seal/bind-cert', data)
}

export function getSealCertInfo(sealId: string) {
  return http.get<Partial<SealResp>>(`/sign/seal/cert-info/${sealId}`)
}

export function removeSealAuth(sealId: string, userIds: string | Array<string>) {
  const ids = Array.isArray(userIds) ? userIds.join(',') : userIds
  return http.del(`/sign/seal/${sealId}/auth/${ids}`)
}

export function getSealAuthUsers(sealId: string) {
  return http.get<Array<string>>(`/sign/seal/${sealId}/auth/users`)
}

export function getSealImageResource(sealId: string) {
  return `/sign/seal/image/${sealId}`
}

export function getExtractImageResource(recordId: string) {
  return `/sign/seal/extract/image/${recordId}`
}

export function getExtractThumbnailResource(recordId: string) {
  return `/sign/seal/extract/image/thumb/${recordId}`
}

export function getOriginalExtractImageResource(recordId: string) {
  return `/sign/seal/extract/image/original/${recordId}`
}

export function getExtractDownloadResource(recordId: string) {
  return `/sign/seal/extract/download/${recordId}`
}

/** 拉取印章/提取图片为 ObjectURL（需调用方 revoke） */
export async function fetchSignImageObjectUrl(url: string) {
  const res = await http.download(url)
  const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
  return URL.createObjectURL(blob)
}

export function downloadExtractFile(recordId: string) {
  return http.download(getExtractDownloadResource(recordId))
}
