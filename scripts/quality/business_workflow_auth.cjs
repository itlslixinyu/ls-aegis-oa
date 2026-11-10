/** Real, normal API login/session/logout workflow. No fixture DB or credential defaults. */
const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const { execFileSync } = require('node:child_process')
const { createRequire } = require('node:module')
const { Writable } = require('node:stream')
const assert = require('node:assert/strict')
const root = path.resolve(__dirname, '../..')
const args = process.argv.slice(2)
function option(name) { const i = args.indexOf(name); return i >= 0 ? args[i + 1] : undefined }
const runDir = path.resolve(root, option('--run-dir') || '')
const relative = path.relative(path.join(root, '.local/business-workflow'), runDir)
if (!relative || relative.startsWith('..') || path.isAbsolute(relative)) throw Error('Use a specific .local/business-workflow/<run-id> directory')
const api = option('--api') || 'http://127.0.0.1:8000'
const url = new URL(api)
if (url.protocol !== 'http:' || !['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname) || url.port !== '8000' || url.username || url.password) throw Error('Only the authorized local API8000 is supported')
fs.mkdirSync(runDir, { recursive: true })
const ledger = path.join(runDir, 'cases.jsonl')
function record(row) {
  const scopeFile = path.join(runDir, 'scope.json'), runtimeFile = path.join(runDir, 'runtime-build.json')
  const scope = fs.existsSync(scopeFile) ? JSON.parse(fs.readFileSync(scopeFile)) : {}
  const runtime = fs.existsSync(runtimeFile) ? JSON.parse(fs.readFileSync(runtimeFile)) : { api, versionVerified: false }
  fs.appendFileSync(ledger, JSON.stringify({ at: new Date().toISOString(), sourceHead: scope.head, build: runtime,
    role: scope.username, preconditions: 'Explicit authorized identity and normal captcha; no settings changes',
    evidence: path.relative(root, ledger), defect: null, retest: null, cleanup: 'See cleanup.json and logout cases', ...row }) + '\n')
  console.log(`${row.case_id} ${row.status} ${row.actual?.message || ''}`)
}
async function request(method, endpoint, token, body) {
  const res = await fetch(api + endpoint, { method, redirect: 'manual', signal: AbortSignal.timeout(15000),
    headers: { 'Content-Type': 'application/json', 'User-Agent': 'ITLS-Workflow/' + path.basename(runDir), ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }) })
  if (res.status >= 300 && res.status < 400) throw Error('Unexpected redirect; not forwarding credentials')
  const data = await res.json()
  return { http: res.status, body: data }
}
const ok = result => result.http === 200 && result.body.success === true && String(result.body.code) === '0'
const denied = result => [401, 403].includes(result.http) || (result.http === 200 && result.body.success === false && ['401', '403'].includes(String(result.body.code)))
async function step(id, description, fn) {
  try { const actual = await fn(); record({ case_id: id, module: 'F01-auth-session', action: description, expected: description, actual, status: 'PASS' }); return actual }
  catch (error) { record({ case_id: id, module: 'F01-auth-session', action: description, expected: description, actual: { message: String(error.message).slice(0, 220) }, status: 'FAIL' }); throw error }
}
function hiddenPassword() {
  const input = require('node:readline').createInterface({ input: process.stdin, output: new Writable({ write(_s, _e, done) { done() } }), terminal: true })
  console.log('CREDENTIAL_INPUT_READY_HIDDEN')
  return new Promise(resolve => input.once('line', value => { input.close(); resolve(value) }))
}
async function prepare() {
  if (fs.existsSync(path.join(runDir, 'session-private.json'))) throw Error('An owned workflow session is already present; finish/logout it before preparing another')
  const username = option('--username'), clientId = option('--client-id')
  if (!username || !clientId) throw Error('Explicit authorized username and public client-id required')
  const scope = { runId: path.basename(runDir), api, username, clientId, head: execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim(), startedAt: new Date().toISOString() }
  fs.writeFileSync(path.join(runDir, 'scope.json'), JSON.stringify(scope, null, 2))
  await step('F01-01', 'Public GM login prerequisite responds successfully', async () => {
    const key = await request('GET', '/auth/gm/public-key'); assert.ok(ok(key)); assert.ok(key.body.data.enable && key.body.data.publicKeyHex)
    return { http: key.http, code: key.body.code, gmEnabled: true, publicKeyFingerprint: crypto.createHash('sha256').update(key.body.data.publicKeyHex).digest('hex') }
  })
  await step('F01-02', 'Unauthenticated current-user request is rejected', async () => {
    const result = await request('GET', '/auth/user/info'); assert.ok(denied(result)); return { http: result.http, code: result.body.code }
  })
  const result = await request('GET', '/captcha/image'); assert.ok(ok(result)); const challenge = result.body.data
  fs.writeFileSync(path.join(runDir, 'challenge.json'), JSON.stringify(challenge))
  if (challenge.isEnabled) {
    assert.ok(challenge.img && challenge.uuid)
    fs.writeFileSync(path.join(runDir, 'captcha.gif'), Buffer.from(challenge.img.split(',').at(-1), 'base64'))
  }
  record({ case_id: 'F01-PREPARE', module: 'F01-auth-session', status: 'PASS', actual: { captchaEnabled: challenge.isEnabled, captchaImageSaved: Boolean(challenge.img), settingsChanged: false } })
}
async function authenticate() {
  const scope = JSON.parse(fs.readFileSync(path.join(runDir, 'scope.json')))
  assert.equal(scope.api, api)
  const challenge = JSON.parse(fs.readFileSync(path.join(runDir, 'challenge.json')))
  const answer = challenge.isEnabled ? JSON.parse(fs.readFileSync(path.join(runDir, 'captcha-answer.json'))) : null
  if (answer) { assert.equal(answer.uuid, challenge.uuid); assert.ok(Date.now() < Number(challenge.expireTime), 'Challenge expired; prepare a new real image') }
  let password = await hiddenPassword(), token = '', loggedOut = false, retained = false, tenantId
  if (!password) throw Error('No user-provided credential supplied')
  try {
    await step('F01-03', 'Authorized account completes normal encrypted API login', async () => {
      const key = await request('GET', '/auth/gm/public-key'); assert.ok(ok(key))
      const req = createRequire(path.join(root, 'ls-aegis-web/package.json')), { sm2 } = req('sm-crypto')
      const raw = sm2.doEncrypt(password, key.body.data.publicKeyHex, 1); password = ''
      const result = await request('POST', '/auth/login', '', { username: scope.username, password: raw.startsWith('04') ? raw : '04' + raw,
        authType: 'ACCOUNT', clientId: scope.clientId, uuid: challenge.uuid, captcha: answer?.answer || '' })
      if (!ok(result)) throw Error(`Normal login rejected: HTTP ${result.http}; code=${result.body.code}; message=${String(result.body.msg || '').slice(0, 120)}`)
      assert.ok(result.body.data?.token); token = result.body.data.token; tenantId = result.body.data.tenantId
      return { http: result.http, code: result.body.code, tokenIssued: true, tokenPersisted: false }
    })
    await step('F01-04', 'Current-user identity matches authorized login account', async () => {
      const result = await request('GET', '/auth/user/info', token); assert.ok(ok(result)); const user = result.body.data
      assert.equal(user.username, scope.username); assert.ok(Array.isArray(user.roles) && user.roles.length)
      return { http: result.http, code: result.body.code, userId: user.id, roleCount: user.roles.length, permissionCount: user.permissions?.length, pwdExpired: user.pwdExpired }
    })
    await step('F01-05', 'Authorized menu route tree is present and excludes retired tools', async () => {
      const result = await request('GET', '/auth/user/route', token); assert.ok(ok(result)); assert.ok(Array.isArray(result.body.data) && result.body.data.length)
      const flat = []
      function visit(items, parent = '') {
        for (const node of items) {
          const p = node.path?.startsWith('/') ? node.path : parent + '/' + (node.path || '')
          assert.ok(!/(?:^|\/)generator(?:\/|$)/.test(p) && p !== '/open/app', 'Retired function remains in authenticated route tree')
          flat.push({ path: p, name: node.name, title: node.meta?.title || node.title, component: node.component })
          if (node.children) visit(node.children, p)
        }
      }
      visit(result.body.data); fs.writeFileSync(path.join(runDir, 'menu-evidence.json'), JSON.stringify(flat, null, 2))
      return { http: result.http, code: result.body.code, topLevelRoutes: result.body.data.length, totalRoutes: flat.length }
    })
    if (args.includes('--keep-session')) {
      fs.writeFileSync(path.join(runDir, 'session-private.json'), JSON.stringify({ token, username: scope.username, tenantId, runId: scope.runId, api }), { mode: 0o600 })
      retained = true
      record({ case_id: 'AUTH-SETUP', module: 'workflow-session', status: 'PASS', actual: { sessionRetainedForNextSteps: true, completeLogoutWorkflowClaimed: false } })
      return
    }
    await step('F01-06', 'Logout revokes this authenticated session', async () => {
      const result = await request('POST', '/auth/logout', token); assert.ok(ok(result)); loggedOut = true; return { http: result.http, code: result.body.code }
    })
    await step('F01-07', 'Current-user request after logout is rejected', async () => {
      const result = await request('GET', '/auth/user/info', token); assert.ok(denied(result)); return { http: result.http, code: result.body.code }
    })
  } finally {
    password = ''
    if (token && !loggedOut && !retained) { try { const result = await request('POST', '/auth/logout', token); loggedOut = ok(result) } catch {} }
    const answerFile = path.join(runDir, 'captcha-answer.json')
    if (fs.existsSync(answerFile)) fs.unlinkSync(answerFile)
    fs.writeFileSync(path.join(runDir, 'cleanup.json'), JSON.stringify({ sessionIssued: Boolean(token), sessionLogoutSucceeded: loggedOut, passwordPersisted: false, tokenPersisted: retained, sessionRetainedForWorkflow: retained, businessFixturesCreated: false }, null, 2))
    token = ''
  }
}
;(async () => {
  if (option('--phase') === 'prepare') await prepare()
  else if (option('--phase') === 'authenticate') await authenticate()
  else throw Error('Use --phase prepare|authenticate')
})().catch(error => { console.error(String(error.message).slice(0, 220)); process.exitCode = 1 })
