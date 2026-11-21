<template>
  <GiPageLayout>
    <div class="dashboard-page">
      <!-- C9：仪表盘只留 KPI；工单趋势 / 物料 Top 在统计分析页 -->
      <div class="toolbar">
        <span class="title">维修概览</span>
        <a-space>
          <a-button type="primary" :loading="loading" @click="loadData">
            <template #icon><icon-refresh /></template>
            刷新
          </a-button>
          <a-button v-permission="['eoms:dashboard:list']" @click="onExport">
            <template #icon><icon-download /></template>
            导出
          </a-button>
        </a-space>
      </div>

      <a-row :gutter="16">
        <a-col :xs="12" :sm="8" :md="6" :lg="4">
          <a-card>
            <a-statistic title="客户总数" :value="overview.customerTotal || 0" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="6" :lg="4">
          <a-card>
            <a-statistic title="本月新增客户" :value="overview.thisMonthCustomer || 0" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="6" :lg="4">
          <a-card>
            <a-statistic title="工单总数" :value="overview.orderTotal || 0" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="6" :lg="4">
          <a-card>
            <a-statistic title="进行中工单" :value="overview.processingOrder || 0" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="6" :lg="4">
          <a-card>
            <a-statistic title="本月收入" :value="overview.thisMonthIncome || 0" :precision="2" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="6" :lg="4">
          <a-card>
            <a-statistic title="本月利润" :value="overview.thisMonthProfit || 0" :precision="2" />
          </a-card>
        </a-col>
      </a-row>
    </div>
  </GiPageLayout>
</template>

<script setup lang="ts">
import { exportDashboardReport, getOverview } from '@/apis/eoms'
import { useDownload } from '@/hooks'

defineOptions({ name: 'EomsDashboard' })

const loading = ref(false)
const overview = ref<Record<string, any>>({})

const loadData = async () => {
  loading.value = true
  try {
    const overviewRes = await getOverview().catch(() => ({ data: {} }))
    overview.value = overviewRes.data || {}
  } finally {
    loading.value = false
  }
}

const onExport = () => useDownload(() => exportDashboardReport())

onMounted(loadData)
</script>

<style scoped>
.dashboard-page {
  padding: 12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
</style>
