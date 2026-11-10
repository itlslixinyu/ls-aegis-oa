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
        <a-input-search v-model="queryForm.contractNo" placeholder="合同编号" allow-clear @search="search" />
        <a-input-search v-model="queryForm.name" placeholder="合同名称" allow-clear @search="search" />
        <a-select v-model="queryForm.docStatus" placeholder="状态" allow-clear style="width: 120px" @change="search">
          <a-option value="DRAFT">草稿</a-option>
          <a-option value="CONFIRMED">已确认</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:contractPurchase:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
        <a-button v-permission="['psi:contractPurchase:list']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
      </template>
      <template #progress="{ record }">
        <span v-if="record.progress == null || record.progress === ''">-</span>
        <a-progress v-else :percent="toPercent(record.progress)" :stroke-width="6" size="small" />
      </template>
      <template #docStatus="{ record }">
        <a-tag v-if="record.docStatus === 'DRAFT'">草稿</a-tag>
        <a-tag v-else-if="record.docStatus === 'CONFIRMED'" color="green">已确认</a-tag>
        <a-tag v-else>{{ record.docStatus }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:contractPurchase:list']" @click="onView(record)">详情</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:contractPurchase:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:contractPurchase:update']" @click="onConfirm(record)">确认</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:contractPurchase:update']" status="danger" @click="onDelete(record)">删除</a-link>
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
  confirmContractPurchase,
  deleteContractPurchase,
  exportContractPurchase,
  listContractPurchase,
} from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'

defineOptions({ name: 'PsiContractPurchase' })

/** 后端 progress 为 0~1 比例，进度条需要 0~100；非法超大值直接封顶，避免出现上千百分比 */
const toPercent = (v: number | string) => {
  const n = Number(v)
  if (Number.isNaN(n) || n <= 0) return 0
  const pct = n <= 1 ? n * 100 : n
  return Math.min(100, Math.round(pct * 100) / 100)
}

const queryForm = reactive<any>({ sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listContractPurchase({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '合同编号', dataIndex: 'contractNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '合同名称', dataIndex: 'name' },
  { title: '签订日期', dataIndex: 'signDate', width: PsiCol.date },
  { title: '含税总额', dataIndex: 'taxInclusiveAmount', width: PsiCol.amount },
  { title: '起', dataIndex: 'startDate', width: PsiCol.dateSm },
  { title: '止', dataIndex: 'endDate', width: PsiCol.dateSm },
  { title: '执行进度', dataIndex: 'progress', slotName: 'progress', width: PsiCol.amount },
  { title: '状态', dataIndex: 'docStatus', slotName: 'docStatus', width: PsiCol.status },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionM },
]

const reset = () => {
  queryForm.contractNo = undefined
  queryForm.name = undefined
  queryForm.docStatus = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const onAdd = () => EditModalRef.value?.onAdd()
const onExport = () => useDownload(() => exportContractPurchase(queryForm as any))
const onUpdate = (record: any) => EditModalRef.value?.onUpdate(record.id)
const onView = (record: any) => EditModalRef.value?.onView(record.id)

const onConfirm = (record: any) => {
  Modal.warning({
    title: '确认合同',
    content: `确认生效采购合同 ${record.contractNo}，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await confirmContractPurchase(record.id, crypto.randomUUID())
      Message.success('确认成功')
      search()
    },
  })
}

const onDelete = (record: any) => {
  handleDelete(() => deleteContractPurchase(record.id), {
    content: `是否删除草稿 ${record.contractNo}？`,
  })
}
</script>
