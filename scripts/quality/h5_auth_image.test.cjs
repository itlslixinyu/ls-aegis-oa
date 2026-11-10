const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function fixture() {
  const source = fs.readFileSync('ls-aegis-h5/src/components/EomsAuthImage.vue', 'utf8')
    .match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm, '')
  const requests = [], released = []; let watcher, dispose, cleanup
  const view = new Function('ref', 'watch', 'onUnmounted', 'defineProps', 'fetchEomsFileObjectUrl', 'needsAuthFetch', 'releaseEomsFileObjectUrl', source + '\nreturn { displaySrc }')(
    v => ({ value: v }), (get, fn) => { watcher = fn }, fn => { dispose = fn }, () => ({ src: '' }),
    id => { const pending = deferred(); requests.push({ id, ...pending }); return pending.promise },
    value => /^\d+$/.test(value), value => released.push(value))
  return { view, requests, released,
    change(value) { if (cleanup) cleanup(); return watcher(value, undefined, fn => { cleanup = fn }) },
    unmount() { if (cleanup) cleanup(); dispose() } }
}

test('authenticated image: B wins when prior A resolves last and both references release once', async () => {
  const f = fixture(); const a = f.change('101'), b = f.change('102')
  f.requests[1].resolve('blob:B'); await b
  f.requests[0].resolve('blob:A'); await a
  assert.equal(f.view.displaySrc.value, 'blob:B'); assert.deepEqual(f.released, ['101'])
  f.unmount(); assert.deepEqual(f.released, ['101', '102'])
})

test('authenticated image: unmounted pending request releases its late result without changing display', async () => {
  const f = fixture(); const pending = f.change('101'); f.unmount()
  f.requests[0].resolve('blob:A'); await pending
  assert.equal(f.view.displaySrc.value, ''); assert.deepEqual(f.released, ['101'])
})

test('authenticated image: A-B-A generations reject the first A and stale failures cannot clear the latest A', async () => {
  const f = fixture(); const firstA = f.change('101'), b = f.change('102'), latestA = f.change('101')
  f.requests[2].resolve('blob:latestA'); await latestA
  f.requests[1].reject(new Error('controlled old error')); await b
  f.requests[0].resolve('blob:firstA'); await firstA
  assert.equal(f.view.displaySrc.value, 'blob:latestA'); assert.deepEqual(f.released, ['101'])
  f.unmount(); assert.deepEqual(f.released, ['101', '101'])
})

test('authenticated image: changing to direct image clears prior display and releases a late authenticated result', async () => {
  const f = fixture(); const pending = f.change('101'); await f.change('data:image/png,test')
  f.requests[0].resolve('blob:A'); await pending
  assert.equal(f.view.displaySrc.value, 'data:image/png,test'); assert.deepEqual(f.released, ['101'])
})
