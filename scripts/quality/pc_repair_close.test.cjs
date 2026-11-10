const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const { createRequire } = require('node:module')
const req = createRequire(path.resolve(__dirname, '../../ls-aegis-web/package.json'))
const ts = req('typescript'), vue = req('vue')
function fixture(api = async () => {}) {
  const source = fs.readFileSync('ls-aegis-web/src/views/eoms/repair/apply.vue', 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('apply.ts', source, ts.ScriptTarget.Latest, true)
  const names = ['closeVisible', 'closeReason', 'closeId', 'closing', 'onClose', 'submitClose']
  const selected = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
    .map(s => s.getText(ast)).join('\n')
  const code = ts.transpileModule(selected + '\nexport { ' + names.join(',') + ' }', { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const calls = [], warnings = []; let refreshes = 0
  const exports = {}
  vm.runInNewContext(code, { exports, ...vue, Message: { warning: v => warnings.push(v), success() {} },
    closeRepairApply: async (id, reason) => { calls.push({ id, reason }); return api(id, reason) }, search: () => refreshes++ })
  return { ...exports, calls, warnings, refreshes: () => refreshes }
}
test('close rejects blank and oversized reasons without requests', async () => {
  const f = fixture(); f.onClose({ id: '910308990905229435' })
  for (const reason of ['', ' \n ', 'x'.repeat(501)]) { f.closeReason.value = reason; assert.equal(await f.submitClose(), false) }
  assert.equal(f.calls.length, 0); assert.equal(f.refreshes(), 0)
})
test('close captures string ID and trimmed reason, forbids concurrent submission', async () => {
  let done; const f = fixture(() => new Promise(resolve => { done = resolve }))
  f.onClose({ id: '910308990905229435' }); f.closeReason.value = '  客户取消 & 无需服务  '
  const pending = f.submitClose(); assert.equal(await f.submitClose(), false)
  f.onClose({ id: 'other' }); assert.equal(f.closeId.value, '910308990905229435')
  done(); assert.equal(await pending, true); assert.equal(f.calls.length, 1)
  assert.equal(f.calls[0].id, '910308990905229435'); assert.equal(f.calls[0].reason, '客户取消 & 无需服务')
  assert.equal(f.refreshes(), 1); assert.equal(f.closing.value, false)
})
test('failed close keeps dialog eligible for retry and does not refresh success', async () => {
  const f = fixture(async () => { throw Error('network error') }); f.onClose({ id: 'test' }); f.closeReason.value = 'reason'
  assert.equal(await f.submitClose(), false); assert.equal(f.closing.value, false); assert.equal(f.refreshes(), 0)
  assert.equal(f.closeVisible.value, true)
})
test('close API preserves query reason containing reserved characters', async () => {
  const source = fs.readFileSync('ls-aegis-web/src/apis/eoms/repairApply.ts', 'utf8')
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const calls = [], exports = {}; vm.runInNewContext(code, { exports, require: () => ({ default: { put: async url => calls.push(url) } }) })
  await exports.closeRepairApply('910308990905229435', '客户 & a=b?中文')
  const url = new URL(calls[0], 'http://localhost'); assert.equal(url.searchParams.get('remark'), '客户 & a=b?中文')
  assert.equal([...url.searchParams].length, 1)
})
