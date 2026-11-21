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
        <a-input-search v-model="queryForm.docNo" placeholder="出库单号" allow-clear @search="search" />
        <a-select v-model="queryForm.docStatus" placeholder="状态" allow-clear style="width: 120px" @change="search">
          <a-option value="DRAFT">草稿</a-option>
          <a-option value="CONFIRMED">已确认</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:stockOut:list']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
        <a-button v-permission="['psi:stockOut:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
      </template>
      <template #outType="{ record }">
        <GiCellTag :value="record.outType" :dict="psi_stock_out_type" />
      </template>
      <template #docStatus="{ record }">
        <a-tag v-if="record.docStatus === 'DRAFT'">草稿</a-tag>
        <a-tag v-else-if="record.docStatus === 'CONFIRMED'" color="green">已确认</a-tag>
        <a-tag v-else-if="record.docStatus === 'VOID'" color="red">已作废</a-tag>
        <a-tag v-else color="red">{{ record.docStatus }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:stockOut:get']" @click="onView(record)">详情</a-link>
          <a-link v-permission="['psi:printTemplate:get']" @click="onPrint(record)">打印</a-link>
          <a-link v-permission="['psi:printTemplate:get']" @click="onExportWord(record)">Word</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:stockOut:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:stockOut:confirm']" @click="onConfirm(record)">确认</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:stockOut:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <EditModal ref="EditModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { PsiCol } from '../constants/table-col'
import EditModal from './EditModal.vue'
import { type StockOutQuery, type StockOutResp, confirmStockOut, deleteStockOut, downloadPrintDocx, exportStockOut, listStockOut, renderPrintTemplate } from '@/apis/psi'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { printHtml } from '@/utils/psi-print'

defineOptions({ name: 'PsiStockOut' })

const { psi_stock_out_type } = useDict('psi_stock_out_type')

const queryForm = reactive<StockOutQuery>({ sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listStockOut({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '出库单号', dataIndex: 'docNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '类型', dataIndex: 'outType', slotName: 'outType', width: PsiCol.type },
  { title: '合同名称', dataIndex: 'contractName' },
  { title: '出库日期', dataIndex: 'outDate', width: PsiCol.date },
  { title: '含税金额', dataIndex: 'taxInclusiveAmount', width: PsiCol.amount },
  { title: '出库成本', dataIndex: 'costAmount', width: PsiCol.amount },
  { title: '状态', dataIndex: 'docStatus', slotName: 'docStatus', width: PsiCol.status },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionL },
]

const reset = () => {
  queryForm.docNo = undefined
  queryForm.docStatus = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const onAdd = () => EditModalRef.value?.onAdd()
const onExport = () => useDownload(() => exportStockOut(queryForm as any))
const onUpdate = (record: StockOutResp) => EditModalRef.value?.onUpdate(record.id)
const onView = (record: StockOutResp) => EditModalRef.value?.onView(record.id)

const onPrint = async (record: StockOutResp) => {
  try {
    const { data } = await renderPrintTemplate({ bizType: 'STOCK_OUT', bizId: record.id })
    printHtml(data.html, data.title || record.docNo)
  } catch (e: any) {
    Message.error(e?.message || '打印失败')
  }
}

const onExportWord = async (record: StockOutResp) => {
  await useDownload(
    () => downloadPrintDocx({ bizType: 'STOCK_OUT', bizId: record.id }),
    `${record.docNo || 'stock-out'}.docx`,
    '.docx',
  )
}

const onConfirm = (record: StockOutResp) => {
  Modal.warning({
    title: '确认出库',
    content: `确认后将按 FIFO 扣减库存并生成应收，单号 ${record.docNo}，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await confirmStockOut(record.id, crypto.randomUUID())
      Message.success('确认成功')
      search()
    },
  })
}

const onDelete = (record: StockOutResp) => {
  Modal.warning({
    title: '提示',
    content: `是否删除草稿 ${record.docNo}？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deleteStockOut(record.id))
      Message.success('删除成功')
    },
  })
}
</script>
