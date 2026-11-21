import { isEqual } from 'lodash-es'

type KeyOf<T> = keyof T | ((row: T, index: number) => string | number)

function resolveKey<T extends Record<string, unknown>>(
  row: T,
  index: number,
  key: KeyOf<T>,
): string {
  const raw = typeof key === 'function' ? key(row, index) : row[key]
  if (raw == null || raw === '') {
    return `__idx_${index}`
  }
  return String(raw)
}

/**
 * 按业务主键对列表做 in-place diff 局部更新。
 * - 无实质变化返回 false（调用方勿触发视图重渲染）
 * - 字段变更时原地改对象，复用同一引用，减少表格行 DOM 重挂载
 * - 增删/重排时用 splice 写回，避免整体替换新数组引用
 */
export function patchListByKey<T extends Record<string, unknown>>(
  target: T[],
  next: T[],
  key: KeyOf<T> = 'id' as keyof T,
): boolean {
  if (target.length === next.length) {
    let identical = true
    for (let i = 0; i < next.length; i += 1) {
      if (
        resolveKey(target[i], i, key) !== resolveKey(next[i], i, key)
        || !isEqual(target[i], next[i])
      ) {
        identical = false
        break
      }
    }
    if (identical) {
      return false
    }
  }

  const oldByKey = new Map<string, T>()
  target.forEach((row, i) => {
    oldByKey.set(resolveKey(row, i, key), row)
  })

  const result: T[] = []
  let changed = target.length !== next.length

  next.forEach((n, i) => {
    const k = resolveKey(n, i, key)
    const old = oldByKey.get(k)
    if (old) {
      if (!isEqual(old, n)) {
        const oldKeys = Object.keys(old)
        const newKeys = Object.keys(n)
        for (const nk of newKeys) {
          if (!isEqual(old[nk], n[nk])) {
            ;(old as Record<string, unknown>)[nk] = n[nk]
            changed = true
          }
        }
        for (const ok of oldKeys) {
          if (!(ok in n)) {
            delete (old as Record<string, unknown>)[ok]
            changed = true
          }
        }
      }
      result.push(old)
    } else {
      result.push(n)
      changed = true
    }
  })

  const orderChanged =
    target.length !== result.length
    || target.some((row, i) => result[i] !== row)

  if (orderChanged) {
    target.splice(0, target.length, ...result)
    return true
  }
  return changed
}

/** 标量 ref：仅在值变化时写入，避免无意义触发依赖 */
export function assignIfChanged<T>(target: { value: T }, next: T): boolean {
  if (isEqual(target.value, next)) {
    return false
  }
  target.value = next
  return true
}
