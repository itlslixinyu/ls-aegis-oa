const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const root = path.resolve(__dirname, '../..')
const req = createRequire(path.join(root, 'ls-aegis-web/package.json'))
const ts = req('typescript')
const vue = req('vue')

test('消息页重复进入后只由当前实例刷新计数，卸载不误删其他订阅', async () => {
  const source = fs.readFileSync(path.join(root, 'ls-aegis-web/src/views/user/message/index.vue'), 'utf8')
    .match(/<script setup lang="tsx">([\s\S]*?)<\/script>/)[1]
  const code = ts.transpileModule(source, { fileName: 'Message.tsx', compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.React,
  } }).outputText
  const bus = req('mitt')()
  let unrelated = 0
  bus.on('count-refresh', () => unrelated++)
  const instances = []
  function mount() {
    const mounted = [], unmounted = []
    const calls = { message: 0, notice: 0 }
    vm.runInNewContext(code, { ...vue, exports: {}, defineOptions: () => {},
      onMounted: fn => mounted.push(fn), onUnmounted: fn => unmounted.push(fn),
      require: name => {
        if (name === 'vue-router') return { useRoute: () => ({ query: {}, path: '/user/message' }), useRouter: () => ({}) }
        if (name === '@/hooks') return { useDevice: () => ({ isDesktop: true }) }
        if (name === '@/utils/mitt') return { default: bus }
        if (name.endsWith('.vue')) return {}
        if (name === '@/apis') return {
          getUnreadMessageCount: async () => { calls.message++; return { data: { total: 2 } } },
          getUnreadNoticeCount: async () => { calls.notice++; return { data: { total: 3 } } },
        }
        throw new Error(`未声明测试依赖：${name}`)
      },
    })
    mounted.forEach(fn => fn())
    const item = { calls, unmount: () => unmounted.forEach(fn => fn()) }
    instances.push(item)
    return item
  }
  for (let i = 0; i < 4; i++) {
    const current = mount()
    assert.deepEqual(current.calls, { message: 1, notice: 1 })
    bus.emit('count-refresh')
    assert.deepEqual(current.calls, { message: 2, notice: 2 })
    current.unmount()
  }
  bus.emit('count-refresh')
  for (const item of instances) assert.deepEqual(item.calls, { message: 2, notice: 2 })
  assert.equal(unrelated, 5)
  await Promise.resolve()
})
