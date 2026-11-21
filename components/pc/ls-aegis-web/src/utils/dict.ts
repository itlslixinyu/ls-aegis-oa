import { reactive, toRefs } from 'vue'
import { listCommonDict } from '@/apis/system'

export type DictOption = { label: string, value: string | number, extra?: string }

/** 字典项转下拉选项 */
export function dictSelectOptions(items?: { label?: string, value?: any }[] | null) {
  return (items || []).map((d) => ({ label: d.label ?? String(d.value ?? ''), value: d.value }))
}

/** 按值取字典标签 */
export function findDictLabel(items: { label?: string, value?: any }[] | undefined, value: unknown, fallback = '-') {
  const hit = (items || []).find((d) => String(d.value) === String(value))
  if (hit?.label) {
    return hit.label
  }
  return value == null || value === '' ? fallback : String(value)
}

/** 按值取字典 extra（颜色语义：primary/success/warning/error/default） */
export function findDictExtra(items: { extra?: string, value?: any }[] | undefined, value: unknown) {
  return (items || []).find((d) => String(d.value) === String(value))?.extra
}

const EXTRA_TO_TAG: Record<string, string> = {
  primary: 'arcoblue',
  success: 'green',
  warning: 'orangered',
  error: 'red',
  default: 'gray',
}

export function findDictTagColor(items: { extra?: string, value?: any }[] | undefined, value: unknown, fallback = 'gray') {
  const extra = findDictExtra(items, value)
  return (extra && EXTRA_TO_TAG[extra]) || fallback
}

/**
 * 若依风格字典：返回 { [code]: Ref<{label,value}[]> }
 * 优先拉取平台公共字典；失败时返回空数组，避免页面崩溃。
 */
export function useDict(...codes: string[]) {
  const state = reactive<Record<string, DictOption[]>>({})

  codes.forEach((code) => {
    state[code] = []
    listCommonDict(code)
      .then((res: any) => {
        const list = res?.data ?? res ?? []
        state[code] = (Array.isArray(list) ? list : []).map((item: any) => ({
          label: item.label ?? item.dictLabel ?? String(item.value ?? ''),
          value: item.value ?? item.dictValue ?? '',
          extra: item.extra,
        }))
      })
      .catch(() => {
        state[code] = []
      })
  })

  return toRefs(state)
}
