<template>
  <div class="biz-panel summary-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>近 30 天出入库</span>
      </div>
      <a-link @click="goFlow">库存流水</a-link>
    </div>
    <div class="biz-panel-body summary-body">
      <div class="stat-grid">
        <div class="stat-item" @click="goIn">
          <div class="summary-label">入库金额 · {{ summary.last30InCount ?? 0 }} 笔</div>
          <div class="stat-value amount">¥ {{ money(summary.last30InAmount) }}</div>
        </div>
        <div class="stat-item" @click="goIn">
          <div class="summary-label">入库数量</div>
          <div class="stat-value">{{ qty(summary.last30InQty) }}</div>
        </div>
        <div class="stat-item" @click="goOut">
          <div class="summary-label">出库金额 · {{ summary.last30OutCount ?? 0 }} 笔</div>
          <div class="stat-value amount">¥ {{ money(summary.last30OutAmount) }}</div>
        </div>
        <div class="stat-item" @click="goOut">
          <div class="summary-label">出库数量</div>
          <div class="stat-value">{{ qty(summary.last30OutQty) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PsiWorkbenchSummary } from '@/apis/psi'

defineOptions({ name: 'BizLast30Stock' })

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
const goFlow = () => router.push('/psi/stock-flow')
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
@import './biz-summary.scss';
</style>
