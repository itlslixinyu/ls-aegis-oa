// vue-tsc 2.0.x 可能吞掉非预期异常；确保 CI 不会把工具崩溃当作类型检查通过。
const { createRequire } = require('node:module')
const fromVueTsc = createRequire(require.resolve('vue-tsc'))
const runner = fromVueTsc('@volar/typescript/lib/quickstart/runTsc')
const originalRun = runner.runTsc

runner.runTsc = (...args) => {
  try {
    return originalRun(...args)
  } catch (error) {
    // 此异常是 vue-tsc 自身用于重新配置扩展名的正常控制流。
    if (error.message !== 'extensions changed') {
      console.error('类型检查工具执行失败：', error)
      process.exitCode = 1
    }
    throw error
  }
}

process.argv = [process.execPath, 'vue-tsc', '--noEmit', ...process.argv.slice(2)]
require('vue-tsc').run()
