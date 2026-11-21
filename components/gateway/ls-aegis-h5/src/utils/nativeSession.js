// Only the new app capability marker enables this bridge; older apps and browsers use existing storage.
function bridge(role, action, session) {
  if (typeof navigator === 'undefined' || !/LSAegisRoutes\/1(?:\s|$)/.test(navigator.userAgent)) return null
  try { return JSON.parse(window.prompt('LS_AEGIS_SESSION_V1', JSON.stringify({role, action, session}))) } catch { return {available:false} }
}
const keys = role => role === 'engineer' ? ['engineer_token','engineer_info'] : ['h5_token','customer_info']
export function hydrateNativeSession(role) {
  const response = bridge(role, 'get')
  if (!response) return
  const [tokenKey, infoKey] = keys(role)
  if (!response.available) { localStorage.removeItem(tokenKey);localStorage.removeItem(infoKey);return }
  if (!response.found) {
    const token=localStorage.getItem(tokenKey)
    if(token) { let info=null;try{info=JSON.parse(localStorage.getItem(infoKey))}catch{};persistNativeSession(role,token,info) }
    return
  }
  const session=response.session || {}
  if (typeof session.token === 'string' && session.token) {
    localStorage.setItem(tokenKey,session.token);localStorage.setItem(infoKey,JSON.stringify(session.info ?? null))
  } else { localStorage.removeItem(tokenKey);localStorage.removeItem(infoKey) }
}
export function persistNativeSession(role, token, info) {
  return bridge(role,'set',{token:token || '',info:info ?? null})?.available !== false
}
