<template>
  <div v-if="data && data.length" class="gi-cell-tags">
    <a-space :size="4" wrap>
      <a-tag v-for="(item, index) in visibleData" :key="index" size="small">
        {{ item }}
      </a-tag>
      <a-popover v-if="overflowCount > 0" :content-style="{ maxWidth: '300px', padding: '8px 12px' }">
        <a-tag color="arcoblue" size="small">+{{ overflowCount }}</a-tag>
        <template #content>
          <a-space wrap>
            <a-tag v-for="tag in data.slice(max)" :key="tag" size="small">
              {{ tag }}
            </a-tag>
          </a-space>
        </template>
      </a-popover>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'GiCellTags' })

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  max: 2,
})

interface Props {
  data: string[]
  /** 超出后折叠为 +N，避免 overflow-list 测宽误判 */
  max?: number
}

const visibleData = computed(() => props.data.slice(0, props.max))
const overflowCount = computed(() => Math.max(0, props.data.length - props.max))
</script>

<style scoped lang="scss">
.gi-cell-tags {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
