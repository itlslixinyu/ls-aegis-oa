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
          v-model="queryForm.paymentNo"
          placeholder="搜索收款单号"
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
        <a-select v-model="queryForm.status" placeholder="收款状态" :options="[{label: '有效收款', value: 'ACTIVE'}, {label: '已撤销', value: 'REVERSED'}, {label: '全部审计', value: 'ALL'}]" style="width:140px" @change="search" />
        <a-button @click="reset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['eoms:payment:add']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>
          新增
        </a-button>
      </template>
      <template #paymentMethod="{ record }">
        <GiCellTag :value="record.paymentMethod" :dict="eoms_payment_method" />
      </template>
      <template #status="{ record }"><a-tag :color="record.status === 'REVERSED' ? 'orange' : 'green'">{{ record.status === 'REVERSED' ? '已撤销' : '有效' }}</a-tag></template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['eoms:payment:query']" @click="onView(record)">详情</a-link>
          <a-link v-if="record.status !== 'REVERSED'" v-permission="['eoms:payment:delete']" status="danger" @click="onDelete(record)">撤销</a-link>
        </a-space>
      </template>
    </BaseTable>
    <AddModal ref="AddModalRef" @save-success="search" />

    <a-modal v-model:visible="detailVisible" title="维修收款详情" :footer="false" :width="520">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="收款单号">{{ detail.paymentNo }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="金额">{{ detail.amount }}</a-descriptions-item>
        <a-descriptions-item label="收款方式">
          <GiCellTag :value="detail.paymentMethod" :dict="eoms_payment_method" />
        </a-descriptions-item>
        <a-descriptions-item label="收款时间">{{ detail.paymentTime }}</a-descriptions-item>
        <a-descriptions-item label="备注">{{ detail.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { EomsCol } from '../constants/table-col'
import AddModal from './PaymentAddModal.vue'
import { type PaymentResp, deletePayment, getPayment, listPayment } from '@/apis/eoms'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { useTable } from '@/hooks'
import { useDict } from '@/hooks/app'

defineOptions({ name: 'EomsFinanceReceipt' })

const { eoms_payment_method } = useDict('eoms_payment_method')

const queryForm = reactive<Record<string, any>>({})
const detailVisible = ref(false)
const detail = ref<Partial<PaymentResp>>({})

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listPayment({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '收款单号', dataIndex: 'paymentNo', minWidth: EomsCol.codeMin, ellipsis: false },
  { title: '客户', dataIndex: 'customerName', width: EomsCol.name },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: 90 },
  { title: '金额', dataIndex: 'amount', width: 120 },
  { title: '收款方式', dataIndex: 'paymentMethod', slotName: 'paymentMethod', width: 120 },
  { title: '收款时间', dataIndex: 'paymentTime', width: EomsCol.date },
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
const onView = async (record: PaymentResp) => {
  const { data } = await getPayment(record.id)
  detail.value = data || record
  detailVisible.value = true
}
const onDelete = (record: PaymentResp) =>
  handleDelete(() => deletePayment(record.id), { content: `是否撤销收款单「${record.paymentNo}」？将冲销对应收入并恢复应收，保留原始记录。` })
</script>
