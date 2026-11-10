import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  showToast,
  showFailToast,
  showSuccessToast,
  showLoadingToast,
  showDialog,
  showConfirmDialog,
  showNotify
} from 'vant'
// 模板组件由VantResolver按需导入；函数式API需要显式保留自身样式。
import 'vant/es/toast/style'
import 'vant/es/dialog/style'
import 'vant/es/notify/style'
import '@vant/touch-emulator'
import App from './App.vue'
import router from './router'
import './styles/index.css'
import { useSystemStore } from './store/system'

const app = createApp(App)
app.use(createPinia())
app.use(router)

// 【标题抗闪烁：HMR/构建后双重保险】在 loadSystemInfo 异步接口之前，
// 同步读取 localStorage 缓存的标题立刻设置。配合 index.html 的 head 内联脚本
// 一起工作：应对 Vite HMR 热更新（HTML head 不重新解析）的场景
try {
  const cache = JSON.parse(localStorage.getItem('h5-app-title-cache') || '{}')
  if (cache) {
    if (cache.lastPageTitle) {
      document.title = cache.lastPageTitle
    } else if (cache.h5Title) {
      document.title = cache.h5Title
    }
  }
} catch (e) {
  // 静默忽略
}

app.mount('#app')

// 应用启动时加载系统外观配置（系统名称、Logo、版权信息等）
useSystemStore().loadSystemInfo()

// Vant 4 函数式 API（showToast/showDialog 等）需要显式挂载到全局，
// 否则 unplugin-vue-components 的 VantResolver 只按需导入模板组件，
// JS 中直接调用 showFailToast 会被 tree-shaking 移除导致 ReferenceError
window.showToast = showToast
window.showFailToast = showFailToast
window.showSuccessToast = showSuccessToast
window.showLoadingToast = showLoadingToast
window.showDialog = showDialog
window.showConfirmDialog = showConfirmDialog
window.showNotify = showNotify
