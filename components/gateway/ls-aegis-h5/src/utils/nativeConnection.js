// Native routing never retries an API mutation or changes origins while a form is active.
const supported = () => typeof navigator !== 'undefined' && /LSAegisRoutes\/1(?:\s|$)/.test(navigator.userAgent)
const requests = new WeakSet()
let pending = 0
let switching = false
let timer
const idlePaths = new Set(['/customer/home','/customer/workbench','/customer/repairs/all','/customer/repairs','/customer/todos','/customer/profile','/engineer/home','/engineer/workbench','/engineer/tasks','/engineer/todos','/engineer/pool','/engineer/profile'])
export const hasNativeConnection = supported
export function canSwitchConnection() {
  if (!supported() || switching || pending !== 0) return false
  const path = window.location.pathname
  const idle = idlePaths.has(path) || /^\/(customer\/repairs|engineer\/tasks)\/status\/[A-Z_]+$/.test(path)
  const dialog = [...document.querySelectorAll('[role=\"dialog\"],.van-popup:not(.van-toast)')].some(node => node.getClientRects().length > 0)
  return idle && !dialog
}
function bridge(payload) {
  if (!supported()) return 'unavailable'
  try { return window.prompt('LS_AEGIS_NETWORK_V1', JSON.stringify(payload)) || 'unavailable' } catch { return 'unavailable' }
}
export function connectionStatus() {
  try { return JSON.parse(bridge({action:'status'})).mode === 'lan' ? '局域网' : '远程隧道' } catch { return '未连接' }
}
export function reconnectConnection(interactive = true) {
  if (!canSwitchConnection()) return 'busy'
  return bridge({action:'reconnect', pending, interactive})
}
export function beginConnectionRequest(config) {
  if (switching) { const error=new Error('正在切换连接，请稍候');error.config=config;throw error }
  if (!requests.has(config)) { requests.add(config); pending++ }
}
export function endConnectionRequest(config) {
  if (config && requests.delete(config)) pending = Math.max(0, pending - 1)
}
export function handleConnectionFailure(error) {
  if (error.response || (error.config?.method || '').toLowerCase() !== 'get') return
  scheduleDetection()
}
function scheduleDetection() {
  if (!supported()) return
  clearTimeout(timer)
  timer = setTimeout(() => { if (canSwitchConnection()) reconnectConnection(false) }, 800)
}
if (typeof window !== 'undefined' && supported()) {
  // A second check at probe completion closes the race with navigation/new requests.
  window.__LS_AEGIS_PREPARE_SWITCH__ = () => {
    if (!canSwitchConnection()) return false
    switching = true
    document.body.inert = true
    return true
  }
  window.addEventListener('ls-aegis-network-change', scheduleDetection)
}
