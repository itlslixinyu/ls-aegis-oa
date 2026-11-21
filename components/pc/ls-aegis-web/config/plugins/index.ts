import type { PluginOption } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'

import appInfo from './app-info'
import createAutoImport from './auto-import'
import createComponents from './components'
import createSvgIcon from './svg-icon'

export default async function createVitePlugins(viteEnv, isBuild = false) {
  const vitePlugins: (PluginOption | PluginOption[])[] = [appInfo(), vue(), vueJsx()]
  // 关闭时不加载插件及其依赖，减少日常开发配置解析的开销。
  if (!isBuild && viteEnv.VITE_OPEN_DEVTOOLS === 'true') {
    const { default: createDevtools } = await import('./devtools')
    vitePlugins.push(await createDevtools(viteEnv))
  }
  vitePlugins.push(createAutoImport())
  vitePlugins.push(createComponents())
  vitePlugins.push(createSvgIcon(isBuild))
  if (!isBuild && viteEnv.VITE_LOCAL_MOCK === 'true') {
    const { default: createMock } = await import('./mock')
    vitePlugins.push(await createMock(viteEnv, isBuild))
  }
  return vitePlugins
}
