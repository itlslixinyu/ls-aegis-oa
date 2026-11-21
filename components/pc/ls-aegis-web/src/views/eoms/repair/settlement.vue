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
          v-model="queryForm.settlementNo"
          placeholder="搜索结算单号"
          allow-clear
          @search="search"
        />
        <a-input
          v-model="queryForm.orderNo"
          placeholder="工单编号"
          allow-clear
          style="width: 140px"
          @press-enter="search"
        />
        <a-select
          v-model="queryForm.status"
          placeholder="结算状态"
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
        <a-tag>工单提交结算后自动生成草稿，请在列表编辑并确认</a-tag>
      </template>
      <template #status="{ record }">
        <a-tag v-if="record.status === 'REVERSED'" color="orange">已反确认</a-tag><GiCellTag v-else :value="record.status" :dict="eoms_settle_status" />
      </template>
      <template #action="{ record }">
        <a-space wrap>
          <a-link v-permission="['eoms:settlement:query']" @click="onView(record)">查看</a-link>
          <a-link @click="onMaterial(record)">用料</a-link>
          <a-link v-if="record.status === 'CONFIRMED'" v-permission="['eoms:settlement:confirm']" status="danger" @click="onReverse(record)">反确认</a-link>
          <a-link v-if="record.status === 'REVERSED'" v-permission="['eoms:order:finish']" @click="onResettle(record)">重新结算</a-link>
          <a-link
            v-if="record.status === 'DRAFT'"
            v-permission="['eoms:settlement:save']"
            @click="onUpdate(record)"
          >
            编辑
          </a-link>
          <a-link
            v-if="record.status === 'DRAFT'"
            v-permission="['eoms:settlement:confirm']"
            @click="onConfirm(record)"
          >
            确认
          </a-link>
        </a-space>
      </template>
    </BaseTable>
    <a-modal v-model:visible="reverseVisible" title="反确认结算" :on-before-ok="submitReverse">
      <p>请先撤销全部收款。反确认将恢复库存、冲销成本、作废应收并将工单退回待结算，原结算保留审计。</p>
      <a-textarea v-model="reverseReason" placeholder="请输入反确认原因" :max-length="500" show-word-limit />
    </a-modal>
    <AddModal ref="AddModalRef" @save-success="search" />
    <MaterialModal ref="MaterialModalRef" />

    <a-modal v-model:visible="detailVisible" title="结算详情" :footer="false" :width="640">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="结算单号">{{ detail.settlementNo }}</a-descriptions-item>
        <a-descriptions-item label="工单编号">{{ detail.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag v-if="detail.status === 'REVERSED'" color="orange">已反确认</a-tag><GiCellTag v-else :value="detail.status" :dict="eoms_settle_status" />
        </a-descriptions-item>
        <a-descriptions-item label="人工费">{{ detail.laborAmount ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="材料费">{{ detail.materialAmount ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="合计" :span="2">{{ detail.totalIncome ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import { EomsCol } from '../constants/table-col'
import AddModal from './SettlementAddModal.vue'
import MaterialModal from './MaterialLineModal.vue'
import {
  type RepairSettlementResp,
  confirmSettlement,
  reverseSettlement,
  submitOrder,
  getSettlement,
  listSettlement,
} from '@/apis/eoms'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useTable } from '@/hooks'
import { useDict } from '@/hooks/app'

defineOptions({ name: 'EomsRepairSettlement' })

const { eoms_settle_status } = useDict('eoms_settle_status')

const statusOptions = computed(() =>
  [...(eoms_settle_status.value || []).map((d) => ({ label: d.label, value: d.value })), { label: '已反确认', value: 'REVERSED' }],
)

const queryForm = reactive<Record<string, any>>({})
const reverseVisible = ref(false)
const reverseReason = ref('')
const reverseId = ref('')
const onReverse = (record: RepairSettlementResp) => {
  reverseId.value = record.id
  reverseReason.value = ''
  reverseVisible.value = true
}
const submitReverse = async () => {
  if (!reverseReason.value.trim()) { Message.warning('请输入反确认原因'); return false }
  try {
    await reverseSettlement(reverseId.value, reverseReason.value.trim())
    Message.success('反确认成功，工单已退回待结算')
    search()
    return true
  } catch { return false }
}
const detailVisible = ref(false)
const detail = ref<Partial<RepairSettlementResp>>({})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => listSettlement({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '结算单号', dataIndex: 'settlementNo', minWidth: EomsCol.codeMin, ellipsis: false },
  { title: '工单编号', dataIndex: 'orderNo', width: EomsCol.codeMin },
  { title: '客户', dataIndex: 'customerName', width: EomsCol.name },
  { title: '人工费', dataIndex: 'laborAmount', width: 100 },
  { title: '材料费', dataIndex: 'materialAmount', width: 100 },
  { title: '合计', dataIndex: 'totalIncome', width: 100 },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: EomsCol.status },
  { title: '创建时间', dataIndex: 'createTime', width: EomsCol.date },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: EomsCol.actionL },
]

const reset = () => {
  Object.keys(queryForm).forEach((k) => {
    queryForm[k] = undefined
  })
  search()
}

const AddModalRef = ref<InstanceType<typeof AddModal>>()
const MaterialModalRef = ref<InstanceType<typeof MaterialModal>>()
const onUpdate = (record: RepairSettlementResp) => AddModalRef.value?.onUpdate(String(record.id))
const onMaterial = (record: RepairSettlementResp) => {
  const orderId = record.repairOrderId ?? record.orderId
  if (!orderId) {
    Message.warning('结算单未关联工单')
    return
  }
  MaterialModalRef.value?.onOpen(orderId, record.status === 'DRAFT')
}

const onView = async (record: RepairSettlementResp) => {
  const { data } = await getSettlement(record.id)
  detail.value = data || record
  detailVisible.value = true
}

const onConfirm = (record: RepairSettlementResp) => {
  Modal.warning({
    title: '提示',
    content: `确认结算单「${record.settlementNo}」？确认后不可再修改。`,
    hideCancel: false,
    onOk: async () => {
      await confirmSettlement(record.id)
      Message.success('结算确认成功')
      search()
    },
  })
}
const onResettle = (record: RepairSettlementResp) => {
  const orderId = record.repairOrderId ?? record.orderId
  if (!orderId) { Message.warning('结算单未关联工单'); return }
  Modal.warning({
    title: '重新结算',
    content: '保留原结算及冲销记录，为待结算工单生成新的结算草稿；已有草稿时继续使用该草稿。',
    hideCancel: false,
    onOk: async () => {
      await submitOrder(orderId)
      Message.success('结算草稿已就绪，请编辑新草稿后确认')
      search()
    },
  })
}
</script>
