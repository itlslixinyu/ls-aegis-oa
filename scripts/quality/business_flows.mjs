/** 独立测试栈的真实 API 业务回归；SQL 只用于身份夹具和持久化断言。 */
import { readFileSync, writeFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { randomBytes, randomUUID } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import { resolve } from 'node:path'
import assert from 'node:assert/strict'
import { deflateSync } from 'node:zlib'

const state = JSON.parse(readFileSync(process.env.ITLS_TEST_STATE, 'utf8'))
assert.match(state.runId, /^[a-f0-9]{10}$/)
for (const container of [state.mysql, state.redis, ...(state.backend ? [state.backend] : [])]) {
  const label = spawnSync('docker', ['inspect', '--format', '{{index .Config.Labels "itls.quality.run"}}', container], { encoding: 'utf8' })
  assert.equal(label.status, 0, '隔离容器必须存在')
  assert.equal(label.stdout.trim(), state.runId, '只允许访问本轮专用测试容器')
}
const requireWeb = createRequire(resolve('ls-aegis-web/package.json'))
const { sm2, sm3 } = requireWeb('sm-crypto')
const password = state.password
function sql(query) {
  const r = spawnSync('docker', ['exec', '-e', `MYSQL_PWD=${password}`, state.mysql,
    'mysql', '-uroot', '--default-character-set=utf8mb4', '-N', '-B', 'ls_aegis', '-e', query], { encoding: 'utf8' })
  if (r.status) throw new Error(`数据库断言失败：${r.stderr.slice(-500)}`)
  return r.stdout.trim()
}
const account = (role) => `test_${state.runId}_${role}`
const userId = { admin: '900001', engineer: '900002', other: '900003', limited: '900004' }
if (process.argv.includes('--bootstrap')) {
  const salt = randomBytes(16)
  const hash = `{sm3}v1$${salt.toString('hex')}$${sm3([...salt, ...Buffer.from(password)])}`
  const engineerRole = sql("SELECT id FROM sys_role WHERE code='eoms_engineer' AND deleted=0 LIMIT 1")
  assert.ok(engineerRole, '缺少工程师种子角色')
  for (const role of Object.keys(userId)) {
    sql(`CREATE TEMPORARY TABLE fixture_user LIKE sys_user;
      INSERT INTO fixture_user SELECT * FROM sys_user WHERE id=1;
      UPDATE fixture_user SET id=${userId[role]},username='${account(role)}',nickname='测试${role}',password='${hash}',
        email=NULL,phone=NULL,is_system=0,pwd_reset_time=NOW(),deleted=0;
      INSERT INTO sys_user SELECT * FROM fixture_user;
      INSERT INTO sys_user_role(user_id,role_id) VALUES(${userId[role]},${role === 'admin' ? '1' : role === 'limited' ? '3' : engineerRole});`)
  }
  // 专用夹具配置，不修改部署种子或开发库。PC 图形验证码另作为未覆盖项报告。
  sql("UPDATE sys_option SET value='0' WHERE code='LOGIN_CAPTCHA_ENABLED'")
  console.log('独立测试身份已初始化（未修改既有管理员）')
  process.exit(0)
}
assert.match(state.url, /^http:\/\/127\.0\.0\.1:\d+$/)
const prefix = `ITLS-TEST-${Date.now()}`
const date = new Date().toISOString().slice(0, 19).replace('T', ' ')
const results = []
const ids = {}
async function request(path, { method = 'GET', data, token, headers = {}, reject = false, base = state.url } = {}) {
  assert.ok(base === state.url || (state.h5Url && base === state.h5Url), 'Only the owned fixture backend/proxy is supported')
  const response = await fetch(base + path, {
    method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    body: data === undefined ? undefined : JSON.stringify(data), signal: AbortSignal.timeout(30000),
  })
  const body = await response.json()
  if (reject) {
    assert.equal(body.success, false, `应拒绝 ${method} ${path}`)
    return body
  }
  assert.equal(body.success, true, `${method} ${path}: ${body.msg}`)
  return body.data
}
async function login(role, wrong = false) {
  const key = await request('/auth/gm/public-key')
  const cipher = sm2.doEncrypt(wrong ? 'wrong-test-password' : password, key.publicKeyHex, 1)
  const client = sql('SELECT client_id FROM sys_client WHERE status=1 ORDER BY id LIMIT 1')
  const r = await request('/auth/login', { method: 'POST', data: {
    username: account(role), password: cipher.startsWith('04') ? cipher : `04${cipher}`, authType: 'ACCOUNT', clientId: client,
  }, reject: wrong })
  return r?.token
}
async function test(id, fn) {
  try { const evidence = await fn(); results.push({ id, status: 'PASS', evidence }); console.log(`PASS ${id}`) }
  catch (error) { results.push({ id, status: 'FAIL', error: error.message }); console.log(`FAIL ${id}: ${error.message}`) }
}
const blocked = (id, reason) => { results.push({ id, status: 'BLOCKED', reason }); console.log(`BLOCKED ${id}: ${reason}`) }
async function upload(path, form, { token = admin, headers = {} } = {}) {
  const response = await fetch(state.url + path, { method: 'POST', headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers }, body: form, signal: AbortSignal.timeout(60000) })
  assert.equal(response.status, 200, `上传 ${path}`)
  const body = await response.json()
  assert.equal(body.success, true, `上传 ${path}: ${body.msg}`)
  return body.data
}
// 合成图片供测试印章与客户报修使用，不使用真实业务照片。
function syntheticPng() {
  function crc32(bytes) { let c = 0xffffffff; for (const b of bytes) { c ^= b; for (let i = 0; i < 8; i++) c = (c >>> 1) ^ ((c & 1) ? 0xedb88320 : 0) } return (c ^ 0xffffffff) >>> 0 }
  function chunk(type, bytes) {
    const content = Buffer.concat([Buffer.from(type), bytes]), len = Buffer.alloc(4), crc = Buffer.alloc(4)
    len.writeUInt32BE(bytes.length); crc.writeUInt32BE(crc32(content)); return Buffer.concat([len, content, crc])
  }
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(64, 0); ihdr.writeUInt32BE(64, 4); ihdr[8] = 8; ihdr[9] = 6
  const pixels = Buffer.alloc(64 * (1 + 64 * 4))
  for (let y = 0; y < 64; y++) for (let x = 0; x < 64; x++) {
    const i = y * 257 + 1 + x * 4; pixels[i] = 255; pixels[i + 3] = 180
  }
  return Buffer.concat([Buffer.from('89504e470d0a1a0a', 'hex'), chunk('IHDR', ihdr), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))])
}
const artifactFile = (path, extension) => resolve(`.local/${prefix}-${path.replace(/[^a-zA-Z0-9-]/g, '_')}.${extension}`)
async function download(path, extension, token = admin) {
  const response = await fetch(state.url + path, { headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(60000) })
  assert.equal(response.status, 200, `下载 ${path}`)
  const bytes = Buffer.from(await response.arrayBuffer())
  const valid = bytes.subarray(0, 8).toString().startsWith(extension === 'pdf' ? '%PDF-' : 'PK')
  let errorDetail = ''
  if (!valid) {
    try { const failure = JSON.parse(bytes.toString('utf8')); errorDetail = failure.msg ?? '返回 JSON 而非文件' }
    catch { errorDetail = `响应前缀=${bytes.subarray(0, 12).toString('hex')} 长度=${bytes.length}` }
  }
  assert.ok(valid, `下载格式不正确 ${path}: ${errorDetail}`)
  writeFileSync(artifactFile(path, extension), bytes)
  return bytes.length
}
let admin, engineer, other
await test('AUTH-01', async () => {
  assert.equal(String((await request('/auth/user/info', { reject: true })).code), '401')
  await login('admin', true)
  admin = await login('admin'); engineer = await login('engineer'); other = await login('other')
  const info = await request('/auth/user/info', { token: admin })
  assert.equal(info.username, account('admin'))
  assert.ok(info.permissions.includes('*:*:*'), '专用管理员必须获得平台完整权限')
  const routes = await request('/auth/user/route', { token: admin }); assert.ok(routes.length)
  const limited = await login('limited')
  assert.equal(String((await request('/sign/execute/projects', { token: limited, reject: true })).code), '403')
  await request('/auth/logout', { method: 'POST', token: limited })
  assert.equal(String((await request('/auth/user/info', { token: limited, reject: true })).code), '401')
  return { roles: ['admin', 'engineer', 'other', 'limited'], loginEncryption: 'SM2', logoutRevoked: true }
})
if (admin) await test('AUTH-02-CONTEXT-ISOLATION', async () => {
  const limited = await login('limited')
  for (let i = 0; i < 20; i++) {
    assert.equal(String((await request('/sign/execute/projects', { token: limited, reject: true })).code), '403')
    const current = await request('/auth/user/info', { token: admin })
    assert.equal(current.username, account('admin'), '拒绝请求后不得串用其他账户上下文')
    await request('/sign/execute/projects', { token: admin })
    const worker = await request('/auth/user/info', { token: engineer })
    assert.equal(worker.username, account('engineer'), '线程复用不得串用管理员身份')
  }
  return { alternatingRounds: 20, accounts: 3, permissionFailureIsolation: true }
})
async function create(path, data, table, key, value) {
  await request(path, { method: 'POST', data, token: admin })
  const id = sql(`SELECT id FROM ${table} WHERE ${key}='${value}' ORDER BY id DESC LIMIT 1`)
  assert.match(id, /^\d+$/)
  return id
}
if (admin) await test('FIXTURE-01', async () => {
  ids.customer = await create('/psi/unit', { unitName: `${prefix}-客户`, unitType: 'CUSTOMER', unitNature: 'ENTERPRISE', status: 1 }, 'psi_unit', 'unit_name', `${prefix}-客户`)
  ids.customer2 = await create('/psi/unit', { unitName: `${prefix}-其他客户`, unitType: 'CUSTOMER', unitNature: 'ENTERPRISE', status: 1 }, 'psi_unit', 'unit_name', `${prefix}-其他客户`)
  ids.supplier = await create('/psi/unit', { unitName: `${prefix}-供应商`, unitType: 'SUPPLIER', unitNature: 'ENTERPRISE', status: 1 }, 'psi_unit', 'unit_name', `${prefix}-供应商`)
  const root = await create('/psi/goods-category', { parentId: '0', name: `${prefix}-一级`, status: 1 }, 'psi_goods_category', 'name', `${prefix}-一级`)
  const category = await create('/psi/goods-category', { parentId: root, name: `${prefix}-二级`, status: 1 }, 'psi_goods_category', 'name', `${prefix}-二级`)
  ids.goods = await create('/psi/goods', { goodsName: `${prefix}-商品`, unit: '件', defaultTaxRate: 0, categoryId: category, status: 1 }, 'psi_goods', 'goods_name', `${prefix}-商品`)
  ids.warehouse = await create('/psi/warehouse', { warehouseName: `${prefix}-仓库`, managerUserId: userId.admin, isDefault: false, status: 1 }, 'psi_warehouse', 'warehouse_name', `${prefix}-仓库`)
  return { ...ids }
})
const line = (quantity, price = 100) => ({ goodsId: ids.goods, quantity, taxInclusivePrice: price, taxRate: 0 })
async function confirm(path, id, key = randomUUID()) {
  return request(`${path}/${id}/confirm`, { method: 'POST', token: admin, data: { idempotentKey: key } })
}
async function settlePsi(kind, unitId, openId, amount) {
  const incoming = kind === 'receipt'
  const payload = { [incoming ? 'receiptDate' : 'paymentDate']: date, unitId, amount, payMethod: 'CASH', remark: prefix }
  const id = await request(`/psi/${kind}`, { method: 'POST', token: admin, data: payload })
  await request(`/psi/${kind}/${id}/confirm`, { method: 'POST', token: admin,
    data: { idempotentKey: randomUUID(), writeOffItems: [{ openId, amount }], writeOffReason: prefix } })
  return id
}
if (ids.warehouse) {
  await test('PSI-01', async () => {
    ids.stockIn = await request('/psi/stock-in', { method: 'POST', token: admin, data: {
      inType: 'PURCHASE', inDate: date, warehouseId: ids.warehouse, unitId: ids.supplier, items: [line(10)], remark: prefix,
    } })
    const key = randomUUID()
    await confirm('/psi/stock-in', ids.stockIn, key)
    await confirm('/psi/stock-in', ids.stockIn, key)
    const doc = await request(`/psi/stock-in/${ids.stockIn}`, { token: admin })
    assert.equal(doc.docStatus, 'CONFIRMED')
    assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_payable WHERE source_id=${ids.stockIn} AND deleted=0`)), 1)
    assert.equal(Number(sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`)), 10)
    const payable = sql(`SELECT id FROM psi_payable WHERE source_id=${ids.stockIn} AND deleted=0`)
    const payment = await settlePsi('payment', ids.supplier, payable, 1000)
    assert.equal(Number(sql(`SELECT unwritten_amount FROM psi_payable WHERE id=${payable}`)), 0)
    await request(`/psi/payment/${payment}/write-off`, { method: 'POST', token: admin, data: { items: [{ openId: payable, amount: 1 }] }, reject: true })
    return { documentId: String(ids.stockIn), quantity: 10, payableRecords: 1, paymentId: String(payment), payableBalance: 0, retry: 'same key, no duplicate' }
  })
  await test('PSI-02', async () => {
    const contractId = await request('/psi/contract-sale', { method: 'POST', token: admin,
      data: { name: `${prefix}-销售合同`, signDate: date, unitId: ids.customer, taxInclusiveAmount: 300 } })
    await confirm('/psi/contract-sale', contractId)
    const payload = { outType: 'SALE', outDate: date, warehouseId: ids.warehouse, unitId: ids.customer, contractId,
      items: [line(2, 150)], remark: prefix }
    ids.stockOut = await request('/psi/stock-out', { method: 'POST', token: admin, data: payload })
    const key = randomUUID(); await confirm('/psi/stock-out', ids.stockOut, key); await confirm('/psi/stock-out', ids.stockOut, key)
    assert.equal(Number(sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`)), 8)
    assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_receivable WHERE source_id=${ids.stockOut} AND deleted=0`)), 1)
    assert.equal(Number(sql(`SELECT cost_amount FROM psi_stock_out WHERE id=${ids.stockOut}`)), 200)
    assert.equal(Number(sql(`SELECT SUM(remain_amount) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`)), 800)
    const shortage = await request('/psi/stock-out', { method: 'POST', token: admin, data: { ...payload, items: [line(1000)] } })
    await request(`/psi/stock-out/${shortage}/confirm`, { method: 'POST', token: admin, data: { idempotentKey: randomUUID() }, reject: true })
    assert.equal(Number(sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`)), 8)
    const receivable = sql(`SELECT id FROM psi_receivable WHERE source_id=${ids.stockOut} AND deleted=0`)
    const receipt = await settlePsi('receipt', ids.customer, receivable, 300)
    assert.equal(Number(sql(`SELECT unwritten_amount FROM psi_receivable WHERE id=${receivable}`)), 0)
    return { documentId: String(ids.stockOut), remainingQuantity: 8, receivableRecords: 1, receiptId: String(receipt), receivableBalance: 0, shortageRollback: true }
  })
  await test('QUOTE-01', async () => {
    ids.quote = await create('/quote', { customerId: ids.customer, warehouseId: ids.warehouse, projectName: prefix, taxRate: 0,
      details: [{ lineType: 'MATERIAL', goodsId: ids.goods, itemName: '测试材料', quantity: 2, price: 150 }] }, 'eoms_quote', 'project_name', prefix)
    const q = await request(`/quote/${ids.quote}`, { token: admin })
    assert.equal(q.status, 'DRAFT'); assert.equal(Number(q.totalAmount), 300)
    await request('/quote', { method: 'PUT', token: admin, data: { ...q, status: 'SUBMIT' } })
    ids.order = await request(`/quote/${ids.quote}/to-order`, { method: 'PUT', token: admin, data: { address: '隔离测试地址', remark: prefix } })
    const after = await request(`/quote/${ids.quote}`, { token: admin })
    assert.equal(after.status, 'CONFIRM'); assert.equal(String(after.repairOrderId), String(ids.order))
    const replayOrder = await request(`/quote/${ids.quote}/to-order`, { method: 'PUT', token: admin, data: {} })
    assert.equal(String(replayOrder), String(ids.order), '重复转换应返回同一工单，不重复创建')
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_order WHERE quote_id=${ids.quote} AND del_flag='0'`)), 1)
    await request('/quote', { method: 'PUT', token: admin, data: { ...after, projectName: '不应修改' }, reject: true })
    const render = await request(`/quote/${ids.quote}/render`, { token: admin }); assert.ok(render.html.includes('测试材料'))
    const docxBytes = await download(`/quote/${ids.quote}/export-docx`, 'docx')
    const pdfBytes = await download(`/quote/${ids.quote}/export-pdf`, 'pdf')
    const word = spawnSync('python', ['-X', 'utf8', '-c',
      'import zipfile,sys,xml.etree.ElementTree as E; print("".join(E.fromstring(zipfile.ZipFile(sys.argv[1]).read("word/document.xml")).itertext()))',
      artifactFile(`/quote/${ids.quote}/export-docx`, 'docx')], { encoding: 'utf8' })
    assert.equal(word.status, 0); assert.ok(word.stdout.includes('测试材料') && word.stdout.includes('300'))
    return { quoteId: ids.quote, orderId: String(ids.order), amount: 300, terminalEditRejected: true, duplicateConversionIdempotent: true, docxBytes, pdfBytes }
  })
  await test('QUOTE-02-INDEPENDENT', async () => {
    const quoteId = await create('/quote', { customerId: ids.customer, projectName: `${prefix}-独立报价`, taxRate: 0,
      details: [{ lineType: 'SERVICE', itemName: '测试服务', quantity: 1, price: 50 }] }, 'eoms_quote', 'project_name', `${prefix}-独立报价`)
    const quote = await request(`/quote/${quoteId}`, { token: admin })
    await request('/quote', { method: 'PUT', token: admin, data: { ...quote, status: 'SUBMIT' } })
    await request('/quote', { method: 'PUT', token: admin, data: { ...quote, status: 'CONFIRM' } })
    const confirmed = await request(`/quote/${quoteId}`, { token: admin })
    assert.equal(confirmed.status, 'CONFIRM'); assert.ok(confirmed.repairOrderId == null)
    assert.equal(Number(confirmed.totalAmount), 50)
    await request('/quote', { method: 'PUT', token: admin, data: { ...confirmed, status: 'DRAFT' }, reject: true })
    return { quoteId, status: 'CONFIRM', amount: 50, orderRequired: false, terminalRevertRejected: true }
  })
}
if (admin && ids.customer && ids.supplier) await test('PSI-BANK-01', async () => {
  const accountId = await create('/psi/account', {
    accountName: `${prefix}-bank`, bankName: '合成测试银行', accountNo: `TEST-${state.runId}`, status: 1,
  }, 'psi_company_account', 'account_name', `${prefix}-bank`)
  const docs = []
  for (const [kind, type, direction, unitId] of [
    ['receipt', 'RECEIPT', 'IN', ids.customer], ['payment', 'PAYMENT', 'OUT', ids.supplier],
  ]) {
    const id = await request(`/psi/${kind}`, { method: 'POST', token: admin, data: {
      unitId, accountId, amount: 123.45, payMethod: 'TRANSFER', remark: prefix,
      [kind === 'receipt' ? 'receiptDate' : 'paymentDate']: date,
    } })
    await confirm(`/psi/${kind}`, id)
    docs.push({ kind, type, direction, id: String(id), bankSeqNo: `${prefix}-${direction}` })
  }
  const items = docs.map(doc => ({ stmtDate: date, direction: doc.direction, amount: 123.45,
    bankSeqNo: doc.bankSeqNo, counterparty: prefix, summary: prefix }))
  items.push({ ...items[0], bankSeqNo: `${prefix}-IN-duplicate` })
  assert.equal(Number(await request('/psi/bank/import', { method: 'POST', token: admin, data: { accountId, items } })), 3)
  assert.equal(Number(await request('/psi/bank/import', { method: 'POST', token: admin, data: { accountId, items } })), 0)
  for (const doc of docs) {
    const statementId = sql(`SELECT id FROM psi_bank_statement WHERE bank_seq_no='${doc.bankSeqNo}' AND deleted=0`)
    const candidates = await request(`/psi/bank/${statementId}/candidates`, { token: admin })
    assert.deepEqual(candidates.map(item => String(item.value)), [doc.id])
    await request(`/psi/bank/${statementId}/candidates`, { token: other, reject: true })
    assert.equal(await request('/psi/bank/match', { method: 'POST', token: admin,
      data: { statementId, matchBizType: doc.type, matchBizId: doc.id } }), 'MATCHED')
    assert.equal((await request(`/psi/bank/${statementId}`, { token: admin })).matchStatus, 'MATCHED')
    if (doc.kind === 'receipt') {
      const duplicate = sql(`SELECT id FROM psi_bank_statement WHERE bank_seq_no='${prefix}-IN-duplicate' AND deleted=0`)
      const occupied = await request(`/psi/bank/${duplicate}/candidates`, { token: admin })
      assert.ok(!occupied.some(item => String(item.value) === doc.id))
      await request('/psi/bank/match', { method: 'POST', token: admin, reject: true,
        data: { statementId: duplicate, matchBizType: doc.type, matchBizId: doc.id } })
    }
    const action = { reason: prefix, idempotentKey: randomUUID() }
    await request(`/psi/bank/${statementId}/unmatch`, { method: 'POST', token: admin, data: action })
    await request(`/psi/bank/${statementId}/unmatch`, { method: 'POST', token: admin, data: action })
    assert.equal((await request(`/psi/bank/${statementId}`, { token: admin })).matchStatus, 'UNMATCHED')
    const available = await request(`/psi/bank/${statementId}/candidates`, { token: admin })
    assert.ok(available.some(item => String(item.value) === doc.id))
  }
  return { accountId, documentTypes: docs.map(doc => doc.type), importReplayAdded: 0,
    occupiedExcluded: true, forbiddenRoleRejected: true, repeatedUnmatchIdempotent: true }
})
if (ids.order) {
  await test('EOMS-01', async () => {
    await request(`/eoms/repair/order/submit/${ids.order}`, { method: 'PUT', token: admin, reject: true })
    await request(`/eoms/repair/order/assign/${ids.order}?engineerId=${userId.engineer}`, { method: 'PUT', token: admin })
    await request(`/eoms/engineer/accept/${ids.order}`, { method: 'POST', token: engineer })
    await request(`/eoms/engineer/task/${ids.order}`, { token: other, reject: true })
    await request(`/eoms/engineer/start/${ids.order}?longitude=116.4&latitude=39.9`, { method: 'POST', token: engineer })
    await request('/eoms/engineer/record', { method: 'POST', token: engineer, data: { repairOrderId: ids.order,
      faultReason: '测试故障', checkProcess: '检查', solution: '修复', result: '已修复' } })
    await request('/eoms/repair/material', { method: 'POST', token: admin, data: { repairOrderId: ids.order,
      materialId: ids.goods, warehouseId: ids.warehouse, materialName: '测试材料', quantity: 1, salePrice: 150, costPrice: 100 } })
    await request(`/eoms/engineer/finish/${ids.order}?longitude=116.4&latitude=39.9`, { method: 'POST', token: engineer })
    const order = await request(`/eoms/repair/order/${ids.order}`, { token: admin })
    assert.equal(order.status, 'WAIT_SETTLEMENT')
    ids.settlement = sql(`SELECT id FROM eoms_repair_settlement WHERE repair_order_id=${ids.order} AND del_flag='0' LIMIT 1`)
    return { orderId: String(ids.order), assignedEngineer: userId.engineer, crossEngineerRejected: true, state: order.status }
  })
  if (ids.settlement) await test('EOMS-02-X-01', async () => {
    const incomeBefore = Number(sql('SELECT COUNT(*) FROM eoms_income'))
    const concurrent = await Promise.allSettled([1, 2].map(() => request(`/eoms/repair/settlement/confirm/${ids.settlement}`, { method: 'PUT', token: admin })))
    assert.equal(concurrent.filter(r => r.status === 'fulfilled').length, 1, '并发确认只能成功一次')
    await request(`/eoms/repair/settlement/confirm/${ids.settlement}`, { method: 'PUT', token: admin, reject: true })
    assert.equal(Number(sql('SELECT COUNT(*) FROM eoms_income')), incomeBefore)
    assert.equal(Number(sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`)), 7)
    assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_stock_out WHERE out_type='REPAIR' AND deleted=0 AND remark LIKE CONCAT('工单[',(SELECT order_no FROM eoms_repair_order WHERE id=${ids.order}),']结算出库%')`)), 1)
    const receivable = sql(`SELECT id FROM eoms_receivable WHERE repair_order_id=${ids.order} AND del_flag='0'`)
    assert.match(receivable, /^\d+$/)
    const amount = Number(sql(`SELECT amount FROM eoms_receivable WHERE id=${receivable}`))
    await request('/eoms/finance/receipt/verify', { method: 'POST', token: admin, data: { receivableId: receivable,
      amount: amount + 1, paymentMethod: 'CASH', paymentTime: date, idempotentToken: randomUUID() }, reject: true })
    const payment = { receivableId: receivable, amount, paymentMethod: 'CASH', paymentTime: date, idempotentToken: randomUUID() }
    await request('/eoms/finance/receipt/verify', { method: 'POST', token: admin, data: payment })
    // 重试可被拒绝或幂等返回，必须核实只有一条实收记录。
    try { await request('/eoms/finance/receipt/verify', { method: 'POST', token: admin, data: payment }) } catch {}
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_payment WHERE receivable_id=${receivable} AND del_flag='0'`)), 1)
    assert.equal(Number(sql('SELECT COUNT(*) FROM eoms_income')), incomeBefore + 1)
    assert.equal(Number(sql(`SELECT SUM(amount) FROM eoms_income WHERE repair_order_id=${ids.order} AND del_flag='0'`)), amount)
    return { settlementId: ids.settlement, receivableId: receivable, remainingQuantity: 7, paymentRecords: 1, noPrematureIncome: true, concurrentConfirmSuccess: 1 }
  })
  await test('EOMS-03-STOCK-ROLLBACK', async () => {
    const marker = `${prefix}-rollback`
    const orderId = await create('/eoms/repair/order', { customerId: ids.customer, remark: marker,
      faultDesc: marker, address: '隔离测试地址' }, 'eoms_repair_order', 'remark', marker)
    await request(`/eoms/repair/order/assign/${orderId}?engineerId=${userId.engineer}`, { method: 'PUT', token: admin })
    await request(`/eoms/engineer/accept/${orderId}`, { method: 'POST', token: engineer })
    await request(`/eoms/engineer/start/${orderId}?longitude=116.4&latitude=39.9`, { method: 'POST', token: engineer })
    await request('/eoms/engineer/record', { method: 'POST', token: engineer,
      data: { repairOrderId: orderId, faultReason: marker, checkProcess: '检查', solution: '修复', result: '已修复' } })
    await request('/eoms/repair/material', { method: 'POST', token: admin, data: { repairOrderId: orderId,
      materialId: ids.goods, warehouseId: ids.warehouse, materialName: '测试材料', quantity: 1000, salePrice: 150, costPrice: 100 } })
    await request(`/eoms/engineer/finish/${orderId}?longitude=116.4&latitude=39.9`, { method: 'POST', token: engineer })
    const settlement = sql(`SELECT id FROM eoms_repair_settlement WHERE repair_order_id=${orderId} AND del_flag='0'`)
    const before = sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`)
    await request(`/eoms/repair/settlement/confirm/${settlement}`, { method: 'PUT', token: admin, reject: true })
    assert.equal(sql(`SELECT status FROM eoms_repair_settlement WHERE id=${settlement}`), 'DRAFT')
    assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${orderId}`), 'WAIT_SETTLEMENT')
    assert.equal(sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`), before)
    assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_receivable WHERE repair_order_id=${orderId} AND del_flag='0'`)), 0)
    assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_stock_out WHERE deleted=0 AND remark LIKE CONCAT('工单[',(SELECT order_no FROM eoms_repair_order WHERE id=${orderId}),']结算出库%')`)), 0)
    return { orderId, settlementId: settlement, statusRolledBack: true, stockUnchanged: true, receivableRecords: 0, leakedStockOutDocuments: 0 }
  })
}
await test('SIGN-01-AUTH', async () => {
  assert.equal(String((await request('/sign/execute/projects', { reject: true })).code), '401')
  const limited = await login('limited')
  assert.equal(String((await request('/sign/execute/projects', { token: limited, reject: true })).code), '403')
  const invalid = await request('/sign/execute/prepare', { method: 'POST', token: admin,
    data: { projectId: '999999999', sysFileId: '999999999', sealId: '999999999', signMode: 'single',
      positions: [{ page: 1, x: 0, y: 0, size: 60 }] }, reject: true })
  assert.match(invalid.msg, /不存在|不可用|无权|未授权/)
  return { unauthorizedRejected: true, nonexistentResourcesRejected: true }
})
if (admin) await test('SIGN-01-SIGNATURE', async () => {
  // 合成测试 PDF，无真实合同内容；标准 xref 偏移由实际字节数计算。
  const content = 'BT /F1 24 Tf 50 750 Td (ITLS TEST) Tj ET\n'
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>',
    `<< /Length ${Buffer.byteLength(content)} >>\nstream\n${content}endstream`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
  ]
  let pdf = '%PDF-1.4\n'; const offsets = [0]
  objects.forEach((object, i) => { offsets.push(Buffer.byteLength(pdf)); pdf += `${i + 1} 0 obj\n${object}\nendobj\n` })
  const xref = Buffer.byteLength(pdf)
  pdf += `xref\n0 6\n0000000000 65535 f \n${offsets.slice(1).map(x => `${String(x).padStart(10, '0')} 00000 n \n`).join('')}trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
  const source = new FormData(); source.set('file', new Blob([pdf], { type: 'application/pdf' }), 'isolated-test.pdf')
  const file = await upload('/system/common/file', source)
  assert.ok(file.id)
  const seal = new FormData(); seal.set('file', new Blob([syntheticPng()], { type: 'image/png' }), 'test-only-seal.png')
  seal.set('sealName', prefix); seal.set('sealCategory', 'TEST'); seal.set('status', '0'); seal.set('deptId', '1')
  await upload('/sign/seal', seal)
  const sealId = sql(`SELECT seal_id FROM sign_seal WHERE seal_name='${prefix}' ORDER BY seal_id DESC LIMIT 1`)
  assert.match(sealId, /^\d+$/)
  await request(`/sign/seal/${sealId}/auth/bind`, { method: 'POST', token: admin, data: [userId.admin] })
  await request('/sign/seal/bind-cert', { method: 'POST', token: admin, data: { sealId } })
  await request('/sign/project', { method: 'POST', token: admin, data: { projectName: prefix, managerIds: userId.admin,
    deptId: '1', startDate: '2020-01-01 00:00:00', endDate: '2099-12-31 23:59:59',
    otherFileIds: file.id, sealIds: [sealId], userIds: [userId.admin] } })
  const projectId = sql(`SELECT project_id FROM sign_project WHERE project_name='${prefix}' ORDER BY project_id DESC LIMIT 1`)
  await request(`/sign/project/review/${projectId}?status=1`, { method: 'PUT', token: admin })
  const payload = { projectId, sysFileId: file.id, sealId, signMode: 'single', useManagedKey: '1', hasTimestamp: '0', positions: [{ page: 1, x: 100, y: 100, size: 60 }] }
  await request('/sign/execute/prepare', { method: 'POST', token: admin, data: payload })
  const signing = await Promise.allSettled([1, 2].map(() => request('/sign/execute/run', { method: 'POST', token: admin, data: payload })))
  const completed = signing.filter(r => r.status === 'fulfilled')
  assert.equal(completed.length, 1, '并发重复签章只能执行一次')
  const signed = completed[0].value
  assert.ok(signed.recordId)
  const receipt = await request(`/sign/execute/receipt/${signed.recordId}`, { token: admin })
  assert.equal(receipt.verifyStatus, '1'); assert.equal(String(receipt.signedFileId), String(signed.signedFileId))
  const bytes = await download(`/sign/file/download/${signed.signedFileId}`, 'pdf')
  const signedPdf = readFileSync(artifactFile(`/sign/file/download/${signed.signedFileId}`, 'pdf'))
  const range = /\/ByteRange\s*\[\s*(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*\]/.exec(signedPdf.toString('latin1'))
  assert.ok(range, '签章 PDF 必须含数字签名 ByteRange')
  const [start, count, second, secondCount] = range.slice(1).map(Number)
  assert.equal(start, 0); assert.equal(second + secondCount, signedPdf.length)
  const cmsHex = signedPdf.subarray(count, second).toString('ascii').replace(/[<>\s]/g, '')
  const cmsPadded = Buffer.from(cmsHex, 'hex')
  const lengthBytes = cmsPadded[1] & 0x80 ? cmsPadded[1] & 0x7f : 0
  const contentLength = lengthBytes ? cmsPadded.readUIntBE(2, lengthBytes) : cmsPadded[1]
  const cms = cmsPadded.subarray(0, 2 + lengthBytes + contentLength)
  const cmsFile = resolve(`.local/${prefix}-signature.der`), contentFile = resolve(`.local/${prefix}-signed-content.bin`)
  writeFileSync(cmsFile, cms)
  writeFileSync(contentFile, Buffer.concat([signedPdf.subarray(start, count), signedPdf.subarray(second, second + secondCount)]))
  for (const [sourcePath, destination] of [[cmsFile, '/tmp/test-signature.der'], [contentFile, '/tmp/test-signed-content.bin']]) {
    assert.equal(spawnSync('docker', ['cp', sourcePath, `${state.backend}:${destination}`], { encoding: 'utf8' }).status, 0)
  }
  const verification = spawnSync('docker', ['exec', state.backend, 'openssl', 'cms', '-verify', '-binary', '-inform', 'DER',
    '-in', '/tmp/test-signature.der', '-content', '/tmp/test-signed-content.bin', '-noverify', '-out', '/tmp/test-verified.bin'], { encoding: 'utf8' })
  assert.equal(verification.status, 0, `独立 CMS 签名核验失败：${verification.stderr}`)
  const tampered = readFileSync(contentFile); tampered[20] ^= 1
  const tamperedFile = resolve(`.local/${prefix}-tampered-content.bin`)
  writeFileSync(tamperedFile, tampered)
  assert.equal(spawnSync('docker', ['cp', tamperedFile, `${state.backend}:/tmp/test-tampered-content.bin`], { encoding: 'utf8' }).status, 0)
  const rejection = spawnSync('docker', ['exec', state.backend, 'openssl', 'cms', '-verify', '-binary', '-inform', 'DER',
    '-in', '/tmp/test-signature.der', '-content', '/tmp/test-tampered-content.bin', '-noverify', '-out', '/tmp/test-rejected.bin'], { encoding: 'utf8' })
  assert.notEqual(rejection.status, 0, '篡改内容必须导致密码学核验失败')
  const unifiedPayload = { projectId, sysFileId: file.id, sealId,
    generalSeals: [{ sealId, mode: 'single', positions: [{ page: 1, x: 100, y: 100, width: 60, height: 60 }] }] }
  const unified = await Promise.allSettled([1, 2].map(() => request('/sign/execute/unified', { method: 'POST', token: admin, data: unifiedPayload })))
  const unifiedCompleted = unified.filter(r => r.status === 'fulfilled')
  assert.equal(unifiedCompleted.length, 1, '合并签章接口也必须阻止并发重复执行')
  const unifiedReceipt = await request(`/sign/execute/receipt/${unifiedCompleted[0].value.recordId}`, { token: admin })
  assert.equal(unifiedReceipt.verifyStatus, '1')
  return { projectId, sealId, recordId: signed.recordId, verifyStatus: receipt.verifyStatus, signedPdfBytes: bytes,
    cryptographicSignatureVerified: true, tamperingRejected: true, concurrentSigningSuccess: 1, concurrentUnifiedSigningSuccess: 1,
    certificate: '隔离环境自签测试证书；OpenSSL 核验文件签名，未校验外部信任链' }
})
if (ids.customer2) await test('EOMS-01-CUSTOMER-H5', async () => {
  async function customerToken(customerId) {
    const username = `customer_${state.runId}_${String(customerId).slice(-6)}`
    const customerPassword = `C${password.slice(0, 20)}7`
    const invitation = await request('/eoms/admin/h5/auth/invitations', {
      method: 'POST', token: admin, data: { customerId },
    })
    async function captchaFields() {
      const cap = await request('/eoms/h5/auth/captcha')
      const keys = spawnSync('docker', ['exec', state.redis, 'redis-cli', '--no-auth-warning', '-a', password,
        '--scan', '--pattern', `*${cap.uuid}`], { encoding: 'utf8' }).stdout.trim().split('\n')
      assert.equal(keys.length, 1, '隔离验证码缓存必须唯一')
      const raw = spawnSync('docker', ['exec', state.redis, 'redis-cli', '--no-auth-warning', '-a', password,
        '--raw', 'GET', keys[0].trim()], { encoding: 'utf8' }).stdout.trim()
      let captcha = raw
      try { const parsed = JSON.parse(raw); captcha = Array.isArray(parsed) ? parsed.at(-1) : parsed } catch {}
      return { captcha, uuid: cap.uuid }
    }
    const publicKey = await request('/auth/gm/public-key')
    const encryptedPassword = () => {
      const cipher = sm2.doEncrypt(customerPassword, publicKey.publicKeyHex, 1)
      return cipher.startsWith('04') ? cipher : `04${cipher}`
    }
    const activated = await request('/eoms/h5/auth/activate', { method: 'POST', data: {
      invitationCode: invitation.invitationCode, username, password: encryptedPassword(), ...await captchaFields(),
    } })
    assert.ok(activated.token, '隔离客户激活应取得客户会话')
    await request('/eoms/h5/auth/logout', { method: 'POST', headers: { 'h5-token': activated.token } })
    const loggedIn = await request('/eoms/h5/auth/password', { method: 'POST', data: {
      username, password: encryptedPassword(), ...await captchaFields(),
    } })
    assert.ok(loggedIn.token, '客户激活后应能使用用户名密码登录')
    return loggedIn.token
  }
  const first = await customerToken(ids.customer), second = await customerToken(ids.customer2)
  const photo = new FormData()
  photo.set('file', new Blob([syntheticPng()], { type: 'image/png' }), 'synthetic-repair-photo.png')
  const uploadedPhoto = await upload('/eoms/h5/repair/upload', photo, { token: null, headers: { 'h5-token': first } })
  assert.ok(uploadedPhoto.fileId)
  const apply = await request('/eoms/h5/repair/apply', { method: 'POST', headers: { 'h5-token': first }, data: {
    customerId: ids.customer2, contactName: '隔离测试客户', contactPhone: '13800000000', faultType: 'OTHER',
    faultDesc: prefix, address: '隔离测试地址', photos: String(uploadedPhoto.fileId),
  } })
  assert.equal(sql(`SELECT customer_id FROM eoms_repair_apply WHERE id=${apply.id}`), ids.customer)
  await request(`/eoms/h5/repair/detail/${apply.id}`, { headers: { 'h5-token': first } })
  await request(`/eoms/h5/repair/detail/${apply.id}`, { headers: { 'h5-token': second }, reject: true })
  await request(`/eoms/repair/apply/confirm/${apply.id}`, { method: 'PUT', token: admin })
  await request(`/eoms/repair/apply/toOrder/${apply.id}`, { method: 'PUT', token: admin, data: {} })
  const h5Order = sql(`SELECT id FROM eoms_repair_order WHERE apply_id=${apply.id} AND del_flag='0'`)
  assert.match(h5Order, /^\d+$/)
  // RepairApply has no persisted address field; compare the actual work-order site,
  // rather than assuming an unknown JSON field submitted above was persisted.
  const originalServiceAddress = sql(`SELECT address FROM eoms_repair_order WHERE id=${h5Order}`)
  await request(`/eoms/repair/order/assign/${h5Order}?engineerId=${userId.engineer}`, { method: 'PUT', token: admin })
  await request(`/eoms/engineer/accept/${h5Order}`, { method: 'POST', token: engineer })
  const attendanceAddress = { start: '河北省廊坊市合成签到地点', finish: '河北省廊坊市合成签退地点' }
  await request(`/eoms/engineer/start/${h5Order}?${new URLSearchParams({ longitude: '116.4', latitude: '39.9', location: attendanceAddress.start, remark: '合成签到备注' })}`, { method: 'POST', token: engineer })
  assert.equal(sql(`SELECT sign_in_address FROM eoms_repair_order WHERE id=${h5Order}`), attendanceAddress.start)
  assert.equal(sql(`SELECT address FROM eoms_repair_order WHERE id=${h5Order}`), originalServiceAddress)
  assert.ok(sql(`SELECT remark FROM eoms_repair_order WHERE id=${h5Order}`).includes('合成签到备注'))
  const repairPhotos = {}
  for (const photoType of ['processing', 'completed']) {
    const form = new FormData()
    form.set('file', new Blob([syntheticPng()], { type: 'image/png' }), `synthetic-${photoType}.png`)
    form.set('orderId', h5Order); form.set('photoType', photoType)
    const image = await upload('/eoms/engineer/upload', form, { token: engineer })
    assert.match(String(image.fileId), /^[1-9][0-9]*$/)
    repairPhotos[photoType] = String(image.fileId)
    const preview = await fetch(state.url + `/eoms/file/preview/${image.fileId}`, { headers: { Authorization: `Bearer ${engineer}` } })
    assert.equal(preview.status, 200)
    const bytes = new Uint8Array(await preview.arrayBuffer())
    assert.ok(bytes.length > 0 && bytes[0] === 255 && bytes[1] === 216, '维修照片应回显为有效水印JPEG')
  }
  await request('/eoms/engineer/record', { method: 'POST', token: engineer, data: { repairOrderId: h5Order,
    faultReason: '测试客户报修', checkProcess: '检查', solution: '修复', result: '完成',
    processingPhotos: repairPhotos.processing, completedPhotos: repairPhotos.completed } })
  assert.equal(sql(`SELECT processing_photos FROM eoms_repair_record WHERE repair_order_id=${h5Order} AND del_flag='0'`), repairPhotos.processing)
  assert.equal(sql(`SELECT completed_photos FROM eoms_repair_record WHERE repair_order_id=${h5Order} AND del_flag='0'`), repairPhotos.completed)
  await request('/eoms/repair/material', { method: 'POST', token: admin, data: { repairOrderId: h5Order,
    materialId: ids.goods, warehouseId: ids.warehouse, materialName: '测试材料', quantity: 1, salePrice: 150, costPrice: 100 } })
  await request(`/eoms/engineer/finish/${h5Order}?${new URLSearchParams({ longitude: '116.4', latitude: '39.9', location: attendanceAddress.finish, remark: '合成签退备注' })}`, { method: 'POST', token: engineer })
  assert.equal(sql(`SELECT sign_in_address FROM eoms_repair_order WHERE id=${h5Order}`), attendanceAddress.start)
  assert.equal(sql(`SELECT sign_out_address FROM eoms_repair_order WHERE id=${h5Order}`), attendanceAddress.finish)
  assert.equal(sql(`SELECT address FROM eoms_repair_order WHERE id=${h5Order}`), originalServiceAddress)
  await request('/eoms/h5/repair/confirm', { method: 'POST', headers: { 'h5-token': first }, data: { orderId: h5Order } })
  const h5Settlement = sql(`SELECT id FROM eoms_repair_settlement WHERE repair_order_id=${h5Order} AND del_flag='0'`)
  await request(`/eoms/repair/settlement/confirm/${h5Settlement}`, { method: 'PUT', token: admin })
  await request(`/eoms/repair/order/close/${h5Order}?remark=${encodeURIComponent('隔离回归完成')}`, { method: 'PUT', token: admin })
  const finalDetail = await request(`/eoms/h5/repair/detail/${apply.id}`, { headers: { 'h5-token': first } })
  assert.equal(finalDetail.order.status, 'CLOSED')
  await request('/eoms/h5/auth/logout', { method: 'POST', headers: { 'h5-token': first } })
  await request(`/eoms/h5/repair/detail/${apply.id}`, { headers: { 'h5-token': first }, reject: true })
  await request('/eoms/h5/auth/logout', { method: 'POST', headers: { 'h5-token': second } })
  return { applyId: String(apply.id), orderId: h5Order, finalOrderStatus: 'CLOSED', forgedCustomerIgnored: true, crossCustomerRejected: true,
    tokenRevoked: true, repairPhotoGroupsPersisted: 2, watermarkedPhotoPreview: true, independentAttendanceAddresses: true, signInRemarkPersisted: true, captcha: 'API 测试从专用 Redis 读取正确答案；未覆盖图像交互' }
})
if (admin && ids.warehouse) await test('PSI-CASH-SUMMARY-01', async () => {
  const evidence = []
  for (const kind of ['payment', 'receipt']) {
    const incoming = kind === 'receipt', stock = incoming ? 'stock-out' : 'stock-in'
    const unitId = incoming ? ids.customer : ids.supplier, table = incoming ? 'psi_receivable' : 'psi_payable'
    let contractId
    if (incoming) {
      contractId = await request('/psi/contract-sale', { method: 'POST', token: admin, data: {
        name: `${prefix}-核销汇总合同`, signDate: date, unitId, taxInclusiveAmount: 200,
      } })
      await confirm('/psi/contract-sale', contractId)
    }
    const opens = []
    for (let i = 0; i < 2; i++) {
      const document = await request(`/psi/${stock}`, { method: 'POST', token: admin, data: {
        [incoming ? 'outType' : 'inType']: incoming ? 'SALE' : 'PURCHASE',
        [incoming ? 'outDate' : 'inDate']: date, unitId, warehouseId: ids.warehouse, contractId,
        items: [line(1)], remark: `${prefix}-summary-${kind}-${i}`,
      } })
      await confirm(`/psi/${stock}`, document)
      opens.push(sql(`SELECT id FROM ${table} WHERE source_id=${document} AND deleted=0`))
    }
    const cash = await request(`/psi/${kind}`, { method: 'POST', token: admin, data: {
      [incoming ? 'receiptDate' : 'paymentDate']: date, unitId, amount: 200, payMethod: 'CASH', remark: prefix,
    } })
    await confirm(`/psi/${kind}`, cash)
    const masters = []
    for (const openId of opens) masters.push(String(await request(`/psi/${kind}/${cash}/write-off`, {
      method: 'POST', token: admin, data: { items: [{ openId, amount: 100 }], reason: prefix },
    })))
    const path = `/psi/${kind}/${cash}/write-offs`
    const summaries = await request(path, { token: admin })
    assert.equal(summaries.length, 2)
    assert.deepEqual(summaries.map(s => String(s.writeOffId)).sort(), [...masters].sort())
    for (const summary of summaries) { assert.equal(Number(summary.amount), 100); assert.equal(summary.reason, prefix) }
    await request(path, { token: other, reject: true })
    await request(`/psi/${kind}/${cash}/reverse`, { method: 'POST', token: admin,
      data: { writeOffId: masters[0], reason: prefix } })
    const remaining = await request(path, { token: admin })
    assert.deepEqual(remaining.map(s => String(s.writeOffId)), [masters[1]])
    assert.equal(Number(sql(`SELECT unwritten_amount FROM ${table} WHERE id=${opens[0]}`)), 100)
    assert.equal(Number(sql(`SELECT unallocated_amount FROM psi_${kind} WHERE id=${cash}`)), 100)
    await request(`/psi/${kind}/${cash}/reverse`, { method: 'POST', token: admin,
      data: { writeOffId: masters[1], reason: prefix } })
    assert.equal((await request(path, { token: admin })).length, 0)
    assert.equal(Number(sql(`SELECT unallocated_amount FROM psi_${kind} WHERE id=${cash}`)), 200)
    evidence.push({ kind, cashId: String(cash), masters, hiddenAfterReversal: true, finalUnallocated: 200 })
  }
  return evidence
})
if (admin && ids.warehouse) await test('PSI-03-WORKBENCH', async () => {
  const summary = await request('/psi/workbench/summary', { token: admin })
  const tenantId = sql(`SELECT tenant_id FROM psi_warehouse WHERE id=${ids.warehouse}`)
  assert.match(tenantId, /^\d+$/)
  // 后端测试 JVM 为 Asia/Shanghai，不能把 MySQL 容器的默认 UTC 日期当作业务日期。
  const today = sql("SELECT DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+08:00'))")
  assert.match(today, /^\d{4}-\d{2}-\d{2}$/)
  const day = `'${today}'`
  const money = [
    ['psi_receivable', "doc_status='OPEN' AND unwritten_amount>0", 'unwritten_amount', 'openReceivableCount', 'openReceivableAmount'],
    ['psi_payable', "doc_status='OPEN' AND unwritten_amount>0", 'unwritten_amount', 'openPayableCount', 'openPayableAmount'],
    ['psi_receipt', "doc_status='DRAFT'", 'amount', 'pendingReceiptCount', 'pendingReceiptAmount'],
    ['psi_bank_statement', "match_status='UNMATCHED'", 'amount', 'unmatchedBankCount', 'unmatchedBankAmount'],
  ]
  const close = (actual, expected, field) => assert.ok(Math.abs(Number(actual) - Number(expected)) < 0.000001,
    `${field}: API=${actual}, 数据库=${expected}`)
  for (const [table, condition, amount, countKey, amountKey] of money) {
    const [count, sum] = sql(`SELECT COUNT(*),COALESCE(SUM(${amount}),0) FROM ${table} WHERE deleted=0 AND tenant_id=${tenantId} AND ${condition}`).split('\t')
    assert.equal(Number(summary[countKey]), Number(count), countKey)
    close(summary[amountKey], sum, amountKey)
  }
  const month = `DATE_FORMAT(${day},'%Y-%m-01')`
  const span = `DATE_SUB(${day},INTERVAL 29 DAY)`
  for (const [kind, monthly, recent] of [['out', 'monthSale', 'last30Out'], ['in', 'monthPurchase', 'last30In']]) {
    for (const [begin, prefix] of [[month, monthly], [span, recent]]) {
      const condition = `deleted=0 AND tenant_id=${tenantId} AND doc_status='CONFIRMED' AND confirm_time>=${begin} AND confirm_time<DATE_ADD(${day},INTERVAL 1 DAY)`
      const [count, sum] = sql(`SELECT COUNT(*),COALESCE(SUM(tax_inclusive_amount),0) FROM psi_stock_${kind} WHERE ${condition}`).split('\t')
      assert.equal(Number(summary[`${prefix}Count`]), Number(count), `${prefix}Count`)
      close(summary[`${prefix}Amount`], sum, `${prefix}Amount`)
      const qty = sql(`SELECT COALESCE(SUM(quantity),0) FROM psi_stock_${kind}_item WHERE deleted=0 AND tenant_id=${tenantId}
        AND stock_${kind}_id IN (SELECT id FROM psi_stock_${kind} WHERE ${condition})`)
      close(summary[`${prefix}Qty`], qty, `${prefix}Qty`)
    }
  }
  return { fieldsChecked: 20, assertionSource: '隔离 MySQL 独立只读聚合', inventoryAndFinanceWrites: false }
})
if (admin && ids.goods) await test('PSI-IMPORT-BATCH-01', async () => {
  const code = sql(`SELECT goods_code FROM psi_goods WHERE id=${ids.goods}`)
  const heading = ['商品编号', '数量', '含税单价', '税率']
  function fixture(rows) {
    const generated = spawnSync('python', ['-X', 'utf8', 'scripts/quality/xlsx_fixture.py'], { input: JSON.stringify([heading, ...rows]) })
    assert.equal(generated.status, 0, generated.stderr.toString())
    const form = new FormData()
    form.append('file', new Blob([generated.stdout], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), 'synthetic.xlsx')
    return form
  }
  for (const path of ['/psi/stock-in/import/parse', '/psi/stock-out/import/parse']) {
    const parsed = await upload(path, fixture([[code.toLowerCase(), '1.2345', '2.50', '13']]))
    assert.equal(parsed.validRows, 1)
    assert.equal(String(parsed.items[0].goodsId), String(ids.goods))
    assert.equal(parsed.items[0].goodsCode, code)
    assert.equal(Number(parsed.items[0].quantity), 1.2345)
    assert.equal(Number(parsed.items[0].taxInclusivePrice), 2.5)
  }
  for (const rows of [[[code, '1'], [code, '2']], [[`${prefix}-missing`, '1']], [[code, '0']]]) {
    const r = await fetch(state.url + '/psi/stock-in/import/parse', { method: 'POST', headers: { Authorization: `Bearer ${admin}` }, body: fixture(rows), signal: AbortSignal.timeout(30000) })
    assert.equal((await r.json()).success, false)
  }
  return { directions: 2, databaseCaseMatching: true, exactQuantity: true, duplicateMissingAndZeroRejected: true, writes: false }
})
if (admin && other) await test('NOTICE-COUNT-01', async () => {
  const root = '/user/message'
  const beforeAdmin = await request(`${root}/unread?detail=true`, { token: admin })
  const beforeOther = await request(`${root}/unread?detail=true`, { token: other })
  const first = BigInt(sql('SELECT COALESCE(MAX(id),0)+100 FROM sys_message'))
  const messageIds = Array.from({ length: 4 }, (_, i) => String(first + BigInt(i)))
  sql(`INSERT INTO sys_message(id,title,type,scope,users,create_time) VALUES
    (${messageIds[0]},'${prefix}-broadcast',1,1,NULL,NOW()),
    (${messageIds[1]},'${prefix}-admin',2,2,JSON_ARRAY('${userId.admin}'),NOW()),
    (${messageIds[2]},'${prefix}-other',2,2,JSON_ARRAY('${userId.other}'),NOW()),
    (${messageIds[3]},'${prefix}-deleted',1,1,NULL,NOW());
    INSERT INTO sys_message_log(message_id,user_id,is_deleted) VALUES(${messageIds[3]},${userId.admin},1),(${messageIds[3]},${userId.other},1);`)
  const total = async (token, detail = true) => request(`${root}/unread?detail=${detail}`, { token })
  let a = await total(admin), b = await total(other)
  assert.equal(Number(a.total), Number(beforeAdmin.total) + 2)
  assert.equal(Number(b.total), Number(beforeOther.total) + 2)
  assert.equal(Number((await total(admin, false)).total), Number(a.total))
  for (const type of [1, 2]) assert.equal(Number(a.details.find(d => Number(d.type) === type).count), Number(beforeAdmin.details.find(d => Number(d.type) === type).count) + 1)
  await request(`${root}/read`, { method: 'PATCH', token: admin, data: { ids: [messageIds[0], messageIds[2]] } })
  await request(`${root}/read`, { method: 'PATCH', token: admin, data: { ids: [messageIds[0]] } })
  assert.equal(Number((await total(admin)).total), Number(beforeAdmin.total) + 1)
  assert.equal(Number((await total(other)).total), Number(beforeOther.total) + 2)
  assert.equal(sql(`SELECT COUNT(*) FROM sys_message_log WHERE message_id=${messageIds[2]} AND user_id=${userId.admin}`), '0')
  await request(root, { method: 'DELETE', token: admin, data: { ids: [messageIds[1]] } })
  assert.equal(Number((await total(admin)).total), Number(beforeAdmin.total))
  await request(`${root}/readAll`, { method: 'PATCH', token: admin })
  assert.equal(Number((await total(admin)).total), 0)
  assert.equal(Number((await total(other)).total), Number(beforeOther.total) + 2)
  const noticeBefore = await request(`${root}/notice/unread`, { token: admin })
  const noticeFirst = BigInt(sql('SELECT COALESCE(MAX(id),0)+100 FROM sys_notice'))
  const noticeIds = Array.from({ length: 4 }, (_, i) => String(noticeFirst + BigInt(i)))
  sql(`INSERT INTO sys_notice(id,title,content,type,notice_scope,notice_users,notice_methods,status,publish_time,create_user,create_time) VALUES
    (${noticeIds[0]},'${prefix}-notice','synthetic','1',1,NULL,JSON_ARRAY(1),3,NOW(),${userId.admin},NOW()),
    (${noticeIds[1]},'${prefix}-notice-own','synthetic','1',2,JSON_ARRAY('${userId.admin}'),JSON_ARRAY(1),3,NOW(),${userId.admin},NOW()),
    (${noticeIds[2]},'${prefix}-notice-other','synthetic','1',2,JSON_ARRAY('${userId.other}'),JSON_ARRAY(1),3,NOW(),${userId.admin},NOW()),
    (${noticeIds[3]},'${prefix}-notice-draft','synthetic','1',1,NULL,JSON_ARRAY(1),1,NOW(),${userId.admin},NOW());`)
  assert.equal(Number((await request(`${root}/notice/unread`, { token: admin })).total), Number(noticeBefore.total) + 2)
  await request(`${root}/notice/${noticeIds[0]}`, { token: admin })
  assert.equal(Number((await request(`${root}/notice/unread`, { token: admin })).total), Number(noticeBefore.total) + 1)
  return { messageRecipients: 2, groupedTotalsAndMissingTypes: true, foreignReadExcluded: true, readRetryReadAllAndPersonalDeletion: true, noticeRecipientAndPublishedState: true }
})
if (admin && ids.customer) await test('ENGINEER-PASSWORD-RECOVERY', async () => {
  const mobile = (path, options = {}) => request(path, { ...options, base: state.h5Url || state.url })
  const key = await mobile('/auth/gm/public-key')
  const encrypt = value => { const text = sm2.doEncrypt(value, key.publicKeyHex, 1); return text.startsWith('04') ? text : '04' + text }
  const initial = 'Aa3!' + randomBytes(10).toString('hex'), updated = 'Bb4!' + randomBytes(10).toString('hex')
  const username = `password_${state.runId}`
  const roleId = sql("SELECT id FROM sys_role WHERE code='eoms_engineer' AND deleted=0 LIMIT 1")
  const adminInfo = await mobile(`/system/user/${userId.admin}`, { token: admin })
  const created = await mobile('/system/user', { method: 'POST', token: admin, data: {
    username, nickname: '合成初始密码工程师', password: encrypt(initial), gender: 0, deptId: adminInfo.deptId, roleIds: [roleId], status: 1
  } })
  assert.ok(created.id); assert.equal(created.generated, false)
  const clientId = sql('SELECT client_id FROM sys_client WHERE status=1 ORDER BY id LIMIT 1')
  const signIn = async value => (await mobile('/auth/login', { method: 'POST', data: { username, password: encrypt(value), authType: 'ACCOUNT', clientId } })).token
  const token = await signIn(initial)
  assert.equal((await mobile('/auth/user/info', { token })).pwdExpired, true)
  const denied = await mobile('/eoms/engineer/tasks?page=1&size=10', { token, reject: true })
  assert.match(denied.msg, /密码已过期/)
  const profileDenied = await mobile('/eoms/engineer/profile', { token, reject: true }); assert.match(profileDenied.msg, /密码已过期/)
  await request('/eoms/repair/order', { method: 'POST', token: admin, data: { customerId: ids.customer, status: 'WAIT_ACCEPT', address: `${prefix}-password-site` } })
  const orderId = sql(`SELECT id FROM eoms_repair_order WHERE address='${prefix}-password-site' AND del_flag='0'`)
  assert.match(orderId, /^\d+$/)
  await request(`/eoms/repair/order/assign/${orderId}?engineerId=${created.id}`, { method: 'PUT', token: admin })
  const wrong = await mobile('/user/profile/password', { method: 'PATCH', token, data: { oldPassword: encrypt('Wrong1!synthetic'), newPassword: encrypt(updated) }, reject: true })
  assert.match(wrong.msg, /当前密码不正确/)
  await mobile('/user/profile/password', { method: 'PATCH', token, data: { oldPassword: encrypt(initial), newPassword: encrypt(updated) } })
  assert.equal(String((await mobile('/auth/user/info', { token, reject: true })).code), '401')
  const next = await signIn(updated)
  assert.equal((await mobile('/auth/user/info', { token: next })).pwdExpired, false)
  assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${orderId}`), 'WAIT_ACCEPT')
  await mobile(`/eoms/engineer/accept/${orderId}`, {method:'POST',token:next})
  const tasks = await mobile('/eoms/engineer/tasks?page=1&size=10&status=ACCEPTED', { token: next })
  assert.ok(tasks.list.some(order => String(order.id) === String(orderId)))
  const profile = await mobile('/eoms/engineer/profile', { token: next }); assert.equal(Number(profile.accepted), 1)
  assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${orderId}`), 'ACCEPTED')
  await mobile('/auth/logout', { method: 'POST', token: next })
  return { normalUserApi: true, realH5NginxProxy: Boolean(state.h5Url), expiredTasksAndProfileRejected: true, wrongOldPasswordRejected: true, selfServiceChangeAvailableWhileExpired: true, oldSessionRevoked: true, newLoginRestoresAssignedTasks: true, acceptedCount: 1, explicitEngineerAcceptanceVerified: true }
})
await test('EOMS-POOL-UPGRADE',async()=>{
  const database='ls_aegis_upgrade_'+state.runId
  sql(`CREATE DATABASE ${database} CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; CREATE TABLE ${database}.eoms_repair_apply(id bigint primary key,tenant_id bigint,status varchar(32),apply_time datetime,del_flag char(1),customer_id bigint,marker varchar(80)); INSERT INTO ${database}.eoms_repair_apply VALUES(1,0,'WAIT',NOW(),'0',1,'retained-application');`)
  for(let round=0;round<2;round++){
    const report=resolve('.local',`service-pool-upgrade-${state.runId}-${round}.json`)
    const run=spawnSync('python',['-X','utf8','scripts/apply-service-pool-upgrade.py','--container',state.mysql,'--database',database,'--apply','--report',report],{encoding:'utf8',timeout:90000})
    assert.equal(run.status,0,run.stderr);const result=JSON.parse(readFileSync(report,'utf8'));assert.equal(result.neededStatements,round===0?2:0);assert.equal(result.existingRowsAfter,1)
  }
  assert.equal(sql(`SELECT marker FROM ${database}.eoms_repair_apply WHERE id=1`),'retained-application')
  return {oldSchemaUpgrade:true,repeatNoOp:true,existingRowPreserved:true,additiveOnly:true}
})
if (ids.customer && ids.goods && ids.warehouse && engineer && other) await test('EOMS-POOL-MATERIALS', async () => {
  const oldTime = new Intl.DateTimeFormat('sv-SE', { timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false }).format(new Date(Date.now()-11*60*1000))
  const makeApply = async (suffix, applyTime) => create('/eoms/repair/apply', {
    customerId:ids.customer,contactName:'合成报修人',contactPhone:'13800000000',faultType:'设备故障',faultDesc:prefix+'-'+suffix,
    reportAddress:'合成报修地址', ...(applyTime ? {applyTime} : {})
  },'eoms_repair_apply','fault_desc',prefix+'-'+suffix)
  const young=await makeApply('pool-young')
  await request(`/eoms/engineer/pool/${young}/claim`,{method:'POST',token:engineer,reject:true})
  const open=await makeApply('pool-open',oldTime)
  const listed=await request('/eoms/engineer/pool?page=1&size=10',{token:engineer})
  assert.ok(listed.list.some(item=>String(item.applyId)===open));assert.ok(!listed.list.some(item=>String(item.applyId)===young))
  const item=listed.list.find(item=>String(item.applyId)===open);assert.equal(item.contactPhone,undefined);assert.equal(item.photos,undefined)
  await request('/eoms/engineer/pool?page=1&size=10',{token:admin,reject:true})
  const claim=async token=>{const r=await fetch((state.h5Url||state.url)+`/eoms/engineer/pool/${open}/claim`,{method:'POST',headers:{Authorization:`Bearer ${token}`},signal:AbortSignal.timeout(30000)});return r.json()}
  const outcomes=await Promise.all([claim(engineer),claim(other)])
  assert.equal(outcomes.filter(body=>body.success).length,1)
  const winner=outcomes[0].success?engineer:other,loser=winner===engineer?other:engineer
  const orderId=String(outcomes.find(body=>body.success).data.orderId)
  assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_order WHERE apply_id=${open} AND del_flag='0'`)),1)
  assert.equal(sql(`SELECT status FROM eoms_repair_apply WHERE id=${open}`),'ORDER')
  assert.equal(String((await request(`/eoms/engineer/pool/${open}/claim`,{method:'POST',token:winner})).orderId),orderId)
  await request(`/eoms/engineer/task/${orderId}`,{token:loser,reject:true})
  await request(`/eoms/repair/apply/cancel/${open}`,{method:'PUT',token:admin,reject:true})
  const detail=await request(`/eoms/engineer/task/${orderId}`,{token:winner});assert.equal(detail.order.reportAddress,'合成报修地址')
  await request(`/eoms/engineer/start/${orderId}?longitude=116.4&latitude=39.9`,{method:'POST',token:winner})
  const options=await request(`/eoms/engineer/task/${orderId}/material-options`,{token:winner})
  assert.ok(options.goods.some(g=>String(g.id)===String(ids.goods)))
  for(const good of options.goods){assert.equal(good.salePrice,undefined);assert.equal(good.purchasePrice,undefined)}
  await request(`/eoms/engineer/task/${orderId}/material-options`,{token:loser,reject:true})
  const record={repairOrderId:orderId,faultReason:'合成原因',checkProcess:'合成检测过程',solution:'更换测试配件',result:'合成修复结果',materials:[{materialId:ids.goods,warehouseId:ids.warehouse,quantity:1,remark:'合成安装说明'}]}
  const stock=sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`)
  await request('/eoms/engineer/record',{method:'POST',token:winner,data:{...record,materials:[{...record.materials[0],warehouseId:'9223372036854775806'}]},reject:true})
  assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_record WHERE repair_order_id=${orderId} AND del_flag='0'`)),0)
  assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_material WHERE repair_order_id=${orderId} AND del_flag='0'`)),0)
  await request('/eoms/engineer/record',{method:'POST',token:winner,data:record})
  await request('/eoms/engineer/record',{method:'POST',token:winner,data:record,reject:true})
  const materials=await request(`/eoms/engineer/task/${orderId}/materials`,{token:winner})
  assert.equal(materials.length,1);assert.equal(Number(materials[0].quantity),1);assert.equal(materials[0].salePrice,undefined);assert.equal(materials[0].costPrice,undefined)
  assert.equal(sql(`SELECT SUM(remain_quantity) FROM psi_stock_batch WHERE warehouse_id=${ids.warehouse} AND goods_id=${ids.goods}`),stock)
  await request(`/eoms/engineer/finish/${orderId}?longitude=116.4&latitude=39.9`,{method:'POST',token:winner})
  const completed=await request('/eoms/engineer/tasks?page=1&size=100&serviceCompleted=true',{token:winner})
  assert.ok(completed.list.some(task=>String(task.id)===orderId));assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${orderId}`),'WAIT_SETTLEMENT')
  // Compete with ordinary back-office operations, using only synthetic reports.
  const cancelApply=await makeApply('pool-cancel-race',oldTime)
  const raw=async(path,token,method)=>{const response=await fetch(state.url+path,{method,headers:{Authorization:`Bearer ${token}`},signal:AbortSignal.timeout(30000)});return response.json()}
  const cancelRace=await Promise.all([raw(`/eoms/engineer/pool/${cancelApply}/claim`,engineer,'POST'),raw(`/eoms/repair/apply/cancel/${cancelApply}`,admin,'PUT')])
  assert.equal(cancelRace.filter(body=>body.success).length,1)
  if(cancelRace[0].success){assert.equal(sql(`SELECT status FROM eoms_repair_apply WHERE id=${cancelApply}`),'ORDER');assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_order WHERE apply_id=${cancelApply} AND del_flag='0'`)),1)}
  else {assert.equal(sql(`SELECT status FROM eoms_repair_apply WHERE id=${cancelApply}`),'CANCEL');assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_order WHERE apply_id=${cancelApply} AND del_flag='0'`)),0)}
  const dispatchApply=await makeApply('pool-dispatch-race',oldTime)
  await request(`/eoms/repair/apply/confirm/${dispatchApply}`,{method:'PUT',token:admin})
  await request(`/eoms/repair/apply/toOrder/${dispatchApply}`,{method:'PUT',token:admin,data:{}})
  const dispatchOrder=sql(`SELECT id FROM eoms_repair_order WHERE apply_id=${dispatchApply} AND del_flag='0'`)
  const dispatchRace=await Promise.all([raw(`/eoms/engineer/pool/${dispatchApply}/claim`,other,'POST'),raw(`/eoms/repair/order/assign/${dispatchOrder}?engineerId=${userId.engineer}`,admin,'PUT')])
  assert.equal(dispatchRace[0].success,true)
  assert.equal(sql(`SELECT engineer_id FROM eoms_repair_order WHERE id=${dispatchOrder}`),userId.other)
  assert.equal(sql(`SELECT status FROM eoms_repair_order WHERE id=${dispatchOrder}`),'ACCEPTED')
  assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_repair_order WHERE apply_id=${dispatchApply} AND del_flag='0'`)),1)
  return {beforeTenMinutesRejected:true,unconfirmedAutoEligible:true,concurrentClaimWinners:1,claimVsCancelSingleWinner:true,claimVsDispatchKeepsActualAcceptingEngineer:true,retryCreatesNoDuplicates:true,contactsHiddenBeforeClaim:true,loserCannotReadTask:true,cancelAfterClaimRejected:true,reportAddressPreserved:true,invalidWarehouseRollsBackRecordAndMaterials:true,materialRetryRejected:true,noPrematureStockChange:true,engineerCompletedRetainsBackendSettlement:true}
})
for (const id of ['EOMS-POOL-UPGRADE', 'EOMS-POOL-MATERIALS', 'ENGINEER-PASSWORD-RECOVERY', 'AUTH-01', 'AUTH-02-CONTEXT-ISOLATION', 'FIXTURE-01', 'PSI-01', 'PSI-02', 'PSI-BANK-01', 'PSI-CASH-SUMMARY-01', 'PSI-03-WORKBENCH', 'PSI-IMPORT-BATCH-01', 'NOTICE-COUNT-01', 'QUOTE-01', 'QUOTE-02-INDEPENDENT',
  'EOMS-01', 'EOMS-02-X-01', 'EOMS-03-STOCK-ROLLBACK', 'SIGN-01-AUTH', 'SIGN-01-SIGNATURE', 'EOMS-01-CUSTOMER-H5']) {
  if (!results.some(r => r.id === id)) blocked(id, '上游测试数据或身份前置未完成')
}
writeFileSync(resolve('.local/business-results.json'), JSON.stringify({ at: new Date().toISOString(), runId: state.runId, prefix,
  environment: state.url, isolation: 'dedicated MySQL/Redis/backend', results }, null, 2))
console.log(`业务测试：${results.filter(r => r.status === 'PASS').length} PASS / ${results.filter(r => r.status === 'FAIL').length} FAIL / ${results.filter(r => r.status === 'BLOCKED').length} BLOCKED`)
process.exitCode = results.some(r => r.status === 'FAIL') ? 1 : results.some(r => r.status === 'BLOCKED') ? 2 : 0
