import type { TableColumnData } from '@arco-design/web-vue'
import { TableCol } from '@/constant/table-col'

/** 名称列默认宽度 */
export const DEFAULT_NAME_WIDTH = TableCol.name

/** 操作列兜底宽度 */
export const DEFAULT_ACTION_WIDTH = TableCol.actionS

type Col = TableColumnData & {
  minWidth?: number
  show?: boolean
  bodyCellStyle?: Record<string, string | number>
}

/**
 * 归一化列：全部落到明确 width（px）
 */
export function normalizeTableColumns(columns: Col[] = []): Col[] {
  return columns.map((col) => {
    const next: Col = { ...col }

    if (next.align == null) {
      next.align = 'center'
    }

    // 操作列
    if (next.dataIndex === 'action') {
      next.fixed = next.fixed ?? 'right'
      next.ellipsis = false
      if (next.width == null) {
        next.width = DEFAULT_ACTION_WIDTH
      }
      next.bodyCellStyle = {
        ...next.bodyCellStyle,
        whiteSpace: 'nowrap',
      }
      return next
    }

    // 已有 width：短列 / 显式名称宽
    if (next.width != null) {
      if (next.ellipsis == null) {
        next.ellipsis = true
      }
      if (next.tooltip == null && next.ellipsis) {
        next.tooltip = true
      }
      return next
    }

    // 编号：minWidth + ellipsis:false → 锁 width + nowrap
    if (next.ellipsis === false) {
      const locked = next.minWidth ?? TableCol.codeMinSm
      next.width = locked
      delete next.minWidth
      next.bodyCellStyle = {
        ...next.bodyCellStyle,
        whiteSpace: 'nowrap',
      }
      return next
    }

    // 名称等：默认锁 240
    next.width = next.minWidth ?? DEFAULT_NAME_WIDTH
    delete next.minWidth
    next.ellipsis = true
    next.tooltip = true
    return next
  })
}

/** 可见列 width 求和（忽略 show:false） */
export function sumTableColumnsWidth(columns: Col[] = []): number {
  return columns.reduce((sum, col) => {
    if (col.show === false) {
      return sum
    }
    return sum + (Number(col.width) || 0)
  }, 0)
}

type Scroll = { x?: number | string; y?: number | string; minWidth?: number }

/**
 * 合并滚动：默认 scroll.x = Σ width，禁止 '100%' 均分
 */
export function mergeTableScroll(scroll: Scroll | undefined, columns: Col[]): Scroll {
  const sum = sumTableColumnsWidth(columns)
  const next: Scroll = {
    y: '100%',
    ...scroll,
  }
  if (next.x == null || next.x === '100%') {
    next.x = sum || undefined
  }
  delete next.minWidth
  return next
}
