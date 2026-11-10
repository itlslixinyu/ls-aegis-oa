/** 基于 Vite manifest 统计静态依赖闭包，不把异步资源计入页面首次加载。 */
import { readFile, writeFile, mkdir } from 'node:fs/promises'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { gzipSync } from 'node:zlib'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const dist = resolve(process.argv.slice(2).find((arg) => !arg.startsWith('--')) || resolve(root, 'ls-aegis-web/dist'))
const manifest = JSON.parse(await readFile(resolve(dist, '.vite/manifest.json'), 'utf8'))
const selected = [
  'index.html',
  'src/views/dashboard/analysis/index.vue',
  'src/views/quote/form.vue',
  'src/views/sign/execute/index.vue',
  'src/views/system/file/index.vue',
  'src/components/FilePreview/index.vue',
]
const engines = Object.keys(manifest).filter((key) => /^src\/components\/FilePreview\/renderers\/\w+Renderer\.vue$/.test(key))
function closure(key, keys = new Set()) {
  if (keys.has(key)) return keys
  if (!manifest[key]) throw new Error(`manifest 缺失依赖: ${key}`)
  keys.add(key)
  for (const child of manifest[key].imports || []) closure(child, keys)
  return keys
}
const rows = []
for (const source of selected) {
  const key = manifest[source] ? source : Object.keys(manifest).find((key) => manifest[key].src === source)
  if (!key) {
    if (source.startsWith('src/components/')) continue // 公共组件可能被合并到共享 chunk。
    throw new Error(`缺少待验证的页面入口: ${source}`)
  }
  const keys = closure(key)
  // 登录后的视图还要挂载平台 Layout；它是路由动态关联，不能只数视图的静态 import。
  if (source.startsWith('src/views/')) closure('src/layout/index.vue', keys)
  const files = new Set()
  for (const item of keys) {
    const chunk = manifest[item]
    if (/\.(?:js|mjs|css)$/.test(chunk.file)) files.add(chunk.file)
    for (const css of chunk.css || []) files.add(css)
  }
  let bytes = 0, gzipBytes = 0
  for (const file of files) {
    const buffer = await readFile(resolve(dist, file))
    bytes += buffer.length
    gzipBytes += gzipSync(buffer, { level: 5 }).length
  }
  const eagerRendererEntries = engines.filter((engine) => keys.has(engine))
  rows.push({ source, files: files.size, bytes, gzipBytes, eagerRendererEntries })
}
const result = { at: new Date().toISOString(), dist, engines, rows,
  note: 'JS/CSS 静态依赖闭包，登录后视图包含平台 Layout；gzip level=5 为离线估计，不包含图片、字体、API、worker、CPU 或交互耗时。各行有共享资源，不可相加。静态基线的引擎直接合并进 chunk，不会出现独立 Renderer 入口。' }
await mkdir(resolve(root, '.local'), { recursive: true })
const output = resolve(root, '.local', process.argv.includes('--baseline') ? 'frontend-assets-baseline.json' : 'frontend-assets-after.json')
await writeFile(output, JSON.stringify(result, null, 2))
for (const row of rows) console.log(`${row.source}: ${(row.bytes / 1024).toFixed(1)} KiB / gzip ${(row.gzipBytes / 1024).toFixed(1)} KiB / ${row.files} files`)
console.log(`Office 独立入口: ${engines.length}；证据 ${output}`)
if (process.argv.includes('--check')) {
  if (engines.length !== 3 || rows.some((row) => row.eagerRendererEntries.length)) {
    throw new Error('页面仍静态依赖 Office 引擎，或未生成三类独立预览入口')
  }
}
