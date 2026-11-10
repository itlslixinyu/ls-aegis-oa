import { Button, Message, Notification, Space } from '@arco-design/web-vue'
import NProgress from 'nprogress'
import type { NavigationGuardNext, Router } from 'vue-router'
import { useAppStore, useRouteStore, useUserStore } from '@/stores'
import { getToken } from '@/utils/auth'
import { isHttp } from '@/utils/validate'
import { createVersionChecker } from '@/utils/version-check'
import { safeLoginRedirect } from '@/utils/login-redirect'
import { isRetiredNavigation } from '@/utils/retired-navigation'
import 'nprogress/nprogress.css'

NProgress.configure({
  easing: 'ease', // 动画方式
  speed: 500, // 递增进度条的速度
  showSpinner: false, // 是否显示圆圈加载
  trickleSpeed: 200, // 自动递增间隔
  minimum: 0.3, // 初始化时的最小百分比
})

// 版本更新
// 更新
const onUpdateSystem = (id: string) => {
  Notification.remove(id)
  window.location.reload()
}
// 关闭更新弹窗
const onCloseUpdateSystem = (id: string) => {
  Notification.remove(id)
}
// 提示用户更新弹窗
const handleNotification = () => {
  const id = `updateModel`
  Notification.info({
    id,
    title: '新版本更新',
    content: '当前系统检测到有新的版本，请及时更新',
    duration: 0,
    closable: true,
    position: 'bottomRight',
    footer: () => {
      return h(Space, {}, () => [h(Button, {
        type: 'primary',
        onClick: () => onUpdateSystem(id),
      }, '更新'), h(Button, { type: 'secondary', onClick: () => onCloseUpdateSystem(id) }, '关闭')])
    },
  })
}

/**
 * 获取首页的 ETag 或 Last-Modified 值，作为当前版本标识
 * @returns {Promise<string|null>} 返回 ETag 或 Last-Modified 值
 */
const getVersionTag = async () => {
  const response = await fetch(import.meta.env.BASE_URL, {
    method: 'HEAD',
    cache: 'no-cache',
  })
  if (!response.ok) throw new Error('版本探测失败')
  return response.headers.get('etag') || response.headers.get('last-modified')
}

/**
 * 比较当前的 ETag 或 Last-Modified 值与最新获取的值
 */
const compareTag = createVersionChecker(getVersionTag, handleNotification)

/** 免登录白名单 */
const whiteList = ['/login', '/pwdExpired', '/forgot-password']

/** 强制改密提示（初始随机密码 / 口令过期） */
const PWD_EXPIRED_TIP = '密码已过期或为初始密码，请先修改后再使用系统'

/** 是否已经生成过路由表 */
let hasRouteFlag = false
export const resetHasRouteFlag = () => {
  hasRouteFlag = false
}

/** 初始口令或密码过期时，除改密页外一律拦截 */
const redirectIfPwdExpired = (
  userStore: ReturnType<typeof useUserStore>,
  toPath: string,
  next: NavigationGuardNext,
) => {
  if (userStore.userInfo.pwdExpired && toPath !== '/pwdExpired') {
    Message.warning(PWD_EXPIRED_TIP)
    next('/pwdExpired')
    return true
  }
  return false
}

/** 初始化路由守卫 */
export const setupRouterGuard = (router: Router) => {
  router.beforeEach(async (to, from, next) => {
    NProgress.start()
    const userStore = useUserStore()
    const routeStore = useRouteStore()
    if (isRetiredNavigation(to)) {
      next({ path: '/dashboard/workplace', replace: true })
      return
    }
    // 判断该用户是否登录
    if (getToken()) {
      if (to.path === '/login') {
        // 已登录访问登录页：进入首页（由后续守卫拉用户信息 / 动态路由）
        next({ path: '/' })
      } else {
        if (!hasRouteFlag) {
          try {
            await userStore.getInfo()
            if (redirectIfPwdExpired(userStore, to.path, next)) {
              hasRouteFlag = true
              return
            }
            // 鉴权完成后，菜单与界面偏好互不依赖，同时加载，减少一次串行等待。
            const appStore = useAppStore()
            const [accessRoutes] = await Promise.all([
              routeStore.generateRoutes(),
              appStore.loadAccountSettings().catch(() => undefined),
            ])
            accessRoutes.forEach((route) => {
              if (!isHttp(route.path)) {
                router.addRoute(route) // 动态添加可访问路由表
              }
            })
            hasRouteFlag = true
            // 确保添加路由已完成
            // 设置 replace: true, 因此导航将不会留下历史记录
            next({ ...to, replace: true })
          } catch (error: any) {
            // 拉用户信息或动态路由失败：清会话并回登录，避免「欢迎使用」后卡在登录页无提示
            const tip = error?.message || error?.msg || '登录状态校验失败，请重新登录'
            Message.error(typeof tip === 'string' ? tip : '登录状态校验失败，请重新登录')
            await userStore.logoutCallBack()
            next({ path: '/login', query: { redirect: safeLoginRedirect(to.fullPath) } })
          }
        } else {
          // 二次导航也必须拦截未改密（含 ErrorPage「返回首页」绕过）
          if (redirectIfPwdExpired(userStore, to.path, next)) {
            return
          }
          next()
        }
      }
    } else {
      // 如果没有 Token
      if (whiteList.includes(to.path)) {
        // 如果在免登录的白名单中，则直接进入
        next()
      } else {
        // 其他没有访问权限的页面将被重定向到登录页面
        next({ path: '/login', query: { redirect: safeLoginRedirect(to.fullPath) } })
      }
    }

    // 生产环境开启检测版本更新
    const isProd = import.meta.env.PROD
    if (isProd) {
      await compareTag()
    }
  })

  router.onError((error) => {
    NProgress.done()
    const msg = error?.message || String(error)
    if (/Failed to fetch dynamically imported module|Loading chunk|Cannot find module/i.test(msg)) {
      Message.error('页面资源加载失败，请刷新后重试；若刚更新前端，请强制刷新缓存')
    }
  })

  router.afterEach(() => {
    NProgress.done()
  })
}
