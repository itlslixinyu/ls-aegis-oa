const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { randomUUID } = require('node:crypto')
const { execFileSync, execFile } = require('node:child_process')
const { promisify } = require('node:util')
const { gzipSync, gunzipSync } = require('node:zlib')
const exec = promisify(execFile)
const root = path.resolve(__dirname, '../..'), local = path.join(root, '.local')
const image = 'eclipse-temurin:21-jre-jammy'
let available = false
try { execFileSync('docker', ['image', 'inspect', image], { stdio: 'ignore', timeout: 5000 }); available = true } catch {}

for (const scenario of ['sql-failure', 'files-failure', 'success']) {
  test(`系统备份隔离执行：${scenario}`, { skip: !available && '需要Docker及已缓存Java21/Jammy测试镜像', timeout: 60000 }, async t => {
    fs.mkdirSync(local, { recursive: true })
    const dir = fs.mkdtempSync(path.join(local, 'r51-backup-'))
    const run = `R51-BACKUP-${randomUUID()}`, container = `itls-backup-${randomUUID()}`
    t.after(async () => {
      const { stdout } = await exec('docker', ['ps', '-aq', '--filter', `name=^/${container}$`])
      if (stdout.trim()) {
        const label = (await exec('docker', ['inspect', '--format', '{{index .Config.Labels "itls.quality.run"}}', container])).stdout.trim()
        assert.equal(label, run)
        await exec('docker', ['rm', '-f', container])
      }
      const resolved = fs.realpathSync(dir)
      assert.ok(resolved.startsWith(fs.realpathSync(local) + path.sep) && path.basename(resolved).startsWith('r51-backup-'))
      fs.rmSync(resolved, { recursive: true, force: true })
    })
    const write = (name, data) => {
      const target = path.join(dir, name); fs.mkdirSync(path.dirname(target), { recursive: true }); fs.writeFileSync(target, data)
    }
    const project = 'project & backup'
    write(`${project}/scripts/backup.sh`, fs.readFileSync(path.join(root, 'scripts/backup.sh')))
    write(`${project}/docker-compose.yml`, 'services: {}\n')
    write(`${project}/.env`, 'MYSQL_ROOT_PASSWORD=synthetic-backup-only\n')
    write(`${project}/docker/data/ls-aegis-server/data/snapshot.txt`, 'synthetic-file-only')
    const old = `${project}/backups/ls_aegis_20000101_000000.sql.gz`, foreign = `${project}/backups/foreign-archive.gz`
    write(old, gzipSync('previous-valid-backup')); write(foreign, gzipSync('foreign-backup'))
    write('bin/docker', `#!/bin/sh\nif [ "$1" = ps ]; then printf 'ls-aegis-mysql\\n'; exit 0; fi\nif [ "$1" = exec ]; then ${scenario === 'sql-failure' ? "echo 'synthetic dump failure' >&2; exit 19" : "printf 'SYNTHETIC SQL\\n'; exit 0"}; fi\nexit 99\n`)
    if (scenario === 'files-failure') write('bin/tar', '#!/bin/sh\nprintf broken > "$2"\nexit 17\n')
    write('probe.sh', `#!/bin/bash\nset -eu\ncd /fixture\nchmod +x bin/*\ntouch -d '10 days ago' '${old}' '${foreign}'\nset +e\nPATH="/fixture/bin:$PATH" bash '${project}/scripts/backup.sh' > stdout.txt 2> stderr.txt\nstatus=$?\nprintf '%s' "$status" > status.txt\nexit 0\n`)
    await exec('docker', ['run', '--rm', '--name', container, '--label', `itls.quality.run=${run}`, '--network', 'none',
      '--mount', `type=bind,source=${dir},target=/fixture`, '--entrypoint', 'bash', image, '/fixture/probe.sh'], { timeout: 45000 })
    const status = Number(fs.readFileSync(path.join(dir, 'status.txt'), 'utf8'))
    const output = fs.readFileSync(path.join(dir, 'stdout.txt'), 'utf8')
    assert.ok(!output.includes('synthetic-backup-only'))
    const backupDir = path.join(dir, project, 'backups'), entries = fs.readdirSync(backupDir)
    assert.ok(!entries.some(name => name.startsWith('.')), '未完成临时文件必须清理')
    assert.ok(fs.existsSync(path.join(dir, foreign)), '不能清理其他命名归档')
    if (scenario === 'success') {
      assert.equal(status, 0); assert.ok(!fs.existsSync(path.join(dir, old)))
      const sql = entries.find(name => /^ls_aegis_\d{8}_\d{6}\.sql\.gz$/.test(name))
      assert.equal(gunzipSync(fs.readFileSync(path.join(backupDir, sql))).toString(), 'SYNTHETIC SQL\n')
      assert.ok(entries.some(name => /^files_\d{8}_\d{6}\.tar\.gz$/.test(name)))
    } else {
      assert.notEqual(status, 0); assert.ok(fs.existsSync(path.join(dir, old)), '失败不得删除旧备份')
      assert.equal(gunzipSync(fs.readFileSync(path.join(dir, old))).toString(), 'previous-valid-backup')
      assert.ok(!entries.some(name => name.startsWith('files_')))
      if (scenario === 'sql-failure') assert.ok(!entries.some(name => /^ls_aegis_\d{8}_\d{6}\.sql\.gz$/.test(name) && name !== path.basename(old)))
    }
  })
}
