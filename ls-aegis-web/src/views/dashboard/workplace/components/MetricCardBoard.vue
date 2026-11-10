<template>
  <div class="metric-board" :class="{ 'metric-board--dense': dense }" :style="boardStyle">
    <div
      v-for="item in items"
      :key="item.key"
      class="metric-card"
      :style="{ '--accent': item.accent || 'rgb(var(--arcoblue-6))' }"
      @click="onClick(item)"
    >
      <div class="metric-top">
        <span class="metric-label">{{ item.label }}</span>
        <span v-if="item.icon" class="metric-icon">
          <component :is="item.icon" />
        </span>
      </div>
      <div class="metric-value">{{ item.value }}</div>
      <div v-if="item.sub" class="metric-sub">{{ item.sub }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

export interface MetricCardItem {
  key: string
  label: string
  value: string | number
  sub?: string
  path?: string
  accent?: string
  icon?: Component
}

defineOptions({ name: 'MetricCardBoard' })

const props = withDefaults(
  defineProps<{
    items: MetricCardItem[]
    columns?: number
    dense?: boolean
  }>(),
  {
    columns: undefined,
    dense: false,
  },
)

const router = useRouter()

const boardStyle = computed(() => {
  const cols = props.columns ?? Math.min(Math.max(props.items.length, 1), 8)
  return { '--metric-cols': String(cols) }
})

const onClick = (item: MetricCardItem) => {
  if (item.path) {
    router.push(item.path)
  }
}
</script>

<style scoped lang="scss">
.metric-board {
  display: grid;
  grid-template-columns: repeat(var(--metric-cols, 4), minmax(0, 1fr));
  gap: 12px;

  &--dense {
    gap: 6px;

    .metric-card {
      padding: 8px 10px 6px;
      border-radius: 6px;
    }

    .metric-icon {
      width: 22px;
      height: 22px;
      border-radius: 6px;
      font-size: 13px;
    }

    .metric-label {
      font-size: 12px;
    }

    .metric-value {
      margin-top: 4px;
      font-size: 18px;
    }

    .metric-sub {
      margin-top: 2px;
      font-size: 11px;
    }
  }
}

.metric-card {
  background: var(--color-bg-2);
  border-radius: 8px;
  padding: 12px 12px 10px;
  cursor: pointer;
  border: 1px solid var(--color-border-2);
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
  min-width: 0;

  &:hover {
    border-color: var(--accent);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  }
}

.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.metric-label {
  font-size: 13px;
  color: var(--color-text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.metric-icon {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  background: var(--color-fill-2);
  font-size: 15px;
}

.metric-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 650;
  line-height: 1.15;
  color: var(--color-text-1);
}

.metric-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 1400px) {
  .metric-board {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .metric-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
