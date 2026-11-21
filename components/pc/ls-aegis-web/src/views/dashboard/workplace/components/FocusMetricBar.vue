<template>
  <div v-if="items.length" class="focus-bar" :class="{ 'focus-bar--dense': dense }">
    <div class="focus-head">
      <span class="focus-title">今日关注</span>
      <span v-if="!dense" class="focus-hint">需处理事项，点击进入对应业务</span>
    </div>
    <MetricCardBoard :items="items" :columns="Math.min(items.length, 8)" :dense="dense" />
  </div>
</template>

<script setup lang="ts">
import MetricCardBoard, { type MetricCardItem } from './MetricCardBoard.vue'

defineOptions({ name: 'FocusMetricBar' })

withDefaults(
  defineProps<{
    items: MetricCardItem[]
    dense?: boolean
  }>(),
  { dense: false },
)
</script>

<style scoped lang="scss">
.focus-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;

  &--dense {
    gap: 4px;

    .focus-head {
      padding: 0;
    }

    .focus-title {
      font-size: 13px;
    }
  }
}

.focus-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 0 2px;
}

.focus-title {
  font-size: 15px;
  font-weight: 650;
  color: var(--color-text-1);
}

.focus-hint {
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
