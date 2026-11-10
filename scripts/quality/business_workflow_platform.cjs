/** Owned department hierarchy and dictionary lifecycle against local in-use API. */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const { WorkflowClient } = require('./business_workflow_client.cjs')
const c = new WorkflowClient(process.argv[2])
const file = path.join(c.dir, 'platform-fixtures.json')
const s = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : {
  runId: c.scope.runId, prefix: 'ITLS-TEST-' + c.tag + '_' + crypto.randomBytes(3).toString('hex'),
  code: 'itls_' + c.tag.toLowerCase() + '_' + crypto.randomBytes(4).toString('hex'),
}
assert.equal(s.runId, c.scope.runId)
const save = () => c.save('platform-fixtures.json', s)
const flatten = xs => xs.flatMap(x => [x, ...flatten(x.children || [])])
const rows = x => x.list || x.records || []
async function owned() {
  const depts = flatten(await c.call('GET', '/system/dept/tree')).filter(x => x.name?.startsWith(s.prefix))
  const dicts = (await c.call('GET', '/system/dict/list?code=' + s.code)).filter(x => x.code === s.code)
  assert.ok(depts.length <= 2 && dicts.length <= 1)
  return { depts, dicts }
}
async function cleanup() {
  const found = await owned()
  for (const d of found.dicts) {
    const detail = await c.call('GET', '/system/dict/' + d.id); assert.equal(detail.description, s.runId)
    await c.call('DELETE', '/system/dict', { ids: [String(d.id)] })
    const items = await c.call('GET', '/system/dict/item?dictId=' + d.id)
    assert.equal(rows(items).length, 0)
  }
  for (const d of found.depts.sort((a, b) => b.name.localeCompare(a.name))) {
    const detail = await c.call('GET', '/system/dept/' + d.id); assert.equal(detail.description, s.runId)
    await c.call('DELETE', '/system/dept', { ids: [String(d.id)] })
  }
  const after = await owned(); assert.equal(after.depts.length, 0); assert.equal(after.dicts.length, 0)
  const cached = await c.call('GET', '/system/common/dict/' + s.code); assert.equal(cached.length, 0)
  s.cleaned = true; save(); return { remainingDepts: 0, remainingDicts: 0, cachedItems: 0 }
}
async function main() {
  save()
  if (process.argv.includes('--cleanup-only')) return c.step('F03-CLEANUP', 'Compensate owned hierarchy and dictionary; verify cache empty', cleanup)
  assert.ok(!s.parentId && !s.dictId, 'Use cleanup-only for an existing run')
  const before = await owned(); assert.equal(before.depts.length + before.dicts.length, 0)
  const me = await c.call('GET', '/auth/user/info')
  try {
    const parent = { parentId: String(me.deptId), name: s.prefix + '_A', sort: 1, description: s.runId, status: 1 }
    await c.step('F03-01', 'Create parent-child departments and verify tree ancestry', async () => {
      const p = await c.call('POST', '/system/dept', parent); s.parentId = String(p.id || p); save()
      const ch = await c.call('POST', '/system/dept', { ...parent, parentId: s.parentId, name: s.prefix + '_Z' })
      s.childId = String(ch.id || ch); save()
      const d = await c.call('GET', '/system/dept/' + s.childId); assert.equal(String(d.parentId), s.parentId)
      const tree = flatten(await c.call('GET', '/system/dept/tree')).find(x => String(x.id) === s.parentId)
      assert.ok(flatten(tree.children || []).some(x => String(x.id) === s.childId))
      return { parentId: s.parentId, childId: s.childId, treeLinked: true }
    })
    await c.step('F03-02', 'Department with child cannot be deleted', async () => {
      const r = await c.raw('DELETE', '/system/dept', { ids: [s.parentId] })
      assert.equal(r.body.success, false); assert.match(r.body.msg, /下级部门/)
      return { message: r.body.msg }
    })
    await c.step('F03-03', 'Rename synthetic department and persist hierarchy', async () => {
      await c.call('PUT', '/system/dept/' + s.parentId, { ...parent, name: s.prefix + '_Aedit' })
      const d = await c.call('GET', '/system/dept/' + s.parentId); assert.equal(d.name, s.prefix + '_Aedit')
      const ch = await c.call('GET', '/system/dept/' + s.childId); assert.equal(String(ch.parentId), s.parentId)
      return { editPersisted: true, childRetained: true }
    })
    const dict = { name: s.prefix + '_dict', code: s.code, description: s.runId }
    await c.step('F03-04', 'Create dictionary and active item visible through common dictionary API', async () => {
      const d = await c.call('POST', '/system/dict', dict); s.dictId = String(d.id || d); save()
      const item = await c.call('POST', '/system/dict/item', { label: 'Initial', value: 'one', sort: 1, status: 1,
        dictId: s.dictId, description: s.runId, color: 'blue' })
      s.itemId = String(item.id || item); save()
      const list = await c.call('GET', '/system/common/dict/' + s.code)
      assert.ok(list.some(x => x.label === 'Initial' && x.value === 'one'))
      return { dictId: s.dictId, itemId: s.itemId, commonLookupVisible: true }
    })
    await c.step('F03-05', 'Dictionary item edit invalidates existing cached label', async () => {
      await c.call('PUT', '/system/dict/item/' + s.itemId, { label: 'Updated', value: 'one', sort: 1, status: 1,
        dictId: s.dictId, description: s.runId, color: 'blue' })
      const list = await c.call('GET', '/system/common/dict/' + s.code)
      assert.ok(list.some(x => x.label === 'Updated')); assert.ok(!list.some(x => x.label === 'Initial'))
      return { updatedLabelVisible: true, oldLabelAbsent: true }
    })
    await c.step('F03-06', 'Dictionary code edit is rejected without changing original code', async () => {
      const r = await c.raw('PUT', '/system/dict/' + s.dictId, { ...dict, code: s.code + 'x' })
      assert.equal(r.body.success, false); assert.match(r.body.msg, /不允许修改字典编码/)
      const d = await c.call('GET', '/system/dict/' + s.dictId); assert.equal(d.code, s.code)
      return { immutableCodeGuarded: true }
    })
  } finally { await c.step('F03-CLEANUP', 'Compensate owned hierarchy and dictionary; verify cache empty', cleanup) }
}
main().catch(e => { console.error(String(e.message).slice(0, 220)); process.exitCode = 1 })
