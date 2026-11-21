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
        <a-input-search v-model="queryForm.docNo" placeholder="调整单号" allow-clear @search="search" />
        <a-select v-model="queryForm.docStatus" placeholder="状态" allow-clear style="width: 120px" @change="search">
          <a-option value="DRAFT">草稿</a-option>
          <a-option value="CONFIRMED">已确认</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:adjust:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
        <a-button v-permission="['psi:adjust:list']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
      </template>
      <template #docStatus="{ record }">
        <a-tag v-if="record.docStatus === 'DRAFT'">草稿</a-tag>
        <a-tag v-else-if="record.docStatus === 'CONFIRMED'" color="green">已确认</a-tag>
        <a-tag v-else color="red">{{ record.docStatus }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:adjust:list']" @click="onView(record)">详情</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:adjust:create']" @click="onUpdate(record)">修改</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:adjust:confirm']" @click="onConfirm(record)">确认</a-link>
          <a-link v-if="record.docStatus === 'DRAFT'" v-permission="['psi:adjust:create']" status="danger" @click="onDelete(record)">删除</a-link>
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
import { confirmStockAdjust, deleteStockAdjust, exportStockAdjust, listStockAdjust } from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'

defineOptions({ name: 'PsiStockAdjust' })

const queryForm = reactive<any>({ sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listStockAdjust({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '调整单号', dataIndex: 'docNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '调整日期', dataIndex: 'adjustDate', width: PsiCol.date },
  { title: '仓库', dataIndex: 'warehouseName', width: PsiCol.name },
  { title: '状态', dataIndex: 'docStatus', slotName: 'docStatus', width: PsiCol.status },
  { title: '备注', dataIndex: 'remark' },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionM },
]

const reset = () => {
  queryForm.docNo = undefined
  queryForm.docStatus = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const onAdd = () => EditModalRef.value?.onAdd()
const onExport = () => useDownload(() => exportStockAdjust(queryForm as any))
const onUpdate = (record: any) => EditModalRef.value?.onUpdate(record.id)
const onView = (record: any) => EditModalRef.value?.onView(record.id)

const onConfirm = (record: any) => {
  Modal.warning({
    title: '确认调整',
    content: `确认后将按盈亏调整库存，单号 ${record.docNo}，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await confirmStockAdjust(record.id, crypto.randomUUID())
      Message.success('确认成功')
      search()
    },
  })
}

const onDelete = (record: any) => {
  Modal.warning({
    title: '提示',
    content: `是否删除草稿 ${record.docNo}？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deleteStockAdjust(record.id))
      Message.success('删除成功')
    },
  })
}
</script>
