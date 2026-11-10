/** R28 in-use local business-logic testing. See Reports-README §27 for scope and outcomes.
 * Requires an explicitly provisioned own session in ignored .local; never discovers
 * browser sessions, resets existing passwords, disables CAPTCHA, or runs bootstrap SQL.
 * Every phase must be selected explicitly. This run is closed; do not replay blindly.
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
const local = name => resolve(root, '.local', name)
const base = 'http://127.0.0.1:8000'
const admin = JSON.parse(readFileSync(local('r28-session.json'), 'utf8')).token
const ledgerPath = local('r28-ledger.json')
const privatePath = local('r28-identities.json')
const state = existsSync(ledgerPath) ? JSON.parse(readFileSync(ledgerPath, 'utf8'))
  : { run: 'R28', prefix: 'ITLS-TEST-R28', target: base, results: [], objects: {}, started: new Date().toISOString() }
assert.equal(state.run, 'R28'); assert.equal(state.target, base)
assert.ok(!state.closed, 'R28 is closed. Preserve evidence; provision a new run instead of replaying historical fixtures.')
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
  const text = 'BT /F1 18 Tf 50 750 Td (ITLS TEST R28 SYNTHETIC) Tj ET\n'
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
  try { const evidence = await fn(); state.results.push({ id, at: new Date().toISOString(), status: 'PASS', evidence }); console.log(`PASS ${id}`) }
  catch (error) { state.results.push({ id, at: new Date().toISOString(), status: 'FAIL', error: error.message }); console.log(`FAIL ${id}: ${error.message}`); process.exitCode = 1 }
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

const now = () => new Date().toISOString().replace('T', ' ').slice(0, 19)
async function createOwned(key, path, payload, table, column, marker) {
  await api(path, { method: 'POST', data: payload })
  return register(key, table, sql(`SELECT id FROM ${table} WHERE ${column}='${marker}' ORDER BY id DESC LIMIT 1`), marker, 'exact business deletion or full return compensation')
}
async function fixtures() {
  await test('R28-BASELINE-FIXTURES', async () => {
    assert.equal(String((await api('/auth/user/info')).id), '1')
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
      if (state.objects[key]) continue
      const marker = state.prefix + suffix
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
      return evidence
    } finally {
      if (created) { await api(deletePath + created, { method: 'DELETE' }); removed(id) }
    }
  })
}
async function quoteLogic() {
  for (const scenario of ['tax-only', 'status-only', 'forged-total']) await test('R28-QUOTE-' + scenario.toUpperCase(), async () => {
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
  await rejectionProbe('R28-TRANSFER-SAME-WAREHOUSE', '/psi/stock-transfer', { ...transfer, toWarehouseId: warehouseId })
  await rejectionProbe('R28-TRANSFER-NEGATIVE-QUANTITY', '/psi/stock-transfer', { ...transfer, items: [{ goodsId, quantity: -1 }] })
  await rejectionProbe('R28-TRANSFER-SHORTAGE-ROLLBACK', '/psi/stock-transfer', transfer, { confirmFailure: true })
  const check = { checkDate: now(), warehouseId, remark: state.prefix, items: [{ goodsId, bookQty: 0, checkQty: -1 }] }
  await rejectionProbe('R28-CHECK-NEGATIVE-COUNT', '/psi/stock-check', check)
  const invoice = { invoiceNo: state.prefix + '-TEST-NOT-REAL', invoiceType: 'NORMAL', invoiceDate: now(), unitId,
    taxInclusiveAmount: 113, untaxAmount: 100, taxAmount: 13, remark: state.prefix, links: [], items: [] }
  await rejectionProbe('R28-INVOICE-UNLINKED-CONFIRM', '/psi/invoice-out', invoice, { confirmFailure: true })
}
async function sourcePurchases() {
  await test('R28-PURCHASE-SOURCE-DOCUMENTS', async () => {
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
    ['R28-RETURN-CROSS-UNIT', { ...payload, unitId: state.objects.unitOther.id }],
    ['R28-RETURN-CROSS-ORIGIN-ITEM', { ...payload, items: [{ ...line, originItemId: String(otherLine.id) }] }],
    ['R28-RETURN-WRONG-GOODS', { ...payload, items: [{ ...line, goodsId: state.objects.goodsOther.id }] }],
    ['R28-RETURN-NEGATIVE-PRICE', { ...payload, items: [{ ...line, taxInclusivePrice: -100 }] }],
    ['R28-RETURN-NEGATIVE-QUANTITY', { ...payload, items: [{ ...line, quantity: -1 }] }],
    ['R28-RETURN-OVER-QUANTITY', { ...payload, items: [{ ...line, quantity: 11 }] }],
    ['R28-RETURN-DUPLICATE-ORIGIN-OVER-TOTAL', { ...payload, items: [{ ...line, quantity: 6 }, { ...line, quantity: 6 }] }],
    ['R28-RETURN-WRONG-WAREHOUSE', { ...payload, warehouseId: state.objects.warehouseOther.id }]
  ]) await rejectionProbe(id, '/psi/stock-return', changed)
}
async function compensatePurchases() {
  await test('R28-PURCHASE-RETURN-COMPENSATION', async () => {
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
  await test('R28-UNPAID-RETURN-REFUND-TODO', async () => {
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
  await rejectionProbe('R28-ADJUST-NEGATIVE-COST-PRICE', '/psi/stock-adjust', { adjustDate: now(), warehouseId, remark: state.prefix,
    items: [{ goodsId, adjustType: 'PROFIT', quantity: 1, untaxPrice: -10 }] })
  await rejectionProbe('R28-INVOICE-HEADER-TAX-INCONSISTENT', '/psi/invoice-in', { invoiceNo: state.prefix + '-INVALID-TAX', invoiceType: 'NORMAL', invoiceDate: now(), unitId,
    taxInclusiveAmount: 100, untaxAmount: 100, taxAmount: 13, remark: state.prefix, links: [], items: [] })
}
async function stockMovement() {
  const a = state.objects.warehouse.id, b = state.objects.warehouseOther.id, goodsId = state.objects.goods.id
  const stock = warehouse => sql(`SELECT COALESCE(SUM(remain_quantity),0),COALESCE(SUM(remain_amount),0) FROM psi_stock_batch WHERE warehouse_id=${warehouse} AND goods_id=${goodsId}`).split('\t').map(Number)
  async function confirm(path, id, key = randomUUID()) { return api(path + '/' + id + '/confirm', { method: 'POST', data: { idempotentKey: key } }) }
  await test('R28-ADJUST-TRANSFER-COST-AND-COMPENSATION', async () => {
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
  await test('R28-CHECK-ZERO-DIFFERENCE-NO-STOCK-CHANGE', async () => {
    const id = register('checkZero', 'psi_stock_check', idOf(await api('/psi/stock-check', { method: 'POST', data: {
      checkDate: now(), warehouseId: a, remark: state.prefix, items: [{ goodsId, bookQty: 0, checkQty: 0 }] } })), state.prefix, 'confirmed zero-difference audit retained')
    await confirm('/psi/stock-check', id); assert.deepEqual(stock(a), [0, 0])
    state.objects.checkZero.status = 'AUDIT_RETAINED'; save()
    return { id, noDifference: true, quantity: 0, cost: 0 }
  })
}
async function cleanup() {
  await test('R28-CLOSE-EXACT-SYNTHETIC-FALSE-TODOS', async () => {
    const old = JSON.parse(readFileSync(local('r27-ledger.json'), 'utf8'))
    const unitIds = [old.objects.unit.id, state.objects.unit.id]
    for (const table of ['psi_payment', 'psi_receipt']) assert.equal(Number(sql(`SELECT COUNT(*) FROM ${table} WHERE unit_id IN (${unitIds.join(',')}) AND doc_status='CONFIRMED' AND deleted=0`)), 0)
    const rows = sql("SELECT t.id,t.todo_type,t.biz_id,r.remark,t.done FROM psi_todo t JOIN psi_stock_return r ON r.id=t.biz_id WHERE t.deleted=0 AND r.deleted=0 AND t.todo_type IN ('REFUND','RECEIVE_REFUND') AND r.remark IN ('ITLS-TEST-R27-补偿','ITLS-TEST-R28-补偿')")
    const todos = rows.split('\n').filter(Boolean).map(row => { const [id, type, bizId, marker, done] = row.split('\t'); return { id, type, bizId, marker, beforeDone: done } })
    state.exactSyntheticTodoCleanup ||= todos; save()
    for (const entry of todos) {
      assert.match(entry.id, /^\d+$/); assert.ok(['ITLS-TEST-R27-补偿', 'ITLS-TEST-R28-补偿'].includes(entry.marker))
      if (entry.beforeDone === '0') await api('/psi/todo/' + entry.id + '/done', { method: 'POST' })
      assert.equal(sql(`SELECT done FROM psi_todo WHERE id=${entry.id}`), '1')
    }
    return { exactTodos: todos, onlySynthetic: true, noConfirmedPaymentsOrReceipts: true, action: 'manual close of inapplicable test reminders; no money transfer or assertion of actual refund' }
  })
  await test('R28-FIXTURE-CLEANUP-NET-ASSERTIONS', async () => {
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
  await test('R28-OWN-SESSION-LOGOUT', async () => {
    await api('/auth/logout', { method: 'POST' }); await api('/auth/user/info', { reject: true })
    return { onlyOwnSession: true, originalAdminPasswordChanged: false }
  })
  if (!process.exitCode) {
    if (existsSync(privatePath)) unlinkSync(privatePath)
    unlinkSync(local('r28-session.json')); state.closed = new Date().toISOString(); state.privateCredentialsFilesRemoved = true
    writeFileSync(ledgerPath, JSON.stringify(state, null, 2))
  }
}
const command = process.argv[2]
if (command === 'fixtures') await fixtures()
else if (command === 'drafts') await draftModules()
else if (command === 'quote') await quoteLogic()
else if (command === 'purchase') await sourcePurchases()
else if (command === 'returns') await returnLogic()
else if (command === 'compensate') await compensatePurchases()
else if (command === 'todo') await todoLogic()
else if (command === 'extra') await extraBoundaries()
else if (command === 'movement') await stockMovement()
else if (command === 'cleanup') await cleanup()
else if (command === 'finish') await finish()
else throw new Error('Select an explicit implemented R28 phase')
