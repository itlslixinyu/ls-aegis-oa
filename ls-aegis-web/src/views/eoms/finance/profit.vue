<template>
  <GiPageLayout>
    <div class="profit-page">
      <a-space wrap style="margin-bottom: 16px">
        <a-input
          v-model="queryForm.customerName"
          placeholder="客户名称"
          allow-clear
          style="width: 160px"
          @press-enter="loadAll"
        />
        <a-input
          v-model="queryForm.orderNo"
          placeholder="工单编号"
          allow-clear
          style="width: 160px"
          @press-enter="loadAll"
        />
        <a-month-picker v-model="queryForm.beginMonth" placeholder="起始月份" style="width: 140px" />
        <a-month-picker v-model="queryForm.endMonth" placeholder="截止月份" style="width: 140px" />
        <a-button v-permission="['eoms:profit:query']" type="primary" @click="loadAll">查询</a-button>
        <a-button @click="reset">重置</a-button>
      </a-space>

      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :xs="12" :sm="8" :md="4">
          <a-card>
            <a-statistic title="总收入" :value="summary.totalIncome || 0" :precision="2" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="4">
          <a-card>
            <a-statistic title="总成本" :value="summary.totalCost || 0" :precision="2" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="4">
          <a-card>
            <a-statistic title="总利润" :value="summary.totalProfit || 0" :precision="2" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="4">
          <a-card>
            <a-statistic title="利润率(%)" :value="(summary.profitRate || 0) * 100" :precision="2" />
          </a-card>
        </a-col>
        <a-col :xs="12" :sm="8" :md="4">
          <a-card>
            <a-statistic title="结算单数" :value="summary.orderCount || 0" />
          </a-card>
        </a-col>
      </a-row>

      <BaseTable
        row-key="orderNo"
        :data="dataList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        :disabled-tools="['size']"
        @refresh="refresh"
      />
    </div>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { EomsCol } from '../constants/table-col'
import { type ProfitSummaryResp, getProfitList, getProfitSummary } from '@/apis/eoms'
import { useTable } from '@/hooks'

defineOptions({ name: 'EomsFinanceProfit' })

const queryForm = reactive<Record<string, any>>({})
const summary = ref<ProfitSummaryResp>({})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => getProfitList({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '工单编号', dataIndex: 'orderNo', minWidth: EomsCol.codeMin, ellipsis: false },
  { title: '客户', dataIndex: 'customerName', width: EomsCol.name },
  { title: '收入', dataIndex: 'income', width: 120 },
  { title: '成本', dataIndex: 'cost', width: 120 },
  { title: '利润', dataIndex: 'profit', width: 120 },
  { title: '月份', dataIndex: 'month', width: 100 },
]

const loadSummary = async () => {
  const { data } = await getProfitSummary(queryForm)
  summary.value = data || {}
}

const loadAll = async () => {
  await Promise.all([search(), loadSummary()])
}

const reset = () => {
  Object.keys(queryForm).forEach((k) => {
    queryForm[k] = undefined
  })
  loadAll()
}

onMounted(loadSummary)
</script>
