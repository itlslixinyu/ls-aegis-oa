let sequence = 0

/** Native system geocoder is optional; plain browsers never display a prompt dialog. */
export function getNativeAddress(position) {
  const latitude = Number(position?.coords?.latitude)
  const longitude = Number(position?.coords?.longitude)
  if (!window.__LS_AEGIS_NATIVE_GEOCODER__ || !Number.isFinite(latitude) || !Number.isFinite(longitude)
    || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) return Promise.resolve('')
  return new Promise(resolve => {
    const id = `${Date.now().toString(36)}-${++sequence}`
    let settled = false
    const finish = address => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      window.removeEventListener('ls-aegis-address-result', onResult)
      window.removeEventListener('pagehide', onPageHide)
      resolve(address)
    }
    const onResult = event => {
      if (event.detail?.id !== id) return
      const address = event.detail.address
      finish(typeof address === 'string' ? address.replace(/[\r\n\t]+/g, ' ').trim().slice(0, 512) : '')
    }
    const onPageHide = () => finish('')
    const timer = setTimeout(() => finish(''), 2200)
    window.addEventListener('ls-aegis-address-result', onResult)
    window.addEventListener('pagehide', onPageHide)
    try {
      if (window.prompt('LS_AEGIS_ADDRESS_V1', JSON.stringify({ id, latitude, longitude })) !== 'accepted') finish('')
    } catch { finish('') }
  })
}
