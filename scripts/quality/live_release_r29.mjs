/** R29 in-use local business-logic testing. Execution results stay under .local; see Docs/testing/verification.md for scope.
 * Requires an explicitly provisioned own session in ignored .local; never discovers
 * browser sessions, resets existing passwords, disables CAPTCHA, or runs bootstrap SQL.
 * Every phase is explicit; a closed ledger refuses replay. --joint uses fresh R30-JOINT business fixtures.
 * auth-users/h5-auth public-image rendering additionally requires Python + Pillow.
 */
import assert from 'node:assert/strict'
import { readFileSync, writeFileSync, existsSync, unlinkSync } from 'node:fs'
import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import { randomBytes, randomUUID } from 'node:crypto'
import { deflateSync } from 'node:zlib'
import { spawnSync } from 'node:child_process'
import { createInterface } from 'node:readline/promises'

const root = resolve(import.meta.dirname, '../..')
const requireWeb = createRequire(resolve(root, 'ls-aegis-web/package.json'))
const { sm2, sm3 } = requireWeb('sm-crypto')
const joint = process.argv.includes('--joint')
const runOption = process.argv.find(value => value.startsWith('--run='))?.slice(6)
assert.ok(!runOption || /^R\d+(?:-UI)?$/.test(runOption), 'Explicit new run must use R<number> or R<number>-UI')
const runName = runOption || (joint ? 'R30-JOINT' : 'R29')
const deploymentOption = process.argv.find(value => value.startsWith('--deployment='))?.slice(13)
function deploymentEvidence(fallback) {
  const file = deploymentOption ? resolve(root, deploymentOption) : fallback
  assert.ok(file.startsWith(resolve(root, '.local') + (process.platform === 'win32' ? '\\' : '/')), 'Deployment evidence must stay in local evidence directory')
  const evidence = JSON.parse(readFileSync(file, 'utf8'))
  assert.equal(evidence.ready, true)
  assert.equal(evidence.pendingChangesets, 0)
  assert.match(evidence.sha256, /^[a-f0-9]{64}$/)
  if (deploymentOption) {
    const actual = spawnSync('docker', ['exec', 'ls-aegis-mcs-backend', 'sha256sum', '/app/jars/ls-aegis-server.jar'], { encoding: 'utf8', timeout: 20000 })
    assert.equal(actual.status, 0, 'Cannot independently verify current local backend JAR')
    assert.equal(actual.stdout.trim().split(/\s+/)[0], evidence.sha256, 'Deployment evidence does not match actual backend; no business phase may run')
  }
  return evidence
}
const local = name => resolve(root, '.local', name.replace(/^r29-/, runName.toLowerCase() + '-'))
const base = 'http://127.0.0.1:8000'
const admin = JSON.parse(readFileSync(local('r29-session.json'), 'utf8')).token
const ledgerPath = local('r29-ledger.json')
const privatePath = local('r29-identities.json')
const state = existsSync(ledgerPath) ? JSON.parse(readFileSync(ledgerPath, 'utf8'))
  : { run: runName, prefix: 'ITLS-TEST-'+runName, target: base, results: [], objects: {}, started: new Date().toISOString() }
assert.equal(state.run, runName); assert.equal(state.target, base)
assert.ok(!state.closed, 'Run is closed. Preserve evidence; provision a new run instead of replaying historical fixtures.')
const identities = existsSync(privatePath) ? JSON.parse(readFileSync(privatePath, 'utf8')) : {}
function save() { writeFileSync(ledgerPath, JSON.stringify(state, null, 2)); writeFileSync(privatePath, JSON.stringify(identities), { mode: 0o600 }) }
function register(key, kind, id, marker, cleanup) {
  assert.match(String(id), kind === 'sign_record' ? /^[a-f0-9]{32}$/ : /^\d+$/)
  if (state.objects[key] && state.objects[key].id !== String(id)) (state.archivedObjects ||= []).push(state.objects[key])
  state.objects[key] = { kind, id: String(id), marker, cleanup, status: 'ACTIVE' }; save()
  return String(id)
}
const idOf = value => String(typeof value === 'object' ? value.id : value)
const listOf = value => Array.isArray(value) ? value : value?.list || value?.records || []
function pdfFixture() {
  const text = `BT /F1 18 Tf 50 750 Td (ITLS TEST ${runName} SYNTHETIC) Tj ET\n`
  const objects = ['<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>',
    `<< /Length ${Buffer.byteLength(text)} >>\nstream\n${text}endstream`, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>']
  let pdf = '%PDF-1.4\n'; const offsets = []
  objects.forEach((object, index) => { offsets.push(Buffer.byteLength(pdf)); pdf += `${index + 1} 0 obj\n${object}\nendobj\n` })
  const xref = Buffer.byteLength(pdf)
  return Buffer.from(pdf + `xref\n0 6\n0000000000 65535 f \n${offsets.map(value => `${String(value).padStart(10, '0')} 00000 n \n`).join('')}trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`)
}
async function api(path, { method = 'GET', data, token = admin, reject = false, headers = {} } = {}) {
  assert.ok(path.startsWith('/') && !path.startsWith('//'))
  const response = await fetch(base + path, { method,
    headers: { ...(data instanceof FormData ? {} : { 'Content-Type': 'application/json' }), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    body: data === undefined ? undefined : data instanceof FormData ? data : JSON.stringify(data), signal: AbortSignal.timeout(30000) })
  const body = await response.json()
  if (reject) { assert.equal(body.success, false, `Expected rejection: ${method} ${path}`); return body }
  assert.equal(response.status, 200, `HTTP ${method} ${path}`)
  assert.equal(body.success, true, `${method} ${path}: ${body.msg}`)
  return body.data
}
async function test(id, fn) {
  id=id.replace(/^R29-/,runName+'-')
  try { const evidence = await fn(); state.results.push({ id, at: new Date().toISOString(), status: 'PASS', backendSha256: state.currentBackendSha256, evidence }); console.log(`PASS ${id}`) }
  catch (error) { state.results.push({ id, at: new Date().toISOString(), status: 'FAIL', backendSha256: state.currentBackendSha256, error: error.message }); console.log(`FAIL ${id}: ${error.message}`); process.exitCode = 1 }
  save()
}
function sql(query) {
  assert.match(query, /^SELECT\b/i)
  assert.ok(!/;|\b(INTO|OUTFILE|DUMPFILE|SLEEP|BENCHMARK|LOAD_FILE)\b/i.test(query), 'Read-only SQL only')
  const shell = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -N -B ls_aegis -e "$1"'
  const run = spawnSync('docker', ['exec', 'ls-aegis-mysql', 'sh', '-c', shell, 'sh', query], { encoding: 'utf8', timeout: 20000 })
  assert.equal(run.status, 0, 'Read-only persistence assertion failed; connection details suppressed')
  return run.stdout.trim()
}
const removed = key => { state.objects[key].status = 'CLEANED'; save() }

const now = () => {
  const parts = Object.fromEntries(new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' }).formatToParts(new Date()).map(part => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`
}
async function createOwned(key, path, payload, table, column, marker) {
  await api(path, { method: 'POST', data: payload })
  return register(key, table, sql(`SELECT id FROM ${table} WHERE ${column}='${marker}' ORDER BY id DESC LIMIT 1`), marker, 'exact business deletion or full return compensation')
}
async function fixtures() {
  await test('R29-BASELINE-FIXTURES', async () => {
    assert.equal(String((await api('/auth/user/info',{token:admin})).id), '1')
    for (const [key, path, table, column, suffix, field, extra] of [
      ['unit', '/psi/unit', 'psi_unit', 'unit_name', '-供应商A', 'unitName', { unitType: 'BOTH', unitNature: 'ENTERPRISE', status: 1 }],
      ['unitOther', '/psi/unit', 'psi_unit', 'unit_name', '-供应商B', 'unitName', { unitType: 'BOTH', unitNature: 'ENTERPRISE', status: 1 }],
      ['rootCategory', '/psi/goods-category', 'psi_goods_category', 'name', '-一级', 'name', { parentId: '0', status: 1 }],
      ['category', '/psi/goods-category', 'psi_goods_category', 'name', '-二级', 'name', () => ({ parentId: state.objects.rootCategory.id, status: 1 })],
      ['goods', '/psi/goods', 'psi_goods', 'goods_name', '-商品A', 'goodsName', () => ({ categoryId: state.objects.category.id, unit: '件', defaultTaxRate: 0, status: 1 })],
      ['goodsOther', '/psi/goods', 'psi_goods', 'goods_name', '-商品B', 'goodsName', () => ({ categoryId: state.objects.category.id, unit: '件', defaultTaxRate: 0, status: 1 })],
      ['warehouse', '/psi/warehouse', 'psi_warehouse', 'warehouse_name', '-仓库A', 'warehouseName', { managerUserId: '1', isDefault: false, status: 1 }],
      ['warehouseOther', '/psi/warehouse', 'psi_warehouse', 'warehouse_name', '-仓库B', 'warehouseName', { managerUserId: '1', isDefault: false, status: 1 }]
    ]) {
      const marker = state.prefix + suffix
      if (state.objects[key]) {
        assert.equal(state.objects[key].status, 'ACTIVE', 'Do not reactivate cleaned or retained fixtures')
        const existing = await api(path + '/' + state.objects[key].id)
        assert.equal(existing[field], marker, 'Existing fixture ownership must be rechecked')
        assert.equal(Number(existing.status), 1, 'Existing fixture must remain enabled')
        continue
      }
      await createOwned(key, path, { [field]: marker, ...(typeof extra === 'function' ? extra() : extra) }, table, column, marker)
    }
    return { onlySynthetic: true, originalUsersChanged: false, target: base }
  })
}
async function rejectionProbe(id, path, payload, { table, deletePath = path + '/', expected = 'Invalid request must not be accepted', confirmFailure = false } = {}) {
  await test(id, async () => {
    const response = await fetch(base + path, { method: 'POST', headers: { Authorization: 'Bearer ' + admin, 'Content-Type': 'application/json' }, body: JSON.stringify(payload), signal: AbortSignal.timeout(30000) })
    const result = await response.json()
    let created
    const evidence = { path, payload, http: response.status, success: result.success, code: result.code, message: result.msg, confirmed: false }
    ;(state.probes ||= {})[id] = evidence; save()
    try {
      if (confirmFailure) assert.equal(result.success,true,'Draft creation is a required precondition; a creation error cannot prove confirmation rejection')
      if (result.success) {
        created = register(id, table || path, idOf(result.data), state.prefix, 'exact draft DELETE')
        evidence.createdId = created; evidence.persisted = await api(path + '/' + created); save()
        if (confirmFailure) {
          const failure = await api(path + '/' + created + '/confirm', { method: 'POST', data: { idempotentKey: randomUUID() }, reject: true })
          evidence.confirmError = failure.msg; save()
          assert.equal((await api(path + '/' + created)).docStatus, 'DRAFT')
          return evidence
        }
      }
      assert.equal(result.success, false, expected)
      if(id.endsWith('INVOICE-HEADER-TAX-INCONSISTENT'))assert.match(result.msg,/价税|合计/,'Must reject the inconsistent amount, not an unrelated database prerequisite')
      return evidence
    } finally {
      if (created) { await api(deletePath + created, { method: 'DELETE' }); removed(id) }
    }
  })
}
async function quoteLogic() {
  for (const scenario of ['tax-only', 'status-only', 'forged-total']) await test('R29-QUOTE-' + scenario.toUpperCase(), async () => {
    const marker = state.prefix + '-' + scenario
    const id = register('quote-' + scenario, 'eoms_quote', idOf(await api('/quote', { method: 'POST', data: {
      customerId: state.objects.unit.id, projectName: marker, taxRate: 13,
      details: [{ lineType: 'SERVICE', itemName: marker, quantity: 2, price: 50 }] } })), marker, 'DRAFT/SUBMIT DELETE')
    try {
      const before = await api('/quote/' + id); assert.equal(Number(before.totalAmount), 113)
      const update = scenario === 'tax-only' ? { id, taxRate: 6 } : scenario === 'forged-total' ? { id, totalAmount: 1, taxRate: 13 } : { id, status: 'SUBMIT' }
      await api('/quote', { method: 'PUT', data: update })
      const after = await api('/quote/' + id)
      const evidence = { id, before: { taxRate: before.taxRate, totalAmount: before.totalAmount }, update,
        after: { taxRate: after.taxRate, totalAmount: after.totalAmount, status: after.status }, expected: scenario === 'tax-only' ? { taxRate: 6, totalAmount: 106 } : { taxRate: 13, totalAmount: 113 } }
      ;(state.quoteProbes ||= []).push(evidence); save()
      assert.equal(Number(after.taxRate), evidence.expected.taxRate, 'Omitted tax rate must be retained')
      assert.equal(Number(after.totalAmount), evidence.expected.totalAmount, 'Amount must match persisted tax rate and lines')
      return evidence
    } finally { await api('/quote/' + id, { method: 'DELETE' }); removed('quote-' + scenario) }
  })
}
async function draftModules() {
  const warehouseId = state.objects.warehouse.id, other = state.objects.warehouseOther.id, goodsId = state.objects.goods.id, unitId = state.objects.unit.id
  const transfer = { transferDate: now(), fromWarehouseId: warehouseId, toWarehouseId: other, remark: state.prefix, items: [{ goodsId, quantity: 1 }] }
  await rejectionProbe('R29-TRANSFER-SAME-WAREHOUSE', '/psi/stock-transfer', { ...transfer, toWarehouseId: warehouseId })
  await rejectionProbe('R29-TRANSFER-NEGATIVE-QUANTITY', '/psi/stock-transfer', { ...transfer, items: [{ goodsId, quantity: -1 }] })
  await rejectionProbe('R29-TRANSFER-SHORTAGE-ROLLBACK', '/psi/stock-transfer', transfer, { confirmFailure: true })
  const check = { checkDate: now(), warehouseId, remark: state.prefix, items: [{ goodsId, bookQty: 0, checkQty: -1 }] }
  await rejectionProbe('R29-CHECK-NEGATIVE-COUNT', '/psi/stock-check', check)
  const invoice = { invoiceNo: state.prefix + '-TEST-NOT-REAL', invoiceType: 'NORMAL', invoiceDate: now(), unitId,
    taxInclusiveAmount: 113, untaxAmount: 100, taxAmount: 13, buyerTaxNo: 'TEST-BUYER', sellerTaxNo: 'TEST-SELLER', remark: state.prefix, links: [], items: [] }
  await rejectionProbe('R29-INVOICE-UNLINKED-CONFIRM', '/psi/invoice-out', invoice, { confirmFailure: true })
}
async function sourcePurchases() {
  await test('R29-PURCHASE-SOURCE-DOCUMENTS', async () => {
    for (const key of ['purchaseA', 'purchaseB']) {
      if (state.objects[key]) continue
      const id = register(key, 'psi_stock_in', idOf(await api('/psi/stock-in', { method: 'POST', data: { inType: 'PURCHASE', inDate: now(),
        warehouseId: state.objects.warehouse.id, unitId: state.objects.unit.id, remark: state.prefix,
        items: [{ goodsId: state.objects.goods.id, quantity: 10, taxInclusivePrice: 100, taxRate: 0 }, { goodsId: state.objects.goodsOther.id, quantity: 4, taxInclusivePrice: 50, taxRate: 0 }] } })), state.prefix, 'full purchase return')
      await api('/psi/stock-in/' + id + '/confirm', { method: 'POST', data: { idempotentKey: randomUUID() } })
    }
    return { sourceIds: ['purchaseA', 'purchaseB'].map(key => state.objects[key].id), onlyOwnedWarehouseGoods: true, cashDocumentsCreated: 0 }
  })
}
async function returnLogic() {
  const first = await api('/psi/stock-in/' + state.objects.purchaseA.id), second = await api('/psi/stock-in/' + state.objects.purchaseB.id)
  const firstLine = first.items.find(item => String(item.goodsId) === state.objects.goods.id), otherLine = second.items.find(item => String(item.goodsId) === state.objects.goods.id)
  const line = { originItemId: String(firstLine.id), goodsId: state.objects.goods.id, quantity: 1, taxInclusivePrice: 100, taxRate: 0 }
  const payload = { returnType: 'PURCHASE_RETURN', returnDate: now(), warehouseId: state.objects.warehouse.id,
    unitId: state.objects.unit.id, originStockInId: state.objects.purchaseA.id, remark: state.prefix, items: [line] }
  for (const [id, changed] of [
    ['R29-RETURN-CROSS-UNIT', { ...payload, unitId: state.objects.unitOther.id }],
    ['R29-RETURN-CROSS-ORIGIN-ITEM', { ...payload, items: [{ ...line, originItemId: String(otherLine.id) }] }],
    ['R29-RETURN-WRONG-GOODS', { ...payload, items: [{ ...line, goodsId: state.objects.goodsOther.id }] }],
    ['R29-RETURN-NEGATIVE-PRICE', { ...payload, items: [{ ...line, taxInclusivePrice: -100 }] }],
    ['R29-RETURN-NEGATIVE-QUANTITY', { ...payload, items: [{ ...line, quantity: -1 }] }],
    ['R29-RETURN-OVER-QUANTITY', { ...payload, items: [{ ...line, quantity: 11 }] }],
    ['R29-RETURN-DUPLICATE-ORIGIN-OVER-TOTAL', { ...payload, items: [{ ...line, quantity: 6 }, { ...line, quantity: 6 }] }],
    ['R29-RETURN-WRONG-WAREHOUSE', { ...payload, warehouseId: state.objects.warehouseOther.id }]
  ]) await rejectionProbe(id, '/psi/stock-return', changed)
}
async function compensatePurchases() {
  await test('R29-PURCHASE-RETURN-COMPENSATION', async () => {
    for (const key of ['purchaseA', 'purchaseB']) {
      const origin = state.objects[key]; if (!origin || origin.status === 'COMPENSATED') continue
      const doc = await api('/psi/stock-in/' + origin.id)
      if (doc.docStatus === 'DRAFT') { await api('/psi/stock-in/' + origin.id, { method: 'DELETE' }); removed(key); continue }
      const returnKey = key + 'Return'
      if (!state.objects[returnKey]) {
        const payload = { returnType: 'PURCHASE_RETURN', returnDate: now(), warehouseId: state.objects.warehouse.id, unitId: state.objects.unit.id,
          originStockInId: origin.id, remark: state.prefix + '-补偿', items: doc.items.map(item => ({ originItemId: String(item.id), goodsId: String(item.goodsId),
            quantity: item.quantity, taxInclusivePrice: item.taxInclusivePrice, taxRate: item.taxRate })) }
        register(returnKey, 'psi_stock_return', idOf(await api('/psi/stock-return', { method: 'POST', data: payload })), state.prefix, 'retain confirmed return audit')
      }
      const id = state.objects[returnKey].id
      if ((await api('/psi/stock-return/' + id)).docStatus === 'DRAFT') await api('/psi/stock-return/' + id + '/confirm', { method: 'POST', data: { idempotentKey: randomUUID() } })
      origin.status = 'COMPENSATED'; state.objects[returnKey].status = 'AUDIT_RETAINED'; save()
    }
    const quantities = sql(`SELECT COALESCE(SUM(remain_quantity),0),COALESCE(SUM(remain_amount),0) FROM psi_stock_batch WHERE warehouse_id=${state.objects.warehouse.id} AND goods_id IN (${state.objects.goods.id},${state.objects.goodsOther.id})`)
    assert.deepEqual(quantities.split('\t').map(Number), [0, 0])
    assert.equal(Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_payable WHERE unit_id=${state.objects.unit.id} AND deleted=0`)), 0)
    return { quantityAndCost: [0, 0], payable: 0, cashPaymentsCreated: 0 }
  })
}
async function todoLogic() {
  await test('R29-UNPAID-RETURN-REFUND-TODO', async () => {
    const returnIds = ['purchaseAReturn', 'purchaseBReturn'].map(key => state.objects[key].id)
    const rows = sql(`SELECT id,todo_type,biz_id,done FROM psi_todo WHERE biz_id IN (${returnIds.join(',')}) AND deleted=0`)
    const todos = rows.split('\n').filter(Boolean).map(row => { const [id, type, bizId, done] = row.split('\t'); return { id, type, bizId, done } })
    state.refundTodoProbe = { todos, unitId: state.objects.unit.id, confirmedPayments: Number(sql(`SELECT COUNT(*) FROM psi_payment WHERE unit_id=${state.objects.unit.id} AND doc_status='CONFIRMED' AND deleted=0`)), expectedRefundTodos: 0 }; save()
    assert.equal(state.refundTodoProbe.confirmedPayments, 0)
    assert.equal(todos.filter(item => ['REFUND', 'RECEIVE_REFUND'].includes(item.type) && item.done === '0').length, 0, 'Unpaid purchase returns must not request cash refund receipt')
    return state.refundTodoProbe
  })
}

async function extraBoundaries() {
  const warehouseId = state.objects.warehouse.id, goodsId = state.objects.goods.id, unitId = state.objects.unit.id
  await rejectionProbe('R29-ADJUST-NEGATIVE-COST-PRICE', '/psi/stock-adjust', { adjustDate: now(), warehouseId, remark: state.prefix,
    items: [{ goodsId, adjustType: 'PROFIT', quantity: 1, untaxPrice: -10 }] })
  await rejectionProbe('R29-INVOICE-HEADER-TAX-INCONSISTENT', '/psi/invoice-in', { invoiceNo: state.prefix + '-INVALID-TAX', invoiceType: 'NORMAL', invoiceDate: now(), unitId,
    taxInclusiveAmount: 100, untaxAmount: 100, taxAmount: 13, buyerTaxNo: 'TEST-BUYER', sellerTaxNo: 'TEST-SELLER', remark: state.prefix, links: [], items: [] })
}
async function stockMovement() {
  const a = state.objects.warehouse.id, b = state.objects.warehouseOther.id, goodsId = state.objects.goods.id
  const stock = warehouse => sql(`SELECT COALESCE(SUM(remain_quantity),0),COALESCE(SUM(remain_amount),0) FROM psi_stock_batch WHERE warehouse_id=${warehouse} AND goods_id=${goodsId}`).split('\t').map(Number)
  async function confirm(path, id, key = randomUUID()) { return api(path + '/' + id + '/confirm', { method: 'POST', data: { idempotentKey: key } }) }
  await test('R29-ADJUST-TRANSFER-COST-AND-COMPENSATION', async () => {
    assert.deepEqual(stock(a), [0, 0]); assert.deepEqual(stock(b), [0, 0])
    for (const key of ['transferOut', 'transferBack']) {
      if (state.objects[key]?.status === 'ACTIVE' && (await api('/psi/stock-transfer/' + state.objects[key].id)).docStatus === 'CONFIRMED') state.objects[key].status = 'AUDIT_RETAINED'
    }
    save()
    try {
      const gain = register('gain', 'psi_stock_adjust', idOf(await api('/psi/stock-adjust', { method: 'POST', data: {
        adjustDate: now(), warehouseId: a, remark: state.prefix, items: [{ goodsId, adjustType: 'PROFIT', quantity: 5, untaxPrice: 20 }] } })), state.prefix, 'loss adjustment compensation')
      const key = randomUUID(); await confirm('/psi/stock-adjust', gain, key); await confirm('/psi/stock-adjust', gain, key)
      assert.deepEqual(stock(a), [5, 100])
      for (const [key, fromWarehouseId, toWarehouseId] of [['transferOut', a, b], ['transferBack', b, a]]) {
        const id = register(key, 'psi_stock_transfer', idOf(await api('/psi/stock-transfer', { method: 'POST', data: {
          transferDate: now(), fromWarehouseId, toWarehouseId, remark: state.prefix, items: [{ goodsId, quantity: 2 }] } })), state.prefix, 'opposite transfer / loss adjustment compensation')
        await confirm('/psi/stock-transfer', id)
        const detail = await api('/psi/stock-transfer/' + id)
        assert.equal(detail.items.reduce((total, item) => total + Number(item.costAmount), 0), 40)
        state.objects[key].status = 'AUDIT_RETAINED'; save()
      }
      assert.deepEqual(stock(a), [5, 100]); assert.deepEqual(stock(b), [0, 0])
      return { gainQuantity: 5, gainCost: 100, transferQuantity: 2, transferCostEach: 40, sameKeyGainNoDuplicates: true, roundTripConservesQuantityAndCost: true }
    } finally {
      for (const [suffix, warehouseId] of [['A', a], ['B', b]]) {
        const [quantity] = stock(warehouseId)
        if (quantity > 0) {
          const key = 'compensatingLoss' + suffix
          const id = register(key, 'psi_stock_adjust', idOf(await api('/psi/stock-adjust', { method: 'POST', data: {
            adjustDate: now(), warehouseId, remark: state.prefix + '-盘亏补偿', items: [{ goodsId, adjustType: 'LOSS', quantity }] } })), state.prefix, 'compensates synthetic profit; retain audit')
          await confirm('/psi/stock-adjust', id); state.objects[key].status = 'AUDIT_RETAINED'; save()
        }
        assert.deepEqual(stock(warehouseId), [0, 0])
      }
      if (state.objects.gain) state.objects.gain.status = 'COMPENSATED'; save()
    }
  })
  await test('R29-CHECK-ZERO-DIFFERENCE-NO-STOCK-CHANGE', async () => {
    assert.deepEqual(stock(a), [0, 0], 'Never create a zero-count document while owned stock remains')
    const id = register('checkZero', 'psi_stock_check', idOf(await api('/psi/stock-check', { method: 'POST', data: {
      checkDate: now(), warehouseId: a, remark: state.prefix, items: [{ goodsId, bookQty: 0, checkQty: 0 }] } })), state.prefix, 'confirmed zero-difference audit retained')
    await confirm('/psi/stock-check', id); assert.deepEqual(stock(a), [0, 0])
    state.objects.checkZero.status = 'AUDIT_RETAINED'; save()
    return { id, noDifference: true, quantity: 0, cost: 0 }
  })
}
async function cleanup() {
  await test('R29-CLOSE-EXACT-SYNTHETIC-FALSE-TODOS', async () => {
    const old = JSON.parse(readFileSync(local('r27-ledger.json'), 'utf8'))
    const unitIds = [old.objects.unit.id, state.objects.unit.id]
    for (const table of ['psi_payment', 'psi_receipt']) assert.equal(Number(sql(`SELECT COUNT(*) FROM ${table} WHERE unit_id IN (${unitIds.join(',')}) AND doc_status='CONFIRMED' AND deleted=0`)), 0)
    const rows = sql("SELECT t.id,t.todo_type,t.biz_id,r.remark,t.done FROM psi_todo t JOIN psi_stock_return r ON r.id=t.biz_id WHERE t.deleted=0 AND r.deleted=0 AND t.todo_type IN ('REFUND','RECEIVE_REFUND') AND r.remark IN ('ITLS-TEST-R27-补偿','ITLS-TEST-R29-补偿')")
    const todos = rows.split('\n').filter(Boolean).map(row => { const [id, type, bizId, marker, done] = row.split('\t'); return { id, type, bizId, marker, beforeDone: done } })
    state.exactSyntheticTodoCleanup ||= todos; save()
    for (const entry of todos) {
      assert.match(entry.id, /^\d+$/); assert.ok(['ITLS-TEST-R27-补偿', 'ITLS-TEST-R29-补偿'].includes(entry.marker))
      if (entry.beforeDone === '0') await api('/psi/todo/' + entry.id + '/done', { method: 'POST' })
      assert.equal(sql(`SELECT done FROM psi_todo WHERE id=${entry.id}`), '1')
    }
    return { exactTodos: todos, onlySynthetic: true, noConfirmedPaymentsOrReceipts: true, action: 'manual close of inapplicable test reminders; no money transfer or assertion of actual refund' }
  })
  await test('R29-FIXTURE-CLEANUP-NET-ASSERTIONS', async () => {
    const disabled = []
    for (const [key, path, name] of [['unit', '/psi/unit/', 'unitName'], ['goods', '/psi/goods/', 'goodsName'], ['goodsOther', '/psi/goods/', 'goodsName'],
      ['warehouse', '/psi/warehouse/', 'warehouseName'], ['warehouseOther', '/psi/warehouse/', 'warehouseName'], ['category', '/psi/goods-category/', 'name'], ['rootCategory', '/psi/goods-category/', 'name']]) {
      const entry = state.objects[key]; if (entry.status !== 'ACTIVE') continue
      const detail = await api(path + entry.id); assert.equal(detail[name], entry.marker)
      await api(path + entry.id, { method: 'PUT', data: { ...detail, status: 2 } }); assert.equal(Number((await api(path + entry.id)).status), 2)
      entry.status = 'DISABLED_AUDIT_RETAINED'; save(); disabled.push(key)
    }
    const other = state.objects.unitOther
    if (other.status === 'ACTIVE') {
      assert.equal((await api('/psi/unit/' + other.id)).unitName, other.marker)
      await api('/psi/unit', { method: 'DELETE', data: { ids: [other.id] } }); removed('unitOther')
    }
    const warehouses = [state.objects.warehouse.id, state.objects.warehouseOther.id]
    const goods = [state.objects.goods.id, state.objects.goodsOther.id]
    const net = sql(`SELECT COALESCE(SUM(remain_quantity),0),COALESCE(SUM(remain_amount),0) FROM psi_stock_batch WHERE warehouse_id IN (${warehouses.join(',')}) AND goods_id IN (${goods.join(',')})`).split('\t').map(Number)
    assert.deepEqual(net, [0, 0])
    assert.equal(Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_payable WHERE unit_id=${state.objects.unit.id} AND deleted=0`)), 0)
    const drafts = []
    for (const [table, condition] of [['psi_stock_in', '1=1'], ['psi_stock_return', '1=1'], ['psi_stock_transfer', '1=1'], ['psi_stock_adjust', '1=1'], ['psi_stock_check', '1=1'], ['psi_invoice_in', '1=1'], ['psi_invoice_out', '1=1']]) {
      // All matching rows must be this explicit run; no table-wide cleanup.
      const count = Number(sql(`SELECT COUNT(*) FROM ${table} WHERE remark LIKE '${state.prefix}%' AND deleted=0 AND doc_status='DRAFT' AND ${condition}`))
      assert.equal(count, 0, table + ' owned draft residue'); drafts.push({ table, activeDrafts: count })
    }
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_quote WHERE project_name LIKE '${state.prefix}%' AND del_flag='0'`)), 0)
    const active = Object.entries(state.objects).filter(([, entry]) => entry.status === 'ACTIVE')
    assert.equal(active.length, 0)
    state.cleanupFinal = { quantity: net[0], cost: net[1], unpaidPayable: 0, drafts, activeLedgerObjects: active.length, retainedHistoricalMasters: Object.entries(state.objects).filter(([, entry]) => entry.status === 'DISABLED_AUDIT_RETAINED').map(([key]) => key),
      auditRetention: 'confirmed purchase/return, profit/loss, transfers and zero-difference counts retained; associated masters disabled', realAccountsModified: false }; save()
    return state.cleanupFinal
  })
}
async function finish() {
  assert.ok(state.cleanupFinal && state.cleanupFinal.activeLedgerObjects === 0, 'Cleanup must pass before closing session')
  await test('R29-OWN-SESSION-LOGOUT', async () => {
    await api('/auth/logout', { method: 'POST' }); await api('/auth/user/info', { reject: true })
    return { onlyOwnSession: true, originalAdminPasswordChanged: false }
  })
  if (!process.exitCode) {
    if (existsSync(privatePath)) unlinkSync(privatePath)
    unlinkSync(local('r29-session.json')); state.closed = new Date().toISOString(); state.privateCredentialsFilesRemoved = true
    writeFileSync(ledgerPath, JSON.stringify(state, null, 2))
  }
}
async function provisionUsers() {
  await test('R29-RBAC-TEST-IDENTITIES', async () => {
    for (const suffix of ['A', 'B']) {
      const key = 'dept' + suffix
      if (!state.objects[key]) {
        const marker = state.prefix + '-部门' + suffix
        const created = await api('/system/dept', { method: 'POST', data: { parentId: '1', name: marker, sort: 99, status: 1, description: state.prefix } })
        register(key, 'dept', idOf(created), marker, 'delete after own test users')
      }
    }
    if (!state.objects.limitedRole) {
      const marker = state.prefix + '-无业务权限'
      const created = await api('/system/role', { method: 'POST', data: { name: marker, code: runName.toLowerCase().replaceAll('-', '_')+'_limited', dataScope: 4, sort: 99, description: state.prefix } })
      const id = register('limitedRole', 'role', idOf(created), marker, 'delete after own test users')
      await api(`/system/role/${id}/permission`, { method: 'PUT', data: { roleId: id, menuIds: [], menuCheckStrictly: false } })
    }
    const definitions = { engineer: ['12', 'deptA'], other: ['12', 'deptB'], manager: ['11', 'deptA'], finance: ['13', 'deptA'], limited: [state.objects.limitedRole.id, 'deptB'] }
    for (const [role, [roleId, dept]] of Object.entries(definitions)) {
      if (identities[role]) continue
      const username = runName.toLowerCase().replaceAll('-', '_') + '_' + role
      const created = await api('/system/user', { method: 'POST', data: { username, nickname: state.prefix + '-' + role,
        gender: 0, deptId: state.objects[dept].id, roleIds: [roleId], status: 1, description: state.prefix } })
      assert.ok(created.initialPassword && created.generated)
      identities[role] = { id: idOf(created), username, password: created.initialPassword, initial: true }
      register('user_' + role, 'user', idOf(created), username, 'logout and delete exact test user')
    }
    return { roles: Object.keys(definitions), departments: [state.objects.deptA.id, state.objects.deptB.id], method: 'normal user API; generated initial passwords never printed' }
  })
}

async function authUsers() {
  const fileInput = process.argv.includes('--file-input')
  const input = fileInput ? null : createInterface({ input: process.stdin, output: process.stdout })
  try {
    for (const [role, account] of Object.entries(identities)) {
      if (account.kind === 'h5') continue
      if (account.token) {
        const response = await fetch(base+'/auth/user/info', {
          headers: { Authorization: 'Bearer '+account.token }, signal: AbortSignal.timeout(15000),
        })
        if ((await response.json()).success) continue
        account.token = undefined
        save()
      }
      for (let step = 0; step < (account.initial ? 2 : 1); step++) {
        const challenge = await api('/captcha/image', { token: null })
        assert.equal(challenge.isEnabled, true)
        writeFileSync(local('r29-current-captcha.gif'), Buffer.from(challenge.img.split(',').at(-1), 'base64'))
        assert.equal(spawnSync('python', [resolve(root, 'scripts/quality/render_public_captcha.py'), local('r29-current-captcha.gif'), local('r29-current-captcha.png')], { cwd: root, encoding: 'utf8' }).status, 0, 'Public challenge image rendering failed')
        const ticket = randomUUID()
        writeFileSync(local('r29-auth-prompt.json'), JSON.stringify({ ticket, role, phase: account.initial ? 'initial' : 'active', expireTime: challenge.expireTime }))
        console.log(`CAPTCHA READY ${role} ${account.initial ? 'initial' : 'active'}`)
        let answer
        if (fileInput) {
          const deadline = Date.now() + 180000
          while (Date.now() < deadline) {
            if (existsSync(local('r29-auth-answer.json'))) {
              const packet = JSON.parse(readFileSync(local('r29-auth-answer.json'), 'utf8'))
              if (packet.ticket === ticket) { answer = packet.answer; unlinkSync(local('r29-auth-answer.json')); break }
            }
            await new Promise(resolve => setTimeout(resolve, 100))
          }
          assert.ok(answer, 'Public challenge input timed out')
        } else answer = await input.question('Captcha: ')
        const key = await api('/auth/gm/public-key', { token: null })
        const encrypt = password => { const value = sm2.doEncrypt(password, key.publicKeyHex, 1); return value.startsWith('04') ? value : '04' + value }
        const logged = await api('/auth/login', { method: 'POST', token: null, data: { username: account.username,
          password: encrypt(account.password), authType: 'ACCOUNT', clientId: 'ef51c9a3e9046c4f2ea45142c8a8344a', uuid: challenge.uuid, captcha: answer.trim() } })
        const info = await api('/auth/user/info', { token: logged.token })
        assert.equal(String(info.id), account.id)
        if (account.initial) {
          assert.equal(info.pwdExpired, true)
          const failure = await api('/psi/unit', { token: logged.token, reject: true })
          assert.match(failure.msg, /密码已过期/)
          const next = 'Te9!' + randomBytes(12).toString('hex')
          await api('/user/profile/password', { method: 'PATCH', token: logged.token,
            data: { oldPassword: encrypt(account.password), newPassword: encrypt(next) } })
          account.password = next; account.initial = false; save()
          await api('/auth/user/info', { token: logged.token, reject: true })
          // The loop must perform the second normal login after the mandatory password change.
          step = -1
          continue
        }
        assert.equal(info.pwdExpired, false)
        account.token = logged.token; save()
        console.log('AUTH READY ' + role)
      }
    }
  } finally { input?.close() }
  await test('R29-RBAC-ROLES-CONTEXT', async () => {
    for (let round = 0; round < 3; round++) {
      assert.equal(String((await api('/sign/execute/projects', { token: identities.limited.token, reject: true })).code), '403')
      assert.equal(String((await api('/auth/user/info')).id), '1')
      assert.equal(String((await api('/auth/user/info', { token: identities.engineer.token })).id), identities.engineer.id)
    }
    return { syntheticRoles: Object.keys(identities), initialPasswordEnforced: true, selfPasswordChange: true, alternatingRounds: 3, existingUsersChanged: false }
  })
}

async function authStatus() {
  const checks = [['admin', admin], ...Object.entries(identities).map(([role, value]) => [role, value.token])]
  for (const [role, token] of checks) {
    const response = await fetch(base + '/auth/user/info', { headers: { Authorization: 'Bearer ' + token }, signal: AbortSignal.timeout(15000) })
    const body = await response.json()
    console.log(JSON.stringify({ role, http: response.status, success: body.success, code: body.code, message: body.msg, id: body.data?.id, pwdExpired: body.data?.pwdExpired }))
  }
}


async function inspectOwnedState() {
  const warehouses = [state.objects.warehouse.id, state.objects.warehouseOther.id]
  const goods = [state.objects.goods.id, state.objects.goodsOther.id]
  const snapshot = {
    batches: sql(`SELECT id,source_id,goods_id,total_quantity,remain_quantity,total_amount,remain_amount FROM psi_stock_batch WHERE warehouse_id IN (${warehouses.join(',')}) AND goods_id IN (${goods.join(',')}) ORDER BY id`),
    flows: sql(`SELECT id,source_type,source_id,goods_id,change_qty,change_amount FROM psi_stock_flow WHERE warehouse_id IN (${warehouses.join(',')}) AND goods_id IN (${goods.join(',')}) ORDER BY id`),
    payable: sql(`SELECT id,source_id,tax_inclusive_amount,unwritten_amount FROM psi_payable WHERE unit_id=${state.objects.unit.id} AND deleted=0`)
  }; state.incidentSnapshot ||= snapshot; state.latestOwnedState = snapshot; save(); console.log(JSON.stringify(snapshot))
}
async function recoverOwnedCountIncident() {
  await test('R29-OWNED-COUNT-INCIDENT-CORRECTION', async () => {
    assert.ok(state.incidentSnapshot, 'Preserve pre-correction evidence first')
    const warehouse = state.objects.warehouse.id, goods = state.objects.goods.id
    const check = state.objects.checkZero
    assert.ok(check && check.marker === state.prefix)
    const originalCount = await api('/psi/stock-check/' + check.id)
    assert.equal(originalCount.docStatus, 'CONFIRMED'); assert.equal(originalCount.remark, state.prefix)
    const sourceIds = ['purchaseA', 'purchaseB'].map(key => state.objects[key].id)
    const rows = sql(`SELECT b.id,b.source_id,b.total_quantity,b.remain_quantity,b.untax_price,b.total_amount,b.remain_amount FROM psi_stock_batch b JOIN psi_stock_in s ON s.id=b.source_id WHERE b.warehouse_id=${warehouse} AND b.goods_id=${goods} AND b.source_type='STOCK_IN' AND b.source_id IN (${sourceIds.join(',')}) AND s.remark='${state.prefix}' ORDER BY b.id`)
    const batches = rows.split('\n').map(row => { const [id, source, total, remain, price, amount, remainAmount] = row.split('\t'); return { id, source, total, remain, price, amount, remainAmount } })
    assert.equal(batches.length, 2)
    let entry = state.objects.countIncidentCorrection
    if (!entry) {
      assert.ok(batches.every(batch => Number(batch.total) === 10 && Number(batch.remain) === 0 && Number(batch.amount) === 1000 && Number(batch.remainAmount) === 0))
      const id = register('countIncidentCorrection', 'psi_stock_adjust', idOf(await api('/psi/stock-adjust', { method: 'POST', data: {
        adjustDate: now(), warehouseId: warehouse, remark: state.prefix + '-测试盘点前置错误原批次纠正',
        items: batches.map(batch => ({ goodsId: goods, originBatchId: batch.id, adjustType: 'PROFIT', quantity: 10, untaxPrice: batch.price }))
      } })), state.prefix + '-测试盘点前置错误原批次纠正', 'restores exact owned batches; immutable corrective audit retained')
      entry = state.objects.countIncidentCorrection; entry.idempotentKey = randomUUID(); save()
    }
    const doc = await api('/psi/stock-adjust/' + entry.id)
    assert.equal(doc.remark, entry.marker)
    assert.deepEqual(doc.items.map(item => String(item.originBatchId)).sort(), batches.map(batch => batch.id).sort(), 'Backend must persist explicit batch links before any confirmation')
    if (doc.docStatus === 'DRAFT') await api('/psi/stock-adjust/' + entry.id + '/confirm', { method: 'POST', data: { idempotentKey: entry.idempotentKey } })
    assert.equal(Number(sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE id IN (${batches.map(batch => batch.id).join(',')})`)), 20)
    assert.equal(Number(sql(`SELECT SUM(remain_amount) FROM psi_stock_batch WHERE id IN (${batches.map(batch => batch.id).join(',')})`)), 2000)
    entry.status = 'AUDIT_RETAINED'; check.status = 'COMPENSATED'
    state.incidentRecovery = { originalCountId: check.id, correctionId: entry.id, originalBatchIds: batches.map(batch => batch.id), quantityRestored: 20, costRestored: 2000,
      realDataAffected: false, directSqlMutation: false, restoreMethod: 'explicit original-batch PROFIT adjustment with reason and stock flow audit' }
    for (const result of state.results) if (result.id === 'R29-CHECK-ZERO-DIFFERENCE-NO-STOCK-CHANGE' && !result.validity) {
      result.validity = 'INVALID_PRECONDITION'; result.note = 'The first harness lacked zero-stock prerequisite; it wrote an owned -20 count. Do not treat it as no-change PASS.'
    }
    save(); return state.incidentRecovery
  })
}
const ownStock = () => Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE warehouse_id=${state.objects.warehouse.id} AND goods_id=${state.objects.goods.id}`))
async function confirmOwned(path, key, extra = {}) {
  const entry = state.objects[key]; entry.confirmKey ||= randomUUID(); save()
  return api(path + '/' + entry.id + '/confirm', { method: 'POST', data: { idempotentKey: entry.confirmKey, ...extra } })
}
async function cashSetup() {
  await test('R29-PSI-CONFIRMED-CASH-ALLOCATION', async () => {
    assert.equal(ownStock(), 0)
    const unitId = state.objects.unit.id, warehouseId = state.objects.warehouse.id, goodsId = state.objects.goods.id
    const marker = state.prefix + '-资金全流程'
    assert.ok(!state.objects.fundStockIn, 'Setup is single-use; resume partial fixtures explicitly')
    const account = await createOwned('financeAccount', '/psi/account', { accountName: marker, bankName: '合成测试开户行', accountNo: state.prefix + '-NOT-A-REAL-BANK-ACCOUNT', status: 1, remark: marker }, 'psi_company_account', 'account_name', marker)
    register('fundStockIn', 'psi_stock_in', idOf(await api('/psi/stock-in', { method: 'POST', data: { inType: 'PURCHASE', inDate: now(), unitId, warehouseId, remark: marker,
      items: [{ goodsId, quantity: 10, taxInclusivePrice: 100, taxRate: 0 }] } })), marker, 'full purchase return')
    await confirmOwned('/psi/stock-in', 'fundStockIn')
    register('fundContract', 'psi_contract', idOf(await api('/psi/contract-sale', { method: 'POST', data: { name: marker, signDate: now(), unitId, taxInclusiveAmount: 650, remark: marker } })), marker, 'confirmed audit retained')
    await confirmOwned('/psi/contract-sale', 'fundContract'); state.objects.fundContract.status = 'AUDIT_RETAINED'; save()
    state.funds = { accountId: account, receipts: [], payments: [], saleIds: [], saleReturnIds: [], amounts: [300, 300, 50] }; save()
    for (let index = 0; index < 3; index++) {
      const key = 'fundSale' + index
      const id = register(key, 'psi_stock_out', idOf(await api('/psi/stock-out', { method: 'POST', data: { outType: 'SALE', outDate: now(), unitId, warehouseId,
        contractId: state.objects.fundContract.id, remark: marker, items: [{ goodsId, quantity: index === 2 ? 1 : 2, taxInclusivePrice: index === 2 ? 50 : 150, taxRate: 0 }] } })), marker, 'full sale return')
      state.funds.saleIds.push(id); save(); await confirmOwned('/psi/stock-out', key)
      const receivable = sql(`SELECT id FROM psi_receivable WHERE source_id=${id} AND deleted=0`); assert.match(receivable, /^\d+$/)
      const cashKey = 'fundReceipt' + index, amount = state.funds.amounts[index]
      const receipt = register(cashKey, 'psi_receipt', idOf(await api('/psi/receipt', { method: 'POST', data: { receiptDate: now(), unitId, accountId: account,
        amount, payMethod: 'CASH', remark: marker } })), marker, 'reverse allocation then refund / void; audit retained')
      state.funds.receipts.push({ id: receipt, receivable, amount }); save()
      await confirmOwned('/psi/receipt', cashKey, { writeOffItems: [{ openId: receivable, amount }], writeOffReason: marker })
      assert.equal(Number(sql(`SELECT unwritten_amount FROM psi_receivable WHERE id=${receivable}`)), 0)
    }
    const payable = sql(`SELECT id FROM psi_payable WHERE source_id=${state.objects.fundStockIn.id} AND deleted=0`)
    const payment = register('fundPayment', 'psi_payment', idOf(await api('/psi/payment', { method: 'POST', data: { paymentDate: now(), unitId, accountId: account,
      amount: 1000, payMethod: 'CASH', remark: marker } })), marker, 'reverse allocation then receive refund; audit retained')
    state.funds.payments.push({ id: payment, payable, amount: 1000 }); save()
    await confirmOwned('/psi/payment', 'fundPayment', { writeOffItems: [{ openId: payable, amount: 1000 }], writeOffReason: marker })
    await confirmOwned('/psi/payment', 'fundPayment', { writeOffItems: [{ openId: payable, amount: 1000 }], writeOffReason: marker })
    assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_write_off_item WHERE payment_id=${payment} AND direction='WRITE_OFF' AND deleted=0`)), 1)
    assert.equal(ownStock(), 5)
    return { accountId: account, receipts: state.funds.receipts.map(row => row.id), payment, sameKeyConfirmNoDuplicateAllocation: true, quantity: 5, noExternalBankCall: true }
  })
}
async function cashBank() {
  await test('R29-BANK-CONCURRENT-EXCLUSIVE-AND-UNMATCH', async () => {
    assert.ok(state.funds && state.funds.receipts.length === 3)
    if(!joint){
      const deployment=deploymentEvidence(local('r29-deployment-v3.json'))
      assert.equal(deployment.ready,true);assert.equal(deployment.pendingChangesets,0)
      const existing=sql(`SELECT bank_seq_no,match_status,amount FROM psi_bank_statement WHERE account_id=${state.funds.accountId} AND deleted=0`)
      for(const row of existing.split('\n').filter(Boolean)){
        const [seq,status,amount]=row.split('\t');assert.ok([state.prefix+'-IN-1',state.prefix+'-IN-2'].includes(seq));assert.equal(status,'UNMATCHED');assert.equal(Number(amount),300)
      }
      state.funds.resumePreflight={candidate:deployment.sha256,existingRows:existing,onlyMissingSequenceMayBeAdded:true};save()
    }
    const marker = state.prefix + '-资金全流程'
    const importRequest={accountId:state.funds.accountId,items:[1,2].map(index=>({stmtDate:now(),direction:'IN',amount:300,counterparty:marker,summary:marker,bankSeqNo:state.prefix+'-IN-'+index}))}
    await api('/psi/bank/import', {method:'POST',data:importRequest})
    const ids = sql(`SELECT id FROM psi_bank_statement WHERE account_id=${state.funds.accountId} AND bank_seq_no LIKE '${state.prefix}-IN-%' AND deleted=0 ORDER BY id`).split('\n')
    assert.equal(ids.length, 2); ids.forEach((id, index) => register('fundBankIn' + index, 'psi_bank_statement', id, marker, 'reconcile to synthetic cash originals; retain matched audit'))
    const replay=await Promise.all([1,2].map(()=>api('/psi/bank/import',{method:'POST',data:importRequest})))
    assert.deepEqual(replay,[0,0])
    const receipt = state.funds.receipts[0].id
    const outcomes = await Promise.allSettled(ids.map(statementId => api('/psi/bank/match', { method: 'POST', data: { statementId, matchBizType: 'RECEIPT', matchBizId: receipt } })))
    assert.equal(outcomes.filter(row => row.status === 'fulfilled').length, 1)
    const matched = sql(`SELECT id FROM psi_bank_statement WHERE account_id=${state.funds.accountId} AND match_biz_type='RECEIPT' AND match_biz_id=${receipt} AND match_status='MATCHED' AND deleted=0`)
    assert.match(matched, /^\d+$/); state.funds.bankInIds = ids; state.funds.firstMatched = matched; save()
    const writeOffs = listOf(await api('/psi/receipt/' + receipt + '/write-offs')); assert.equal(writeOffs.length, 1)
    await api('/psi/receipt/' + receipt + '/reverse', { method: 'POST', data: { writeOffId: String(writeOffs[0].writeOffId), reason: marker } })
    state.funds.receipts[0].reversed = true; save()
    const blocked = await api('/psi/receipt/' + receipt + '/void', { method: 'POST', data: { reason: marker, idempotentKey: randomUUID() }, reject: true })
    assert.match(blocked.msg, /银行/)
    const key = randomUUID()
    await api('/psi/bank/' + matched + '/unmatch', { method: 'POST', data: { reason: marker, idempotentKey: key } })
    await api('/psi/bank/' + matched + '/unmatch', { method: 'POST', data: { reason: marker, idempotentKey: key } })
    assert.equal(sql(`SELECT match_status FROM psi_bank_statement WHERE id=${matched}`), 'UNMATCHED')
    const other = ids.find(id => id !== matched)
    await api('/psi/bank/match', { method: 'POST', data: { statementId: matched, matchBizType: 'RECEIPT', matchBizId: receipt } })
    await api('/psi/bank/match', { method: 'POST', data: { statementId: other, matchBizType: 'RECEIPT', matchBizId: state.funds.receipts[1].id } })
    state.funds.bankInMapping = [{ id: matched, cash: receipt }, { id: other, cash: state.funds.receipts[1].id }]; save()
    return { statements: ids, concurrentSuccess: 1, matchedCashVoidDenied: true, unmatchReasonPersisted: true, repeatedUnmatchIdempotent: true, reconciledBothOriginals: true }
  })
}
async function cashReturns() {
  await test('R29-PAID-STOCK-RETURNS-AND-ALLOCATION-REVERSALS', async () => {
    const marker = state.prefix + '-资金全流程'
    for (const item of [...state.funds.receipts.map(row => ({ ...row, kind: 'receipt' })), ...state.funds.payments.map(row => ({ ...row, kind: 'payment' }))]) {
      const existing = listOf(await api('/psi/' + item.kind + '/' + item.id + '/write-offs'))
      for (const entry of existing) await api('/psi/' + item.kind + '/' + item.id + '/reverse', { method: 'POST', data: { writeOffId: String(entry.writeOffId), reason: marker } })
    }
    for (const [key, sale] of [...state.funds.saleIds.map((_, index) => ['fundSale' + index, true]), ['fundStockIn', false]]) {
      const origin = state.objects[key], detail = await api('/psi/stock-' + (sale ? 'out' : 'in') + '/' + origin.id), returnKey = key + 'Return'
      const payload = { returnType: sale ? 'SALE_RETURN' : 'PURCHASE_RETURN', returnDate: now(), warehouseId: state.objects.warehouse.id, unitId: state.objects.unit.id,
        [sale ? 'originStockOutId' : 'originStockInId']: origin.id, remark: marker,
        items: detail.items.map(item => ({ originItemId: String(item.id), goodsId: String(item.goodsId), quantity: item.quantity, taxInclusivePrice: item.taxInclusivePrice, taxRate: item.taxRate })) }
      const id = register(returnKey, 'psi_stock_return', idOf(await api('/psi/stock-return', { method: 'POST', data: payload })), marker, 'confirmed compensating return audit retained')
      await confirmOwned('/psi/stock-return', returnKey)
      origin.status = 'COMPENSATED'; state.objects[returnKey].status = 'AUDIT_RETAINED'; save()
      if (sale) state.funds.saleReturnIds.push(id); else state.funds.purchaseReturnId = id
      save()
    }
    assert.equal(ownStock(), 0)
    verifyFundOpenItems()
    return { saleReturns: state.funds.saleReturnIds, purchaseReturn: state.funds.purchaseReturnId, quantity: 0, openReceivable: 0, openPayable: 0, allocatedFundsReleasedForRefund: true }
  })
}
function verifyFundOpenItems(){
  assert.equal(Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_receivable WHERE id IN (${state.funds.receipts.map(x=>x.receivable).join(',')}) AND deleted=0`)),0)
  assert.equal(Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_payable WHERE id IN (${state.funds.payments.map(x=>x.payable).join(',')}) AND deleted=0`)),0)
}
async function cashReturnVerify(){
  await test('R29-PAID-STOCK-RETURNS-AND-ALLOCATION-REVERSALS',async()=>{
    assert.equal(state.funds.saleReturnIds.length,3);assert.ok(state.funds.purchaseReturnId)
    for(const id of [...state.funds.saleReturnIds,state.funds.purchaseReturnId])assert.equal((await api('/psi/stock-return/'+id)).docStatus,'CONFIRMED')
    assert.equal(ownStock(),0);verifyFundOpenItems()
    return {saleReturns:state.funds.saleReturnIds,purchaseReturn:state.funds.purchaseReturnId,quantity:0,openReceivable:0,openPayable:0,
      assertionScope:'exact fund-chain open IDs; independent pending EOMS purchase is not silently treated as this chain',additionalReturnWrites:false}
  })
}
async function cashRefunds() {
  await test('R29-CASH-REFUND-IDEMPOTENCE-REVERSAL-AND-VOID', async () => {
    const marker = state.prefix + '-资金全流程'
    state.funds.refundPayments ||= []; state.funds.refundReceipts ||= []; save()
    for (let index = 0; index < state.funds.receipts.length; index++) {
      const source = state.funds.receipts[index], returnId = state.funds.saleReturnIds[index]
      const request = { stockReturnId: returnId, amount: source.amount, reason: marker, idempotentKey: randomUUID() }
      if (index === 0) {
        await api('/psi/receipt/' + source.id + '/refund', { method: 'POST', data: { ...request, amount: source.amount + 1 }, reject: true })
      }
      const attempts = await Promise.all([1, 2].map(() => api('/psi/receipt/' + source.id + '/refund', { method: 'POST', data: request })))
      assert.equal(String(attempts[0]), String(attempts[1]))
      const id = register('fundRefundPayment' + index, 'psi_payment', idOf(attempts[0]), marker, 'true compensating refund or explicit refund reversal audit retained')
      state.funds.refundPayments.push({ id, sourceId: source.id, returnId, amount: source.amount }); save()
      await api('/psi/receipt/' + source.id + '/refund', { method: 'POST', data: { ...request, amount: 1, idempotentKey: randomUUID() }, reject: true })
      if (index === 2) {
        const key = randomUUID()
        await api('/psi/payment/' + id + '/refund/reverse', { method: 'POST', data: { reason: marker, idempotentKey: key } })
        await api('/psi/payment/' + id + '/refund/reverse', { method: 'POST', data: { reason: marker, idempotentKey: key } })
        assert.equal((await api('/psi/payment/' + id)).docStatus, 'VOID')
        const voidKey = randomUUID()
        await api('/psi/receipt/' + source.id + '/void', { method: 'POST', data: { reason: marker, idempotentKey: voidKey } })
        await api('/psi/receipt/' + source.id + '/void', { method: 'POST', data: { reason: marker, idempotentKey: voidKey } })
        assert.equal((await api('/psi/receipt/' + source.id)).docStatus, 'VOID')
        state.funds.thirdCashVoided = true; save()
      }
      state.objects['fundRefundPayment' + index].status = 'AUDIT_RETAINED'; state.objects['fundReceipt' + index].status = 'COMPENSATED'; save()
    }
    const source = state.funds.payments[0]
    const request = { stockReturnId: state.funds.purchaseReturnId, amount: 1000, reason: marker, idempotentKey: randomUUID() }
    const first = await api('/psi/payment/' + source.id + '/refund', { method: 'POST', data: request })
    const second = await api('/psi/payment/' + source.id + '/refund', { method: 'POST', data: request }); assert.equal(String(first), String(second))
    const id = register('fundRefundReceipt', 'psi_receipt', idOf(first), marker, 'full supplier refund audit retained')
    state.funds.refundReceipts.push({ id, sourceId: source.id, amount: 1000 }); state.objects.fundRefundReceipt.status = 'AUDIT_RETAINED'; state.objects.fundPayment.status = 'COMPENSATED'; save()
    const unit = state.objects.unit.id
    const incoming = Number(sql(`SELECT COALESCE(SUM(amount),0) FROM psi_receipt WHERE unit_id=${unit} AND doc_status='CONFIRMED' AND deleted=0`))
    const outgoing = Number(sql(`SELECT COALESCE(SUM(amount),0) FROM psi_payment WHERE unit_id=${unit} AND doc_status='CONFIRMED' AND deleted=0`))
    assert.equal(incoming, outgoing)
    const unallocated = Number(sql(`SELECT COALESCE(SUM(unallocated_amount),0) FROM psi_receipt WHERE unit_id=${unit} AND doc_status='CONFIRMED' AND deleted=0`))
      + Number(sql(`SELECT COALESCE(SUM(unallocated_amount),0) FROM psi_payment WHERE unit_id=${unit} AND doc_status='CONFIRMED' AND deleted=0`))
    assert.equal(unallocated, 0)
    state.funds.net = { incoming, outgoing, unallocated, quantity: ownStock() }; save()
    return { ...state.funds.net, overRefundDenied: true, sameKeyConcurrentRefundSingleCounterDocument: true, reverseRefundIdempotent: true, originalCashVoidIdempotent: true }
  })
}
async function cashBankFinish() {
  await test('R29-BANK-REFUND-RECONCILIATION-NET-ZERO', async () => {
    const marker = state.prefix + '-资金全流程'
    const pairs = [{ kind: 'PAYMENT', id: state.funds.payments[0].id, amount: 1000, direction: 'OUT' },
      ...state.funds.refundPayments.slice(0, 2).map(row => ({ kind: 'PAYMENT', id: row.id, amount: row.amount, direction: 'OUT' })),
      { kind: 'RECEIPT', id: state.funds.refundReceipts[0].id, amount: 1000, direction: 'IN' }]
    for (let index = 0; index < pairs.length; index++) {
      const pair = pairs[index], seq = state.prefix + '-CASH-' + index
      await api('/psi/bank/import', { method: 'POST', data: { accountId: state.funds.accountId,
        items: [{ stmtDate: now(), direction: pair.direction, amount: pair.amount, counterparty: marker, summary: marker, bankSeqNo: seq }] } })
      const id = sql(`SELECT id FROM psi_bank_statement WHERE account_id=${state.funds.accountId} AND bank_seq_no='${seq}' AND deleted=0`)
      register('fundBankCounter' + index, 'psi_bank_statement', id, marker, 'matched synthetic cash audit retained')
      await api('/psi/bank/match', { method: 'POST', data: { statementId: id, matchBizType: pair.kind, matchBizId: pair.id } })
      state.objects['fundBankCounter' + index].status = 'AUDIT_RETAINED'; save()
    }
    for (let index = 0; index < 2; index++) state.objects['fundBankIn' + index].status = 'AUDIT_RETAINED'
    assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_bank_statement WHERE account_id=${state.funds.accountId} AND match_status='UNMATCHED' AND deleted=0`)), 0)
    const net = Number(sql(`SELECT COALESCE(SUM(CASE WHEN direction='IN' THEN amount ELSE -amount END),0) FROM psi_bank_statement WHERE account_id=${state.funds.accountId} AND deleted=0`))
    assert.equal(net, 0); state.funds.bankNet = net; save()
    return { rows: 6, allMatched: true, bankNet: 0, realBankTransportInvoked: false }
  })
  await test('R29-VOID-CASH-REMOVES-REFUND-TODO', async () => {
    const returnId = state.funds.saleReturnIds[2]
    const count = Number(sql(`SELECT COUNT(*) FROM psi_todo WHERE biz_id=${returnId} AND todo_type='REFUND' AND done=0 AND deleted=0`))
    state.funds.voidTodoProbe = { returnId, thirdCashVoided: state.funds.thirdCashVoided, remainingOpenTodos: count }; save()
    assert.equal(count, 0, 'Voiding the original cash after reversing its refund must close inapplicable refund prompts')
    return state.funds.voidTodoProbe
  })
}
async function eomsPositiveSetup(resume=false) {
  await test('R29-EOMS-ENGINEER-REAL-STOCK-SETTLEMENT', async () => {
    const marker=state.prefix+'-维修正向闭环', goodsId=state.objects.goodsOther.id, warehouseId=state.objects.warehouseOther.id, unitId=state.objects.unit.id
    let id,settlement
    if(!resume){
    if (state.objects.eomsOrder?.id === state.objects.compatOrder?.id && state.objects.compatOrder?.status === 'CLEANED') {
      assert.equal(sql(`SELECT del_flag FROM eoms_repair_order WHERE id=${state.objects.eomsOrder.id}`), '2')
      state.objects.eomsOrder.status = 'CLEANED'; save()
    }
    assert.ok(!state.objects.eomsOrder || state.objects.eomsOrder.status === 'CLEANED', 'Single-use setup: resume registered objects explicitly')
    if (!state.objects.eomsPurchase) {
    assert.equal(Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE goods_id=${goodsId} AND warehouse_id=${warehouseId}`)),0)
    register('eomsPurchase','psi_stock_in',idOf(await api('/psi/stock-in',{method:'POST',data:{inType:'PURCHASE',inDate:now(),unitId,warehouseId,remark:marker,items:[{goodsId,quantity:4,taxInclusivePrice:50,taxRate:0}]}})),marker,'full purchase return after EOMS reversal')
    await confirmOwned('/psi/stock-in','eomsPurchase')
    } else {
      const purchase = await api('/psi/stock-in/' + state.objects.eomsPurchase.id)
      assert.equal(purchase.remark, marker); assert.equal(purchase.docStatus, 'CONFIRMED')
      assert.equal(Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE goods_id=${goodsId} AND warehouse_id=${warehouseId}`)),4)
    }
    const compat = state.objects.compatOrder?.status !== 'CLEANED' ? state.objects.compatOrder : null
    id=compat?.id || await createOwned('eomsOrder','/eoms/repair/order',{customerId:unitId,address:marker,remark:marker},'eoms_repair_order','remark',marker)
    if(compat)register('eomsOrder','eoms_repair_order',id,marker,'formal settlement reversal; retain linked quotation and order history')
    await api('/eoms/repair/order/submit/'+id,{method:'PUT',reject:true})
    await api('/eoms/repair/order/assign/'+id+'?engineerId='+identities.engineer.id,{method:'PUT'})
    await api('/eoms/engineer/task/'+id,{token:identities.other.token,reject:true})
    await api('/eoms/engineer/task/'+id,{token:identities.engineer.token})
    await api('/eoms/repair/order/start/'+id,{method:'PUT'})
    await api('/eoms/engineer/record',{method:'POST',token:identities.engineer.token,data:{repairOrderId:id,faultReason:marker,checkProcess:marker,solution:marker,result:marker}})
    register('eomsRecord','eoms_repair_record',sql(`SELECT id FROM eoms_repair_record WHERE repair_order_id=${id} AND del_flag='0'`),marker,'audit retained')
    await api('/eoms/repair/material',{method:'POST',data:{repairOrderId:id,materialId:goodsId,warehouseId,materialName:marker,quantity:2,salePrice:80,costPrice:999}})
    register('eomsMaterial','eoms_repair_material',sql(`SELECT id FROM eoms_repair_material WHERE repair_order_id=${id} AND del_flag='0'`),marker,'audit retained')
    await api('/eoms/repair/order/submit/'+id,{method:'PUT'})
    settlement=register('eomsSettlement','eoms_repair_settlement',sql(`SELECT id FROM eoms_repair_settlement WHERE repair_order_id=${id} AND status='DRAFT' AND del_flag='0'`),marker,'formal settlement reversal')
    await api('/eoms/repair/settlement',{method:'PUT',data:{id:settlement,serviceFee:40,laborAmount:20,laborCost:10,otherAmount:0,otherCost:0,remark:marker}})
    } else {
      id=state.objects.eomsOrder.id;settlement=state.objects.eomsSettlement.id
      assert.equal(sql(`SELECT status FROM eoms_repair_settlement WHERE id=${settlement}`),'DRAFT')
      assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_stock_out WHERE repair_order_id=${id} AND deleted=0`)),0)
      await api('/eoms/repair/material',{method:'PUT',data:{id:state.objects.eomsMaterial.id,repairOrderId:id,materialId:goodsId,warehouseId,materialName:marker,quantity:2,salePrice:80,costPrice:999}})
    }
    const outcomes=await Promise.allSettled([1,2].map(()=>api('/eoms/repair/settlement/confirm/'+settlement,{method:'PUT'})))
    state.eomsConfirmAttempts=outcomes.map(x=>({status:x.status,error:x.reason?.message}));save()
    if(outcomes.every(x=>x.status==='rejected'))throw new Error(outcomes.map(x=>x.reason.message).join(' | '))
    assert.equal(outcomes.filter(x=>x.status==='fulfilled').length,1)
    assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${id}`),'FINISHED')
    assert.equal(Number(sql(`SELECT total_income FROM eoms_repair_settlement WHERE id=${settlement}`)),220)
    assert.equal(Number(sql(`SELECT total_cost FROM eoms_repair_settlement WHERE id=${settlement}`)),110)
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_income WHERE repair_order_id=${id} AND del_flag='0'`)),0)
    assert.equal(Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE goods_id=${goodsId} AND warehouse_id=${warehouseId}`)),2)
    const receivable=sql(`SELECT id FROM eoms_receivable WHERE repair_order_id=${id} AND status<>'VOID' AND del_flag='0'`)
    state.eoms={orderId:id,settlementIds:[settlement],receivable,expectedAmount:220,expectedCost:110};save()
    return {...state.eoms,otherEngineerDenied:true,concurrentConfirmationSingleEffect:true,confirmationDoesNotBookIncome:true,remainingStock:2}
  })
}
async function eomsCashReverse(resume=false) {
  await test('R29-EOMS-PAYMENT-REVERSAL-AND-RECONFIRM',async()=>{
    const e=state.eoms, marker=state.prefix+'-维修正向闭环', id=e.orderId, settlement=e.settlementIds[0]
    assert.ok(e && !state.objects.eomsPayment,'Single-use payment phase; inspect any partial registered payment')
    if(!resume){
      e.paymentRequest={receivableId:e.receivable,amount:e.expectedAmount,paymentMethod:'CASH',paymentTime:now(),idempotentToken:randomUUID(),remark:marker};save()
      await api('/eoms/finance/receipt/verify',{method:'POST',data:e.paymentRequest})
    }
    const payment=register('eomsPayment','eoms_payment',sql(`SELECT id FROM eoms_payment WHERE receivable_id=${e.receivable} AND status='ACTIVE' AND del_flag='0'`),marker,'reversed audit retained')
    if(!resume){const retry=await api('/eoms/finance/receipt/verify',{method:'POST',data:e.paymentRequest,reject:true});assert.match(retry.msg,/重复|处理/)}
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_payment WHERE receivable_id=${e.receivable} AND status='ACTIVE' AND del_flag='0'`)),1)
    assert.equal(Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_income WHERE repair_order_id=${id} AND del_flag='0'`)),220)
    await api('/eoms/repair/settlement/reverse/'+settlement+'?reason='+encodeURIComponent(marker),{method:'PUT',reject:true})
    await api('/eoms/repair/settlement/'+settlement,{method:'DELETE',reject:true})
    await api('/eoms/finance/receipt/'+payment,{method:'DELETE'})
    await api('/eoms/finance/receipt/'+payment,{method:'DELETE'})
    assert.equal(sql(`SELECT status FROM eoms_payment WHERE id=${payment}`),'REVERSED')
    assert.equal(Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_income WHERE repair_order_id=${id} AND del_flag='0'`)),0)
    state.objects.eomsPayment.status='COMPENSATED';save()
    for (let round=0;round<2;round++) {
      const current=e.settlementIds[round]
      await api('/eoms/repair/settlement/reverse/'+current+'?reason='+encodeURIComponent(marker),{method:'PUT'})
      await api('/eoms/repair/settlement/reverse/'+current+'?reason='+encodeURIComponent(marker),{method:'PUT'})
      assert.equal(sql(`SELECT status FROM eoms_repair_settlement WHERE id=${current}`),'REVERSED')
      assert.equal(Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_cost WHERE repair_order_id=${id} AND del_flag='0'`)),0)
      assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${id}`),'WAIT_SETTLEMENT')
      assert.equal(Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE goods_id=${state.objects.goodsOther.id} AND warehouse_id=${state.objects.warehouseOther.id}`)),4)
      if(round===0){
        await api('/eoms/repair/order/submit/'+id,{method:'PUT'})
        const next=register('eomsSettlement2','eoms_repair_settlement',sql(`SELECT id FROM eoms_repair_settlement WHERE repair_order_id=${id} AND status='DRAFT' AND del_flag='0'`),marker,'formal settlement reversal')
        assert.notEqual(next,current);e.settlementIds.push(next);save()
        await api('/eoms/repair/settlement/confirm/'+next,{method:'PUT'})
        assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_stock_out WHERE repair_order_id=${id} AND doc_status='CONFIRMED' AND deleted=0`)),1)
      }
    }
    for(const key of ['eomsSettlement','eomsSettlement2','eomsOrder','eomsRecord','eomsMaterial'])state.objects[key].status='AUDIT_RETAINED'
    if(joint && state.objects.compatOrder)state.objects.compatOrder.status='AUDIT_RETAINED'
    const origin=await api('/psi/stock-in/'+state.objects.eomsPurchase.id)
    register('eomsPurchaseReturn','psi_stock_return',idOf(await api('/psi/stock-return',{method:'POST',data:{returnType:'PURCHASE_RETURN',returnDate:now(),unitId:state.objects.unit.id,warehouseId:state.objects.warehouseOther.id,originStockInId:origin.id,remark:marker,items:origin.items.map(row=>({originItemId:String(row.id),goodsId:String(row.goodsId),quantity:row.quantity,taxInclusivePrice:row.taxInclusivePrice,taxRate:row.taxRate}))}})),marker,'confirmed compensating return audit retained')
    await confirmOwned('/psi/stock-return','eomsPurchaseReturn');state.objects.eomsPurchaseReturn.status='AUDIT_RETAINED';state.objects.eomsPurchase.status='COMPENSATED';save()
    assert.equal(Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE goods_id=${state.objects.goodsOther.id} AND warehouse_id=${state.objects.warehouseOther.id}`)),0)
    return {paymentReversed:true,netIncome:0,netCost:0,inventory:0,settlementHistory:e.settlementIds,activeStockOuts:0,rawSqlWrites:false}
  })
}
async function removeOwnFile(key) {
  const object = state.objects[key]
  if (!object || object.status === 'CLEANED') return
  const current = sql(`SELECT deleted FROM sys_file WHERE id=${object.id}`)
  if (current === '0') await api('/system/file', { method: 'DELETE', data: { ids: [object.id] } })
  if (sql(`SELECT deleted FROM sys_file WHERE id=${object.id}`) === '1') await api('/system/file/recycle/' + object.id, { method: 'DELETE' })
  assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_file WHERE id=${object.id} AND deleted IN (0,1)`)), 0)
  removed(key)
}

async function fileModule() {
  await test('R29-FILE-CONTENT-ACCESS-RECYCLE', async () => {
    const baseline = await api('/system/file/statistics')
    const ids = []
    try {
      const unsupported = new FormData(); unsupported.append('file', new Blob(['{}'], { type: 'application/json' }), state.prefix + '.json')
      assert.match((await api('/system/file/upload?parentPath=/', { method: 'POST', data: unsupported, reject: true })).msg, /不支持的文件类型/)
      for (const [extension, type, content] of [['txt', 'text/plain', state.prefix + ' synthetic bytes'], ['pdf', 'application/pdf', pdfFixture()]]) {
        const bytes = Buffer.from(content)
        const name = `${state.prefix}.${extension}`, form = new FormData()
        form.append('file', new Blob([bytes], { type }), name)
        const uploaded = await api('/system/file/upload?parentPath=/', { method: 'POST', data: form })
        const key = 'file_' + extension, id = register(key, 'file', uploaded.id, name, 'delete to recycle then exact recycle delete')
        ids.push(id)
        const metadata = await api('/system/common/file/' + id + '/info')
        assert.equal(metadata.originalName, name)
        assert.equal(Number(metadata.size), bytes.length)
        assert.equal(metadata.fileDigest.toLowerCase(), sm3([...bytes]))
        const stream = await fetch(base + '/system/common/file/' + id, { headers: { Authorization: 'Bearer ' + admin }, signal: AbortSignal.timeout(30000) })
        assert.equal(stream.status, 200); assert.deepEqual(Buffer.from(await stream.arrayBuffer()), bytes)
        await api('/system/common/file/' + id, { token: null, reject: true })
        await api('/system/common/file/' + id, { token: identities.other.token, reject: true })
      }
      const uploadedStats = await api('/system/file/statistics')
      assert.equal(Number(uploadedStats.number), Number(baseline.number ?? 0) + 2)
      await api('/system/file', { method: 'DELETE', data: { ids } })
      const recycle = listOf(await api('/system/file/recycle?originalName=' + state.prefix + '&page=1&size=100'))
      assert.ok(ids.every(id => recycle.some(row => String(row.id) === id)))
      await api('/system/file/recycle/restore/' + ids[0], { method: 'PUT' })
      assert.equal(sql(`SELECT deleted FROM sys_file WHERE id=${ids[0]}`), '0')
      await removeOwnFile('file_txt'); await removeOwnFile('file_pdf')
      const final = await api('/system/file/statistics')
      assert.equal(Number(final.number ?? 0), Number(baseline.number ?? 0)); assert.equal(Number(final.size ?? 0), Number(baseline.size ?? 0))
      return { ids, txtAndPdfExactBytes: true, unsupportedJsonRejected: true, sm3Matched: true, anonymousAndOtherUserDenied: true, recycleAndRestore: true,
        beforeCount: Number(baseline.number ?? 0), finalCount: Number(final.number ?? 0), statisticsWireType: typeof uploadedStats.number,
        cleanup: 'active/recycled rows absent; platform deletion audit retained', browserRetest: 'NOT_RUN' }
    } finally { await removeOwnFile('file_txt'); await removeOwnFile('file_pdf') }
  })
}

async function noticeModule() {
  await test('R29-NOTICE-DRAFT-CRUD', async () => {
    const marker = state.prefix + '-公告草稿'
    const types = listOf(await api('/system/common/dict/notice_type'))
    const type = String(types[0].value)
    const payload = { title: marker, content: marker + '仅测试接收者', type, noticeScope: 2,
      noticeUsers: [identities.engineer.id], noticeMethods: [], isTiming: false, isTop: false, status: 1 }
    const result = await api('/system/notice', { method: 'POST', data: payload })
    const id = register('noticeDraft', 'notice', idOf(result), marker, 'exact notice DELETE')
    try {
      const before = await api('/system/notice/' + id); assert.equal(Number(before.status), 1)
      await api('/system/notice/' + id, { method: 'PUT', data: { ...payload, title: marker + '-编辑' } })
      assert.equal((await api('/system/notice/' + id)).title, marker + '-编辑')
      assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_message WHERE path LIKE '%${id}%'`)), 0)
      return { noticeId: id, draftStatus: 1, edited: true, syntheticRecipientsOnly: true, messagesSent: 0 }
    } finally { await api('/system/notice', { method: 'DELETE', data: { ids: [id] } }); removed('noticeDraft') }
  })
  await test('R29-MESSAGE-OBJECT-DELETE-AUTHORIZATION', async () => {
    const marker = state.prefix + '-仅工程师消息'
    const types = listOf(await api('/system/common/dict/notice_type'))
    const result = await api('/system/notice', { method: 'POST', data: { title: marker, content: marker,
      type: String(types[0].value), noticeScope: 2, noticeUsers: [identities.engineer.id], noticeMethods: [1], isTiming: false, isTop: false, status: 3 } })
    const noticeId = register('noticeMessage', 'notice', idOf(result), marker, 'exact notice DELETE')
    try {
      const messageId = sql(`SELECT id FROM sys_message WHERE path LIKE '%${noticeId}%' AND content LIKE '%${state.prefix}%' AND deleted=0`)
      assert.match(messageId, /^\d+$/)
      register('messageExclusive', 'message', messageId, marker, 'only synthetic message, owned recipient cleanup')
      const otherInbox = listOf(await api('/user/message?page=1&size=100', { token: identities.other.token }))
      assert.ok(!otherInbox.some(row => String(row.id) === messageId))
      await api('/user/message/' + messageId, { token: identities.other.token, reject: true })
      await api('/user/message/' + messageId, { token: identities.engineer.token })
      const response = await fetch(base + '/user/message', { method: 'DELETE', headers: { Authorization: 'Bearer ' + identities.other.token, 'Content-Type': 'application/json' }, body: JSON.stringify({ ids: [messageId] }) })
      const deletion = await response.json()
      state.messageAuthorizationProbe = { noticeId, messageId, unauthorizedDeleteSuccess: deletion.success, code: deletion.code,
        activeRowsAfter: Number(sql(`SELECT COUNT(*) FROM sys_message WHERE id=${messageId} AND deleted=0`)), allObjectsSynthetic: true }; save()
      assert.equal(deletion.success, false, 'Non-recipient must not delete another user message')
      assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_message WHERE id=${messageId} AND deleted=0`)), 1)
      return state.messageAuthorizationProbe
    } finally {
      const message = state.objects.messageExclusive
      if (message && Number(sql(`SELECT COUNT(*) FROM sys_message WHERE id=${message.id} AND deleted=0`))) await api('/user/message', { method: 'DELETE', token: identities.engineer.token, data: { ids: [message.id] } })
      if (message) { message.status = 'AUDIT_RETAINED'; save() }
      await api('/system/notice', { method: 'DELETE', data: { ids: [noticeId] } }); removed('noticeMessage')
    }
  })
}


async function messageRetest() {
  await test('R29-MESSAGE-PRIVATE-DELETE-BATCH-CONCURRENT', async () => {
    const type = String(listOf(await api('/system/common/dict/notice_type'))[0].value)
    const created = []
    async function send(key, users) {
      const marker = state.prefix + '-' + key
      const result = await api('/system/notice', { method: 'POST', data: { title: marker, content: marker, type, noticeScope: 2, noticeUsers: users,
        noticeMethods: [1], isTiming: false, isTop: false, status: 3 } })
      const noticeId = register(key + 'Notice', 'notice', idOf(result), marker, 'exact notice DELETE')
      const id = sql(`SELECT id FROM sys_message WHERE path LIKE '%${noticeId}%' AND content LIKE '%${marker}%' AND deleted=0`)
      register(key, 'message', id, marker, 'personal deletion for every synthetic recipient; shared audit retained')
      created.push({ key, noticeId, id, users }); return id
    }
    const engineer = identities.engineer, other = identities.other
    try {
      const exclusive = await send('messagePrivate', [engineer.id]), shared = await send('messageShared', [engineer.id, other.id])
      const denied = await api('/user/message', { method: 'DELETE', token: other.token, data: { ids: [shared, exclusive] }, reject: true })
      assert.equal(String(denied.code), '403')
      assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_message_log WHERE message_id IN (${shared},${exclusive}) AND user_id=${other.id} AND is_deleted=1`)), 0)
      await api('/user/message/' + shared, { token: engineer.token })
      const beforeRead = sql(`SELECT read_time FROM sys_message_log WHERE message_id=${shared} AND user_id=${engineer.id}`)
      const outcomes = await Promise.allSettled([1, 2].map(() => api('/user/message', { method: 'DELETE', token: engineer.token, data: { ids: [shared, exclusive] } })))
      assert.equal(outcomes.filter(r => r.status === 'fulfilled').length, 2)
      await api('/user/message/read', { method: 'PATCH', token: engineer.token, data: { ids: [shared, exclusive] } })
      await api('/user/message/readAll', { method: 'PATCH', token: engineer.token })
      assert.equal(sql(`SELECT read_time FROM sys_message_log WHERE message_id=${shared} AND user_id=${engineer.id}`), beforeRead)
      assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_message_log WHERE message_id IN (${shared},${exclusive}) AND user_id=${engineer.id} AND is_deleted=1`)), 2)
      const inbox = listOf(await api('/user/message?page=1&size=100', { token: engineer.token })); assert.ok(!inbox.some(row => [shared, exclusive].includes(String(row.id))))
      await api('/user/message/' + shared, { token: engineer.token, reject: true })
      await api('/user/message/' + shared, { token: other.token })
      assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_message WHERE id IN (${shared},${exclusive}) AND deleted=0`)), 2)
      assert.equal(Number((await api('/user/message/unread', { token: engineer.token })).total), 0)
      return { exclusive, shared, mixedBatch403NoPartialWrite: true, duplicateConcurrentDelete: '2/2 success, one tombstone per user/message',
        readAndReadAllDoNotResurrect: true, readTimePreserved: true, otherRecipientUnaffected: true, inboxAndUnreadConsistent: true, sharedAuditRetained: true }
    } finally {
      for (const entry of created) {
        for (const user of entry.users) {
          const identity = Object.values(identities).find(value => value.id === user && value.kind !== 'h5')
          await api('/user/message', { method: 'DELETE', token: identity.token, data: { ids: [entry.id] } })
        }
        state.objects[entry.key].status = 'AUDIT_RETAINED'; save()
        await api('/system/notice', { method: 'DELETE', data: { ids: [entry.noticeId] } }); removed(entry.key + 'Notice')
      }
    }
  })
}
async function platformRead() {
  await test('R29-PLATFORM-ROUTES-OPTIONS-READ', async () => {
    const routes = await api('/auth/user/route'); assert.ok(Array.isArray(routes) && routes.length)
    const paths = ['/system/dept/tree', '/system/role/list', '/system/menu/tree', '/system/dict/list', '/monitor/online?page=1&size=10', '/system/log?page=1&size=10']
    for (const path of paths) { const data = await api(path); assert.ok(data && typeof data === 'object') }
    const flags = sql("SELECT code,value FROM sys_option WHERE code IN ('MODULE_OPEN_ENABLED','MODULE_SCHEDULE_ENABLED')")
    state.moduleFlags = flags.split('\n').filter(Boolean).map(row => { const [code, value] = row.split('\t'); return { code, value } }); save()
    const option = await api('/system/option?category=MODULE'); assert.ok(option)
    const open = await api('/open/app?page=1&size=10', { reject: true })
    const schedule = await api('/schedule/job?page=1&size=10', { reject: true })
    assert.match(open.msg, /禁用/); assert.match(schedule.msg, /禁用|未开启/)
    return { readPaths: paths, routeRoots: routes.length, moduleFlags: state.moduleFlags, openGate: open.msg, scheduleGate: schedule.msg,
      openAndScheduleApplicability: 'N/A for disabled current deployment; do not enable for test', tenantEnabled: state.tenantEnabled }
  })
  await test('R29-PC-H5-HTTP-ENTRY', async () => {
    const entries = []
    for (const port of [5173, 5174, 5175]) {
      const response = await fetch('http://127.0.0.1:' + port, { signal: AbortSignal.timeout(15000) })
      const html = await response.text(); assert.equal(response.status, 200); assert.match(html, /<html|<!doctype/i)
      entries.push({ port, status: response.status, bytes: Buffer.byteLength(html) })
    }
    return { entries, browserUI: 'NOT_RUN; API substitution explicitly authorized' }
  })
}
async function workbenchRead() {
  await test('R29-WORKBENCH-SOURCE-CONSISTENCY-READONLY', async () => {
    const durations = []; let summary
    for (let i = 0; i < 10; i++) { const start = performance.now(); summary = await api('/psi/workbench/summary'); durations.push(performance.now() - start) }
    const tenantId = sql(`SELECT tenant_id FROM psi_warehouse WHERE id=${state.objects.warehouse.id}`)
    assert.match(tenantId, /^\d+$/)
    let fields = 0
    for (const [table, condition, amount, countKey, amountKey] of [
      ['psi_receivable', "doc_status='OPEN' AND unwritten_amount>0", 'unwritten_amount', 'openReceivableCount', 'openReceivableAmount'],
      ['psi_payable', "doc_status='OPEN' AND unwritten_amount>0", 'unwritten_amount', 'openPayableCount', 'openPayableAmount'],
      ['psi_receipt', "doc_status='DRAFT'", 'amount', 'pendingReceiptCount', 'pendingReceiptAmount'],
      ['psi_bank_statement', "match_status='UNMATCHED'", 'amount', 'unmatchedBankCount', 'unmatchedBankAmount']
    ]) {
      const [count, sum] = sql(`SELECT COUNT(*),COALESCE(SUM(${amount}),0) FROM ${table} WHERE deleted=0 AND tenant_id=${tenantId} AND ${condition}`).split('\t')
      assert.equal(Number(summary[countKey]), Number(count), countKey); assert.equal(Number(summary[amountKey]), Number(sum), amountKey); fields += 2
    }
    const today = sql("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+08:00'))")
    assert.match(today, /^\d{4}-\d{2}-\d{2}$/)
    for (const [kind, monthly, recent] of [['out', 'monthSale', 'last30Out'], ['in', 'monthPurchase', 'last30In']]) {
      for (const [begin, prefix] of [[`DATE_FORMAT('${today}','%Y-%m-01')`, monthly], [`DATE_SUB('${today}',INTERVAL 29 DAY)`, recent]]) {
        const condition = `deleted=0 AND tenant_id=${tenantId} AND doc_status='CONFIRMED' AND confirm_time>=${begin} AND confirm_time<DATE_ADD('${today}',INTERVAL 1 DAY)`
        const [count, sum] = sql(`SELECT COUNT(*),COALESCE(SUM(tax_inclusive_amount),0) FROM psi_stock_${kind} WHERE ${condition}`).split('\t')
        assert.equal(Number(summary[prefix + 'Count']), Number(count), prefix + 'Count')
        assert.equal(Number(summary[prefix + 'Amount']), Number(sum), prefix + 'Amount')
        const qty = sql(`SELECT COALESCE(SUM(quantity),0) FROM psi_stock_${kind}_item WHERE deleted=0 AND tenant_id=${tenantId} AND stock_${kind}_id IN (SELECT id FROM psi_stock_${kind} WHERE ${condition})`)
        assert.equal(Number(summary[prefix + 'Qty']), Number(qty), prefix + 'Qty'); fields += 3
      }
    }
    const sorted = durations.toSorted((a, b) => a - b)
    return { fields, assertionSource: 'independent read-only source-table aggregate', samples: durations.length, serialConcurrency: 1,
      minMs: Math.round(sorted[0]), medianMs: Math.round(sorted[4]), p95Ms: Math.round(sorted[9]), performanceAcceptance: 'BLOCKED: no agreed latency/concurrency targets; measurement only',
      fullJointAcceptance: 'Evaluated separately from workbench sampling; see joint financial/H5/sign cases and final independent audit' }
  })
}

function sealPng() {
  function crc32(bytes) { let c = 0xffffffff; for (const b of bytes) { c ^= b; for (let i = 0; i < 8; i++) c = (c >>> 1) ^ ((c & 1) ? 0xedb88320 : 0) } return (c ^ 0xffffffff) >>> 0 }
  function chunk(type, bytes) { const content = Buffer.concat([Buffer.from(type), bytes]), len = Buffer.alloc(4), crc = Buffer.alloc(4); len.writeUInt32BE(bytes.length); crc.writeUInt32BE(crc32(content)); return Buffer.concat([len, content, crc]) }
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(64, 0); ihdr.writeUInt32BE(64, 4); ihdr[8] = 8; ihdr[9] = 6
  const pixels = Buffer.alloc(64 * 257)
  for (let y = 0; y < 64; y++) for (let x = 0; x < 64; x++) { const i = y * 257 + 1 + x * 4; pixels[i] = 255; pixels[i + 3] = 180 }
  return Buffer.concat([Buffer.from('89504e470d0a1a0a', 'hex'), chunk('IHDR', ihdr), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))])
}
async function signModule() {
  await test('R29-SIGN-SYNTHETIC-CONCURRENT-CRYPTOGRAPHY', async () => {
    const marker = state.prefix + '-自签测试', backend = 'ls-aegis-mcs-backend'
    await api('/sign/execute/projects', { token: null, reject: true })
    await api('/sign/execute/projects', { token: identities.limited.token, reject: true })
    const source = new FormData(); source.set('file', new Blob([pdfFixture()], { type: 'application/pdf' }), marker + '.pdf')
    const file = state.objects.signSource ? { id: state.objects.signSource.id } : await api('/system/common/file', { method: 'POST', data: source })
    if (!state.objects.signSource) register('signSource', 'sys_file', file.id, marker, 'sign source reference audit retained')
    const seal = new FormData(); seal.set('file', new Blob([sealPng()], { type: 'image/png' }), marker + '.png')
    seal.set('sealName', marker); seal.set('sealCategory', 'TEST'); seal.set('status', '0'); seal.set('deptId', '1')
    if (!state.objects.seal) await api('/sign/seal', { method: 'POST', data: seal })
    const sealId = state.objects.seal?.id || sql(`SELECT seal_id FROM sign_seal WHERE seal_name='${marker}' ORDER BY seal_id DESC LIMIT 1`)
    if (!state.objects.seal) register('seal', 'sign_seal', sealId, marker, 'disable test seal and revoke own authorizations; retain referenced audit')
    try {
      await api('/sign/seal', { method: 'PUT', data: { sealId, sealName: marker, sealCategory: 'TEST', status: '0', deptId: '1' } })
      await api('/sign/seal/' + sealId + '/auth/bind', { method: 'POST', data: ['1'] })
      if (!state.objects.signProject) await api('/sign/seal/bind-cert', { method: 'POST', data: { sealId } })
      if (!state.objects.signProject) await api('/sign/project', { method: 'POST', data: { projectName: marker, managerIds: '1', deptId: '1',
        startDate: '2020-01-01 00:00:00', endDate: '2099-12-31 23:59:59', otherFileIds: String(file.id), sealIds: [sealId], userIds: ['1'] } })
      const projectId = state.objects.signProject?.id || sql(`SELECT project_id FROM sign_project WHERE project_name='${marker}' ORDER BY project_id DESC LIMIT 1`)
      if (!state.objects.signProject) register('signProject', 'sign_project', projectId, marker, 'preserve signed project audit')
      if (String((await api('/sign/project/' + projectId)).status) === '0') await api('/sign/project/review/' + projectId + '?status=1', { method: 'PUT' })
      const payload = { projectId, sysFileId: String(file.id), sealId, signMode: 'single', useManagedKey: '1', hasTimestamp: '0', positions: [{ page: 1, x: 100, y: 100, size: 60 }] }
      await api('/sign/execute/prepare', { method: 'POST', data: payload })
      const attempts = await Promise.allSettled([1, 2].map(() => api('/sign/execute/run', { method: 'POST', data: payload })))
      const completed = attempts.filter(r => r.status === 'fulfilled'); assert.equal(completed.length, 1, 'Exactly one ordinary concurrent sign must succeed')
      const signed = completed[0].value
      register('signRecord', 'sign_record', signed.recordId, marker, 'immutable synthetic signature audit')
      register('signedFile', 'sign_file', signed.signedFileId, marker, 'immutable synthetic signature audit')
      const receipt = await api('/sign/execute/receipt/' + signed.recordId); assert.equal(receipt.verifyStatus, '1')
      const response = await fetch(base + '/sign/file/download/' + signed.signedFileId, { headers: { Authorization: 'Bearer ' + admin } })
      const pdf = Buffer.from(await response.arrayBuffer()); assert.equal(pdf.subarray(0, 4).toString(), '%PDF')
      writeFileSync(local('r29-signed.pdf'), pdf)
      const range = /\/ByteRange\s*\[\s*(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*\]/.exec(pdf.toString('latin1')); assert.ok(range)
      const [start, count, second, secondCount] = range.slice(1).map(Number); assert.equal(start, 0); assert.equal(second + secondCount, pdf.length)
      const padded = Buffer.from(pdf.subarray(count, second).toString('ascii').replace(/[<>\s]/g, ''), 'hex')
      const lengthBytes = padded[1] & 0x80 ? padded[1] & 0x7f : 0
      const contentLength = lengthBytes ? padded.readUIntBE(2, lengthBytes) : padded[1]
      writeFileSync(local('r29-signature.der'), padded.subarray(0, 2 + lengthBytes + contentLength))
      const content = Buffer.concat([pdf.subarray(start, count), pdf.subarray(second, second + secondCount)])
      writeFileSync(local('r29-sign-content.bin'), content); content[20] ^= 1; writeFileSync(local('r29-sign-tampered.bin'), content)
      for (const name of ['r29-signature.der', 'r29-sign-content.bin', 'r29-sign-tampered.bin']) assert.equal(spawnSync('docker', ['cp', local(name), backend + ':/tmp/' + name], { encoding: 'utf8' }).status, 0)
      const verify = name => spawnSync('docker', ['exec', backend, 'openssl', 'cms', '-verify', '-binary', '-inform', 'DER', '-in', '/tmp/r29-signature.der', '-content', '/tmp/' + name, '-noverify', '-out', '/tmp/r29-verified.bin'], { encoding: 'utf8' })
      assert.equal(verify('r29-sign-content.bin').status, 0); assert.notEqual(verify('r29-sign-tampered.bin').status, 0)
      const unifiedPayload = { projectId, sysFileId: String(file.id), sealId, generalSeals: [{ sealId, mode: 'single', positions: [{ page: 1, x: 100, y: 100, width: 60, height: 60 }] }] }
      const unified = await Promise.allSettled([1, 2].map(() => api('/sign/execute/unified', { method: 'POST', data: unifiedPayload })))
      const success = unified.filter(r => r.status === 'fulfilled'); assert.equal(success.length, 1)
      register('unifiedRecord', 'sign_record', success[0].value.recordId, marker, 'synthetic signature audit')
      register('unifiedFile', 'sign_file', success[0].value.signedFileId, marker, 'synthetic signature audit')
      assert.equal((await api('/sign/execute/receipt/' + success[0].value.recordId)).verifyStatus, '1')
      for (const key of ['signSource', 'signProject', 'signRecord', 'signedFile', 'unifiedRecord', 'unifiedFile']) state.objects[key].status = 'AUDIT_RETAINED'
      save()
      return { projectId, sealId, ordinaryConcurrentSuccess: 1, unifiedConcurrentSuccess: 1, verifyStatus: '1', opensslCmsVerified: true, tamperingRejected: true, signedBytes: pdf.length,
        externalCAOrTSA: false, trust: 'synthetic self-signed certificate; signature integrity verified without external chain trust' }
    } finally {
      await api('/sign/seal', { method: 'PUT', data: { sealId, sealName: marker, sealCategory: 'TEST', status: '1', deptId: '1' } })
      await api('/sign/seal/' + sealId + '/auth/1', { method: 'DELETE' })
      assert.equal(sql(`SELECT status FROM sign_seal WHERE seal_id=${sealId}`), '1')
      state.objects.seal.status = 'DISABLED_AUDIT_RETAINED'; save()
    }
  })
}
async function h5Auth() {
  if (!state.objects.unitOther) {
    const marker = state.prefix + '-其他客户'
    await createOwned('unitOther', '/psi/unit', { unitName: marker, unitType: 'CUSTOMER', unitNature: 'ENTERPRISE', status: 1 }, 'psi_unit', 'unit_name', marker)
  }
  for (const key of ['unit', 'unitOther']) {
    if(state.h5AuthVersion===2 && identities[key]?.token) {
      const check = await fetch(base + '/eoms/h5/repair/list?page=1&size=1', {
        headers: { 'h5-token': identities[key].token }, signal: AbortSignal.timeout(15000)
      })
      if ((await check.json()).success === true) continue
      identities[key].token = undefined; save()
    }
    await api('/eoms/admin/h5/auth/invitations',{method:'POST',token:identities.limited.token,data:{customerId:state.objects[key].id},reject:true})
    const invitation=await api('/eoms/admin/h5/auth/invitations',{method:'POST',data:{customerId:state.objects[key].id}})
    assert.equal(String(invitation.customerId),state.objects[key].id);assert.equal(Number(invitation.expiresInSeconds),600)
    assert.ok(invitation.invitationCode.length>=32)
    const challenge = await api('/eoms/h5/auth/captcha', { token: null })
    writeFileSync(local('r29-current-captcha.gif'), Buffer.from(challenge.img.replace(/^data:[^,]+,/, ''), 'base64'))
    assert.equal(spawnSync('python', [resolve(root, 'scripts/quality/render_public_captcha.py'), local('r29-current-captcha.gif'), local('r29-current-captcha.png')], { encoding: 'utf8' }).status, 0)
    const ticket = randomUUID()
    writeFileSync(local('r29-auth-prompt.json'), JSON.stringify({ ticket, role: key, phase: 'h5', expireTime: challenge.expireTime }))
    console.log('H5 CAPTCHA READY ' + key)
    let answer
    const deadline = Date.now() + 150000
    while (Date.now() < deadline) {
      if (existsSync(local('r29-auth-answer.json'))) {
        const packet = JSON.parse(readFileSync(local('r29-auth-answer.json'), 'utf8'))
        if (packet.ticket === ticket) { answer = packet.answer; unlinkSync(local('r29-auth-answer.json')); break }
      }
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    assert.ok(answer)
    const customerNo = sql(`SELECT unit_code FROM psi_unit WHERE id=${state.objects[key].id}`)
    await api('/eoms/h5/auth/token',{method:'POST',token:null,data:{customerNo,captcha:answer.trim(),uuid:challenge.uuid},reject:true})
    const data = await api('/eoms/h5/auth/token', { method: 'POST', token: null, data: { invitationCode:invitation.invitationCode, customerNo:'FORGED-IGNORED', captcha: answer.trim(), uuid: challenge.uuid } })
    assert.equal(String(data.customerId),state.objects[key].id)
    assert.equal(String(data.tenantId),sql(`SELECT tenant_id FROM psi_unit WHERE id=${state.objects[key].id}`))
    identities[key] = { id: state.objects[key].id, token: data.token, kind: 'h5' }; save()
    ;(state.h5AuthProofs ||= {})[key]={customerId:state.objects[key].id,tenantId:String(data.tenantId),customerNoWithCorrectCaptchaDenied:true,invitationIsRequired:true,limitedCannotIssue:true}
    console.log('H5 AUTH READY ' + key)
  }
  state.h5AuthVersion=2;save()
  await test('R29-H5-INVITATION-AUTHENTICATION',async()=>({version:2,proofs:state.h5AuthProofs,secretPrintedOrStoredInReport:false}))
}
async function uploadOwnH5Photo(key, headers) {
  const filename = state.prefix + '-' + key + '.png'
  const form = new FormData()
  form.set('file', new Blob([sealPng()], { type: 'image/png' }), filename)
  const uploaded = await api('/eoms/h5/repair/upload', { method: 'POST', token: null, headers, data: form })
  const id = String(uploaded.fileId || uploaded.fileName)
  assert.match(id, /^[1-9][0-9]*$/)
  register(key, 'sys_file', id, filename, 'delete after unused apply or retain linked repair audit')
  const response = await fetch(base + '/eoms/file/preview/' + id, { headers, signal: AbortSignal.timeout(30000) })
  assert.equal(response.status, 200, 'Authenticated customer can preview their upload before application submission')
  assert.match(response.headers.get('content-type') || '', /^image\//)
  assert.ok((await response.arrayBuffer()).byteLength > 100)
  return id
}
async function h5Module() {
  await test('R29-CUSTOMER-H5-OWNERSHIP-LOGOUT', async () => {
    const first = { 'h5-token': identities.unit.token }, second = { 'h5-token': identities.unitOther.token }
    const marker = state.prefix + '-H5报修'
    let id
    try {
      const photo = await uploadOwnH5Photo('h5Upload', first)
      const foreignPreview = await fetch(base + '/eoms/file/preview/' + photo, { headers: second, signal: AbortSignal.timeout(15000) })
      assert.equal(foreignPreview.status, 403, 'Other customer must not preview an unsubmitted foreign upload')
      await api('/eoms/h5/repair/apply', { method: 'POST', token: null, headers: first,
        data: { contactName: marker, contactPhone: '13800000000', faultType: 'OTHER', faultDesc: marker }, reject: true })
      const result = await api('/eoms/h5/repair/apply', { method: 'POST', token: null, headers: first,
        data: { customerId: state.objects.unitOther.id, tenantId: '999', deptId: '999', status: 'CONFIRM', delFlag: '2', photos: photo,
          contactName: marker, contactPhone: '13800000000', faultType: 'OTHER', faultDesc: marker, address: marker, remark: marker } })
      id = register('h5Apply', 'eoms_repair_apply', result.id, marker, 'admin exact apply DELETE')
      assert.equal(sql(`SELECT customer_id FROM eoms_repair_apply WHERE id=${id}`), identities.unit.id)
      assert.equal(sql(`SELECT status FROM eoms_repair_apply WHERE id=${id}`), 'WAIT')
      assert.equal(sql(`SELECT del_flag FROM eoms_repair_apply WHERE id=${id}`), '0')
      assert.equal(sql(`SELECT dept_id FROM eoms_repair_apply WHERE id=${id}`), sql(`SELECT dept_id FROM psi_unit WHERE id=${identities.unit.id}`))
      await api('/eoms/h5/repair/detail/' + id, { token: null, headers: first })
      await api('/eoms/h5/repair/detail/' + id, { token: null, headers: second, reject: true })
      await api('/eoms/h5/repair/detail/' + id, { token: null, reject: true })
      const ownList = listOf(await api('/eoms/h5/repair/list?page=1&size=100', { token: null, headers: first }))
      assert.ok(ownList.some(row => String(row.id) === id))
      const otherList = listOf(await api('/eoms/h5/repair/list?page=1&size=100', { token: null, headers: second }))
      assert.ok(!otherList.some(row => String(row.id) === id))
      await api('/eoms/h5/auth/logout', { method:'POST',token: null,headers:first })
      await api('/eoms/h5/repair/detail/' + id, { token: null, headers: first, reject: true })
      await api('/eoms/h5/auth/logout', { method:'POST',token: null,headers:second })
      const evidence = { applyId: id, forgedCustomerIgnored: true, crossCustomerDenied: true, listIsolation: true, logoutRevoked: true, captcha: 'normal public image challenge; no cache access', ui: 'NOT_RUN' }
      // Preserve completed business steps if a later cleanup operation fails.
      state.h5BusinessProof = { at: new Date().toISOString(), ...evidence }; save()
      return evidence
    } finally {
      if (id) { await api('/eoms/repair/apply/' + id, { method: 'DELETE' }); removed('h5Apply') }
      if (state.objects.h5Upload) await removeOwnFile('h5Upload')
    }
  })
}

async function quoteExports() {
  await test('R29-QUOTE-INDEPENDENT-AMOUNT-EXPORT', async () => {
    const marker = state.prefix + '-独立报价'
    const id = register('quote', 'eoms_quote', idOf(await api('/quote', { method: 'POST', data: { customerId: state.objects.unit.id,
      projectName: marker, taxRate: 0, details: [{ lineType: 'SERVICE', itemName: marker + '-服务', quantity: 2, price: 50 }] } })), marker, 'draft/submitted DELETE')
    try {
      const q = await api('/quote/' + id); assert.equal(q.status, 'DRAFT'); assert.equal(Number(q.totalAmount), 100)
      await api('/quote', { method: 'PUT', data: { id, status: 'SUBMIT' } })
      assert.equal((await api('/quote/' + id)).status, 'SUBMIT')
      const render = await api('/quote/' + id + '/render'); assert.ok(render.html.includes(marker))
      const sizes = {}
      for (const [suffix, magic] of [['docx', 'PK'], ['pdf', '%PDF']]) {
        const response = await fetch(base + '/quote/' + id + '/export-' + suffix, { headers: { Authorization: 'Bearer ' + admin }, signal: AbortSignal.timeout(60000) })
        const bytes = Buffer.from(await response.arrayBuffer()); assert.equal(response.status, 200)
        assert.equal(bytes.subarray(0, magic.length).toString(), magic, bytes.subarray(0, 1).toString() === '{' ? bytes.toString().slice(0, 600) : 'Export signature mismatch'); sizes[suffix] = bytes.length
        writeFileSync(local('r29-quote.' + suffix), bytes)
      }
      const word = spawnSync('python', ['-X', 'utf8', '-c', 'import zipfile,sys,xml.etree.ElementTree as E; print("".join(E.fromstring(zipfile.ZipFile(sys.argv[1]).read("word/document.xml")).itertext()))', local('r29-quote.docx')], { encoding: 'utf8' })
      assert.equal(word.status, 0); assert.ok(word.stdout.includes(marker) && word.stdout.includes('100'))
      await api('/quote', { method: 'PUT', data: { id, status: 'DRAFT' } })
      assert.equal((await api('/quote/' + id)).status, 'DRAFT')
      await api('/quote/' + id, { token: identities.limited.token, reject: true })
      return { id, amount: 100, independentWarehouseOptional: true, states: ['DRAFT', 'SUBMIT', 'DRAFT'], renderContent: true, wordContent: true, exportBytes: sizes, limitedDenied: true }
    } finally {
      await api('/quote/' + id, { method: 'DELETE' })
      assert.equal(sql(`SELECT del_flag FROM eoms_quote WHERE id=${id}`), '2'); removed('quote')
    }
  })
}
async function preflight() {
  await test('R29-PLATFORM-AUTH-READ', async () => {
    const info = await api('/auth/user/info',{token:admin})
    assert.equal(String(info.id), '1'); assert.equal(info.pwdExpired, false)
    const routes = await api('/auth/user/route'); assert.ok(routes.length)
    const tenantEnabled = await api('/system/common/dict/option/tenant')
    const roleList = listOf(await api('/system/role/list'))
    state.roles = roleList.map(row => ({ id: String(row.id), code: row.code, dataScope: row.dataScope }))
    state.tenantEnabled = tenantEnabled
    return { authenticatedId: '1', role: 'super_admin', expired: false, routeRoots: routes.length, effectiveTenantEnabled: tenantEnabled }
  })
}


async function applyOrderCleanup() {
  await test('R29-APPLY-ORDER-DELETE-RESTORES-STATE',async()=>{
    const marker=state.prefix+'-申请工单一致性'
    const applyId=await createOwned('applyOrderApply','/eoms/repair/apply',{customerId:state.objects.unit.id,faultDesc:marker,remark:marker},'eoms_repair_apply','remark',marker)
    await api('/eoms/repair/apply/confirm/'+applyId,{method:'PUT'})
    const outcomes=await Promise.allSettled([1,2].map(()=>api('/eoms/repair/apply/toOrder/'+applyId,{method:'PUT'})))
    assert.equal(outcomes.filter(x=>x.status==='fulfilled').length,1)
    const id=register('applyOrderOrder','eoms_repair_order',idOf(outcomes.find(x=>x.status==='fulfilled').value),marker,'unaccepted order DELETE')
    assert.equal(sql(`SELECT status FROM eoms_repair_apply WHERE id=${applyId}`),'ORDER')
    await api('/eoms/repair/apply/'+applyId,{method:'DELETE',reject:true})
    await api('/eoms/repair/order/'+id,{method:'DELETE'});removed('applyOrderOrder')
    assert.equal(sql(`SELECT status FROM eoms_repair_apply WHERE id=${applyId}`),'CONFIRM')
    assert.equal(sql(`SELECT apply_id FROM eoms_repair_order WHERE id=${id}`),applyId)
    await api('/eoms/repair/apply/'+applyId,{method:'DELETE'});removed('applyOrderApply')
    assert.equal(sql(`SELECT del_flag FROM eoms_repair_apply WHERE id=${applyId}`),'2')
    return {applyId,orderId:id,concurrentConversionOneOrder:true,liveOrderPreventsApplyDeletion:true,unacceptedOrderDeletionRestoresApply:true,historyLinkPreserved:true}
  })
}
async function invoicePositiveSetup() {
  await test('R29-INVOICE-CONCURRENT-CAP-AND-SOURCE-UNIT',async()=>{
    assert.ok(!state.objects.invoiceOut0,'Single-use invoice setup; inspect existing registered drafts first')
    const marker=state.prefix+'-合成发票台账', unitId=state.objects.unit.id
    state.invoice={blues:[]};save()
    const request=(no,stockDocId,amount,unit=unitId)=>({invoiceNo:state.prefix+'-'+no,invoiceType:'NORMAL',invoiceDate:now(),unitId:unit,
      buyerTaxNo:'TEST-BUYER',sellerTaxNo:'TEST-SELLER',taxInclusiveAmount:amount,untaxAmount:amount,taxAmount:0,remark:marker,links:[{stockDocId,amount}],items:[]})
    const source=state.objects.fundSale0.id
    for(let index=0;index<2;index++)register('invoiceOut'+index,'psi_invoice_out',idOf(await api('/psi/invoice-out',{method:'POST',data:request('OUT-'+index,source,300)})),marker,'draft DELETE or formal red counter audit')
    const outcomes=await Promise.allSettled([0,1].map(index=>confirmOwned('/psi/invoice-out','invoiceOut'+index)))
    state.invoice.confirmOutcomes=outcomes.map(x=>({status:x.status,error:x.reason?.message}));save()
    assert.equal(outcomes.filter(x=>x.status==='fulfilled').length,1,'Two full invoices for the same stock source must not both confirm')
    for(let index=0;index<2;index++){
      const key='invoiceOut'+index,entry=state.objects[key]
      if(outcomes[index].status==='fulfilled')state.invoice.blues.push({key,side:'out',id:entry.id,stockId:source,amount:300})
      else {assert.equal((await api('/psi/invoice-out/'+entry.id)).docStatus,'DRAFT');await api('/psi/invoice-out/'+entry.id,{method:'DELETE'});removed(key)}
    }
    const invalid=register('invoiceWrongUnit','psi_invoice_out',idOf(await api('/psi/invoice-out',{method:'POST',data:request('WRONG-UNIT',state.objects.fundSale1.id,300,state.objects.unitOther.id)})),marker,'draft DELETE')
    const denial=await api('/psi/invoice-out/'+invalid+'/confirm',{method:'POST',data:{idempotentKey:randomUUID()},reject:true});assert.match(denial.msg,/单位|客户/)
    await api('/psi/invoice-out/'+invalid,{method:'DELETE'});removed('invoiceWrongUnit')
    const inId=register('invoiceIn','psi_invoice_in',idOf(await api('/psi/invoice-in',{method:'POST',data:request('IN',state.objects.fundStockIn.id,1000)})),marker,'formal red counter audit')
    await confirmOwned('/psi/invoice-in','invoiceIn');await confirmOwned('/psi/invoice-in','invoiceIn')
    state.invoice.blues.push({key:'invoiceIn',side:'in',id:inId,stockId:state.objects.fundStockIn.id,amount:1000});save()
    return {concurrentFullInvoiceSuccess:1,oppositeAttemptDraftRolledBack:true,wrongUnitDenied:true,invoiceIds:state.invoice.blues.map(x=>x.id),realTaxPlatformCalled:false}
  })
}
async function invoiceRedCleanup() {
  const returnedBeforeInvoice = process.argv.includes('--returned-before-invoice')
  await test(returnedBeforeInvoice ? 'R29-INVOICE-RED-AFTER-EARLIER-RETURN' : 'R29-INVOICE-RED-IDEMPOTENCE-AND-RETURN-TODO',async()=>{
    if (returnedBeforeInvoice) {
      const returned = state.results.find(r => r.id.endsWith('-PAID-STOCK-RETURNS-AND-ALLOCATION-REVERSALS') && r.status === 'PASS')
      const invoiced = state.results.find(r => r.id.endsWith('-INVOICE-CONCURRENT-CAP-AND-SOURCE-UNIT') && r.status === 'PASS')
      assert.ok(returned && invoiced && returned.at < invoiced.at, 'Require recorded return completion before invoice creation')
    }
    assert.equal(state.invoice.blues.length,2)
    const evidence=[]
    for(const blue of state.invoice.blues){
      const marker=state.objects[blue.key].marker,key=randomUUID(),path='/psi/invoice-'+blue.side+'/'+blue.id+'/red'
      const returned=blue.side==='out'?state.funds.saleReturnIds[0]:state.funds.purchaseReturnId
      assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_todo WHERE biz_id=${returned} AND todo_type='RED_INVOICE' AND done=0 AND deleted=0`)),returnedBeforeInvoice ? 0 : 1,'Return reminder must match the actual invoice/return chronology')
      const first=await api(path,{method:'POST',data:{idempotentKey:key}})
      assert.equal(await api(path,{method:'POST',data:{idempotentKey:key}}),first)
      assert.equal(await api(path,{method:'POST',data:{idempotentKey:randomUUID()}}),first)
      const red=register(blue.key+'Red','psi_invoice_'+blue.side,sql(`SELECT id FROM psi_invoice_${blue.side} WHERE red_of_id=${blue.id} AND deleted=0`),marker,'immutable synthetic red invoice audit')
      const redDoc=await api('/psi/invoice-'+blue.side+'/'+red);assert.equal(Number(redDoc.taxInclusiveAmount),-blue.amount)
      await api('/psi/invoice-'+blue.side+'/'+red+'/red',{method:'POST',data:{idempotentKey:randomUUID()},reject:true})
      assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_todo WHERE biz_id=${returned} AND todo_type='RED_INVOICE' AND done=0 AND deleted=0`)),0)
      assert.equal(sql(`SELECT invoice_status FROM psi_stock_${blue.side} WHERE id=${blue.stockId}`),'UNINVOICED')
      state.objects[blue.key].status='COMPENSATED';state.objects[blue.key+'Red'].status='AUDIT_RETAINED';save();evidence.push({blueId:blue.id,redId:red,amount:blue.amount})
    }
    return {evidence,oneRedPerBlue:true,sameAndNewKeyReplaySafe:true,redOfRedDenied:true,openRedTodos:0,externalTaxEffect:false}
  })
}
async function quoteCompatibility() {
  await test('R29-QUOTE-COMPATIBILITY-ORDER-SINGLE-EFFECT',async()=>{
    assert.ok(!state.objects.compatQuote && (!joint || !state.objects.eomsOrder),'Fresh compatibility fixture required')
    const marker=state.prefix+'-报价兼容联动',unitId=state.objects.unit.id
    const id=register('compatQuote','eoms_quote',idOf(await api('/quote',{method:'POST',data:{customerId:unitId,projectName:marker,taxRate:0,
      details:[{lineType:'MATERIAL',goodsId:state.objects.goodsOther.id,itemName:marker+'材料',quantity:2,price:80},{lineType:'SERVICE',itemName:marker+'服务',quantity:1,price:60}]}})),marker,'confirmed quotation/order reference audit retained')
    assert.equal(Number((await api('/quote/'+id)).totalAmount),220)
    const path='/quote/'+id+'/to-order?address='+encodeURIComponent(marker)+'&remark='+encodeURIComponent(marker)
    const pair=await Promise.all([1,2].map(()=>api(path,{method:'PUT'})));assert.equal(String(pair[0]),String(pair[1]))
    const order=register('compatOrder','eoms_repair_order',idOf(pair[0]),marker,'use same order through actual EOMS settlement and formal reversal')
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_order WHERE quote_id=${id} AND del_flag='0'`)),1)
    await api('/quote',{method:'PUT',data:{id,totalAmount:1,taxRate:99},reject:true})
    await api('/quote/'+id,{method:'DELETE',reject:true})
    state.objects.compatQuote.status='AUDIT_RETAINED';save()
    if(!joint){await api('/eoms/repair/order/'+order,{method:'DELETE'});removed('compatOrder')}
    return {quoteId:id,orderId:order,sameCustomer:unitId,sameGoods:state.objects.goodsOther.id,total:220,concurrentOneOrder:true,terminalMutationDenied:true,compatibilityOptional:true}
  })
}
async function cancelOwnedEoms(){
  await test('R29-EOMS-REVERSED-ORDER-TERMINAL-CLEANUP',async()=>{
    assert.ok(state.eoms?.orderId)
    const id=state.eoms.orderId,reason=state.prefix+'-全部结算及收付款已冲销，结束合成测试工单'
    await api('/eoms/repair/order/cancel/'+id+'?reason='+encodeURIComponent(reason),{method:'PUT'})
    await api('/eoms/repair/order/cancel/'+id+'?reason='+encodeURIComponent(reason),{method:'PUT'})
    assert.equal((await api('/eoms/repair/order/'+id)).status,'CLOSED')
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_settlement WHERE repair_order_id=${id} AND status IN ('DRAFT','CONFIRMED') AND del_flag='0'`)),0)
    state.eoms.closed=true;state.objects.eomsOrder.status='AUDIT_RETAINED';save()
    return {orderId:id,status:'CLOSED',reasonRecorded:true,duplicateCloseSingleEffect:true,noNewSettlementOrAccounting:true}
  })
}
async function financeRoleFlows(){
  await test('R29-REAL-ROLES-ZERO-AND-PAID-FINANCE',async()=>{
    const deployment=deploymentEvidence(resolve(root,'.local/r29-deployment-v5.json'));assert.equal(deployment.ready,true)
    const manager=identities.manager.token,finance=identities.finance.token,engineer=identities.engineer.token
    await api('/psi/unit?page=1&size=1',{token:manager,reject:true});await api('/quote?page=1&size=1',{token:manager,reject:true})
    const evidence=[]
    for(const amount of [0,20]){
      const prefix='role'+amount,marker=state.prefix+'-现有岗位-'+amount,unitId=state.objects.unit.id
      assert.ok(!state.objects[prefix+'Order'],'Role case is single-use; resume registered records explicitly')
      await api('/eoms/repair/order',{method:'POST',token:manager,data:{customerId:unitId,address:marker,remark:marker}})
      const id=register(prefix+'Order','eoms_repair_order',sql(`SELECT id FROM eoms_repair_order WHERE remark='${marker}' AND del_flag='0'`),marker,'formal reverse then cancellation closure')
      await api('/eoms/repair/order/assign/'+id+'?engineerId='+identities.finance.id,{method:'PUT',token:manager,reject:true})
      assert.equal((await api('/eoms/repair/order/'+id,{token:manager})).status,'WAIT_ACCEPT')
      await api('/eoms/repair/order/assign/'+id+'?engineerId='+identities.engineer.id,{method:'PUT',token:manager})
      await api('/eoms/engineer/task/'+id,{token:identities.other.token,reject:true})
      await api('/eoms/repair/order/start/'+id,{method:'PUT',token:identities.other.token,reject:true})
      assert.equal((await api('/eoms/repair/order/'+id,{token:manager})).status,'ACCEPTED')
      await api('/eoms/repair/order/start/'+id,{method:'PUT',token:engineer})
      await api('/eoms/engineer/record',{method:'POST',token:engineer,data:{repairOrderId:id,faultReason:marker,checkProcess:marker,solution:marker,result:marker}})
      register(prefix+'Record','eoms_repair_record',sql(`SELECT id FROM eoms_repair_record WHERE repair_order_id=${id} AND del_flag='0'`),marker,'terminal order record audit retained')
      await api('/eoms/repair/order/submit/'+id,{method:'PUT',token:manager})
      const settlement=register(prefix+'Settlement','eoms_repair_settlement',sql(`SELECT id FROM eoms_repair_settlement WHERE repair_order_id=${id} AND status='DRAFT' AND del_flag='0'`),marker,'formal settlement reversal')
      await api('/eoms/repair/settlement',{method:'PUT',token:finance,data:{id:settlement,serviceFee:amount,laborAmount:0,otherAmount:0,laborCost:0,otherCost:0,remark:marker}})
      await api('/eoms/repair/settlement/confirm/'+settlement,{method:'PUT',token:finance})
      const receivable=register(prefix+'Receivable','eoms_receivable',sql(`SELECT id FROM eoms_receivable WHERE repair_order_id=${id} AND status<>'VOID' AND del_flag='0'`),marker,'VOID receivable audit retained')
      const open=await api('/eoms/finance/receivable/'+receivable,{token:finance})
      assert.equal(open.status,amount===0?'PAID':'UNPAID');assert.equal(Number(open.arrearsAmount),amount)
      const unsettled=listOf(await api('/eoms/finance/receivable/unsettled?customerId='+unitId,{token:finance}))
      assert.equal(unsettled.some(row=>String(row.id)===receivable),amount>0)
      await api('/eoms/finance/receipt/verify',{method:'POST',token:engineer,data:{receivableId:receivable,amount:1,paymentMethod:'CASH',paymentTime:now(),idempotentToken:randomUUID()},reject:true})
      if(amount===0){
        await api('/eoms/finance/receipt/verify',{method:'POST',token:finance,data:{receivableId:receivable,amount:0,paymentMethod:'CASH',paymentTime:now(),idempotentToken:randomUUID()},reject:true})
      }else{
        const request={receivableId:receivable,amount,paymentMethod:'CASH',paymentTime:now(),idempotentToken:randomUUID(),remark:marker}
        await api('/eoms/finance/receipt/verify',{method:'POST',token:finance,data:request})
        const payment=register(prefix+'Payment','eoms_payment',sql(`SELECT id FROM eoms_payment WHERE receivable_id=${receivable} AND status='ACTIVE' AND del_flag='0'`),marker,'reversed receipt audit retained')
        await api('/eoms/finance/receipt/verify',{method:'POST',token:finance,data:request,reject:true})
        assert.equal(Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_income WHERE repair_order_id=${id} AND del_flag='0'`)),amount)
        await api('/eoms/finance/receipt/'+payment,{method:'DELETE',token:finance});state.objects[prefix+'Payment'].status='COMPENSATED';save()
      }
      await api('/eoms/repair/settlement/reverse/'+settlement+'?reason='+encodeURIComponent(marker),{method:'PUT',token:finance})
      await api('/eoms/repair/order/cancel/'+id+'?reason='+encodeURIComponent(marker),{method:'PUT',token:finance,reject:true})
      await api('/eoms/repair/order/cancel/'+id+'?reason='+encodeURIComponent(marker),{method:'PUT',token:manager})
      assert.equal((await api('/eoms/repair/order/'+id,{token:manager})).status,'CLOSED')
      assert.equal(Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_income WHERE repair_order_id=${id} AND del_flag='0'`)),0)
      for(const suffix of ['Order','Record','Settlement','Receivable'])state.objects[prefix+suffix].status='AUDIT_RETAINED';save()
      evidence.push({orderId:id,settlementId:settlement,receivableId:receivable,amount,zeroNotUnsettled:amount===0,status:'CLOSED',netIncome:0})
    }
    return {evidence,existingRoleDefinitionsChanged:false,extraGrants:false,managerDoesNotHavePsiOrQuote:true,financeCannotCloseOrder:true,engineerCannotCollectMoney:true,department:state.objects.deptA.id}
  })
}
async function h5SessionControls(){
  await test('R29-H5-SESSION-HANDLE-NOT-BEARER-AND-KICKOUT',async()=>{
    const rows=listOf(await api('/eoms/admin/h5/auth/online?page=1&size=100'))
    assert.ok(rows.every(row=>!Object.hasOwn(row,'token')),'Management list must not disclose bearer tokens')
    const mine=rows.filter(row=>String(row.customerId)===state.objects.unitOther.id)
    assert.equal(mine.length,1);assert.ok(mine[0].sessionId)
    await api('/eoms/h5/repair/list?page=1&size=10',{token:null,headers:{'h5-token':mine[0].sessionId},reject:true})
    await api('/eoms/admin/h5/auth/online/'+encodeURIComponent(mine[0].sessionId),{method:'DELETE'})
    await api('/eoms/h5/repair/list?page=1&size=10',{token:null,headers:{'h5-token':identities.unitOther.token},reject:true})
    identities.unitOther.token=undefined;save()
    return {customerId:state.objects.unitOther.id,managementContainsNoBearer:true,handleCannotLogin:true,authorizedKickoutRevokes:true,needsFreshOwnInvitationForLaterOwnershipTests:true}
  })
}
async function h5JointFlow(){
  await test('R29-H5-APPLY-ENGINEER-CUSTOMER-CONFIRM-FINANCE-JOINT',async()=>{
    assert.equal(joint,true);assert.ok(!state.objects.h5JointOrder,'Single-use fresh joint branch')
    const marker=state.prefix+'-客户工程师完整联动',unitId=state.objects.unit.id,goodsId=state.objects.goodsOther.id,warehouseId=state.objects.warehouseOther.id
    const first={'h5-token':identities.unit.token},second={'h5-token':identities.unitOther.token},engineer=identities.engineer.token
    if (!state.objects.h5JointPurchase) {
      assert.equal(Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE goods_id=${goodsId} AND warehouse_id=${warehouseId}`)),0)
      register('h5JointPurchase','psi_stock_in',idOf(await api('/psi/stock-in',{method:'POST',data:{inType:'PURCHASE',inDate:now(),unitId,warehouseId,remark:marker,items:[{goodsId,quantity:2,taxInclusivePrice:50,taxRate:0}]}})),marker,'full return after repair reversal')
      await confirmOwned('/psi/stock-in','h5JointPurchase')
    } else {
      // Resume only the exact confirmed prerequisite after a pre-application upload failure.
      const prior = await api('/psi/stock-in/' + state.objects.h5JointPurchase.id)
      assert.equal(prior.remark, marker); assert.equal(String(prior.unitId), unitId)
      assert.equal(String(prior.warehouseId), warehouseId); assert.equal(prior.docStatus, 'CONFIRMED')
      assert.equal(prior.items.length, 1); assert.equal(String(prior.items[0].goodsId), goodsId)
      assert.equal(Number(prior.items[0].quantity), 2)
      assert.equal(Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE goods_id=${goodsId} AND warehouse_id=${warehouseId}`)),2)
    }
    const photo=state.objects.h5JointPhoto?.id || await uploadOwnH5Photo('h5JointPhoto',first)
    const deniedPreview=await fetch(base+'/eoms/file/preview/'+photo,{headers:second,signal:AbortSignal.timeout(30000)})
    if (deniedPreview.status === 200) assert.equal((await deniedPreview.json()).success,false)
    else assert.ok([401,403,404].includes(deniedPreview.status),'Cross-customer binary preview must be denied')
    for (const coords of [{longitude:'0'}, {longitude:'181',latitude:'0'}, {longitude:'NaN',latitude:'0'}]) {
      const form = new FormData()
      form.set('file',new Blob([sealPng()],{type:'image/png'}),state.prefix+'-invalid-coordinates.png')
      for (const [name,value] of Object.entries(coords)) form.set(name,value)
      await api('/eoms/h5/repair/upload',{method:'POST',token:null,headers:first,data:form,reject:true})
    }
    const apply=register('h5JointApply','eoms_repair_apply',idOf(await api('/eoms/h5/repair/apply',{method:'POST',token:null,headers:first,data:{customerId:state.objects.unitOther.id,tenantId:'999',photos:photo,contactName:marker,contactPhone:'13800000000',faultType:'OTHER',faultDesc:marker,address:marker,remark:marker}})),marker,'closed linked repair history retained')
    assert.equal(sql(`SELECT customer_id FROM eoms_repair_apply WHERE id=${apply}`),unitId)
    assert.equal(sql(`SELECT tenant_id FROM eoms_repair_apply WHERE id=${apply}`),sql(`SELECT tenant_id FROM psi_unit WHERE id=${unitId}`))
    await api('/eoms/repair/apply/confirm/'+apply,{method:'PUT'})
    const order=register('h5JointOrder','eoms_repair_order',idOf(await api('/eoms/repair/apply/toOrder/'+apply,{method:'PUT'})),marker,'formal reversal then terminal cancellation')
    await api('/eoms/h5/repair/confirm',{method:'POST',token:null,headers:first,data:{orderId:order,remark:marker},reject:true})
    await api('/eoms/repair/order/assign/'+order+'?engineerId='+identities.engineer.id,{method:'PUT'})
    await api('/eoms/engineer/start/'+order,{method:'POST',token:identities.other.token,reject:true})
    await api('/eoms/engineer/start/'+order,{method:'POST',token:engineer,reject:true})
    for (const query of ['longitude=NaN&latitude=0','longitude=181&latitude=0','longitude=0&latitude=-91','longitude=0']) {
      const before = sql(`SELECT status FROM eoms_repair_order WHERE id=${order}`)
      await api('/eoms/engineer/start/'+order+'?'+query,{method:'POST',token:engineer,reject:true})
      assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${order}`),before)
    }
    const geoConfigured=Number(sql("SELECT COALESCE(MAX(CHAR_LENGTH(value)),0)>0 FROM sys_option WHERE code='GEO_BAIDU_AK'"))
    const coords='?longitude=0&latitude=0'
    if(geoConfigured===0)await api('/eoms/engineer/start/'+order+coords,{method:'POST',token:engineer})
    else await api('/eoms/repair/order/start/'+order,{method:'PUT'})
    const engineerPhotos = {}
    const sourceResponse = await fetch(base+'/eoms/file/preview/'+photo,{headers:first,signal:AbortSignal.timeout(30000)})
    assert.equal(sourceResponse.status,200)
    const jpeg = Buffer.from(await sourceResponse.arrayBuffer())
    assert.equal(jpeg.subarray(0,2).toString('hex'),'ffd8','Watermarked customer PNG must be served as actual JPEG')
    writeFileSync(local('r29-h5-watermark.jpg'),jpeg)
    for (const type of ['processing','completed']) {
      const form = new FormData(), key='h5JointEngineer'+type
      const filename=state.prefix+'-'+type+'.jpg'
      form.set('file',new Blob([jpeg],{type:'image/jpeg'}),filename)
      form.set('orderId',order);form.set('photoType',type)
      const uploaded=await api('/eoms/engineer/upload',{method:'POST',token:engineer,data:form})
      const fileId=register(key,'sys_file',uploaded.fileId||uploaded.fileName,filename,'retain linked repair photo audit')
      engineerPhotos[type]=fileId
      const preview=await fetch(base+'/eoms/file/preview/'+fileId,{headers:{Authorization:'Bearer '+engineer},signal:AbortSignal.timeout(30000)})
      assert.equal(preview.status,200);assert.equal(Buffer.from(await preview.arrayBuffer()).subarray(0,2).toString('hex'),'ffd8')
      await api('/eoms/engineer/record',{method:'POST',token:identities.other.token,data:{repairOrderId:order,processingPhotos:fileId},reject:true})
    }
    await api('/eoms/engineer/record',{method:'POST',token:engineer,data:{repairOrderId:order,faultReason:marker,checkProcess:marker,solution:marker,result:marker,processingPhotos:engineerPhotos.processing,completedPhotos:engineerPhotos.completed}})
    register('h5JointRecord','eoms_repair_record',sql(`SELECT id FROM eoms_repair_record WHERE repair_order_id=${order} AND del_flag='0'`),marker,'terminal repair history retained')
    await api('/eoms/repair/material',{method:'POST',data:{repairOrderId:order,materialId:goodsId,warehouseId,materialName:marker,quantity:1,salePrice:80,costPrice:999}})
    register('h5JointMaterial','eoms_repair_material',sql(`SELECT id FROM eoms_repair_material WHERE repair_order_id=${order} AND del_flag='0'`),marker,'terminal material audit retained')
    for (const query of ['longitude=Infinity&latitude=0','longitude=-181&latitude=0','longitude=0&latitude=91','latitude=0']) {
      const before = sql(`SELECT status FROM eoms_repair_order WHERE id=${order}`)
      await api('/eoms/engineer/finish/'+order+'?'+query,{method:'POST',token:engineer,reject:true})
      assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${order}`),before)
    }
    if(geoConfigured===0)await api('/eoms/engineer/finish/'+order+coords,{method:'POST',token:engineer})
    else await api('/eoms/repair/order/submit/'+order,{method:'PUT'})
    await api('/eoms/h5/repair/confirm',{method:'POST',token:null,headers:second,data:{orderId:order,customerId:unitId,remark:marker},reject:true})
    const confirmed=await Promise.all([1,2].map(()=>api('/eoms/h5/repair/confirm',{method:'POST',token:null,headers:first,data:{orderId:order,customerId:state.objects.unitOther.id,tenantId:'999',remark:marker}})))
    assert.ok(confirmed.every(x=>x.confirmed===true));assert.equal(confirmed[0].confirmedAt,confirmed[1].confirmedAt)
    const replay=await api('/eoms/h5/repair/confirm',{method:'POST',token:null,headers:first,data:{orderId:order,remark:'不能覆盖首次确认'}})
    assert.equal(replay.reason,marker);assert.equal(replay.confirmedAt,confirmed[0].confirmedAt)
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_finance_event WHERE aggregate_type='ORDER' AND aggregate_id=${order} AND action='CUSTOMER_CONFIRM'`)),1)
    const detail=await api('/eoms/h5/repair/detail/'+apply,{token:null,headers:first});assert.equal(detail.customerConfirmation.confirmed,true)
    await api('/eoms/repair/order/customer-confirmation/'+order,{token:identities.limited.token,reject:true})
    assert.equal((await api('/eoms/repair/order/customer-confirmation/'+order)).reason,marker)
    const settlement=register('h5JointSettlement','eoms_repair_settlement',sql(`SELECT id FROM eoms_repair_settlement WHERE repair_order_id=${order} AND status='DRAFT' AND del_flag='0'`),marker,'formal reversal audit retained')
    await api('/eoms/repair/settlement',{method:'PUT',data:{id:settlement,serviceFee:20,laborAmount:0,laborCost:0,otherAmount:0,otherCost:0,remark:marker}})
    await api('/eoms/repair/settlement/confirm/'+settlement,{method:'PUT'})
    assert.equal(Number(sql(`SELECT material_cost FROM eoms_repair_settlement WHERE id=${settlement}`)),50)
    const receivable=register('h5JointReceivable','eoms_receivable',sql(`SELECT id FROM eoms_receivable WHERE repair_order_id=${order} AND status<>'VOID' AND del_flag='0'`),marker,'VOID receivable audit retained')
    await api('/eoms/finance/receipt/verify',{method:'POST',data:{receivableId:receivable,amount:100,paymentMethod:'CASH',paymentTime:now(),idempotentToken:randomUUID(),remark:marker}})
    const payment=register('h5JointPayment','eoms_payment',sql(`SELECT id FROM eoms_payment WHERE receivable_id=${receivable} AND status='ACTIVE' AND del_flag='0'`),marker,'reversed payment audit retained')
    assert.equal(Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_income WHERE repair_order_id=${order} AND del_flag='0'`)),100)
    await api('/eoms/finance/receipt/'+payment,{method:'DELETE'})
    await api('/eoms/repair/settlement/reverse/'+settlement+'?reason='+encodeURIComponent(marker),{method:'PUT'})
    await api('/eoms/repair/order/cancel/'+order+'?reason='+encodeURIComponent(marker),{method:'PUT'})
    const origin=await api('/psi/stock-in/'+state.objects.h5JointPurchase.id)
    register('h5JointPurchaseReturn','psi_stock_return',idOf(await api('/psi/stock-return',{method:'POST',data:{returnType:'PURCHASE_RETURN',returnDate:now(),unitId,warehouseId,originStockInId:origin.id,remark:marker,items:origin.items.map(row=>({originItemId:String(row.id),goodsId:String(row.goodsId),quantity:row.quantity,taxInclusivePrice:row.taxInclusivePrice,taxRate:row.taxRate}))}})),marker,'confirmed compensating return retained')
    await confirmOwned('/psi/stock-return','h5JointPurchaseReturn')
    const final=await api('/eoms/h5/repair/detail/'+apply,{token:null,headers:first});assert.equal(final.order.status,'CLOSED');assert.equal(final.customerConfirmation.confirmed,true)
    assert.equal(Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE goods_id=${goodsId} AND warehouse_id=${warehouseId}`)),0)
    assert.equal(Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_income WHERE repair_order_id=${order} AND del_flag='0'`)),0)
    assert.equal(Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_cost WHERE repair_order_id=${order} AND del_flag='0'`)),0)
    for(const key of ['h5JointApply','h5JointOrder','h5JointRecord','h5JointMaterial','h5JointSettlement','h5JointReceivable','h5JointPurchaseReturn'])state.objects[key].status='AUDIT_RETAINED'
    state.objects.h5JointPurchase.status='COMPENSATED';state.objects.h5JointPayment.status='COMPENSATED';save()
    state.objects.h5JointPhoto.status='AUDIT_RETAINED';save()
    for (const type of ['processing','completed']) state.objects['h5JointEngineer'+type].status='AUDIT_RETAINED'
    save()
    return {applyId:apply,orderId:order,settlementId:settlement,customerId:unitId,sharedGoodsId:goodsId,actualMaterialCost:50,receivedThenReversed:100,confirmation:confirmed[0],
      concurrentConfirmationOneEvent:true,otherCustomerAndWrongStateDenied:true,forgedCustomerAndTenantIgnored:true,closed:true,netIncome:0,netCost:0,stock:0,
      engineerH5SyntheticCoordinates:geoConfigured===0,realDeviceGps:'NOT_RUN',externalGeocoderCalled:false,browserUI:'NOT_RUN',
      customerPngToJpegPreviewVerified:true,engineerJpegUploads:2,recordPhotosBound:true,
      invalidStartAndFinishCoordinatesRejected:8,invalidOptionalPhotoCoordinatesRejected:3,invalidCoordinatesPreservedOrderState:true}
  })
}
function ownedIds(kind){return [...new Set([...Object.values(state.objects),...(state.archivedObjects||[])].filter(x=>x.kind===kind).map(x=>x.id))]}
function businessZeroProof(){
  const goods=[state.objects.goods.id,state.objects.goodsOther.id].join(','),units=[state.objects.unit.id,state.objects.unitOther.id].join(',')
  const orders=ownedIds('eoms_repair_order'),orderSet=orders.length?orders.join(','):'0'
  const [quantity,cost]=sql(`SELECT COALESCE(SUM(remain_quantity),0),COALESCE(SUM(remain_amount),0) FROM psi_stock_batch WHERE goods_id IN (${goods}) AND deleted=0`).split('\t').map(Number)
  const proof={quantity,cost,
    unexpectedCustomerOrders:Number(sql(`SELECT COUNT(*) FROM eoms_repair_order WHERE customer_id IN (${units}) AND id NOT IN (${orderSet}) AND del_flag='0'`)),
    psiReceivable:Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_receivable WHERE unit_id IN (${units}) AND deleted=0`)),
    psiPayable:Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_payable WHERE unit_id IN (${units}) AND deleted=0`)),
    cashUnallocated:Number(sql(`SELECT COALESCE(SUM(unallocated_amount),0) FROM psi_receipt WHERE unit_id IN (${units}) AND doc_status='CONFIRMED' AND deleted=0`))+Number(sql(`SELECT COALESCE(SUM(unallocated_amount),0) FROM psi_payment WHERE unit_id IN (${units}) AND doc_status='CONFIRMED' AND deleted=0`)),
    openTodos:Number(sql(`SELECT COUNT(*) FROM psi_todo WHERE done=0 AND deleted=0 AND biz_id IN (SELECT id FROM psi_stock_return WHERE unit_id IN (${units}) AND deleted=0)`)),
    openOrders:Number(sql(`SELECT COUNT(*) FROM eoms_repair_order WHERE id IN (${orderSet}) AND status<>'CLOSED' AND del_flag='0'`)),
    activeSettlements:Number(sql(`SELECT COUNT(*) FROM eoms_repair_settlement WHERE repair_order_id IN (${orderSet}) AND status IN ('DRAFT','CONFIRMED') AND del_flag='0'`)),
    eomsArrears:Number(sql(`SELECT COALESCE(SUM(arrears_amount),0) FROM eoms_receivable WHERE repair_order_id IN (${orderSet}) AND del_flag='0'`)),
    eomsIncome:Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_income WHERE repair_order_id IN (${orderSet}) AND del_flag='0'`)),
    eomsCost:Number(sql(`SELECT COALESCE(SUM(amount),0) FROM eoms_cost WHERE repair_order_id IN (${orderSet}) AND del_flag='0'`)),
    activeRepairOuts:Number(sql(`SELECT COUNT(*) FROM psi_stock_out WHERE repair_order_id IN (${orderSet}) AND doc_status='CONFIRMED' AND deleted=0`)),
    activeEomsPayments:Number(sql(`SELECT COUNT(*) FROM eoms_payment p JOIN eoms_receivable r ON r.id=p.receivable_id WHERE r.repair_order_id IN (${orderSet}) AND p.status='ACTIVE' AND p.del_flag='0'`)),
    invoiceDrafts:Number(sql(`SELECT COUNT(*) FROM psi_invoice_out WHERE unit_id IN (${units}) AND doc_status='DRAFT' AND deleted=0`))+Number(sql(`SELECT COUNT(*) FROM psi_invoice_in WHERE unit_id IN (${units}) AND doc_status='DRAFT' AND deleted=0`))}
  for(const table of ['psi_stock_in','psi_stock_out','psi_receipt','psi_payment','psi_invoice_in','psi_invoice_out']){
    const ids=ownedIds(table),known=ids.length?ids.join(','):'0'
    const allowedInternal=table==='psi_stock_out'?` AND NOT (out_type='REPAIR' AND COALESCE(repair_order_id,0) IN (${orderSet}))`:''
    proof['unregistered_'+table]=Number(sql(`SELECT COUNT(*) FROM ${table} WHERE unit_id IN (${units}) AND id NOT IN (${known}) AND deleted=0${allowedInternal}`))
  }
  if(state.funds?.accountId){
    proof.unmatchedBank=Number(sql(`SELECT COUNT(*) FROM psi_bank_statement WHERE account_id=${state.funds.accountId} AND match_status='UNMATCHED' AND deleted=0`))
    proof.bankNet=Number(sql(`SELECT COALESCE(SUM(CASE WHEN direction='IN' THEN amount ELSE -amount END),0) FROM psi_bank_statement WHERE account_id=${state.funds.accountId} AND deleted=0`))
    proof.cashNet=Number(sql(`SELECT COALESCE(SUM(amount),0) FROM psi_receipt WHERE account_id=${state.funds.accountId} AND doc_status='CONFIRMED' AND deleted=0`))-Number(sql(`SELECT COALESCE(SUM(amount),0) FROM psi_payment WHERE account_id=${state.funds.accountId} AND doc_status='CONFIRMED' AND deleted=0`))
  }
  for(const [key,value]of Object.entries(proof))assert.equal(value,0,'Business cleanup invariant '+key)
  return proof
}
async function businessCleanup(){
  await test('R29-BUSINESS-CLEANUP-INDEPENDENT-ZERO',async()=>{
    const proof=businessZeroProof()
    const ownCustomers=[state.objects.unit.id,state.objects.unitOther.id]
    const online=listOf(await api('/eoms/admin/h5/auth/online?page=1&size=100'))
    assert.equal(online.filter(row=>ownCustomers.includes(String(row.customerId))).length,0,'Own H5 sessions must be logged out before disabling masters')
    const project=state.objects.signProject
    if(project && project.status!=='CANCELLED_AUDIT_RETAINED'){
      const detail=await api('/sign/project/'+project.id);assert.equal(detail.projectName,project.marker)
      await api('/sign/project',{method:'PUT',data:{...detail,status:'5',sealIds:[],userIds:[]}})
      assert.equal(sql(`SELECT status FROM sign_project WHERE project_id=${project.id}`),'5')
      project.status='CANCELLED_AUDIT_RETAINED';save()
    }
    if(state.objects.seal){assert.equal(sql(`SELECT status FROM sign_seal WHERE seal_id=${state.objects.seal.id}`),'1');assert.equal(Number(sql(`SELECT COUNT(*) FROM sign_seal_permission WHERE seal_id=${state.objects.seal.id}`)),0)}
    const disabled=[]
    for(const [key,path,name]of [['financeAccount','/psi/account/','accountName'],['goods','/psi/goods/','goodsName'],['goodsOther','/psi/goods/','goodsName'],['warehouse','/psi/warehouse/','warehouseName'],['warehouseOther','/psi/warehouse/','warehouseName'],['category','/psi/goods-category/','name'],['rootCategory','/psi/goods-category/','name'],['unit','/psi/unit/','unitName']]){
      const entry=state.objects[key];if(!entry || entry.status!=='ACTIVE')continue
      const detail=await api(path+entry.id);assert.equal(detail[name],entry.marker)
      await api(path+entry.id,{method:'PUT',data:{...detail,status:2}})
      assert.equal(Number((await api(path+entry.id)).status),2);entry.status='DISABLED_AUDIT_RETAINED';disabled.push(key);save()
    }
    const other=state.objects.unitOther
    if(other.status==='ACTIVE'){
      assert.equal((await api('/psi/unit/'+other.id)).unitName,other.marker)
      await api('/psi/unit',{method:'DELETE',data:{ids:[other.id]}});assert.equal(sql(`SELECT deleted FROM psi_unit WHERE id=${other.id}`),other.id);removed('unitOther')
    }
    for(const key of ['file_txt','file_pdf'])await removeOwnFile(key)
    state.businessCleanup={at:new Date().toISOString(),proof,disabled,sourceFinancialAuditDeleted:false,sharedIdentityCarryover:joint?[]:['engineer','other','manager','finance','limited']};save()
    return state.businessCleanup
  })
}
async function finishIdentities(){
  assert.equal(joint,false,'Shared identities belong to R29; joint run must not remove another run accounts')
  await test('R29-SHARED-IDENTITY-FINAL-CLEANUP',async()=>{
    if(runName==='R29'){
      const jointLedger=JSON.parse(readFileSync(resolve(root,'.local/r30-joint-ledger.json'),'utf8'));assert.ok(jointLedger.businessCleanup)
    }else assert.ok(state.businessCleanup,'Own business cleanup must precede identity removal')
    writeFileSync(local('r29-stop-session-checks-final'),'stop before account cleanup')
    for(const role of ['engineer','other','manager','finance','limited']){
      const entry=state.objects['user_'+role];if(entry.status!=='ACTIVE')continue
      const detail=await api('/system/user/'+entry.id);assert.equal(detail.username,entry.marker);assert.ok(detail.username.startsWith(runName.toLowerCase().replaceAll('-','_')+'_'))
      await api('/auth/logout',{method:'POST',token:identities[role].token})
      await api('/auth/user/info',{token:identities[role].token,reject:true})
      await api('/system/user',{method:'DELETE',data:{ids:[entry.id]}})
      assert.equal(sql(`SELECT deleted FROM sys_user WHERE id=${entry.id}`),entry.id);removed('user_'+role)
    }
    for(const [key,path,table]of [['limitedRole','/system/role','sys_role'],['deptA','/system/dept','sys_dept'],['deptB','/system/dept','sys_dept']]){
      const entry=state.objects[key];if(entry.status!=='ACTIVE')continue
      assert.equal(sql(`SELECT name FROM ${table} WHERE id=${entry.id} AND deleted=0`),entry.marker)
      await api(path,{method:'DELETE',data:{ids:[entry.id]}});assert.equal(sql(`SELECT deleted FROM ${table} WHERE id=${entry.id}`),entry.id);removed(key)
    }
    return {deletedOwnUsers:5,deletedOwnRole:1,deletedOwnDepartments:2,originalAccountsOrRolesModified:false,ownSessionsRevoked:true}
  })
}
async function moduleExit(){
  await test('R29-APPLICABLE-API-MODULE-EXIT',async()=>{
    const latest=Object.values(state.results.reduce((all,item)=>(all[item.id]=item,all),{})).filter(item=>item.validity!=='INVALID_PRECONDITION')
    assert.equal(latest.filter(item=>item.status!=='PASS').length,0,'A required API case still fails')
    assert.ok(state.businessCleanup)
    const proof=businessZeroProof();state.moduleApiExit={at:new Date().toISOString(),version:deploymentEvidence(resolve(root,'.local/r29-deployment-v6.json')).sha256,
      latestApiCases:latest.length,proof,applicationManagement:'REMOVED by product scope',schedule:'N/A disabled by actual configuration',tenant:'N/A disabled in current deployment; SQL isolation covered separately',browserUi:'NOT_RUN in this API logic run; tracked separately, never substituted by API',fullPerformanceAcceptance:'Not evaluated in this logic run; prior R31 engineering benchmark is separate evidence'};save();return state.moduleApiExit
  })
}
async function closeCurrentRun(){
  await test('R29-FINAL-INDEPENDENT-AUDIT',async()=>{
    assert.ok(state.businessCleanup)
    const active=Object.entries(state.objects).filter(([,value])=>value.status==='ACTIVE')
    assert.deepEqual(active.map(([key])=>key),[],'Owned active objects must be resolved')
    const proof=businessZeroProof();state.finalAudit={at:new Date().toISOString(),proof,activeOwnedObjects:0,retainedAuditIsIntentional:true};save();return state.finalAudit
  })
  assert.equal(process.exitCode||0,0);state.closed=new Date().toISOString();save()
}
const command = process.argv[2]
if (deploymentOption) {
  state.currentBackendSha256 = deploymentEvidence('').sha256
  ;(state.executions ||= []).push({ command, backendSha256: state.currentBackendSha256, at: new Date().toISOString() })
  save()
}
if (command === 'h5-online-probe') {
  const data=await api('/eoms/admin/h5/auth/online?page=1&size=100'),rows=listOf(data)
  const customerSessions={}
  for(const key of ['unit','unitOther']){
    const response=await fetch(base+'/eoms/h5/repair/list?page=1&size=1',{headers:{'h5-token':identities[key].token},signal:AbortSignal.timeout(15000)})
    const packet=await response.json();customerSessions[key]={success:packet.success,code:packet.code,message:packet.msg}
  }
  const evidence={keys:Object.keys(data||{}),rows:rows.length,total:data?.total,customerSessions,owned:rows.filter(row=>String(row.customerNo)===sql(`SELECT unit_code FROM psi_unit WHERE id=${state.objects.unitOther.id}`)).map(row=>({keys:Object.keys(row),idType:typeof row.customerId,id:String(row.customerId),hasHandle:!!row.sessionId,hasBearer:Object.hasOwn(row,'token')}))}
  state.h5OnlineProbe=evidence;save();console.log(JSON.stringify(evidence))
}
else if (command === 'role-capabilities') {
  const result={}
  for(const role of ['manager','finance','engineer','other','limited']){
    const info=await api('/auth/user/info',{token:identities[role].token})
    result[role]={id:String(info.id),permissions:(info.permissions||[]).filter(p=>p==='*'||/^(quote:|eoms:|psi:)/.test(p))}
  }
  state.roleCapabilities=result;save();console.log(JSON.stringify(result))
}
else if (command === 'bank-preflight') {
  const accountId=state.funds.accountId
  const rows=sql(`SELECT id,bank_seq_no,match_status,amount FROM psi_bank_statement WHERE account_id=${accountId} AND deleted=0 ORDER BY id`)
  state.funds.bankPreflight={at:new Date().toISOString(),rows}; save(); console.log(rows)
  const lines=rows.split('\n').filter(Boolean)
  for(const row of lines){const [id,seq]=row.split('\t');assert.ok([state.prefix+'-IN-1',state.prefix+'-IN-2'].includes(seq));register('fundBankIn'+(seq.endsWith('-1')?0:1),'psi_bank_statement',id,state.prefix+'-资金全流程','reconcile synthetic original cash; retain audit')}
}
else if (command === 'preflight') await preflight()
else if (command === 'quote-export') await quoteExports()
else if (command === 'sign') await signModule()
else if (command === 'h5-auth') await h5Auth()
else if (command === 'h5-test') await h5Module()
else if (command === 'h5-session-controls') await h5SessionControls()
else if (command === 'h5-joint') await h5JointFlow()
else if (command === 'files') await fileModule()
else if (command === 'notices') await noticeModule()
else if (command === 'messages') await messageRetest()
else if (command === 'platform') await platformRead()
else if (command === 'workbench') await workbenchRead()
else if (command === 'fixtures') await fixtures()
else if (command === 'provision-users') await provisionUsers()
else if (command === 'auth-users') await authUsers()
else if (command === 'auth-status') await authStatus()
else if (command === 'inspect-owned') await inspectOwnedState()
else if (command === 'recover-owned-count') await recoverOwnedCountIncident()
else if (command === 'cash-setup') await cashSetup()
else if (command === 'cash-bank') await cashBank()
else if (command === 'cash-returns') await cashReturns()
else if (command === 'cash-return-verify') await cashReturnVerify()
else if (command === 'cash-refunds') await cashRefunds()
else if (command === 'cash-bank-finish') await cashBankFinish()
else if (command === 'eoms-setup') await eomsPositiveSetup()
else if (command === 'eoms-resume') await eomsPositiveSetup(true)
else if (command === 'apply-order') await applyOrderCleanup()
else if (command === 'invoice-setup') await invoicePositiveSetup()
else if (command === 'invoice-red') await invoiceRedCleanup()
else if (command === 'quote-compat') await quoteCompatibility()
else if (command === 'eoms-reverse') await eomsCashReverse()
else if (command === 'eoms-payment-resume') await eomsCashReverse(true)
else if (command === 'eoms-cancel') await cancelOwnedEoms()
else if (command === 'finance-roles') await financeRoleFlows()
else if (command === 'drafts') await draftModules()
else if (command === 'quote') await quoteLogic()
else if (command === 'purchase') await sourcePurchases()
else if (command === 'returns') await returnLogic()
else if (command === 'compensate') await compensatePurchases()
else if (command === 'todo') await todoLogic()
else if (command === 'extra') await extraBoundaries()
else if (command === 'movement') await stockMovement()
else if (command === 'business-cleanup') await businessCleanup()
else if (command === 'finish-identities') await finishIdentities()
else if (command === 'module-exit') await moduleExit()
else if (command === 'close-run') await closeCurrentRun()
else throw new Error('Select an explicit implemented R29 phase')
