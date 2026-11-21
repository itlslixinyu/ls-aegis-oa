<template>
  <div class="biz-panel">
    <div class="biz-panel-head">
      <div class="biz-panel-title">
        <span>未结应付</span>
        <a-tag size="small" color="purple">{{ list.length }}</a-tag>
      </div>
      <a-space>
        <a-link @click="load">刷新</a-link>
        <a-link @click="goAll">全部</a-link>
      </a-space>
    </div>
    <div class="biz-panel-body">
      <a-spin :loading="loading" class="biz-spin">
        <a-empty v-if="!list.length" description="暂无未结应付" />
        <div v-else class="doc-list">
          <div v-for="item in list" :key="item.id" class="doc-item" @click="goAll">
            <div class="doc-main">
              <a-tag size="small" color="purple">未结清</a-tag>
              <span class="doc-no" :title="item.sourceNo || item.unitName">{{ item.sourceNo || item.unitName || '-' }}</span>
            </div>
            <div class="doc-meta">
              <span class="doc-unit" :title="item.unitName">{{ item.unitName || '-' }}</span>
              <span class="doc-amount">¥ {{ money(item.unwrittenAmount) }}</span>
            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type OpenItemResp, listPayable } from '@/apis/psi'

defineOptions({ name: 'BizOpenPayable' })

const router = useRouter()
const loading = ref(false)
const list = ref<OpenItemResp[]>([])

const money = (v?: number | string) => {
  const n = Number(v || 0)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const load = async () => {
  loading.value = true
  try {
    const { data } = await listPayable({
      docStatus: 'OPEN',
      page: 1,
      size: 6,
      sort: ['unwrittenAmount,desc'],
    })
    list.value = data?.list || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

const goAll = () => router.push('/psi/payable')
onMounted(load)
</script>

<style scoped lang="scss">
@import './biz-panel.scss';
</style>
