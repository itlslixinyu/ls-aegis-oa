<template>
  <a-menu
    :mode="mode"
    :selected-keys="activeMenu"
    :auto-open-selected="autoOpenSelected"
    :accordion="appStore.menuAccordion"
    :trigger-props="{ animationName: 'slide-dynamic-origin' }"
    :collapsed="menuCollapsed"
    :style="menuStyle"
    @menu-item-click="onMenuItemClick"
  >
    <MenuItem v-for="(item, index) in sidebarRoutes" :key="item.path + index" :item="item"></MenuItem>
  </a-menu>
</template>

<script setup lang="ts">
import type { RouteRecordRaw } from 'vue-router'
import type { CSSProperties } from 'vue'
import MenuItem from './MenuItem.vue'
import { useAppStore, useRouteStore } from '@/stores'
import { isExternal } from '@/utils/validate'
import { openSafeExternal } from '@/utils/safeUrl'
import { useDevice } from '@/hooks'

defineOptions({ name: 'AppMenu' })
const props = withDefaults(defineProps<Props>(), {})

const emit = defineEmits<{
  (e: 'menu-item-click-after'): void
}>()

interface Props {
  menus?: RouteRecordRaw[]
  menuStyle?: CSSProperties
}

const { isDesktop } = useDevice()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const routeStore = useRouteStore()
const sidebarRoutes = computed(() => (props.menus ? props.menus : routeStore.routes))

// 菜单垂直模式/水平模式
const mode = computed(() => {
  return ['left', 'mix', 'columns'].includes(appStore.layout) ? 'vertical' : 'horizontal'
})

// 仅展示折叠样式，不监听 @collapse，避免与 store 受控状态互相触发
const menuCollapsed = computed(() => {
  return mode.value === 'vertical' && isDesktop.value ? appStore.menuCollapse : false
})

// 是否默认展开选中的菜单
const autoOpenSelected = computed(() => {
  return ['left', 'mix'].includes(appStore.layout)
})

// 当前页面激活菜单路径，先从路由里面找
const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta?.activeMenu) {
    return [meta.activeMenu]
  }
  return [path]
})

// 菜单项点击事件
const onMenuItemClick = (key: string) => {
  if (isExternal(key)) {
    openSafeExternal(key)
    return
  }
  router.push({ path: key })
  emit('menu-item-click-after')
}
</script>

<style scoped lang="scss"></style>
