/**
 * EOMS 带鉴权文件预览：axios 拉 blob → ObjectURL
 */
import request from './request'

export const EOMS_PREVIEW_PREFIX = '/eoms/file/preview/'

/** @type {Map<string, { url: string, refs: number, inflight?: Promise<string> }>} */
const objectUrlCache = new Map()

/** @type {HTMLElement | null} */
let activePreviewEl = null
let activePreviewCleanup = null
let activePreviewOwner = null

function extractFileId(urlOrId) {
  if (!urlOrId) return ''
  const s = String(urlOrId).trim()
  if (/^\d+$/.test(s)) return s
  const previewIdx = s.indexOf(EOMS_PREVIEW_PREFIX)
  if (previewIdx >= 0) {
    const id = s.substring(previewIdx + EOMS_PREVIEW_PREFIX.length).split(/[/?#]/)[0]
    return /^\d+$/.test(id) ? id : ''
  }
  return ''
}

/** 是否需要 axios 带 token 拉取（非 blob/http/本地 profile 路径） */
export function needsAuthFetch(urlOrId) {
  if (!urlOrId) return false
  const s = String(urlOrId).trim()
  if (!s || s.startsWith('blob:') || s.startsWith('data:')) return false
  if (/^https?:\/\//i.test(s)) return false
  if (s.startsWith('/profile/')) return false
  if (s.includes(EOMS_PREVIEW_PREFIX)) return true
  if (/^\d+$/.test(s)) return true
  if (s.includes('/api/file/') || s.includes('/file/eoms/')) return true
  return false
}

/** 规范为带鉴权的预览接口相对路径；优先走 /eoms/file/preview/{id} */
export function toPreviewPath(urlOrId) {
  const s = String(urlOrId || '').trim()
  if (!s) return s
  if (s.startsWith('blob:') || s.startsWith('data:') || /^https?:\/\//i.test(s)) return s
  const fileId = extractFileId(s)
  if (fileId) return `${EOMS_PREVIEW_PREFIX}${fileId}`
  if (s.includes(EOMS_PREVIEW_PREFIX)) {
    return s.substring(s.indexOf(EOMS_PREVIEW_PREFIX))
  }
  return s
}

function toCacheKey(urlOrId) {
  const path = toPreviewPath(urlOrId)
  return path.startsWith('/') ? path : `/${path}`
}

async function downloadPreviewBlob(path) {
  const res = await request({
    url: path.startsWith('/') ? path : `/${path}`,
    method: 'get',
    responseType: 'blob',
    skipErrorToast: true
  })
  let blob = res.data instanceof Blob ? res.data : new Blob([res.data])
  if (blob.type && blob.type.startsWith('application/json')) {
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
  // 无 MIME / octet-stream 时按魔数纠正，避免预览组件尺寸计算异常
  blob = await normalizeImageBlob(blob)
  return URL.createObjectURL(blob)
}

async function normalizeImageBlob(blob) {
  const known = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp'])
  if (known.has(blob.type)) {
    return blob
  }
  const buf = await blob.slice(0, 16).arrayBuffer()
  const u8 = new Uint8Array(buf)
  let mime = 'image/jpeg'
  if (u8.length >= 8 && u8[0] === 0x89 && u8[1] === 0x50 && u8[2] === 0x4e && u8[3] === 0x47) {
    mime = 'image/png'
  } else if (u8.length >= 3 && u8[0] === 0xff && u8[1] === 0xd8 && u8[2] === 0xff) {
    mime = 'image/jpeg'
  } else if (u8.length >= 6 && u8[0] === 0x47 && u8[1] === 0x49 && u8[2] === 0x46) {
    mime = 'image/gif'
  } else if (u8.length >= 12 && u8[8] === 0x57 && u8[9] === 0x45 && u8[10] === 0x42 && u8[11] === 0x50) {
    mime = 'image/webp'
  }
  return new Blob([blob], { type: mime })
}

/**
 * 拉取 EOMS 文件为 ObjectURL（引用计数，组件卸载或预览关闭时 release）
 */
export async function fetchEomsFileObjectUrl(urlOrId) {
  const path = toPreviewPath(urlOrId)
  if (!needsAuthFetch(path) && !extractFileId(urlOrId)) {
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

/**
 * 上传成功后绑定水印预览；预览失败不影响上传成功（保留本地缩略图）
 */
export async function bindUploaderWatermarkPreview(fileItem, fileIdOrUrl, isActive = () => true) {
  if (!fileItem || !fileIdOrUrl || !isActive()) return false
  try {
    if (needsAuthFetch(fileIdOrUrl) || extractFileId(fileIdOrUrl)) {
      const url = await fetchEomsFileObjectUrl(fileIdOrUrl)
      if (!isActive()) { releaseEomsFileObjectUrl(fileIdOrUrl); return false }
      fileItem.url = url
      return true
    }
    if (isActive()) fileItem.url = fileIdOrUrl
  } catch {
    // Keep the local thumbnail; the upload itself has already succeeded.
  }
  return false
}

/** 释放一次 ObjectURL 引用（refs 归零时 revoke） */
export function releaseEomsFileObjectUrl(urlOrId) {
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
export async function resolveEomsPhotoList(urls) {
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
export function releaseEomsPhotoList(urls) {
  if (!urls?.length) return
  urls.forEach((item) => releaseEomsFileObjectUrl(item))
}

/** @deprecated 请用 releaseEomsFileObjectUrl / releaseEomsPhotoList */
export function revokeObjectUrls(urls) {
  if (!urls?.length) return
  urls.forEach((u) => {
    if (u && String(u).startsWith('blob:')) {
      URL.revokeObjectURL(u)
    }
  })
}

function closeActivePreview() {
  const el = activePreviewEl
  const cleanup = activePreviewCleanup
  activePreviewEl = null; activePreviewCleanup = null; activePreviewOwner = null
  cleanup?.()
  if (!el) return
  el.classList.add('eoms-img-preview--closing')
  const onDone = () => {
    el.removeEventListener('transitionend', onDone)
    el.remove()
  }
  el.addEventListener('transitionend', onDone)
  setTimeout(onDone, 280)
}

/**
 * 自建全屏预览：绕开 Vant ImagePreview 竖图 width:auto;height:100% 在 WebView 下压成细白条的问题。
 */
function mountEomsImagePreview(images, startIndex, onClose, owner) {
  closeActivePreview()

  let index = startIndex
  const root = document.createElement('div')
  root.className = 'eoms-img-preview'
  root.setAttribute('role', 'dialog')
  root.setAttribute('aria-modal', 'true')

  root.innerHTML = `
    <div class="eoms-img-preview__backdrop"></div>
    <button type="button" class="eoms-img-preview__close" aria-label="关闭">×</button>
    <div class="eoms-img-preview__stage">
      <img class="eoms-img-preview__img" alt="预览图片" draggable="false" />
    </div>
    <div class="eoms-img-preview__index"></div>
  `

  const img = root.querySelector('.eoms-img-preview__img')
  const indexEl = root.querySelector('.eoms-img-preview__index')
  const closeBtn = root.querySelector('.eoms-img-preview__close')
  const stage = root.querySelector('.eoms-img-preview__stage')

  const render = () => {
    img.removeAttribute('src')
    img.src = images[index]
    indexEl.textContent = images.length > 1 ? `${index + 1} / ${images.length}` : ''
    indexEl.style.display = images.length > 1 ? 'block' : 'none'
  }

  const destroy = () => closeActivePreview()

  const onKey = (e) => {
    if (e.key === 'Escape') destroy()
    if (e.key === 'ArrowLeft' && images.length > 1) {
      index = (index - 1 + images.length) % images.length
      render()
    }
    if (e.key === 'ArrowRight' && images.length > 1) {
      index = (index + 1) % images.length
      render()
    }
  }

  let touchStartX = 0
  let touchStartY = 0
  stage.addEventListener('touchstart', (e) => {
    const t = e.changedTouches[0]
    touchStartX = t.clientX
    touchStartY = t.clientY
  }, { passive: true })
  stage.addEventListener('touchend', (e) => {
    if (images.length <= 1) return
    const t = e.changedTouches[0]
    const dx = t.clientX - touchStartX
    const dy = t.clientY - touchStartY
    if (Math.abs(dx) < 50 || Math.abs(dx) < Math.abs(dy)) return
    index = dx < 0
      ? (index + 1) % images.length
      : (index - 1 + images.length) % images.length
    render()
  }, { passive: true })

  closeBtn.addEventListener('click', (e) => {
    e.stopPropagation()
    destroy()
  })
  root.querySelector('.eoms-img-preview__backdrop').addEventListener('click', destroy)
  img.addEventListener('click', destroy)
  document.addEventListener('keydown', onKey)

  document.body.appendChild(root)
  activePreviewEl = root
  activePreviewOwner = owner
  activePreviewCleanup = () => { document.removeEventListener('keydown', onKey); onClose?.() }
  requestAnimationFrame(() => root.classList.add('eoms-img-preview--open'))
  render()
}

/**
 * 带鉴权大图预览（自建全屏层，避免 Vant 竖图细条）
 */
export async function openEomsImagePreview(urls, startIndex = 0, options = {}) {
  const list = Array.isArray(urls) ? urls : [urls]
  if (!list.length) return
  const resolved = await resolveEomsPhotoList(list)
  const images = resolved.filter(Boolean)
  if (options.isActive && !options.isActive()) { releaseEomsPhotoList(list); return }
  if (!images.length) return
  const selected = resolved[Math.min(Math.max(0, startIndex), resolved.length - 1)]
  const safeIndex = selected ? Math.max(0, images.indexOf(selected)) : 0
  mountEomsImagePreview(images, safeIndex, () => {
    // 关闭动画后再释放，避免白条/残影
    setTimeout(() => releaseEomsPhotoList(list), 500)
  }, options.owner)
}

export function closeEomsImagePreview(owner) {
  if (owner === undefined || owner === activePreviewOwner) closeActivePreview()
}
