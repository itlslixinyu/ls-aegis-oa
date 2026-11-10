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
        <a-select
          v-model="queryForm.warehouseId"
          placeholder="仓库"
          allow-clear
          allow-search
          :options="warehouseOptions"
          :field-names="{ value: 'value', label: 'label' }"
          style="width: 180px"
          @change="search"
        />
        <a-select
          v-model="queryForm.goodsId"
          placeholder="商品"
          allow-clear
          allow-search
          :options="goodsOptions"
          :field-names="{ value: 'value', label: 'label' }"
          style="width: 200px"
          @change="search"
        />
        <a-checkbox v-model="queryForm.onlyPositive" @change="search">仅有库存</a-checkbox>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:stockQuery:list']" @click="onExport">
          <template #icon><icon-download /></template>
          导出
        </a-button>
      </template>
    </BaseTable>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { PsiCol } from '../constants/table-col'
import { type StockQueryPageQuery, exportStockQuery, listGoodsOptions, listStockQuery, listWarehouseOptions } from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'

defineOptions({ name: 'PsiStockQuery' })

const queryForm = reactive<Partial<StockQueryPageQuery>>({
  onlyPositive: true,
})
const warehouseOptions = ref<{ label: string, value: string | number }[]>([])
const goodsOptions = ref<{ label: string, value: string | number }[]>([])

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => listStockQuery({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '仓库', dataIndex: 'warehouseName', width: PsiCol.name },
  { title: '商品编码', dataIndex: 'goodsCode', minWidth: PsiCol.codeMinSm, ellipsis: false },
  { title: '商品名称', dataIndex: 'goodsName' },
  { title: '规格', dataIndex: 'spec', width: PsiCol.spec },
  { title: '单位', dataIndex: 'unit', width: PsiCol.unit },
  { title: '剩余数量', dataIndex: 'remainQuantity', width: PsiCol.qty },
  { title: '剩余金额', dataIndex: 'remainAmount', width: PsiCol.amount ?? 120 },
]

const onExport = () => useDownload(() => exportStockQuery(queryForm as any))

const reset = () => {
  queryForm.warehouseId = undefined
  queryForm.goodsId = undefined
  queryForm.onlyPositive = true
  search()
}

onMounted(async () => {
  const [wh, goods] = await Promise.all([listWarehouseOptions(), listGoodsOptions()])
  const toOpts = (list: any) => (list || []).map((i: any) => ({ label: i.label, value: String(i.value) }))
  warehouseOptions.value = toOpts((wh as any)?.data || wh)
  goodsOptions.value = toOpts((goods as any)?.data || goods)
})
</script>
