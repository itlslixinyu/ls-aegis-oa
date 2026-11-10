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
        <a-select
          v-model="queryForm.accountId"
          :options="accountOptions"
          placeholder="银行账户"
          allow-clear
          allow-search
          style="width: 220px"
          @change="search"
        />
        <a-select v-model="queryForm.matchStatus" placeholder="匹配状态" allow-clear style="width: 130px" @change="search">
          <a-option value="UNMATCHED">未匹配</a-option>
          <a-option value="MATCHED">已匹配</a-option>
        </a-select>
        <a-button @click="reset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <template #toolbar-right>
        <a-button v-permission="['psi:bank:import']" type="primary" @click="onImport">
          <template #icon><icon-plus /></template>导入流水
        </a-button>
      </template>
      <template #direction="{ record }">
        <a-tag v-if="record.direction === 'IN'" color="green">收入</a-tag>
        <a-tag v-else-if="record.direction === 'OUT'" color="orangered">支出</a-tag>
        <span v-else>{{ record.direction }}</span>
      </template>
      <template #matchStatus="{ record }">
        <a-tag v-if="record.matchStatus === 'UNMATCHED'">未匹配</a-tag>
        <a-tag v-else-if="record.matchStatus === 'MATCHED'" color="green">已匹配</a-tag>
        <a-tag v-else>{{ record.matchStatus }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space>
          <a-link v-permission="['psi:bank:list']" @click="onView(record)">详情</a-link>
          <a-link
            v-if="record.matchStatus === 'UNMATCHED'"
            v-permission="['psi:bank:match']"
            @click="onMatch(record)"
          >
            匹配
          </a-link>
          <a-link v-if="record.matchStatus === 'MATCHED'" v-permission="['psi:bank:match']" status="danger" @click="onUnmatch(record)">解除匹配</a-link>
        </a-space>
      </template>
    </BaseTable>
    <EditModal ref="EditModalRef" @save-success="search" />
    <a-modal v-model:visible="unmatchVisible" title="解除银行流水匹配" :mask-closable="false" @before-ok="submitUnmatch">
      <a-alert type="warning" style="margin-bottom: 16px">只解除本流水与业务单据的对账关联，保留银行流水和业务单据，不会执行银行资金操作。</a-alert>
      <p>银行流水号：{{ unmatchSource?.bankSeqNo || unmatchSource?.id }}</p>
      <p>当前关联：{{ unmatchSource?.matchBizType }} #{{ unmatchSource?.matchBizId }}</p>
      <a-form-item label="解除原因" required><a-textarea v-model="unmatchReason" :max-length="500" show-word-limit placeholder="请输入解除匹配原因" /></a-form-item>
    </a-modal>
  </GiPageLayout>
</template>

<script setup lang="ts">
import type { TableInstance } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { PsiCol } from '../constants/table-col'
import EditModal from './EditModal.vue'
import { type BankStatementResp, listAccountOptions, listBank, unmatchBank } from '@/apis/psi'
import { useTable } from '@/hooks'

defineOptions({ name: 'PsiBank' })

const queryForm = reactive<any>({ sort: ['id,desc'] })
const accountOptions = ref<{ label: string, value: string }[]>([])

const {
  tableData: dataList,
  loading,
  pagination,
  search,
  refresh,
} = useTable((page) => listBank({ ...queryForm, ...page }), { immediate: true })

const columns: TableInstance['columns'] = [
  { title: '账户', dataIndex: 'accountName', width: PsiCol.name },
  { title: '流水日期', dataIndex: 'stmtDate', width: PsiCol.date },
  { title: '方向', dataIndex: 'direction', slotName: 'direction', width: PsiCol.typeSm },
  { title: '金额', dataIndex: 'amount', width: PsiCol.amount },
  { title: '对方', dataIndex: 'counterparty', width: PsiCol.name },
  { title: '摘要', dataIndex: 'summary' },
  { title: '银行流水号', dataIndex: 'bankSeqNo', minWidth: PsiCol.codeMin, ellipsis: false },
  { title: '匹配状态', dataIndex: 'matchStatus', slotName: 'matchStatus', width: PsiCol.status },
  { title: '操作', dataIndex: 'action', slotName: 'action', width: PsiCol.actionS },
]

const loadAccountOptions = async () => {
  const { data } = await listAccountOptions()
  accountOptions.value = (data || []).map((i: any) => ({ label: i.label, value: String(i.value) }))
}

onMounted(() => {
  loadAccountOptions()
})

const reset = () => {
  queryForm.accountId = undefined
  queryForm.matchStatus = undefined
  search()
}

const EditModalRef = ref<InstanceType<typeof EditModal>>()
const onImport = () => EditModalRef.value?.onImport()
const onView = (record: any) => EditModalRef.value?.onView(record.id)
const onMatch = (record: any) => EditModalRef.value?.onMatch(record.id)

const unmatchVisible = ref(false)
const unmatchSource = ref<BankStatementResp>()
const unmatchReason = ref('')
const unmatchKey = ref('')
const onUnmatch = (record: BankStatementResp) => {
  unmatchSource.value = record
  unmatchReason.value = ''
  unmatchKey.value = crypto.randomUUID()
  unmatchVisible.value = true
}
const submitUnmatch = async () => {
  if (!unmatchSource.value || !unmatchReason.value.trim()) { Message.warning('请输入解除原因'); return false }
  try {
    await unmatchBank(unmatchSource.value.id, { reason: unmatchReason.value.trim(), idempotentKey: unmatchKey.value })
    Message.success('匹配已解除')
    search()
    return true
  } catch { return false }
}
</script>
