import http from '@/utils/http'

export interface SignPositionPayload {
  page: number
  x: number
  y: number
  width: number
  height: number
}

export interface GeneralSealPayload {
  sealId: string
  mode: 'single' | 'fixed' | 'custom'
  positions: SignPositionPayload[]
}

export interface SpanSealPayload {
  sealId: string
  startPage: number
  endPage: number
  xPosition: number
  yPosition: number
  sealSize: number
}

export interface UnifiedSignPayload {
  projectId?: string
  sysFileId?: string
  fileId?: string
  sealId?: string
  generalSeals?: GeneralSealPayload[]
  spanSeals?: SpanSealPayload[]
  hasTimestamp?: boolean
  appendMode?: boolean
}

export function getSignableProjects() {
  return http.get<any[]>('/sign/execute/projects')
}

export function prepareSign(data: Record<string, unknown>) {
  return http.post('/sign/execute/prepare', data, { timeout: 60000 })
}

export function runSign(data: Record<string, unknown>) {
  return http.post('/sign/execute/run', data, { timeout: 120000 })
}

/** 统一签章（合并签章 - 一次数字签名） */
export function unifiedSign(data: UnifiedSignPayload) {
  return http.post('/sign/execute/unified', data, { timeout: 120000 })
}

export function extractCertInfo(fileId: string) {
  return http.post('/sign/execute/cert-info', fileId)
}

export function getVerifyReceipt(recordId: string) {
  return http.get<any>(`/sign/execute/receipt/${recordId}`)
}

export function downloadVerifyReceiptPdf(recordId: string) {
  return http.download(`/sign/execute/receipt/${recordId}/download`)
}
