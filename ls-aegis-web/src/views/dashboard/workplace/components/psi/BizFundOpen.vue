<template>
  <div class="biz-panel summary-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>未结资金</span>
      </div>
      <a-space :size="8">
        <a-link @click="goAr">应收</a-link>
        <a-link @click="goAp">应付</a-link>
      </a-space>
    </div>
    <div class="biz-panel-body summary-body">
      <div class="stat-grid">
        <div class="stat-item" @click="goAr">
          <div class="summary-label">应收金额</div>
          <div class="stat-value amount">¥ {{ money(summary.openReceivableAmount) }}</div>
        </div>
        <div class="stat-item" @click="goAr">
          <div class="summary-label">应收笔数</div>
          <div class="stat-value">{{ summary.openReceivableCount ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goAp">
          <div class="summary-label">应付金额</div>
          <div class="stat-value amount">¥ {{ money(summary.openPayableAmount) }}</div>
        </div>
        <div class="stat-item" @click="goAp">
          <div class="summary-label">应付笔数</div>
          <div class="stat-value">{{ summary.openPayableCount ?? 0 }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PsiWorkbenchSummary } from '@/apis/psi'

defineOptions({ name: 'BizFundOpen' })

defineProps<{
  summary: Partial<PsiWorkbenchSummary>
}>()

const router = useRouter()
const money = (v?: number | string) => {
  const n = Number(v || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}
const goAr = () => router.push('/psi/receivable')
const goAp = () => router.push('/psi/payable')
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
@import './biz-summary.scss';
</style>
