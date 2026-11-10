/** 受控对照：恢复三引擎静态导入、CryptoJS 全量导入；源码与正式 dist 不变。 */
import { readFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { resolve, dirname } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const web = resolve(root, 'ls-aegis-web')
const require = createRequire(resolve(web, 'package.json'))
const pkgPath = require.resolve('vite/package.json')
const pkg = JSON.parse(await readFile(pkgPath, 'utf8'))
const { build } = await import(pathToFileURL(resolve(dirname(pkgPath), pkg.main)))
process.chdir(web) // 主配置通过 cwd 加载同一份非敏感 VITE 环境配置。
let transformed = false
let cryptoTransformed = false
await build({
  root: web,
  configFile: resolve(web, 'vite.config.ts'),
  build: { outDir: resolve(root, '.local/perf-eager-preview'), manifest: true },
  plugins: [{
    name: 'quality-eager-preview-control', enforce: 'pre',
    transform(code, id) {
      if (id.replaceAll('\\', '/').endsWith('/src/utils/encrypt.ts')) {
        for (const [symbol, module] of [['AES', 'aes'], ['ECB', 'mode-ecb'], ['Pkcs7', 'pad-pkcs7']]) {
          const statement = `import ${symbol} from 'crypto-js/${module}'`
          if (!code.includes(statement)) throw new Error('加密导入结构变化，请更新受控对照')
          code = code.replace(statement, '')
        }
        cryptoTransformed = true
        return `import CryptoJS from 'crypto-js'\nconst AES = CryptoJS.AES\nconst ECB = CryptoJS.mode.ECB\nconst Pkcs7 = CryptoJS.pad.Pkcs7\n${code}`
      }
      if (!id.replaceAll('\\', '/').endsWith('/src/components/FilePreview/index.vue')) return
      const begin = code.indexOf('const VueOfficePdf = createPreviewRenderer')
      const sessionStart = code.indexOf('const emit = defineEmits', begin)
      const end = sessionStart >= 0 ? sessionStart : code.indexOf('const visible =', begin)
      if (begin < 0 || end < 0) throw new Error('FilePreview 结构变化，请更新受控对照脚本')
      transformed = true
      return code.slice(0, begin) + `
import VueOfficePdf from '@vue-office/pdf'
import VueOfficeDocx from '@vue-office/docx'
import '@vue-office/docx/lib/index.css'
import VueOfficeExcel from '@vue-office/excel'
import '@vue-office/excel/lib/index.css'
\n` + code.slice(end)
    },
    buildEnd(error) {
      if (!error && (!transformed || !cryptoTransformed)) throw new Error('未应用完整受控对照，拒绝生成误导性基线')
    },
  }],
})
