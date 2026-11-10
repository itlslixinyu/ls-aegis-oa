import type { RouteRecordRaw } from 'vue-router'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMenu } from './useMenus'
import { isExternal } from '@/utils/validate'
import { openSafeExternal } from '@/utils/safeUrl'

function containsPath(menu: RouteRecordRaw, path: string): boolean {
  return menu.path === path || (menu.children?.some((child) => containsPath(child, path)) ?? false)
}

/** 混合/分栏布局共用响应式菜单；不另存一份权限菜单快照。 */
export function useLevelMenu() {
  const route = useRoute()
  const router = useRouter()
  const { menuList: oneLevelMenus } = useMenu()

  const oneLevelMenuActiveRoute = computed<RouteRecordRaw | null>(() => {
    const path = route.meta?.activeMenu || route.path
    const menus = oneLevelMenus.value
    const exact = menus.find((menu) => containsPath(menu, path))
    if (exact) return exact
    const matched = menus.find((menu) => route.matched.some((item) => item.path === menu.path))
    if (matched) return matched
    // 详情页可能不在菜单中；按完整路径段找最长前缀，避免 /ops 匹配 /ops-tools。
    return menus.reduce<RouteRecordRaw | null>((selected, menu) => {
      if (menu.path && path.startsWith(`${menu.path}/`) && menu.path.length > (selected?.path.length ?? 0)) {
        return menu
      }
      return selected
    }, null)
  })

  const twoLevelMenus = computed(() => oneLevelMenuActiveRoute.value?.children || [])

  function targetPath(item: RouteRecordRaw): string {
    const redirect = typeof item.redirect === 'string' ? item.redirect : undefined
    if (item.children?.length && (!redirect || redirect === 'noRedirect')) {
      return item.children[0].path
    }
    return redirect && redirect !== 'noRedirect' ? redirect : item.path
  }

  function navigate(path: string, replace: boolean) {
    if (isExternal(path)) {
      openSafeExternal(path)
      return
    }
    if (replace) router.replace({ path })
    else router.push({ path })
  }

  function handleMenuItemClickByItem(item: RouteRecordRaw) {
    navigate(targetPath(item), true)
  }

  function handleMenuItemClickByPath(path: string) {
    const item = oneLevelMenus.value.find((menu) => menu.path === path)
    navigate(item ? targetPath(item) : path, false)
  }

  return { oneLevelMenus, twoLevelMenus, oneLevelMenuActiveRoute, handleMenuItemClickByItem, handleMenuItemClickByPath }
}
