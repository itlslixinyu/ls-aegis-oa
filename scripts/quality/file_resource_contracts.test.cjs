// 文件统计与通用外链下载契约回归；不访问网络、浏览器或业务库。
const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const root = path.resolve(__dirname, '../..')
const requireWeb = createRequire(path.join(root, 'ls-aegis-web/package.json'))
const ts = requireWeb('typescript')
const vue = requireWeb('vue')

function load(relative, { modules = {}, globals = {}, declarations, exports = '' } = {}) {
  let source = fs.readFileSync(path.join(root, 'ls-aegis-web/src', relative), 'utf8')
  if (relative.endsWith('.vue')) source = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  if (declarations) {
    const ast = ts.createSourceFile(relative, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
    source = ast.statements.filter((statement) =>
      (ts.isVariableStatement(statement) && statement.declarationList.declarations.some((item) => declarations.includes(item.name.getText(ast))))
      || (ts.isFunctionDeclaration(statement) && declarations.includes(statement.name?.text)))
      .map((statement) => statement.getText(ast)).join('\n')
  }
  const code = ts.transpileModule(source + exports, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText
  const result = { exports: {} }
  vm.runInNewContext(code, {
    ...vue, Blob, URL, console, ...globals, exports: result.exports,
    require(name) {
      if (name in modules) return modules[name]
      throw new Error(`未声明测试依赖：${name}`)
    },
  }, { filename: relative })
  return result.exports
}

function statistics(payload) {
  const { FileTypeList } = load('constant/file.ts')
  const { formatFileSize } = load('utils/index.ts', { declarations: ['formatFileSize'] })
  return load('views/system/file/main/FileAsideStatistics.vue', {
    declarations: ['totalData', 'chartData', 'loading', 'statisticNumber', 'getStatisticsData'],
    exports: '\nexport { totalData, chartData, getStatisticsData }',
    globals: { FileTypeList, formatFileSize, getFileStatistics: async () => ({ data: payload }) },
  })
}

test('Long 字符串统计在 Arco 中显示数量，不被解释为 HH:mm:ss', async () => {
  const h = statistics({ number: '12', size: '4096', data: [{ type: '2', number: '7', size: '2048' }] })
  await h.getStatisticsData()
  assert.equal(h.totalData.value.number, 12)
  assert.equal(h.totalData.value.size, 4)
  assert.equal(h.totalData.value.unit, 'KB')
  assert.equal(h.chartData.value[0].name, '图片')
  assert.equal(h.chartData.value[0].value, 7)
  assert.equal(h.chartData.value[0].size, '2.00 KB')
  const { Statistic } = requireWeb('@arco-design/web-vue')
  const fromVue = createRequire(requireWeb.resolve('vue'))
  const html = await fromVue('@vue/server-renderer').renderToString(vue.createSSRApp(Statistic, { value: h.totalData.value.number }))
  assert.match(html, /value-integer[^>]*>12</)
  assert.doesNotMatch(html, /00:00:00/)
})

test('字符串零、空缺和异常统计不会产生 NaN 或非法图表数值', async () => {
  for (const value of ['0', 0, undefined, null, 'invalid', -1, 'Infinity']) {
    const h = statistics({ number: value, size: value, data: [{ type: '3', number: value, size: value }] })
    await h.getStatisticsData()
    assert.equal(h.totalData.value.number, 0)
    assert.equal(h.totalData.value.size, 0)
    assert.equal(h.totalData.value.unit, 'Bytes')
    assert.equal(h.chartData.value[0].value, 0)
    assert.equal(h.chartData.value[0].size, '0 Bytes')
  }
})

function downloadHarness(response, { reject, clickFails = false } = {}) {
  const state = { requests: [], clicks: 0, created: 0, revoked: [], removed: 0, link: undefined }
  const assertSafeDownloadUrl = load('utils/safeUrl.ts', {
    declarations: ['assertSafeDownloadUrl'], globals: { window: { location: { origin: 'http://localhost:5173' } } },
  }).assertSafeDownloadUrl
  class DownloadURL extends URL {
    static createObjectURL() { state.created++; return 'blob:quality-file' }
    static revokeObjectURL(value) { state.revoked.push(value) }
  }
  const { downloadByUrl } = load('utils/downloadFile.ts', {
    modules: { '@/utils/safeUrl': { assertSafeDownloadUrl } },
    globals: {
      URL: DownloadURL,
      fetch: async (...args) => { state.requests.push(args); if (reject) throw reject; return response },
      document: {
        body: { appendChild(link) { state.link = link } },
        createElement: () => ({ style: {}, click() { if (clickFails) throw new Error('click failed'); state.clicks++ }, remove() { state.removed++ } }),
      },
    },
  })
  return { downloadByUrl, state }
}

test('同源和跨源 HTTP 401/403/404/500 均拒绝下载，不生成错误文件', async () => {
  for (const url of ['/files/test.pdf', 'https://files.example.test/test.pdf']) {
    for (const status of [401, 403, 404, 500]) {
      const h = downloadHarness(new Response('<html>failure</html>', { status, headers: { 'content-type': 'text/html' } }))
      await assert.rejects(h.downloadByUrl({ url }))
      assert.equal(h.state.requests.length, 1)
      assert.equal(h.state.clicks, 0)
      assert.equal(h.state.created, 0)
    }
  }
})

test('HTTP 200 的明确平台 R 错误拒绝下载，兼容无 msg 的鉴权错误', async () => {
  for (const payload of [
    { success: false, code: '401', msg: '登录过期' },
    { success: false, code: 403 },
    { success: false, code: '500', message: '文件读取失败' },
  ]) {
    const h = downloadHarness(new Response(JSON.stringify(payload), { headers: { 'content-type': 'application/json; charset=utf-8' } }))
    await assert.rejects(h.downloadByUrl({ url: '/file.pdf' }), /登录|权限|文件读取失败/)
    assert.equal(h.state.clicks, 0)
    assert.equal(h.state.created, 0)
  }
})

test('合法 JSON、带 success:false 的业务 JSON 和正常二进制均可下载', async () => {
  for (const [content, type] of [
    ['{"items":[1,2]}', 'application/json'],
    ['{"success":false,"reason":"这是业务文件"}', 'application/json'],
    ['{"success":true,"code":"0","data":[]}', 'application/json'],
    ['%PDF-test', 'application/pdf'],
    ['', 'application/octet-stream'],
  ]) {
    const h = downloadHarness(new Response(content, { headers: { 'content-type': type } }))
    assert.equal(await h.downloadByUrl({ url: 'https://files.example.test/文件%20一.json?signature=test#hash', target: '_self' }), true)
    assert.equal(h.state.clicks, 1)
    assert.equal(h.state.link.download, '文件 一.json')
    assert.equal(h.state.link.target, '_self')
    assert.equal(h.state.requests[0][1].credentials, 'same-origin')
    assert.equal(h.state.requests[0][1].headers, undefined)
    assert.equal(h.state.removed, 1)
    assert.equal(h.state.revoked[0], 'blob:quality-file')
  }
})

test('网络失败与非法协议不会触发下载，点击异常也会释放对象 URL', async () => {
  const offline = downloadHarness(undefined, { reject: new Error('offline') })
  await assert.rejects(offline.downloadByUrl({ url: '/file.pdf' }), /offline/)
  assert.equal(offline.state.created, 0)
  const unsafe = downloadHarness(undefined)
  await assert.rejects(unsafe.downloadByUrl({ url: 'javascript:alert(1)' }), /非法下载地址/)
  assert.equal(unsafe.state.requests.length, 0)
  const clickError = downloadHarness(new Response('file'), { clickFails: true })
  await assert.rejects(clickError.downloadByUrl({ url: '/file.pdf', fileName: '指定文件名.pdf' }), /click failed/)
  assert.equal(clickError.state.link.download, '指定文件名.pdf')
  assert.equal(clickError.state.removed, 1)
  assert.equal(clickError.state.revoked[0], 'blob:quality-file')
})
