import type { RouteRecordRaw } from 'vue-router'
import { computed } from 'vue'
import eachTree from 'xe-utils/eachTree'
import { useRouteStore } from '@/stores'
import { filterTree, isRouteHidden } from '@/utils'

/**
 * 菜单管理 Hooks
 * 用于获取和处理应用程序的菜单列表
 * 提供菜单的过滤、展平处理等功能
 * @returns {object} 包含处理后菜单列表的响应式对象
 */
export function useMenu() {
  // 路由存储实例，用于获取原始路由配置
  const routeStore = useRouteStore()

  /**
   * 处理后的菜单列表
   * 响应式计算属性，当路由配置变化时自动更新，
   * > hidden:false那么代表这个路由项显示在左侧菜单栏中
   * > 子项chidren只有一个hidden:false的子元素时, 且alwaysShow为false, 那么这个子项会被展平到父项中
   * 包含以下处理逻辑：
   * 1. 深拷贝原始路由配置避免直接修改
   * 2. 过滤掉设置为隐藏的菜单项
   * 3. 展平只有一个子项的菜单项（提升用户体验）
   */
  const menuList = computed(() => {
    // 深拷贝路由配置，防止修改原始数据
    const cloneRoutes = JSON.parse(JSON.stringify(routeStore.routes)) as RouteRecordRaw[]

    // 过滤出非隐藏的菜单（兼容 isHidden=false 被 JSON 省略）
    const showMenuList = filterTree(cloneRoutes, (i) => !isRouteHidden(i.meta?.hidden)) as RouteRecordRaw[]

    // 仅当「目录始终展开」关闭，且唯一子页与目录 path 相同时才展平。
    // 报价管理/项目管理各只有一页，path 不同，必须保留子菜单，避免侧栏看起来是空目录。
    eachTree(showMenuList, (i) => {
      const only = i?.children?.length === 1 ? i.children[0] : null
      if (only && i?.meta?.alwaysShow !== true && only.path === i.path) {
        if (i.meta) {
          i.meta.title = i.meta?.title || only.meta?.title
          i.meta.svgIcon = i.meta?.svgIcon || only.meta?.svgIcon
          i.meta.icon = i.meta?.icon || only.meta?.icon
        }
        i.path = only.path
        delete i.children
      }
    })
    return showMenuList
  })

  return {
    menuList,
  }
}
