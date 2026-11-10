const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const { createRequire } = require('node:module')
const requireWeb = createRequire(path.resolve('ls-aegis-web/package.json'))
const ts = requireWeb('typescript'), vue = requireWeb('vue')
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
function fixture(api = {}) {
  const script = fs.readFileSync('ls-aegis-web/src/views/sign/execute/index.vue', 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('sign.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const names = ['projectLoadSeq','fileLoadSeq','assertBlobOk','clearFile','clearProjectFiles','loadPdfByFileId','resetAll','onSubmit']
  const source = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast)))).map(s => s.getText(ast)).join('\n')
  const form = vue.reactive({ projectId: 'P', fileId: undefined }), pendingTasks = vue.ref([]), calls = []
  const globals = { ...vue, form, pendingTasks, calls, exports: {}, Blob, console,
    selectedFileName: vue.ref(''), pdfSrc: vue.ref(), selectedTaskId: vue.ref(), pdfLoading: vue.ref(false), pdfMeta: { totalPages: 1, pages: [] },
    fileOptions: vue.ref([]), sealKeyword: vue.ref(''), sealList: vue.ref([]), placementMode: vue.ref('general'), submitting: vue.ref(false),
    getFileInfo: async id => ({ data: { id, originalName: `${id}.pdf` } }), downloadFile: api.download || (async id => ({ data: new Blob([id], { type: 'application/pdf' }) })),
    buildGeneralSeals: () => [{ sealId: '909000000000000001', page: 1 }], buildSpanSeals: () => [], revokeSealImages() {},
    unifiedSign: async payload => { calls.push(JSON.parse(JSON.stringify(payload))); return api.sign ? api.sign(payload) : {} },
    Message: { error() {}, success() {}, warning() {} },
  }
  vm.runInNewContext(ts.transpileModule(source + '\nexports.api={clearFile,resetAll,loadPdfByFileId,onSubmit}',
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, globals)
  return { ...globals, ...globals.exports.api }
}

test('sign PDF: clearing selection invalidates a pending file fetch', async () => {
  const response = deferred(), f = fixture({ download: () => response.promise })
  const pending = f.loadPdfByFileId('A'); f.clearFile()
  response.resolve({ data: new Blob(['A'], { type: 'application/pdf' }) }); await pending
  assert.equal(f.form.fileId, undefined); assert.equal(f.pdfSrc.value, undefined); assert.equal(f.selectedFileName.value, '')
})

test('sign PDF: delayed old arrayBuffer cannot replace B bytes while form still selects B', async () => {
  const bytes = deferred(); let reading = false
  const a = new Blob(['A'], { type: 'application/pdf' })
  Object.defineProperty(a, 'arrayBuffer', { value: () => { reading = true; return bytes.promise } })
  const f = fixture({ download: async id => ({ data: id === 'A' ? a : new Blob(['B'], { type: 'application/pdf' }) }) })
  const old = f.loadPdfByFileId('A')
  for (let step = 0; step < 12 && !reading; step++) await Promise.resolve()
  assert.equal(reading, true)
  await f.loadPdfByFileId('B'); bytes.resolve(new TextEncoder().encode('A').buffer); await old
  assert.equal(f.form.fileId, 'B'); assert.equal(new TextDecoder().decode(f.pdfSrc.value), 'B'); assert.equal(f.selectedFileName.value, 'B.pdf')
})

test('sign submit: repeated click cannot send duplicate signing requests', async () => {
  const response = deferred(), f = fixture({ sign: () => response.promise })
  f.form.fileId = 'A'; f.pendingTasks.value = [{ sealId: '909000000000000001' }]
  const first = f.onSubmit(), second = f.onSubmit(); const sent = f.calls.length
  response.resolve({}); await Promise.all([first, second])
  assert.equal(sent, 1); assert.equal(f.submitting.value, false)
})

test('sign submit: completion for old project does not clear newly selected project and tasks', async () => {
  const response = deferred(), f = fixture({ sign: () => response.promise })
  f.form.fileId = 'A'; f.pendingTasks.value = [{ sealId: '909000000000000001' }]
  const old = f.onSubmit(); f.resetAll(); f.form.projectId = 'NEW'; f.form.fileId = 'B'; f.pendingTasks.value = [{ sealId: '909000000000000002' }]
  response.resolve({}); await old
  assert.equal(f.form.projectId, 'NEW'); assert.equal(f.form.fileId, 'B'); assert.equal(f.pendingTasks.value[0].sealId, '909000000000000002')
})
