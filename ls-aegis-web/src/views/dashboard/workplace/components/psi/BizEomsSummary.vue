<template>
  <div v-if="visible" class="biz-panel summary-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>运维汇总</span>
      </div>
      <a-link @click="goDashboard">看板</a-link>
    </div>
    <div class="biz-panel-body summary-body">
      <div class="stat-grid">
        <div class="stat-item" @click="goOrders">
          <div class="summary-label">本月工单</div>
          <div class="stat-value">{{ overview.thisMonthOrder ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goSettlements">
          <div class="summary-label">本月收入</div>
          <div class="stat-value amount">¥ {{ money(overview.monthIncome) }}</div>
        </div>
        <div class="stat-item" @click="goOrders">
          <div class="summary-label">工单总量</div>
          <div class="stat-value">{{ overview.orderTotal ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goDashboard">
          <div class="summary-label">客户数</div>
          <div class="stat-value">{{ overview.customerTotal ?? 0 }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import has from '@/utils/has'

defineOptions({ name: 'BizEomsSummary' })

const props = defineProps<{ overview: Record<string, any> | null }>()

const router = useRouter()
const visible = computed(() => props.overview !== null && has.hasPermOr(['eoms:dashboard:list']))
const overview = computed(() => props.overview || {})

const money = (v?: number | string) => {
  const n = Number(v || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const goDashboard = () => router.push('/eoms/dashboard')
const goOrders = () => router.push('/eoms/repair/order')
const goSettlements = () => router.push('/eoms/repair/settlement')


</script>

<style scoped lang="scss">
@import './biz-panel.scss';
@import './biz-summary.scss';
</style>
