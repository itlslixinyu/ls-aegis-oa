/** 有界隔离性能对比：只操作本次业务夹具，禁止指定在用API地址。 */
import assert from 'node:assert/strict'
import { readFileSync, writeFileSync, existsSync, realpathSync } from 'node:fs'
import { createRequire } from 'node:module'
import { resolve, sep } from 'node:path'
import { spawnSync } from 'node:child_process'
import { createHash, randomUUID } from 'node:crypto'
import { performance } from 'node:perf_hooks'

const root = resolve(import.meta.dirname, '../..')
process.chdir(root)
const argument = name => process.argv.find(value => value.startsWith(`--${name}=`))?.slice(name.length + 3)
const candidate = name => {
  assert.ok(argument(name), `必须显式指定 --${name}=候选目录`)
  const dir = realpathSync(resolve(root, argument(name)))
  assert.ok(dir.startsWith(root + sep), '候选包必须位于当前仓库内')
  return { dir, sha256: createHash('sha256').update(readFileSync(resolve(dir, 'ls-aegis-server.jar'))).digest('hex') }
}
const before = candidate('before-dir'), after = candidate('after-dir')
const scenario = argument('scenario') || 'bank-candidates'
assert.ok(['bank-candidates', 'writeoff-summary'].includes(scenario), '不支持的隔离对比场景')
// 当前统一编号采用两位日流水，已有业务夹具也会消耗编号；不绕过该业务限制造数。
const rows = Number(argument('rows') || 80), samples = Number(argument('samples') || 40)
assert.ok(Number.isInteger(rows) && rows >= 1 && rows <= 80, '当前编号规则下合成单据/核销数限制为1..80')
assert.ok(Number.isInteger(samples) && samples >= 20 && samples <= 100)
const manifest = resolve(root, '.local/business-env.json')
assert.ok(!existsSync(manifest), '已有隔离业务夹具，不能覆盖或借用')
const output = resolve(root, '.local', scenario === 'bank-candidates' ? 'isolated-capacity-results.json' : 'writeoff-capacity-results.json')
const result = { at: new Date().toISOString(), beforeSha256: before.sha256, afterSha256: after.sha256,
  scenario, syntheticBusinessRows: rows, samplesPerVariant: samples, maximumReadConcurrency: 5, status: 'RUNNING' }
const save = () => writeFileSync(output, JSON.stringify(result, null, 2) + '\n')
save()
function command(program, args) {
  const r = spawnSync(program, args, { encoding: 'utf8', maxBuffer: 4 * 1024 * 1024, windowsHide: true })
  if (r.status !== 0) throw new Error(`${program} ${args[0]} 未成功，拒绝继续操作夹具`)
  return r.stdout.trim()
}
const docker = (...args) => command('docker', args)
const label = id => docker('inspect', '--format', '{{index .Config.Labels "itls.quality.run"}}', id)
const pause = ms => new Promise(done => setTimeout(done, ms))
let state, baselineId
try {
  const memory = Number(docker('exec', 'itls-r32-master-ref', 'sh', '-c', "awk '/MemAvailable/ {print $2}' /proc/meminfo"))
  assert.ok(memory > 2 * 1024 * 1024, '隔离负载前Docker可用内存不足2GiB，停止而不挤占在用服务')
  const setup = spawnSync('python', ['-X', 'utf8', 'scripts/quality/run_business_flows.py', '--keep', '--jar-dir', after.dir],
    { encoding: 'utf8', maxBuffer: 4 * 1024 * 1024, windowsHide: true })
  writeFileSync(resolve(root, '.local', scenario === 'bank-candidates' ? 'isolated-capacity-setup.log' : 'writeoff-capacity-setup.log'), (setup.stdout || '') + (setup.stderr || ''))
  if (existsSync(manifest)) state = JSON.parse(readFileSync(manifest, 'utf8'))
  assert.equal(setup.status, 0, '独立业务闭环未通过，不能继续容量测量')
  assert.match(state.runId, /^[a-f0-9]{10}$/)
  result.runId = state.runId
  for (const id of state.containers) assert.equal(label(id), state.runId)
  assert.equal(docker('network', 'inspect', '--format', '{{index .Labels "itls.quality.run"}}', state.network), state.runId)
  assert.equal(new URL(state.url).hostname, '127.0.0.1')
  const config = JSON.parse(docker('inspect', state.backend))[0]
  const env = Object.fromEntries(config.Config.Env.map(value => value.split(/=(.*)/s).slice(0, 2)))
  const options = ['run', '-d', '--name', `itls-test-baseline-${state.runId}`, '--network', state.network,
    '--label', `itls.quality.run=${state.runId}`, '-p', '127.0.0.1::8000', '--tmpfs', '/app/data:rw,size=128m', '-w', '/app',
    '--mount', `type=bind,source=${before.dir},target=/app/jars,readonly`]
  for (const key of ['SPRING_PROFILES_ACTIVE', 'SPRING_LIQUIBASE_ENABLED', 'DB_HOST', 'DB_USER', 'DB_NAME', 'DB_PWD',
    'REDIS_HOST', 'REDIS_PWD', 'JWT_SECRET_KEY', 'TOKEN_SECRET', 'SNAIL_JOB_ENABLED', 'GM_KEY_STORE_PATH', 'TZ']) {
    assert.ok(key in env, `缺少夹具变量${key}`)
    options.push('-e', `${key}=${env[key]}`)
  }
  options.push(config.Config.Image, 'java', '-Xms256m', '-Xmx768m', '-jar', '/app/jars/ls-aegis-server.jar')
  baselineId = docker(...options)
  assert.equal(label(baselineId), state.runId)
  state.containers.push(baselineId)
  writeFileSync(manifest, JSON.stringify(state))
  const baselineUrl = 'http://127.0.0.1:' + docker('port', baselineId, '8000/tcp').split(':').at(-1)
  const targetUrl = state.url
  let ready = false
  for (let attempt = 0; attempt < 180; attempt++) {
    try {
      const r = await fetch(baselineUrl + '/auth/gm/public-key', { signal: AbortSignal.timeout(1000) })
      if (r.ok && (await r.json()).success) { ready = true; break }
    } catch {}
    await pause(1000)
  }
  assert.ok(ready, '基线后端未就绪')
  const sql = query => docker('exec', '-e', `MYSQL_PWD=${state.password}`, state.mysql,
    'mysql', '-uroot', '--default-character-set=utf8mb4', '-N', '-B', 'ls_aegis', '-e', query)
  const webRequire = createRequire(resolve(root, 'ls-aegis-web/package.json'))
  const { sm2 } = webRequire('sm-crypto')
  const clientId = sql('SELECT client_id FROM sys_client WHERE status=1 ORDER BY id LIMIT 1')
  async function api(base, path, { token, method = 'GET', data } = {}) {
    assert.ok(path.startsWith('/auth/') || path.startsWith('/psi/'))
    const response = await fetch(base + path, { method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: data === undefined ? undefined : JSON.stringify(data), signal: AbortSignal.timeout(15000) })
    assert.equal(response.status, 200)
    const body = await response.json()
    assert.equal(body.success, true, `${method} ${path}: ${body.msg}`)
    return body.data
  }
  async function login(base) {
    const key = await api(base, '/auth/gm/public-key')
    const cipher = sm2.doEncrypt(state.password, key.publicKeyHex, 1)
    return (await api(base, '/auth/login', { method: 'POST', data: {
      username: `test_${state.runId}_admin`, password: cipher.startsWith('04') ? cipher : `04${cipher}`,
      authType: 'ACCOUNT', clientId,
    } })).token
  }
  const targetToken = await login(targetUrl), baselineToken = await login(baselineUrl)
  const business = JSON.parse(readFileSync(resolve(root, '.local/business-results.json'), 'utf8'))
  assert.equal(business.runId, state.runId, '业务结果必须来自本轮独立夹具')
  const customer = business.results.find(row => row.id === 'FIXTURE-01').evidence.customer
  const prefix = `PERF-${state.runId}`, date = new Date().toISOString().slice(0, 19)
  let readPath, validate
  if (scenario === 'writeoff-summary') {
    const fixture = business.results.find(row => row.id === 'FIXTURE-01').evidence
    const stock = await api(targetUrl, '/psi/stock-in', { token: targetToken, method: 'POST', data: {
      inType: 'PURCHASE', inDate: date, warehouseId: fixture.warehouse, unitId: fixture.supplier,
      items: [{ goodsId: fixture.goods, quantity: 1, taxInclusivePrice: 100, taxRate: 0 }], remark: prefix,
    } })
    await api(targetUrl, `/psi/stock-in/${stock}/confirm`, { token: targetToken, method: 'POST', data: { idempotentKey: randomUUID() } })
    const openId = sql(`SELECT id FROM psi_payable WHERE source_id=${stock} AND deleted=0`)
    assert.match(openId, /^\d+$/)
    const cash = await api(targetUrl, '/psi/payment', { token: targetToken, method: 'POST', data: {
      paymentDate: date, unitId: fixture.supplier, amount: rows, payMethod: 'CASH', remark: prefix,
    } })
    await api(targetUrl, `/psi/payment/${cash}/confirm`, { token: targetToken, method: 'POST', data: { idempotentKey: randomUUID() } })
    const masters = []
    for (let i = 0; i < rows; i++) masters.push(String(await api(targetUrl, `/psi/payment/${cash}/write-off`, {
      token: targetToken, method: 'POST', data: { items: [{ openId, amount: 1 }], reason: `${prefix}-${i}` },
    })))
    assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_write_off_item WHERE payment_id=${cash} AND direction='WRITE_OFF' AND deleted=0`)), rows)
    assert.equal(Number(sql(`SELECT unallocated_amount FROM psi_payment WHERE id=${cash}`)), 0)
    readPath = `/psi/payment/${cash}/write-offs`
    const expected = masters.sort()
    validate = list => {
      assert.deepEqual(list.map(item => String(item.writeOffId)).sort(), expected)
      assert.ok(list.every(item => Number(item.amount) === 1 && !item.reversed))
    }
    result.syntheticWriteOffMasters = rows
  } else {
    await api(targetUrl, '/psi/account', { token: targetToken, method: 'POST', data: {
      accountName: prefix, bankName: '合成负载测试', accountNo: prefix, status: 1,
    } })
    const accountId = sql(`SELECT id FROM psi_company_account WHERE account_name='${prefix}' AND deleted=0`)
    assert.match(accountId, /^\d+$/)
    let targetReceipt
    // 串行通过业务API生成有效已确认资料，不用SQL伪造财务状态。
    for (let i = 0; i <= rows; i++) {
      const id = await api(targetUrl, '/psi/receipt', { token: targetToken, method: 'POST', data: {
        unitId: customer, accountId, amount: i === rows ? 777.77 : 500, receiptDate: date,
        payMethod: 'TRANSFER', remark: `${prefix}-${i}`,
      } })
      await api(targetUrl, `/psi/receipt/${id}/confirm`, { token: targetToken, method: 'POST', data: { idempotentKey: randomUUID() } })
      if (i === rows) targetReceipt = String(id)
      if (i && i % 100 === 0) console.log(`已确认合成回款 ${i}/${rows}`)
    }
    assert.equal(Number(sql(`SELECT COUNT(*) FROM psi_receipt WHERE account_id=${accountId} AND doc_status='CONFIRMED' AND deleted=0`)), rows + 1)
    await api(targetUrl, '/psi/bank/import', { token: targetToken, method: 'POST', data: { accountId,
      items: [{ stmtDate: date, direction: 'IN', amount: 777.77, bankSeqNo: prefix, summary: prefix }] } })
    const bankId = sql(`SELECT id FROM psi_bank_statement WHERE bank_seq_no='${prefix}' AND deleted=0`)
    assert.match(bankId, /^\d+$/)
    readPath = `/psi/bank/${bankId}/candidates`
    validate = list => assert.deepEqual(list.map(item => String(item.value)), [targetReceipt])
  }
  async function read(base, token) {
    const begin = performance.now()
    const list = await api(base, readPath, { token })
    validate(list)
    return performance.now() - begin
  }
  for (let i = 0; i < 5; i++) { await read(baselineUrl, baselineToken); await read(targetUrl, targetToken) }
  result.measurements = []
  for (const concurrency of [1, 5]) {
    const timings = { before: [], after: [] }
    for (let offset = 0; offset < samples; offset += concurrency) {
      const order = offset % 2 ? ['after', 'before'] : ['before', 'after']
      for (const variant of order) {
        const n = Math.min(concurrency, samples - offset)
        const durations = await Promise.all(Array.from({ length: n }, () => variant === 'before'
          ? read(baselineUrl, baselineToken) : read(targetUrl, targetToken)))
        timings[variant].push(...durations)
      }
    }
    for (const [variant, durations] of Object.entries(timings)) {
      durations.sort((a, b) => a - b)
      const round = value => Math.round(value * 100) / 100
      result.measurements.push({ concurrency, variant, samples: durations.length, failures: 0,
        p50Ms: round(durations[Math.ceil(durations.length * .5) - 1]),
        p95Ms: round(durations[Math.ceil(durations.length * .95) - 1]),
        meanMs: round(durations.reduce((a, b) => a + b, 0) / durations.length) })
    }
  }
  result.docker = JSON.parse(docker('info', '--format', '{"cpus":{{.NCPU}},"memoryBytes":{{.MemTotal}}}'))
  if (scenario === 'bank-candidates') result.confirmedSyntheticReceipts = rows + 1
  result.status = 'PASS'
  console.log(JSON.stringify(result.measurements))
} catch (error) {
  result.status = 'FAIL'; result.error = error.message
  process.exitCode = 1
  console.error(error.message)
} finally {
  if (baselineId && docker('ps', '-aq', '--filter', `id=${baselineId}`)) {
    assert.equal(label(baselineId), state.runId)
    docker('rm', '--force', baselineId)
  }
  if (existsSync(manifest)) {
    const owned = JSON.parse(readFileSync(manifest, 'utf8'))
    assert.equal(owned.runId, state?.runId, '禁止清理其他夹具')
    command('python', ['-X', 'utf8', 'scripts/quality/run_business_flows.py', '--cleanup'])
  }
  result.fixtureCleaned = !existsSync(manifest)
  save()
}
