// 使用项目已安装的 TS/Vue 和 Node test，无需新增测试框架。
const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const root = path.resolve(__dirname, '../..')
const webRequire = createRequire(path.join(root, 'ls-aegis-web/package.json'))
const ts = webRequire('typescript')
const vue = webRequire('vue')
const fromVue = createRequire(webRequire.resolve('vue'))

function load(file, modules = {}, globals = {}, exports = '', declarations) {
  let source = fs.readFileSync(path.join(root, 'ls-aegis-web/src', file), 'utf8')
  if (file.endsWith('.vue')) source = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  if (declarations) {
    const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
    source = ast.statements.filter((statement) => ts.isVariableStatement(statement)
      && statement.declarationList.declarations.some((item) => declarations.includes(item.name.getText(ast))))
      .map((statement) => statement.getText(ast)).join('\n')
  }
  const code = ts.transpileModule(source + exports, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText
  const result = { exports: {} }
  vm.runInNewContext(code, {
    ...vue, console, ...globals, exports: result.exports,
    require: (name) => {
      if (name in modules) return modules[name]
      if (name === 'vue') return vue
      throw new Error(`测试尚未声明依赖: ${name}`)
    },
  }, { filename: file })
  return result.exports
}
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('会话过期只提示一次，保留业务地址，旧响应或旧弹窗不清除新会话', async () => {
  const { createSessionExpiryHandler } = load('utils/login-redirect.ts', {}, { URL })
  let token = 'old', route = { path: '/system/file', fullPath: '/system/file?name=50%25' }
  const prompts = [], replacements = []; let logouts = 0
  const handle = createSessionExpiryHandler({
    getToken: () => token, route: () => route,
    prompt: (message, onOk) => prompts.push({ message, onOk }),
    logout: async () => { logouts++; token = '' }, replace: async location => { replacements.push(location) },
  })
  handle('Bearer old', '过期'); handle('Bearer old', '另一个并发请求')
  assert.equal(prompts.length, 1)
  route = { path: '/login', fullPath: '/login?redirect=%2Fsystem%2Ffile' }
  await prompts[0].onOk()
  assert.equal(logouts, 1)
  assert.equal(replacements[0].path, '/login')
  assert.equal(replacements[0].query.redirect, '/system/file?name=50%25')
  token = 'new'
  handle('Bearer old', '旧响应晚到')
  assert.equal(prompts.length, 1)
  route = { path: '/quote/index', fullPath: '/quote/index' }
  handle('Bearer new', '新会话过期')
  token = 'newer'
  await prompts[1].onOk()
  assert.equal(logouts, 1, '旧提示不得退出更新的登录')
  assert.equal(token, 'newer')
  route = { path: '/login', fullPath: '/login?redirect=/' }
  handle('Bearer newer', '已在登录页')
  assert.equal(prompts.length, 2)
})

test('登录回跳只保留本地业务路径，消除嵌套登录且不重复解码百分号', () => {
  const { safeLoginRedirect } = load('utils/login-redirect.ts', {}, { URL })
  assert.equal(safeLoginRedirect('/quote/index?name=50%25'), '/quote/index?name=50%25')
  assert.equal(safeLoginRedirect('/login?redirect=%2Flogin%3Fredirect%3D%252Fsystem%252Ffile'), '/system/file')
  for (const value of [undefined, ['duplicate'], 'https://outside.invalid', '//outside.invalid', '/\\outside.invalid', '/login', '/login?redirect=%2F%2Foutside.invalid']) {
    assert.equal(safeLoginRedirect(value), '/')
  }
})

test('文件下载复用鉴权API，保留长ID和原名，不访问过期存储域名；失败不保存错误文件', async () => {
  const id = '908798053845041260'
  for (const failed of [false, true]) {
    const calls = [], saved = [], errors = []
    const source = new Blob(['synthetic bytes'], { type: 'text/plain' })
    const { downloadFile } = load('apis/system/common.ts', {
      '@/utils/http': { default: { download: async (url) => {
        calls.push(url)
        if (failed) throw new Error('无权访问该文件')
        return { status: 200, headers: {}, data: source }
      } } },
    })
    let link
    const { useDownload } = load('hooks/modules/useDownload.ts', {
      '@arco-design/web-vue': { Message: { error: (message) => errors.push(message) }, Notification: {} },
    }, {
      Blob, navigator: {},
      document: {
        createElement: () => (link = { style: {}, click: () => saved.push(link.download) }),
        body: { appendChild() {}, removeChild() {} },
      },
      window: { URL: { createObjectURL: () => 'blob:synthetic', revokeObjectURL() {} } },
    })
    const { onDownload } = load('views/system/file/main/FileMain/index.vue', {}, {
      useDownload, downloadFile,
    }, '\nexport { onDownload }', ['onDownload'])
    await onDownload({ id, originalName: 'ITLS-TEST-下载.txt', url: 'http://localhost:8080/api/file/obsolete.txt' })
    assert.deepEqual(calls, [`/system/common/file/${id}`])
    assert.deepEqual(saved, failed ? [] : ['ITLS-TEST-下载.txt'])
    assert.equal(errors.length, failed ? 1 : 0)
  }
})

test('鉴权blob响应区分平台R失败和合法JSON文件，不把文件内容当导出错误', async () => {
  const source = fs.readFileSync(path.join(root, 'ls-aegis-web/src/utils/http.ts'), 'utf8')
  const ast = ts.createSourceFile('http.ts', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  let callback
  const visit = (node) => {
    if (ts.isCallExpression(node) && node.expression.getText(ast) === 'http.interceptors.response.use') {
      callback = node.arguments[0].getText(ast)
    }
    ts.forEachChild(node, visit)
  }
  visit(ast)
  assert.ok(callback)
  const code = ts.transpileModule(`export const onResponse = ${callback}`, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText
  const result = { exports: {} }, errors = [], expiries = []
  vm.runInNewContext(code, { exports: result.exports, Blob, handleError: (message) => errors.push(message),
    handleSessionExpiry: (authorization, message) => expiries.push({ authorization, message }) })
  for (const content of ['{"name":"合成JSON文件"}', 'null', '{invalid JSON file', '{"success":false,"detail":"business data"}', '{"success":false,"code":{"stage":"draft"},"content":"business JSON file"}']) {
    const response = { request: { responseType: 'blob' }, data: new Blob([content], { type: 'application/json' }) }
    assert.equal(await result.exports.onResponse(response), response)
  }
  assert.equal(errors.length, 0)
  await assert.rejects(() => result.exports.onResponse({
    request: { responseType: 'blob' },
    data: new Blob(['{"success":false,"code":"403","msg":"无权访问该文件"}'], { type: 'application/json' }),
  }), /无权访问该文件/)
  assert.deepEqual(errors, ['无权访问该文件'])
  const { AxiosHeaders } = webRequire('axios')
  for (const name of ['Authorization', 'authorization']) {
    await assert.rejects(() => result.exports.onResponse({
      request: { responseType: 'json' }, data: { success: false, code: '401', msg: '会话过期' },
      config: { url: '/psi/workbench/summary', headers: new AxiosHeaders({ [name]: 'Bearer expired-test' }) },
    }), /会话过期/)
  }
  assert.deepEqual(expiries, [1, 2].map(() => ({ authorization: 'Bearer expired-test', message: '会话过期' })))
})

test('部门顶部新增显示上级必填项，编辑嵌套父部门不被误隐藏', () => {
  const isUpdate = vue.ref(false)
  const { columns } = load('views/system/dept/AddModal.vue', {}, {
    isUpdate, deptSelectTree: vue.ref([{ key: '1', title: '根', children: [{ key: '2', title: '子' }] }]),
    findTree: webRequire('xe-utils').findTree,
  }, '\nexport { columns }', ['columns'])
  const parent = columns.find((column) => column.field === 'parentId')
  assert.equal(parent.hide({ parentId: undefined }), false)
  assert.equal(parent.rules[0].required, true)
  isUpdate.value = true
  assert.equal(parent.hide({ parentId: '2' }), false)
  assert.equal(parent.hide({ parentId: '0' }), true)
  assert.equal(parent.hide({ parentId: 'deleted-parent' }), true)
})

test('按需预览包装层透传文件、样式、Excel 配置及成功/失败事件', async () => {
  const { parse, compileScript } = fromVue('@vue/compiler-sfc')
  const { renderToString } = fromVue('@vue/server-renderer')
  for (const kind of ['Pdf', 'Docx', 'Excel']) {
    const file = `components/FilePreview/renderers/${kind}Renderer.vue`
    const source = fs.readFileSync(path.join(root, 'ls-aegis-web/src', file), 'utf8')
    const { descriptor } = parse(source)
    const script = compileScript(descriptor, { id: `quality-${kind}`, inlineTemplate: true })
    const code = ts.transpileModule(script.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
    let attributes, rendered = 0, errors = 0
    const engine = vue.defineComponent({
      inheritAttrs: false, emits: ['rendered', 'error'],
      setup(_, { attrs, emit }) {
        attributes = attrs
        emit('rendered'); emit('error')
        return () => vue.h('div')
      },
    })
    const result = { exports: {} }
    vm.runInNewContext(code, { exports: result.exports, require: (name) => {
      if (name === 'vue') return vue
      if (name.endsWith('.css')) return {}
      if (name === `@vue-office/${kind.toLowerCase()}`) return { default: engine }
      throw new Error(`非目标预览器被加载: ${name}`)
    } })
    const options = { xls: true }
    await renderToString(vue.createSSRApp(result.exports.default, {
      src: 'quality-file', class: 'h-full', options,
      onRendered: () => { rendered++ }, onError: () => { errors++ },
    }))
    assert.equal(attributes.src, 'quality-file')
    assert.equal(attributes.class, 'h-full')
    assert.equal(attributes.options, options)
    assert.equal(rendered, 1)
    assert.equal(errors, 1)
  }
})

function tableHarness() {
  const pagination = { current: 1, pageSize: 20, total: 0 }
  const { useTable } = load('hooks/modules/useTable.ts', {
    '@arco-design/web-vue': { Message: {}, Modal: {} },
    '@/hooks': {
      usePagination: () => ({ pagination, setTotal: (n) => { pagination.total = n } }),
      useBreakpoint: () => ({ breakpoint: vue.ref('lg') }),
    },
    '@/utils/listDiff': { patchListByKey: (target, next) => { target.splice(0, target.length, ...next); return true } },
  })
  const calls = []
  const table = useTable(() => { const call = deferred(); calls.push(call); return call.promise }, { immediate: false })
  return { table, calls, pagination }
}

test('快速翻页：较慢的旧响应不能覆盖最新页和总数', async () => {
  const { table, calls, pagination } = tableHarness()
  const old = table.getTableData()
  const latest = table.getTableData()
  calls[1].resolve({ data: { list: [{ id: '908733364796653611' }], total: 100 } })
  await latest
  calls[0].resolve({ data: { list: [{ id: 'old' }], total: 1 } })
  await old
  assert.equal(table.tableData.value[0].id, '908733364796653611')
  assert.equal(pagination.total, 100)
  assert.equal(table.loading.value, false)
  table.select(['908733364796653611'])
  assert.equal(table.selectedKeys.value[0], '908733364796653611')
})

test('旧请求失败不清空新结果、不提前结束最新请求的 loading', async () => {
  const { table, calls } = tableHarness()
  const old = table.getTableData()
  const latest = table.getTableData()
  calls[0].reject(new Error('old network failure'))
  await old
  assert.equal(table.loading.value, true)
  calls[1].resolve({ data: [{ id: 'new' }] })
  await latest
  assert.equal(table.tableData.value[0].id, 'new')
  assert.equal(table.loading.value, false)
})

test('无库存的新建与服务报价回显不弹商品录入，用户主动选材料才提示', async () => {
  let prompts = 0
  const dataId = vue.ref(), materialLines = vue.ref([]), serviceLines = vue.ref([])
  const form = {}, goodsPrompted = vue.ref(false)
  const { loadDetail, onGoodsPopup } = load('views/quote/form.vue', {}, {
    dataId, materialLines, serviceLines, form, goodsPrompted,
    syncingValidUntil: vue.ref(false), pendingGoodsLineUid: vue.ref(), locked: vue.ref(false),
    goodsLoading: vue.ref(false), goodsOptions: vue.ref([]),
    GoodsAddModalRef: vue.ref({ onAdd: () => prompts++ }),
    appStore: { getCompany: () => '测试单位' }, dayjs: webRequire('dayjs'),
    DATETIME_FORMAT: 'YYYY-MM-DD HH:mm:ss', defaultTerms: '测试条款',
    calcValidUntil: () => '2026-11-13 09:00:00', ensureDefaultRows: () => {},
    listGoodsOptions: async () => ({ data: [] }), toBizId: (id) => id == null ? undefined : String(id),
    getQuote: async () => ({ data: { id: '20', status: 'DRAFT', taxRate: 13, totalAmount: 113,
      details: [{ id: '1', lineType: 'SERVICE', itemName: '服务', quantity: 2, price: 50, amount: 100 }] } }),
  }, '\nexports.loadDetail=loadDetail; exports.onGoodsPopup=onGoodsPopup;',
  ['detailRequestVersion', 'detailLoading', 'loadedQuoteId', 'quoteReady', 'resetQuoteForm', 'loadDetail', 'loadGoodsOptions', 'toGoodsOpts', 'promptCreateGoods', 'onGoodsPopup'])
  await loadDetail()
  assert.equal(prompts, 0, '进入空库存新建页不能强制创建商品')
  dataId.value = '20'
  await loadDetail()
  assert.equal(prompts, 0, '保存后回显服务报价不能被商品弹窗遮挡')
  assert.equal(form.totalAmount, 113)
  assert.equal(serviceLines.value[0].amount, 100)
  onGoodsPopup(true, { uid: 'explicit-material' })
  assert.equal(prompts, 1)
  onGoodsPopup(true)
  assert.equal(prompts, 1, '主动提示仍受一次性保护')
})

test('报价商品索引保留字符串精度/首个选项，快速切换商品不被旧详情覆盖', async () => {
  const firstId = '908733364796653611', secondId = '908733364796653612'
  const goodsOptions = vue.ref([
    { value: firstId, label: '商品一' }, { value: firstId, label: '重复一' },
    { value: secondId, label: '商品二' },
  ])
  const calls = new Map()
  const { goodsById, applyGoodsToLine } = load('views/quote/form.vue', {}, {
    goodsOptions, toBizId: (id) => id, clearMaterialGoodsFields: () => {}, tipIfNoStock: () => {},
    getGoods: (id) => { const call = deferred(); calls.set(id, call); return call.promise },
    recalc: (row) => { row.amount = row.price * row.quantity },
  }, '\nexport { goodsById, applyGoodsToLine }', ['goodsById', 'goodsRequestVersions', 'applyGoodsToLine'])
  assert.equal(goodsById.value.get(firstId).label, '商品一')
  assert.equal(goodsById.value.get(secondId).label, '商品二')
  const record = { quantity: 2 }
  const old = applyGoodsToLine(record, firstId)
  const latest = applyGoodsToLine(record, secondId)
  calls.get(secondId).resolve({ data: { goodsName: '最新商品二', salePrice: 150, unit: '台' } })
  await latest
  calls.get(firstId).resolve({ data: { goodsName: '旧商品一', salePrice: 999 } })
  await old
  assert.equal(record.goodsId, secondId)
  assert.equal(record.itemName, '最新商品二')
  assert.equal(record.price, 150)
  assert.equal(record.amount, 300)
})

test('报价 A→B→A：最旧 A 不覆盖最新 A，不同行请求相互独立', async () => {
  const calls = []
  const { applyGoodsToLine } = load('views/quote/form.vue', {}, {
    goodsOptions: vue.ref([]), goodsById: { value: new Map() },
    toBizId: (id) => id, clearMaterialGoodsFields: () => {}, tipIfNoStock: () => {},
    getGoods: () => { const call = deferred(); calls.push(call); return call.promise },
    recalc: (row) => { row.amount = row.price * row.quantity },
  }, '\nexport { applyGoodsToLine }', ['goodsRequestVersions', 'applyGoodsToLine'])
  const row = { quantity: 2 }, other = { quantity: 3 }
  const first = applyGoodsToLine(row, 'A')
  const second = applyGoodsToLine(row, 'B')
  const latest = applyGoodsToLine(row, 'A')
  const independent = applyGoodsToLine(other, 'A')
  calls[2].resolve({ data: { goodsName: '最新A', salePrice: 150 } })
  await latest
  calls[0].resolve({ data: { goodsName: '旧A', salePrice: 999 } })
  calls[1].resolve({ data: { goodsName: '旧B', salePrice: 888 } })
  calls[3].resolve({ data: { goodsName: '另一行', salePrice: 20 } })
  await Promise.all([first, second, independent])
  assert.equal(row.itemName, '最新A')
  assert.equal(row.amount, 300)
  assert.equal(other.amount, 60)
})

function dashboardHarness({ statistics = true, psiFails = false, profitFails = false } = {}) {
  const requests = []
  const state = { trendDays: vue.ref(7), orderTrend: vue.ref([{ day: 'last-good' }]),
    profitTrend: vue.ref([{ month: 'last-good' }]), dashboardOk: vue.ref(true) }
  let psiCalls = 0
  const model = load('views/dashboard/analysis/index.vue', {}, {
    ...state, pageActive: true, visibility: vue.ref('visible'), refreshing: false,
    loading: vue.ref(false), updatedAt: vue.ref(''), systemOnline: vue.ref(true),
    lastCpu: vue.ref(0), lastMem: vue.ref(0), navigator: { onLine: true },
    eomsOverview: vue.ref({}), psiSummary: vue.ref({}), signStats: vue.ref({}),
    signStatusList: vue.ref([]), quoteSummary: vue.ref({}),
    has: { hasPermOr: (permissions) => statistics && permissions.includes('eoms:statistics:list') },
    getOrderTrend: ({ days }) => { const r = deferred(); requests.push({ ...r, days }); return r.promise },
    getProfitTrend: () => profitFails ? Promise.reject(new Error('profit unavailable')) : Promise.resolve({ data: [] }),
    getPsiWorkbenchSummary: () => { psiCalls++; return psiFails ? Promise.reject(new Error('PSI unavailable')) : Promise.resolve({ data: null }) },
    settledData: (result) => result.status === 'fulfilled' ? result.value.data : undefined,
    mapOrderTrend: (data) => data || [], mapProfitTrend: (data) => data || [],
    patchListByKey: (target, next) => { target.splice(0, target.length, ...next); return true },
    assignIfChanged: (target, next) => { target.value = next; return true },
    num: (v) => Number(v || 0), dayjs: () => ({ format: () => 'now' }), applyRunStatus: () => {},
  }, '\nexport { loadOrderTrend, refreshAll }', ['orderTrendRequestVersion', 'loadOrderTrend', 'refreshAll'])
  return { ...model, ...state, requests, get psiCalls() { return psiCalls } }
}

test('趋势 7→30：旧筛选的晚到响应不能覆盖当前序列', async () => {
  const h = dashboardHarness()
  const first = h.loadOrderTrend()
  h.trendDays.value = 30
  const latest = h.loadOrderTrend()
  h.requests[1].resolve({ data: [{ day: 'current-30' }] })
  await latest
  h.requests[0].resolve({ data: [{ day: 'old-7' }] })
  await first
  assert.equal(h.requests[0].days, 7)
  assert.equal(h.requests[1].days, 30)
  assert.equal(h.orderTrend.value[0].day, 'current-30')
})

test('轮询与手动趋势切换共享版本，较慢的轮询不覆盖当前筛选', async () => {
  const h = dashboardHarness()
  const poll = h.refreshAll({ silent: true })
  h.trendDays.value = 30
  const latest = h.loadOrderTrend()
  h.requests[1].resolve({ data: [{ day: 'current-30' }] })
  await latest
  h.requests[0].resolve({ data: [{ day: 'poll-7' }] })
  await poll
  assert.equal(h.orderTrend.value[0].day, 'current-30')
})

test('趋势接口失败保留最近有效序列，其他域成功仍可保持部分可用', async () => {
  const h = dashboardHarness({ profitFails: true })
  const poll = h.refreshAll({ silent: true })
  h.requests[0].reject(new Error('trend unavailable'))
  await poll
  assert.equal(h.orderTrend.value[0].day, 'last-good')
  assert.equal(h.profitTrend.value[0].month, 'last-good')
  assert.equal(h.dashboardOk.value, true)
})

test('只有占位任务成功、实际授权请求全失败时不能报告健康', async () => {
  const h = dashboardHarness({ statistics: false, psiFails: true })
  await h.refreshAll()
  assert.equal(h.psiCalls, 1)
  assert.equal(h.requests.length, 0)
  assert.equal(h.dashboardOk.value, false)
})

test('预览 chunk 加载失败立即停止 loading、清理定时器并提示重试', async () => {
  const messages = []
  let cleared = 0, globalErrors = 0
  const model = load('components/FilePreview/index.vue', {}, {
    Message: { error: (message) => messages.push(message) }, clearTimeout: () => { cleared++ },
  }, '\nexport { createPreviewRenderer, loading }; export function arm() { loading.value = true; loadingTimer = 1 }',
  ['createPreviewRenderer', 'loading', 'loadingTimer', 'clearLoadingTimer', 'stopLoading', 'errorHandler'])
  model.arm()
  const component = model.createPreviewRenderer(() => Promise.reject(new Error('chunk unavailable')))
  const app = vue.createSSRApp(component)
  app.config.errorHandler = () => { globalErrors++ }
  await fromVue('@vue/server-renderer').renderToString(app)
  assert.equal(model.loading.value, false)
  assert.equal(cleared, 1)
  assert.equal(messages.length, 1)
  assert.ok(messages[0].includes('重试'))
  assert.equal(globalErrors, 1)
})

test('统一工作台：报修/运维共用一份概览，PSI 失败不丢运维结果，无权限不请求', async () => {
  let psiReply = deferred(), eomsReply = deferred(), psiCalls = 0, eomsCalls = 0, permitted = true
  const events = []
  const model = load('views/dashboard/workplace/components/PsiBusinessPanel.vue', {}, {
    getPsiWorkbenchSummary: () => { psiCalls++; return psiReply.promise },
    getOverview: () => { eomsCalls++; return eomsReply.promise },
    emit: (...args) => events.push(args), has: { hasPermOr: () => permitted },
  }, '\nexport { load, loading, summary, eomsOverview }', ['load', 'loadPsi', 'loadEoms', 'loading', 'summary', 'eomsOverview'])
  const first = model.load()
  await model.load() // 重复刷新不重复发请求。
  assert.equal(psiCalls, 1)
  assert.equal(eomsCalls, 1)
  eomsReply.resolve({ data: { waitApply: 3 } })
  for (let i = 0; i < 10 && !model.eomsOverview.value; i++) await Promise.resolve()
  assert.equal(model.eomsOverview.value.waitApply, 3)
  assert.equal(model.loading.value, true)
  psiReply.reject(new Error('PSI unavailable'))
  await first
  assert.equal(model.eomsOverview.value.waitApply, 3)
  assert.equal(model.loading.value, false)
  permitted = false
  psiReply = deferred()
  psiReply.resolve({ data: {} })
  await model.load()
  assert.equal(eomsCalls, 1)
  assert.equal(model.eomsOverview.value, null)
  assert.ok(events.some(([name, value]) => name === 'eomsLoaded' && value === null))
})

test('版本检查：并发合并、60 秒节流、缺少响应头与失败不阻断导航', async () => {
  let now = 0, calls = 0, changes = 0
  let response = deferred()
  const { createVersionChecker } = load('utils/version-check.ts', {}, { Date: { now: () => now } })
  const check = createVersionChecker(() => { calls++; return response.promise }, () => { changes++ })
  const first = check()
  assert.equal(check(), first)
  response.resolve('v1')
  await first
  for (let i = 0; i < 20; i++) await check()
  assert.equal(calls, 1)
  now = 60_001
  response = deferred()
  const second = check()
  response.resolve(null)
  await second
  assert.equal(changes, 0)
  now += 60_001
  response = deferred()
  const third = check()
  response.reject(new Error('offline'))
  await third
  now += 60_001
  response = deferred()
  const fourth = check()
  response.resolve('v2')
  await fourth
  assert.equal(changes, 1)
})

function encryptionHarness(getGmPublicKey = async () => ({ data: { enable: false } })) {
  const modules = { '@/apis/auth': { getGmPublicKey }, 'sm-crypto': webRequire('sm-crypto') }
  for (const name of ['enc-base64', 'enc-utf8', 'aes', 'mode-ecb', 'pad-pkcs7']) {
    modules[`crypto-js/${name}`] = { default: webRequire(`crypto-js/${name}`) }
  }
  return load('utils/encrypt.ts', modules, { TextEncoder, btoa })
}

test('按需密码算法导入保持原 AES/ECB/PKCS7 字节结果、SM3 向量与 Base64 行为', () => {
  const original = webRequire('crypto-js')
  const model = encryptionHarness()
  for (const key of ['1234567890abcdef', '1234567890abcdef1234567890abcdef']) {
    for (const text of ['', '1234567890abcdef', '[{"x":10,"y":20}]', '验证码坐标与中文']) {
      const expected = original.AES.encrypt(original.enc.Utf8.parse(text), original.enc.Utf8.parse(key), {
        mode: original.mode.ECB, padding: original.pad.Pkcs7,
      }).toString()
      assert.equal(model.encryptByAes(text, key), expected)
      assert.equal(model.decodeByBase64(model.encodeByBase64(text)), text)
    }
  }
  assert.equal(model.encryptBySm3('abc'), '66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0')
  const hex = webRequire('sm-crypto').sm4.encrypt('测试', Buffer.from('1234567890abcdef').toString('hex'))
  assert.equal(model.encryptBySm4Ecb('测试', '1234567890abcdef'), Buffer.from(hex, 'hex').toString('base64'))
  assert.throws(() => model.encryptByAes('test', ''))
})

test('公钥并发请求合并，失败可重试，SM2 登录传输格式仍可解密', async () => {
  let reply = deferred(), calls = 0
  const model = encryptionHarness(() => { calls++; return reply.promise })
  const first = model.loadGmPublicKey()
  assert.equal(model.loadGmPublicKey(), first)
  reply.reject(new Error('offline'))
  assert.equal(await first, false)
  assert.equal(calls, 1)
  const { sm2 } = webRequire('sm-crypto')
  const pair = sm2.generateKeyPairHex()
  reply = deferred()
  const retry = model.loadGmPublicKey()
  reply.resolve({ data: { enable: true, publicKeyHex: pair.publicKey } })
  assert.equal(await retry, true)
  assert.equal(calls, 2)
  const encrypted = await model.encryptTransport('quality-password-测试')
  assert.equal(encrypted.slice(0, 2), '04')
  assert.equal(sm2.doDecrypt(encrypted.slice(2), pair.privateKey, 1), 'quality-password-测试')
  assert.equal(calls, 2)
})

function weatherHarness(provider = 'mock') {
  const hooks = { mounted: [], activated: [], deactivated: [], beforeUnmount: [] }
  const watchers = []
  const visibility = vue.ref('visible')
  const timers = new Map()
  let sequence = 0, calls = 0, geolocationCalls = 0
  let reply = deferred()
  const { useDashboardWeather } = load('views/dashboard/composables/useDashboardWeather.ts', {
    'lodash-es': { isEqual: (a, b) => JSON.stringify(a) === JSON.stringify(b) },
    '@vueuse/core': { useDocumentVisibility: () => visibility },
    '@/apis/system': {
      listWeatherOptionDict: async () => ({ data: [{ label: 'WEATHER_PROVIDER', value: provider }] }),
      getWeatherNow: () => { calls++; return reply.promise },
    },
    '../utils/weather': {
      defaultWeatherRuntimeConfig: () => ({ enabled: true, city: '测试', cityMode: 'auto', refreshInterval: 600, provider }),
      tryBrowserGeolocation: async () => { geolocationCalls++; return { lat: 0, lon: 0 } },
      mapWeatherNowToDashboard: (data) => data,
      createMockWeather: () => ({ city: '测试' }),
      listActiveWeatherAlerts: () => [],
    },
  }, {
    ...Object.fromEntries(Object.entries(hooks).map(([name, list]) => [`on${name[0].toUpperCase() + name.slice(1)}`, (fn) => list.push(fn)])),
    watch: (source, fn) => watchers.push(fn),
    setInterval: (fn) => { timers.set(++sequence, fn); return sequence },
    clearInterval: (id) => timers.delete(id),
  })
  return {
    model: useDashboardWeather({ autoLoad: false }), hooks, timers,
    get calls() { return calls }, get geolocationCalls() { return geolocationCalls },
    resolve: () => reply.resolve({ data: { city: '测试', provider } }),
    next: () => { reply = deferred() },
    visibility: (state) => { visibility.value = state; watchers.forEach((fn) => fn(state)) },
  }
}

test('模拟天气不等待定位；同时刷新只发一个请求', async () => {
  const h = weatherHarness()
  const config = h.model.loadWeatherConfig()
  // VM 与宿主的 Promise 需要跨上下文微任务交接。
  for (let i = 0; i < 10 && !h.calls; i++) await Promise.resolve()
  const first = h.model.refreshWeather()
  const second = h.model.refreshWeather()
  assert.equal(first, second)
  assert.equal(h.calls, 1)
  assert.equal(h.geolocationCalls, 0)
  h.resolve()
  await Promise.all([config, first, second])
  assert.equal(h.timers.size, 1)
})

test('天气轮询：缓存页离开/隐藏停止，恢复仅一个定时器，卸载后不再请求', async () => {
  const h = weatherHarness()
  h.resolve()
  await h.model.loadWeatherConfig()
  h.hooks.deactivated.forEach((fn) => fn())
  assert.equal(h.timers.size, 0)
  const before = h.calls
  await h.model.refreshWeather()
  assert.equal(h.calls, before)
  h.hooks.activated.forEach((fn) => fn())
  h.hooks.activated.forEach((fn) => fn())
  assert.equal(h.timers.size, 1)
  h.visibility('hidden')
  assert.equal(h.timers.size, 0)
  h.next()
  h.visibility('visible')
  assert.equal(h.timers.size, 1)
  h.resolve()
  await h.model.refreshWeather()
  h.hooks.beforeUnmount.forEach((fn) => fn())
  assert.equal(h.timers.size, 0)
  const last = h.calls
  await h.model.refreshWeather()
  assert.equal(h.calls, last)
})

// 最小 DOM 模型仅用于测“扫描范围/调度”，不替代浏览器的控件与无障碍验收。
function domHarness() {
  let queries = 0, work = 0, observer
  const frames = new Map()
  let sequence = 0
  class Element {
    constructor(parent, form = false) {
      this.parentElement = parent; this.children = []; this.form = form; this.isConnected = true
      parent?.children.push(this)
    }
    matches() { return false }
    closest(selector) { return this.form && selector.includes('form-item') ? this : this.parentElement?.closest(selector) || null }
    contains(node) { return node === this || this.children.some((child) => child.contains(node)) }
    querySelectorAll() {
      queries++
      const visit = (node) => { work++; node.children.forEach(visit) }
      this.children.forEach(visit)
      return []
    }
  }
  const body = new Element()
  const clock = new Element(body)
  for (let i = 0; i < 5000; i++) new Element(body)
  const form = new Element(body, true)
  new Element(form)
  const document = { body, querySelectorAll: (...args) => body.querySelectorAll(...args) }
  const { watchFormAccessibility } = load('utils/form-field-identity.ts', {}, {
    Element, HTMLElement: Element, document,
    MutationObserver: class {
      constructor(fn) { this.callback = fn; observer = this }
      observe(target, options) { this.options = options }
      disconnect() {}
      takeRecords() { return [] }
    },
    requestAnimationFrame: (fn) => { frames.set(++sequence, fn); return sequence },
    cancelAnimationFrame: (id) => frames.delete(id),
  })
  const stop = watchFormAccessibility()
  const flush = () => { const callbacks = [...frames.values()]; frames.clear(); callbacks.forEach((fn) => fn()) }
  flush()
  const initialWork = work
  queries = 0; work = 0
  return { stop, flush, body, clock, form, Element, observer,
    get frames() { return frames.size }, get work() { return work }, get initialWork() { return initialWork } }
}

test('全站表单修复：时钟文本变化不扫描，新增表单仅扫描该区域，批量变化合并', () => {
  const h = domHarness()
  for (let i = 0; i < 60; i++) {
    h.observer.callback([{ type: 'childList', target: h.clock, addedNodes: [{}] }])
    h.flush()
  }
  assert.equal(h.work, 0)
  assert.equal(h.observer.options.attributeFilter.includes('class'), false)
  const input = new h.Element(h.form)
  for (let i = 0; i < 10; i++) h.observer.callback([{ type: 'childList', target: h.form, addedNodes: [input] }])
  assert.equal(h.frames, 1)
  h.flush()
  assert.ok(h.work < 20)
  assert.ok(h.initialWork > 15_000)
  h.observer.callback([{ type: 'childList', target: h.form, addedNodes: [input] }])
  h.stop()
  assert.equal(h.frames, 0)
})

test('局部表单修复包含根控件，跨弹窗新字段不会生成重复 id，既有标识保留', () => {
  const inputs = []
  class Element {}
  class Input extends Element {
    constructor() { super(); this.attributes = { type: 'text' }; this.labels = []; this.tagName = 'INPUT'; inputs.push(this) }
    get type() { return this.attributes.type }
    get id() { return this.attributes.id || '' }
    set id(value) { this.attributes.id = value }
    get name() { return this.attributes.name || '' }
    set name(value) { this.attributes.name = value }
    getAttribute(key) { return this.attributes[key] ?? null }
    setAttribute(key, value) { this.attributes[key] = value }
    hasAttribute(key) { return key in this.attributes }
    querySelectorAll() { return [] }
    matches(selector) { return selector.startsWith('input') }
  }
  const existing = new Input()
  existing.id = 'aegis-text-1'
  const { patchFormAccessibility } = load('utils/form-field-identity.ts', {}, {
    Element, HTMLElement: Element, HTMLInputElement: Input,
    HTMLSelectElement: class extends Element {}, HTMLTextAreaElement: class extends Element {}, HTMLButtonElement: class extends Element {},
    document: { getElementById: (id) => inputs.find((input) => input.id === id) || null },
  })
  const first = new Input(), second = new Input()
  patchFormAccessibility(first)
  patchFormAccessibility(second)
  assert.equal(first.id, first.name)
  assert.equal(second.id, second.name)
  assert.notEqual(first.id, second.id)
  assert.notEqual(first.id, existing.id)
  assert.equal(existing.id, 'aegis-text-1')
})
