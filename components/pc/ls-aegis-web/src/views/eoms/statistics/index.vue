<template>
  <GiPageLayout>
    <div class="statistics-page">
      <div class="toolbar">
        <span class="title">EOMS 统计分析</span>
        <a-space>
          <a-button v-permission="['eoms:statistics:list']" type="primary" :loading="exporting" @click="onExport">
            <template #icon><icon-download /></template>
            导出 Excel
          </a-button>
          <a-button @click="loadData">
            <template #icon><icon-refresh /></template>
            刷新
          </a-button>
        </a-space>
      </div>

      <a-row :gutter="16">
        <a-col :xs="24" :md="12" style="margin-bottom: 16px">
          <a-card title="工单趋势（近7天）" :loading="loading">
            <a-table
              :data="orderTrend"
              :pagination="false"
              size="small"
              :bordered="{ cell: true }"
              :columns="[
                { title: '日期', dataIndex: 'date' },
                { title: '工单数', dataIndex: 'count' },
              ]"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" style="margin-bottom: 16px">
          <a-card title="利润趋势（近6月）" :loading="loading">
            <a-table
              :data="profitTrend"
              :pagination="false"
              size="small"
              :bordered="{ cell: true }"
              :columns="[
                { title: '月份', dataIndex: 'month' },
                { title: '收入', dataIndex: 'income' },
                { title: '成本', dataIndex: 'cost' },
                { title: '利润', dataIndex: 'profit' },
              ]"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" style="margin-bottom: 16px">
          <a-card title="物料消耗 Top10" :loading="loading">
            <a-table
              :data="materialTop"
              :pagination="false"
              size="small"
              :bordered="{ cell: true }"
              :columns="[
                { title: '物料', dataIndex: 'materialName' },
                { title: '消耗量', dataIndex: 'quantity' },
              ]"
            />
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" style="margin-bottom: 16px">
          <a-card title="客户分布" :loading="loading">
            <a-table
              :data="customerDist"
              :pagination="false"
              size="small"
              :bordered="{ cell: true }"
              :columns="[
                { title: '类型', dataIndex: 'name' },
                { title: '数量', dataIndex: 'value' },
              ]"
            />
          </a-card>
        </a-col>
        <a-col :span="24">
          <a-card title="工程师绩效 Top10" :loading="loading">
            <a-table
              :data="engineerPerf"
              :pagination="false"
              size="small"
              :bordered="{ cell: true }"
              :columns="[
                { title: '工程师', dataIndex: 'engineerName' },
                { title: '完成工单', dataIndex: 'orderCount' },
                { title: '评分', dataIndex: 'score' },
              ]"
            />
          </a-card>
        </a-col>
      </a-row>
    </div>
  </GiPageLayout>
</template>

<script setup lang="ts">
import {
  exportStatisticsReport,
  getCustomerDistribution,
  getEngineerPerformance,
  getMaterialTop,
  getOrderTrend,
  getProfitTrend,
} from '@/apis/eoms'
import { useDownload } from '@/hooks'

defineOptions({ name: 'EomsStatistics' })

const loading = ref(false)
const exporting = ref(false)
const orderTrend = ref<any[]>([])
const profitTrend = ref<any[]>([])
const materialTop = ref<any[]>([])
const customerDist = ref<any[]>([])
const engineerPerf = ref<any[]>([])

const normalizeList = (raw: any): any[] => {
  if (Array.isArray(raw)) return raw
  if (Array.isArray(raw?.list)) return raw.list
  if (Array.isArray(raw?.rows)) return raw.rows
  return []
}

const loadData = async () => {
  loading.value = true
  try {
    const [o, p, m, c, e] = await Promise.all([
      getOrderTrend({ days: 7 }),
      getProfitTrend({ months: 6 }),
      getMaterialTop({ limit: 10 }),
      getCustomerDistribution(),
      getEngineerPerformance({ limit: 10 }),
    ])
    orderTrend.value = normalizeList(o.data)
    profitTrend.value = normalizeList(p.data)
    materialTop.value = normalizeList(m.data)
    customerDist.value = normalizeList(c.data).map((item: any) => ({
      name: item.name || item.customerType || item.label,
      value: item.value ?? item.count ?? item.total,
    }))
    engineerPerf.value = normalizeList(e.data)
  } finally {
    loading.value = false
  }
}

const onExport = async () => {
  exporting.value = true
  try {
    await useDownload(() => exportStatisticsReport())
  } finally {
    exporting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.statistics-page {
  padding: 12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
</style>
