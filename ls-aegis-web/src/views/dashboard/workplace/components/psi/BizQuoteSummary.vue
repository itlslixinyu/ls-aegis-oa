<template>
  <div v-if="visible" class="biz-panel summary-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>报价汇总</span>
      </div>
      <a-link @click="goAll">全部</a-link>
    </div>
    <div class="biz-panel-body summary-body">
      <div class="stat-grid">
        <div class="stat-item" @click="goAll">
          <div class="summary-label">草稿</div>
          <div class="stat-value">{{ summary.draftCount ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goAll">
          <div class="summary-label">本月笔数</div>
          <div class="stat-value">{{ summary.monthCount ?? 0 }}</div>
        </div>
        <div class="stat-item" @click="goAll">
          <div class="summary-label">本月金额</div>
          <div class="stat-value amount">¥ {{ money(summary.monthAmount) }}</div>
        </div>
        <div class="stat-item" @click="goAll">
          <div class="summary-label">笔均金额</div>
          <div class="stat-value amount">¥ {{ money(avgAmount) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type QuoteSummary, getQuoteSummary } from '@/apis/quote'
import has from '@/utils/has'

defineOptions({ name: 'BizQuoteSummary' })

const emit = defineEmits<{
  loaded: [summary: QuoteSummary | null]
}>()

const router = useRouter()
const visible = ref(false)
const summary = ref<Partial<QuoteSummary>>({})

const money = (v?: number | string) => {
  const n = Number(v || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const avgAmount = computed(() => {
  const count = Number(summary.value.monthCount || 0)
  const amount = Number(summary.value.monthAmount || 0)
  if (!count || !Number.isFinite(amount)) {
    return 0
  }
  return amount / count
})

const goAll = () => router.push('/quote/index')

const load = async () => {
  if (!has.hasPermOr(['quote:list', 'quote:query'])) {
    visible.value = false
    emit('loaded', null)
    return
  }
  try {
    const { data } = await getQuoteSummary()
    summary.value = data || {}
    visible.value = true
    emit('loaded', summary.value as QuoteSummary)
  } catch {
    visible.value = false
    emit('loaded', null)
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
@import './biz-summary.scss';
</style>
