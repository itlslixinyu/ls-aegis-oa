const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const root = path.resolve(__dirname, '../..')
const req = createRequire(path.join(root, 'ls-aegis-web/package.json'))
const ts = req('typescript'), vue = req('vue'), arco = req('@arco-design/web-vue')
const fromVue = createRequire(req.resolve('vue'))
const { renderToString } = fromVue('@vue/server-renderer')
function load(name) {
  const source = fs.readFileSync(path.join(root, 'ls-aegis-web/src', name), 'utf8')
  const code = ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true,
  } }).outputText
  const exports = {}
  vm.runInNewContext(code, { exports, require: name => {
    if (name === '@arco-design/web-vue') return arco
    if (name === '@arco-design/web-vue/es/icon') return req('@arco-design/web-vue/lib/icon')
    throw new Error(`未声明依赖：${name}`)
  } })
  return exports
}
const { resolveFormComponent } = load('components/GiForm/src/component-map.ts')
test('动态表单覆盖全部公开控件类型，不依赖全局a-*字符串解析', () => {
  const union = fs.readFileSync(path.join(root, 'ls-aegis-web/src/components/GiForm/src/type.ts'), 'utf8')
    .split('export type ColumnItemType =')[1].split('export type ComponentProps')[0]
  for (const [, type] of union.matchAll(/'([^']+)'/g)) {
    if (type === 'group-title') continue
    const component = resolveFormComponent(type)
    assert.ok(component && ['object', 'function'].includes(typeof component), type)
  }
  assert.equal(resolveFormComponent('group-title'), undefined)
  assert.equal(resolveFormComponent(undefined), undefined)
})
test('没有Arco全局组件的应用仍可渲染动态输入和数字输入', async () => {
  for (const [type, value, marker] of [['input', 'fixture', 'arco-input'], ['input-number', 12, 'arco-input-number']]) {
    const app = vue.createSSRApp({ render: () => vue.h(resolveFormComponent(type), { modelValue: value }) })
    const html = await renderToString(app)
    assert.ok(html.includes(marker))
    assert.ok(html.includes(`value="${value}"`))
    assert.ok(!html.includes(`<a-${type}`))
  }
})
test('函数式弹窗、上传默认配置及任意菜单图标继续可用', () => {
  const app = vue.createSSRApp({ render: () => null })
  load('utils/setup-arco.ts').setupArco(app)
  for (const key of ['$modal', '$drawer', '$message', '$notification']) assert.ok(app.config.globalProperties[key], key)
  assert.equal(arco.Modal._context, app._context)
  assert.equal(arco.Drawer._context, app._context)
  assert.equal(arco.Message._context, app._context)
  assert.ok(app.component('IconUser'))
  assert.ok(app.component('ATooltip'))
  assert.equal(app.component('ACarousel'), undefined)
})

test('动态下拉、日期及开关保留实际Arco渲染与字段值', async () => {
  for (const [type, props, expected] of [
    ['select', { modelValue: 'customer-1', options: [{ value: 'customer-1', label: '合成客户' }] }, '合成客户'],
    ['date-picker', { modelValue: '2026-11-08', valueFormat: 'YYYY-MM-DD', format: 'YYYY-MM-DD' }, '2026-11-08'],
    ['switch', { modelValue: true }, 'aria-checked="true"'],
  ]) {
    const html = await renderToString(vue.createSSRApp({ render: () => vue.h(resolveFormComponent(type), props) }))
    assert.ok(html.includes(expected), `${type}: ${expected}`)
  }
})
