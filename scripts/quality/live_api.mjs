/** R27 in-use local API testing. Execution results stay under .local; see Docs/testing/verification.md for scope.
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
const admin = JSON.parse(readFileSync(local('r27-session.json'), 'utf8')).token
const ledgerPath = local('r27-ledger.json')
const privatePath = local('r27-identities.json')
const state = existsSync(ledgerPath) ? JSON.parse(readFileSync(ledgerPath, 'utf8'))
  : { run: 'R27', prefix: 'ITLS-TEST-R27', target: base, results: [], objects: {}, started: new Date().toISOString() }
assert.equal(state.run, 'R27'); assert.equal(state.target, base)
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
  const text = 'BT /F1 18 Tf 50 750 Td (ITLS TEST R27 SYNTHETIC) Tj ET\n'
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

async function cleanupOld() {
  await test('R27-CLEAN-OLD-NOTICE', async () => {
    const id = '908798778079707254', marker = 'ITLS-TEST-R23-仅草稿'
    const rows = listOf(await api('/system/notice?title=' + encodeURIComponent(marker) + '&page=1&size=50'))
    const row = rows.find(row => String(row.id) === id)
    if (row) {
      assert.equal(row.title, marker)
      register('oldNotice', 'notice', id, marker, 'business DELETE by exact ID')
      await api('/system/notice', { method: 'DELETE', data: { ids: [id] } })
    }
    assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_notice WHERE id=${id} AND deleted=0`)), 0)
    // Published notice delivery is separate from personal notice-read logs.
    const messages = sql(`SELECT id FROM sys_message WHERE path LIKE '%${id}%' AND content LIKE '%ITLS-TEST-R23%'`)
    const messageIds = messages ? messages.split('\n') : []
    for (const messageId of messageIds) assert.match(messageId, /^\d+$/)
    if (messageIds.length) await api('/user/message', { method: 'DELETE', data: { ids: messageIds } })
    if (state.objects.oldNotice) removed('oldNotice')
    return { noticeId: id, activeNoticeRows: 0, exactSyntheticMessagesRemoved: messageIds.length,
      remainingSyntheticMessages: Number(sql(`SELECT COUNT(*) FROM sys_message WHERE path LIKE '%${id}%' AND content LIKE '%ITLS-TEST-R23%'`)) }
  })
  await test('R27-CLEAN-OLD-FILE', async () => {
    const id = '908798053845041260', marker = 'ITLS-TEST-R23.txt'
    const rows = listOf(await api('/system/file/recycle?originalName=' + encodeURIComponent(marker) + '&page=1&size=50'))
    const row = rows.find(row => String(row.id) === id)
    if (row) {
      assert.equal(row.originalName, marker)
      register('oldFile', 'file', id, marker, 'recycle DELETE exact ID')
      await api('/system/file/recycle/' + id, { method: 'DELETE' })
    }
    assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_file WHERE id=${id} AND deleted IN (0,1)`)), 0)
    const auditState = sql(`SELECT deleted FROM sys_file WHERE id=${id}`)
    if (auditState) assert.equal(auditState, id, 'Permanent removal keeps deleted=id metadata audit')
    if (state.objects.oldFile) removed('oldFile')
    return { fileId: id, activeOrRecycledRows: 0, auditState: auditState || 'no-row', scope: 'Only exact synthetic recycled file; no recycle clean endpoint' }
  })
}

async function preflight() {
  await test('R27-PLATFORM-AUTH-READ', async () => {
    const info = await api('/auth/user/info')
    assert.equal(String(info.id), '1'); assert.equal(info.pwdExpired, false)
    const routes = await api('/auth/user/route'); assert.ok(routes.length)
    const tenantEnabled = await api('/system/common/dict/option/tenant')
    const roleList = listOf(await api('/system/role/list'))
    state.roles = roleList.map(row => ({ id: String(row.id), code: row.code, dataScope: row.dataScope }))
    state.tenantEnabled = tenantEnabled
    return { authenticatedId: '1', role: 'super_admin', expired: false, routeRoots: routes.length, effectiveTenantEnabled: tenantEnabled }
  })
}

async function provisionUsers() {
  await test('R27-RBAC-TEST-IDENTITIES', async () => {
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
      const created = await api('/system/role', { method: 'POST', data: { name: marker, code: 'r27_limited', dataScope: 4, sort: 99, description: state.prefix } })
      const id = register('limitedRole', 'role', idOf(created), marker, 'delete after own test users')
      await api(`/system/role/${id}/permission`, { method: 'PUT', data: { roleId: id, menuIds: [], menuCheckStrictly: false } })
    }
    const definitions = { engineer: ['12', 'deptA'], other: ['12', 'deptB'], manager: ['11', 'deptA'], finance: ['13', 'deptA'], limited: [state.objects.limitedRole.id, 'deptB'] }
    for (const [role, [roleId, dept]] of Object.entries(definitions)) {
      if (identities[role]) continue
      const username = 'r27_' + role
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
      if (account.token) continue
      for (let step = 0; step < (account.initial ? 2 : 1); step++) {
        const challenge = await api('/captcha/image', { token: null })
        assert.equal(challenge.isEnabled, true)
        writeFileSync(local('r27-current-captcha.gif'), Buffer.from(challenge.img.split(',').at(-1), 'base64'))
        assert.equal(spawnSync('python', [resolve(root, 'scripts/quality/render_public_captcha.py'), local('r27-current-captcha.gif'), local('r27-current-captcha.png')], { cwd: root, encoding: 'utf8' }).status, 0, 'Public challenge image rendering failed')
        const ticket = randomUUID()
        writeFileSync(local('r27-auth-prompt.json'), JSON.stringify({ ticket, role, phase: account.initial ? 'initial' : 'active', expireTime: challenge.expireTime }))
        console.log(`CAPTCHA READY ${role} ${account.initial ? 'initial' : 'active'}`)
        let answer
        if (fileInput) {
          const deadline = Date.now() + 180000
          while (Date.now() < deadline) {
            if (existsSync(local('r27-auth-answer.json'))) {
              const packet = JSON.parse(readFileSync(local('r27-auth-answer.json'), 'utf8'))
              if (packet.ticket === ticket) { answer = packet.answer; unlinkSync(local('r27-auth-answer.json')); break }
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
  await test('R27-RBAC-ROLES-CONTEXT', async () => {
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
  await test('R27-FILE-CONTENT-ACCESS-RECYCLE', async () => {
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
  await test('R27-NOTICE-DRAFT-CRUD', async () => {
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
  await test('R27-MESSAGE-OBJECT-DELETE-AUTHORIZATION', async () => {
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

async function createOwned(key, path, payload, table, column, marker) {
  await api(path, { method: 'POST', data: payload })
  const id = sql(`SELECT id FROM ${table} WHERE ${column}='${marker}' ORDER BY id DESC LIMIT 1`)
  return register(key, table, id, marker, 'business API deletion or documented compensation')
}
const now = () => new Date().toISOString().replace('T', ' ').slice(0, 19)
async function psiFixtures() {
  await test('R27-PSI-MASTER-DATA', async () => {
    for (const [key, path, table, column, suffix, extra] of [
      ['unit', '/psi/unit', 'psi_unit', 'unit_name', '-双向单位', { unitType: 'BOTH', unitNature: 'ENTERPRISE', status: 1 }],
      ['categoryRoot', '/psi/goods-category', 'psi_goods_category', 'name', '-一级', { parentId: '0', status: 1 }],
      ['category', '/psi/goods-category', 'psi_goods_category', 'name', '-二级', () => ({ parentId: state.objects.categoryRoot.id, status: 1 })],
      ['goods', '/psi/goods', 'psi_goods', 'goods_name', '-商品', () => ({ unit: '件', defaultTaxRate: 0, categoryId: state.objects.category.id, status: 1 })],
      ['warehouse', '/psi/warehouse', 'psi_warehouse', 'warehouse_name', '-仓库', { managerUserId: '1', isDefault: false, status: 1 }]
    ]) {
      if (state.objects[key]?.status === 'ACTIVE') continue
      const marker = state.prefix + suffix
      const field = { unit: 'unitName', categoryRoot: 'name', category: 'name', goods: 'goodsName', warehouse: 'warehouseName' }[key]
      await createOwned(key, path, { [field]: marker, ...(typeof extra === 'function' ? extra() : extra) }, table, column, marker)
    }
    for (const [key, path] of [['unit', '/psi/unit'], ['goods', '/psi/goods'], ['warehouse', '/psi/warehouse']]) {
      assert.equal(String((await api(path + '/' + state.objects[key].id)).id), state.objects[key].id)
      await api(path + '/' + state.objects[key].id, { token: identities.limited.token, reject: true })
    }
    return { keys: ['unit', 'categoryRoot', 'category', 'goods', 'warehouse'], onlySynthetic: true, limitedDenied: true }
  })
  await test('R27-PSI-CASH-DRAFT-VALIDATION', async () => {
    const tested = []
    for (const kind of ['receipt', 'payment']) {
      const payload = { [kind + 'Date']: now(), unitId: state.objects.unit.id, amount: 1, payMethod: 'CASH', remark: state.prefix }
      const path = '/psi/' + kind
      await api(path, { method: 'POST', data: { ...payload, amount: 0 }, reject: true })
      const id = register(kind + 'Draft', 'psi_' + kind, idOf(await api(path, { method: 'POST', data: payload })), state.prefix, 'exact draft DELETE')
      try {
        assert.equal((await api(path + '/' + id)).docStatus, 'DRAFT')
        await api(path + '/' + id, { method: 'PUT', data: { ...payload, amount: 2 } })
        assert.equal(Number((await api(path + '/' + id)).amount), 2)
        tested.push(kind)
      } finally { await api(path + '/' + id, { method: 'DELETE' }); removed(kind + 'Draft') }
    }
    return { draftCrud: tested, zeroAmountRejected: true, confirmedCash: false, confirmedCashBlocker: 'No complete void/refund allocation compensation API; do not create unallocated financial residue' }
  })
}

async function confirmDoc(path, id, key = randomUUID()) {
  return api(path + '/' + id + '/confirm', { method: 'POST', data: { idempotentKey: key } })
}
async function psiStock() {
  const unitId = state.objects.unit.id, warehouseId = state.objects.warehouse.id, goodsId = state.objects.goods.id
  const line = (quantity, price = 100) => ({ goodsId, quantity, taxInclusivePrice: price, taxRate: 0 })
  const quantity = () => Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE warehouse_id=${warehouseId} AND goods_id=${goodsId}`))
  async function returnFull(key, originKey, sale) {
    const origin = state.objects[originKey]; if (!origin) return
    const path = sale ? '/psi/stock-out' : '/psi/stock-in'
    const original = await api(path + '/' + origin.id)
    if (original.docStatus !== 'CONFIRMED') return
    let entry = state.objects[key]
    if (!entry) {
      const payload = { returnType: sale ? 'SALE_RETURN' : 'PURCHASE_RETURN', returnDate: now(), warehouseId, unitId,
        [sale ? 'originStockOutId' : 'originStockInId']: origin.id, remark: state.prefix + '-补偿',
        items: original.items.map(item => ({ originItemId: String(item.id), goodsId: String(item.goodsId), quantity: item.quantity,
          taxInclusivePrice: item.taxInclusivePrice, taxRate: item.taxRate })) }
      register(key, 'psi_stock_return', idOf(await api('/psi/stock-return', { method: 'POST', data: payload })), state.prefix, 'confirmed return audit retained')
      entry = state.objects[key]
    }
    if ((await api('/psi/stock-return/' + entry.id)).docStatus === 'DRAFT') await confirmDoc('/psi/stock-return', entry.id)
    entry.status = 'AUDIT_RETAINED'; origin.status = 'COMPENSATED'; save()
  }
  await test('R27-PSI-PURCHASE-SALE-FIFO-RETURN', async () => {
    assert.equal(quantity(), 0, 'Only initially empty synthetic stock scope')
    for (const key of ['stockIn', 'stockOut', 'saleContract', 'saleReturn', 'purchaseReturn', 'stockShortage']) {
      if (!state.objects[key]) continue
      assert.notEqual(state.objects[key].status, 'ACTIVE', 'Clean previous owned drafts before starting a fresh stock cycle')
      ;(state.archivedObjects ||= []).push(state.objects[key]); delete state.objects[key]
    }
    save()
    try {
      const stockIn = register('stockIn', 'psi_stock_in', idOf(await api('/psi/stock-in', { method: 'POST', data: {
        inType: 'PURCHASE', inDate: now(), warehouseId, unitId, items: [line(10)], remark: state.prefix } })), state.prefix, 'full purchase return')
      const inKey = randomUUID(); await confirmDoc('/psi/stock-in', stockIn, inKey); await confirmDoc('/psi/stock-in', stockIn, inKey)
      assert.equal(quantity(), 10)
      assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_payable WHERE source_id=${stockIn} AND deleted=0`)), 1)
      const contract = register('saleContract', 'psi_contract', idOf(await api('/psi/contract-sale', { method: 'POST', data: {
        name: state.prefix + '-销售合同', signDate: now(), unitId, taxInclusiveAmount: 300, remark: state.prefix } })), state.prefix, 'confirmed contract audit retained')
      await confirmDoc('/psi/contract-sale', contract); state.objects.saleContract.status = 'AUDIT_RETAINED'; save()
      const payload = { outType: 'SALE', outDate: now(), warehouseId, unitId, contractId: contract, items: [line(2, 150)], remark: state.prefix }
      const stockOut = register('stockOut', 'psi_stock_out', idOf(await api('/psi/stock-out', { method: 'POST', data: payload })), state.prefix, 'full sale return')
      const outKey = randomUUID(); await confirmDoc('/psi/stock-out', stockOut, outKey); await confirmDoc('/psi/stock-out', stockOut, outKey)
      assert.equal(quantity(), 8)
      assert.equal(Number(sql(`SELECT cost_amount FROM psi_stock_out WHERE id=${stockOut}`)), 200)
      assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_receivable WHERE source_id=${stockOut} AND deleted=0`)), 1)
      const shortage = register('stockShortage', 'psi_stock_out', idOf(await api('/psi/stock-out', { method: 'POST', data: { ...payload, items: [line(1000)] } })), state.prefix, 'draft DELETE')
      await api('/psi/stock-out/' + shortage + '/confirm', { method: 'POST', data: { idempotentKey: randomUUID() }, reject: true })
      assert.equal(quantity(), 8); assert.equal((await api('/psi/stock-out/' + shortage)).docStatus, 'DRAFT')
      return { stockIn, stockOut, purchaseQuantity: 10, saleQuantity: 2, fifoCost: 200, shortageRollback: true, sameKeyRetryNoDuplicates: true }
    } finally {
      await returnFull('saleReturn', 'stockOut', true)
      await returnFull('purchaseReturn', 'stockIn', false)
      state.stockCleanup = { quantity: quantity(), openReceivable: Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_receivable WHERE unit_id=${unitId} AND deleted=0`)),
        openPayable: Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_payable WHERE unit_id=${unitId} AND deleted=0`)) }; save()
      assert.deepEqual(state.stockCleanup, { quantity: 0, openReceivable: 0, openPayable: 0 })
      if (state.objects.stockShortage?.status === 'ACTIVE') { await api('/psi/stock-out/' + state.objects.stockShortage.id, { method: 'DELETE' }); removed('stockShortage') }
    }
  })
}

async function quoteModule() {
  await test('R27-QUOTE-INDEPENDENT-AMOUNT-EXPORT', async () => {
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
        writeFileSync(local('r27-quote.' + suffix), bytes)
      }
      const word = spawnSync('python', ['-X', 'utf8', '-c', 'import zipfile,sys,xml.etree.ElementTree as E; print("".join(E.fromstring(zipfile.ZipFile(sys.argv[1]).read("word/document.xml")).itertext()))', local('r27-quote.docx')], { encoding: 'utf8' })
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
async function eomsModule() {
  const marker = state.prefix + '-维修回滚', unitId = state.objects.unit.id
  await test('R27-EOMS-APPLY-STATE-CLEANUP', async () => {
    const id = await createOwned('apply', '/eoms/repair/apply', { customerId: unitId, faultDesc: marker, remark: marker }, 'eoms_repair_apply', 'remark', marker)
    try {
      await api('/eoms/repair/apply/confirm/' + id, { method: 'PUT' })
      await api('/eoms/repair/apply/close/' + id + '?remark=ITLS-TEST-R27', { method: 'PUT' })
      return { applyId: id, confirmedAndClosed: true, noCustomerNotification: true }
    } finally { await api('/eoms/repair/apply/' + id, { method: 'DELETE' }); removed('apply') }
  })
  await test('R27-EOMS-ENGINEER-STATE-STOCK-ROLLBACK', async () => {
    const id = await createOwned('order', '/eoms/repair/order', { customerId: unitId, address: marker, faultDesc: marker, remark: marker }, 'eoms_repair_order', 'remark', marker)
    try {
      await api('/eoms/repair/order/submit/' + id, { method: 'PUT', reject: true })
      await api('/eoms/repair/order/assign/' + id + '?engineerId=' + identities.engineer.id, { method: 'PUT' })
      await api('/eoms/engineer/task/' + id, { token: identities.other.token, reject: true })
      await api('/eoms/engineer/task/' + id, { token: identities.engineer.token })
      await api('/eoms/repair/order/start/' + id, { method: 'PUT' })
      await api('/eoms/engineer/record', { method: 'POST', token: identities.engineer.token, data: { repairOrderId: id, faultReason: marker, checkProcess: marker, solution: marker, result: marker } })
      const recordId = sql(`SELECT id FROM eoms_repair_record WHERE repair_order_id=${id} AND del_flag='0'`)
      register('repairRecord', 'eoms_repair_record', recordId, marker, 'record DELETE')
      await api('/eoms/repair/material', { method: 'POST', data: { repairOrderId: id, materialId: state.objects.goods.id, warehouseId: state.objects.warehouse.id,
        materialName: marker, quantity: 1000, salePrice: 150, costPrice: 100 } })
      const materialId = sql(`SELECT id FROM eoms_repair_material WHERE repair_order_id=${id} AND del_flag='0'`)
      register('repairMaterial', 'eoms_repair_material', materialId, marker, 'material DELETE')
      await api('/eoms/repair/order/submit/' + id, { method: 'PUT' })
      assert.equal((await api('/eoms/repair/order/' + id)).status, 'WAIT_SETTLEMENT')
      const settlement = sql(`SELECT id FROM eoms_repair_settlement WHERE repair_order_id=${id} AND del_flag='0'`)
      register('settlement', 'eoms_repair_settlement', settlement, marker, 'draft DELETE')
      const rejected = await api('/eoms/repair/settlement/confirm/' + settlement, { method: 'PUT', reject: true })
      assert.match(rejected.msg, /库存|不足/)
      assert.equal(sql(`SELECT status FROM eoms_repair_settlement WHERE id=${settlement}`), 'DRAFT')
      assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_receivable WHERE repair_order_id=${id} AND del_flag='0'`)), 0)
      assert.equal(Number(sql(`SELECT COUNT(*) FROM eoms_income WHERE repair_order_id=${id} AND del_flag='0'`)), 0)
      assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_stock_out WHERE deleted=0 AND remark LIKE CONCAT('工单[',(SELECT order_no FROM eoms_repair_order WHERE id=${id}),']结算出库%')`)), 0)
      return { orderId: id, settlement, otherEngineerDenied: true, states: ['WAIT_ACCEPT', 'ACCEPTED', 'PROCESSING', 'WAIT_SETTLEMENT'],
        shortageRejected: true, noReceivableIncomeOrStockDocumentLeak: true, externalGeocodingUsed: false }
    } finally {
      for (const [key, path] of [['settlement', '/eoms/repair/settlement/'], ['repairMaterial', '/eoms/repair/material/'], ['repairRecord', '/eoms/repair/record/'], ['order', '/eoms/repair/order/']]) {
        const entry = state.objects[key]; if (entry?.status === 'ACTIVE') { await api(path + entry.id, { method: 'DELETE' }); removed(key) }
      }
    }
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
  await test('R27-SIGN-SYNTHETIC-CONCURRENT-CRYPTOGRAPHY', async () => {
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
      writeFileSync(local('r27-signed.pdf'), pdf)
      const range = /\/ByteRange\s*\[\s*(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*\]/.exec(pdf.toString('latin1')); assert.ok(range)
      const [start, count, second, secondCount] = range.slice(1).map(Number); assert.equal(start, 0); assert.equal(second + secondCount, pdf.length)
      const padded = Buffer.from(pdf.subarray(count, second).toString('ascii').replace(/[<>\s]/g, ''), 'hex')
      const lengthBytes = padded[1] & 0x80 ? padded[1] & 0x7f : 0
      const contentLength = lengthBytes ? padded.readUIntBE(2, lengthBytes) : padded[1]
      writeFileSync(local('r27-signature.der'), padded.subarray(0, 2 + lengthBytes + contentLength))
      const content = Buffer.concat([pdf.subarray(start, count), pdf.subarray(second, second + secondCount)])
      writeFileSync(local('r27-sign-content.bin'), content); content[20] ^= 1; writeFileSync(local('r27-sign-tampered.bin'), content)
      for (const name of ['r27-signature.der', 'r27-sign-content.bin', 'r27-sign-tampered.bin']) assert.equal(spawnSync('docker', ['cp', local(name), backend + ':/tmp/' + name], { encoding: 'utf8' }).status, 0)
      const verify = name => spawnSync('docker', ['exec', backend, 'openssl', 'cms', '-verify', '-binary', '-inform', 'DER', '-in', '/tmp/r27-signature.der', '-content', '/tmp/' + name, '-noverify', '-out', '/tmp/r27-verified.bin'], { encoding: 'utf8' })
      assert.equal(verify('r27-sign-content.bin').status, 0); assert.notEqual(verify('r27-sign-tampered.bin').status, 0)
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
    const challenge = await api('/eoms/h5/auth/captcha', { token: null })
    writeFileSync(local('r27-current-captcha.gif'), Buffer.from(challenge.img.replace(/^data:[^,]+,/, ''), 'base64'))
    assert.equal(spawnSync('python', [resolve(root, 'scripts/quality/render_public_captcha.py'), local('r27-current-captcha.gif'), local('r27-current-captcha.png')], { encoding: 'utf8' }).status, 0)
    const ticket = randomUUID()
    writeFileSync(local('r27-auth-prompt.json'), JSON.stringify({ ticket, role: key, phase: 'h5', expireTime: challenge.expireTime }))
    console.log('H5 CAPTCHA READY ' + key)
    let answer
    const deadline = Date.now() + 150000
    while (Date.now() < deadline) {
      if (existsSync(local('r27-auth-answer.json'))) {
        const packet = JSON.parse(readFileSync(local('r27-auth-answer.json'), 'utf8'))
        if (packet.ticket === ticket) { answer = packet.answer; unlinkSync(local('r27-auth-answer.json')); break }
      }
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    assert.ok(answer)
    const customerNo = sql(`SELECT unit_code FROM psi_unit WHERE id=${state.objects[key].id}`)
    const data = await api('/eoms/h5/auth/token', { method: 'POST', token: null, data: { customerNo, captcha: answer.trim(), uuid: challenge.uuid } })
    identities[key] = { id: state.objects[key].id, token: data.token, kind: 'h5' }; save()
    console.log('H5 AUTH READY ' + key)
  }
}
async function h5Module() {
  await test('R27-CUSTOMER-H5-OWNERSHIP-LOGOUT', async () => {
    const first = { 'h5-token': identities.unit.token }, second = { 'h5-token': identities.unitOther.token }
    const marker = state.prefix + '-H5报修'
    let id
    try {
      const result = await api('/eoms/h5/repair/apply', { method: 'POST', token: null, headers: first,
        data: { customerId: state.objects.unitOther.id, contactName: marker, contactPhone: '13800000000', faultType: 'OTHER', faultDesc: marker, address: marker, remark: marker } })
      id = register('h5Apply', 'eoms_repair_apply', result.id, marker, 'admin exact apply DELETE')
      assert.equal(sql(`SELECT customer_id FROM eoms_repair_apply WHERE id=${id}`), identities.unit.id)
      await api('/eoms/h5/repair/detail/' + id, { token: null, headers: first })
      await api('/eoms/h5/repair/detail/' + id, { token: null, headers: second, reject: true })
      await api('/eoms/h5/repair/detail/' + id, { token: null, reject: true })
      const ownList = listOf(await api('/eoms/h5/repair/list?page=1&size=100', { token: null, headers: first }))
      assert.ok(ownList.some(row => String(row.id) === id))
      const otherList = listOf(await api('/eoms/h5/repair/list?page=1&size=100', { token: null, headers: second }))
      assert.ok(!otherList.some(row => String(row.id) === id))
      await api('/eoms/h5/auth/logout?token=' + encodeURIComponent(identities.unit.token), { token: null })
      await api('/eoms/h5/repair/detail/' + id, { token: null, headers: first, reject: true })
      await api('/eoms/h5/auth/logout?token=' + encodeURIComponent(identities.unitOther.token), { token: null })
      return { applyId: id, forgedCustomerIgnored: true, crossCustomerDenied: true, listIsolation: true, logoutRevoked: true, captcha: 'normal public image challenge; no cache access', ui: 'NOT_RUN' }
    } finally { if (id) { await api('/eoms/repair/apply/' + id, { method: 'DELETE' }); removed('h5Apply') } }
  })
}
async function messageRetest() {
  await test('R27-MESSAGE-PRIVATE-DELETE-BATCH-CONCURRENT', async () => {
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
  await test('R27-PLATFORM-ROUTES-OPTIONS-READ', async () => {
    const routes = await api('/auth/user/route'); assert.ok(Array.isArray(routes) && routes.length)
    const paths = ['/system/dept/tree', '/system/role/list', '/system/menu/tree', '/system/dict/list', '/monitor/online?page=1&size=10', '/system/log?page=1&size=10']
    for (const path of paths) { const data = await api(path); assert.ok(data && typeof data === 'object') }
    const flags = sql("SELECT code,value FROM sys_option WHERE code IN ('MODULE_SCHEDULE_ENABLED')")
    state.moduleFlags = flags.split('\n').filter(Boolean).map(row => { const [code, value] = row.split('\t'); return { code, value } }); save()
    const option = await api('/system/option?category=MODULE'); assert.ok(option)
    const schedule = await api('/schedule/job?page=1&size=10', { reject: true })
    assert.match(schedule.msg, /禁用|未开启/)
    return { readPaths: paths, routeRoots: routes.length, moduleFlags: state.moduleFlags, scheduleGate: schedule.msg,
      scheduleApplicability: 'N/A for disabled current deployment; do not enable for test', tenantEnabled: state.tenantEnabled }
  })
  await test('R27-PC-H5-HTTP-ENTRY', async () => {
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
  await test('R27-WORKBENCH-SOURCE-CONSISTENCY-READONLY', async () => {
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
      fullJointAcceptance: 'NOT_RUN until required financial positive-flow and compensation gaps resolved' }
  })
}
async function cleanupDrafts() {
  await test('R27-LOGICAL-DELETE-LIVE-RETEST-CLEANUP', async () => {
    const cleaned = []
    for (const [key, path, table] of [['receiptDraft', '/psi/receipt/', 'psi_receipt'], ['paymentDraft', '/psi/payment/', 'psi_payment'], ['stockShortage', '/psi/stock-out/', 'psi_stock_out']]) {
      const entry = state.objects[key]; if (!entry || entry.status !== 'ACTIVE') continue
      const row = await api(path + entry.id); assert.equal(row.docStatus, 'DRAFT'); assert.equal(row.remark, state.prefix)
      await api(path + entry.id, { method: 'DELETE' })
      assert.equal(sql(`SELECT deleted FROM ${table} WHERE id=${entry.id}`), entry.id)
      removed(key); cleaned.push({ kind: key, id: entry.id })
    }
    return { cleaned, exactBusinessDelete: true, directSqlDeletion: false }
  })
}
async function cleanupFixtures() {
  await test('R27-FINAL-FIXTURE-CLEANUP', async () => {
    writeFileSync(local('r27-stop-session-checks'), 'stop')
    await cleanupDrafts()
    const disabled = [], deleted = []
    for (const [key, path, name] of [['goods', '/psi/goods/', 'goodsName'], ['warehouse', '/psi/warehouse/', 'warehouseName'],
      ['unit', '/psi/unit/', 'unitName'], ['category', '/psi/goods-category/', 'name'], ['categoryRoot', '/psi/goods-category/', 'name']]) {
      const entry = state.objects[key]; if (!entry || entry.status !== 'ACTIVE') continue
      const detail = await api(path + entry.id); assert.equal(detail[name], entry.marker)
      await api(path + entry.id, { method: 'PUT', data: { ...detail, status: 2 } })
      assert.equal(Number((await api(path + entry.id)).status), 2)
      entry.status = 'DISABLED_AUDIT_RETAINED'; save(); disabled.push(key)
    }
    if (state.objects.unitOther?.status === 'ACTIVE') {
      const entry = state.objects.unitOther; assert.equal((await api('/psi/unit/' + entry.id)).unitName, entry.marker)
      await api('/psi/unit', { method: 'DELETE', data: { ids: [entry.id] } })
      assert.equal(sql(`SELECT deleted FROM psi_unit WHERE id=${entry.id}`), entry.id); removed('unitOther'); deleted.push('unitOther')
    }
    const project = state.objects.signProject
    if (project) {
      const detail = await api('/sign/project/' + project.id); assert.equal(detail.projectName, project.marker)
      await api('/sign/project', { method: 'PUT', data: { ...detail, status: '5', sealIds: [], userIds: [] } })
      assert.equal(sql(`SELECT status FROM sign_project WHERE project_id=${project.id}`), '5')
      project.status = 'CANCELLED_AUDIT_RETAINED'; save()
      const records = sql(`SELECT record_id,signed_file_id FROM sign_record WHERE project_id=${project.id}`)
      state.signatureAuditInventory = records.split('\n').filter(Boolean).map(row => { const [recordId, signedFileId] = row.split('\t'); return { recordId, signedFileId, syntheticProjectId: project.id } }); save()
      for (const entry of [state.objects.signedFile, state.objects.unifiedFile]) if (entry) entry.kind = 'sign_file'
    }
    for (const role of ['engineer', 'other', 'manager', 'finance', 'limited']) {
      const entry = state.objects['user_' + role]; if (!entry || entry.status !== 'ACTIVE') continue
      const detail = await api('/system/user/' + entry.id); assert.equal(detail.username, entry.marker); assert.ok(detail.username.startsWith('r27_'))
      await api('/auth/logout', { method: 'POST', token: identities[role].token })
      await api('/auth/user/info', { token: identities[role].token, reject: true })
      await api('/system/user', { method: 'DELETE', data: { ids: [entry.id] } })
      assert.equal(sql(`SELECT deleted FROM sys_user WHERE id=${entry.id}`), entry.id)
      removed('user_' + role); deleted.push('user_' + role)
    }
    for (const [key, path, table, column] of [['limitedRole', '/system/role', 'sys_role', 'name'], ['deptA', '/system/dept', 'sys_dept', 'name'], ['deptB', '/system/dept', 'sys_dept', 'name']]) {
      const entry = state.objects[key]; if (!entry || entry.status !== 'ACTIVE') continue
      assert.equal(sql(`SELECT ${column} FROM ${table} WHERE id=${entry.id} AND deleted=0`), entry.marker)
      await api(path, { method: 'DELETE', data: { ids: [entry.id] } })
      assert.equal(sql(`SELECT deleted FROM ${table} WHERE id=${entry.id}`), entry.id); removed(key); deleted.push(key)
    }
    const stock = Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE warehouse_id=${state.objects.warehouse.id} AND goods_id=${state.objects.goods.id}`))
    const ar = Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_receivable WHERE unit_id=${state.objects.unit.id} AND deleted=0`))
    const ap = Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_payable WHERE unit_id=${state.objects.unit.id} AND deleted=0`))
    assert.equal(stock, 0); assert.equal(ar, 0); assert.equal(ap, 0)
    assert.equal(sql(`SELECT status FROM sign_seal WHERE seal_id=${state.objects.seal.id}`), '1')
    assert.equal(Number(sql(`SELECT COUNT(*) FROM sign_seal_permission WHERE seal_id=${state.objects.seal.id}`)), 0)
    return { disabledHistoricalMasters: disabled, deletedSyntheticFixtures: deleted, stock, openReceivable: ar, openPayable: ap,
      testSealDisabled: true, testProjectCancelled: true, signatureAuditRecords: state.signatureAuditInventory.length,
      retainedAndCleanedObjects: Object.fromEntries(Object.entries(state.objects).map(([key, entry]) => [key, entry.status])),
      financialAuditPhysicallyDeleted: false, originalAccountsChanged: false }
  })
}
async function closeRun() {
  await test('R27-FINAL-INDEPENDENT-AUDIT', async () => {
    const activeObjects = Object.entries(state.objects).filter(([, entry]) => entry.status === 'ACTIVE')
    assert.equal(activeObjects.length, 0)
    const users = ['engineer', 'other', 'manager', 'finance', 'limited'].map(role => state.objects['user_' + role].id)
    assert.equal(Number(sql(`SELECT COUNT(*) FROM sys_user WHERE id IN (${users.join(',')}) AND deleted=0`)), 0)
    const unit = state.objects.unit.id, warehouse = state.objects.warehouse.id, goods = state.objects.goods.id
    const remainingCashDrafts = Number(sql(`SELECT COUNT(*) FROM psi_receipt WHERE remark='${state.prefix}' AND deleted=0`))
      + Number(sql(`SELECT COUNT(*) FROM psi_payment WHERE remark='${state.prefix}' AND deleted=0`))
    assert.equal(remainingCashDrafts, 0)
    const remainingShortage = Number(sql(`SELECT COUNT(*) FROM psi_stock_out WHERE warehouse_id=${warehouse} AND remark='${state.prefix}' AND doc_status='DRAFT' AND deleted=0`))
    assert.equal(remainingShortage, 0)
    const net = { quantity: Number(sql(`SELECT COALESCE(SUM(remain_quantity),0) FROM psi_stock_batch WHERE warehouse_id=${warehouse} AND goods_id=${goods}`)),
      cost: Number(sql(`SELECT COALESCE(SUM(remain_amount),0) FROM psi_stock_batch WHERE warehouse_id=${warehouse} AND goods_id=${goods}`)),
      receivable: Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_receivable WHERE unit_id=${unit} AND deleted=0`)),
      payable: Number(sql(`SELECT COALESCE(SUM(unwritten_amount),0) FROM psi_payable WHERE unit_id=${unit} AND deleted=0`)) }
    assert.deepEqual(net, { quantity: 0, cost: 0, receivable: 0, payable: 0 })
    const files = sql(`SELECT id,original_name FROM sys_file WHERE original_name LIKE '${state.prefix}%' AND deleted=0`)
    const retainedFiles = files.split('\n').filter(Boolean).map(row => { const [id, name] = row.split('\t'); return { id, name } })
    state.cleanupFinal = { activeOwnedObjects: 0, testUsersActive: 0, remainingCashDrafts, remainingShortage, net, retainedFiles,
      auditRetention: 'confirmed documents/returns, cancelled project, disabled seal, certificate and referenced files, personally deleted message records retained' }; save()
    return state.cleanupFinal
  })
  await test('R27-OWN-ADMIN-SESSION-LOGOUT', async () => {
    await api('/auth/logout', { method: 'POST' })
    await api('/auth/user/info', { reject: true })
    return { onlyCurrentTestSessionLoggedOut: true, originalAdminPasswordChanged: false, testSessionHelperStopped: true }
  })
  if (!process.exitCode) {
    unlinkSync(privatePath); unlinkSync(local('r27-session.json'))
    state.closed = new Date().toISOString(); state.privateCredentialsFilesRemoved = true
    writeFileSync(ledgerPath, JSON.stringify(state, null, 2))
  }
}
const command = process.argv[2]
if (command === 'preflight') await preflight()
else if (command === 'cleanup-old') await cleanupOld()
else if (command === 'provision-users') await provisionUsers()
else if (command === 'auth-users') await authUsers()
else if (command === 'auth-status') await authStatus()
else if (command === 'file') await fileModule()
else if (command === 'notice') await noticeModule()
else if (command === 'psi-fixtures') await psiFixtures()
else if (command === 'psi-stock') await psiStock()
else if (command === 'quote') await quoteModule()
else if (command === 'eoms') await eomsModule()
else if (command === 'sign') await signModule()
else if (command === 'h5-auth') await h5Auth()
else if (command === 'h5') await h5Module()
else if (command === 'message-retest') await messageRetest()
else if (command === 'platform-read') await platformRead()
else if (command === 'workbench-read') await workbenchRead()
else if (command === 'cleanup-drafts') await cleanupDrafts()
else if (command === 'cleanup-fixtures') await cleanupFixtures()
else if (command === 'finish') await closeRun()
else throw new Error('Unknown command; choose an explicitly implemented test phase')
