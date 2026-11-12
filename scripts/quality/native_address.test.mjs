import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
const source = await readFile(new URL('../../ls-aegis-h5/src/utils/nativeAddress.js', import.meta.url), 'utf8')
const { getNativeAddress } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)
const position = { coords: { latitude: 39.9087, longitude: 116.3975 } }

function fixture(t, prompt, native = true) {
  const old = globalThis.window
  class Window extends EventTarget {
    listeners = new Set()
    __LS_AEGIS_NATIVE_GEOCODER__ = native
    prompt = prompt
    addEventListener(type, handler) { this.listeners.add(handler); super.addEventListener(type, handler) }
    removeEventListener(type, handler) { this.listeners.delete(handler); super.removeEventListener(type, handler) }
  }
  const window = new Window()
  globalThis.window = window
  t.after(() => { globalThis.window = old })
  return window
}
const answer = (window, id, address) => window.dispatchEvent(new CustomEvent('ls-aegis-address-result', { detail: { id, address } }))

test('plain browser never opens a prompt dialog', async t => {
  fixture(t, () => { throw Error('browser prompt must not open') }, false)
  assert.equal(await getNativeAddress(position), '')
})
test('native response is correlated, normalized and released', async t => {
  const window = fixture(t, (message, payload) => {
    assert.equal(message, 'LS_AEGIS_ADDRESS_V1')
    const request = JSON.parse(payload)
    assert.equal(request.latitude, position.coords.latitude)
    queueMicrotask(() => {
      answer(window, 'unrelated', '错误地址')
      answer(window, request.id, '  示例市\n服务中心  ')
    })
    return 'accepted'
  })
  assert.equal(await getNativeAddress(position), '示例市 服务中心')
  assert.equal(window.listeners.size, 0)
})
test('unavailable native provider falls back immediately', async t => {
  const window = fixture(t, () => 'unavailable')
  assert.equal(await getNativeAddress(position), '')
  assert.equal(window.listeners.size, 0)
})
test('stalled provider times out without retaining event listeners', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  const window = fixture(t, () => 'accepted')
  const request = getNativeAddress(position)
  t.mock.timers.tick(2300)
  assert.equal(await request, '')
  assert.equal(window.listeners.size, 0)
})
test('leaving the page releases the pending lookup', async t => {
  const window = fixture(t, () => 'accepted')
  const request = getNativeAddress(position)
  window.dispatchEvent(new Event('pagehide'))
  assert.equal(await request, '')
  assert.equal(window.listeners.size, 0)
})
test('coordinates outside valid ranges never call the native bridge', async t => {
  fixture(t, () => { throw Error('invalid coordinates') })
  for (const coords of [{ latitude: 91, longitude: 0 }, { latitude: 0, longitude: 181 }, { latitude: NaN, longitude: 0 }]) {
    assert.equal(await getNativeAddress({ coords }), '')
  }
})
