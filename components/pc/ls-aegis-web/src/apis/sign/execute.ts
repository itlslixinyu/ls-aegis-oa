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
export interface UnifiedSignResult {
  recordId: string
  signedFileId: string
  signedFileName?: string
  signatureValidation?: {
    beforeCount: number
    afterCount: number
    addedCount: number
    previousSignaturesPreserved: boolean
    integrityVerified: boolean
    verificationScope: string
  }
}

export interface PdfSignatureReport {
  signatureCount: number
  allSignaturesIntact: boolean
  latestSignatureCoversWholeDocument: boolean
  continuationAllowed: boolean
  message: string
  verificationScope: string
  signatures: { fieldName: string, intact: boolean, coversWholeDocument: boolean, revision: number, signer: string, signedAt: string }[]
}

export function inspectSignatures(file: Blob, name = 'document.pdf') {
  const form = new FormData()
  form.append('file', file, name)
  return http.post<PdfSignatureReport>('/sign/execute/inspect', form, { timeout: 60000 })
}

export function unifiedSign(data: UnifiedSignPayload) {
  return http.post<UnifiedSignResult>('/sign/execute/unified', data, { timeout: 120000 })
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
