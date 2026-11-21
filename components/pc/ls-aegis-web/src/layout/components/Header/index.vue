<template>
  <a-layout-header class="header">
    <section class="fold-btn-wrapper">
      <MenuFoldBtn></MenuFoldBtn>
    </section>
    <a-row align="center" class="h-full header-right">
      <a-col v-if="appStore.showBreadcrumb !== false" :xs="0" :md="10" :lg="10" :xl="12" :xxl="12">
        <Breadcrumb></Breadcrumb>
      </a-col>
      <a-col
        :xs="24"
        :md="appStore.showBreadcrumb !== false ? 14 : 24"
        :lg="appStore.showBreadcrumb !== false ? 14 : 24"
        :xl="appStore.showBreadcrumb !== false ? 12 : 24"
        :xxl="appStore.showBreadcrumb !== false ? 12 : 24"
      >
        <a-row justify="end" align="center">
          <HeaderRightBar></HeaderRightBar>
        </a-row>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import HeaderRightBar from '../HeaderRightBar/index.vue'
import MenuFoldBtn from '../MenuFoldBtn.vue'
import { useAppStore } from '@/stores'

defineOptions({ name: 'LayoutHeader' })

const appStore = useAppStore()
</script>

<style scoped lang="scss">
.arco-dropdown-open .arco-icon-down {
  transform: rotate(180deg);
}

.header {
  display: flex;
  align-items: center;

  .header-right {
    flex: 1;
    overflow: hidden;
    margin-left: $padding;
  }
}

.arco-layout-header {
  padding: 0 $padding;
  height: 56px;
  background: var(--color-bg-1);
  border-bottom: 1px solid var(--color-neutral-3);
}
</style>
