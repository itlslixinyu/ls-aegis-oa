<template>
  <div class="biz-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>待匹配银行流水</span>
        <a-tag size="small" color="orangered">{{ list.length }}</a-tag>
      </div>
      <a-space>
        <a-link @click="load">刷新</a-link>
        <a-link @click="goAll">全部</a-link>
      </a-space>
    </div>
    <div class="biz-panel-body">
      <a-spin :loading="loading" class="biz-spin">
        <a-empty v-if="!list.length" description="暂无待匹配流水" />
        <div v-else class="doc-list">
          <div v-for="item in list" :key="item.id" class="doc-item" @click="goAll">
            <div class="doc-main">
              <a-tag size="small" color="orangered">未匹配</a-tag>
              <span class="doc-no" :title="item.counterparty || item.summary">{{ item.counterparty || item.summary || item.bankSeqNo || '-' }}</span>
            </div>
            <div class="doc-meta">
              <span>{{ item.stmtDate || '-' }}</span>
              <span class="doc-amount">¥ {{ money(item.amount) }}</span>
            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type BankStatementResp, listBank } from '@/apis/psi'

defineOptions({ name: 'BizUnmatchedBank' })

const router = useRouter()
const loading = ref(false)
const list = ref<BankStatementResp[]>([])

const money = (v?: number | string) => {
  const n = Number(v || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const load = async () => {
  loading.value = true
  try {
    const { data } = await listBank({
      matchStatus: 'UNMATCHED',
      page: 1,
      size: 6,
      sort: ['id,desc'],
    })
    list.value = data?.list || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

const goAll = () => router.push('/psi/bank')
onMounted(load)
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
</style>
