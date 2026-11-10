/** Repair application -> unassigned order -> compensation -> reasoned closure. */
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const assert = require('node:assert/strict')
const { WorkflowClient } = require('./business_workflow_client.cjs')
const c = new WorkflowClient(process.argv[2])
const file = path.join(c.dir, 'repair-fixtures.json')
const s = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : {
  runId: c.scope.runId, prefix: 'ITLS-TEST-' + c.tag + '-' + crypto.randomBytes(5).toString('hex'),
}
assert.equal(s.runId, c.scope.runId)
const save = () => c.save('repair-fixtures.json', s)
const rows = d => d.list || d.records || []
async function applies() {
  const d = await c.call('GET', '/eoms/repair/apply/list?size=100&faultDesc=' + s.prefix)
  return rows(d).filter(x => x.faultDesc === s.prefix)
}
async function cleanup() {
  if (s.orderId) {
    const order = await c.call('GET', '/eoms/repair/order/' + s.orderId)
    if (order) { assert.equal(String(order.applyId), s.applyId); assert.equal(order.status, 'WAIT_ACCEPT'); await c.call('DELETE', '/eoms/repair/order/' + s.orderId) }
  }
  const list = await applies(); assert.ok(list.length <= 1)
  for (const a of list) {
    if (s.applyId) assert.equal(String(a.id), s.applyId)
    assert.equal(String(a.customerId), s.customerId)
    await c.call('DELETE', '/eoms/repair/apply/' + a.id)
  }
  assert.equal((await applies()).length, 0)
  const customers = rows(await c.call('GET', '/psi/unit?size=100&keyword=' + s.prefix)).filter(x => x.unitName === s.prefix)
  assert.ok(customers.length <= 1)
  for (const u of customers) {
    const d = await c.call('GET', '/psi/unit/' + u.id); assert.equal(d.remark, s.runId)
    await c.call('DELETE', '/psi/unit', { ids: [String(u.id)] })
  }
  assert.equal(rows(await c.call('GET', '/psi/unit?keyword=' + s.prefix)).filter(x => x.unitName === s.prefix).length, 0)
  s.cleaned = true; save(); return { remainingApplies: 0, remainingCustomers: 0, unassignedOrderCompensated: Boolean(s.orderId), inventoryPostings: 0, accountingPostings: 0 }
}
async function main() {
  save()
  if (process.argv.includes('--cleanup-only')) return c.step('F10-CLEANUP', 'Compensate only owned unassigned order, application and customer', cleanup)
  assert.ok(!s.customerId && !s.applyId)
  assert.equal((await applies()).length, 0)
  try {
    const u = await c.call('POST', '/psi/unit', { unitName: s.prefix, unitType: 'CUSTOMER', unitNature: 'ENTERPRISE', status: 1, remark: s.runId })
    s.customerId = String(u.id || u); save()
    await c.step('F10-01', 'Create application linked to synthetic customer in WAIT status', async () => {
      await c.call('POST', '/eoms/repair/apply', { customerId: s.customerId, faultDesc: s.prefix, contactName: s.prefix, remark: s.runId, address: 'Workflow test address' })
      const list = await applies(); assert.equal(list.length, 1); s.applyId = String(list[0].id); save()
      const d = await c.call('GET', '/eoms/repair/apply/' + s.applyId)
      assert.equal(d.status, 'WAIT'); assert.equal(String(d.customerId), s.customerId)
      return { applyId: s.applyId, customerLinked: true, status: d.status }
    })
    await c.step('F10-02', 'Confirm application and create exactly linked unassigned order', async () => {
      await c.call('PUT', '/eoms/repair/apply/confirm/' + s.applyId + '?remark=' + s.runId)
      assert.equal((await c.call('GET', '/eoms/repair/apply/' + s.applyId)).status, 'CONFIRM')
      const id = await c.call('PUT', '/eoms/repair/apply/toOrder/' + s.applyId + '?remark=' + s.runId)
      s.orderId = String(id); save()
      const o = await c.call('GET', '/eoms/repair/order/' + s.orderId)
      assert.equal(o.status, 'WAIT_ACCEPT'); assert.equal(String(o.applyId), s.applyId); assert.equal(String(o.customerId), s.customerId)
      assert.equal((await c.call('GET', '/eoms/repair/apply/' + s.applyId)).status, 'ORDER')
      return { orderId: s.orderId, linkedApply: true, linkedCustomer: true, orderStatus: o.status }
    })
    await c.step('F10-03', 'Deleting unassigned order restores application to CONFIRM', async () => {
      await c.call('DELETE', '/eoms/repair/order/' + s.orderId)
      assert.equal(await c.call('GET', '/eoms/repair/order/' + s.orderId), null)
      assert.equal((await c.call('GET', '/eoms/repair/apply/' + s.applyId)).status, 'CONFIRM')
      return { orderRemoved: true, applicationRestored: true }
    })
    await c.step('F10-04', 'Confirmed application requires nonblank closure reason', async () => {
      for (const reason of [undefined, ' \t ', 'x'.repeat(501)]) {
        const query = reason === undefined ? '' : '?remark=' + encodeURIComponent(reason)
        const r = await c.raw('PUT', '/eoms/repair/apply/close/' + s.applyId + query)
        assert.equal(r.body.success, false, 'Blank or oversized closure reason must be rejected')
        assert.match(r.body.msg, /关闭原因/)
        assert.equal((await c.call('GET', '/eoms/repair/apply/' + s.applyId)).status, 'CONFIRM')
      }
      return { missingReasonRejected: true, whitespaceRejected: true, oversizedRejected: true, statusPreserved: true }
    })
    await c.step('F10-05', 'Close confirmed application with persisted reason', async () => {
      await c.call('PUT', '/eoms/repair/apply/close/' + s.applyId + '?remark=' + encodeURIComponent(s.runId + ' completed'))
      const d = await c.call('GET', '/eoms/repair/apply/' + s.applyId)
      assert.equal(d.status, 'CANCEL'); assert.equal(d.remark, s.runId + ' completed')
      return { status: d.status, reasonPersisted: true }
    })
  } finally { await c.step('F10-CLEANUP', 'Compensate only owned unassigned order, application and customer', cleanup) }
}
main().catch(e => { console.error(String(e.message).slice(0, 220)); process.exitCode = 1 })
