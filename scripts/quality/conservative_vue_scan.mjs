/** 阶段2只读ESLint流水线；不调用fix，H5使用虚拟路径复用已有Vue/JS解析器。 */
import { createRequire } from 'node:module'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve, relative, isAbsolute } from 'node:path'
import { execFileSync } from 'node:child_process'

const root = resolve(import.meta.dirname, '../..')
const output = resolve(root, process.argv[2] || '')
const within = relative(resolve(root, '.local/conservative-cleanup'), output)
if (!within || within.startsWith('..') || isAbsolute(within)) throw Error('输出必须是本项目清理运行号目录')
const baseline = JSON.parse(readFileSync(resolve(output, 'baseline.json'), 'utf8'))
if (baseline.mode !== 'conservative' || baseline.startupBaseline.status !== 'PASS') throw Error('启动基线未通过，不能进入阶段2')
const web = resolve(root, 'ls-aegis-web'), req = createRequire(resolve(web, 'package.json'))
const { ESLint } = req('eslint')
const eslint = new ESLint({ cwd: web, fix: false, errorOnUnmatchedPattern: false })
const names = execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z'], { cwd: root, encoding: 'utf8' }).split('\0').filter(Boolean)
const targets = names.filter(p => /^ls-aegis-(web|h5)\//.test(p) && /\.(?:vue|ts|tsx|js|jsx|mjs|cjs)$/.test(p) && !/\.d\.ts$/.test(p))
const results = [], ruleConfigs = {}
for (const file of targets) {
  const isH5 = file.startsWith('ls-aegis-h5/')
  const filePath = isH5 ? resolve(web, 'src/__h5_scan__', file.slice('ls-aegis-h5/'.length)) : resolve(root, file)
  try {
    const config = await eslint.calculateConfigForFile(filePath)
    const configured = Object.fromEntries(Object.entries(config?.rules || {}).filter(([name]) => /unused|constant-condition|unreachable|dupe-else-if/.test(name)))
    ruleConfigs[isH5 ? 'h5VirtualParser' : 'pc'] ||= configured
    const parsed = await eslint.lintText(readFileSync(resolve(root, file), 'utf8'), { filePath, warnIgnored: true })
    results.push({ file, mode: isH5 ? 'H5_VIRTUAL_LEXICAL_ONLY' : 'PROJECT_ESLINT',
      messages: parsed.flatMap(r => r.messages.filter(m => m.fatal || /unused|constant-condition|unreachable|dupe-else-if/.test(m.ruleId || ''))),
      ignored: !config, totalDiagnostics: parsed.reduce((n, r) => n + r.messages.length, 0) })
  } catch (error) {
    results.push({ file, mode: 'TOOL_ERROR', error: String(error.message) })
  }
}
writeFileSync(resolve(output, 'vue-eslint-raw.json'), JSON.stringify({ tool: 'ESLint', version: req('eslint/package.json').version,
  fix: false, ruleConfigs, targets: targets.length, results }, null, 2))
console.log(JSON.stringify({ targets: targets.length, toolErrors: results.filter(r => r.error).length,
  ignored: results.filter(r => r.ignored).length, fatalFiles: results.filter(r => r.messages?.some(m => m.fatal)).length,
  relevantDiagnostics: results.reduce((n, r) => n + (r.messages?.length || 0), 0) }))
