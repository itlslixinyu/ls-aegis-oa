/**
 * 业务主键（雪花 Long）H5 约定：一律字符串。
 * 后端 Jackson 已 ToStringSerializer；禁止 Number()/parseInt，否则超安全整数丢精度。
 */

/** 空值 → undefined；其余 → 去空白字符串 */
export function toBizId(v) {
  if (v == null || v === '') return undefined
  const s = String(v).trim()
  return s || undefined
}

/** 列表/多选：统一成字符串数组（支持逗号分隔） */
export function toBizIdList(v) {
  if (v == null || v === '') return []
  if (Array.isArray(v)) {
    return v.map(toBizId).filter((id) => id !== undefined)
  }
  return String(v).split(',').map((s) => s.trim()).filter(Boolean)
}
