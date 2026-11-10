/** Local in-use PDF verification regression; only own synthetic signed fixtures.
 * Uses an explicitly provisioned session. No login changes or browser state reads.
 */
import assert from 'node:assert/strict'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { createRequire } from 'node:module'
import { pathToFileURL } from 'node:url'

const run = process.argv.find(x => x.startsWith('--run='))?.slice(6)
assert.match(run || '', /^R\d+$/)
const root = resolve(import.meta.dirname, '../..'), local = name => resolve(root, '.local', `${run.toLowerCase()}-${name}`)
const ledger = JSON.parse(readFileSync(local('ledger.json'), 'utf8'))
assert.equal(ledger.run, run); assert.ok(!ledger.closed)
assert.equal(ledger.prefix, 'ITLS-TEST-' + run)
for (const key of ['signSource', 'signedFile', 'unifiedFile']) assert.ok(ledger.objects[key].marker.startsWith(ledger.prefix))
const deploymentFile = process.argv.find(x => x.startsWith('--deployment='))?.slice(13)
assert.ok(deploymentFile?.startsWith('.local/'), 'Provide reviewed local deployment evidence')
const deployment = JSON.parse(readFileSync(resolve(root, deploymentFile), 'utf8'))
assert.equal(deployment.ready, true)
const runtime = spawnSync('docker', ['exec', 'ls-aegis-mcs-backend', 'sha256sum', '/app/jars/ls-aegis-server.jar'], { encoding: 'utf8', timeout: 20000 })
assert.equal(runtime.status, 0); const runtimeSha256 = runtime.stdout.trim().split(/\s+/)[0]
assert.equal(runtimeSha256, deployment.sha256)
const token = JSON.parse(readFileSync(local('session.json'), 'utf8')).token
const base = 'http://127.0.0.1:8000', baseline = process.argv.includes('--baseline')
const sql = query => {
  assert.match(query, /^SELECT\b/); assert.ok(!query.includes(';'))
  const r = spawnSync('docker', ['exec', 'ls-aegis-mysql', 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B ls_aegis -e "$1"', 'sh', query], { encoding: 'utf8', timeout: 20000 })
  assert.equal(r.status, 0, 'Read-only persistence check failed'); return r.stdout.trim()
}
const counts = () => ['sys_file', 'sign_file', 'sign_record'].map(t => Number(sql(`SELECT COUNT(*) FROM ${t}`)))
const before = counts(), results = [], receiptChecks = []
async function upload(name, bytes, filename, expected) {
  const form = new FormData(); if (bytes !== null) form.set('file', new Blob([bytes], { type: 'application/pdf' }), filename)
  const r = await fetch(base + '/sign/verify/fingerprint/upload', { method: 'POST', headers: { Authorization: 'Bearer ' + token }, body: form, signal: AbortSignal.timeout(30000) })
  const body = await r.json(); results.push({ id: name, http: r.status, body })
  if (!baseline) expected(body, bytes)
}
const signed = readFileSync(local('signed.pdf'))
const unifiedResponse = await fetch(base + '/sign/file/download/' + ledger.objects.unifiedFile.id, { headers: { Authorization: 'Bearer ' + token }, signal: AbortSignal.timeout(30000) })
assert.equal(unifiedResponse.status, 200)
const unified = Buffer.from(await unifiedResponse.arrayBuffer()); assert.equal(unified.subarray(0, 4).toString(), '%PDF')
writeFileSync(local('unified-signed.pdf'), unified)
const valid = (b, bytes) => {
  assert.equal(b.success, true); assert.equal(b.data.valid, true); assert.equal(b.data.signaturePresent, true)
  assert.equal(b.data.trustVerified, false); assert.equal(b.data.ca.result, '信任链未验证'); assert.equal(b.data.ca.resultType, 'warning')
  assert.equal(b.data.fingerprint.currentFileHash, createHash('sha256').update(bytes).digest('hex'))
}
try {
  await upload('ordinary-signed', signed, `${run}-ordinary.pdf`, valid)
  await upload('unified-signed-uppercase', unified, `${run}-unified.PDF`, valid)
  // Reuse the generated synthetic source from its registered platform file.
  const src = await fetch(base + '/system/common/file/' + ledger.objects.signSource.id, { headers: { Authorization: 'Bearer ' + token }, signal: AbortSignal.timeout(30000) })
  assert.equal(src.status, 200); const source = Buffer.from(await src.arrayBuffer()); assert.equal(source.subarray(0, 4).toString(), '%PDF')
  await upload('unsigned-pdf', source, `${run}-unsigned.pdf`, b => { assert.equal(b.success, true); assert.equal(b.data.valid, false); assert.equal(b.data.signaturePresent, false); assert.equal(b.data.fingerprint.result, '未检测到数字签名'); assert.equal(b.data.fingerprint.resultType, 'warning'); assert.ok(!b.data.fingerprint.compareTip.includes('被修改')) })
  for (const [id, bytes, name] of [['empty-file', Buffer.alloc(0), 'empty.pdf'], ['wrong-extension', signed, 'document.txt'], ['malformed-pdf', Buffer.from('ITLS TEST ONLY not a PDF'), 'invalid.pdf']]) {
    await upload(id, bytes, name, b => { assert.equal(b.success, true); assert.equal(b.data.valid, false); assert.ok(!JSON.stringify(b.data).includes('Exception')) })
  }
  await upload('missing-file', null, '', b => assert.equal(b.success, false))
  if (!baseline) {
    const req = createRequire(resolve(root, 'ls-aegis-web/package.json'))
    const pdfjs = await import(pathToFileURL(req.resolve('pdfjs-dist/legacy/build/pdf.mjs')).href)
    for (const [recordKey, fileKey] of [['signRecord', 'signedFile'], ['unifiedRecord', 'unifiedFile']]) {
      const id = ledger.objects[recordKey].id
      const response = await fetch(base + '/sign/execute/receipt/' + id, { headers: { Authorization: 'Bearer ' + token }, signal: AbortSignal.timeout(30000) })
      const body = await response.json(); assert.equal(body.success, true); assert.equal(body.data.recordId, id)
      assert.equal(String(body.data.signedFileId), ledger.objects[fileKey].id); assert.equal(body.data.verifyStatus, '1')
      const download = await fetch(base + '/sign/execute/receipt/' + id + '/download', { headers: { Authorization: 'Bearer ' + token }, signal: AbortSignal.timeout(30000) })
      assert.equal(download.status, 200); assert.ok(download.headers.get('content-type').includes('application/pdf'))
      const bytes = new Uint8Array(await download.arrayBuffer()), size = bytes.length
      writeFileSync(local(recordKey + '-receipt.pdf'), bytes)
      const doc = await pdfjs.getDocument({ data: bytes, cMapUrl: resolve(req.resolve('pdfjs-dist/package.json'), '../cmaps') + '/', cMapPacked: true, useWorkerFetch: false, useSystemFonts: true, disableFontFace: true, isEvalSupported: false }).promise
      let text = ''
      try { for (let i = 1; i <= doc.numPages; i++) text += (await (await doc.getPage(i)).getTextContent()).items.map(x => x.str || '').join(' ') } finally { await doc.destroy() }
      text = text.replace(/\s/g, '')
      assert.ok(text.includes(id)); assert.ok(text.includes(body.data.fingerprintSha256))
      assert.ok(text.includes('证书信息（SHA-256指纹）')); assert.ok(text.includes('证书指纹')); assert.ok(!text.includes('文件指纹(SHA-256)'))
      assert.ok(text.includes('信任链及撤销状态未验证')); assert.ok(!text.includes('自动验签并生成'))
      receiptChecks.push({ recordId: id, signedFileId: ledger.objects[fileKey].id, pdfBytes: size, recordAndCertificateMatched: true, trustBoundaryExplicit: true })
    }
  }
  const after = counts(); assert.deepEqual(after, before, 'Verification must not persist uploaded PDF or signature objects')
  const proof = { run, at: new Date().toISOString(), backendSha256: runtimeSha256, baseline, results, receiptChecks, countsBefore: before, countsAfter: after, verificationCreatedObjects: 0 }
  writeFileSync(local(baseline ? 'upload-baseline.json' : `upload-${runtimeSha256.slice(0,12)}.json`), JSON.stringify(proof, null, 2))
  console.log(`${run} PDF upload ${baseline ? 'baseline collected' : 'PASS'}: ${results.length} cases; persisted objects 0`)
} catch (e) {
  writeFileSync(local('upload-failure.json'), JSON.stringify({ at: new Date().toISOString(), results, error: e.message }, null, 2)); throw e
}
