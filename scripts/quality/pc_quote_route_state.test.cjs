const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const { createRequire } = require('node:module')
const requireWeb = createRequire(path.resolve('ls-aegis-web/package.json'))
const ts = requireWeb('typescript'), vue = requireWeb('vue')
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
const header = (id, name = id) => ({ id, projectName: name, quoteNo: `Q-${id}`, customerId: '909000000000000001', status: 'DRAFT', taxRate: 13,
  details: [{ id: `line-${id}`, lineType: 'SERVICE', itemName: name, quantity: 2, price: 50, amount: 100 }] })

function fixture(api = {}) {
  const script = fs.readFileSync('ls-aegis-web/src/views/quote/form.vue', 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('quote.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const names = ['detailRequestVersion', 'detailLoading', 'loadedQuoteId', 'quoteReady', 'resetQuoteForm', 'loadDetail', 'save', 'changeStatus', 'buildPayload', 'onExportWord', 'onExportPdf']
  const source = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
    .map(s => s.getText(ast)).join('\n')
  const form = vue.reactive({}), dataId = vue.ref('A'), saving = vue.ref(false), syncingValidUntil = vue.ref(false)
  const materialLines = vue.ref([]), serviceLines = vue.ref([]), calls = { get: [], update: [], add: [], downloads: [], routes: [], modals: [], warnings: [] }
  let uid = 0
  const globals = { ...vue, form, dataId, saving, syncingValidUntil, materialLines, serviceLines,
    goodsPrompted: vue.ref(false), pendingGoodsLineUid: vue.ref(), appStore: { getCompany: () => 'Synthetic Company' },
    DATETIME_FORMAT: 'test', dayjs: value => ({ format: () => value || '2026-11-07 13:00:00' }), calcValidUntil: () => '2026-11-14 13:00:00', defaultTerms: 'Synthetic terms',
    ensureDefaultRows() { if (!materialLines.value.length) materialLines.value.push({ uid: `default-${++uid}` }); if (!serviceLines.value.length) serviceLines.value.push({ uid: `default-${++uid}` }) },
    loadGoodsOptions: api.loadGoodsOptions || (async () => {}), crypto: { randomUUID: () => `uid-${++uid}` }, toBizId: value => value == null ? undefined : String(value),
    locked: vue.computed(() => ['CONFIRM', 'CANCEL'].includes(form.status)),
    materialSubtotal: vue.ref(0), serviceSubtotal: vue.ref(100), totalWithTax: vue.ref(113), buildDetails: () => serviceLines.value.filter(row => row.itemName).map(row => ({ ...row })),
    getQuote: async id => { calls.get.push(id); return api.get ? api.get(id) : { data: header(id) } },
    updateQuote: async payload => { calls.update.push(JSON.parse(JSON.stringify(payload))); return api.update ? api.update(payload) : {} },
    addQuote: async payload => { calls.add.push(JSON.parse(JSON.stringify(payload))); return api.add ? api.add(payload) : { data: 'NEW' } },
    Message: { warning: message => calls.warnings.push(message), success() {} }, Modal: { confirm: options => calls.modals.push(options) },
    router: { replace: async route => calls.routes.push(route), push: route => calls.routes.push(route) },
    buildQuoteExportFileName: (name, no, ext) => `${name}-${no}${ext}`,
    useDownload: async (fn, name) => { if (api.downloadGate) await api.downloadGate.promise; const data = await fn(); calls.downloads.push({ name, data }); return data },
    downloadQuoteDocx: async id => ({ id, format: 'docx' }), downloadQuotePdf: async id => ({ id, format: 'pdf' }),
    exports: {}, console,
  }
  const code = ts.transpileModule(source + '\nexports.api = { loadDetail, save, changeStatus, onExportWord, onExportPdf }',
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  vm.runInNewContext(code, globals)
  return { ...globals.exports.api, form, dataId, saving, syncingValidUntil, materialLines, serviceLines, calls }
}

test('quote route: late A detail cannot overwrite B or become B save payload', async () => {
  const a = deferred(), b = deferred(); let first = true
  const f = fixture({ get: id => id === 'A' ? a.promise : first ? (first = false, b.promise) : { data: header('B') } })
  const old = f.loadDetail(); f.dataId.value = 'B'; const current = f.loadDetail()
  b.resolve({ data: header('B') }); await current; a.resolve({ data: header('A') }); await old
  await f.save()
  assert.equal(f.form.projectName, 'B'); assert.equal(f.calls.update[0].id, 'B'); assert.equal(f.calls.update[0].projectName, 'B')
})

test('quote route: permission failure for B removes A content and forbids saving/exporting B', async () => {
  const f = fixture({ get: id => id === 'A' ? { data: header('A') } : Promise.reject(new Error('controlled permission rejection')) })
  await f.loadDetail(); f.dataId.value = 'B'; await f.loadDetail().catch(() => {})
  await f.save(); await f.onExportWord()
  assert.equal(f.form.projectName, undefined); assert.equal(f.form.customerId, undefined)
  assert.equal(f.calls.update.length, 0); assert.equal(f.calls.downloads.length, 0)
})

test('quote route: starting a new draft does not retain the previous quote ID, customer or lines', async () => {
  const f = fixture(); await f.loadDetail(); f.dataId.value = ''; await f.loadDetail()
  assert.equal(f.form.id, undefined); assert.equal(f.form.projectName, undefined); assert.equal(f.form.customerId, undefined)
  assert.equal(f.serviceLines.value.filter(row => row.itemName).length, 0)
})

test('quote route: an old confirmation dialog cannot submit the newly selected quote', async () => {
  const f = fixture(); await f.loadDetail(); f.changeStatus('CONFIRM', '确认')
  f.dataId.value = 'B'; await f.loadDetail(); await f.calls.modals[0].onOk()
  assert.equal(f.calls.update.length, 0)
})

test('quote route: A-B-A generations keep the newest A detail', async () => {
  const firstA = deferred(), b = deferred(), lastA = deferred(); let aCount = 0
  const f = fixture({ get: id => id === 'A' ? (++aCount === 1 ? firstA.promise : lastA.promise) : b.promise })
  const p1 = f.loadDetail(); f.dataId.value = 'B'; const p2 = f.loadDetail(); f.dataId.value = 'A'; const p3 = f.loadDetail()
  lastA.resolve({ data: header('A', 'new A') }); await p3; b.resolve({ data: header('B') }); await p2
  firstA.resolve({ data: header('A', 'old A') }); await p1
  assert.equal(f.form.projectName, 'new A'); assert.equal(f.syncingValidUntil.value, false)
})

test('quote route: duplicate saves are rejected while the first request is pending', async () => {
  const response = deferred(); const f = fixture({ update: () => response.promise }); await f.loadDetail()
  const first = f.save(), second = f.save(); const writes = f.calls.update.length
  response.resolve({}); await Promise.all([first, second])
  assert.equal(writes, 1); assert.equal(f.saving.value, false)
})

test('quote export: delayed download executes the captured quote ID matching its filename', async () => {
  for (const method of ['onExportWord', 'onExportPdf']) {
    const gate = deferred(), f = fixture({ downloadGate: gate }); await f.loadDetail()
    const pending = f[method](); f.dataId.value = 'B'; gate.resolve(); await pending
    assert.equal(f.calls.downloads[0].data.id, 'A'); assert.match(f.calls.downloads[0].name, /^A-Q-A\./)
  }
})

test('quote route: completed old save does not reload the new route', async () => {
  const response = deferred(), f = fixture({ update: () => response.promise }); await f.loadDetail()
  const pending = f.save(); f.dataId.value = 'B'; await f.loadDetail(); response.resolve({}); await pending
  assert.equal(f.calls.get.filter(id => id === 'B').length, 1)
})
