import { assertSafeDownloadUrl } from '@/utils/safeUrl'

/**
 * 根据文件url获取文件名
 * @param url 文件url
 */
function getFileName(url: string) {
  const pathname = new URL(url).pathname
  const fileName = pathname.slice(pathname.lastIndexOf('/') + 1)
  try {
    return decodeURIComponent(fileName)
  } catch {
    return fileName
  }
}

function isPlatformFailure(body: unknown): body is { success: false, code: string | number, msg?: unknown, message?: unknown } {
  if (!body || typeof body !== 'object') return false
  const value = body as Record<string, unknown>
  return value.success === false
    && (typeof value.code === 'string' || typeof value.code === 'number')
}

/**
 * 根据文件地址下载文件
 * @param {*} sUrl
 */
export async function downloadByUrl({
  url,
  target = '_blank',
  fileName,
}: {
  url: string
  target?: '_self' | '_blank'
  fileName?: string
}): Promise<boolean> {
  const parsed = assertSafeDownloadUrl(url)
  const safeUrl = parsed.href
  // 同源也先检查响应，避免把 401/403/404 或平台 R 错误保存成“文件”。
  // 外部 URL 不附加平台 Token；浏览器仅发送同源 cookie。
  const response = await fetch(safeUrl, { credentials: 'same-origin' })
  const blob = await response.blob()
  let body: unknown
  const contentType = response.headers.get('content-type') || blob.type
  if (/\b(?:application|text)\/(?:[\w.-]+\+)?json\b/i.test(contentType)) {
    try {
      body = JSON.parse(await blob.text())
    } catch {
      // 合法业务文件可能只是使用 JSON MIME；不以解析失败认定下载接口失败。
    }
  }
  if (!response.ok || isPlatformFailure(body)) {
    const platformMessage = isPlatformFailure(body)
      ? typeof body.msg === 'string' ? body.msg : typeof body.message === 'string' ? body.message : undefined
      : undefined
    const code = isPlatformFailure(body) ? String(body.code) : String(response.status)
    const fallback = code === '401' ? '登录状态已失效，请重新登录后下载'
      : code === '403' ? '没有下载该文件的权限'
        : response.ok ? '文件下载失败' : `下载失败（HTTP ${response.status}）`
    throw new Error(platformMessage || fallback)
  }

  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  try {
    link.href = objectUrl
    link.download = fileName || getFileName(safeUrl)
    link.target = target
    link.rel = 'noopener noreferrer'
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    return true
  } finally {
    link.remove()
    URL.revokeObjectURL(objectUrl)
  }
}
