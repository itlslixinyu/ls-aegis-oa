import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { RouteRecordRaw, Router } from 'vue-router'
import mapTree from 'xe-utils/mapTree'
import toTreeArray from 'xe-utils/toTreeArray'
import { cloneDeep, omit } from 'lodash-es'
import { constantRoutes, systemRoutes } from '@/router/route'
import { type RouteItem, getUserRoute } from '@/apis'
import { transformPathToName } from '@/utils'
import { asyncRouteModules } from '@/router/asyncModules'
import { getToken } from '@/utils/auth'
import { activeNavigation, isRetiredNavigation } from '@/utils/retired-navigation'

const layoutComponentMap = {
  Layout: () => import('@/layout/index.vue'),
  ParentView: () => import('@/components/ParentView/index.vue'),
}

/** 历史菜单 component 别名 → 实际视图路径（避免库未升版时空白/告警） */
const componentAliases: Record<string, string> = {
  'system/dict/item/index': 'system/dict/index',
}

/** 将component由字符串转成真正的模块 */
const transformComponentView = (component: string) => {
  if (!component) {
    return undefined
  }
  const resolved = componentAliases[component] || component
  const mapped = layoutComponentMap[resolved as keyof typeof layoutComponentMap] || asyncRouteModules[resolved]
  if (!mapped) {
    // 未命中时落到 404，避免内容区静默空白
    console.warn(`[route] component not found: ${component}`)
    return () => import('@/views/default/error/404.vue')
  }
  return mapped
}

/** 去掉 path 上的 query，避免写入 vue-router */
const toRoutePath = (rawPath?: string) => {
  if (!rawPath) return ''
  const qIndex = rawPath.indexOf('?')
  return qIndex >= 0 ? rawPath.slice(0, qIndex) : rawPath
}

/**
 * @description 前端来做排序、格式化
 * @params {menus} 后端返回的路由数据，已经根据当前用户角色过滤掉了没权限的路由
 * 1. 对后端返回的路由数据进行排序，格式化
 * 2. 同时将component由字符串转成真正的模块
 */
const formatAsyncRoutes = (menus: RouteItem[]) => {
  if (!menus.length) return []

  const pathMap = new Map<string, string>()
  return mapTree(activeNavigation(menus), (item) => {
    const routePath = toRoutePath(item.path)
    pathMap.set(item.id, routePath)

    if (item.children?.length) {
      item.children.sort((a, b) => (a?.sort ?? 0) - (b?.sort ?? 0))
    }

    // 部分子菜单，例如：通知公告新增、查看详情，需要选中其父菜单
    if (item.parentId && item.type === 2) {
      item.activeMenu = item.activeMenu || pathMap.get(item.parentId) || ''
    }

    return {
      path: routePath,
      name: item.name ?? transformPathToName(routePath),
      component: transformComponentView(item.component),
      redirect: item.redirect,
      meta: {
        title: item.title,
        // 归一化：后端可能省略 false；仅 true 视为隐藏
        hidden: !!item.isHidden,
        keepAlive: !!item.isCache,
        icon: item.icon,
        showInTabs: item.showInTabs,
        activeMenu: item.activeMenu,
        alwaysShow: !!item.alwaysShow,
      },
    }
  }) as unknown as RouteRecordRaw[]
}

/**
 * 路由降级为 Layout 下二级，并去掉同 path 的隐藏子页。
 * 业务页（如系统配置、通知公告）没有 router-view，若再挂子路由会导致内容区空白。
 */
export const flatMultiLevelRoutes = (routes: RouteRecordRaw[]) => {
  return cloneDeep(routes).map((route) => {
    if (!route.children?.length) return route

    const flatChildren = toTreeArray(route.children).map((item) =>
      omit(item, 'children'),
    ) as RouteRecordRaw[]

    // 同 path 只留一条：优先非隐藏（如 system/config/index），丢弃配置 Tab 等重复挂载
    const byPath = new Map<string, RouteRecordRaw>()
    for (const child of flatChildren) {
      const p = String(child.path || '')
      if (!p) continue
      const exist = byPath.get(p)
      if (!exist) {
        byPath.set(p, child)
        continue
      }
      if (exist.meta?.hidden && !child.meta?.hidden) {
        byPath.set(p, child)
      }
    }

    return {
      ...route,
      children: [...byPath.values()],
    }
  })
}

const storeSetup = () => {
  // 所有路由(常驻路由 + 动态路由)
  const routes = ref<RouteRecordRaw[]>([])
  // 动态路由(异步路由)
  const asyncRoutes = ref<RouteRecordRaw[]>([])
  let lastLoadedAt = 0
  let lastAttemptAt = 0
  let requestVersion = 0
  let refreshPromise: Promise<void> | undefined

  // 合并路由
  const setRoutes = (data: RouteRecordRaw[]) => {
    // 合并路由并排序
    const active = activeNavigation(data)
    routes.value = [...constantRoutes, ...systemRoutes].concat(active)
      .sort((a, b) => (a.meta?.sort ?? 0) - (b.meta?.sort ?? 0))
    asyncRoutes.value = active
  }

  // 生成路由
  const generateRoutes = async (): Promise<RouteRecordRaw[]> => {
    const identity = getToken()
    const version = ++requestVersion
    const { data } = await getUserRoute()
    if (identity !== getToken() || version !== requestVersion) throw new Error('登录状态或菜单请求已变化')
    const asyncRoutes = formatAsyncRoutes(data)
    const flatRoutes = flatMultiLevelRoutes(cloneDeep(asyncRoutes))
    setRoutes(asyncRoutes)
    lastLoadedAt = Date.now()
    return flatRoutes
  }

  const clearRoutes = () => {
    ++requestVersion
    lastLoadedAt = 0
    lastAttemptAt = 0
    setRoutes([])
  }
  const pruneRetiredRoutes = (router: Router) => {
    routes.value = activeNavigation(routes.value)
    asyncRoutes.value = activeNavigation(asyncRoutes.value)
    router.getRoutes().filter(isRetiredNavigation).forEach((route) => {
      if (route.name) router.removeRoute(route.name)
    })
  }
  // One request at most per minute on navigation/focus events; no background polling.
  const refreshRoutes = (router: Router): Promise<void> => {
    pruneRetiredRoutes(router)
    if (refreshPromise) return refreshPromise
    if (!getToken() || !lastLoadedAt || Date.now() - Math.max(lastLoadedAt, lastAttemptAt) < 60_000) return Promise.resolve()
    lastAttemptAt = Date.now()
    const previous = [...asyncRoutes.value]
    refreshPromise = (async () => {
      const fresh = await generateRoutes()
      previous.forEach((route) => { if (route.name && router.hasRoute(route.name)) router.removeRoute(route.name) })
      fresh.forEach((route) => router.addRoute(route))
      if (isRetiredNavigation(router.currentRoute.value)) await router.replace('/')
    })().finally(() => { refreshPromise = undefined })
    return refreshPromise
  }

  return {
    routes,
    asyncRoutes,
    generateRoutes,
    clearRoutes,
    pruneRetiredRoutes,
    refreshRoutes,
  }
}

export const useRouteStore = defineStore('route', storeSetup, {
  // 路由含动态 import 函数，持久化会丢 component 导致空白页；仅内存态即可
  persist: false,
})
