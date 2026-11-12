const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const req = require('node:module').createRequire(path.resolve('ls-aegis-web/package.json'))
const ts = req('typescript'), vue = req('vue')
const tick = () => new Promise(resolve => setImmediate(resolve))

function fixture() {
  const requests = [], events = [], successes = [], errors = []
  let unmount
  const props = vue.reactive({ multiple: true, modelValue: undefined, parentPath: '/' })
  const upload = (data, options) => new Promise((resolve, reject) => { requests.push({ data, options, resolve, reject }) })
  const source = fs.readFileSync('ls-aegis-web/src/components/PlatformFileField/index.vue','utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const code = ts.transpileModule(source + '\nexport { handleUpload, uploading, beforeUpload };', { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const result = { exports: {} }
  vm.runInNewContext(code, {
    ...vue, onUnmounted: fn => { unmount = fn },
    defineProps: () => props, withDefaults: (value, defaults) => Object.assign(value, { ...defaults, ...value }),
    defineEmits: () => (...args) => events.push(args),
    defineExpose: () => {},
    AbortController, Blob, File, FormData, URL, console,
    exports: result.exports,
    require(name) {
      if (name === '@arco-design/web-vue') return { Message: { success() {}, error() {}, info() {} } }
      if (name === '@vueuse/core') return { useWindowSize: () => ({ width: vue.ref(1000) }) }
      if (name === '@/apis/system/common') return { upload, getFileInfo: async () => ({ data: {} }), downloadFile: async () => ({}) }
      if (name === '@/constant/file') return { ImageTypes: ['png','jpg'], OfficeTypes: [], WordTypes: [], ExcelTypes: [] }
      if (name === '@/components/FilePreview/index.vue') return {}
      throw Error('Unexpected test import: '+name)
    },
  })
  const start = () => result.exports.handleUpload({ fileItem: { file: new File(['test'], 'synthetic.png') }, onSuccess: value => successes.push(value), onError: value => errors.push(value), onProgress() {} })
  return { ...result.exports, start, requests, events, successes, errors, unmount: () => unmount() }
}

test('parallel attachment completions preserve both IDs before parent model updates', async () => {
  const f = fixture(); f.start(); f.start()
  assert.ok(f.requests.every(r => r.options.signal instanceof AbortSignal))
  f.requests[0].resolve({ data: { id: '101' } }); await tick()
  assert.equal(f.uploading.value, true)
  f.requests[1].resolve({ data: { id: '102' } }); await tick()
  const updates = f.events.filter(e => e[0] === 'update:modelValue')
  assert.equal(updates.at(-1)[1], '101,102'); assert.equal(f.uploading.value, false)
  assert.equal(f.successes.length, 2)
})

test('abort reaches the transport and a late result cannot emit successful attachment data', async () => {
  const f = fixture(); const request = f.start(); request.abort()
  assert.equal(f.requests[0].options.signal.aborted, true)
  f.requests[0].resolve({ data: { id: '101' } }); await tick()
  assert.equal(f.events.length, 0); assert.equal(f.successes.length, 0)
})

test('unmount aborts all owned uploads and ignores late errors or completions', async () => {
  const f = fixture(); f.start(); f.start(); f.unmount()
  assert.ok(f.requests.every(r => r.options.signal.aborted))
  f.requests[0].resolve({ data: { id: '101' } }); f.requests[1].reject(Error('controlled late failure')); await tick()
  assert.equal(f.events.length, 0); assert.equal(f.errors.length, 0); assert.equal(f.successes.length, 0)
})

test('missing server file ID reports an error and retains no attachment reference', async () => {
  const f = fixture(); f.start(); f.requests[0].resolve({ data: {} }); await tick()
  assert.equal(f.errors.length, 1); assert.equal(f.events.length, 0); assert.equal(f.uploading.value, false)
})

test('attachment input rejects empty, oversized and disallowed formats while accepting the inclusive 10MB limit', () => {
  const f = fixture()
  assert.equal(f.beforeUpload(new File([], 'empty.png')), false)
  assert.equal(f.beforeUpload(new File(['data'], 'wrong.exe')), false)
  assert.equal(f.beforeUpload(new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.png')), false)
  assert.equal(f.beforeUpload(new File([new Uint8Array(10 * 1024 * 1024)], 'limit.png')), true)
})

test('unsafe numeric file IDs cannot enter the business attachment model', async () => {
  const f = fixture(); f.start(); f.requests[0].resolve({ data: { id: Number.MAX_SAFE_INTEGER + 1 } }); await tick()
  assert.equal(f.errors.length, 1); assert.equal(f.events.length, 0)
})
