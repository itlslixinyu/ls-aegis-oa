<template>
  <div
    v-if="isDesktop" class="asider" :class="{ 'app-menu-dark': appStore.menuDark }"
    :style="appStore.menuDark ? appStore.themeCSSVar : undefined"
  >
    <!--
      不用 a-layout-sider 的受控 collapsed / breakpoint：
      折叠改宽 ↔ 滚动条 ↔ 视口抖动 会与 @collapse 形成递归更新。
      窄屏由 v-if="isDesktop" 处理；折叠仅改 width。
    -->
    <div class="menu" :style="{ width: siderWidth }">
      <Logo :collapsed="appStore.menuCollapse"></Logo>
      <a-scrollbar outer-class="menu-scroll-view" style="height: 100%; overflow: auto">
        <Menu :menus="menuList"></Menu>
      </a-scrollbar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import Menu from '../Menu/index.vue'
import Logo from '../Logo.vue'
import { useAppStore } from '@/stores'
import { useDevice } from '@/hooks'
import { useMenu } from '@/layout/hooks/useMenus'

defineOptions({ name: 'Asider' })
const appStore = useAppStore()
const { isDesktop } = useDevice()
const { menuList } = useMenu()

const siderWidth = computed(() => (appStore.menuCollapse ? '48px' : '180px'))
</script>

<style scoped lang="scss">
:deep(.arco-menu.arco-menu-vertical.arco-menu-collapsed) {

  // Menu菜单组件修改
  .arco-menu-icon {
    margin-right: 0;
    padding: 10px 0;
  }

  .arco-menu-has-icon {
    padding: 0;
    justify-content: center;
  }

  .arco-menu-title {
    display: none;
  }
}

.asider {
  z-index: 1000;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border-2);
  box-sizing: border-box;
  color: var(--color-text-1);
  background-color: var(--color-bg-1);
  flex-shrink: 0;

  .menu-scroll-view {
    flex: 1;
    overflow: hidden;
  }

  .menu {
    flex: 1;
    overflow: hidden;
    background-color: inherit;
    display: flex;
    flex-direction: column;
    height: 100%;
    transition: width 0.2s;
  }
}
</style>
