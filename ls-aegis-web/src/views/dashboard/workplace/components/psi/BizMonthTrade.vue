<template>
  <div class="biz-panel summary-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>本月进销</span>
      </div>
      <a-space :size="8">
        <a-link @click="goOut">出库单</a-link>
        <a-link @click="goIn">入库单</a-link>
      </a-space>
    </div>
    <div class="biz-panel-body summary-body">
      <div class="stat-grid">
        <div class="stat-item" @click="goOut">
          <div class="summary-label">销售金额 · {{ summary.monthSaleCount ?? 0 }} 笔</div>
          <div class="stat-value amount">¥ {{ money(summary.monthSaleAmount) }}</div>
        </div>
        <div class="stat-item" @click="goOut">
          <div class="summary-label">销售数量</div>
          <div class="stat-value">{{ qty(summary.monthSaleQty) }}</div>
        </div>
        <div class="stat-item" @click="goIn">
          <div class="summary-label">采购金额 · {{ summary.monthPurchaseCount ?? 0 }} 笔</div>
          <div class="stat-value amount">¥ {{ money(summary.monthPurchaseAmount) }}</div>
        </div>
        <div class="stat-item" @click="goIn">
          <div class="summary-label">采购数量</div>
          <div class="stat-value">{{ qty(summary.monthPurchaseQty) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PsiWorkbenchSummary } from '@/apis/psi'

defineOptions({ name: 'BizMonthTrade' })

defineProps<{
  summary: Partial<PsiWorkbenchSummary>
}>()

const router = useRouter()

const money = (v?: number | string) => {
  const n = Number(v || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const qty = (v?: number | string) => {
  const n = Number(v || 0)
  if (!Number.isFinite(n)) {
    return '0'
  }
  return String(parseFloat(n.toFixed(4)))
}

const goIn = () => router.push('/psi/stock-in')
const goOut = () => router.push('/psi/stock-out')
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
@import './biz-summary.scss';
</style>
