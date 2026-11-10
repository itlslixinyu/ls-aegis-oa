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
          v-model="queryForm.customerName"
          placeholder="搜索客户名称"
          allow-clear
          @search="search"
        />
        <a-select
          v-model="queryForm.status"
          placeholder="状态"
          allow-clear
          style="width: 140px"
          :options="statusOptions"
          @change="search"
        />
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['eoms:receivable:add']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          新增
        </a-button>
      </template>
      <template #status="{ record }">
        <a-tag v-if="record.status === 'VOID'" color="orange">已作废</a-tag>
        <a-tag v-else-if="Number(record.amount) === 0 && Number(record.arrearsAmount) === 0" color="green">无需收款</a-tag>
        <GiCellTag v-else :value="record.status" :dict="eoms_receivable_status" />
      </template>
      <template #action="{ record }">
        <a-space>
          <a-tag v-if="record.repairOrderId">维修业务账款</a-tag>
          <template v-else>
            <a-link v-permission="['eoms:receivable:update']" @click="onUpdate(record)">修改</a-link>
            <a-link v-permission="['eoms:receivable:delete']" status="danger" @click="onDelete(record)">删除</a-link>
          </template>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { EomsCol } from '../constants/table-col'
import AddModal from './ReceivableAddModal.vue'
import { type ReceivableResp, deleteReceivable, listReceivable } from '@/apis/eoms'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useTable } from '@/hooks'
import { useDict } from '@/hooks/app'

defineOptions({ name: 'EomsFinanceReceivable' })

const { eoms_receivable_status } = useDict('eoms_receivable_status')

const statusOptions = computed(() =>
  [...(eoms_receivable_status.value || []).map((d) => ({ label: d.label, value: d.value })), { label: '已作废', value: 'VOID' }],
)

const queryForm = reactive<Record<string, any>>({})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listReceivable({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '客户', dataIndex: 'customerName', width: EomsCol.nameLg },
  { title: '应收金额', dataIndex: 'amount', width: 120 },
  { title: '已收金额', dataIndex: 'receivedAmount', width: 120 },
  { title: '欠款金额', dataIndex: 'arrearsAmount', width: 120 },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: EomsCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: EomsCol.date },
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
const onUpdate = (record: ReceivableResp) => AddModalRef.value?.onUpdate(String(record.id))
const onDelete = (record: ReceivableResp) =>
  handleDelete(() => deleteReceivable(record.id), { content: `是否删除应收记录「${record.customerName}」？` })
</script>
