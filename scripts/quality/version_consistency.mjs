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
