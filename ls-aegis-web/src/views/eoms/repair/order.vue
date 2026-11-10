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
          placeholder="搜索维修单号"
          allow-clear
          @search="search"
        />
        <a-input
          v-model="queryForm.customerName"
          placeholder="客户名称"
          allow-clear
          style="width: 140px"
          @press-enter="search"
        />
        <a-select
          v-model="queryForm.status"
          placeholder="工单状态"
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
        <a-button v-permission="['eoms:order:add']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          新增
        </a-button>
      </template>
      <template #status="{ record }">
        <GiCellTag :value="record.status" :dict="eoms_order_status" />
      </template>
      <template #action="{ record }">
        <a-space wrap>
          <a-link
            v-if="record.status === 'WAIT_ACCEPT'"
            v-permission="['eoms:repair:assign']"
            @click="onAssign(record)"
          >
            派单
          </a-link>
          <a-link
            v-if="record.status === 'ACCEPTED'"
            v-permission="['eoms:order:start']"
            @click="onStart(record)"
          >
            开始维修
          </a-link>
          <a-link
            v-if="record.status === 'PROCESSING'"
            v-permission="['eoms:order:finish']"
            @click="onSubmit(record)"
          >
            提交结算
          </a-link>
          <a-link
            v-if="['ACCEPTED', 'PROCESSING', 'WAIT_SETTLEMENT', 'FINISHED'].includes(record.status)"
            v-permission="['eoms:order:query', 'eoms:settlement:query']"
            @click="onMaterial(record)"
          >
            用料
          </a-link>
          <a-link
            v-if="['ACCEPTED', 'PROCESSING', 'WAIT_SETTLEMENT', 'FINISHED'].includes(record.status)"
            v-permission="['eoms:record:list', 'eoms:record:query', 'eoms:order:query']"
            @click="onRecord(record)"
          >
            维修记录
          </a-link>
          <a-link
            v-if="record.status === 'FINISHED'"
            v-permission="['eoms:order:close']"
            @click="onClose(record)"
          >
            关闭
          </a-link>
          <a-link v-if="record.status === 'WAIT_SETTLEMENT'" v-permission="['eoms:order:close']" status="danger" @click="onCancelReversed(record)">撤销关闭</a-link>
          <a-link v-if="record.status === 'WAIT_ACCEPT'" v-permission="['eoms:order:delete']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />
    <AssignModal ref="AssignModalRef" @save-success="search" />
    <MaterialModal ref="MaterialModalRef" />
    <RecordModal ref="RecordModalRef" />
    <a-modal v-model:visible="cancelVisible" title="反确认后撤销关闭" :on-before-ok="submitCancelReversed">
      <a-alert type="warning">仅可关闭已反确认全部结算、冲销收款与成本并恢复库存的工单。原始业务记录和撤销原因将保留。</a-alert>
      <a-textarea v-model="cancelReason" placeholder="请输入撤销关闭原因" :max-length="500" show-word-limit />
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { EomsCol } from '../constants/table-col'
import AddModal from './OrderAddModal.vue'
import AssignModal from './OrderAssignModal.vue'
import MaterialModal from './MaterialLineModal.vue'
import RecordModal from './OrderRecordModal.vue'
import {
  type RepairOrderResp,
  closeOrder,
  cancelReversedOrder,
  deleteRepairOrder,
  listRepairOrder,
  listRepairRecord,
  startOrder,
  submitOrder,
} from '@/apis/eoms'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useTable } from '@/hooks'
import { useDict } from '@/hooks/app'

defineOptions({ name: 'EomsRepairOrder' })

const { eoms_order_status } = useDict('eoms_order_status')

const statusOptions = computed(() =>
  (eoms_order_status.value || []).map((d) => ({ label: d.label, value: d.value })),
)

const queryForm = reactive<Record<string, any>>({})
const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelId = ref('')
const onCancelReversed = (record: RepairOrderResp) => {
  cancelId.value = record.id
  cancelReason.value = ''
  cancelVisible.value = true
}
const submitCancelReversed = async () => {
  if (!cancelReason.value.trim()) { Message.warning('请输入撤销关闭原因'); return false }
  try {
    await cancelReversedOrder(cancelId.value, cancelReason.value.trim())
    Message.success('工单已撤销关闭，历史记录已保留')
    search()
    return true
  } catch { return false }
}

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listRepairOrder({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '维修单号', dataIndex: 'orderNo', minWidth: EomsCol.codeMin, ellipsis: false },
  { title: '客户', dataIndex: 'customerName', width: EomsCol.name },
  { title: '工程师', dataIndex: 'engineerName', width: 120 },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: EomsCol.status },
  { title: '地址', dataIndex: 'address', ellipsis: true, tooltip: true },
  { title: '创建时间', dataIndex: 'createTime', width: EomsCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: 300 },
]

const reset = () => {
  Object.keys(queryForm).forEach((k) => {
    queryForm[k] = undefined
  })
  search()
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const AssignModalRef = ref<InstanceType<typeof AssignModal>>()
const MaterialModalRef = ref<InstanceType<typeof MaterialModal>>()
const RecordModalRef = ref<InstanceType<typeof RecordModal>>()
const onAdd = () => AddModalRef.value?.onAdd()
const onAssign = (record: RepairOrderResp) => AssignModalRef.value?.onOpen(record)
const onMaterial = (record: RepairOrderResp) => {
  const canEdit = ['ACCEPTED', 'PROCESSING', 'WAIT_SETTLEMENT'].includes(String(record.status))
  MaterialModalRef.value?.onOpen(record.id, canEdit)
}
const onRecord = (record: RepairOrderResp) => {
  // 关闭前均可补录/修改，便于历史缺记录工单回补
  const canEdit = ['ACCEPTED', 'PROCESSING', 'WAIT_SETTLEMENT', 'FINISHED'].includes(String(record.status))
  RecordModalRef.value?.onOpen(record.id, record.orderNo, canEdit)
}

const unwrapList = (raw: any) => {
  if (Array.isArray(raw)) return raw
  if (Array.isArray(raw?.list)) return raw.list
  if (Array.isArray(raw?.records)) return raw.records
  if (Array.isArray(raw?.rows)) return raw.rows
  return []
}

const runAction = (content: string, api: () => Promise<any>) => {
  Modal.warning({
    title: '提示',
    content,
    hideCancel: false,
    onOk: async () => {
      await api()
      Message.success('操作成功')
      search()
    },
  })
}

const onStart = (record: RepairOrderResp) =>
  runAction(`开始维修工单「${record.orderNo}」？`, () => startOrder(record.id))
const onSubmit = async (record: RepairOrderResp) => {
  try {
    const { data } = await listRepairRecord({ repairOrderId: record.id, page: 1, size: 1 })
    if (!unwrapList(data).length) {
      Message.warning('请先填写维修记录后再提交结算')
      onRecord(record)
      return
    }
  } catch {
    // 列表失败时仍交给后端门禁
  }
  runAction(`提交工单「${record.orderNo}」结算？`, () => submitOrder(record.id))
}
const onClose = (record: RepairOrderResp) =>
  runAction(`关闭工单「${record.orderNo}」？`, () => closeOrder(record.id))
const onDelete = (record: RepairOrderResp) =>
  handleDelete(() => deleteRepairOrder(record.id), { content: `是否删除工单「${record.orderNo}」？` })
</script>
