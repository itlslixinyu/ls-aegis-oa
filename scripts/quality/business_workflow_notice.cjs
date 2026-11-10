/** Draft-only notice lifecycle: no publication, recipients or external messaging. */
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const { WorkflowClient } = require('./business_workflow_client.cjs')
const c = new WorkflowClient(process.argv[2])
const file = path.join(c.dir, 'notice-fixtures.json')
const s = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : {
  runId: c.scope.runId, title: 'ITLS-TEST-' + c.tag + '-DRAFT-' + crypto.randomBytes(5).toString('hex'),
}
assert.equal(s.runId, c.scope.runId)
const save = () => c.save('notice-fixtures.json', s)
const rows = d => d.list || d.records || []
async function find() {
  return rows(await c.call('GET', '/system/notice?size=20&title=' + s.title)).filter(n => n.title === s.title)
}
async function cleanup() {
  const found = await find(); assert.ok(found.length <= 1)
  for (const n of found) {
    if (s.id) assert.equal(String(n.id), s.id)
    const d = await c.call('GET', '/system/notice/' + n.id)
    assert.equal(d.status, 1); assert.ok(d.content.includes(s.runId))
    await c.call('DELETE', '/system/notice', { ids: [String(n.id)] })
  }
  assert.equal((await find()).length, 0); s.cleaned = true; save()
  return { remainingDrafts: 0, publicationInvoked: false }
}
async function main() {
  save()
  if (process.argv.includes('--cleanup-only')) return c.step('F05-CLEANUP', 'Delete only owned draft and verify absence', cleanup)
  assert.ok(!s.id, 'Existing ledger: cleanup-only then new run')
  assert.equal((await find()).length, 0)
  const types = await c.call('GET', '/system/common/dict/notice_type'); assert.ok(types.length)
  const req = { title: s.title, content: '<p>' + s.runId + ' initial</p>', type: String(types[0].value),
    noticeScope: 1, noticeUsers: [], noticeMethods: [], isTiming: false, isTop: false, status: 1 }
  try {
    await c.step('F05-01', 'Create draft without publishing and verify detail', async () => {
      const r = await c.call('POST', '/system/notice', req); s.id = String(r.id || r); save()
      const d = await c.call('GET', '/system/notice/' + s.id)
      assert.equal(d.status, 1); assert.equal(d.title, s.title); assert.ok(d.content.includes(s.runId))
      return { noticeId: s.id, status: d.status, published: false }
    })
    await c.step('F05-02', 'Draft is excluded from user-facing published notice list', async () => {
      const d = await c.call('GET', '/user/message/notice?title=' + s.title)
      assert.equal(rows(d).filter(x => String(x.id) === s.id).length, 0)
      return { userFacingMatches: 0 }
    })
    await c.step('F05-03', 'Edit draft content without changing draft state', async () => {
      req.content = '<p>' + s.runId + ' updated</p>'
      await c.call('PUT', '/system/notice/' + s.id, req)
      const d = await c.call('GET', '/system/notice/' + s.id)
      assert.equal(d.status, 1); assert.ok(d.content.includes('updated'))
      return { updated: true, remainedDraft: true }
    })
  } finally { await c.step('F05-CLEANUP', 'Delete only owned draft and verify absence', cleanup) }
}
main().catch(e => { console.error(String(e.message).slice(0, 220)); process.exitCode = 1 })
