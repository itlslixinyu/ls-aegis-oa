const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const root = path.resolve(__dirname, '../..')
class WorkflowClient {
  constructor(dir) {
    this.dir = path.resolve(root, dir)
    const rel = path.relative(path.join(root, '.local/business-workflow'), this.dir)
    assert.ok(rel && !rel.startsWith('..') && !path.isAbsolute(rel), 'Owned workflow run directory required')
    this.scope = JSON.parse(fs.readFileSync(path.join(this.dir, 'scope.json')))
    this.session = JSON.parse(fs.readFileSync(path.join(this.dir, 'session-private.json')))
    assert.equal(path.basename(this.dir), this.scope.runId)
    this.tag = this.scope.runId.match(/^ITLS-TEST-(R\d{1,5})(?:-|$)/)?.[1]
      || crypto.createHash('sha256').update(this.scope.runId).digest('hex').slice(0, 6)
    assert.equal(this.scope.runId, this.session.runId); assert.equal(this.scope.api, this.session.api)
    const url = new URL(this.scope.api)
    const allowedPort = this.scope.environment === 'isolated-candidate-shared-db' ? '18003' : '8000'
    assert.ok(url.protocol === 'http:' && ['127.0.0.1', 'localhost', '[::1]'].includes(url.hostname) && url.port === allowedPort && !url.username && !url.password)
  }
  save(name, value) {
    assert.ok(/^[\w-]+\.json$/.test(name)); fs.writeFileSync(path.join(this.dir, name), JSON.stringify(value, null, 2))
  }
  record(row) {
    const runtimeFile = path.join(this.dir, 'runtime-build.json')
    const runtime = fs.existsSync(runtimeFile) ? JSON.parse(fs.readFileSync(runtimeFile)) : { api: this.scope.api, versionVerified: false }
    fs.appendFileSync(path.join(this.dir, 'cases.jsonl'), JSON.stringify({ at: new Date().toISOString(),
      sourceHead: this.scope.head, build: runtime, role: this.session.username,
      module: row.case_id?.split('-')[0], preconditions: 'Authorized local session; owned run fixtures only',
      expected: row.action, evidence: path.relative(root, path.join(this.dir, 'cases.jsonl')),
      defect: null, retest: null, cleanup: 'See corresponding module CLEANUP entry', ...row }) + '\n')
    console.log(`${row.case_id} ${row.status}`)
  }
  async raw(method, endpoint, data) {
    assert.ok(endpoint.startsWith('/') && !endpoint.startsWith('//') && !endpoint.includes('://'))
    const res = await fetch(this.scope.api + endpoint, { method, redirect: 'manual', signal: AbortSignal.timeout(20000),
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + this.session.token,
        'User-Agent': 'ITLS-Workflow/' + this.scope.runId, ...(this.session.tenantId ? { 'X-Tenant-Id': String(this.session.tenantId) } : {}) },
      ...(data === undefined ? {} : { body: JSON.stringify(data) }) })
    assert.ok(res.status < 300 || res.status >= 400, 'No redirect/credential forwarding')
    return { http: res.status, body: await res.json() }
  }
  async call(method, endpoint, data) {
    const r = await this.raw(method, endpoint, data)
    if (r.http !== 200 || r.body.success !== true || String(r.body.code) !== '0') throw Error(`${method} ${endpoint.split('?')[0]} HTTP=${r.http} code=${r.body.code} msg=${String(r.body.msg || '').slice(0, 150)}`)
    return r.body.data
  }
  async step(id, action, fn, metadata = {}) {
    try { const actual = await fn(); this.record({ ...metadata, case_id: id, action, expected: action, actual, status: 'PASS' }); return actual }
    catch (e) { this.record({ ...metadata, case_id: id, action, expected: action, actual: { message: String(e.message).slice(0, 220) }, status: 'FAIL' }); throw e }
  }
  async logout() {
    await this.call('POST', '/auth/logout')
    const checked = await this.raw('GET', '/auth/user/info')
    assert.ok([401, 403].includes(checked.http) || ['401', '403'].includes(String(checked.body.code)))
    fs.unlinkSync(path.join(this.dir, 'session-private.json'))
    const cleanupFile = path.join(this.dir, 'cleanup.json')
    const previous = fs.existsSync(cleanupFile) ? JSON.parse(fs.readFileSync(cleanupFile)) : {}
    this.save('cleanup.json', { ...previous, sessionLogoutSucceeded: true, tokenPersisted: false, sessionRetainedForWorkflow: false })
    this.record({ case_id: 'SESSION-CLEANUP', action: 'Revoke owned workflow session and verify rejection', status: 'PASS', actual: { sessionRevoked: true, privateTokenFileRemoved: true } })
    this.session.token = ''
  }
}
module.exports = { WorkflowClient }
