/** Normal, owned customer H5 API workflow; actual captcha, no camera/GPS or messaging. */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const { deflateSync } = require('node:zlib')
const { WorkflowClient } = require('./business_workflow_client.cjs')
const c = new WorkflowClient(process.argv[2])
const phase = process.argv[3] === '--cleanup-only' ? 'cleanup-only' : process.argv[3]
const file = path.join(c.dir, 'customer-fixtures.json')
const s = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : {
  runId: c.scope.runId, prefix: 'ITLS-TEST-' + c.tag + '-H5-' + crypto.randomBytes(5).toString('hex'),
}
assert.equal(s.runId, c.scope.runId)
const save = () => c.save('customer-fixtures.json', s)
const privateFile = name => path.join(c.dir, name + '-private.json')
let token = fs.existsSync(privateFile('h5-session')) ? JSON.parse(fs.readFileSync(privateFile('h5-session'))).token : ''
const rows = d => d.list || d.records || []
async function raw(method, endpoint, data) {
  const r = await fetch(c.scope.api + endpoint, { method, redirect: 'manual', signal: AbortSignal.timeout(20000),
    headers: { 'Content-Type': 'application/json', ...(token ? { 'h5-token': token } : {}) },
    ...(data === undefined ? {} : { body: JSON.stringify(data) }) })
  assert.ok(r.status < 300 || r.status >= 400)
  return { http: r.status, body: await r.json() }
}
async function call(method, endpoint, data) {
  const r = await raw(method, endpoint, data)
  if (r.http !== 200 || !r.body.success || String(r.body.code) !== '0') throw Error(`${endpoint}: HTTP=${r.http} code=${r.body.code} ${String(r.body.msg).slice(0, 120)}`)
  return r.body.data
}
const step = (id, action, fn) => c.step(id, action, fn, { module: 'customer-H5-API', role: 'synthetic-customer / authorized-PC-operator' })
async function customers() {
  return rows(await c.call('GET', '/psi/unit?size=100&keyword=' + s.prefix)).filter(x => x.unitName === s.prefix)
}
async function applies() {
  return rows(await c.call('GET', '/eoms/repair/apply/list?size=100&faultDesc=' + s.prefix)).filter(x => x.faultDesc === s.prefix)
}
async function photos(recycle = false) {
  const d = await c.call('GET', '/system/file' + (recycle ? '/recycle' : '') + '?size=100&originalName=' + s.prefix)
  const found = rows(d).filter(x => x.originalName?.startsWith(s.prefix))
  assert.ok(found.length <= 1)
  for (const f of found) if (s.photoId) assert.equal(String(f.id), s.photoId)
  return found
}
function syntheticPng() {
  const width = 640, height = 480
  function crc32(bytes) { let n = 0xffffffff; for (const b of bytes) { n ^= b; for (let i = 0; i < 8; i++) n = (n >>> 1) ^ ((n & 1) ? 0xedb88320 : 0) } return (n ^ 0xffffffff) >>> 0 }
  function chunk(type, bytes) {
    const content = Buffer.concat([Buffer.from(type), bytes]), size = Buffer.alloc(4), crc = Buffer.alloc(4)
    size.writeUInt32BE(bytes.length); crc.writeUInt32BE(crc32(content)); return Buffer.concat([size, content, crc])
  }
  const header = Buffer.alloc(13); header.writeUInt32BE(width); header.writeUInt32BE(height, 4); header[8] = 8; header[9] = 6
  const pixels = Buffer.alloc(height * (1 + width * 4))
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    const offset = y * (1 + width * 4) + 1 + x * 4
    pixels[offset] = 185; pixels[offset + 1] = 205; pixels[offset + 2] = 225; pixels[offset + 3] = 255
  }
  return Buffer.concat([Buffer.from('89504e470d0a1a0a', 'hex'), chunk('IHDR', header), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))])
}
async function cleanup() {
  const found = await applies(); assert.ok(found.length <= 1)
  for (const a of found) {
    assert.equal(String(a.customerId), s.customerId); if (s.applyId) assert.equal(String(a.id), s.applyId)
    assert.ok(['WAIT', 'CONFIRM', 'CANCEL'].includes(a.status))
    await c.call('DELETE', '/eoms/repair/apply/' + a.id)
  }
  assert.equal((await applies()).length, 0)
  for (const f of await photos()) {
    const detail = await c.call('GET', '/system/common/file/' + f.id + '/info')
    const metadata = typeof detail.metadata === 'string' ? JSON.parse(detail.metadata) : detail.metadata
    assert.equal(String(metadata.itlsEomsH5UploadOwner.customerId), s.customerId)
    await c.call('DELETE', '/system/file', { ids: [String(f.id)] })
  }
  for (const f of await photos(true)) await c.call('DELETE', '/system/file/recycle/' + f.id)
  assert.equal((await photos()).length + (await photos(true)).length, 0)
  if (token) {
    await call('POST', '/eoms/h5/auth/logout')
    const rejected = await raw('GET', '/eoms/h5/repair/list')
    assert.equal(String(rejected.body.code), '10007'); assert.equal(rejected.body.success, false)
    fs.unlinkSync(privateFile('h5-session')); token = ''; s.sessionRevoked = true; save()
  }
  const units = await customers(); assert.ok(units.length <= 1)
  for (const u of units) {
    const d = await c.call('GET', '/psi/unit/' + u.id); assert.equal(d.remark, s.runId)
    await c.call('DELETE', '/psi/unit', { ids: [String(u.id)] })
  }
  assert.equal((await customers()).length, 0)
  for (const name of ['h5-invitation-private.json', 'h5-captcha-answer.json']) {
    const owned = path.join(c.dir, name); if (fs.existsSync(owned)) fs.unlinkSync(owned)
  }
  s.cleaned = true; save()
  return { remainingApplications: 0, remainingCustomers: 0, remainingPhotos: 0, sessionRevoked: Boolean(s.sessionRevoked), externalMessagesSent: 0 }
}
async function captcha() {
  const challenge = await call('GET', '/eoms/h5/auth/captcha')
  assert.ok(challenge.uuid && challenge.img && challenge.isEnabled)
  c.save('h5-challenge.json', challenge)
  fs.writeFileSync(path.join(c.dir, 'h5-captcha.gif'), Buffer.from(challenge.img.split(',').at(-1), 'base64'))
  console.log('H5_REAL_CAPTCHA_READY')
}
async function prepare() {
  assert.ok(!s.customerId, 'Existing fixtures require cleanup-only or run phase')
  assert.equal((await customers()).length, 0); save()
  try {
    await step('F12-C01', 'Create synthetic customer and issue normal one-time customer invitation', async () => {
      const u = await c.call('POST', '/psi/unit', { unitName: s.prefix, unitType: 'CUSTOMER', unitNature: 'ENTERPRISE', status: 1, remark: s.runId })
      s.customerId = String(u.id || u); save()
      const invite = await c.call('POST', '/eoms/admin/h5/auth/invitations', { customerId: s.customerId })
      assert.equal(String(invite.customerId), s.customerId); assert.ok(invite.invitationCode)
      fs.writeFileSync(privateFile('h5-invitation'), JSON.stringify(invite), { mode: 0o600 })
      return { customerId: s.customerId, normalInvitationIssued: true, credentialLogged: false }
    })
    await captcha()
  } catch (e) { await step('F12-CLEANUP', 'Compensate only owned H5 application, session and customer', cleanup); throw e }
}
async function run() {
  assert.ok(s.customerId && !s.cleaned)
  try {
    await step('F12-C02', 'Exchange normal invitation and current real captcha for matching customer session', async () => {
      const invite = JSON.parse(fs.readFileSync(privateFile('h5-invitation')))
      const challenge = JSON.parse(fs.readFileSync(path.join(c.dir, 'h5-challenge.json')))
      const answer = JSON.parse(fs.readFileSync(path.join(c.dir, 'h5-captcha-answer.json')))
      assert.equal(answer.uuid, challenge.uuid); assert.ok(Date.now() < Number(challenge.expireTime))
      const d = await call('POST', '/eoms/h5/auth/token', { invitationCode: invite.invitationCode, captcha: answer.answer, uuid: challenge.uuid })
      assert.ok(d.token); token = d.token
      fs.writeFileSync(privateFile('h5-session'), JSON.stringify({ token }), { mode: 0o600 })
      fs.unlinkSync(privateFile('h5-invitation')); fs.unlinkSync(path.join(c.dir, 'h5-captcha-answer.json'))
      assert.equal(String(d.customerId), s.customerId)
      return { customerIdentityMatches: true, sessionIssued: true, credentialLogged: false }
    })
    await step('F12-C03', 'New customer sees empty own application list', async () => {
      const d = await call('GET', '/eoms/h5/repair/list'); assert.equal(Number(d.total), 0)
      return { ownApplicationCount: 0 }
    })
    await step('F12-PHOTO', 'Upload synthetic PNG and preview server-watermarked JPEG without camera or GPS', async () => {
      const form = new FormData(); form.append('file', new Blob([syntheticPng()], { type: 'image/png' }), s.prefix + '-photo.png')
      form.append('serviceAddress', 'Synthetic workflow address'); form.append('remark', s.runId)
      const response = await fetch(c.scope.api + '/eoms/h5/repair/upload', { method: 'POST', body: form,
        headers: { 'h5-token': token }, redirect: 'manual', signal: AbortSignal.timeout(30000) })
      assert.equal(response.status, 200); const d = await response.json(); assert.equal(d.success, true, String(d.msg))
      s.photoId = String(d.data.fileId); save(); assert.match(s.photoId, /^\d+$/)
      const preview = await fetch(c.scope.api + '/eoms/file/preview/' + s.photoId, { redirect: 'manual',
        headers: { 'h5-token': token }, signal: AbortSignal.timeout(20000) })
      assert.equal(preview.status, 200); assert.match(preview.headers.get('content-type'), /image\/jpeg/)
      const bytes = Buffer.from(await preview.arrayBuffer()); assert.equal(bytes.subarray(0, 3).toString('hex'), 'ffd8ff')
      fs.writeFileSync(path.join(c.dir, 'h5-watermarked.jpg'), bytes)
      return { fileId: s.photoId, serverJpegBytes: bytes.length, cameraUsed: false, gpsUsed: false, externalMapCalled: false }
    })
    await step('F12-C04', 'Customer submits repair and PC sees the same owned customer/application', async () => {
      const dict = await c.call('GET', '/system/common/dict/eoms_fault_type'); assert.ok(dict.length, 'Current fault-type dictionary required')
      const d = await call('POST', '/eoms/h5/repair/apply', { contactName: s.prefix, contactPhone: '13800000000',
        faultType: String(dict[0].value), faultDesc: s.prefix, photos: s.photoId, address: 'Synthetic workflow address', remark: s.runId })
      s.applyId = String(d.id); save()
      const pc = await c.call('GET', '/eoms/repair/apply/' + s.applyId)
      assert.equal(pc.status, 'WAIT'); assert.equal(String(pc.customerId), s.customerId)
      const mine = await call('GET', '/eoms/h5/repair/detail/' + s.applyId)
      assert.equal(String(mine.apply.id), s.applyId); assert.equal(String(mine.apply.customerId), s.customerId)
      return { applyId: s.applyId, pcAndCustomerSameRecord: true, status: 'WAIT' }
    })
    await step('F12-C05', 'PC confirms and closes with reason; customer detail reflects both transitions', async () => {
      await c.call('PUT', '/eoms/repair/apply/confirm/' + s.applyId + '?remark=' + s.runId)
      assert.equal((await call('GET', '/eoms/h5/repair/detail/' + s.applyId)).apply.status, 'CONFIRM')
      const reason = s.runId + ' synthetic completion'
      await c.call('PUT', '/eoms/repair/apply/close/' + s.applyId + '?remark=' + encodeURIComponent(reason))
      const mine = await call('GET', '/eoms/h5/repair/detail/' + s.applyId)
      assert.equal(mine.apply.status, 'CANCEL'); assert.equal(mine.apply.remark, reason)
      return { customerSawConfirmation: true, customerSawClosure: true, reasonPersisted: true }
    })
  } finally { await step('F12-CLEANUP', 'Compensate only owned H5 application, session and customer', cleanup) }
}
;(async () => {
  if (phase === 'prepare') await prepare()
  else if (phase === 'captcha') await captcha()
  else if (phase === 'run') await run()
  else if (phase === 'cleanup-only') await step('F12-CLEANUP', 'Compensate only owned H5 application, session and customer', cleanup)
  else throw Error('Use <run-dir> prepare|captcha|run|cleanup-only')
})().catch(e => { console.error(String(e.message).slice(0, 220)); process.exitCode = 1 })
