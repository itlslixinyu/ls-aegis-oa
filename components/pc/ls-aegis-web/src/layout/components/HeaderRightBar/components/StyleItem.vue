<!--
  @file StyleItem 组件
  @description 整体风格缩略图：深色侧栏 / 浅色 / 暗黑
-->
<template>
  <div class="style-item" :class="{ 'is-active': active }" @click="emit('click')">
    <div class="style-item__preview" :class="`style-item__preview--${props.mode}`">
      <div class="block-side"></div>
      <div class="block-main">
        <div class="block-header"></div>
        <div class="block-body"></div>
      </div>
    </div>
    <icon-check v-if="active" class="style-item__check" :size="14" />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StyleItem' })

export type StyleMode = 'dark-menu' | 'light' | 'dark'

interface Props {
  mode: StyleMode
  active?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  active: false,
})

const emit = defineEmits<{
  (e: 'click'): void
}>()
</script>

<style lang="scss" scoped>
.style-item {
  position: relative;
  width: 100%;
  padding: 6px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid var(--color-border-2);
  background: var(--color-bg-2);
  box-sizing: border-box;
  transition: border-color 0.2s;

  &:hover,
  &.is-active {
    border-color: rgb(var(--primary-6));
  }

  &__preview {
    height: 48px;
    display: flex;
    overflow: hidden;
    border-radius: 2px;

    .block-side {
      width: 12px;
      flex-shrink: 0;
    }

    .block-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .block-header {
      height: 10px;
      flex-shrink: 0;
    }

    .block-body {
      flex: 1;
    }

    &--dark-menu {
      .block-side {
        background: #1d2129;
      }

      .block-header,
      .block-body {
        background: #f2f3f5;
      }

      .block-header {
        border-bottom: 1px solid #e5e6eb;
      }
    }

    &--light {
      .block-side {
        background: #e5e6eb;
      }

      .block-header,
      .block-body {
        background: #f7f8fa;
      }

      .block-header {
        border-bottom: 1px solid #e5e6eb;
      }
    }

    &--dark {
      .block-side {
        background: #000;
      }

      .block-header,
      .block-body {
        background: #232324;
      }

      .block-header {
        border-bottom: 1px solid #2e2e30;
      }
    }
  }

  &__check {
    position: absolute;
    right: 4px;
    bottom: 4px;
    color: rgb(var(--primary-6));
  }
}
</style>
