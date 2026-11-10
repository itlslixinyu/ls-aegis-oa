<!--
  @file Layout 组件
  @description 布局根组件，支持默认布局、混合布局和顶部布局三种模式
-->
<template>
  <LayoutDefault v-if="layoutKey === 'default'" />
  <LayoutMix v-else-if="layoutKey === 'mix'" />
  <LayoutTop v-else-if="layoutKey === 'top'" />
  <LayoutColumns v-else-if="layoutKey === 'columns'" />
  <LayoutDefault v-else />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import LayoutDefault from './LayoutDefault.vue'
import LayoutColumns from './LayoutColumns.vue'
import LayoutMix from './LayoutMix.vue'
import LayoutTop from './LayoutTop.vue'
import { useAppStore } from '@/stores'

/** 组件名称 */
defineOptions({ name: 'Layout' })

/** 状态管理 */
const appStore = useAppStore()

/** 归一化 layout：历史值 left 与 default 同义 */
const layoutKey = computed(() => {
  const layout = appStore.layout || 'left'
  return layout === 'left' ? 'default' : layout
})
</script>

<style lang="scss" scoped></style>
