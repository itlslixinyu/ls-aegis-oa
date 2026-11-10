/** Valid synthetic PDF lifecycle; no browser, camera or external storage settings changes. */
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')
const { WorkflowClient } = require('./business_workflow_client.cjs')
const c = new WorkflowClient(process.argv[2])
const ledger = path.join(c.dir, 'file-fixtures.json')
const s = fs.existsSync(ledger) ? JSON.parse(fs.readFileSync(ledger)) : {
  runId: c.scope.runId, prefix: 'ITLS-TEST-' + c.tag + '-' + crypto.randomBytes(6).toString('hex'),
}
assert.equal(s.runId, c.scope.runId)
const save = () => c.save('file-fixtures.json', s)
function pdf() {
  const stream = `BT /F1 12 Tf 40 100 Td (${s.prefix}) Tj ET\n`
  const objects = ['<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 200] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>', `<< /Length ${Buffer.byteLength(stream)} >>\nstream\n${stream}endstream`]
  let data = '%PDF-1.4\n', offsets = [0]
  objects.forEach((o, i) => { offsets.push(Buffer.byteLength(data)); data += `${i + 1} 0 obj\n${o}\nendobj\n` })
  const start = Buffer.byteLength(data)
  data += 'xref\n0 6\n0000000000 65535 f \n' + offsets.slice(1).map(x => String(x).padStart(10, '0') + ' 00000 n \n').join('')
  return Buffer.from(data + `trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${start}\n%%EOF\n`)
}
async function transfer(method, endpoint, body) {
  const res = await fetch(c.scope.api + endpoint, { method, body, redirect: 'manual', signal: AbortSignal.timeout(25000),
    headers: { Authorization: 'Bearer ' + c.session.token, 'User-Agent': 'ITLS-Workflow/' + s.runId } })
  assert.ok(res.status < 300 || res.status >= 400, 'No redirect forwarding')
  return res
}
const rows = d => d.list || d.records || []
async function find(recycle = false) {
  const d = await c.call('GET', '/system/file' + (recycle ? '/recycle' : '') + '?size=100&originalName=' + s.prefix)
  const owned = rows(d).filter(x => x.originalName?.startsWith(s.prefix))
  assert.ok(owned.length <= 1, 'Unexpected multiple fixture matches')
  for (const x of owned) if (s.id) assert.equal(String(x.id), s.id)
  return owned
}
async function download(expected) {
  const res = await transfer('GET', '/system/common/file/' + s.id)
  assert.equal(res.status, 200); assert.match(res.headers.get('content-type'), /application\/pdf/)
  const bytes = Buffer.from(await res.arrayBuffer()); assert.deepEqual(bytes, expected)
  return { bytes: bytes.length, sha256: crypto.createHash('sha256').update(bytes).digest('hex') }
}
async function cleanup() {
  const active = await find()
  for (const f of active) await c.call('DELETE', '/system/file', { ids: [String(f.id)] })
  const recycled = await find(true)
  for (const f of recycled) await c.call('DELETE', '/system/file/recycle/' + f.id)
  assert.equal((await find()).length, 0); assert.equal((await find(true)).length, 0)
  if (s.id) {
    const info = await c.raw('GET', '/system/common/file/' + s.id + '/info')
    assert.ok(info.body.success === false || info.body.data == null, 'Deleted file must expose no metadata')
    const res = await transfer('GET', '/system/common/file/' + s.id)
    assert.ok(res.headers.get('content-type')?.includes('json')); const d = await res.json(); assert.equal(d.success, false)
  }
  s.cleaned = true; save(); return { activeMatches: 0, recycledMatches: 0, oldDownloadUnavailable: Boolean(s.id) }
}
async function main() {
  save()
  if (process.argv.includes('--cleanup-only')) return c.step('F04-CLEANUP', 'Purge exact synthetic PDF only and verify metadata/download absence', cleanup)
  assert.ok(!s.id, 'Existing ledger: cleanup-only then a new run')
  assert.equal((await find()).length + (await find(true)).length, 0)
  const bytes = pdf(); fs.writeFileSync(path.join(c.dir, 'synthetic.pdf'), bytes)
  try {
    await c.step('F04-01', 'Upload structurally valid synthetic PDF and verify stored metadata and digest', async () => {
      const form = new FormData(); form.append('file', new Blob([bytes], { type: 'application/pdf' }), s.prefix + '.pdf')
      const res = await transfer('POST', '/system/file/upload?parentPath=%2F', form)
      const d = await res.json(); assert.equal(d.success, true, String(d.msg)); s.id = String(d.data.id); save()
      const info = await c.call('GET', '/system/common/file/' + s.id + '/info')
      assert.equal(info.originalName, s.prefix + '.pdf'); assert.equal(Number(info.size), bytes.length)
      const req = createRequire(path.resolve(__dirname, '../../ls-aegis-web/package.json'))
      const digest = req('sm-crypto').sm3(Array.from(bytes))
      const found = await c.call('GET', '/system/file/check?fileDigest=' + digest)
      assert.equal(String(found.id), s.id)
      return { fileId: s.id, size: bytes.length, sm3Matched: true }
    })
    await c.step('F04-02', 'Authenticated PDF download matches uploaded bytes', () => download(bytes))
    await c.step('F04-03', 'Rename display filename while preserving physical download', async () => {
      await c.call('PUT', '/system/file/' + s.id, { originalName: s.prefix + '-renamed.pdf' })
      const info = await c.call('GET', '/system/common/file/' + s.id + '/info'); assert.equal(info.originalName, s.prefix + '-renamed.pdf')
      return { renamed: true, ...await download(bytes) }
    })
    await c.step('F04-04', 'Recycle synthetic PDF and independently verify active absence', async () => {
      await c.call('DELETE', '/system/file', { ids: [s.id] })
      assert.equal((await find()).length, 0); assert.equal((await find(true)).length, 1)
      return { active: 0, recycled: 1 }
    })
    await c.step('F04-05', 'Restore recycled PDF with original ID and identical content', async () => {
      await c.call('PUT', '/system/file/recycle/restore/' + s.id)
      assert.equal((await find()).length, 1); assert.equal((await find(true)).length, 0)
      return { restored: true, ...await download(bytes) }
    })
  } finally { await c.step('F04-CLEANUP', 'Purge exact synthetic PDF only and verify metadata/download absence', cleanup) }
}
main().catch(e => { console.error(String(e.message).slice(0, 220)); process.exitCode = 1 })
