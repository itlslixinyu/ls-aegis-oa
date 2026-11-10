/**
 * 业务主键（雪花 Long）前端约定：一律字符串。
 * 后端 Jackson 已 ToStringSerializer；禁止 Number()/parseInt，否则超安全整数丢精度，
 * a-select / GiForm 的 value 对不上 options，界面会显示裸 ID。
 */

export type BizId = string

/** 空值 → undefined；其余 → 去空白字符串 */
export function toBizId(v: unknown): BizId | undefined {
  if (v == null || v === '') return undefined
  const s = String(v).trim()
  return s || undefined
}

/** 列表/多选：统一成字符串数组（支持逗号分隔） */
export function toBizIdList(v: unknown): BizId[] {
  if (v == null || v === '') return []
  if (Array.isArray(v)) {
    return v.map((x) => String(x).trim()).filter(Boolean)
  }
  return String(v).split(',').map((x) => x.trim()).filter(Boolean)
}

/** 下拉选项：label + value(string) */
export function toSelectOpts(
  list: Array<{ label?: string, value?: unknown, id?: unknown }> | null | undefined,
  opts?: { labelKey?: string, valueKey?: string },
): Array<{ label: string, value: BizId }> {
  const labelKey = opts?.labelKey || 'label'
  const valueKey = opts?.valueKey || 'value'
  return (list || []).map((i: any) => {
    const raw = i?.[valueKey] ?? i?.id
    return {
      label: String(i?.[labelKey] ?? i?.label ?? raw ?? ''),
      value: String(raw),
    }
  }).filter((o) => o.value !== '' && o.value !== 'undefined' && o.value !== 'null')
}

/**
 * 用户类下拉：nickname / nickName / username / id
 * （部分业务 Map 查询返回 nickName，如 EOMS 工程师选项）
 */
export function toUserSelectOpts(
  list: Array<{ nickname?: string, nickName?: string, username?: string, id?: unknown }> | null | undefined,
): Array<{ label: string, value: BizId }> {
  return (list || []).map((u) => ({
    label: u.nickname || u.nickName || u.username || String(u.id ?? ''),
    value: String(u.id ?? ''),
  })).filter((o) => o.value !== '' && o.value !== 'undefined' && o.value !== 'null')
}

/**
 * 字典/LabelValue 下拉：统一 value 为字符串（套餐、往来单位等）
 */
export function toLabelValueOpts(
  list: Array<{ label?: string, value?: unknown }> | null | undefined,
): Array<{ label: string, value: BizId }> {
  return toSelectOpts(list, { labelKey: 'label', valueKey: 'value' })
}

/**
 * 缺选项时用名称兜底，避免 Select 只显示裸 ID
 */
export function ensureSelectOpt(
  options: Array<{ label: string, value: BizId }>,
  id: unknown,
  label?: string,
): void {
  const value = toBizId(id)
  if (!value) return
  if (options.some((o) => o.value === value)) return
  options.push({ label: label || value, value })
}

/** 递归把树节点 key/value/id 规范成字符串（tree-select） */
export function normalizeTreeBizIds<T extends Record<string, any>>(
  nodes: T[] | null | undefined,
  fields: string[] = ['key', 'value', 'id'],
): T[] {
  return (nodes || []).map((n) => {
    const next: any = { ...n }
    for (const f of fields) {
      if (next[f] != null && next[f] !== '') {
        next[f] = String(next[f])
      }
    }
    if (Array.isArray(next.children) && next.children.length) {
      next.children = normalizeTreeBizIds(next.children, fields)
    }
    return next as T
  })
}
