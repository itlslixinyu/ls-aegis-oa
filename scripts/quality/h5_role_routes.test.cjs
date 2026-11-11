const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const root = path.resolve(__dirname, '../..')
const ts = createRequire(path.join(root, 'ls-aegis-web/package.json'))('typescript')

function setup(role) {
  let guard
  const user = { h5Token: '', engineerToken: '' }
  const base = role === 'customer' ? '/h5/' : '/h5-engineer/'
  function load(name) {
    const source = fs.readFileSync(path.join(root, 'ls-aegis-h5/src/router', name), 'utf8')
      .replaceAll('import.meta.env.BASE_URL', JSON.stringify(base))
    const code = ts.transpileModule(source, { compilerOptions: {
      module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022,
    } }).outputText
    const exports = {}
    vm.runInNewContext(code, { exports, __H5_ROLE__: role,
      require: name => {
        if (name === '#h5-routes') return load(`${role}-routes.js`)
        if (name === '@/store/user') return { useUserStore: () => user }
        if (name === '@/store/system') return { useSystemStore: () => ({ setPageTitle() {} }) }
        if (name === 'vue-router') return {
          createWebHistory: value => value,
          createRouter: options => ({ ...options, beforeEach: fn => { guard = fn } }),
        }
        throw new Error(`未声明依赖：${name}`)
      },
    })
    return exports
  }
  const router = load('index.js').default
  return { router, user, navigate(meta) { let target; guard({ meta }, {}, value => { target = value }); return target } }
}

for (const role of ['customer', 'engineer']) {
  test(`${role}构建只保留本端页面，另一端已知入口重定向登录，通用404仍可用`, () => {
    const { router } = setup(role)
    const own = router.routes.filter(r => r.meta?.role === role && r.component)
    const expectedPaths = role === 'customer'
      ? ['/customer/todos', '/customer/auth', '/customer/workbench', '/customer/apply', '/customer/repairs/all', '/customer/repairs/status/:status', '/customer/repairs', '/customer/profile', '/customer/repair/:id']
      : ['/engineer/todos', '/engineer/login', '/engineer/workbench', '/engineer/tasks/status/:status', '/engineer/tasks', '/engineer/task/:id', '/engineer/record/:orderId', '/engineer/profile']
    assert.deepEqual(Array.from(own, r => r.path).sort(), expectedPaths.sort())
    for (const route of own) {
      const publicPage = route.path === '/customer/auth' || route.path === '/engineer/login'
      assert.equal(Boolean(route.meta[role === 'customer' ? 'requireH5Token' : 'requireAuth']), !publicPage)
    }
    assert.equal(router.routes.filter(r => r.meta?.role && r.meta.role !== role && r.component).length, 0)
    const peerLogin = role === 'customer' ? '/engineer/login' : '/customer/auth'
    const home = role === 'customer' ? '/customer/auth' : '/engineer/login'
    assert.equal(router.routes.find(r => r.path === peerLogin)?.redirect, home)
    assert.equal(typeof router.routes.find(r => r.name === 'NotFound').component, 'function')
    assert.equal(router.history, role === 'customer' ? '/h5/' : '/h5-engineer/')
  })
  test(`${role}页面继续使用原身份守卫，不能由另一种令牌放行`, () => {
    const env = setup(role)
    const protectedPath = `/${role}/todos`
    const meta = env.router.routes.find(r => r.path === protectedPath).meta
    const home = role === 'customer' ? '/customer/auth' : '/engineer/login'
    env.user[role === 'customer' ? 'engineerToken' : 'h5Token'] = 'other-role-fixture'
    assert.equal(env.navigate(meta), home)
    env.user[role === 'customer' ? 'h5Token' : 'engineerToken'] = 'own-role-fixture'
    assert.equal(env.navigate(meta), undefined)
    assert.equal(env.navigate({ role: role === 'customer' ? 'engineer' : 'customer' }), home)
  })
}
