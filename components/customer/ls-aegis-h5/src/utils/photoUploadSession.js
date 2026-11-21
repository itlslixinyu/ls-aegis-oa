import { validateUploadImage } from './uploadImage.js'

/** One page owns its upload queue and preview references; business writes remain separate. */
export function createPhotoUploadSession({ getItems, upload, prepare = async () => {}, preview = async () => false, release = () => {}, concurrency = 2 }) {
  const records = new Map()
  const queue = []
  let active = 0
  let disposed = false
  const present = (record) => !disposed && !record.cancelled && getItems().includes(record.item)
  const releaseRecord = (record) => {
    if (record.previewHeld) { record.previewHeld = false; release(record.item.fileId) }
  }
  async function run(record) {
    const item = record.item
    try {
      const file = await validateUploadImage(item.file)
      if (!present(record)) return
      item.message = '正在获取定位'
      await prepare()
      if (!present(record)) return
      item.message = '上传中'
      const response = await upload(file, record.context, {
        signal: record.controller.signal,
        onUploadProgress(event) {
          if (!present(record)) return
          const percent = event.total > 0 ? Math.min(99, Math.floor(event.loaded / event.total * 100)) : 0
          item.progress = percent
          item.message = percent >= 99 ? '正在生成水印' : percent ? `上传中 ${percent}%` : '上传中'
        },
      })
      if (!present(record)) return
      const id = response?.data?.fileId
      if ((typeof id === 'number' && !Number.isSafeInteger(id)) || !/^[1-9][0-9]{0,18}$/.test(String(id ?? ''))) {
        throw new Error('服务器未返回有效照片ID，请重试')
      }
      item.fileId = String(id)
      item.status = 'done'; item.message = ''; item.progress = 100
      // A preview failure cannot turn a successful server upload into an upload failure.
      try { record.previewHeld = Boolean(await preview(item, item.fileId, () => present(record))) } catch { }
      if (!present(record)) releaseRecord(record)
    } catch (error) {
      if (present(record)) {
        item.status = 'failed'; item.message = error?.msg || error?.message || '上传失败，请重试'
      }
    } finally {
      active--; record.settled = true; record.resolve(); pump()
    }
  }
  function pump() {
    while (!disposed && active < concurrency && queue.length) {
      const record = queue.shift()
      if (!present(record)) { record.settled = true; record.resolve(); continue }
      active++; void run(record)
    }
  }
  function enqueue(item, context) {
    if (disposed || !getItems().includes(item)) return Promise.resolve()
    const previous = records.get(item)
    if (previous && !previous.settled) return previous.promise
    if (item.status === 'done' && item.fileId) return Promise.resolve()
    const record = { item, context: context ?? previous?.context, controller: new AbortController(), previewHeld: false, cancelled: false, settled: false }
    record.promise = new Promise(resolve => { record.resolve = resolve })
    records.set(item, record)
    item.status = 'uploading'; item.message = '等待上传'; item.progress = 0
    queue.push(record); pump()
    return record.promise
  }
  function remove(item) {
    const record = records.get(item)
    if (!record) return
    record.cancelled = true; record.controller.abort(); releaseRecord(record)
  }
  function dispose() {
    disposed = true
    records.forEach(record => { record.cancelled = true; record.controller.abort(); releaseRecord(record) })
    queue.splice(0).forEach(record => { record.settled = true; record.resolve() })
  }
  return { enqueue, retry: item => enqueue(item), remove, dispose, isActive: () => !disposed }
}

export function photoSubmissionError(items, minimum = 0) {
  if (items.some(item => item.status === 'failed')) return '有照片上传失败，请重试或删除后再提交'
  if (items.some(item => item.status !== 'done' || !item.fileId)) return '照片正在处理，请等待上传完成'
  if (items.length < minimum) return `请至少上传 ${minimum} 张故障现场照片`
  return ''
}
