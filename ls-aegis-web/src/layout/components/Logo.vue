<template>
  <section
    class="system-logo"
    :class="{ collapsed: props.collapsed }"
    @click="toHome"
  >
    <div class="system-logo__inner">
      <img v-if="logo" class="logo" :src="logo" alt="logo" />
      <img v-else class="logo" src="/logo.svg" alt="logo" />
      <span v-if="!props.hideName" class="system-name gi_line_1">{{ title }}</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores'

const props = withDefaults(defineProps<Props>(), {
  collapsed: false,
  hideName: false,
})
const appStore = useAppStore()
const title = computed(() => appStore.getTitleShort())
const logo = computed(() => appStore.getLogo())

/** Props 类型定义 */
interface Props {
  /** 是否折叠状态 */
  collapsed?: boolean
  /** 是否隐藏名称 */
  hideName?: boolean
}
const router = useRouter()
// 跳转首页
const toHome = () => {
  router.push('/')
}
</script>

<style scoped lang="scss">
.system-logo {
  height: 56px;
  padding: 0 12px;
  color: var(--color-text-1);
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 100%;
  box-sizing: border-box;
  cursor: pointer;
  user-select: none;

  &.collapsed {
    padding: 0;
    .system-name {
      display: none;
    }
  }

  &__inner {
    display: inline-flex;
    align-items: center;
    max-width: 100%;
  }

  .logo {
    width: 32px;
    height: 32px;
    border-radius: 6px;
    transition: all 0.2s;
    overflow: hidden;
    flex-shrink: 0;
  }

  .system-name {
    padding-left: 6px;
    white-space: nowrap;
    transition: color 0.3s;
    line-height: 1.5;
    display: inline-flex;
    align-items: center;
    max-width: 9em;
    overflow: hidden;
    text-overflow: ellipsis;

    &:hover {
      color: $color-theme !important;
      cursor: pointer;
    }
  }
}
</style>
