/** 两端H5真实构建与组件解析检查；审计资料仅进入.local。 */
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { gzipSync } from 'node:zlib'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const h5 = resolve(root, 'ls-aegis-h5')
const req = createRequire(resolve(h5, 'package.json'))
const { build } = await import(pathToFileURL(resolve(dirname(req.resolve('vite/package.json')), 'dist/node/index.js')))
process.chdir(h5)
const results = []
for (const mode of ['customer', 'engineer']) {
  await build({ root: h5, configFile: resolve(h5, 'vite.config.js'), mode,
    plugins: [{ name: 'itls-h5-component-audit', apply: 'build',
      generateBundle(_options, bundle) {
        const unresolved = []
        for (const id of this.getModuleIds()) {
          if (!id.replaceAll('\\', '/').includes('/src/')) continue
          const code = this.getModuleInfo(id)?.code || ''
          assert.ok(!/\btemplate\s*:\s*[`"']/.test(code), `${mode}运行时模板绕过组件预编译：${id}`)
          if (/resolveComponent\(["']van-/.test(code)) unresolved.push(id)
        }
        const chunks = Object.values(bundle).filter(item => item.type === 'chunk')
        const roleModules = [...new Set(chunks.flatMap(chunk => Object.entries(chunk.modules)
          .filter(([id, info]) => info.renderedLength > 0 && /[\\/]src[\\/]views[\\/](customer|engineer)[\\/]/.test(id))
          .map(([id]) => id.replaceAll('\\', '/').split('/src/').at(-1))))]
        const vantModules = [...new Set(chunks.flatMap(chunk => Object.entries(chunk.modules)
          .filter(([id, info]) => info.renderedLength > 0 && /[\\/]node_modules[\\/]vant[\\/]/.test(id))
          .map(([id]) => id.replaceAll('\\', '/').split('/node_modules/vant/').at(-1))))]
        const assets = Object.values(bundle).filter(item => /\.(js|css)$/.test(item.fileName))
          .map(item => Buffer.from(item.type === 'chunk' ? item.code : item.source))
        results.push({ mode, unresolvedVantComponents: unresolved,
          vantModules, roleModules, totalJsCssBytes: assets.reduce((n, b) => n + b.length, 0),
          gzipJsCssBytes: assets.reduce((n, b) => n + gzipSync(b).length, 0) })
        assert.deepEqual(unresolved, [], `${mode}仍依赖全局注册Vant组件`)
        if (process.argv.includes('--check')) {
          const otherRole = mode === 'customer' ? 'engineer' : 'customer'
          assert.ok(roleModules.some(id => id.startsWith(`views/${mode}/`)), `${mode}缺少本端业务页面`)
          assert.ok(!roleModules.some(id => id.startsWith(`views/${otherRole}/`)), `${mode}包含另一端页面`)
          assert.ok(!vantModules.includes('es/index.mjs'), `${mode}重新引入Vant整套注册入口`)
          const css = Object.values(bundle).filter(item => item.type === 'asset' && item.fileName.endsWith('.css'))
            .map(item => String(item.source)).join('\n')
          for (const selector of ['.van-toast', '.van-dialog', '.van-notify']) {
            assert.ok(css.includes(selector), `${mode}缺少函数式API样式${selector}`)
          }
        }
      },
    }],
  })
}
mkdirSync(resolve(root, '.local'), { recursive: true })
writeFileSync(resolve(root, '.local/h5-module-audit.json'), JSON.stringify({ at: new Date().toISOString(), results }, null, 2) + '\n')
console.log(JSON.stringify(results.map(({ vantModules, ...rest }) => ({ ...rest, vantModuleCount: vantModules.length }))))
