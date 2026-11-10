/** PSI master-data workflow; no inventory or accounting postings. */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const { WorkflowClient } = require('./business_workflow_client.cjs')
const c = new WorkflowClient(process.argv[2])
const file = path.join(c.dir, 'psi-fixtures.json')
const s = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : {
  runId: c.scope.runId, prefix: 'ITLS-TEST-' + c.tag + '-' + crypto.randomBytes(5).toString('hex'), objects: {},
}
assert.equal(s.runId, c.scope.runId)
const save = () => c.save('psi-fixtures.json', s)
const rows = d => d.list || d.records || []
const flatten = xs => xs.flatMap(x => [x, ...flatten(x.children || [])])
const types = {
  customer: { base: '/psi/unit', field: 'unitName', suffix: '-customer' },
  supplier: { base: '/psi/unit', field: 'unitName', suffix: '-supplier' },
  parent: { base: '/psi/goods-category', field: 'name', suffix: '-A' },
  category: { base: '/psi/goods-category', field: 'name', suffix: '-Z' },
  goods: { base: '/psi/goods', field: 'goodsName', suffix: '-goods' },
  warehouse: { base: '/psi/warehouse', field: 'warehouseName', suffix: '-warehouse' },
}
async function find(kind) {
  const t = types[kind]
  const all = kind === 'parent' || kind === 'category'
    ? flatten(await c.call('GET', t.base + '/tree'))
    : rows(await c.call('GET', t.base + '?size=100&keyword=' + s.prefix))
  const found = all.filter(x => x[t.field] === s.prefix + t.suffix)
  assert.ok(found.length <= 1, 'Ambiguous fixture ownership')
  if (s.objects[kind]) for (const x of found) assert.equal(String(x.id), s.objects[kind])
  return found
}
async function create(kind, data) {
  const t = types[kind]
  assert.equal((await find(kind)).length, 0)
  const req = { ...data, [t.field]: s.prefix + t.suffix }
  const r = await c.call('POST', t.base, req)
  const id = String(r?.id || r); s.objects[kind] = id; save(); assert.match(id, /^\d+$/)
  const detail = await c.call('GET', t.base + '/' + id)
  assert.equal(detail[t.field], req[t.field]); return { id, detail, req }
}
async function cleanup() {
  for (const kind of ['goods', 'warehouse', 'customer', 'supplier', 'category', 'parent']) {
    for (const d of await find(kind)) {
      await c.call('DELETE', types[kind].base, { ids: [String(d.id)] })
    }
    assert.equal((await find(kind)).length, 0)
  }
  s.cleaned = true; save(); return { remainingMasterObjects: 0, inventoryPostings: 0, accountingPostings: 0 }
}
async function main() {
  save()
  if (process.argv.includes('--cleanup-only')) return c.step('F08-CLEANUP', 'Delete only owned PSI master data in dependency order', cleanup)
  assert.equal(Object.keys(s.objects).length, 0, 'Existing ledger requires cleanup-only')
  const current = await c.call('GET', '/auth/user/info')
  try {
    await c.step('F08-01', 'Create dedicated customer and supplier; verify type persistence', async () => {
      const customer = await create('customer', { unitType: 'CUSTOMER', unitNature: 'ENTERPRISE', status: 1, remark: s.runId })
      const supplier = await create('supplier', { unitType: 'SUPPLIER', unitNature: 'ENTERPRISE', status: 1, remark: s.runId })
      assert.equal(customer.detail.unitType, 'CUSTOMER'); assert.equal(supplier.detail.unitType, 'SUPPLIER')
      return { customerId: customer.id, supplierId: supplier.id }
    })
    await c.step('F08-02', 'Customer contact add-edit-delete preserves owning customer', async () => {
      const base = '/psi/unit/' + s.objects.customer + '/contacts'
      const req = { contactName: s.prefix, position: 'workflow', isDefault: 1, remark: s.runId }
      const r = await c.call('POST', base, req)
      const list = await c.call('GET', base); const contact = list.find(x => x.contactName === s.prefix)
      assert.ok(contact); s.contactId = String(contact.id || r?.id || r); save()
      await c.call('PUT', base + '/' + s.contactId, { ...req, position: 'updated' })
      const edited = (await c.call('GET', base)).find(x => String(x.id) === s.contactId)
      assert.equal(edited.position, 'updated')
      await c.call('DELETE', base + '/' + s.contactId)
      assert.ok(!(await c.call('GET', base)).some(x => String(x.id) === s.contactId))
      return { contactId: s.contactId, edited: true, removed: true }
    })
    await c.step('F08-03', 'Create two-level categories with generated codes and linked product', async () => {
      const p = await create('parent', { parentId: '0', status: 1, sort: 1 })
      const ch = await create('category', { parentId: p.id, status: 1, sort: 1 })
      assert.ok(p.detail.code && ch.detail.code); assert.equal(String(ch.detail.parentId), p.id)
      const g = await create('goods', { unit: '件', defaultTaxRate: 13, categoryId: ch.id, status: 1,
        purchasePrice: 10, salePrice: 20, minSalePrice: 12, stockLower: 0, stockUpper: 100, remark: s.runId })
      assert.equal(String(g.detail.categoryId), ch.id); assert.equal(Number(g.detail.defaultTaxRate), 13)
      return { parentId: p.id, categoryId: ch.id, goodsId: g.id, parentCode: p.detail.code, categoryCode: ch.detail.code }
    })
    await c.step('F08-04', 'Categories with children/products cannot be deleted', async () => {
      for (const [kind, message] of [['parent', /下级分类/], ['category', /仍有商品/]]) {
        const r = await c.raw('DELETE', '/psi/goods-category', { ids: [s.objects[kind]] })
        assert.equal(r.body.success, false); assert.match(r.body.msg, message)
      }
      return { childGuarded: true, productGuarded: true }
    })
    await c.step('F08-05', 'Create nondefault warehouse with resolved manager and editable address', async () => {
      const w = await create('warehouse', { managerUserId: String(current.id), isDefault: false, status: 1, remark: s.runId })
      assert.equal(String(w.detail.managerUserId), String(current.id)); assert.equal(w.detail.isDefault, false)
      await c.call('PUT', '/psi/warehouse/' + w.id, { ...w.req, address: s.prefix + '-address' })
      const d = await c.call('GET', '/psi/warehouse/' + w.id); assert.equal(d.address, s.prefix + '-address')
      return { warehouseId: w.id, managerResolved: true, defaultWarehouseChanged: false, addressPersisted: true }
    })
  } finally { await c.step('F08-CLEANUP', 'Delete only owned PSI master data in dependency order', cleanup) }
}
main().catch(e => { console.error(String(e.message).slice(0, 220)); process.exitCode = 1 })
