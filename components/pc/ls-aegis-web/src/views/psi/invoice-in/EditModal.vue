<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 960 ? 960 : '100%'"
    :ok-text="readonly ? '关闭' : '保存草稿'"
    :hide-cancel="readonly"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <a-form class="psi-biz-form" :model="form" layout="vertical">
      <div
        v-if="!readonly"
        style="margin-bottom: 12px; display: flex; gap: 12px; align-items: center; justify-content: flex-end; flex-wrap: wrap;"
      >
        <span style="color: var(--color-text-3); font-size: 12px;">支持 XML / PDF / OFD；货未入完可先保存，未分摊金额须补关联入库后才能确认</span>
        <a-upload :custom-request="handleInvoiceFileUpload" :show-file-list="false" accept=".xml,.pdf,.ofd">
          <a-button type="outline" size="mini">上传税务发票文件</a-button>
        </a-upload>
      </div>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="发票号" required>
            <a-input v-model="form.invoiceNo" :disabled="readonly" placeholder="发票号码" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="发票类型" required>
            <a-select v-model="form.invoiceType" :disabled="readonly">
              <a-option value="SPECIAL">专票</a-option>
              <a-option value="NORMAL">普票</a-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="开票日期" required>
            <a-date-picker
              v-model="form.invoiceDate"
              show-time
              :format="DATETIME_FORMAT"
              :value-format="DATETIME_FORMAT"
              :disabled="readonly"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="供应商" required>
            <a-select
              v-model="form.unitId"
              :options="unitOptions"
              :disabled="readonly"
              allow-search
              placeholder="请选择供应商"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="购方税号">
            <a-input v-model="form.buyerTaxNo" :disabled="readonly" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="销方税号">
            <a-input v-model="form.sellerTaxNo" :disabled="readonly" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="含税金额" required>
            <a-input-number
              v-model="form.taxInclusiveAmount"
              :min="0"
              :precision="2"
              :disabled="readonly"
              @change="recalcHeader"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税率%">
            <a-select v-model="headerTaxRate" :disabled="readonly" @change="recalcHeader">
              <a-option :value="13">13</a-option>
              <a-option :value="9">9</a-option>
              <a-option :value="6">6</a-option>
              <a-option :value="3">3</a-option>
              <a-option :value="1">1</a-option>
              <a-option :value="0">0</a-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="不含税金额" required>
            <a-input-number v-model="form.untaxAmount" :precision="2" :disabled="readonly" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税额" required>
            <a-input-number v-model="form.taxAmount" :precision="2" :disabled="readonly" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="psi-biz-form__section">
        <div class="psi-biz-form__section-title">
          <h4>明细</h4>
        </div>
        <a-table class="psi-biz-form__table" :data="form.items" :pagination="false" row-key="uid" size="small" :bordered="{ cell: true }">
          <template #columns>
            <a-table-column title="货物/应税劳务" :width="180">
              <template #cell="{ record }">
                <a-input v-model="record.goodsName" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column title="数量" :width="100">
              <template #cell="{ record }">
                <a-input-number v-model="record.quantity" :min="0" :precision="4" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column title="含税金额" :width="120">
              <template #cell="{ record }">
                <a-input-number
                  v-model="record.taxInclusiveAmount"
                  :min="0"
                  :precision="2"
                  :disabled="readonly"
                  @change="recalcItem(record)"
                />
              </template>
            </a-table-column>
            <a-table-column title="税率%" :width="90">
              <template #cell="{ record }">
                <a-select v-model="record.taxRate" :disabled="readonly" @change="recalcItem(record)">
                  <a-option :value="13">13</a-option>
                  <a-option :value="9">9</a-option>
                  <a-option :value="6">6</a-option>
                  <a-option :value="3">3</a-option>
                  <a-option :value="1">1</a-option>
                  <a-option :value="0">0</a-option>
                </a-select>
              </template>
            </a-table-column>
            <a-table-column title="不含税" :width="110">
              <template #cell="{ record }">
                <a-input-number v-model="record.untaxAmount" :precision="2" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column title="税额" :width="110">
              <template #cell="{ record }">
                <a-input-number v-model="record.taxAmount" :precision="2" :disabled="readonly" />
              </template>
            </a-table-column>
          </template>
        </a-table>
      </div>

      <div class="psi-biz-form__section">
        <div class="psi-biz-form__section-title">
          <h4>关联入库</h4>
          <div style="display: flex; align-items: center; gap: 12px;">
            <span v-if="unallocatedAmount > 0.05" style="color: rgb(var(--warning-6)); font-size: 12px;">
              未分摊 {{ unallocatedAmount.toFixed(2) }}（仅可保存，确认前须分摊完）
            </span>
            <span v-else-if="Number(form.taxInclusiveAmount) > 0" style="color: var(--color-text-3); font-size: 12px;">
              已分摊齐
            </span>
            <a-button v-if="!readonly" type="outline" size="mini" @click="addLink">加行</a-button>
          </div>
        </div>
        <a-table class="psi-biz-form__table" :data="form.links" :pagination="false" row-key="uid" size="small" :bordered="{ cell: true }">
          <template #columns>
            <a-table-column title="入库单" :width="280">
              <template #cell="{ record }">
                <a-select
                  v-model="record.stockDocId"
                  :options="stockOptions"
                  :disabled="readonly"
                  allow-search
                  placeholder="请选择已确认入库单"
                  @change="(val) => onStockDocChange(record, val)"
                />
              </template>
            </a-table-column>
            <a-table-column title="关联金额" :width="140">
              <template #cell="{ record }">
                <a-input-number v-model="record.amount" :min="0" :precision="2" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column v-if="!readonly" title="操作" :width="72">
              <template #cell="{ rowIndex }">
                <a-link status="danger" @click="form.links.splice(rowIndex, 1)">删除</a-link>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </div>

      <a-form-item label="备注">
        <a-textarea v-model="form.remark" :disabled="readonly" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="选填" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { Message, type RequestOption } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import dayjs from 'dayjs'
import {
  addInvoiceIn,
  getInvoiceIn,
  listStockIn,
  listUnitOptions,
  parseImportInvoiceIn,
  updateInvoiceIn,
} from '@/apis/psi'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()

/** 统一为后端 LocalDateTime 可解析格式，避免导入解析产生的缺秒串（如 2026-08-12T00:00） */
const toApiDateTime = (v: unknown): string | undefined => {
  if (v == null || v === '') return undefined
  const d = dayjs(v as string | Date)
  return d.isValid() ? d.format(DATETIME_FORMAT) : undefined
}

const { width } = useWindowSize()

const visible = ref(false)
const readonly = ref(false)
const dataId = ref('')
const isUpdate = computed(() => !!dataId.value && !readonly.value)
const title = computed(() => (readonly.value ? '进项发票详情' : isUpdate.value ? '修改进项发票' : '新增进项发票'))

const unitOptions = ref<{ label: string, value: string }[]>([])
const stockOptions = ref<{ label: string, value: string }[]>([])
/** 入库单含税总价，用于选择后自动带出关联金额 */
const stockAmountMap = ref<Record<string, number>>({})
const headerTaxRate = ref(13)

const form = reactive<any>({
  invoiceNo: '',
  invoiceType: 'SPECIAL',
  invoiceDate: undefined,
  unitId: undefined,
  buyerTaxNo: '',
  sellerTaxNo: '',
  taxInclusiveAmount: 0,
  untaxAmount: 0,
  taxAmount: 0,
  remark: '',
  links: [] as any[],
  items: [] as any[],
})

const TOLERANCE = 0.05
const linkSum = computed(() =>
  (form.links || []).reduce((s: number, l: any) => s + (Number(l.amount) || 0), 0),
)
const unallocatedAmount = computed(() => {
  const gap = (Number(form.taxInclusiveAmount) || 0) - linkSum.value
  return gap > 0 ? Number(gap.toFixed(2)) : 0
})

const splitAmount = (inclusive: number, rate: number) => {
  const r = Number(rate) || 0
  const taxInclusiveAmount = Number(inclusive) || 0
  const untaxAmount = Number((taxInclusiveAmount / (1 + r / 100)).toFixed(2))
  const taxAmount = Number((taxInclusiveAmount - untaxAmount).toFixed(2))
  return { untaxAmount, taxAmount }
}

const recalcHeader = () => {
  const { untaxAmount, taxAmount } = splitAmount(form.taxInclusiveAmount, headerTaxRate.value)
  form.untaxAmount = untaxAmount
  form.taxAmount = taxAmount
}

const recalcItem = (row: any) => {
  const { untaxAmount, taxAmount } = splitAmount(row.taxInclusiveAmount, row.taxRate)
  row.untaxAmount = untaxAmount
  row.taxAmount = taxAmount
}

const loadOptions = async () => {
  const [u, s] = await Promise.all([
    listUnitOptions('SUPPLIER'),
    listStockIn({ docStatus: 'CONFIRMED', page: 1, size: 200, sort: ['id,desc'] }),
  ])
  unitOptions.value = toLabelValueOpts(u.data || [])
  const amountMap: Record<string, number> = {}
  stockOptions.value = (s.data?.list || []).map((d: any) => {
    const id = String(d.id)
    amountMap[id] = Number(d.taxInclusiveAmount ?? 0)
    return {
      label: `${d.docNo}（¥${d.taxInclusiveAmount ?? 0}）`,
      value: id,
    }
  })
  stockAmountMap.value = amountMap
}

/** 选择入库单后自动带出含税总价，仍可手改 */
const onStockDocChange = (record: any, stockDocId: number | string | undefined) => {
  if (stockDocId == null || stockDocId === '') {
    return
  }
  const total = stockAmountMap.value[String(stockDocId)]
  if (total != null) {
    record.amount = total
  }
}

const addLink = () => {
  form.links.push({ uid: crypto.randomUUID(), stockDocId: undefined, amount: 0 })
}

const handleInvoiceFileUpload = (options: RequestOption) => {
  const controller = new AbortController()
  ;(async () => {
    const { onError, onSuccess, fileItem, name = 'file' } = options
    const formData = new FormData()
    const rawFile = fileItem.file as File | Blob
    const filename = (fileItem.name || (rawFile as File).name || 'invoice.xml') as string
    formData.append(name as string, rawFile, filename)
    try {
      const res = await parseImportInvoiceIn(formData)
      const data = res.data || {}
      form.invoiceNo = data.invoiceNo || ''
      form.invoiceType = data.invoiceType || 'SPECIAL'
      form.invoiceDate = toApiDateTime(data.invoiceDate)
      form.unitId = toBizId(data.unitId)
      ensureSelectOpt(unitOptions.value, form.unitId, (data as any).unitName)
      form.buyerTaxNo = data.buyerTaxNo || ''
      form.sellerTaxNo = data.sellerTaxNo || ''
      form.taxInclusiveAmount = Number(data.taxInclusiveAmount ?? 0)
      form.untaxAmount = Number(data.untaxAmount ?? 0)
      form.taxAmount = Number(data.taxAmount ?? 0)
      if (data.remark) {
        form.remark = data.remark
      }
      form.items = (data.items || []).map((i: any) => ({
        uid: crypto.randomUUID(),
        goodsName: i.goodsName || '',
        quantity: Number(i.quantity ?? 1),
        taxInclusiveAmount: Number(i.taxInclusiveAmount ?? 0),
        untaxAmount: Number(i.untaxAmount ?? 0),
        taxAmount: Number(i.taxAmount ?? 0),
        taxRate: Number(i.taxRate ?? 13),
      }))
      if (form.items.length) {
        headerTaxRate.value = Number(form.items[0].taxRate ?? 13)
      }
      if (!form.links.length) {
        addLink()
      }
      Message.success('已解析并填入发票信息，请核对后补选关联入库单')
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
  form.invoiceNo = ''
  form.invoiceType = 'SPECIAL'
  form.invoiceDate = undefined
  form.unitId = undefined
  form.buyerTaxNo = ''
  form.sellerTaxNo = ''
  form.taxInclusiveAmount = 0
  form.untaxAmount = 0
  form.taxAmount = 0
  form.remark = ''
  form.links = []
  form.items = []
  headerTaxRate.value = 13
  dataId.value = ''
  readonly.value = false
}

const save = async () => {
  if (readonly.value) return true
  try {
    if (!form.invoiceNo || !form.invoiceType || !form.invoiceDate || !form.unitId) {
      Message.warning('请完善发票号、类型、日期与供应商')
      return false
    }
    const validLinks = (form.links || []).filter((l: any) => l.stockDocId)
    if ((form.links || []).some((l: any) => !l.stockDocId)) {
      Message.warning('请完善关联入库单或删除空行')
      return false
    }
    const sum = validLinks.reduce((s: number, l: any) => s + (Number(l.amount) || 0), 0)
    if (sum - (Number(form.taxInclusiveAmount) || 0) > TOLERANCE) {
      Message.warning('关联金额不能超过发票含税金额')
      return false
    }
    const payload = {
      invoiceNo: form.invoiceNo,
      invoiceType: form.invoiceType,
      invoiceDate: toApiDateTime(form.invoiceDate),
      unitId: form.unitId,
      buyerTaxNo: form.buyerTaxNo || undefined,
      sellerTaxNo: form.sellerTaxNo || undefined,
      taxInclusiveAmount: form.taxInclusiveAmount,
      untaxAmount: form.untaxAmount,
      taxAmount: form.taxAmount,
      remark: form.remark,
      links: validLinks.map((l: any) => ({ stockDocId: l.stockDocId, amount: l.amount })),
      items: form.items.length
        ? form.items.map((i: any) => ({
          goodsName: i.goodsName,
          quantity: i.quantity,
          taxInclusiveAmount: i.taxInclusiveAmount,
          untaxAmount: i.untaxAmount,
          taxAmount: i.taxAmount,
          taxRate: i.taxRate,
        }))
        : undefined,
    }
    if (isUpdate.value) {
      await updateInvoiceIn(payload, dataId.value)
      Message.success(unallocatedAmount.value > TOLERANCE ? '已保存（尚有未分摊，确认前请补关联）' : '修改成功')
    } else {
      await addInvoiceIn(payload)
      Message.success(unallocatedAmount.value > TOLERANCE ? '已保存（尚有未分摊，确认前请补关联）' : '新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const fill = async (id: string) => {
  const { data } = await getInvoiceIn(id)
  form.invoiceNo = data.invoiceNo
  form.invoiceType = data.invoiceType
  form.invoiceDate = toApiDateTime(data.invoiceDate)
  form.unitId = toBizId(data.unitId)
  form.buyerTaxNo = data.buyerTaxNo || ''
  form.sellerTaxNo = data.sellerTaxNo || ''
  form.taxInclusiveAmount = Number(data.taxInclusiveAmount)
  form.untaxAmount = Number(data.untaxAmount)
  form.taxAmount = Number(data.taxAmount)
  form.remark = data.remark || ''
  form.links = (data.links || []).map((l: any) => ({
    uid: crypto.randomUUID(),
    stockDocId: toBizId(l.stockDocId),
    amount: Number(l.amount),
  }))
  form.items = (data.items || []).map((i: any) => ({
    uid: crypto.randomUUID(),
    goodsName: i.goodsName,
    quantity: Number(i.quantity ?? 0),
    taxInclusiveAmount: Number(i.taxInclusiveAmount),
    untaxAmount: Number(i.untaxAmount),
    taxAmount: Number(i.taxAmount),
    taxRate: Number(i.taxRate ?? 13),
  }))
  ensureSelectOpt(unitOptions.value, form.unitId, (data as any).unitName)
  ;(data.links || []).forEach((l: any) => ensureSelectOpt(stockOptions.value, l.stockDocId, l.stockDocNo))
}

const open = async () => {
  await loadOptions()
  visible.value = true
}

const onAdd = async () => {
  reset()
  addLink()
  await open()
}
const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  await loadOptions()
  await fill(id)
  visible.value = true
}
const onView = async (id: string) => {
  reset()
  readonly.value = true
  dataId.value = id
  await loadOptions()
  await fill(id)
  visible.value = true
}

defineExpose({ onAdd, onUpdate, onView })
</script>

<style lang="scss">
@import '../styles/psi-form.scss';
</style>
