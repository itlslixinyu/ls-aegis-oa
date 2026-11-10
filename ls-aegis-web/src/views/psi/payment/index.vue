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
        <a-input-search v-model="queryForm.docNo" placeholder="付款单号" allow-clear @search="search" />
        <a-select v-model="queryForm.docStatus" placeholder="状态" allow-clear style="width: 120px" @change="search">
          <a-option value="DRAFT">草稿</a-option>
          <a-option value="CONFIRMED">已确认</a-option>
          <a-option value="VOID">已作废</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:payment:list']" @click="onExport">
          <template #icon><icon-download /></template>导出
        </a-button>
        <a-button v-permission="['psi:payment:create']" type="primary" @click="onAdd">
          <template #icon><icon-plus /></template>新增
        </a-button>
      </template>
      <template #docStatus="{ record }">
        <a-tag v-if="!record.refundSourceId && record.docStatus === 'DRAFT'">草稿</a-tag>
        <a-tag v-else-if="record.docStatus === 'CONFIRMED'" color="green">已确认</a-tag>
        <a-tag v-else-if="record.docStatus === 'VOID'" color="orange">已作废</a-tag>
        <a-tag v-else color="red">{{ record.docStatus }}</a-tag>
      </template>
      <template #cashSource="{ record }">
        <a-tag v-if="record.refundSourceId" color="purple">退款子单 · 源 #{{ record.refundSourceId }}</a-tag>
        <span v-else>普通单据</span>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:payment:list']" @click="onView(record)">详情</a-link>
          <a-link v-if="!record.refundSourceId && record.docStatus === 'DRAFT'" v-permission="['psi:payment:create']" @click="onUpdate(record)">修改</a-link>
          <a-link v-if="!record.refundSourceId && record.docStatus === 'DRAFT'" v-permission="['psi:payment:confirm']" @click="onConfirm(record)">确认</a-link>
          <a-link v-if="!record.refundSourceId && record.docStatus === 'DRAFT'" v-permission="['psi:payment:confirm']" @click="onConfirmWriteOff(record)">确认并核销</a-link>
          <a-link
            v-if="!record.refundSourceId && record.docStatus === 'CONFIRMED' && Number(record.unallocatedAmount) > 0"
            v-permission="['psi:payment:writeoff']"
            @click="onWriteOff(record)"
          >
            核销
          </a-link>
          <a-link
            v-if="!record.refundSourceId && record.docStatus === 'CONFIRMED' && Number(record.unallocatedAmount) < Number(record.amount)"
            v-permission="['psi:payment:writeoff']"
            @click="onReverse(record)"
          >
            反核销
          </a-link>
          <a-link v-if="!record.refundSourceId && record.docStatus === 'CONFIRMED' && canRefund && refundable(record) > 0" @click="onCashAction('REFUND', record)">登记退款</a-link>
          <a-link v-if="!record.refundSourceId && record.docStatus === 'CONFIRMED' && Number(record.refundedAmount || 0) === 0 && Number(record.unallocatedAmount) === Number(record.amount)" v-permission="['psi:payment:confirm']" status="danger" @click="onCashAction('VOID', record)">更正作废</a-link>
          <a-link v-if="record.refundSourceId && record.docStatus === 'CONFIRMED' && canReverseRefund" status="danger" @click="onCashAction('REVERSE', record)">撤销退款登记</a-link>
          <a-link v-if="!record.refundSourceId && record.docStatus === 'DRAFT'" v-permission="['psi:payment:create']" status="danger" @click="onDelete(record)">删除</a-link>
        </a-space>
      </template>
    </BaseTable>
    <a-modal v-model:visible="cashVisible" :title="cashTitle" :mask-closable="false" :width="600" @before-ok="submitCashAction">
      <a-alert type="info" style="margin-bottom: 16px">这里只登记已实际发生的退款或更正，不会自动向银行发起退款。请核对实际资金流水。</a-alert>
      <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
        <a-descriptions-item label="单号">{{ cashSource?.docNo }}</a-descriptions-item>
        <a-descriptions-item label="往来单位">{{ cashSource?.unitName }}</a-descriptions-item>
        <a-descriptions-item label="原金额">{{ cashSource?.amount }}</a-descriptions-item>
        <a-descriptions-item label="已退金额">{{ cashSource?.refundedAmount || 0 }}</a-descriptions-item>
      </a-descriptions>
      <a-form :model="cashForm" layout="vertical" :disabled="cashBusy">
        <template v-if="cashMode === 'REFUND'">
          <a-form-item label="已确认退货单" required>
            <a-select v-model="cashForm.stockReturnId" :options="cashReturnOptions" :loading="cashReturnsLoading" allow-search :filter-option="false" placeholder="输入退货单号搜索（仅当前单位及对应退货类型）" @search="loadCashReturns" />
          </a-form-item>
          <a-form-item :label="`退款金额（当前最多 ${cashAvailable}，以服务端校验为准）`" required>
            <a-input-number v-model="cashForm.amount" :min="0.01" :max="cashAvailable" :precision="2" />
          </a-form-item>
        </template>
        <a-alert v-else-if="cashMode === 'VOID'" type="warning" style="margin-bottom: 16px">作废仅用于更正登记。需先反核销、解除银行匹配，且本单尚无退款；实际退款请使用登记退款。</a-alert>
        <a-alert v-else type="warning" style="margin-bottom: 16px">仅更正本退款子单的台账登记并恢复源单余额；不会自动执行银行资金操作。</a-alert>
        <a-form-item label="原因" required><a-textarea v-model="cashForm.reason" :max-length="500" show-word-limit placeholder="请输入本次退款或更正原因" /></a-form-item>
      </a-form>
    </a-modal>
    <EditModal ref="EditModalRef" @save-success="search" />

    <a-modal
      v-model:visible="confirmWoVisible"
      title="确认付款并即时核销"
      :mask-closable="false"
      :width="640"
      draggable
      @before-ok="submitConfirmWriteOff"
      @close="resetConfirmWo"
    >
      <WriteOffItemsEditor
        ref="confirmWoEditorRef"
        v-model:reason="confirmWoReason"
        open-label="应付开口"
        tip="确认出账后立即核销所选应付（可多行）"
        :budget="confirmWoAmount"
        :options="openOptions"
      />
    </a-modal>

    <a-modal
      v-model:visible="writeOffVisible"
      title="核销应付"
      :mask-closable="false"
      :width="640"
      draggable
      @before-ok="submitWriteOff"
      @close="resetWriteOff"
    >
      <WriteOffItemsEditor
        ref="writeOffEditorRef"
        v-model:reason="writeOffReason"
        open-label="应付开口"
        tip="按开口逐行核销（可多行）"
        :budget="writeOffUnallocated"
        :options="openOptions"
      />
    </a-modal>

    <a-modal
      v-model:visible="reverseVisible"
      title="反核销应付"
      :mask-closable="false"
      :width="520"
      draggable
      @before-ok="submitReverse"
      @close="resetReverse"
    >
      <a-form :model="reverseForm" layout="vertical">
        <a-form-item label="核销批次" required>
          <a-select v-model="reverseForm.writeOffId" :options="writeOffOptions" allow-search placeholder="请选择要反核销的批次" />
        </a-form-item>
        <a-form-item label="原因">
          <a-input v-model="reverseForm.reason" allow-clear />
        </a-form-item>
      </a-form>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message, Modal } from '@arco-design/web-vue'
import WriteOffItemsEditor from '../components/WriteOffItemsEditor.vue'
import { PsiCol } from '../constants/table-col'
import EditModal from './EditModal.vue'
import {
  type PaymentResp,
  getPayment,
  voidPayment,
  refundPayment,
  reversePaymentRefund,
  listStockReturn,
  confirmPayment,
  deletePayment,
  exportPayment,
  listOpenPayables,
  listPayment,
  listPaymentWriteOffs,
  reversePayment,
  writeOffPayment,
} from '@/apis/psi'
import { useDownload, useTable } from '@/hooks'
import { useUserStore } from '@/stores'
import { matchPermission } from '@/utils/permission-match'

defineOptions({ name: 'PsiPayment' })

const queryForm = reactive<any>({ sort: ['id,desc'] })
const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
  handleDelete,
} = useTable((page) => listPayment({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '付款单号', dataIndex: 'docNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '付款日期', dataIndex: 'paymentDate', width: PsiCol.date },
  { title: '供应商', dataIndex: 'unitName' },
  { title: '账户', dataIndex: 'accountName', width: PsiCol.name },
  { title: '付款方式', dataIndex: 'payMethod', width: PsiCol.type },
  { title: '来源', dataIndex: 'refundSourceId', slotName: 'cashSource', width: 200 },
  { title: '已退金额', dataIndex: 'refundedAmount', width: PsiCol.amount },
  { title: '金额', dataIndex: 'amount', width: PsiCol.amount },
  { title: '未核销', dataIndex: 'unallocatedAmount', width: PsiCol.amount },
  { title: '状态', dataIndex: 'docStatus', slotName: 'docStatus', width: PsiCol.status },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionXl },
]

const reset = () => {
  queryForm.docNo = undefined
  queryForm.docStatus = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const onAdd = () => EditModalRef.value?.onAdd()
const onExport = () => useDownload(() => exportPayment(queryForm))
const onUpdate = (record: any) => EditModalRef.value?.onUpdate(record.id)
const onView = (record: any) => EditModalRef.value?.onView(record.id)

const onConfirm = (record: any) => {
  const confirmKey = crypto.randomUUID()
  Modal.warning({
    title: '确认付款',
    content: `确认后将出账并生成可核销余额，单号 ${record.docNo}，是否继续？`,
    hideCancel: false,
    onOk: async () => {
      await confirmPayment(record.id, confirmKey)
      Message.success('确认成功')
      search()
    },
  })
}

const confirmWoVisible = ref(false)
const confirmWoId = ref('')
const confirmWoKey = ref('')
const confirmWoAmount = ref(0)
const confirmWoReason = ref('')
const confirmWoEditorRef = ref<InstanceType<typeof WriteOffItemsEditor>>()
const openOptions = ref<{ label: string, value: string, unwrittenAmount?: number }[]>([])

const resetConfirmWo = () => {
  confirmWoId.value = ''
  confirmWoAmount.value = 0
  confirmWoReason.value = ''
  openOptions.value = []
  confirmWoEditorRef.value?.reset()
}

const onConfirmWriteOff = async (record: any) => {
  resetConfirmWo()
  confirmWoId.value = record.id
  confirmWoKey.value = crypto.randomUUID()
  confirmWoAmount.value = Number(record.amount) || 0
  const { data } = await listOpenPayables(record.unitId)
  openOptions.value = (data || []).map((i: any) => ({
    label: i.label,
    value: String(i.value),
    unwrittenAmount: Number(i.unwrittenAmount),
  }))
  if (!openOptions.value.length) {
    Message.warning('该供应商暂无未结清应付，请使用「确认」')
    return
  }
  confirmWoVisible.value = true
  await nextTick()
  confirmWoEditorRef.value?.reset(confirmWoAmount.value || undefined)
}

const submitConfirmWriteOff = async () => {
  try {
    const items = confirmWoEditorRef.value?.validateAndGet()
    if (!items?.length) return false
    await confirmPayment(confirmWoId.value, confirmWoKey.value, {
      writeOffItems: items,
      writeOffReason: confirmWoReason.value || undefined,
    })
    Message.success('确认并核销成功')
    search()
    return true
  } catch {
    return false
  }
}

const onDelete = (record: any) => {
  Modal.warning({
    title: '提示',
    content: `是否删除草稿 ${record.docNo}？`,
    hideCancel: false,
    onOk: async () => {
      await handleDelete(() => deletePayment(record.id))
      Message.success('删除成功')
    },
  })
}

const writeOffVisible = ref(false)
const writeOffId = ref('')
const writeOffKey = ref('')
const writeOffUnallocated = ref(0)
const writeOffReason = ref('')
const writeOffEditorRef = ref<InstanceType<typeof WriteOffItemsEditor>>()

const resetWriteOff = () => {
  writeOffId.value = ''
  writeOffUnallocated.value = 0
  writeOffReason.value = ''
  openOptions.value = []
  writeOffEditorRef.value?.reset()
}

const onWriteOff = async (record: any) => {
  resetWriteOff()
  writeOffId.value = record.id
  writeOffKey.value = crypto.randomUUID()
  writeOffUnallocated.value = Number(record.unallocatedAmount) || 0
  const { data } = await listOpenPayables(record.unitId)
  openOptions.value = (data || []).map((i: any) => ({
    label: i.label,
    value: String(i.value),
    unwrittenAmount: Number(i.unwrittenAmount),
  }))
  writeOffVisible.value = true
  await nextTick()
  writeOffEditorRef.value?.reset(writeOffUnallocated.value || undefined)
}

const submitWriteOff = async () => {
  try {
    const items = writeOffEditorRef.value?.validateAndGet()
    if (!items?.length) return false
    const { data } = await writeOffPayment(writeOffId.value, {
      idempotentKey: writeOffKey.value,
      items,
      reason: writeOffReason.value || undefined,
    })
    Message.success(`核销成功${data != null ? `，批次 ${data}` : ''}`)
    search()
    return true
  } catch {
    return false
  }
}

const reverseVisible = ref(false)
const reversePaymentId = ref('')
const reverseKey = ref('')
const writeOffOptions = ref<{ label: string, value: string }[]>([])
const reverseForm = reactive<any>({ writeOffId: undefined, reason: '' })

const resetReverse = () => {
  reversePaymentId.value = ''
  writeOffOptions.value = []
  reverseForm.writeOffId = undefined
  reverseForm.reason = ''
}

const onReverse = async (record: any) => {
  resetReverse()
  reversePaymentId.value = record.id
  reverseKey.value = crypto.randomUUID()
  const { data } = await listPaymentWriteOffs(record.id)
  writeOffOptions.value = (data || []).map((i: any) => ({
    label: `#${i.writeOffId}｜¥${i.amount}${i.writeOffDate ? `｜${i.writeOffDate}` : ''}`,
    value: String(i.writeOffId),
  }))
  if (!writeOffOptions.value.length) {
    Message.warning('无可反核销批次')
    return
  }
  reverseForm.writeOffId = writeOffOptions.value[0].value
  reverseVisible.value = true
}

const submitReverse = async () => {
  try {
    if (!reverseForm.writeOffId) {
      Message.warning('请选择核销批次')
      return false
    }
    await reversePayment(reversePaymentId.value, {
      idempotentKey: reverseKey.value,
      writeOffId: reverseForm.writeOffId,
      reason: reverseForm.reason || undefined,
    })
    Message.success('反核销成功')
    search()
    return true
  } catch {
    return false
  }
}

const userStore = useUserStore()
const hasAllPermissions = (...required: string[]) => required.every((permission) => userStore.permissions.some((owned) => matchPermission(owned, permission)))
const canRefund = computed(() => hasAllPermissions('psi:payment:writeoff', 'psi:receipt:confirm'))
const canReverseRefund = computed(() => hasAllPermissions('psi:payment:confirm', 'psi:receipt:writeoff'))
const cashVisible = ref(false)
const cashBusy = ref(false)
const cashMode = ref<'VOID' | 'REFUND' | 'REVERSE'>('REFUND')
const cashSource = ref<PaymentResp>()
const cashKey = ref('')
const cashForm = reactive({ stockReturnId: undefined as string | undefined, amount: undefined as number | undefined, reason: '' })
const cashReturnOptions = ref<{ label: string, value: string }[]>([])
const cashReturnsLoading = ref(false)
let cashReturnRequest = 0
const refundable = (record: PaymentResp) => Math.max(0, Math.min(Number(record.unallocatedAmount || 0), Number(record.amount) - Number(record.refundedAmount || 0)))
const cashAvailable = computed(() => cashSource.value ? refundable(cashSource.value) : 0)
const cashTitle = computed(() => ({ VOID: '更正作废付款', REFUND: '登记实际退款', REVERSE: '撤销退款登记' })[cashMode.value])

const loadCashReturns = async (keyword = '') => {
  const request = ++cashReturnRequest
  const unitId = cashSource.value?.unitId
  if (!unitId) return
  cashReturnsLoading.value = true
  try {
    const { data } = await listStockReturn({ unitId, docNo: keyword || undefined, docStatus: 'CONFIRMED', returnType: 'PURCHASE_RETURN', page: 1, size: 100, sort: ['id,desc'] })
    if (request !== cashReturnRequest) return
    cashReturnOptions.value = (data?.list || []).filter((row) => row.unitId === unitId && row.docStatus === 'CONFIRMED' && row.returnType === 'PURCHASE_RETURN').map((row) => ({ label: `${row.docNo} · ${row.taxInclusiveAmount}`, value: row.id }))
  } finally { if (request === cashReturnRequest) cashReturnsLoading.value = false }
}

const onCashAction = async (mode: 'VOID' | 'REFUND' | 'REVERSE', record: PaymentResp) => {
  const { data } = await getPayment(record.id)
  if (!data || data.docStatus !== 'CONFIRMED') { Message.warning('单据状态已变更，请刷新'); search(); return }
  cashSource.value = data
  cashMode.value = mode
  cashKey.value = crypto.randomUUID()
  cashForm.stockReturnId = undefined
  cashForm.amount = undefined
  cashForm.reason = ''
  cashReturnOptions.value = []
  cashVisible.value = true
  if (mode === 'REFUND') await loadCashReturns()
}

const submitCashAction = async () => {
  const source = cashSource.value
  const reason = cashForm.reason.trim()
  if (!source || !reason) { Message.warning('请输入原因'); return false }
  if (cashMode.value === 'REFUND' && (!cashForm.stockReturnId || !cashForm.amount || !Number.isFinite(cashForm.amount) || cashForm.amount <= 0 || cashForm.amount > cashAvailable.value)) {
    Message.warning('请选择退货单并输入可退款范围内的金额'); return false
  }
  cashBusy.value = true
  try {
    const request = { reason, idempotentKey: cashKey.value }
    if (cashMode.value === 'REFUND') {
      await refundPayment(source.id, { ...request, stockReturnId: cashForm.stockReturnId!, amount: cashForm.amount! })
    } else if (cashMode.value === 'VOID') {
      await voidPayment(source.id, request)
    } else {
      await reversePaymentRefund(source.id, request)
    }
    Message.success('台账登记已更新')
    search()
    return true
  } catch { return false } finally { cashBusy.value = false }
}

</script>
