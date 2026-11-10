const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const { createRequire } = require('node:module')
const requireWeb = createRequire(path.resolve('ls-aegis-web/package.json'))
const ts = requireWeb('typescript'), vue = requireWeb('vue')
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
const kinds = [['stock-in','StockIn'], ['stock-out','StockOut'], ['receipt','Receipt'], ['payment','Payment']]
const row = id => ({ id, remark: id, inType: 'PURCHASE', outType: 'SALE', inDate: '2026-11-07', outDate: '2026-11-07', receiptDate: '2026-11-07', paymentDate: '2026-11-07',
  warehouseId: '909000000000000001', unitId: '909000000000000002', accountId: '909000000000000003', contractId: '909000000000000004', amount: 123.45,
  items: [{ goodsId: '909000000000000005', quantity: 2, taxInclusivePrice: 56.5, taxRate: 13 }] })
function fixture(folder, name, get, parse) {
  const text = fs.readFileSync(`ls-aegis-web/src/views/psi/${folder}/EditModal.vue`, 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('editor.ts', text, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const names = ['editorRequestVersion', 'editorReady', 'editorSaving', 'reset', 'fill', 'open', 'onAdd', 'onUpdate', 'onView', 'save', 'handleExcelUpload']
  const source = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast)))).map(s => s.getText(ast)).join('\n')
  const form = vue.reactive({}), dataId = vue.ref(''), visible = vue.ref(false), readonly = vue.ref(false), calls = []
  const globals = { ...vue, form, dataId, visible, readonly, isUpdate: vue.computed(() => !!dataId.value && !readonly.value), exports: {},
    loadOptions: async () => {}, loadContractOptions: async () => {}, addLine: () => { form.items.push({ goodsId: '909000000000000005', quantity: 1, taxInclusivePrice: 0, taxRate: 13 }) },
    ensureSelectOpt() {}, toBizId: value => value == null ? undefined : String(value), crypto: { randomUUID: () => 'test-line' },
    warehouseOptions: vue.ref([]), unitOptions: vue.ref([]), accountOptions: vue.ref([]), goodsOptions: vue.ref([]), contractOptions: vue.ref([]),
    Message: { warning() {}, success() {} }, emit: event => calls.push({ event }), AbortController, FormData,
    [`parseImport${name}`]: parse,
    [`get${name}`]: get, [`update${name}`]: async (payload, id) => calls.push({ id, payload: JSON.parse(JSON.stringify(payload)) }),
    [`add${name}`]: async payload => calls.push({ payload: JSON.parse(JSON.stringify(payload)) }), console,
  }
  vm.runInNewContext(ts.transpileModule(source + `\nexports.api={onAdd,onUpdate,onView,reset,save${folder.startsWith('stock-') ? ',handleExcelUpload' : ''}}`,
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, globals)
  return { ...globals.exports.api, form, dataId, visible, readonly, calls }
}

for (const [folder, name] of kinds.filter(([folder]) => folder.startsWith('stock-'))) {
  for (const mode of ['switch', 'abort']) test(`${folder} import: ${mode} invalidates a pending Excel parse result`, async () => {
    const response = deferred(), f = fixture(folder, name, async id => ({ data: row(id) }), () => response.promise)
    await f.onAdd(); let successes = 0, failures = 0
    const request = f.handleExcelUpload({ fileItem: { file: new Blob(['synthetic import fixture']) }, onSuccess: () => successes++, onError: () => failures++ })
    if (mode === 'switch') await f.onUpdate('B')
    else request.abort()
    response.resolve({ data: { items: [{ goodsId: '999', goodsName: 'stale import', quantity: 1, taxInclusivePrice: 1, taxRate: 0 }] } })
    for (let step = 0; step < 15; step++) await Promise.resolve()
    assert.equal(f.form.items[0].goodsId, '909000000000000005'); assert.equal(successes, 0); assert.equal(failures, 1)
  })
}
for (const [folder, name] of kinds) {
  test(`${folder} editor: late A detail cannot become B form or B save payload`, async () => {
    const a = deferred(), b = deferred(); const f = fixture(folder, name, id => id === 'A' ? a.promise : b.promise)
    const old = f.onUpdate('A'); await Promise.resolve(); const current = f.onUpdate('B'); await Promise.resolve()
    b.resolve({ data: row('B') }); await current; a.resolve({ data: row('A') }); await old
    await f.save(); assert.equal(f.form.remark, 'B')
    const write = f.calls.find(call => call.payload); assert.equal(write.id, 'B'); assert.equal(write.payload.remark, 'B')
    assert.equal(write.payload.unitId, '909000000000000002')
  })
  test(`${folder} editor: closing while detail is pending cannot reopen or refill it`, async () => {
    const response = deferred(); const f = fixture(folder, name, () => response.promise)
    const pending = f.onUpdate('A'); await Promise.resolve(); f.reset(); f.visible.value = false
    response.resolve({ data: row('A') }); await pending
    assert.equal(f.visible.value, false); assert.equal(f.form.remark, '')
    const result = await f.save(); assert.equal(result, false); assert.equal(f.calls.length, 0)
  })
  test(`${folder} editor: denied B cannot be reopened by stale A detail`, async () => {
    const a = deferred(), b = deferred(); const f = fixture(folder, name, id => id === 'A' ? a.promise : b.promise)
    const old = f.onUpdate('A'); await Promise.resolve(); const current = f.onUpdate('B'); await Promise.resolve()
    b.reject(new Error('controlled permission failure')); await current.catch(() => {})
    a.resolve({ data: row('A') }); await old
    assert.equal(f.visible.value, false); assert.equal(await f.save(), false); assert.equal(f.calls.length, 0)
  })
}
