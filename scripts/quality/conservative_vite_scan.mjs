/** 只在忽略目录的源码副本构建模块图，避免插件改写原项目生成类型或dist。 */
import { createRequire } from 'node:module'
import { readFileSync, writeFileSync, mkdirSync, copyFileSync, symlinkSync } from 'node:fs'
import { resolve, relative, dirname, isAbsolute } from 'node:path'
import { pathToFileURL } from 'node:url'
import { execFileSync } from 'node:child_process'
import { createHash } from 'node:crypto'

const root = resolve(import.meta.dirname, '../..'), web = resolve(root, 'ls-aegis-web')
const output = resolve(root, process.argv[2] || '')
const within = relative(resolve(root, '.local/conservative-cleanup'), output)
if (!within || within.startsWith('..') || isAbsolute(within)) throw Error('输出必须是本项目清理运行号目录')
const baseline = JSON.parse(readFileSync(resolve(output, 'baseline.json'), 'utf8'))
if (baseline.startupBaseline.status !== 'PASS' || baseline.mode !== 'conservative') throw Error('基线未通过')
const names = execFileSync('git', ['ls-files', '-z', '--', 'ls-aegis-web'], { cwd: root, encoding: 'utf8' }).split('\0').filter(Boolean)
const excluded = names.filter(p => /(?:^|\/)\.env[^/]*$|\.(?:key|pem|p12|pfx|jks|keystore|crt|cer)$/i.test(p))
const files = names.filter(p => !excluded.includes(p))
const sha = p => createHash('sha256').update(readFileSync(resolve(root, p))).digest('hex')
const before = Object.fromEntries(files.map(p => [p, sha(p)]))
const stage = resolve(output, 'vite-input-' + Date.now())
mkdirSync(stage)
for (const p of files) {
  const target = resolve(stage, p.slice('ls-aegis-web/'.length))
  mkdirSync(dirname(target), { recursive: true }); copyFileSync(resolve(root, p), target)
}
symlinkSync(resolve(web, 'node_modules'), resolve(stage, 'node_modules'), 'junction')
const req = createRequire(resolve(web, 'package.json'))
const { build } = await import(pathToFileURL(resolve(dirname(req.resolve('vite/package.json')), 'dist/node/index.js')).href)
const mapped = id => id.replaceAll('\\', '/').replace(stage.replaceAll('\\', '/'), web.replaceAll('\\', '/'))
let graph
try {
  process.chdir(stage)
  await build({ root: stage, configFile: resolve(stage, 'vite.config.ts'), mode: 'production',
    build: { write: false, emptyOutDir: false }, plugins: [{ name: 'conservative-static-module-graph', generateBundle(_options, bundle) {
      graph = {
        mode: 'ISOLATED_PRODUCTION_DEFAULTS', envFilesRead: false, excluded, writeBundle: false,
        limitation: '未复制环境文件，不视为部署产物；未进入图不证明永久无用',
        allModules: [...this.getModuleIds()].map(id => ({ id: mapped(id), included: this.getModuleInfo(id)?.isIncluded })),
        renderedModules: [...new Set(Object.values(bundle).filter(x => x.type === 'chunk').flatMap(chunk => Object.entries(chunk.modules)
          .filter(([, info]) => info.renderedLength > 0).map(([id]) => mapped(id))))],
      }
    } }] })
} finally { process.chdir(root) }
for (const p of files) if (sha(p) !== before[p]) throw Error('原项目输入发生变化：' + p)
writeFileSync(resolve(output, 'vue-vite-graph.json'), JSON.stringify({ ...graph, originalSourceHashesMatched: true,
  sourceHashes: before, stagingDirectory: relative(root, stage) }, null, 2))
console.log(JSON.stringify({ modules: graph.allModules.length, rendered: graph.renderedModules.length,
  originalSourceHashesMatched: true, envFilesCopied: false, stagingDirectory: relative(root, stage) }))
