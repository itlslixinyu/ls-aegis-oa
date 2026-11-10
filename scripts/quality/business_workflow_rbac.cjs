/** Real user/role lifecycle; never changes an existing user's permissions. */
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')
const { WorkflowClient } = require('./business_workflow_client.cjs')
const dir = process.argv[2]
const c = new WorkflowClient(dir)
const stateFile = path.join(c.dir, 'rbac-fixtures.json')
const suffix = crypto.randomBytes(4).toString('hex')
const state = fs.existsSync(stateFile) ? JSON.parse(fs.readFileSync(stateFile)) : {
  runId: c.scope.runId, username: 'itls_' + c.tag.toLowerCase() + '_' + suffix, roleCode: 'itls_' + c.tag.toLowerCase() + '_' + suffix,
  roleName: 'ITLS-TEST-' + c.tag + '_ROLE' + suffix, userDeleted: false, roleDeleted: false,
}
assert.equal(state.runId, c.scope.runId)
function save() { c.save('rbac-fixtures.json', state) }
async function lookup() {
  const roles = await c.call('GET', '/system/role/list?code=' + encodeURIComponent(state.roleCode))
  const users = await c.call('GET', '/system/user/list?username=' + encodeURIComponent(state.username))
  return { roles: roles.filter(x => x.code === state.roleCode), users: users.filter(x => x.username === state.username) }
}
async function cleanup() {
  // Recover ambiguous write responses by unique ownership markers, never arbitrary IDs.
  const found = await lookup()
  assert.ok(found.users.length <= 1 && found.roles.length <= 1)
  if (found.users.length) {
    const u = await c.call('GET', '/system/user/' + found.users[0].id)
    assert.equal(u.username, state.username); assert.equal(u.description, state.runId)
    await c.call('DELETE', '/system/user', { ids: [String(u.id)] })
    state.userDeleted = true; save()
  }
  if (found.roles.length) {
    const r = await c.call('GET', '/system/role/' + found.roles[0].id)
    assert.equal(r.code, state.roleCode); assert.equal(r.description, state.runId)
    await c.call('DELETE', '/system/role', { ids: [String(r.id)] })
    state.roleDeleted = true; save()
  }
  const after = await lookup(); assert.equal(after.users.length, 0); assert.equal(after.roles.length, 0)
  await c.call('GET', '/auth/user/info')
  return { remainingUsers: 0, remainingRoles: 0, administratorStillValid: true }
}
async function main() {
  save()
  if (process.argv.includes('--cleanup-only')) return c.step('F02-CLEANUP', 'Remove only owned fixtures and independently verify absence', cleanup)
  assert.ok(!state.userId && !state.roleId, 'Existing fixture ledger: use cleanup-only, then a new run')
  try {
    const current = await c.call('GET', '/auth/user/info')
    const before = await lookup(); assert.equal(before.users.length, 0); assert.equal(before.roles.length, 0)
    const role = { name: state.roleName, code: state.roleCode, sort: 1, description: state.runId,
      dataScope: 4, deptIds: [], deptCheckStrictly: false }
    await c.step('F02-01', 'Create dedicated SELF role without granting menu permissions', async () => {
      const response = await c.call('POST', '/system/role', role)
      state.roleId = String(response?.id || response); save(); assert.match(state.roleId, /^\d+$/)
      const detail = await c.call('GET', '/system/role/' + state.roleId)
      assert.equal(detail.code, role.code); assert.equal(detail.dataScope, 4)
      return { roleId: state.roleId, dataScope: detail.dataScope }
    })
    const key = await c.call('GET', '/auth/gm/public-key')
    const req = createRequire(path.resolve(__dirname, '../../ls-aegis-web/package.json'))
    let password = 'T!' + crypto.randomBytes(18).toString('base64url') + 'a9'
    let cipher = req('sm-crypto').sm2.doEncrypt(password, key.publicKeyHex, 1); password = ''
    if (!cipher.startsWith('04')) cipher = '04' + cipher
    const user = { username: state.username, nickname: 'ITLS-TEST-' + c.tag + '_User' + suffix, password: cipher,
      gender: 1, deptId: String(current.deptId), roleIds: [state.roleId], description: state.runId, status: 1 }
    await c.step('F02-02', 'Create synthetic user and persist user-role association', async () => {
      const response = await c.call('POST', '/system/user', user)
      state.userId = String(response.id); save(); assert.match(state.userId, /^\d+$/)
      assert.ok(!response.initialPassword && !response.generated)
      const detail = await c.call('GET', '/system/user/' + state.userId)
      assert.equal(detail.username, state.username); assert.ok(detail.roleIds.map(String).includes(state.roleId))
      const ids = await c.call('GET', '/system/role/' + state.roleId + '/user/id')
      assert.ok(ids.map(String).includes(state.userId))
      return { userId: state.userId, roleLinked: true, noPlaintextPasswordReturned: true }
    })
    delete user.password; cipher = ''
    user.nickname = 'ITLS-TEST-' + c.tag + '_Edit' + suffix
    await c.step('F02-03', 'Edit nickname without changing assigned role', async () => {
      await c.call('PUT', '/system/user/' + state.userId, user)
      const detail = await c.call('GET', '/system/user/' + state.userId)
      assert.equal(detail.nickname, user.nickname); assert.deepEqual(detail.roleIds.map(String), [state.roleId])
      return { nicknamePersisted: true, rolePreserved: true }
    })
    await c.step('F02-04', 'Duplicate own synthetic username receives uniqueness rejection', async () => {
      const r = await c.raw('POST', '/system/user', { ...user, nickname: 'ITLS-TEST-' + c.tag + '_Dup' + suffix })
      assert.equal(r.body.success, false); assert.match(r.body.msg, /用户名.*已存在/)
      return { http: r.http, code: r.body.code, message: r.body.msg }
    })
    await c.step('F02-05', 'Role with associated user cannot be deleted', async () => {
      const r = await c.raw('DELETE', '/system/role', { ids: [state.roleId] })
      assert.equal(r.body.success, false); assert.match(r.body.msg, /用户关联/)
      const detail = await c.call('GET', '/system/role/' + state.roleId); assert.equal(detail.code, state.roleCode)
      return { associationGuarded: true, message: r.body.msg }
    })
    for (const status of [2, 1]) {
      await c.step('F02-0' + (status === 2 ? '6' : '7'), 'Persist synthetic user status ' + status, async () => {
        await c.call('PUT', '/system/user/' + state.userId, { ...user, status })
        const detail = await c.call('GET', '/system/user/' + state.userId); assert.equal(detail.status, status)
        return { statusPersisted: detail.status }
      })
    }
  } finally { await c.step('F02-CLEANUP', 'Remove only owned fixtures and independently verify absence', cleanup) }
}
main().catch(e => { console.error(String(e.message).slice(0, 220)); process.exitCode = 1 })
