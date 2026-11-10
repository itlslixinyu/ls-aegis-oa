/** 用真实 Rollup 模块图核验页面依赖，审计资料仅写 .local，不进入部署产物。 */
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const web = resolve(root, 'ls-aegis-web')
const webRequire = createRequire(resolve(web, 'package.json'))
const viteRoot = dirname(webRequire.resolve('vite/package.json'))
const { build } = await import(pathToFileURL(resolve(viteRoot, 'dist/node/index.js')).href)
let audit
process.chdir(web)
await build({
  root: web,
  configFile: resolve(web, 'vite.config.ts'),
  plugins: [{
    name: 'itls-quality-module-audit',
    apply: 'build',
    generateBundle(_options, bundle) {
      const chunks = Object.values(bundle).filter(item => item.type === 'chunk')
      const unresolvedArco = []
      for (const id of this.getModuleIds()) {
        if (!id.replaceAll('\\', '/').startsWith(web.replaceAll('\\', '/') + '/src/')) continue
        const code = this.getModuleInfo(id)?.code || ''
        for (const [, name] of code.matchAll(/resolveComponent\(["'](a-[\w-]+)["']\)/g)) {
          // 上传render回调仍显式保留全局Tooltip，其余组件必须由解析器处理。
          if (name !== 'a-tooltip') unresolvedArco.push({ module: id, component: name })
        }
      }
      const fullArcoInstallModules = chunks.flatMap(chunk => Object.entries(chunk.modules)
        .filter(([id, info]) => info.renderedLength > 0 && /\/arco-vue\.js$/.test(id.replaceAll('\\', '/')))
        .map(([id]) => id))
      const byFile = new Map(chunks.map(chunk => [chunk.fileName, chunk]))
      const fullUtilityRootModules = chunks.flatMap(chunk => Object.entries(chunk.modules)
        .filter(([id, info]) => info.renderedLength > 0 && /\/xe-utils\/index\.js(?:\?|$)/.test(id.replaceAll('\\', '/')))
        .map(([id]) => id))
      function inspect(chunk) {
        if (!chunk) throw new Error('缺少页面构建入口')
        const files = new Set()
        function visit(file) {
          if (files.has(file)) return
          files.add(file)
          for (const child of byFile.get(file)?.imports || []) visit(child)
        }
        visit(chunk.fileName)
        const modules = [...files].flatMap(file => Object.entries(byFile.get(file)?.modules || {})
          .filter(([, info]) => info.renderedLength > 0).map(([id]) => id.replaceAll('\\', '/')))
        return {
          files: [...files],
          cronModules: modules.filter(id => /\/node_modules\/(cron-parser|luxon)\//.test(id))
            .map(id => id.replace(web.replaceAll('\\', '/') + '/', '')),
          pdfModules: modules.filter(id => /\/node_modules\/pdfjs-dist\//.test(id)),
        }
      }
      audit = {
        at: new Date().toISOString(),
        unresolvedArco, fullArcoInstallModules, fullUtilityRootModules,
        home: inspect(chunks.find(chunk => chunk.isEntry && chunk.facadeModuleId?.endsWith('/index.html'))),
        schedule: inspect(chunks.find(chunk => chunk.facadeModuleId?.endsWith('/src/views/schedule/job/index.vue'))),
        sign: inspect(chunks.find(chunk => chunk.facadeModuleId?.endsWith('/src/views/sign/execute/index.vue'))),
        signPlacer: inspect(chunks.find(chunk => chunk.facadeModuleId?.endsWith('/src/views/sign/execute/components/SignPdfPlacer.vue'))),
      }
    },
  }],
})
await mkdir(resolve(root, '.local'), { recursive: true })
await writeFile(resolve(root, '.local/frontend-module-audit.json'), JSON.stringify(audit, null, 2) + '\n')
console.log(`真实静态模块闭包：首页 Cron/Luxon ${audit.home.cronModules.length}，调度页 ${audit.schedule.cronModules.length}`)
if (process.argv.includes('--check')) {
  if (audit.home.cronModules.length !== 0 || audit.schedule.cronModules.length === 0) {
    throw new Error('调度解析器加载边界回归：应在调度页可用且不进入首页静态闭包')
  }
  if (audit.unresolvedArco.length || audit.fullArcoInstallModules.length) {
    throw new Error('Arco按需加载回归：仍有未解析组件或整套安装入口；查看.local模块图')
  }
  if (audit.fullUtilityRootModules.length) {
    throw new Error('公共工具加载回归：生产产物重新引入xe-utils总入口')
  }
  if (audit.sign.pdfModules.length || !audit.signPlacer.pdfModules.length) {
    throw new Error('签章预览加载边界回归：PDF引擎应仅进入定位组件的异步闭包')
  }
}
