/** Reset invalidates stale pages when a user changes the task category. */
export function createTodoPager(fetchPage, size = 10) {
  let page = 1
  let generation = 0
  let pending = null
  return {
    reset() { generation++; page = 1; pending = null },
    load(status) {
      if (pending) return pending
      const current = generation
      const currentPage = page
      const request = Promise.resolve().then(() => fetchPage({ page: currentPage, size, status }))
        .then(response => {
          if (current !== generation) return null
          const rows = response?.data?.list
          if (!Array.isArray(rows)) throw new Error('待办列表响应格式不正确')
          const finished = rows.length < size
          if (!finished) page++
          return { rows, finished }
        }).finally(() => { if (current === generation) pending = null })
      pending = request
      return request
    }
  }
}
