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
        <a-input-search v-model="queryForm.docNo" placeholder="退货单号" allow-clear @search="search" />
        <a-select
          v-model="queryForm.returnType"
          placeholder="类型"
          allow-clear
          style="width: 140px"
          :options="returnTypeOptions"
          @change="search"
        />
        <a-select v-model="queryForm.docStatus" placeholder="状态" allow-clear style="width: 120px" @change="search">
          <a-option value="DRAFT">草稿</a-option>
          <a-option value="CONFIRMED">已确认</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:stockReturn:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
        <a-button v-permission="['psi:stockReturn:list']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
      </template>
      <template #returnType="{ record }">
        <GiCellTag :value="record.returnType" :dict="psi_return_type" />
      </template>
      <template #docStatus="{ record }">
        <a-tag v-if="record.docStatus === 'DRAFT'">草稿</a-tag>
        <a-tag v-else-if="record.docStatus === 'CONFIRMED'" color="green">已确认</a-tag>
        <a-tag v-else color="red">{{ record.docStatus }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:stockReturn:get']" @click="onView(record)">详情</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:stockReturn:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:stockReturn:confirm']" @click="onConfirm(record)">确认</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:stockReturn:delete']" status="danger" @click="onDelete(record)">删除</a-link>
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
import { type StockReturnQuery, type StockReturnResp, confirmStockReturn, deleteStockReturn, exportStockReturn, listStockReturn } from '@/apis/psi'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useDownload, useTable } from '@/hooks'
import { useDict } from '@/hooks/app'
import { dictSelectOptions } from '@/utils/dict'

defineOptions({ name: 'PsiStockReturn' })

const { psi_return_type } = useDict('psi_return_type')
const returnTypeOptions = computed(() => dictSelectOptions(psi_return_type.value))

const route = useRoute()
const queryForm = reactive<StockReturnQuery>({ sort: ['id,desc'] })
if (route.query.docNo) {
  queryForm.docNo = String(route.query.docNo)
}
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listStockReturn({ ...queryForm, ...page }), { immediate: true })

watch(
  () => route.query.docNo,
  (docNo) => {
    queryForm.docNo = docNo ? String(docNo) : undefined
    search()
  },
)

const columns: TableInstance['columns'] = [
  { title: '退货单号', dataIndex: 'docNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '类型', dataIndex: 'returnType', slotName: 'returnType', width: PsiCol.type },
  { title: '退货日期', dataIndex: 'returnDate', width: PsiCol.date },
  { title: '含税金额', dataIndex: 'taxInclusiveAmount', width: PsiCol.amount },
  { title: '成本', dataIndex: 'costAmount', width: PsiCol.amount },
  { title: '状态', dataIndex: 'docStatus', slotName: 'docStatus', width: PsiCol.status },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionM },
]

const reset = () => {
  queryForm.docNo = undefined
  queryForm.docStatus = undefined
  queryForm.returnType = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const onAdd = () => EditModalRef.value?.onAdd()
const onExport = () => useDownload(() => exportStockReturn(queryForm as any))
const onUpdate = (record: StockReturnResp) => EditModalRef.value?.onUpdate(record.id)
const onView = (record: StockReturnResp) => EditModalRef.value?.onView(record.id)

const onConfirm = (record: StockReturnResp) => {
  Modal.warning({
    title: '确认退货',
    content: `确认后将按原路恢复/扣减库存并调整应收应付，单号 ${record.docNo}，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await confirmStockReturn(record.id, crypto.randomUUID())
      Message.success('确认成功')
      search()
    },
  })
}

const onDelete = (record: StockReturnResp) => {
  Modal.warning({
    title: '提示',
    content: `是否删除草稿 ${record.docNo}？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deleteStockReturn(record.id))
      Message.success('删除成功')
    },
  })
}
</script>
