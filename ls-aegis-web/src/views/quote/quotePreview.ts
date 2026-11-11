import dayjs from 'dayjs'
import { DATE_FORMAT } from '@/constant/datetime'

/** 导出/预览材料行默认补足行数（不足时一页内可放下） */
const MATERIAL_ROW_MIN = 10
const SERVICE_ROW_MIN = 1

type PreviewLine = {
  lineType?: 'MATERIAL' | 'SERVICE'
  itemName?: string
  brandModel?: string
  description?: string
  unit?: string
  quantity?: number
  price?: number
  amount?: number
}

export type QuotePreviewInput = {
  projectName?: string
  quoteNo?: string
  quoteUnit?: string
  quoteDate?: unknown
  taxRate?: number
  terms?: string
  materialSubtotal: number
  serviceSubtotal: number
  preTaxTotal: number
  taxAmount: number
  totalWithTax: number
  amountUpper: string
  materialLines: PreviewLine[]
  serviceLines: PreviewLine[]
  logoUrl?: string
}

const esc = (raw?: string | null) => {
  if (!raw) return ''
  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

const escAttr = (raw?: string | null) => {
  if (!raw) return ''
  return raw.replace(/&/g, '&amp;').replace(/"/g, '&quot;')
}

export function resolveLogoUrl(logo?: string | null) {
  const raw = (logo || '/brand-logo.png').trim()
  if (!raw) return `${window.location.origin}/brand-logo.png`
  if (/^data:/i.test(raw)) return raw
  if (/^https?:\/\//i.test(raw)) return raw
  if (raw.startsWith('//')) return `${window.location.protocol}${raw}`
  if (raw.startsWith('/')) return `${window.location.origin}${raw}`
  return raw
}

const formatMoney = (val?: number | null, blankWhenEmpty = false) => {
  if (blankWhenEmpty && (val == null || Number.isNaN(Number(val)))) return ''
  if (blankWhenEmpty && Number(val) === 0) return ''
  return Number(val ?? 0).toFixed(2)
}

const formatQty = (val?: number | null) => {
  if (val == null || Number.isNaN(Number(val))) return ''
  return String(Number(val))
}

const formatQuoteDate = (quoteDate?: unknown) => {
  if (!quoteDate) return dayjs().format(DATE_FORMAT)
  return dayjs(quoteDate as string).format(DATE_FORMAT)
}

const cell = (content: string) => (content ? content : '&#160;')

/** 仅保留有名称的行，再补足到默认行数 */
const padRows = (rows: PreviewLine[], min: number, lineType: 'MATERIAL' | 'SERVICE') => {
  const filled = rows.filter((r) => String(r.itemName || '').trim())
  const next = [...filled]
  while (next.length < min) {
    next.push({ lineType })
  }
  return next
}

const defaultTermsHtml = `
<p>1. 本报价单自出具之日起 7 个自然日内有效，有效期届满本报价自动失效，如需沿用本报价，需经供需双方重新书面确认。</p>
<p>2. 报价为含税价，包含本市范围内运费，无其他额外费用。</p>
<p>3. 付款方式：合同签订后预付 30%，设备到货后支付 50%，验收合格后支付余款 20%。</p>
`.trim()

const summaryNote = (rate: string) =>
  `本报价为含税总价。增值税综合税率${rate}%(含增值税及附加等);开票增值税税率以实际票面为准(小规模纳税人常见为1%)，无需另行支付税款。`

const colgroup = `
<colgroup>
  <col class="col-idx"/><col class="col-name"/><col class="col-spec"/>
  <col class="col-unit"/><col class="col-qty"/><col class="col-price"/><col class="col-amount"/>
</colgroup>`

export function buildQuotePreviewHtml(input: QuotePreviewInput) {
  const materials = padRows(input.materialLines, MATERIAL_ROW_MIN, 'MATERIAL')
  const services = padRows(input.serviceLines, SERVICE_ROW_MIN, 'SERVICE')

  const quoteNo = esc(input.quoteNo || '')
  const quoteUnit = esc(input.quoteUnit || '')
  const quoteDate = esc(formatQuoteDate(input.quoteDate))
  const projectName = esc(String(input.projectName || '').trim())
  const docSubtitle = '设备采购及技术服务报价单'
  const logoSrc = escAttr(resolveLogoUrl(input.logoUrl))
  const logoHtml = logoSrc
    ? `<img src="${logoSrc}" alt="logo" width="80" height="80" style="width:80px;height:80px;object-fit:contain;"/>`
    : ''
  const rate = String(Number(input.taxRate ?? 4))
  const termsRaw = esc(input.terms || '').replace(/\n/g, '<br/>')
  const terms = termsRaw || defaultTermsHtml

  let html = `<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="zh-CN">
<head>
<meta charset="UTF-8"/>
<title>报价单</title>
<style>
  @page { size: A4; margin: 12mm; }
  html { background: #e8e8e8; }
  body {
    font-family: SimSun, "Microsoft YaHei", "Songti SC", serif;
    font-size: 12px;
    color: #000;
    margin: 0 auto;
    box-sizing: border-box;
    background: #fff;
  }
  @media screen {
    body {
      width: 210mm;
      min-height: 297mm;
      margin: 12px auto;
      padding: 12mm;
      box-shadow: 0 1px 6px rgba(0, 0, 0, 0.12);
    }
  }
  @media print {
    html { background: #fff; }
    body { width: auto; min-height: auto; margin: 0; padding: 0; box-shadow: none; }
  }
  .header { position: relative; margin-bottom: 8px; min-height: 72px; }
  .logo { position: absolute; left: 0; top: 0; width: 72px; }
  .logo img { display: block; width: 72px; height: 72px; object-fit: contain; }
  .title-wrap { text-align: center; padding: 0 72px; }
  .title-wrap h1 { margin: 0; font-size: 18px; font-weight: 700; line-height: 1.45; }
  .title-wrap h2 { margin: 4px 0 0; font-size: 14px; font-weight: 400; line-height: 1.45; }
  .meta { text-align: right; margin-top: 6px; font-size: 12px; line-height: 1.6; }
  .section { margin-top: 8px; font-weight: 700; font-size: 12px; }
  table.data-table { width: 100%; border-collapse: collapse; margin-top: 3px; table-layout: fixed; border-spacing: 0; }
  table.data-table th, table.data-table td {
    border: 1px solid #333;
    padding: 5px 4px;
    text-align: center !important;
    vertical-align: middle !important;
    word-break: break-word;
  }
  table.data-table th {
    background: #f5f5f5;
    font-weight: 700;
    padding: 7px 4px;
    white-space: nowrap;
    line-height: 1.35;
  }
  table.data-table td { height: 22px; line-height: 1.3; }
  table.data-table col.col-idx { width: 7%; }
  table.data-table col.col-name { width: 28%; }
  table.data-table col.col-spec { width: 18%; }
  table.data-table col.col-unit { width: 8%; }
  table.data-table col.col-qty { width: 11%; }
  table.data-table col.col-price { width: 14%; }
  table.data-table col.col-amount { width: 14%; }
  table.summary { width: 100%; border-collapse: collapse; margin-top: 3px; table-layout: fixed; border-spacing: 0; }
  table.summary td {
    height: 26px;
    border: 1px solid #333;
    padding: 4px 3px;
    text-align: center !important;
    vertical-align: middle !important;
  }
  .summary-note { margin-top: 3px; font-size: 11px; line-height: 1.5; }
  .terms { margin-top: 8px; line-height: 1.7; font-size: 11px; }
  .terms p { margin: 0 0 3px; }
  @media print { body { margin: 0; } }
</style>
</head>
<body>
<div class="header">
  <div class="logo">${logoHtml}</div>
  <div class="title-wrap">
    <h1>${projectName}</h1>
    <h2>${docSubtitle}</h2>
  </div>
</div>
<div class="meta">
  报价单位：${quoteUnit}<br/>
  报价单号：${quoteNo}<br/>
  报价时间：${quoteDate}
</div>`

  html += '<div class="section">一、材料部分</div>'
  html += `<table class="data-table">${colgroup}<thead><tr>`
  html += '<th>序号</th><th>材料名称/规格</th><th>品牌/型号</th>'
  html += '<th>单位</th><th>数量</th><th>单价(¥)</th><th>金额(¥)</th>'
  html += '</tr></thead><tbody>'
  materials.forEach((row, index) => {
    const blank = !String(row.itemName || '').trim()
    html += '<tr>'
    html += `<td>${index + 1}</td>`
    html += `<td>${cell(esc(row.itemName || ''))}</td>`
    html += `<td>${cell(esc(row.brandModel || ''))}</td>`
    html += `<td>${cell(esc(blank ? '' : (row.unit || '')))}</td>`
    html += `<td>${cell(blank ? '' : formatQty(row.quantity))}</td>`
    html += `<td>${cell(blank ? '' : formatMoney(row.price, true))}</td>`
    html += `<td>${cell(blank ? '' : formatMoney(row.amount, true))}</td>`
    html += '</tr>'
  })
  html += `<tr><td colspan="6">材料小计</td><td>${formatMoney(input.materialSubtotal)}</td></tr>`
  html += '</tbody></table>'

  html += '<div class="section">二、技术服务费部分</div>'
  html += `<table class="data-table">${colgroup}<thead><tr>`
  html += '<th>序号</th><th>服务项目</th><th>说明</th>'
  html += '<th>单位</th><th>工作量</th><th>单价(¥)</th><th>金额(¥)</th>'
  html += '</tr></thead><tbody>'
  services.forEach((row, index) => {
    const blank = !String(row.itemName || '').trim()
    html += '<tr>'
    html += `<td>${index + 1}</td>`
    html += `<td>${cell(esc(row.itemName || ''))}</td>`
    html += `<td>${cell(esc(row.description || ''))}</td>`
    html += `<td>${cell(esc(blank ? '' : (row.unit || '')))}</td>`
    html += `<td>${cell(blank ? '' : formatQty(row.quantity))}</td>`
    html += `<td>${cell(blank ? '' : formatMoney(row.price, true))}</td>`
    html += `<td>${cell(blank ? '' : formatMoney(row.amount, true))}</td>`
    html += '</tr>'
  })
  html += `<tr><td colspan="6">服务小计</td><td>${formatMoney(input.serviceSubtotal)}</td></tr>`
  html += '</tbody></table>'

  html += '<div class="section">三、费用合计</div>'
  html += '<table class="summary"><tbody>'
  html += `<tr><td>材料小计</td><td>${formatMoney(input.materialSubtotal)}</td>`
  html += `<td>税前合计</td><td>${formatMoney(input.preTaxTotal)}</td>`
  html += `<td>总价(含税)</td><td>${formatMoney(input.totalWithTax)}</td></tr>`
  html += `<tr><td>技术服务费小计</td><td>${formatMoney(input.serviceSubtotal)}</td>`
  html += `<td>增值税综合税率*${rate}%</td><td>${formatMoney(input.taxAmount)}</td>`
  html += `<td>人民币大写</td><td>${esc(input.amountUpper)}</td></tr>`
  html += '</tbody></table>'
  html += `<div class="summary-note">${summaryNote(rate)}</div>`

  html += '<div class="section">四、备注 / 条款</div>'
  html += `<div class="terms">${terms}</div>`
  html += '</body></html>'

  return html
}
