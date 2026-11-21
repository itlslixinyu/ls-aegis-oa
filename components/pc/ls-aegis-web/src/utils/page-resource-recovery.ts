import { Modal } from '@arco-design/web-vue'

export function isPageResourceError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String((error as any)?.message || error || '')
  return /Unable to preload CSS|Failed to fetch dynamically imported module|Importing a module script failed|Loading (?:CSS )?chunk|Cannot find module|error loading dynamically imported module/i.test(message)
}

let showing = false
let destination: string | undefined

/** 不自动刷新编辑中的表单；错误时保留可见恢复入口，由用户选择重新打开目标页。 */
export function showPageResourceRecovery(error: unknown, target?: string) {
  if (!isPageResourceError(error)) return
  if (target?.startsWith('/') && !target.startsWith('//') && !target.includes('\\')) destination = target
  if (showing) return
  showing = true
  Modal.warning({
    title: '页面资源加载失败',
    content: '页面资源可能已更新，或网络暂时中断。请确认当前编辑内容已保存，再刷新重新打开页面。',
    hideCancel: false,
    okText: '刷新并重新打开',
    cancelText: '留在当前页',
    maskClosable: false,
    onOk: () => {
      const next = destination || `${window.location.pathname}${window.location.search}${window.location.hash}`
      showing = false
      destination = undefined
      window.history.replaceState(window.history.state, '', next)
      window.location.reload()
    },
    onCancel: () => { showing = false; destination = undefined },
  })
}

export function setupPageResourceRecovery() {
  const preload = (event: Event) => showPageResourceRecovery((event as any).payload)
  const rejection = (event: PromiseRejectionEvent) => showPageResourceRecovery(event.reason)
  window.addEventListener('vite:preloadError', preload)
  window.addEventListener('unhandledrejection', rejection)
  return () => {
    window.removeEventListener('vite:preloadError', preload)
    window.removeEventListener('unhandledrejection', rejection)
  }
}
