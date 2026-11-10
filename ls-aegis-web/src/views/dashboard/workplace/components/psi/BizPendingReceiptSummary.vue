<template>
  <div class="biz-panel summary-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>待确认回款</span>
      </div>
      <a-link @click="goAll">回款单</a-link>
    </div>
    <div class="biz-panel-body summary-body">
      <div class="stat-grid">
        <div class="stat-item" @click="goAll">
          <div class="summary-label">待确认金额</div>
          <div class="stat-value amount">¥ {{ money(summary.pendingReceiptAmount) }}</div>
        </div>
        <div class="stat-item" @click="goAll">
          <div class="summary-label">待确认笔数</div>
          <div class="stat-value">{{ summary.pendingReceiptCount ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goAll">
          <div class="summary-label">平均金额</div>
          <div class="stat-value amount">¥ {{ money(avgAmount) }}</div>
        </div>
        <div class="stat-item" @click="goAr">
          <div class="summary-label">未结应收笔数</div>
          <div class="stat-value">{{ summary.openReceivableCount ?? 0 }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PsiWorkbenchSummary } from '@/apis/psi'

defineOptions({ name: 'BizPendingReceiptSummary' })

const props = defineProps<{
  summary: Partial<PsiWorkbenchSummary>
}>()

const router = useRouter()

const money = (v?: number | string) => {
  const n = Number(v || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const avgAmount = computed(() => {
  const count = Number(props.summary.pendingReceiptCount || 0)
  const amount = Number(props.summary.pendingReceiptAmount || 0)
  if (!count || !Number.isFinite(amount)) {
    return 0
  }
  return amount / count
})

const goAll = () => router.push('/psi/receipt')
const goAr = () => router.push('/psi/receivable')
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
@import './biz-summary.scss';
</style>
