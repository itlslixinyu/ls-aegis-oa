export default async function createMock(env, isBuild) {
  const { VITE_LOCAL_MOCK } = env
  // 本地默认关闭；显式 VITE_LOCAL_MOCK=true 才启用，避免误走 mock
  const localEnabled = VITE_LOCAL_MOCK === 'true'
  // 生产包 mock 改由 main.ts 按 VITE_BUILD_MOCK 动态引入；构建阶段无需挂插件
  // v3 在 serve 时即使用 enable:false 仍会预加载 mock 文件，故关闭时直接不注册
  if (isBuild || !localEnabled) {
    return null
  }
  const { viteMockServe } = await import('vite-plugin-mock')
  return viteMockServe({
    mockPath: 'src/mock',
    logger: true,
    enable: true,
    // 忽略工具文件、_data 与根目录 prod 入口，保留 area/index.ts 等业务 mock
    ignore: (fileName: string) => {
      const normalized = fileName.replace(/\\/g, '/')
      if (/\/_/.test(normalized)) {
        return true
      }
      return /\/mock\/index\.ts$/.test(normalized)
    },
  })
}
