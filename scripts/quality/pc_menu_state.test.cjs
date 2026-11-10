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

function createMenu(routes, routePath = '/psi/stock') {
  const store = vue.reactive({ routes })
  const route = vue.reactive({ path: routePath, meta: {}, matched: [] })
  const navigation = []
  const external = []
  const router = {
    push: to => navigation.push({ method: 'push', ...to }),
    replace: to => navigation.push({ method: 'replace', ...to }),
  }
  const cache = new Map()
  function load(file) {
    if (cache.has(file)) return cache.get(file)
    const source = fs.readFileSync(path.join(root, 'ls-aegis-web/src', file), 'utf8')
    const code = ts.transpileModule(source, { compilerOptions: {
      module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true,
    } }).outputText
    const result = { exports: {} }
    const modules = {
      vue, 'vue-router': { useRoute: () => route, useRouter: () => router },
      'xe-utils': webRequire('xe-utils'), 'lodash-es': {},
      '@arco-design/web-vue': { Message: {} }, 'cron-parser': {},
      '@/stores': { useRouteStore: () => store },
      // The retired publisher has no callers; reproduce that absence on the old implementation.
      '@/hooks': { useRouteListener: () => ({ listenerRouteChange() {} }) },
      '@/utils/safeUrl': { openSafeExternal: url => external.push(url) },
    }
    vm.runInNewContext(code, { ...vue, useRoute: () => route, useRouter: () => router,
      exports: result.exports, require: name => {
        if (name.startsWith('xe-utils/')) return webRequire(name)
        if (name in modules) return modules[name]
        if (name === '@/utils') return load('utils/index.ts')
        if (name === '@/utils/validate') return load('utils/validate.ts')
        if (name === './useMenus') return load('layout/hooks/useMenus.ts')
        throw new Error(`Undeclared menu dependency: ${name}`)
      },
    }, { filename: file })
    cache.set(file, result.exports)
    return result.exports
  }
  const menu = load('layout/hooks/useLevelMenu.ts').useLevelMenu()
  menu.getOneLevelMenus?.()
  return { menu, store, route, navigation, external }
}

const roots = () => [
  { path: '/psi', meta: { title: '库存' }, children: [{ path: '/psi/stock' }, { path: '/psi/goods' }] },
  { path: '/eoms', children: [{ path: '/eoms/order' }, { path: '/eoms/apply' }] },
]
const paths = entries => Array.from(entries, entry => entry.path)

test('multi-level menu selects its root immediately without a publisher event', () => {
  const { menu } = createMenu(roots())
  assert.equal(menu.oneLevelMenuActiveRoute.value?.path, '/psi')
  assert.deepEqual(paths(menu.twoLevelMenus.value), ['/psi/stock', '/psi/goods'])
})

test('route changes update root selection and retain all sibling menu items', () => {
  const { menu, route } = createMenu(roots())
  route.path = '/eoms/order'
  assert.equal(menu.oneLevelMenuActiveRoute.value?.path, '/eoms')
  assert.deepEqual(paths(menu.twoLevelMenus.value), ['/eoms/order', '/eoms/apply'])
})

test('late permission menu load and replacement update both menu levels', () => {
  const { menu, store } = createMenu([])
  assert.equal(menu.oneLevelMenuActiveRoute.value, null)
  store.routes = roots()
  assert.deepEqual(paths(menu.oneLevelMenus.value), ['/psi', '/eoms'])
  assert.equal(menu.oneLevelMenuActiveRoute.value?.path, '/psi')
  store.routes = roots().slice(1)
  assert.deepEqual(paths(menu.oneLevelMenus.value), ['/eoms'])
  assert.equal(menu.oneLevelMenuActiveRoute.value, null)
})

test('hidden detail routes select their visible activeMenu', () => {
  const { menu, route } = createMenu(roots(), '/repair/detail/1')
  route.meta.activeMenu = '/eoms/order'
  assert.equal(menu.oneLevelMenuActiveRoute.value?.path, '/eoms')
  assert.deepEqual(paths(menu.twoLevelMenus.value), ['/eoms/order', '/eoms/apply'])
})

test('unlisted detail route chooses longest path prefix with a segment boundary', () => {
  const { menu, route } = createMenu([
    { path: '/ops', children: [{ path: '/ops/list' }] },
    { path: '/ops-tools', children: [{ path: '/ops-tools/list' }] },
  ], '/ops-tools/detail/1')
  assert.equal(menu.oneLevelMenuActiveRoute.value?.path, '/ops-tools')
  route.path = '/ops-other/detail/1'
  assert.equal(menu.oneLevelMenuActiveRoute.value, null)
})

test('menu preparation preserves source routes and honors hidden flags and alwaysShow', () => {
  const source = [
    { path: '/hidden', meta: { hidden: 1 } },
    { path: '/flat', children: [{ path: '/flat', meta: { title: 'Single' } }] },
    { path: '/keep', meta: { alwaysShow: true }, children: [{ path: '/keep' }] },
  ]
  const before = JSON.stringify(source)
  const { menu } = createMenu(source, '/flat')
  assert.deepEqual(paths(menu.oneLevelMenus.value), ['/flat', '/keep'])
  assert.equal(menu.oneLevelMenus.value[0].children, undefined)
  assert.equal(menu.oneLevelMenus.value[1].children.length, 1)
  assert.equal(JSON.stringify(source), before)
})

test('menu redirects open external targets and preserve push/replace semantics', () => {
  const { menu, navigation, external } = createMenu([
    { path: '/links', redirect: 'https://example.com/help' },
    { path: '/ops', redirect: 'noRedirect', children: [{ path: '/ops/list' }] },
  ])
  menu.handleMenuItemClickByPath('/links')
  menu.handleMenuItemClickByPath('/ops')
  menu.handleMenuItemClickByItem({ path: '/leaf', redirect: 'noRedirect' })
  assert.deepEqual(external, ['https://example.com/help'])
  assert.deepEqual(navigation, [{ method: 'push', path: '/ops/list' }, { method: 'replace', path: '/leaf' }])
})
