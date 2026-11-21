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
        <a-input-search v-model="queryForm.invoiceNo" placeholder="发票号" allow-clear @search="search" />
        <a-input-search v-model="queryForm.docNo" placeholder="单据号" allow-clear @search="search" />
        <a-select v-model="queryForm.docStatus" placeholder="状态" allow-clear style="width: 120px" @change="search">
          <a-option value="DRAFT">草稿</a-option>
          <a-option value="CONFIRMED">已确认</a-option>
          <a-option value="VOID">已作废</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:invoice-out:list']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
        <a-button v-permission="['psi:invoice-out:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
      </template>
      <template #invoiceType="{ record }">
        <a-tag v-if="record.invoiceType === 'SPECIAL'" color="arcoblue">专票</a-tag>
        <a-tag v-else-if="record.invoiceType === 'NORMAL'">普票</a-tag>
        <span v-else>{{ record.invoiceType }}</span>
      </template>
      <template #docStatus="{ record }">
        <a-tag v-if="record.docStatus === 'DRAFT'">草稿</a-tag>
        <a-tag v-else-if="record.docStatus === 'CONFIRMED'" color="green">已确认</a-tag>
        <a-tag v-else-if="record.docStatus === 'VOID'" color="red">已作废</a-tag>
        <a-tag v-else>{{ record.docStatus }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:invoice-out:list']" @click="onView(record)">详情</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:invoice-out:create']" @click="onUpdate(record)">修改</a-link>
          <a-tooltip v-if="record.docStatus === 'DRAFT'" :content="confirmDisabledTip(record)">
            <span>
              <a-link
                v-permission="['psi:invoice-out:confirm']"
                :disabled="hasUnallocated(record)"
                @click="onConfirm(record)"
              >确认</a-link>
            </span>
          </a-tooltip>
          <a-link v-if="record.docStatus === 'CONFIRMED' && !record.redOfId" v-permission="['psi:invoice-out:red']" status="warning" @click="onRed(record)">冲红</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:invoice-out:create']" status="danger" @click="onDelete(record)">删除</a-link>
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
import {
  confirmInvoiceOut,
  deleteInvoiceOut,
  exportInvoiceOut,
  listInvoiceOut,
  redInvoiceOut,
} from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'

defineOptions({ name: 'PsiInvoiceOut' })

const queryForm = reactive<any>({ sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listInvoiceOut({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '单据号', dataIndex: 'docNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '发票号', dataIndex: 'invoiceNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '类型', dataIndex: 'invoiceType', slotName: 'invoiceType', width: PsiCol.typeSm },
  { title: '开票日期', dataIndex: 'invoiceDate', width: PsiCol.date },
  { title: '含税金额', dataIndex: 'taxInclusiveAmount', width: PsiCol.amount },
  { title: '未分摊', dataIndex: 'unallocatedAmount', width: PsiCol.amount },
  { title: '税额', dataIndex: 'taxAmount', width: PsiCol.amount },
  { title: '状态', dataIndex: 'docStatus', slotName: 'docStatus', width: PsiCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: PsiCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionL },
]

const reset = () => {
  queryForm.invoiceNo = undefined
  queryForm.docNo = undefined
  queryForm.docStatus = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const onAdd = () => EditModalRef.value?.onAdd()
const onExport = () => useDownload(() => exportInvoiceOut(queryForm))
const onUpdate = (record: any) => EditModalRef.value?.onUpdate(record.id)
const onView = (record: any) => EditModalRef.value?.onView(record.id)

const hasUnallocated = (record: any) => Number(record.unallocatedAmount ?? 0) > 0.05
const confirmDisabledTip = (record: any) =>
  hasUnallocated(record)
    ? `尚有未分摊 ${Number(record.unallocatedAmount).toFixed(2)}，请修改补关联后再确认`
    : '确认后回写出库开票状态'

const onConfirm = (record: any) => {
  if (hasUnallocated(record)) {
    Message.warning(`尚有未分摊金额 ${Number(record.unallocatedAmount).toFixed(2)}，请补关联出库后再确认`)
    return
  }
  Modal.warning({
    title: '确认开票',
    content: `确认后仅回写出库开票状态，不改应收；发票号 ${record.invoiceNo}，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await confirmInvoiceOut(record.id, crypto.randomUUID())
      Message.success('确认成功')
      search()
    },
  })
}

const onRed = (record: any) => {
  Modal.warning({
    title: '冲红',
    content: `冲红将作废蓝字票并回退出库开票状态，不改应收；发票号 ${record.invoiceNo}，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await redInvoiceOut(record.id, crypto.randomUUID())
      Message.success('冲红成功')
      search()
    },
  })
}

const onDelete = (record: any) => {
  handleDelete(() => deleteInvoiceOut(record.id), {
    content: `是否删除草稿 ${record.invoiceNo || record.docNo}？`,
  })
}
</script>
