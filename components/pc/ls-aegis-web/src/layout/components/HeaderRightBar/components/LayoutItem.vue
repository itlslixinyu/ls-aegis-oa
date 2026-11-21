<!--
  @file LayoutItem 组件
  @description 布局切换选项组件，支持左侧布局、顶部布局和混合布局三种模式
-->
<template>
  <div
    class="layout-mode-item"
    :class="[`layout-mode-item__${props.mode}`, { 'is-active': appStore.layout === props.mode }]"
    @click="emit('click')"
  >
    <!-- 左侧布局 -->
    <template v-if="props.mode === 'left'">
      <div class="block-left"></div>
      <div class="block-right"></div>
    </template>

    <!-- 顶部布局 -->
    <template v-if="props.mode === 'top'">
      <div class="block-top"></div>
      <div class="block-bottom"></div>
    </template>

    <!-- 混合布局 -->
    <template v-if="props.mode === 'mix'">
      <div class="block-top"></div>
      <div class="block-main">
        <div class="block-left"></div>
        <div class="block-right"></div>
      </div>
    </template>

    <!-- 双列布局 -->
    <template v-if="props.mode === 'columns'">
      <div class="block-left block-column"></div>
      <div class="block-left"></div>
      <div class="block-right"></div>
    </template>

    <icon-check v-if="appStore.layout === props.mode" class="layout-mode-item__check" :size="14" />
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores'

defineOptions({
  name: 'LayoutModeItem',
})

/** Props 默认值 */
const props = withDefaults(defineProps<Props>(), {
  mode: 'left',
})

/** Emits 定义 */
const emit = defineEmits<{
  (e: 'click'): void
}>()

/** 布局模式类型 */
type LayoutMode = 'left' | 'top' | 'mix' | 'columns'

/** Props 类型定义 */
interface Props {
  /** 布局模式 */
  mode?: LayoutMode
  /** 布局名称（兼容旧用法，缩略图布局不再展示文案） */
  name?: string
}

/** 应用状态 */
const appStore = useAppStore()
</script>

<style lang="scss" scoped>
// 布局项基础样式
.layout-mode-item {
  position: relative;
  width: 100%;
  height: 50px;
  padding: 4px;
  display: flex;
  cursor: pointer;
  overflow: hidden;
  box-sizing: border-box;
  border-radius: 4px;
  background-color: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  transition: border-color 0.2s;

  &:hover,
  &.is-active {
    border-color: rgb(var(--primary-6));
  }

  &__check {
    position: absolute;
    right: 4px;
    bottom: 4px;
    color: rgb(var(--primary-6));
  }
}

.block-left,
.block-right,
.block-top,
.block-bottom {
  border-radius: 2px;
}

// 左侧布局样式
.layout-mode-item__left {
  .block-left {
    width: 10px;
    background-color: $color-theme;
  }

  .block-right {
    flex: 1;
    margin-left: 4px;
    background-color: var(--color-fill-3);
  }
}

// 顶部布局样式
.layout-mode-item__top {
  flex-direction: column;

  .block-top {
    height: 8px;
    background-color: $color-theme;
  }

  .block-bottom {
    flex: 1;
    margin-top: 4px;
    background-color: var(--color-fill-3);
  }
}

// 混合布局样式
.layout-mode-item__mix {
  flex-direction: column;

  .block-top {
    height: 8px;
    margin-bottom: 3px;
    background-color: $color-theme;
  }

  .block-main {
    flex: 1;
    display: flex;

    .block-left {
      width: 10px;
      background-color: $color-theme;
    }

    .block-right {
      flex: 1;
      margin-left: 3px;
      background-color: var(--color-fill-3);
    }
  }
}

// 双列布局样式
.layout-mode-item__columns {

  .block-left {
    width: 10px;
    background-color: $color-theme;
  }

  .block-right {
    flex: 1;
    margin-left: 4px;
    background-color: var(--color-fill-3);
  }

  .block-column {
    margin-right: 4px;
  }
}
</style>
