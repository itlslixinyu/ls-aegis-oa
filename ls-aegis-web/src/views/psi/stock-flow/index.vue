<template>
  <GiPageLayout>
    <BaseTable
      row-key="id"
      :data="dataList"
      :columns="columns"
      :loading="loading"

      :pagination="pagination"
      :disabled-tools="['size']"
      @refresh="refresh"
    >
      <template #toolbar-left>
        <a-input-search v-model="queryForm.sourceNo" placeholder="来源单号" allow-clear @search="search" />
        <a-select
          v-model="queryForm.warehouseId"
          placeholder="仓库"
          allow-clear
          allow-search
          :options="warehouseOptions"
          :field-names="{ value: 'value', label: 'label' }"
          style="width: 160px"
          @change="search"
        />
        <a-select
          v-model="queryForm.goodsId"
          placeholder="商品"
          allow-clear
          allow-search
          :options="goodsOptions"
          :field-names="{ value: 'value', label: 'label' }"
          style="width: 180px"
          @change="search"
        />
        <a-select
          v-model="queryForm.flowType"
          placeholder="流水类型"
          allow-clear
          style="width: 140px"
          :options="flowTypeOptions"
          @change="search"
        />
        <a-select
          v-model="queryForm.sourceType"
          placeholder="来源类型"
          allow-clear
          style="width: 140px"
          :options="sourceTypeOptions"
          @change="search"
        />
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:stockFlow:list']" @click="onExport">
          <template #icon><icon-download /></template>
          导出
        </a-button>
      </template>
      <template #flowType="{ record }">
        <GiCellTag :value="record.flowType" :dict="psi_flow_type" />
      </template>
      <template #sourceType="{ record }">
        <GiCellTag :value="record.sourceType" :dict="psi_source_type" />
      </template>
    </BaseTable>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { PsiCol } from '../constants/table-col'
import { type StockFlowPageQuery, exportStockFlow, listGoodsOptions, listStockFlow, listWarehouseOptions } from '@/apis/psi'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'

defineOptions({ name: 'PsiStockFlow' })

const { psi_flow_type, psi_source_type } = useDict('psi_flow_type', 'psi_source_type')
const flowTypeOptions = computed(() => dictSelectOptions(psi_flow_type.value))
const sourceTypeOptions = computed(() => dictSelectOptions(psi_source_type.value))

const queryForm = reactive<Partial<StockFlowPageQuery>>({ sort: ['id,desc'] })
const warehouseOptions = ref<{ label: string, value: string | number }[]>([])
const goodsOptions = ref<{ label: string, value: string | number }[]>([])

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => listStockFlow({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '仓库', dataIndex: 'warehouseName', width: PsiCol.name },
  { title: '商品', dataIndex: 'goodsName' },
  { title: '类型', dataIndex: 'flowType', slotName: 'flowType', width: PsiCol.typeSm },
  { title: '变动前', dataIndex: 'beforeQty', width: PsiCol.qty },
  { title: '变动量', dataIndex: 'changeQty', width: PsiCol.qty },
  { title: '变动后', dataIndex: 'afterQty', width: PsiCol.qty },
  { title: '来源类型', dataIndex: 'sourceType', slotName: 'sourceType', width: PsiCol.type },
  { title: '来源单号', dataIndex: 'sourceNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '确认时间', dataIndex: 'confirmTime', width: PsiCol.date },
]

const onExport = () => useDownload(() => exportStockFlow(queryForm as any))

const reset = () => {
  queryForm.sourceNo = undefined
  queryForm.warehouseId = undefined
  queryForm.goodsId = undefined
  queryForm.flowType = undefined
  queryForm.sourceType = undefined
  search()
}

onMounted(async () => {
  const [wh, goods] = await Promise.all([listWarehouseOptions(), listGoodsOptions()])
  const toOpts = (list: any) => (list || []).map((i: any) => ({ label: i.label, value: String(i.value) }))
  warehouseOptions.value = toOpts((wh as any)?.data || wh)
  goodsOptions.value = toOpts((goods as any)?.data || goods)
})
</script>
