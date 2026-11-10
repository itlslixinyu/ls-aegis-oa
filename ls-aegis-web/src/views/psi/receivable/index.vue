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
          v-model="queryForm.docStatus"
          placeholder="状态"
          allow-clear
          style="width: 120px"
          :options="openStatusOptions"
          @change="search"
        />
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:receivable:list']" @click="onExport">
          <template #icon><icon-download /></template>
          导出
        </a-button>
      </template>
      <template #sourceType="{ record }">
        <GiCellTag :value="record.sourceType" :dict="psi_source_type" />
      </template>
      <template #docStatus="{ record }">
        <GiCellTag :value="record.docStatus" :dict="psi_open_status" />
      </template>
    </BaseTable>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { PsiCol } from '../constants/table-col'
import { type OpenItemQuery, exportReceivable, listReceivable } from '@/apis/psi'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'

defineOptions({ name: 'PsiReceivable' })

const { psi_source_type, psi_open_status } = useDict('psi_source_type', 'psi_open_status')
const openStatusOptions = computed(() => dictSelectOptions(psi_open_status.value))

const queryForm = reactive<OpenItemQuery>({ docStatus: 'OPEN', sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => listReceivable({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '客户', dataIndex: 'unitName' },
  { title: '来源类型', dataIndex: 'sourceType', slotName: 'sourceType', width: PsiCol.type },
  { title: '来源单号', dataIndex: 'sourceNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '含税金额', dataIndex: 'taxInclusiveAmount', width: PsiCol.amount },
  { title: '已核销', dataIndex: 'writtenOffAmount', width: PsiCol.amount },
  { title: '未核销', dataIndex: 'unwrittenAmount', width: PsiCol.amount },
  { title: '状态', dataIndex: 'docStatus', slotName: 'docStatus', width: PsiCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: PsiCol.date },
]

const onExport = () => useDownload(() => exportReceivable(queryForm as any))

const reset = () => {
  queryForm.sourceNo = undefined
  queryForm.docStatus = 'OPEN'
  search()
}
</script>
