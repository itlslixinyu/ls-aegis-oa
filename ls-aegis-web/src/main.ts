import { createApp } from 'vue'
import { setupArco } from '@/utils/setup-arco'
import '@/styles/arco-ui/index.less'
// import '@arco-themes/vue-gi-demo/index.less'
// import '@arco-design/web-vue/dist/arco.css'

import App from './App.vue'
import router from './router'

// 使用动画库
import 'animate.css/animate.min.css'

// 自定义过渡动画
import '@/styles/css/transition.css'

// 导入全局scss主文件
import '@/styles/index.scss'

// 支持SVG
import 'virtual:svg-icons-register'

// 自定义指令
import directives from './directives'

// 状态管理
import pinia from '@/stores'
import { cleanRetiredNavigationStorage } from '@/utils/retired-navigation'
import { setupMenuSynchronization } from '@/router/menu-sync'

// 测试包可按需启用生产 mock（VITE_BUILD_MOCK=true）
if (import.meta.env.VITE_BUILD_MOCK === 'true') {
  void import('./mock').then(({ setupProdMockServer }) => setupProdMockServer())
}

cleanRetiredNavigationStorage(sessionStorage, localStorage)
const app = createApp(App)

app.use(router)
app.use(pinia)
setupArco(app)
app.use(directives)

app.mount('#app')
const stopMenuSynchronization = setupMenuSynchronization(router)
if (import.meta.hot) import.meta.hot.dispose(stopMenuSynchronization)

// 全站统一修复 Chrome Issues：表单 name/id 缺失、label 未正确关联控件（含 Arco FormItem）
void import('@/utils/form-field-identity').then(({ watchFormAccessibility }) => {
  watchFormAccessibility()
})
