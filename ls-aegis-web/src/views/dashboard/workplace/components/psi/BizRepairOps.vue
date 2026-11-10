<template>
  <div v-if="visible" class="biz-panel summary-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>报修工单</span>
      </div>
      <a-space :size="8">
        <a-link @click="goApply">报修</a-link>
        <a-link @click="goOrders">工单</a-link>
      </a-space>
    </div>
    <div class="biz-panel-body summary-body">
      <div class="stat-grid">
        <div class="stat-item" @click="goApply">
          <div class="summary-label">报修未处理</div>
          <div class="stat-value">{{ overview.waitApply ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goOrders">
          <div class="summary-label">待接单</div>
          <div class="stat-value">{{ overview.waitAccept ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goOrders">
          <div class="summary-label">进行中</div>
          <div class="stat-value">{{ overview.processing ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goSettlements">
          <div class="summary-label">待结算</div>
          <div class="stat-value">{{ overview.waitSettlement ?? 0 }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import has from '@/utils/has'

defineOptions({ name: 'BizRepairOps' })

const props = defineProps<{ overview: Record<string, any> | null }>()

const router = useRouter()
const visible = computed(() => props.overview !== null && has.hasPermOr(['eoms:dashboard:list', 'eoms:order:query', 'eoms:repair:list']))
const overview = computed(() => props.overview || {})

const goApply = () => router.push('/eoms/repair/apply')
const goOrders = () => router.push('/eoms/repair/order')
const goSettlements = () => router.push('/eoms/repair/settlement')


</script>

<style scoped lang="scss">
@import './biz-panel.scss';
@import './biz-summary.scss';
</style>
