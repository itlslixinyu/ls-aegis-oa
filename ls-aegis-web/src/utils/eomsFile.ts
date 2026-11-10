/**

 * EOMS 带鉴权文件预览：axios 拉 blob → ObjectURL（PC Bearer）

 */

import http from '@/utils/http'



export const EOMS_PREVIEW_PREFIX = '/eoms/file/preview/'



interface CacheEntry {

  url: string

  refs: number

  inflight?: Promise<string>

}



const objectUrlCache = new Map<string, CacheEntry>()



/** 是否需要 axios 带 token 拉取（非 blob/http/本地 profile 路径） */

export function needsAuthFetch(urlOrId?: string | number | null): boolean {

  if (urlOrId == null || urlOrId === '') return false

  const s = String(urlOrId).trim()

  if (!s || s.startsWith('blob:') || s.startsWith('data:')) return false

  if (/^https?:\/\//i.test(s)) return false

  if (s.startsWith('/profile/')) return false

  if (s.includes(EOMS_PREVIEW_PREFIX)) return true

  if (/^\d+$/.test(s)) return true

  if (s.includes('/api/file/') || s.includes('/file/eoms/')) return true

  return false

}



/** 规范为预览接口相对路径 */

export function toPreviewPath(urlOrId?: string | number | null): string {

  const s = String(urlOrId ?? '').trim()

  if (!s) return s

  if (s.startsWith('blob:') || s.startsWith('data:') || /^https?:\/\//i.test(s)) return s

  if (s.includes(EOMS_PREVIEW_PREFIX)) {

    const idx = s.indexOf(EOMS_PREVIEW_PREFIX)

    return s.substring(idx)

  }

  if (/^\d+$/.test(s)) return `${EOMS_PREVIEW_PREFIX}${s}`

  if (s.includes('/api/file/') || s.includes('/file/eoms/')) return s

  return s

}



function toCacheKey(urlOrId?: string | number | null): string {

  const path = toPreviewPath(urlOrId)

  return path.startsWith('/') ? path : `/${path}`

}



async function downloadPreviewBlob(path: string): Promise<string> {

  const res = await http.download(path.startsWith('/') ? path : `/${path}`)

  const blob = res.data instanceof Blob ? res.data : new Blob([res.data])

  if (blob.type.startsWith('application/json')) {

    const text = await blob.text()

    let msg = '文件加载失败'

    try {

      const json = JSON.parse(text)

      msg = json.msg || msg

    } catch {

      // ignore parse error

    }

    throw new Error(msg)

  }

  return URL.createObjectURL(blob)

}



/**

 * 拉取 EOMS 文件为 ObjectURL（引用计数，组件卸载时 release）

 */

export async function fetchEomsFileObjectUrl(urlOrId?: string | number | null): Promise<string> {

  const path = toPreviewPath(urlOrId)

  if (!needsAuthFetch(path)) {

    return path

  }

  const key = toCacheKey(urlOrId)

  const cached = objectUrlCache.get(key)

  if (cached?.url) {

    cached.refs++

    return cached.url

  }

  if (cached?.inflight) {

    const url = await cached.inflight

    const hit = objectUrlCache.get(key)

    if (hit?.url) {

      hit.refs++

      return hit.url

    }

    return url

  }

  const inflight = downloadPreviewBlob(key).then((url) => {

    objectUrlCache.set(key, { url, refs: 1 })

    return url

  }).catch((err) => {

    objectUrlCache.delete(key)

    throw err

  })

  objectUrlCache.set(key, { url: '', refs: 0, inflight })

  return inflight

}



/** 释放一次 ObjectURL 引用 */

export function releaseEomsFileObjectUrl(urlOrId?: string | number | null): void {

  if (!urlOrId || !needsAuthFetch(urlOrId)) return

  const key = toCacheKey(urlOrId)

  const cached = objectUrlCache.get(key)

  if (!cached?.url) return

  cached.refs--

  if (cached.refs <= 0) {

    URL.revokeObjectURL(cached.url)

    objectUrlCache.delete(key)

  }

}



/** 批量解析照片列表为可展示的 ObjectURL */

export async function resolveEomsPhotoList(urls?: Array<string | number | null>): Promise<string[]> {

  if (!urls?.length) return []

  return Promise.all(urls.map(async (item) => {

    try {

      return await fetchEomsFileObjectUrl(item)

    } catch {

      return ''

    }

  }))

}



/** 释放一组预览源路径的引用 */

export function releaseEomsPhotoList(urls?: Array<string | number | null>): void {

  if (!urls?.length) return

  urls.forEach((item) => releaseEomsFileObjectUrl(item))

}


