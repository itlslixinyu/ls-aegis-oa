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
        <a-input-search
          v-model="queryForm.orderNo"
          placeholder="搜索工单编号"
          allow-clear
          @search="search"
        />
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['eoms:record:add']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          新增
        </a-button>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['eoms:record:update']" @click="onUpdate(record)">修改</a-link>
          <a-link v-permission="['eoms:record:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { EomsCol } from '../constants/table-col'
import AddModal from './RecordAddModal.vue'
import { type RepairRecordResp, deleteRepairRecord, listRepairRecord } from '@/apis/eoms'
import { useTable } from '@/hooks'

defineOptions({ name: 'EomsRepairRecord' })

const queryForm = reactive<Record<string, any>>({})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listRepairRecord({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '工单编号', dataIndex: 'orderNo', minWidth: EomsCol.codeMin, ellipsis: false },
  { title: '工程师', dataIndex: 'engineerName', width: 120 },
  { title: '故障原因', dataIndex: 'faultReason', ellipsis: true, tooltip: true },
  { title: '维修结果', dataIndex: 'result', ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: EomsCol.date },
  { title: '备注', dataIndex: 'remark', ellipsis: true, tooltip: true },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: EomsCol.actionS },
]

const reset = () => {
  Object.keys(queryForm).forEach((k) => {
    queryForm[k] = undefined
  })
  search()
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const onAdd = () => AddModalRef.value?.onAdd()
const onUpdate = (record: RepairRecordResp) => AddModalRef.value?.onUpdate(String(record.id))
const onDelete = (record: RepairRecordResp) =>
  handleDelete(() => deleteRepairRecord(record.id), { content: `是否删除维修记录「${record.orderNo || record.id}」？` })
</script>
