const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const { createRequire } = require('node:module')
const req = createRequire(path.resolve('ls-aegis-web/package.json'))
const vue = req('vue'), ts = req('typescript')
const fromVue = createRequire(req.resolve('vue'))
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }

function extract(file, names, globals = {}, effects = false) {
  const text = fs.readFileSync('ls-aegis-web/src/' + file, 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile(file, text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const source = ast.statements.filter(s => (ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
    || (effects && ts.isExpressionStatement(s) && ts.isCallExpression(s.expression) && ['watch', 'onUnmounted'].includes(s.expression.expression.getText(ast))))
    .map(s => s.getText(ast)).join('\n')
  const box = { ...vue, Blob, ArrayBuffer, console, ...globals, exports: {} }
  vm.runInNewContext(ts.transpileModule(source + '\nexports.api = {' + names.join(',') + '}', { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, box)
  return box.exports.api
}
function preview(onClose = () => {}) {
  const errors = [], revoked = [], timers = new Map(); let sequence = 0
  const api = extract('components/FilePreview/index.vue', ['previewVersion', 'rendererEvents', 'visible', 'loading', 'blobUrl', 'loadingTimer', 'filePreview', 'clearLoadingTimer', 'startLoading', 'stopLoading', 'releaseBlobUrl', 'onLoading', 'onPreview', 'renderedHandler', 'errorHandler', 'onClose', 'isSessionCurrent'], {
    Message: { error: value => errors.push(value) }, emit: onClose,
    URL: { revokeObjectURL: value => revoked.push(value) },
    setTimeout: fn => { timers.set(++sequence, fn); return sequence }, clearTimeout: id => timers.delete(id),
  })
  return { ...api, errors, revoked, timers }
}
const info = name => ({ fileInfo: { fileName: name, fileType: 'pdf', data: new ArrayBuffer(2) } })

test('preview: old render events cannot stop or fail the current file', () => {
  const p = preview(); p.onPreview(info('A')); const old = p.rendererEvents.value
  p.onPreview(info('B')); old.rendered(); old.error()
  assert.equal(p.loading.value, true); assert.equal(p.errors.length, 0)
  p.rendererEvents.value.rendered(); assert.equal(p.loading.value, false); assert.equal(p.timers.size, 0)
})
test('preview: closing or replacing a pending download invalidates its token and releases old URLs', () => {
  const p = preview(); p.blobUrl.value = 'blob:old'
  const a = p.onLoading({ fileName: 'A' }); assert.equal(p.revoked[0], 'blob:old')
  p.onClose(); p.onPreview(info('A'), a); assert.equal(p.visible.value, false)
  const b = p.onLoading({ fileName: 'B' }); p.onPreview(info('A'), a)
  assert.equal(p.filePreview.fileInfo.fileName, 'B'); p.onPreview(info('B'), b)
  assert.equal(p.filePreview.fileInfo.fileName, 'B'); assert.equal(p.visible.value, true)
})
function field(download, extensions = { A: 'pdf', B: 'pdf' }) {
  let f; const p = preview(() => f?.invalidateOpen())
  const errors = []
  f = extract('components/PlatformFileField/index.vue', ['openingId', 'openRequestVersion', 'invalidateOpen', 'previewOffice', 'openFile'], {
    filePreviewRef: vue.ref(p), nameMap: { A: 'A.pdf', B: 'B.pdf' }, extMap: extensions,
    downloadFile: download, OfficeTypes: ['pdf', 'docx', 'xlsx'], WordTypes: ['docx'], ExcelTypes: ['xlsx'], ImageTypes: [],
    Message: { error: e => errors.push(e) }, closeImagePreview() {},
    textTitle: vue.ref(''), textContent: vue.ref(''), textVisible: vue.ref(false),
  })
  return { f, p, errors }
}
test('attachments: late A download cannot replace B or clear its loading indicator', async () => {
  const a = deferred(), b = deferred(); const { f, p } = field(id => id === 'A' ? a.promise : b.promise)
  const first = f.openFile('A'), second = f.openFile('B')
  a.resolve({ data: new Blob(['A'], { type: 'application/pdf' }) }); await first
  assert.equal(f.openingId.value, 'B'); assert.equal(p.filePreview.fileInfo.fileName, 'B')
  b.resolve({ data: new Blob(['B'], { type: 'application/pdf' }) }); await second
  assert.equal(p.filePreview.fileInfo.fileName, 'B'); assert.equal(f.openingId.value, '')
})
test('attachments: closing during download prevents reopening; stale failure does not close B', async () => {
  const a = deferred(), b = deferred(); const { f, p, errors } = field(id => id === 'A' ? a.promise : b.promise)
  const first = f.openFile('A'); p.onClose(); a.resolve({ data: new Blob(['A']) }); await first
  assert.equal(p.visible.value, false)
  const second = f.openFile('B'); b.resolve({ data: new Blob(['B']) }); await second
  assert.equal(p.visible.value, true); assert.equal(errors.length, 0)
})
test('attachments: late permission failure cannot clear a newer preview', async () => {
  const a = deferred(), b = deferred(); const { f, p, errors } = field(id => id === 'A' ? a.promise : b.promise)
  const first = f.openFile('A'), second = f.openFile('B')
  b.resolve({ data: new Blob(['B']) }); await second; a.reject(new Error('controlled denial')); await first
  assert.equal(p.filePreview.fileInfo.fileName, 'B'); assert.equal(p.visible.value, true); assert.equal(errors.length, 0)
})
test('file manager: Office preview uses authenticated file ID and ignores late closed responses', async () => {
  const gate = deferred(), calls = []; const p = preview()
  const api = extract('views/system/file/main/FileMain/index.vue', ['handleClickFile'], {
    filePreviewRef: vue.ref(p), ImageTypes: [], OfficeTypes: ['pdf'],
    downloadFile: id => { calls.push(id); return gate.promise }, Message: { error() {} },
  })
  const pending = api.handleClickFile({ id: '900000000000000001', extension: 'pdf', originalName: 'A.pdf', url: 'https://obsolete.invalid/file.pdf' })
  p.onClose(); gate.resolve({ data: new Blob(['pdf']) }); await pending
  assert.equal(calls[0], '900000000000000001'); assert.equal(p.visible.value, false)
})
test('common utilities: subpaths are the exact existing implementations', () => {
  const original = req('xe-utils')
  for (const name of ['browse', 'mapTree', 'eachTree', 'toTreeArray', 'findTree', 'groupBy']) assert.equal(req('xe-utils/' + name), original[name])
})
test('sign preview: engine is lazy and chunk failure clears the active file with retry feedback', async () => {
  let imports = 0; const errors = [], pdfSrc = vue.ref(new ArrayBuffer(2)), pdfLoading = vue.ref(true)
  const form = vue.reactive({ fileId: 'long-id' }), selectedFileName = vue.ref('A.pdf')
  const api = extract('views/sign/execute/index.vue', ['SignPdfPlacer', 'onPdfLoadError'], {
    Spin: vue.defineComponent(() => () => vue.h('span')), pdfSrc, pdfLoading, form, selectedFileName,
    Message: { error: e => errors.push(e) }, require: () => { imports++; throw new Error('controlled chunk failure') },
  })
  assert.equal(imports, 0)
  const app = vue.createSSRApp(api.SignPdfPlacer); app.config.errorHandler = () => {}
  await fromVue('@vue/server-renderer').renderToString(app)
  assert.equal(imports, 1); assert.equal(pdfSrc.value, undefined); assert.equal(pdfLoading.value, false)
  assert.equal(form.fileId, undefined); assert.ok(errors[0].includes('重试'))
})

async function renderLines(kind, locked) {
  const { descriptor } = fromVue('@vue/compiler-sfc').parse(fs.readFileSync('ls-aegis-web/src/views/quote/components/QuoteLines.vue', 'utf8'))
  const script = fromVue('@vue/compiler-sfc').compileScript(descriptor, { id: 'quality-quote-lines', inlineTemplate: true })
  const code = ts.transpileModule(script.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const box = { exports: {}, require: name => {
    if (name === 'vue') return vue
    if (name === '@/constant/table-col') {
      const result = { exports: {} }
      vm.runInNewContext(ts.transpileModule(fs.readFileSync('ls-aegis-web/src/constant/table-col.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, { exports: result.exports })
      return result.exports
    }
    throw Error(name)
  } }
  vm.runInNewContext(code, box)
  const app = vue.createSSRApp(box.exports.default, { kind, locked, ready: true, subtotal: 120,
    lines: [{ uid: 'stable-id', lineType: kind, itemName: '现场服务', brandModel: '型号', quantity: 2, price: 60, amount: 120 }],
    unitOptions: [{ label: '次', value: '次' }], goodsOptions: [],
  })
  app.use(req('@arco-design/web-vue').default)
  // Arco Table defers its columns to mounted; exercise its exact column-slot contract
  // in SSR while retaining real Arco inputs, numbers, selects, buttons and links.
  app.component('a-table', vue.defineComponent({ props: ['data'], setup(props, { slots }) {
    vue.provide('quality-rows', props.data)
    return () => vue.h('table', slots.columns?.())
  } }))
  app.component('a-table-column', vue.defineComponent({ props: ['title'], setup(props, { slots }) {
    const rows = vue.inject('quality-rows')
    return () => vue.h('tr', [vue.h('th', props.title), ...rows.map((record, rowIndex) => vue.h('td', slots.cell?.({ record, rowIndex })))])
  } }))
  return fromVue('@vue/server-renderer').renderToString(app)
}
test('quote split: column slots with real Arco controls retain service values and subtotal', async () => {
  const html = await renderLines('SERVICE', false)
  for (const value of ['二、技术服务费部分', '工作量', '现场服务', '120.00', '添加服务行']) assert.ok(html.includes(value), value)
  assert.ok(!html.includes('选择库存商品'))
})
test('quote preview: rapid edits coalesce, stale frame events cannot stop a new render, unmount clears timers', async () => {
  const props = vue.reactive({ html: 'A' }), timers = new Map(); let id = 0, dispose
  const p = extract('views/quote/components/QuotePreviewPane.vue', ['generation', 'frame', 'loading', 'debounce', 'fallback', 'onLoad'], {
    props, setTimeout: fn => { timers.set(++id, fn); return id }, clearTimeout: key => timers.delete(key), onUnmounted: fn => { dispose = fn },
  }, true)
  props.html = 'B'; props.html = 'C'; await vue.nextTick()
  assert.equal(timers.size, 1); const pending = [...timers.values()][0]; timers.clear(); pending()
  assert.equal(p.frame.value.html, 'C'); const old = p.frame.value.loaded
  props.html = 'D'; await vue.nextTick(); const latest = [...timers.values()][0]; timers.clear(); latest()
  old(); assert.equal(p.loading.value, true); assert.equal(p.frame.value.html, 'D')
  dispose(); assert.equal(timers.size, 0)
})
test('quote split: locked material table hides add/remove and disables editing', async () => {
  const html = await renderLines('MATERIAL', true)
  for (const value of ['一、材料部分', '材料名称/规格', '品牌/型号', '120.00', 'disabled']) assert.ok(html.includes(value), value)
  assert.ok(!html.includes('添加材料行')); assert.ok(!html.includes('arco-link-status-danger'))
})
