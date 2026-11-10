<template>
  <div class="section-panel module-panel" :class="{ 'section-panel--dense': dense }">
    <div class="section-head">
      <span class="section-title">业务模块</span>
      <div class="section-actions">
        <a-link @click="load">刷新</a-link>
      </div>
    </div>
    <a-spin :loading="loading" class="section-spin">
      <div class="panel-grid">
        <!-- 第 1 行：明细 / 待办 -->
        <div class="cell"><BizOpenReceivable /></div>
        <div class="cell"><BizOpenPayable /></div>
        <div class="cell"><BizTodoList :list="todoList" @refresh="load" /></div>
        <div class="cell"><BizStockAlert :list="alertList" @refresh="load" /></div>
        <!-- 第 2 行：进销 / 报修 / 回款 -->
        <div class="cell"><BizMonthTrade :summary="summary" /></div>
        <div class="cell"><BizLast30Stock :summary="summary" /></div>
        <div class="cell"><BizRepairOps :overview="eomsOverview" /></div>
        <div class="cell"><BizPendingReceiptSummary :summary="summary" /></div>
        <!-- 第 3 行：资金 / 域汇总 -->
        <div class="cell"><BizFundOpen :summary="summary" /></div>
        <div class="cell"><BizEomsSummary :overview="eomsOverview" /></div>
        <div class="cell"><BizSignSummary @loaded="onSignLoaded" /></div>
        <div class="cell"><BizQuoteSummary @loaded="onQuoteLoaded" /></div>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { type PsiWorkbenchSummary, getPsiWorkbenchSummary } from '@/apis/psi'
import { getOverview } from '@/apis/eoms/dashboard'
import has from '@/utils/has'
import type { DashboardStats } from '@/apis/sign/dashboard'
import type { QuoteSummary } from '@/apis/quote'
import BizEomsSummary from './psi/BizEomsSummary.vue'
import BizFundOpen from './psi/BizFundOpen.vue'
import BizLast30Stock from './psi/BizLast30Stock.vue'
import BizMonthTrade from './psi/BizMonthTrade.vue'
import BizOpenPayable from './psi/BizOpenPayable.vue'
import BizOpenReceivable from './psi/BizOpenReceivable.vue'
import BizPendingReceiptSummary from './psi/BizPendingReceiptSummary.vue'
import BizQuoteSummary from './psi/BizQuoteSummary.vue'
import BizRepairOps from './psi/BizRepairOps.vue'
import BizSignSummary from './psi/BizSignSummary.vue'
import BizStockAlert from './psi/BizStockAlert.vue'
import BizTodoList from './psi/BizTodoList.vue'

defineOptions({ name: 'PsiBusinessPanel' })

withDefaults(
  defineProps<{
    dense?: boolean
  }>(),
  { dense: false },
)

const emit = defineEmits<{
  loaded: [summary: Partial<PsiWorkbenchSummary> | null]
  eomsLoaded: [overview: Record<string, any> | null]
  signLoaded: [stats: DashboardStats | null]
  quoteLoaded: [summary: QuoteSummary | null]
}>()

const loading = ref(false)
const summary = ref<Partial<PsiWorkbenchSummary>>({})
const eomsOverview = ref<Record<string, any> | null>(null)

const todoList = computed(() => (summary.value.recentTodos || []).slice(0, 3))
const alertList = computed(() => (summary.value.stockAlerts || []).slice(0, 3))

const loadPsi = async () => {
  try {
    const { data } = await getPsiWorkbenchSummary()
    summary.value = data || {}
    emit('loaded', summary.value)
  } catch {
    summary.value = {}
    emit('loaded', null)
  }
}

const loadEoms = async () => {
  if (!has.hasPermOr(['eoms:dashboard:list', 'eoms:order:query', 'eoms:repair:list'])) {
    eomsOverview.value = null
    emit('eomsLoaded', null)
    return
  }
  try {
    const { data } = await getOverview()
    eomsOverview.value = data || {}
  } catch {
    eomsOverview.value = null
  }
  emit('eomsLoaded', eomsOverview.value)
}

// 同一工作台的报修/运维卡片共用一次概览请求，刷新也使用同一快照。
const load = async () => {
  if (loading.value) return
  loading.value = true
  try {
    await Promise.all([loadPsi(), loadEoms()])
  } finally {
    loading.value = false
  }
}

const onSignLoaded = (stats: DashboardStats | null) => emit('signLoaded', stats)
const onQuoteLoaded = (summary: QuoteSummary | null) => emit('quoteLoaded', summary)

defineExpose({ load })

onMounted(load)
</script>

<style scoped lang="scss">
@import './section-panel.scss';

.module-panel {
  height: 100%;
  min-height: 0;
}

.section-spin {
  flex: 1;
  min-height: 0;

  :deep(.arco-spin-container) {
    height: 100%;
    min-height: 0;
  }
}

.panel-grid {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.cell {
  min-height: 0;
  overflow: hidden;

  > :deep(.biz-panel),
  > :deep(.summary-panel) {
    height: 100%;
  }
}

.section-panel--dense {
  gap: 4px;

  .section-head {
    padding: 0;
  }

  .section-title {
    font-size: 13px;
  }

  :deep(.biz-panel-head) {
    padding: 4px 8px;
  }

  :deep(.biz-panel-title) {
    font-size: 12px;
  }

  :deep(.biz-panel-body) {
    padding: 4px 6px;

    .arco-empty {
      min-height: 0;
      transform: scale(0.85);
    }
  }

  :deep(.summary-tips) {
    display: none;
  }

  :deep(.summary-value) {
    margin-top: 4px;
    font-size: 18px;
  }

  :deep(.span-item) {
    padding: 6px 8px;
  }

  :deep(.span-value) {
    margin-top: 4px;
    font-size: 15px;
  }

  :deep(.doc-item) {
    padding: 4px 6px;
  }

  :deep(.doc-meta) {
    margin-top: 2px;
  }

  :deep(.doc-no) {
    font-size: 12px;
  }
}

@media (max-width: 1400px) {
  .panel-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: none;
    grid-auto-rows: minmax(0, 1fr);
  }
}

@media (max-width: 1200px) {
  .panel-grid {
    height: auto;
    grid-auto-rows: minmax(140px, auto);
  }
}

@media (max-width: 992px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
