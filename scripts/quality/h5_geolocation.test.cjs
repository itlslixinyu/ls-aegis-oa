const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
function fixture(provider) {
  const timers = new Map(); let next = 0
  const context = { navigator: { geolocation: { getCurrentPosition: provider } },
    setTimeout(fn, ms) { timers.set(++next, { fn, ms }); return next },
    clearTimeout(id) { timers.delete(id) } }
  vm.createContext(context)
  vm.runInContext(fs.readFileSync('ls-aegis-h5/src/utils/geolocation.js', 'utf8').replace('export ', ''), context)
  return { get: context.getCurrentPosition, timers }
}
test('permission prompt without callbacks reaches deadline and ignores late coordinates', () => {
  let success; let options; const outcomes = []
  const f = fixture((s, e, o) => { success = s; options = o })
  f.get(v => outcomes.push(v), e => outcomes.push(e.code), { timeout: 8000, maximumAge: 0 })
  assert.equal(options.maximumAge, 0)
  const timer = [...f.timers.values()][0]; assert.equal(timer.ms, 8000)
  timer.fn(); success({ coords: { longitude: 1 } })
  assert.deepEqual(outcomes, [3]); assert.equal(f.timers.size, 0)
})
test('successful GPS cancels deadline and ignores later failure', () => {
  let ok, fail; const outcomes = []; const f = fixture((s,e) => { ok=s; fail=e })
  f.get(v => outcomes.push(v), e => outcomes.push(e))
  const position = { coords: { latitude: 1, longitude: 2 } }
  ok(position); fail({ code: 1 })
  assert.deepEqual(outcomes, [position]); assert.equal(f.timers.size, 0)
})
test('permission denial and synchronous provider failure settle immediately', () => {
  for (const provider of [(s,e) => e({ code: 1 }), () => { throw new Error('provider unavailable') }]) {
    const errors = []; const f = fixture(provider)
    f.get(() => assert.fail('must not succeed'), e => errors.push(e))
    assert.equal(errors.length, 1); assert.equal(f.timers.size, 0)
  }
})
