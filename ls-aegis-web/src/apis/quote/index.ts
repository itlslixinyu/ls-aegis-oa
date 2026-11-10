/**
 * 报价模块 API：平台 REST（/quote）
 */
import http from '@/utils/http'

/** 平台报价单响应（金额字段为 totalAmount，兼容序列化 amount） */
export interface QuoteResp {
  id: string
  quoteNo?: string
  customerId?: string
  customerName?: string
  customerNo?: string
  projectName?: string
  quoteUnit?: string
  quoteDate?: string
  validUntil?: string
  taxRate?: number
  materialSubtotal?: number
  serviceSubtotal?: number
  terms?: string
  repairOrderId?: string
  orderNo?: string
  totalAmount?: number
  /** 兼容别名，与 totalAmount 同源 */
  amount?: number
  status?: string
  createTime?: string
  remark?: string
  details?: QuoteDetailResp[]
}

export interface QuoteDetailResp {
  id?: string
  quoteId?: string
  lineType?: 'MATERIAL' | 'SERVICE' | string
  goodsId?: string
  itemName?: string
  brandModel?: string
  unit?: string
  description?: string
  sortNo?: number
  quantity?: number
  price?: number
  amount?: number
  remark?: string
}

export type QuotePageQuery = Partial<QuoteResp> & {
  page?: number
  size?: number
  customerName?: string
  /** ACTIVE=进行中(草稿/已提交/已取消)，HISTORY=历史(仅已确认) */
  statusScope?: 'ACTIVE' | 'HISTORY' | string
}

const QUOTE_URL = '/quote'

export function listQuote(query: QuotePageQuery) {
  return http.get<PageRes<QuoteResp[]>>(QUOTE_URL, query)
}

export interface QuoteSummary {
  draftCount: number
  monthCount: number
  monthAmount: number
}

export function getQuoteSummary() {
  return http.get<QuoteSummary>(`${QUOTE_URL}/summary`)
}

export function getQuote(id: string) {
  return http.get<QuoteResp>(`${QUOTE_URL}/${id}`)
}

export function addQuote(data: Partial<QuoteResp> & Record<string, unknown>) {
  return http.post<string>(QUOTE_URL, data)
}

export function updateQuote(data: Partial<QuoteResp> & Record<string, unknown>) {
  return http.put(QUOTE_URL, data)
}

export function deleteQuote(id: string) {
  return http.del(`${QUOTE_URL}/${id}`)
}

export function exportQuote(query: QuotePageQuery) {
  return http.download(`${QUOTE_URL}/export`, query)
}

export function renderQuote(id: string) {
  return http.get<{ title: string, html: string }>(`${QUOTE_URL}/${id}/render`)
}

export function downloadQuoteDocx(id: string) {
  return http.download(`${QUOTE_URL}/${id}/export-docx`)
}

export function downloadQuotePdf(id: string) {
  return http.download(`${QUOTE_URL}/${id}/export-pdf`)
}

/** 报价导出文件名：项目名称-报价单号 */
export function buildQuoteExportFileName(
  projectName?: string | null,
  quoteNo?: string | null,
  ext: '.docx' | '.pdf' = '.docx',
) {
  const project = (projectName || '').trim() || '未命名项目'
  const no = (quoteNo || '').trim() || 'quote'
  const base = `${project}-${no}`.replace(/[\\/:*?"<>|]/g, '_')
  return `${base}${ext}`
}
