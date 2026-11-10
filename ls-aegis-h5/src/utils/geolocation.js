/** Bound permission/provider stalls as well as the browser's acquisition timeout. */
export function getCurrentPosition(onSuccess, onError, options = {}) {
  let settled = false
  const finish = (callback, value) => {
    if (settled) return
    settled = true
    clearTimeout(timer)
    callback(value)
  }
  const timer = setTimeout(() => finish(onError, {
    code: 3, message: '定位超时，请检查位置权限后重试'
  }), options.timeout || 10000)
  try {
    navigator.geolocation.getCurrentPosition(
      position => finish(onSuccess, position),
      error => finish(onError, error),
      options
    )
  } catch (error) {
    finish(onError, error)
  }
}
