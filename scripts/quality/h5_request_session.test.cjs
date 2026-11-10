const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const axios = require('../../ls-aegis-h5/node_modules/axios/dist/node/axios.cjs')

function fixture() {
  const source = fs.readFileSync(path.join(__dirname, '../../ls-aegis-h5/src/utils/request.js'), 'utf8')
    .replace(/^import .*$/gm, '').replace('export default service', 'return service')
  const timers = []
  const location = { href: '/engineer/tasks' }
  const store = {
    engineerToken: 'engineer-a', h5Token: 'customer-a', engineerSessionVersion: 1, h5SessionVersion: 1,
    clearEngineerToken() { this.engineerToken = ''; this.engineerSessionVersion++ },
    clearH5Token() { this.h5Token = ''; this.h5SessionVersion++ }
  }
  const request = new Function('axios', 'showToast', 'showFailToast', 'useUserStore', 'window', 'setTimeout', source)(
    axios, () => {}, () => {}, () => store, { location }, fn => timers.push(fn))
  return { request, store, location, timers }
}

function response(code, http = 200) {
  return async config => {
    const result = { config, status: http, statusText: '', headers: {}, data: { code, msg: 'controlled rejection' } }
    if (http !== 200) throw new axios.AxiosError('controlled rejection', 'ERR_BAD_REQUEST', config, null, result)
    return result
  }
}

for (const http of [200, 401]) {
  test(`public 401 HTTP ${http} cannot clear either logged-in identity`, async () => {
    const f = fixture()
    await assert.rejects(f.request.get('/system/common/dict/option/site', { headers: { isToken: false }, adapter: response('401', http) }))
    assert.equal(f.store.engineerToken, 'engineer-a')
    assert.equal(f.store.h5Token, 'customer-a')
    assert.equal(f.timers.length, 0)
  })

  test(`stale engineer 401 HTTP ${http} cannot clear a new login`, async () => {
    const f = fixture()
    await assert.rejects(f.request.get('/eoms/engineer/tasks', {
      adapter: async config => {
        assert.equal(config.headers.Authorization, 'Bearer engineer-a')
        f.store.engineerToken = 'engineer-b'; f.store.engineerSessionVersion++
        return response('401', http)(config)
      }
    }))
    assert.equal(f.store.engineerToken, 'engineer-b')
    assert.equal(f.store.h5Token, 'customer-a')
    assert.equal(f.timers.length, 0)
  })
}

test('current customer failure only clears customer, even when engineer is also logged in', async () => {
  const f = fixture()
  await assert.rejects(f.request.get('/eoms/h5/repair/list', { adapter: response('401') }))
  assert.equal(f.store.engineerToken, 'engineer-a')
  assert.equal(f.store.h5Token, '')
  f.timers.forEach(fn => fn())
  assert.equal(f.location.href, '/customer/auth')
})

test('old expiry redirect cannot kick a newer session, even when server reuses same token', async () => {
  const f = fixture()
  await assert.rejects(f.request.get('/eoms/engineer/tasks', { adapter: response('401') }))
  assert.equal(f.store.engineerToken, '')
  f.store.engineerToken = 'engineer-a'; f.store.engineerSessionVersion++
  f.timers.forEach(fn => fn())
  assert.equal(f.location.href, '/engineer/tasks')
  assert.equal(f.store.engineerToken, 'engineer-a')
})

test('current engineer failure still expires engineer and redirects', async () => {
  const f = fixture()
  await assert.rejects(f.request.get('/eoms/engineer/tasks', { adapter: response(401) }))
  assert.equal(f.store.engineerToken, '')
  assert.equal(f.store.h5Token, 'customer-a')
  f.timers.forEach(fn => fn())
  assert.equal(f.location.href, '/engineer/login')
})

test('silent business expiry and public customer-code expiry do not clear current sessions', async () => {
  const f = fixture()
  await assert.rejects(f.request.get('/eoms/h5/repair/list', { skipErrorToast: true, adapter: response(10007) }))
  assert.equal(f.store.h5Token, 'customer-a')
  await assert.rejects(f.request.get('/eoms/h5/auth/token', { headers: { isToken: false }, adapter: response(10007) }))
  assert.equal(f.store.h5Token, 'customer-a')
  assert.equal(f.store.engineerToken, 'engineer-a')
  assert.equal(f.timers.length, 0)
})

test('stale customer-specific code cannot expire refreshed customer session', async () => {
  const f = fixture()
  await assert.rejects(f.request.get('/eoms/h5/repair/list', {
    adapter: config => {
      f.store.h5Token = 'customer-b'; f.store.h5SessionVersion++
      return response('10007')(config)
    }
  }))
  assert.equal(f.store.h5Token, 'customer-b')
  assert.equal(f.timers.length, 0)
})

test('current customer-specific code expires only customer', async () => {
  const f = fixture()
  await assert.rejects(f.request.get('/eoms/h5/repair/list', { adapter: response('10007') }))
  assert.equal(f.store.h5Token, '')
  assert.equal(f.store.engineerToken, 'engineer-a')
})

function loginFixture(login, getInfo) {
  const vue = require('../../ls-aegis-h5/node_modules/vue')
  const source = fs.readFileSync(path.join(__dirname, '../../ls-aegis-h5/src/views/engineer/Login.vue'), 'utf8')
    .split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm, '')
  const f = fixture()
  f.store.setEngineerToken = function (token, info) {
    this.engineerToken = token; this.engineerInfo = info; this.engineerSessionVersion++
  }
  f.navigations = []
  const names = ['ref', 'reactive', 'onMounted', 'useRouter', 'showSuccessToast', 'showFailToast', 'login', 'getInfo',
    'getCodeImg', 'encryptTransport', 'loadGmPublicKey', 'useUserStore', 'useSystemStore', 'setTimeout']
  const values = [vue.ref, vue.reactive, () => {}, () => ({ replace: p => f.navigations.push(p) }), () => {}, () => {},
    login, () => getInfo(f), async () => ({}), async p => p, async () => {}, () => f.store, () => ({}), fn => f.timers.push(fn)]
  f.page = new Function(...names, source + '\nreturn {onSubmit,form,loading}')(...values)
  return f
}

test('failed public login does not erase a preexisting session', async () => {
  const f = loginFixture(async () => { throw Error('invalid credentials') }, async () => ({}))
  await f.page.onSubmit()
  assert.equal(f.store.engineerToken, 'engineer-a')
})

test('old login getInfo failure cannot erase newer login', async () => {
  const f = loginFixture(async () => ({ data: { token: 'login-a' } }), async f => {
    f.store.setEngineerToken('login-b', {})
    throw Error('old info failed')
  })
  await f.page.onSubmit()
  assert.equal(f.store.engineerToken, 'login-b')
})

test('normal login navigates but its delayed navigation cannot survive a replacement session', async () => {
  const f = loginFixture(async () => ({ data: { token: 'login-a' } }), async () => ({ data: { id: '123', username: 'fixture' } }))
  await f.page.onSubmit()
  assert.equal(f.store.engineerToken, 'login-a')
  f.timers.forEach(fn => fn())
  assert.deepEqual(f.navigations, ['/engineer/tasks'])
  f.navigations.length = 0
  f.store.setEngineerToken('login-b', {})
  f.timers.forEach(fn => fn())
  assert.deepEqual(f.navigations, [])
})

test('current login getInfo failure clears only its acquired session', async () => {
  const f = loginFixture(async () => ({ data: { token: 'login-a' } }), async () => { throw Error('current info failed') })
  await f.page.onSubmit()
  assert.equal(f.store.engineerToken, '')
  assert.equal(f.store.h5Token, 'customer-a')
})

test('real Pinia store advances identity generation on same-token login and clear', () => {
  const { defineStore, createPinia } = require('../../ls-aegis-h5/node_modules/pinia')
  const { ref, computed } = require('../../ls-aegis-h5/node_modules/vue')
  const source = fs.readFileSync(path.join(__dirname, '../../ls-aegis-h5/src/store/user.js'), 'utf8')
    .replace(/^import .*$/gm, '').replace('export const useUserStore', 'const useUserStore')
  const storage = new Map()
  const localStorage = { getItem: k => storage.get(k) ?? null, setItem: (k, v) => storage.set(k, v), removeItem: k => storage.delete(k) }
  const useStore = new Function('defineStore', 'ref', 'computed', 'localStorage', source + '\nreturn useUserStore')(
    defineStore, ref, computed, localStorage)
  const store = useStore(createPinia())
  for (const [kind, key] of [['Engineer', 'engineer'], ['H5', 'h5']]) {
    const initial = store[`${key}SessionVersion`]
    store[`set${kind}Token`]('same-token', {})
    store[`set${kind}Token`]('same-token', {})
    assert.equal(store[`${key}SessionVersion`], initial + 2)
    store[`clear${kind}Token`]()
    assert.equal(store[`${key}SessionVersion`], initial + 3)
    assert.equal(store[`${key}Token`], '')
  }
})
