<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="modalWidth"
    :ok-text="okText"
    :hide-cancel="mode === 'view'"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <!-- 导入流水 -->
    <a-form v-if="mode === 'import'" class="psi-biz-form" :model="importForm" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="银行账户" required>
            <a-select
              v-model="importForm.accountId"
              :options="accountOptions"
              allow-search
              placeholder="请选择账户"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="psi-biz-form__section">
        <div class="psi-biz-form__section-title">
          <h4>流水明细</h4>
          <a-space>
            <a-link @click="downloadExcelTemplate">
              <template #icon><icon-download /></template>下载模板
            </a-link>
            <a-upload
              :show-file-list="false"
              accept=".xls,.xlsx"
              :custom-request="handleExcelUpload"
            >
              <a-button type="outline" size="mini">Excel 导入</a-button>
            </a-upload>
            <a-button type="outline" size="mini" @click="addLine">加行</a-button>
          </a-space>
        </div>
        <a-table class="psi-biz-form__table" :data="importForm.items" :pagination="false" row-key="uid" size="small" :bordered="{ cell: true }">
          <template #columns>
            <a-table-column title="日期" :width="190">
              <template #cell="{ record }">
                <a-date-picker
                  v-model="record.stmtDate"
                  show-time
                  :format="DATETIME_FORMAT"
                  :value-format="DATETIME_FORMAT"
                />
              </template>
            </a-table-column>
            <a-table-column title="方向" :width="100">
              <template #cell="{ record }">
                <a-select v-model="record.direction">
                  <a-option value="IN">收入</a-option>
                  <a-option value="OUT">支出</a-option>
                </a-select>
              </template>
            </a-table-column>
            <a-table-column title="金额" :width="120">
              <template #cell="{ record }">
                <a-input-number v-model="record.amount" :min="0" :precision="2" />
              </template>
            </a-table-column>
            <a-table-column title="对方" :width="140">
              <template #cell="{ record }">
                <a-input v-model="record.counterparty" />
              </template>
            </a-table-column>
            <a-table-column title="摘要" :width="160">
              <template #cell="{ record }">
                <a-input v-model="record.summary" />
              </template>
            </a-table-column>
            <a-table-column title="银行流水号" :width="140">
              <template #cell="{ record }">
                <a-input v-model="record.bankSeqNo" />
              </template>
            </a-table-column>
            <a-table-column title="操作" :width="72">
              <template #cell="{ rowIndex }">
                <a-link status="danger" @click="importForm.items.splice(rowIndex, 1)">删除</a-link>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </div>
    </a-form>

    <!-- 匹配 -->
    <a-form v-else-if="mode === 'match'" class="psi-biz-form" :model="matchForm" layout="vertical">
      <a-alert type="info" style="margin-bottom: 12px">
        收入流水请匹配收款单（RECEIPT），支出流水请匹配付款单（PAYMENT）。候选按金额差≤0.01、日期差≤2天过滤。
      </a-alert>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="业务类型" required>
            <a-select v-model="matchForm.matchBizType" @change="loadCandidates">
              <a-option value="RECEIPT">收款单</a-option>
              <a-option value="PAYMENT">付款单</a-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="业务单据" required>
            <a-select
              v-model="matchForm.matchBizId"
              :options="bizOptions"
              allow-search
              placeholder="请选择收款/付款单"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <!-- 详情 -->
    <a-form v-else class="psi-biz-form" :model="detailForm" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="银行账户">
            <a-select v-model="detailForm.accountId" :options="accountOptions" disabled allow-search />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="流水日期">
            <a-date-picker
              v-model="detailForm.stmtDate"
              show-time
              :format="DATETIME_FORMAT"
              :value-format="DATETIME_FORMAT"
              disabled
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="方向">
            <a-select v-model="detailForm.direction" disabled>
              <a-option value="IN">收入</a-option>
              <a-option value="OUT">支出</a-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="金额">
            <a-input-number v-model="detailForm.amount" :precision="2" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="对方">
            <a-input v-model="detailForm.counterparty" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="银行流水号">
            <a-input v-model="detailForm.bankSeqNo" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="摘要">
            <a-textarea v-model="detailForm.summary" disabled :auto-size="{ minRows: 2, maxRows: 4 }" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="匹配状态">
            <a-input :model-value="detailForm.matchStatus" disabled />
          </a-form-item>
        </a-col>
        <a-col v-if="detailForm.matchBizType" :span="12">
          <a-form-item label="匹配业务">
            <a-input :model-value="`${detailForm.matchBizType} #${detailForm.matchBizId}`" disabled />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { Message, type RequestOption } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { DATETIME_FORMAT } from '@/constant/datetime'
import {
  addBank,
  downloadBankImportTemplate,
  getBank,
  listAccountOptions,
  listBankCandidates,
  matchBank,
  parseImportBank,
} from '@/apis/psi'
import { useDownload } from '@/hooks'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

type Mode = 'import' | 'match' | 'view'
const mode = ref<Mode>('import')
const visible = ref(false)
const dataId = ref('')
const accountOptions = ref<{ label: string, value: string }[]>([])
const bizOptions = ref<{ label: string, value: string }[]>([])

const title = computed(() => {
  if (mode.value === 'import') return '导入银行流水'
  if (mode.value === 'match') return '匹配流水'
  return '流水详情'
})
const okText = computed(() => {
  if (mode.value === 'view') return '关闭'
  if (mode.value === 'import') return '导入'
  return '确认匹配'
})
const modalWidth = computed(() => {
  if (mode.value === 'import') return width.value >= 960 ? 960 : '100%'
  return width.value >= 640 ? 640 : '100%'
})

const importForm = reactive<any>({
  accountId: undefined,
  items: [] as any[],
})

const matchForm = reactive<any>({
  statementId: undefined as number | undefined,
  matchBizType: 'RECEIPT',
  matchBizId: undefined as string | undefined,
})

const detailForm = reactive<any>({
  accountId: undefined,
  stmtDate: undefined,
  direction: 'IN',
  amount: 0,
  counterparty: '',
  summary: '',
  bankSeqNo: '',
  matchStatus: '',
  matchBizType: '',
  matchBizId: undefined,
})

const loadAccountOptions = async () => {
  const { data } = await listAccountOptions()
  accountOptions.value = toLabelValueOpts(data || [])
}

const loadCandidates = async () => {
  if (!matchForm.statementId) return
  const { data } = await listBankCandidates(matchForm.statementId, matchForm.matchBizType)
  bizOptions.value = toLabelValueOpts(data || [])
  matchForm.matchBizId = bizOptions.value[0]?.value
}

const addLine = () => {
  importForm.items.push({
    uid: crypto.randomUUID(),
    stmtDate: undefined,
    direction: 'IN',
    amount: 0,
    counterparty: '',
    summary: '',
    bankSeqNo: '',
  })
}

const downloadExcelTemplate = () => {
  useDownload(() => downloadBankImportTemplate())
}

const handleExcelUpload = (options: RequestOption) => {
  const controller = new AbortController()
  ;(async () => {
    const { onError, onSuccess, fileItem, name = 'file' } = options
    const formData = new FormData()
    formData.append(name as string, fileItem.file as Blob)
    try {
      const res = await parseImportBank(formData)
      const items = res.data?.items || []
      if (!items.length) {
        Message.warning('未解析到有效流水')
        onError(new Error('empty'))
        return
      }
      importForm.items = items.map((i: any) => ({
        uid: crypto.randomUUID(),
        stmtDate: i.stmtDate,
        direction: i.direction || 'IN',
        amount: Number(i.amount),
        counterparty: i.counterparty || '',
        summary: i.summary || '',
        bankSeqNo: i.bankSeqNo || '',
      }))
      Message.success(`已载入 ${items.length} 笔流水，请核对后确认导入`)
      onSuccess(res)
    } catch (e) {
      onError(e)
    }
  })()
  return {
    abort() {
      controller.abort()
    },
  }
}

const reset = () => {
  mode.value = 'import'
  dataId.value = ''
  importForm.accountId = undefined
  importForm.items = []
  matchForm.statementId = undefined
  matchForm.matchBizType = 'RECEIPT'
  matchForm.matchBizId = undefined
  bizOptions.value = []
  detailForm.accountId = undefined
  detailForm.stmtDate = undefined
  detailForm.direction = 'IN'
  detailForm.amount = 0
  detailForm.counterparty = ''
  detailForm.summary = ''
  detailForm.bankSeqNo = ''
  detailForm.matchStatus = ''
  detailForm.matchBizType = ''
  detailForm.matchBizId = undefined
}

const save = async () => {
  if (mode.value === 'view') return true
  try {
    if (mode.value === 'import') {
      if (!importForm.accountId || !importForm.items.length) {
        Message.warning('请选择账户并至少录入一行流水')
        return false
      }
      if (importForm.items.some((i: any) => !i.stmtDate || !i.direction || i.amount == null)) {
        Message.warning('请完善每行日期、方向与金额')
        return false
      }
      const n = await addBank({
        accountId: importForm.accountId,
        items: importForm.items.map((i: any) => ({
          stmtDate: i.stmtDate,
          direction: i.direction,
          amount: i.amount,
          counterparty: i.counterparty || undefined,
          summary: i.summary || undefined,
          bankSeqNo: i.bankSeqNo || undefined,
        })),
      })
      Message.success(`导入成功${n?.data != null ? `，新增 ${n.data} 笔` : ''}`)
    } else if (mode.value === 'match') {
      if (!matchForm.statementId || !matchForm.matchBizType || !matchForm.matchBizId) {
        Message.warning('请完善匹配信息')
        return false
      }
      await matchBank({
        statementId: matchForm.statementId,
        matchBizType: matchForm.matchBizType,
        matchBizId: matchForm.matchBizId,
      })
      Message.success('匹配成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const fillDetail = async (id: string) => {
  const { data } = await getBank(id)
  detailForm.accountId = toBizId(data.accountId)
  detailForm.stmtDate = data.stmtDate
  detailForm.direction = data.direction
  detailForm.amount = Number(data.amount)
  detailForm.counterparty = data.counterparty || ''
  detailForm.summary = data.summary || ''
  detailForm.bankSeqNo = data.bankSeqNo || ''
  detailForm.matchStatus = data.matchStatus || ''
  detailForm.matchBizType = data.matchBizType || ''
  detailForm.matchBizId = toBizId(data.matchBizId)
  ensureSelectOpt(accountOptions.value, detailForm.accountId, (data as any).accountName)
}

const onImport = async () => {
  reset()
  mode.value = 'import'
  addLine()
  await loadAccountOptions()
  visible.value = true
}

const onMatch = async (id: string) => {
  reset()
  mode.value = 'match'
  dataId.value = id
  matchForm.statementId = String(id)
  const { data } = await getBank(id)
  matchForm.matchBizType = data.direction === 'OUT' ? 'PAYMENT' : 'RECEIPT'
  await loadCandidates()
  ensureSelectOpt(bizOptions.value, data.matchBizId, (data as any).matchBizLabel)
  if (data.matchBizId != null) {
    matchForm.matchBizId = toBizId(data.matchBizId)
  }
  visible.value = true
}

const onView = async (id: string) => {
  reset()
  mode.value = 'view'
  dataId.value = id
  await loadAccountOptions()
  await fillDetail(id)
  visible.value = true
}

defineExpose({ onImport, onMatch, onView, onAdd: onImport })
</script>

<style lang="scss">
@import '../styles/psi-form.scss';
</style>
