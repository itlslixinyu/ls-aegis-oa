/**
 * 列表表格列宽档位（业务只选角色，不抠像素）
 * @see .cursor/rules/前端表格规范.mdc
 */
export const TableCol = {
  /** 短编码 / 用户名等 */
  codeMinSm: 152,
  /** 长单号 / 连续编号 */
  codeMin: 184,
  /** UUID / 超长编码 */
  codeMinLg: 220,
  /** 类型、标签 */
  type: 100,
  typeSm: 88,
  /** 状态 */
  status: 96,
  /** 日期时间 */
  date: 178,
  /** 仅日期 */
  dateSm: 120,
  /** 数量 */
  qty: 88,
  /** 单位 */
  unit: 72,
  /** 规格 */
  spec: 120,
  /** 名称默认 */
  name: 240,
  /** 名称加宽 */
  nameLg: 320,
  /** 操作：仅「⋯」更多 */
  actionIcon: 80,
  /** 操作：约 1 个文字链 */
  actionS: 140,
  /** 操作：详情+修改+更多（推荐） */
  actionM: 220,
  /** 操作：3～4 个文字链 */
  actionL: 280,
  /** 操作：更多文字链 */
  actionXl: 360,
  /** 序号 */
  index: 66,
} as const

export type TableColKey = keyof typeof TableCol
