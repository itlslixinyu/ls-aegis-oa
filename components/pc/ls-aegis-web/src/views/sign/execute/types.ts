/** 签章任务类型（batch = 多页同位置批量普通章） */
export type SealTaskType = 'general' | 'batch' | 'span'

/** 批量页码策略 */
export type BatchPageMode = 'range' | 'all' | 'odd' | 'even'

/** 多页批量页码范围 */
export interface BatchPageRange {
  startPage: number
  endPage: number
  /** 默认连续范围；all/odd/even 在 start-end 区间内筛选 */
  mode?: BatchPageMode
}

/** 普通章坐标（PDF pt，原点在页面左上角） */
export interface SealPlacement {
  page: number
  x: number
  y: number
  width: number
  height: number
}

/** 骑缝章配置（与后端 SpanSealConfig 对齐） */
export interface SpanSealTaskConfig {
  startPage: number
  endPage: number
  /** 印章中心 Y 坐标（pt，自顶向下） */
  yPosition: number
  /** 印章尺寸（mm） */
  sealSize: number
}

export interface PendingSealTask {
  id: string
  sealId: string | number
  sealName: string
  type: SealTaskType
  placement?: SealPlacement
  /** 批量章页码范围；type=batch 时必填 */
  batchPages?: BatchPageRange
  spanConfig?: SpanSealTaskConfig
}

/** 解析批量章实际覆盖页码列表 */
export function resolveBatchPages(range: BatchPageRange, totalPages: number): number[] {
  const mode = range.mode || 'range'
  const start = mode === 'all' ? 1 : Math.max(1, range.startPage)
  const end = mode === 'all' ? totalPages : Math.min(totalPages, range.endPage)
  if (end < start) {
    return []
  }
  const pages: number[] = []
  for (let page = start; page <= end; page += 1) {
    if (mode === 'odd' && page % 2 === 0) {
      continue
    }
    if (mode === 'even' && page % 2 === 1) {
      continue
    }
    pages.push(page)
  }
  return pages
}

/** 批量章覆盖页数 */
export function countBatchPages(range?: BatchPageRange, totalPages = 0) {
  if (!range) {
    return 0
  }
  const total = totalPages || range.endPage
  return resolveBatchPages(range, total).length
}

export function batchModeLabel(mode?: BatchPageMode) {
  switch (mode) {
    case 'all': return '全部页'
    case 'odd': return '奇数页'
    case 'even': return '偶数页'
    default: return '连续范围'
  }
}

/** 将批量章展开为各页坐标（提交后端用） */
export function expandBatchPlacements(task: PendingSealTask, totalPages?: number): SealPlacement[] {
  if (!task.placement || !task.batchPages) {
    return []
  }
  const { x, y, width, height } = task.placement
  const total = totalPages || task.batchPages.endPage
  return resolveBatchPages(task.batchPages, total).map((page) => ({
    page,
    x,
    y,
    width,
    height,
  }))
}

export interface PdfPageMeta {
  page: number
  width: number
  height: number
}

export interface PdfDocumentMeta {
  totalPages: number
  pages: PdfPageMeta[]
}

export const DEFAULT_SEAL_SIZE_PT = 120
export const DEFAULT_SPAN_SEAL_SIZE_MM = 42
