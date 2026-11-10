<template>
  <div class="gi_page workplace">
    <div class="welcome-wrap">
      <Welcome dense />
    </div>
    <FocusMetricBar v-if="focusItems.length" :items="focusItems" class="focus" dense />
    <PsiBusinessPanel
      class="modules"
      dense
      @loaded="onPsiLoaded"
      @eoms-loaded="onEomsLoaded"
      @sign-loaded="onSignLoaded"
      @quote-loaded="onQuoteLoaded"
    />
  </div>
</template>

<script setup lang="ts">
import {
  IconCalendar,
  IconCheckCircle,
  IconExclamationCircle,
  IconStorage,
  IconSubscribed,
  IconThunderbolt,
  IconTrophy,
} from '@arco-design/web-vue/es/icon'
import type { PsiWorkbenchSummary } from '@/apis/psi'
import FocusMetricBar from './components/FocusMetricBar.vue'
import type { MetricCardItem } from './components/MetricCardBoard.vue'
import PsiBusinessPanel from './components/PsiBusinessPanel.vue'
import Welcome from './components/Welcome.vue'

defineOptions({ name: 'Workplace' })

const psiSummary = ref<Partial<PsiWorkbenchSummary> | null>(null)
const eomsOverview = ref<Record<string, any> | null>(null)

const money = (v?: number | string) => {
  const n = Number(v || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const onPsiLoaded = (summary: Partial<PsiWorkbenchSummary> | null) => {
  psiSummary.value = summary
}

const onEomsLoaded = (overview: Record<string, any> | null) => {
  eomsOverview.value = overview
}

const onSignLoaded = (_stats: unknown) => {
  // 签章无动作型待办
}

const onQuoteLoaded = (_summary: unknown) => {
  // 报价不进关注条
}

const focusItems = computed<MetricCardItem[]>(() => {
  const items: MetricCardItem[] = []
  const s = psiSummary.value
  if (s) {
    items.push(
      {
        key: 'receipt',
        label: '待确认回款',
        value: String(s.pendingReceiptCount ?? 0),
        sub: `¥ ${money(s.pendingReceiptAmount)}`,
        path: '/psi/receipt',
        accent: 'rgb(var(--cyan-6))',
        icon: IconCalendar,
      },
      {
        key: 'todo',
        label: '业务待办',
        value: String(s.openTodoCount ?? 0),
        sub: '冲红 / 核销 / 退款',
        path: '/psi/todo',
        accent: 'rgb(var(--purple-6))',
        icon: IconExclamationCircle,
      },
      {
        key: 'alert',
        label: '库存预警',
        value: String((s.stockAlerts || []).length),
        sub: '上下限异常 SKU',
        path: '/psi/goods',
        accent: 'rgb(var(--red-6))',
        icon: IconStorage,
      },
    )
  }

  const e = eomsOverview.value
  if (e) {
    items.push(
      {
        key: 'waitApply',
        label: '报修未处理',
        value: String(e.waitApply ?? 0),
        sub: '报修申请',
        path: '/eoms/repair/apply',
        accent: 'rgb(var(--red-6))',
        icon: IconExclamationCircle,
      },
      {
        key: 'waitAccept',
        label: '待接单',
        value: String(e.waitAccept ?? 0),
        sub: '报修工单',
        path: '/eoms/repair/order',
        accent: 'rgb(var(--orangered-6))',
        icon: IconThunderbolt,
      },
      {
        key: 'processing',
        label: '进行中',
        value: String(e.processing ?? 0),
        sub: '已接单/维修中',
        path: '/eoms/repair/order',
        accent: 'rgb(var(--cyan-6))',
        icon: IconCalendar,
      },
      {
        key: 'waitSettlement',
        label: '待结算',
        value: String(e.waitSettlement ?? 0),
        sub: '维修结算',
        path: '/eoms/repair/settlement',
        accent: 'rgb(var(--arcoblue-6))',
        icon: IconCheckCircle,
      },
    )
  }

  if (s) {
    items.push(
      {
        key: 'ar',
        label: '未结应收',
        value: String(s.openReceivableCount ?? 0),
        sub: `¥ ${money(s.openReceivableAmount)}`,
        path: '/psi/receivable',
        accent: 'rgb(var(--arcoblue-6))',
        icon: IconTrophy,
      },
      {
        key: 'ap',
        label: '未结应付',
        value: String(s.openPayableCount ?? 0),
        sub: `¥ ${money(s.openPayableAmount)}`,
        path: '/psi/payable',
        accent: 'rgb(var(--purple-6))',
        icon: IconSubscribed,
      },
    )
  }

  return items.slice(0, 8)
})
</script>

<style scoped lang="scss">
.workplace {
  display: flex;
  flex-direction: column;
  gap: 6px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.welcome-wrap {
  flex-shrink: 0;
  background-color: var(--color-bg-2);
  border-radius: 4px;
  overflow: hidden;
}

.focus {
  flex-shrink: 0;
}

.modules {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 1200px) {
  .workplace {
    height: auto;
    overflow-y: auto;
  }

  .modules {
    overflow: visible;
  }
}
</style>
