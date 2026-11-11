import roleRoutes from '#h5-routes'
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useSystemStore } from '@/store/system'

/** 构建/启动时注入：customer | engineer */
const h5Role = (typeof __H5_ROLE__ !== 'undefined' ? __H5_ROLE__ : 'customer')
const homePath = h5Role === 'engineer' ? '/engineer/login' : '/customer/auth'

const routes = [
  {
    path: '/',
    redirect: homePath
  },
  ...roleRoutes,
  // 保留另一端已知入口原有的回本站登录行为，不引入另一端页面模块。
  ...(h5Role === 'engineer'
    ? ['/customer/todos', '/customer/auth', '/customer/workbench', '/customer/apply', '/customer/repairs', '/customer/repairs/all', '/customer/repairs/status/:status', '/customer/profile', '/customer/repair/:id']
    : ['/engineer/todos', '/engineer/login', '/engineer/workbench', '/engineer/tasks', '/engineer/tasks/status/:status', '/engineer/task/:id', '/engineer/record/:orderId', '/engineer/profile'])
    .map(path => ({ path, redirect: homePath })),
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题（统一通过 system store，会持久化缓存，避免刷新时标题闪烁）
  if (to.meta.title) {
    useSystemStore().setPageTitle(to.meta.title)
  } else {
    useSystemStore().setPageTitle('')
  }

  // 角色隔离：客户站不进工程师页，工程师站不进客户页
  if (to.meta.role && to.meta.role !== h5Role) {
    return next(homePath)
  }

  const userStore = useUserStore()

  // 需要H5令牌的客户页面
  if (to.meta.requireH5Token) {
    if (!userStore.h5Token) {
      return next('/customer/auth')
    }
  }

  // 需要工程师登录的页面
  if (to.meta.requireAuth) {
    if (!userStore.engineerToken) {
      return next('/engineer/login')
    }
  }

  next()
})

export default router
