import { Message } from '@arco-design/web-vue'
import { isExternal } from '@/utils/validate'

/**
 * 校验可打开的外链：仅允许 http(s)，拒绝 javascript: / data: 等
 */
export function isSafeHttpUrl(url: string): boolean {
  if (!url || typeof url !== 'string') {
    return false
  }
  try {
    const parsed = new URL(url, window.location.origin)
    return parsed.protocol === 'http:' || parsed.protocol === 'https:'
  } catch {
    return false
  }
}

/** 安全打开外链（noopener + noreferrer） */
export function openSafeExternal(url: string): boolean {
  if (!isExternal(url) || !isSafeHttpUrl(url)) {
    Message.warning('非法或不受支持的外链地址')
    return false
  }
  window.open(url, '_blank', 'noopener,noreferrer')
  return true
}

/**
 * 下载/预览用 URL：同源相对路径，或绝对 http(s)
 */
export function assertSafeDownloadUrl(url: string): URL {
  const parsed = new URL(url, window.location.origin)
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    throw new Error('非法下载地址')
  }
  return parsed
}
