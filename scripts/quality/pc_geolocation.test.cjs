const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')
const requireWeb = createRequire(path.resolve('ls-aegis-web/package.json'))
const ts = requireWeb('typescript')

// Execute the real PC component functions with synthetic coordinates and controlled services.
function fixture(provider, api = async () => ({ data: { success: true } })) {
  const vue = fs.readFileSync('ls-aegis-web/src/views/system/config/eoms/index.vue', 'utf8')
  const start = vue.indexOf('const getBrowserLocation =')
  const end = vue.indexOf('const handleResetValue =', start)
  const source = ts.transpileModule(vue.slice(start, end), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const timers = new Map(), messages = [], calls = [], akConfigured = { value: true }, testingAk = { value: false }
  let next = 0, locationCalls = 0
  const navigator = provider === null ? {} : { geolocation: { getCurrentPosition(...args) { locationCalls++; provider(...args) } } }
  const result = new Function('navigator', 'Message', 'akConfigured', 'testingAk', 'testBaiduMapAk', 'setTimeout', 'clearTimeout',
    source + '\nreturn { getBrowserLocation, onTestBaiduAk }')(
    navigator, Object.fromEntries(['warning', 'error', 'success'].map(kind => [kind, value => messages.push({ kind, value })])),
    akConfigured, testingAk, async value => { calls.push(value); return api(value) },
    (fn, ms) => { timers.set(++next, { fn, ms }); return next }, id => timers.delete(id))
  return { ...result, timers, messages, calls, akConfigured, testingAk, locationCalls: () => locationCalls }
}

test('PC map test: no configured AK does not request location or call the map API', async () => {
  const f = fixture(() => assert.fail('must not request location')); f.akConfigured.value = false
  await f.onTestBaiduAk()
  assert.equal(f.locationCalls(), 0); assert.equal(f.calls.length, 0); assert.equal(f.testingAk.value, false)
  assert.match(f.messages[0].value, /先配置并保存/)
})

test('PC map test: provider without callbacks reaches application deadline and releases loading', async () => {
  const f = fixture(() => {}); const pending = f.onTestBaiduAk()
  assert.equal(f.testingAk.value, true)
  assert.equal(f.timers.size, 1, 'application deadline must exist independently of browser timeout')
  const deadline = [...f.timers.values()][0]; assert.equal(deadline.ms, 10000); deadline.fn()
  await pending
  assert.equal(f.testingAk.value, false); assert.equal(f.calls.length, 0); assert.equal(f.timers.size, 0)
  assert.match(f.messages[0].value, /定位超时/)
})

test('PC map test: coordinates arriving after timeout cannot invoke reverse geocoding', async () => {
  let success
  const f = fixture(ok => { success = ok }); const pending = f.onTestBaiduAk()
  assert.equal(f.timers.size, 1); [...f.timers.values()][0].fn(); await pending
  success({ coords: { longitude: 120.123456, latitude: 30.123456 } })
  await Promise.resolve(); assert.equal(f.calls.length, 0); assert.equal(f.testingAk.value, false)
})

test('PC map test: successful coordinates cancel deadline and send one string coordinate pair', async () => {
  let success, failure, options
  const f = fixture((ok, fail, config) => { success = ok; failure = fail; options = config })
  const pending = f.onTestBaiduAk()
  success({ coords: { longitude: 120.123456, latitude: 30.123456 } }); failure({ code: 1, PERMISSION_DENIED: 1 })
  await pending
  assert.deepEqual(f.calls, [{ longitude: '120.123456', latitude: '30.123456' }])
  assert.equal(options.maximumAge, 0); assert.equal(options.timeout, 10000); assert.equal(f.timers.size, 0)
  assert.equal(f.testingAk.value, false)
})

test('PC map test: permission rejection releases loading without contacting map service', async () => {
  const f = fixture((ok, fail) => fail({ code: 1, PERMISSION_DENIED: 1 })); await f.onTestBaiduAk()
  assert.equal(f.calls.length, 0); assert.equal(f.testingAk.value, false); assert.equal(f.timers.size, 0)
  assert.match(f.messages[0].value, /允许浏览器/)
})

test('PC map test: synchronous provider exception clears its deadline and loading', async () => {
  const f = fixture(() => { throw new Error('controlled provider exception') }); await f.onTestBaiduAk()
  assert.equal(f.calls.length, 0); assert.equal(f.testingAk.value, false); assert.equal(f.timers.size, 0)
})

test('PC map test: reverse-geocoding rejection releases loading and ignores a second click while busy', async () => {
  let resolve
  const response = new Promise(yes => { resolve = yes })
  const f = fixture(ok => ok({ coords: { longitude: 120, latitude: 30 } }), () => response)
  const first = f.onTestBaiduAk(); await Promise.resolve(); const second = f.onTestBaiduAk(); await Promise.resolve()
  const observed = { location: f.locationCalls(), api: f.calls.length }
  resolve({ data: { success: false, message: 'controlled map denial' } }); await Promise.all([first, second])
  assert.deepEqual(observed, { location: 1, api: 1 })
  assert.equal(f.testingAk.value, false); assert.equal(f.messages.at(-1).kind, 'error')
})

test('PC map test: map request exception and unavailable geolocation both release loading', async () => {
  const f = fixture(ok => ok({ coords: { longitude: 120, latitude: 30 } }), async () => { throw new Error('controlled request rejection') })
  await f.onTestBaiduAk(); assert.equal(f.testingAk.value, false); assert.equal(f.timers.size, 0)
  const unavailable = fixture(null); await unavailable.onTestBaiduAk()
  assert.equal(unavailable.testingAk.value, false); assert.equal(unavailable.calls.length, 0)
})
