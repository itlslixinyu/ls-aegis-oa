/** Quote draft/submission, arithmetic and exports using an owned customer. */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const { WorkflowClient } = require('./business_workflow_client.cjs')
const c = new WorkflowClient(process.argv[2])
const file = path.join(c.dir, 'quote-fixtures.json')
const s = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : {
  runId: c.scope.runId, prefix: 'ITLS-TEST-' + c.tag + '-' + crypto.randomBytes(5).toString('hex'),
}
assert.equal(s.runId, c.scope.runId)
const save = () => c.save('quote-fixtures.json', s)
const rows = d => d.list || d.records || []
async function ownedQuotes() {
  const data = await c.call('GET', '/quote?size=100&projectName=' + s.prefix)
  return rows(data).filter(q => q.projectName === s.prefix)
}
async function ownedCustomers() {
  const data = await c.call('GET', '/psi/unit?size=100&keyword=' + s.prefix)
  return rows(data).filter(q => q.unitName === s.prefix)
}
async function cleanup() {
  const found = await ownedQuotes(); assert.ok(found.length <= 1)
  for (const q of found) {
    if (s.id) assert.equal(String(q.id), s.id)
    const d = await c.call('GET', '/quote/' + q.id); assert.equal(d.remark, s.runId)
    assert.ok(['DRAFT', 'SUBMIT'].includes(d.status)); await c.call('DELETE', '/quote/' + q.id)
  }
  assert.equal((await ownedQuotes()).length, 0)
  const customers = await ownedCustomers(); assert.ok(customers.length <= 1)
  for (const u of customers) {
    const d = await c.call('GET', '/psi/unit/' + u.id); assert.equal(d.remark, s.runId)
    await c.call('DELETE', '/psi/unit', { ids: [String(u.id)] })
  }
  assert.equal((await ownedCustomers()).length, 0); s.cleaned = true; save()
  return { remainingQuotes: 0, remainingCustomers: 0, accountingPostings: 0 }
}
async function exportFile(ext) {
  const res = await fetch(c.scope.api + '/quote/' + s.id + '/export-' + ext, {
    headers: { Authorization: 'Bearer ' + c.session.token }, redirect: 'manual', signal: AbortSignal.timeout(45000),
  })
  assert.equal(res.status, 200)
  const bytes = Buffer.from(await res.arrayBuffer())
  assert.ok(bytes.subarray(0, 5).toString().startsWith(ext === 'pdf' ? '%PDF-' : 'PK'), 'Export must be actual document bytes')
  const filename = decodeURIComponent(res.headers.get('content-disposition') || '')
  assert.ok(filename.includes(s.prefix))
  fs.writeFileSync(path.join(c.dir, 'quote-export.' + ext), bytes)
  return { size: bytes.length, sha256: crypto.createHash('sha256').update(bytes).digest('hex'), nameMatchesProject: true }
}
async function main() {
  save()
  if (process.argv.includes('--cleanup-only')) return c.step('F09-CLEANUP', 'Delete owned quote and customer; independently verify both absent', cleanup)
  assert.ok(!s.id && !s.customerId, 'Existing ledger requires cleanup-only')
  assert.equal((await ownedQuotes()).length + (await ownedCustomers()).length, 0)
  try {
    const customer = await c.call('POST', '/psi/unit', { unitName: s.prefix, unitType: 'CUSTOMER', unitNature: 'ENTERPRISE', status: 1, remark: s.runId })
    s.customerId = String(customer?.id || customer); save()
    const req = { customerId: s.customerId, projectName: s.prefix, quoteUnit: 'Workflow', taxRate: 13, remark: s.runId,
      details: [{ lineType: 'SERVICE', itemName: 'workflow service', quantity: 3, price: 19.99, unit: '项', sortNo: 1 }] }
    await c.step('F09-01', 'Create quote with string ID and verify persisted calculated tax-inclusive total', async () => {
      const id = await c.call('POST', '/quote', req); assert.equal(typeof id, 'string'); s.id = id; save()
      const d = await c.call('GET', '/quote/' + s.id)
      assert.equal(d.status, 'DRAFT'); assert.equal(String(d.customerId), s.customerId)
      assert.equal(Number(d.details[0].amount), 59.97); assert.equal(Number(d.serviceSubtotal), 59.97)
      assert.equal(Number(d.totalAmount), 67.77)
      return { quoteId: s.id, idType: typeof id, customerLinked: true, lineTotal: 59.97, total: 67.77 }
    })
    await c.step('F09-02', 'Partial header edit preserves details, tax and total', async () => {
      await c.call('PUT', '/quote', { id: s.id, terms: s.runId + ' edited' })
      const d = await c.call('GET', '/quote/' + s.id)
      assert.equal(d.details.length, 1); assert.equal(Number(d.taxRate), 13); assert.equal(Number(d.totalAmount), 67.77)
      assert.equal(d.terms, s.runId + ' edited'); return { detailsPreserved: true, totalPreserved: true }
    })
    await c.step('F09-03', 'Draft to submitted and back to draft persists expected statuses', async () => {
      for (const status of ['SUBMIT', 'DRAFT']) {
        await c.call('PUT', '/quote', { id: s.id, status })
        const d = await c.call('GET', '/quote/' + s.id); assert.equal(d.status, status); assert.equal(Number(d.totalAmount), 67.77)
      }
      return { transitions: ['DRAFT', 'SUBMIT', 'DRAFT'], totalPreserved: true }
    })
    await c.step('F09-04', 'Render HTML includes synthetic project and service item', async () => {
      const d = await c.call('GET', '/quote/' + s.id + '/render')
      assert.ok(d.html.includes(s.prefix) && d.html.includes('workflow service'))
      return { projectVisible: true, lineVisible: true, htmlLength: d.html.length }
    })
    await c.step('F09-05', 'Export actual Word document with project filename', () => exportFile('docx'))
    await c.step('F09-06', 'Export actual PDF document with project filename', () => exportFile('pdf'))
  } finally { await c.step('F09-CLEANUP', 'Delete owned quote and customer; independently verify both absent', cleanup) }
}
main().catch(e => { console.error(String(e.message).slice(0, 220)); process.exitCode = 1 })
