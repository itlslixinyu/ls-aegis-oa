/** 发布前核对产品版本，第三方依赖版本不参与替换或检查。 */
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '../..')
const read = name => readFileSync(resolve(root, name), 'utf8')
const json = name => JSON.parse(read(name))
const version = read('VERSION').trim()
assert.match(version, /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/)
const versions = {
  PC: json('ls-aegis-web/package.json').version,
  H5: json('ls-aegis-h5/package.json').version,
  H5Lock: json('ls-aegis-h5/package-lock.json').version,
  H5LockRoot: json('ls-aegis-h5/package-lock.json').packages[''].version,
  Maven: read('ls-aegis-server/pom.xml').match(/<revision>([^<]+)<\/revision>/)?.[1],
  backend: read('ls-aegis-server/ls-aegis-boot/src/main/resources/config/application.yml').match(/^  version:\s*(\S+)\s*$/m)?.[1],
  deployment: read('.env.example').match(/^ITLS_VERSION=(\S+)\s*$/m)?.[1],
  mobileGateway: read('docker-compose.mobile.yml').match(/image:\s+ls-aegis-h5:\$\{ITLS_VERSION:-([^}]+)\}/)?.[1],
}
const defaults = [...read('docker-compose.yml').matchAll(/image:\s+(ls-aegis-[\w-]+):\$\{ITLS_VERSION:-([^}]+)\}/g)]
assert.equal(defaults.length, 4, '生产Compose应有后端、PC、两端H5四个项目镜像版本')
for (const [, name, value] of defaults) versions[name] = value
for (const [name, value] of Object.entries(versions)) assert.equal(value, version, `${name}产品版本未同步`)
console.log(`PASS 产品版本 ${version}：${Object.keys(versions).length}处一致`)

// Platform and both Android apps share the same release version.
const apps = ['customer', 'engineer'].map(role => {
  const gradle = read(`android-demo/${role}-app/build.gradle.kts`)
  return { role, version: gradle.match(/versionName\s*=\s*"([^"]+)"/)?.[1],
    code: Number(gradle.match(/versionCode\s*=\s*(\d+)/)?.[1]) }
})
assert.match(apps[0].version, /^\d+\.\d+\.\d+$/)
assert.equal(apps[0].version, apps[1].version, '双端App版本必须一致')
assert.equal(apps[0].version, version, 'App与平台发布版本必须一致')
assert.ok(Number.isSafeInteger(apps[0].code) && apps[0].code > 0)
assert.equal(apps[0].code, apps[1].code, '双端App安装版本码必须一致')
console.log(`PASS 双端App与平台 ${version}，versionCode ${apps[0].code}`)
