/** 版本探测为辅助请求：合并并发、限制频率，失败不影响业务导航。 */
export function createVersionChecker(
  readTag: () => Promise<string | null>,
  onChange: () => void,
  intervalMs = 60_000,
) {
  let tag: string | null = null
  let lastAttempt = Number.NEGATIVE_INFINITY
  let pending: Promise<void> | undefined
  return () => {
    if (pending) return pending
    if (Date.now() - lastAttempt < intervalMs) return Promise.resolve()
    lastAttempt = Date.now()
    pending = readTag().then((nextTag) => {
      if (nextTag && tag && nextTag !== tag) onChange()
      if (nextTag) tag = nextTag
    }).catch(() => {
      // 断网或部署层不支持 HEAD 时不打断路由，下一周期重试。
    }).finally(() => { pending = undefined })
    return pending
  }
}
