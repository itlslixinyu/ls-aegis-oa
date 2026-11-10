const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFile } = require('node:child_process')
const { promisify } = require('node:util')

const exec = promisify(execFile)
const repo = path.resolve(__dirname, '../..')
const local = path.join(repo, '.local')
const windows = process.platform === 'win32'

function fixture(t) {
  fs.mkdirSync(local, { recursive: true })
  const dir = fs.mkdtempSync(path.join(local, 'v2-support-'))
  t.after(() => {
    const resolved = fs.realpathSync(dir)
    assert.ok(resolved.startsWith(fs.realpathSync(local) + path.sep))
    assert.ok(path.basename(resolved).startsWith('v2-support-'))
    fs.rmSync(resolved, { recursive: true, force: true })
  })
  const project = path.join(dir, 'ITLS V2 & fixture')
  fs.mkdirSync(project)
  const write = (name, text) => {
    const target = path.join(project, name)
    fs.mkdirSync(path.dirname(target), { recursive: true })
    fs.writeFileSync(target, text)
  }
  const copy = name => fs.copyFileSync(path.join(repo, name), path.join(project, name))
  return { dir, project, write, copy }
}

function batch(command, cwd) {
  return exec(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', `"${command}"`], {
    cwd, windowsVerbatimArguments: true, timeout: 20000, maxBuffer: 2 * 1024 * 1024,
  })
}

for (const mode of [1, 2]) test(`V2 source archive migrates mode ${mode} and excludes local data/artifacts`, { skip: !windows }, async t => {
  const f = fixture(t)
  f.copy('pack.ps1')
  f.copy('pack.bat')
  f.write('VERSION', '2.0.0\n')
  f.write('pack.config.json', JSON.stringify({ BuildMode: mode, CopyClipboard: false, AutoUpload: false }))
  f.write('docker-compose.yml', 'services: {}\n')
  f.write('scripts/install.sh', '#!/bin/sh\nexit 0\n')
  f.write('.env.example', 'ITLS_VERSION=2.0.0\n')
  f.write('ls-aegis-web/.env.production', 'VITE_BASE=/\n')
  const excluded = ['.local/old.jar', 'rules/policy.mdc', 'skills/test/SKILL.md', 'Docs/old.sql', '.env', 'docker/.env',
    'ls-aegis-web/.env.production.local', 'tests/__pycache__/old.pyc', 'pack.config.json',
    'docker/ls-aegis-server/bin/ls-aegis-server.jar', 'ls-aegis-web/dist/index.html', 'ls-aegis-h5/dist-engineer/index.html']
  for (const name of excluded.filter(name => name !== 'pack.config.json')) f.write(name, 'synthetic fixture')
  const archive = path.join(f.dir, 'V2 & deployment with spaces.tar.gz')
  const { stdout } = await batch(`"${path.join(f.project, 'pack.bat')}" -batch -Output "${archive}"`, f.project)
  assert.match(stdout, /V2\.0\.0/)
  assert.ok(fs.existsSync(archive))
  const { stdout: list } = await exec('tar.exe', ['-tzf', archive])
  const entries = list.split(/\r?\n/).map(name => name.replaceAll('\\', '/'))
  const prefix = path.basename(f.project) + '/'
  for (const name of excluded) assert.ok(!entries.includes(prefix + name), `archive leaked ${name}`)
  for (const name of ['VERSION', '.env.example', 'docker-compose.yml', 'scripts/install.sh', 'ls-aegis-web/.env.production']) {
    assert.ok(entries.includes(prefix + name), `archive missing ${name}`)
  }
  const { stdout: info } = await exec('tar.exe', ['-xOf', archive, prefix + 'scripts/deploy-info.json'])
  const metadata = JSON.parse(info.replace(/^\uFEFF/, ''))
  assert.equal(metadata.Version, '2.0.0')
  assert.equal(metadata.BuildMode, 1)
  assert.equal(JSON.parse(fs.readFileSync(path.join(f.project, 'pack.config.json'), 'utf8').replace(/^\uFEFF/, '')).BuildMode, 1)
})

test('batch packaging rejects a missing or invalid VERSION without prompting', { skip: !windows }, async t => {
  const f = fixture(t)
  f.copy('pack.ps1')
  const args = ['-NoLogo', '-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass',
    '-File', path.join(f.project, 'pack.ps1'), '-Batch', '-NoBuild']
  for (const version of [null, '1.0; invalid']) {
    if (version !== null) f.write('VERSION', version)
    await assert.rejects(exec('powershell.exe', args, { timeout: 10000 }), e => e.code === 1 && /VERSION/.test(e.stdout))
  }
})

test('pack.bat batch mode propagates child failures without pausing', { skip: !windows }, async t => {
  const f = fixture(t)
  f.copy('pack.bat')
  f.write('pack.ps1', 'param([switch]$Batch)\nexit 7\n')
  await assert.rejects(batch(`"${path.join(f.project, 'pack.bat')}" -batch`, f.project), e => e.code === 7)
})

test('dev.cmd reads VERSION and propagates the development entry failure', { skip: !windows }, async t => {
  const f = fixture(t)
  f.copy('dev.cmd')
  f.write('VERSION', '2.0.0\n')
  f.write('docker/dev-all.cmd', '@echo off\r\nexit /b 9\r\n')
  await assert.rejects(batch(`"${path.join(f.project, 'dev.cmd')}"`, f.project), e => e.code === 9 && /V2\.0\.0/.test(e.stdout))
})

test('dev.cmd fails when its target entry is missing', { skip: !windows }, async t => {
  const f = fixture(t)
  f.copy('dev.cmd')
  f.write('VERSION', '2.0.0\n')
  await assert.rejects(batch(`"${path.join(f.project, 'dev.cmd')}"`, f.project), e => e.code === 1 && /dev-all.cmd/.test(e.stdout))
})
