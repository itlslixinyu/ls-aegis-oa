<template>
  <GiPageLayout>
    <div class="quote-form">
      <div class="quote-form__head">
        <a-space>
          <GiCellTag :value="form.status" :dict="eoms_quote_status" />
          <span v-if="locked" class="quote-form__lock-tip">已锁定，仅可查看与导出</span>
        </a-space>
        <a-space>
          <a-button
            v-if="canConfirm"
            v-permission="['quote:update']"
            type="outline"
            :loading="saving"
            @click="changeStatus('CONFIRM', '确认')"
          >
            确认
          </a-button>
          <a-button
            v-if="canCancel"
            v-permission="['quote:update']"
            status="danger"
            :loading="saving"
            @click="changeStatus('CANCEL', '取消')"
          >
            取消报价
          </a-button>
          <a-button
            v-if="canRevertDraft"
            v-permission="['quote:update']"
            :loading="saving"
            @click="changeStatus('DRAFT', '退回草稿')"
          >
            退回草稿
          </a-button>
          <a-button v-if="dataId" v-permission="['quote:export']" :disabled="!quoteReady || saving" @click="onExportWord">导出 Word</a-button>
          <a-button v-if="dataId" v-permission="['quote:export']" :disabled="!quoteReady || saving" @click="onExportPdf">导出 PDF</a-button>
          <a-button
            v-if="canSubmit"
            v-permission="['quote:update']"
            :loading="saving"
            @click="changeStatus('SUBMIT', '提交')"
          >
            提交
          </a-button>
          <a-button
            v-if="!locked"
            v-permission="['quote:add', 'quote:update']"
            type="primary"
            :loading="saving || detailLoading"
            :disabled="!quoteReady"
            @click="save"
          >
            保存
          </a-button>
          <a-button @click="goBack">返回列表</a-button>
        </a-space>
      </div>

      <a-alert v-if="!detailLoading && !quoteReady" type="warning">报价单未加载成功，请返回列表后重试。</a-alert>

      <GoodsAddModal ref="GoodsAddModalRef" @save-success="onGoodsCreated" />

      <div class="quote-form__body">
        <section class="quote-form__panel quote-form__panel--entry">
          <header class="quote-form__panel-head">
            <span>单据录入</span>
          </header>
          <div class="quote-form__panel-body">
            <div class="quote-form__section">
              <table class="quote-form__info-table">
                <tbody>
                  <tr>
                    <td>项目名称</td>
                    <td>
                      <a-input
                        v-model="form.projectName"
                        placeholder="请输入项目名称"
                        allow-clear
                        :disabled="locked || !quoteReady"
                      />
                    </td>
                    <td>报价单号</td>
                    <td>{{ form.quoteNo || '保存后自动生成' }}</td>
                  </tr>
                  <tr>
                    <td>报价单位</td>
                    <td>
                      <a-input
                        v-model="form.quoteUnit"
                        placeholder="报价单位全称"
                        allow-clear
                        :disabled="locked || !quoteReady"
                      />
                    </td>
                    <td>报价时间</td>
                    <td>
                      <a-date-picker
                        v-model="form.quoteDate"
                        show-time
                        :format="DATETIME_FORMAT"
                        :value-format="DATETIME_FORMAT"
                        :disabled="locked || !quoteReady"
                        style="width: 100%"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td>增值税综合税率(%)</td>
                    <td>
                      <a-input-number
                        v-model="form.taxRate"
                        :min="0"
                        :max="100"
                        :precision="2"
                        :disabled="locked || !quoteReady"
                        style="width: 100%"
                      />
                    </td>
                    <td>报价有效期</td>
                    <td>
                      <a-date-picker
                        v-model="form.validUntil"
                        show-time
                        :format="DATETIME_FORMAT"
                        :value-format="DATETIME_FORMAT"
                        :disabled="locked || !quoteReady"
                        style="width: 100%"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td>单据状态</td>
                    <td colspan="3" class="quote-form__status-cell">
                      <GiCellTag :value="form.status" :dict="eoms_quote_status" />
                      <span class="quote-form__status-hint">
                        草稿可提交/确认/取消；已提交可退回草稿、确认或取消；已确认与已取消不可再改
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <QuoteLines kind="MATERIAL" :lines="materialLines" :subtotal="materialSubtotal"
              :locked="locked" :ready="quoteReady" :unit-options="unitOptions" :goods-options="goodsOptions"
              :goods-loading="goodsLoading" @add="addMaterialRow" @remove="materialLines.splice($event, 1)"
              @recalc="recalc" @goods-change="onMaterialGoodsChange" @goods-popup="onGoodsPopup" @add-goods="openAddGoods" />
            <QuoteLines kind="SERVICE" :lines="serviceLines" :subtotal="serviceSubtotal"
              :locked="locked" :ready="quoteReady" :unit-options="serviceUnitOptions"
              @add="addServiceRow" @remove="serviceLines.splice($event, 1)" @recalc="recalc" />

            <div class="quote-form__section">
              <h3 class="quote-form__section-title">三、费用合计</h3>
              <table class="quote-form__info-table">
                <tbody>
                  <tr>
                    <td>材料小计</td>
                    <td>{{ formatMoney(materialSubtotal) }}</td>
                    <td>税前合计</td>
                    <td>{{ formatMoney(preTaxTotal) }}</td>
                  </tr>
                  <tr>
                    <td>技术服务费小计</td>
                    <td>{{ formatMoney(serviceSubtotal) }}</td>
                    <td>增值税综合税率 {{ taxRateDisplay }}%</td>
                    <td>{{ formatMoney(taxAmount) }}</td>
                  </tr>
                  <tr>
                    <td>总价(含税)</td>
                    <td>{{ formatMoney(totalWithTax) }}</td>
                    <td>人民币大写</td>
                    <td>{{ amountUpper }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="quote-form__section quote-form__section--last">
              <h3 class="quote-form__section-title">四、备注 / 条款</h3>
              <a-textarea
                v-model="form.terms"
                :auto-size="{ minRows: 4, maxRows: 8 }"
                placeholder="可填写报价有效期、付款方式等条款"
                :disabled="locked || !quoteReady"
              />
            </div>
          </div>
        </section>

        <section class="quote-form__panel quote-form__panel--preview-panel">
          <header class="quote-form__panel-head">
            <span>导出预览</span>
          </header>
          <QuotePreviewPane :html="previewHtml" />
        </section>
      </div>
    </div>
  </GiPageLayout>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { Message, Modal } from '@arco-design/web-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addQuote,
  buildQuoteExportFileName,
  downloadQuoteDocx,
  downloadQuotePdf,
  getQuote,
  updateQuote,
} from '@/apis/quote'
import { getGoods, listGoodsOptions } from '@/apis/psi'
import GiCellTag from '@/components/GiCell/GiCellTag.vue'
import { DATETIME_FORMAT } from '@/constant/datetime'
import QuoteLines from './components/QuoteLines.vue'
import QuotePreviewPane from './components/QuotePreviewPane.vue'
import type { QuoteLine as Line } from './quote-line'
import { useDownload } from '@/hooks'
import { useDict } from '@/hooks/app'
import { useAppStore } from '@/stores'
import { toBizId } from '@/utils/biz-id'
import GoodsAddModal from '@/views/psi/goods/AddModal.vue'
import { buildQuotePreviewHtml } from './quotePreview'

defineOptions({ name: 'QuoteForm' })



type QuoteStatus = 'DRAFT' | 'SUBMIT' | 'CONFIRM' | 'CANCEL'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const { eoms_quote_status } = useDict('eoms_quote_status')

const dataId = computed(() => String(route.query.id || ''))
const saving = ref(false)
let detailRequestVersion = 0
const detailLoading = ref(false)
const loadedQuoteId = ref<string>()
const quoteReady = computed(() => !detailLoading.value && loadedQuoteId.value === dataId.value)

const unitOptions = [
  { label: '台', value: '台' },
  { label: '套', value: '套' },
  { label: '个', value: '个' },
  { label: '只', value: '只' },
  { label: '件', value: '件' },
  { label: '条', value: '条' },
  { label: '根', value: '根' },
  { label: '块', value: '块' },
  { label: '片', value: '片' },
  { label: '张', value: '张' },
  { label: '支', value: '支' },
  { label: '组', value: '组' },
  { label: '对', value: '对' },
  { label: '副', value: '副' },
  { label: '箱', value: '箱' },
  { label: '盒', value: '盒' },
  { label: '包', value: '包' },
  { label: '卷', value: '卷' },
  { label: '米', value: '米' },
  { label: '平方米', value: '平方米' },
  { label: '路', value: '路' },
  { label: '点', value: '点' },
  { label: '端口', value: '端口' },
  { label: '节点', value: '节点' },
  { label: '模块', value: '模块' },
  { label: '许可', value: '许可' },
  { label: '项', value: '项' },
  { label: '批', value: '批' },
  { label: '次', value: '次' },
]
const serviceUnitOptions = [
  { label: '项', value: '项' },
  { label: '人天', value: '人天' },
  { label: '人月', value: '人月' },
  { label: '次', value: '次' },
  { label: '天', value: '天' },
  { label: '小时', value: '小时' },
  { label: '套', value: '套' },
  { label: '批', value: '批' },
  { label: '年', value: '年' },
  { label: '月', value: '月' },
  { label: '场', value: '场' },
]

const defaultTerms = `1. 本报价单自出具之日起7个自然日内有效，有效期届满本报价自动失效，如需沿用本报价，需经供需双方重新书面确认。
2. 报价为含税价，包含本市范围内运费，无其他额外费用。
3. 付款方式：合同签订后预付 30%，设备到货后支付 50%，验收合格后支付余款 20%。`

const calcValidUntil = (quoteDate?: unknown) => {
  const base = quoteDate ? dayjs(quoteDate as string) : dayjs()
  return base.add(7, 'day').format(DATETIME_FORMAT)
}

const syncingValidUntil = ref(false)

const form = reactive<Record<string, any>>({
  status: 'DRAFT' as QuoteStatus,
  taxRate: 4,
  quoteDate: dayjs().format(DATETIME_FORMAT),
  validUntil: calcValidUntil(),
  quoteUnit: '',
  terms: defaultTerms,
})

type GoodsOpt = { label: string, value: string, stockQty?: number }

const goodsOptions = ref<GoodsOpt[]>([])
const goodsRequestVersions = new WeakMap<Line, number>()
// 商品选项供展示/回显使用；库存仍以 PSI 接口和提交时的服务端校验为准。
const goodsById = computed(() => {
  const index = new Map<string, GoodsOpt>()
  for (const option of goodsOptions.value) {
    if (!index.has(option.value)) index.set(option.value, option)
  }
  return index
})
const goodsLoading = ref(false)
const GoodsAddModalRef = ref<InstanceType<typeof GoodsAddModal>>()
const pendingGoodsLineUid = ref<string>()
/** 防止空列表时重复弹出新增商品 */
const goodsPrompted = ref(false)

const promptCreateGoods = (record?: Line) => {
  if (locked.value || goodsPrompted.value) return
  goodsPrompted.value = true
  if (record) {
    pendingGoodsLineUid.value = record.uid
  }
  // 全部仓库无库存商品时直接弹出新增录入
  GoodsAddModalRef.value?.onAdd()
}

const clearMaterialGoodsFields = (row: Line) => {
  row.goodsId = undefined
  row.itemName = undefined
  row.brandModel = undefined
  row.unit = '台'
  row.price = 0
  row.quantity = 1
  row.amount = 0
}

const toGoodsOpts = (list: Array<{ label?: string, value?: unknown, stockQty?: unknown }> | null | undefined): GoodsOpt[] =>
  (list || []).map((i) => {
    const rawQty = i.stockQty
    const stockQty = rawQty == null || rawQty === '' ? undefined : Number(rawQty)
    return {
      label: String(i.label ?? i.value ?? ''),
      value: String(i.value ?? ''),
      // 仅接口明确返回库存时才写入；缺省不算「无库存」
      stockQty: stockQty != null && !Number.isNaN(stockQty) ? stockQty : undefined,
    }
  }).filter((o) => o.value && o.value !== 'undefined' && o.value !== 'null')

/** 加载可见库存商品；新增录入仅由用户选择材料时触发，避免打断服务报价与导出。 */
const loadGoodsOptions = async () => {
  goodsLoading.value = true
  try {
    const { data } = await listGoodsOptions(undefined, { stockAcrossWarehouses: true })
    goodsOptions.value = toGoodsOpts(data)
    const availableIds = new Set(goodsOptions.value.map((option) => option.value))
    // 编辑回显：已选材料即使暂无库存也保留选项（库存记 0）
    materialLines.value.forEach((row) => {
      if (row.goodsId && !availableIds.has(row.goodsId)) {
        goodsOptions.value.push({
          label: row.itemName || row.goodsId,
          value: row.goodsId,
          stockQty: 0,
        })
        availableIds.add(row.goodsId)
      }
    })
  } catch {
    goodsOptions.value = []
  } finally {
    goodsLoading.value = false
  }
}

const onGoodsPopup = (visible: boolean, record?: Line) => {
  if (!visible || locked.value) return
  if (!goodsOptions.value.length) {
    promptCreateGoods(record)
  }
}

const openAddGoods = (record?: Line) => {
  if (locked.value) return
  goodsPrompted.value = false
  if (record) {
    pendingGoodsLineUid.value = record.uid
  }
  GoodsAddModalRef.value?.onAdd()
}

const tipIfNoStock = (_goodsId: string, stockQty?: number) => {
  // 仅当接口明确返回库存且为 0 时提示；缺省/未知不提示（避免旧后端无 stockQty 误报）
  if (stockQty !== undefined && stockQty <= 0) {
    Message.warning('所选商品目前无库存，允许继续选用。')
  }
}

const applyGoodsToLine = async (record: Line, goodsId?: string) => {
  const version = (goodsRequestVersions.get(record) || 0) + 1
  goodsRequestVersions.set(record, version)
  const id = toBizId(goodsId)
  if (!id) {
    clearMaterialGoodsFields(record)
    return
  }
  record.goodsId = id
  const opt = goodsById.value.get(id)
  if (opt?.label) {
    const idx = opt.label.lastIndexOf('（')
    record.itemName = idx > 0 ? opt.label.slice(0, idx) : opt.label
  }
  if (!opt) {
    goodsOptions.value.push({ label: record.itemName || id, value: id, stockQty: 0 })
    tipIfNoStock(id, 0)
  } else {
    tipIfNoStock(id, opt.stockQty)
  }
  try {
    const { data } = await getGoods(id)
    if (goodsRequestVersions.get(record) !== version || record.goodsId !== id) return
    record.itemName = data.goodsName || record.itemName
    record.brandModel = data.spec || record.brandModel
    if (data.unit) {
      record.unit = data.unit
    }
    if (data.salePrice != null && Number(data.salePrice) >= 0) {
      record.price = Number(data.salePrice)
    }
    const exist = goodsById.value.get(id)
    const label = `${data.goodsName || ''}${data.goodsCode ? `（${data.goodsCode}）` : ''}`
    if (exist) {
      exist.label = label || exist.label
    } else {
      goodsOptions.value.push({ label: label || id, value: id, stockQty: 0 })
    }
    recalc(record)
  } catch {
    if (goodsRequestVersions.get(record) === version) recalc(record)
  }
}

const onMaterialGoodsChange = (record: Line, goodsId?: string) => {
  void applyGoodsToLine(record, goodsId)
}

const onGoodsCreated = async (id?: string) => {
  goodsPrompted.value = false
  await loadGoodsOptions()
  const lineUid = pendingGoodsLineUid.value
  pendingGoodsLineUid.value = undefined
  if (!id || !lineUid) return
  const line = materialLines.value.find((row) => row.uid === lineUid)
  if (line) {
    // 新建商品尚无库存：写入当前行并提示可继续选用
    await applyGoodsToLine(line, id)
  }
}

const currentStatus = computed(() => String(form.status || 'DRAFT').toUpperCase() as QuoteStatus)
const locked = computed(() => currentStatus.value === 'CONFIRM' || currentStatus.value === 'CANCEL')
const canSubmit = computed(() => quoteReady.value && !!dataId.value && currentStatus.value === 'DRAFT')
const canRevertDraft = computed(() => quoteReady.value && !!dataId.value && currentStatus.value === 'SUBMIT')
const canConfirm = computed(() =>
  quoteReady.value && !!dataId.value && (currentStatus.value === 'DRAFT' || currentStatus.value === 'SUBMIT'),
)
const canCancel = computed(() =>
  quoteReady.value && !!dataId.value && (currentStatus.value === 'DRAFT' || currentStatus.value === 'SUBMIT'),
)

const materialLines = ref<Line[]>([])
const serviceLines = ref<Line[]>([])

const createLine = (lineType: 'MATERIAL' | 'SERVICE'): Line => ({
  uid: crypto.randomUUID(),
  lineType,
  unit: lineType === 'MATERIAL' ? '台' : '项',
  quantity: 1,
  price: 0,
  amount: 0,
})

const ensureDefaultRows = () => {
  if (locked.value) return
  if (!materialLines.value.length) {
    materialLines.value = Array.from({ length: 10 }, () => createLine('MATERIAL'))
  }
  if (!serviceLines.value.length) {
    serviceLines.value = [createLine('SERVICE')]
  }
}

const addMaterialRow = () => {
  materialLines.value.push(createLine('MATERIAL'))
}
const addServiceRow = () => serviceLines.value.push(createLine('SERVICE'))

const recalc = (record: Line) => {
  const qty = Number(record.quantity || 0)
  const price = Number(record.price || 0)
  record.amount = Math.round(qty * price * 100) / 100
}

const sumLines = (lines: Line[]) =>
  lines.reduce((sum, row) => sum + Number(row.amount || 0), 0)

const materialSubtotal = computed(() => sumLines(materialLines.value))
const serviceSubtotal = computed(() => sumLines(serviceLines.value))
const preTaxTotal = computed(() => materialSubtotal.value + serviceSubtotal.value)
const taxRateDisplay = computed(() => Number(form.taxRate ?? 4))
const taxAmount = computed(() => Math.round(preTaxTotal.value * taxRateDisplay.value) / 100)
const totalWithTax = computed(() => Math.round((preTaxTotal.value + taxAmount.value) * 100) / 100)

const digitUppercase = (n: number) => {
  const fraction = ['角', '分']
  const digit = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const unit = [['元', '万', '亿'], ['', '拾', '佰', '仟']]
  const head = n < 0 ? '负' : ''
  n = Math.abs(n)
  let s = ''
  for (let i = 0; i < fraction.length; i++) {
    s += (digit[Math.floor(n * 10 * 10 ** i) % 10] + fraction[i]).replace(/(零.)+/g, '')
  }
  s = s || '整'
  n = Math.floor(n)
  for (let i = 0; i < unit[0].length && n > 0; i++) {
    let p = ''
    for (let j = 0; j < unit[1].length && n > 0; j++) {
      p = digit[n % 10] + unit[1][j] + p
      n = Math.floor(n / 10)
    }
    s = p.replace(/(零.)*零$/g, '').replace(/^$/, '零') + unit[0][i] + s
  }
  return head + s.replace(/(零.)*零元/, '元').replace(/(零.)+/g, '零').replace(/^整$/, '零元整')
}

const amountUpper = computed(() => digitUppercase(totalWithTax.value))

const previewHtml = computed(() => buildQuotePreviewHtml({
  projectName: String(form.projectName || '').trim(),
  quoteNo: form.quoteNo,
  quoteUnit: form.quoteUnit,
  quoteDate: form.quoteDate,
  taxRate: form.taxRate,
  terms: form.terms,
  materialSubtotal: materialSubtotal.value,
  serviceSubtotal: serviceSubtotal.value,
  preTaxTotal: preTaxTotal.value,
  taxAmount: taxAmount.value,
  totalWithTax: totalWithTax.value,
  amountUpper: amountUpper.value,
  materialLines: materialLines.value,
  serviceLines: serviceLines.value,
  logoUrl: appStore.getLogo(),
}))

const formatMoney = (val?: number | null) => Number(val ?? 0).toFixed(2)

const buildDetails = () => {
  const rows: Line[] = []
  materialLines.value.forEach((row, idx) => {
    let itemName = String(row.itemName || '').trim()
    if (!itemName && row.goodsId) {
      const opt = goodsById.value.get(row.goodsId)
      if (opt?.label) {
        const i = opt.label.lastIndexOf('（')
        itemName = i > 0 ? opt.label.slice(0, i) : opt.label
      }
    }
    if (!itemName) return
    rows.push({
      ...row,
      itemName,
      lineType: 'MATERIAL',
      goodsId: toBizId(row.goodsId),
      sortNo: idx + 1,
    })
  })
  serviceLines.value.forEach((row, idx) => {
    if (!String(row.itemName || '').trim()) return
    rows.push({ ...row, lineType: 'SERVICE', goodsId: undefined, sortNo: idx + 1 })
  })
  return rows
}

const buildPayload = (status?: QuoteStatus) => ({
  ...form,
  status: status || form.status || 'DRAFT',
  quoteDate: form.quoteDate ? dayjs(form.quoteDate).format(DATETIME_FORMAT) : undefined,
  validUntil: form.validUntil ? dayjs(form.validUntil).format(DATETIME_FORMAT) : undefined,
  materialSubtotal: materialSubtotal.value,
  serviceSubtotal: serviceSubtotal.value,
  totalAmount: totalWithTax.value,
  details: buildDetails(),
})

const save = async () => {
  if (saving.value) return
  if (!quoteReady.value) {
    Message.warning('报价单尚未加载成功，请重新打开后再操作')
    return
  }
  if (locked.value) {
    Message.warning('已确认或已取消的报价单不可修改')
    return
  }
  if (!String(form.projectName || '').trim()) {
    Message.warning('请填写项目名称')
    return
  }
  saving.value = true
  const quoteId = dataId.value
  const version = detailRequestVersion
  try {
    const payload = buildPayload()
    if (quoteId) {
      await updateQuote({ ...payload, id: quoteId })
      if (version !== detailRequestVersion || quoteId !== dataId.value) return
      Message.success('保存成功')
      await loadDetail()
    } else {
      const { data: newId } = await addQuote(payload)
      if (version !== detailRequestVersion || quoteId !== dataId.value) return
      Message.success('保存成功')
      if (newId) {
        await router.replace({ path: '/quote/form', query: { id: String(newId) } })
      } else {
        router.push('/quote/index')
      }
    }
  } finally {
    saving.value = false
  }
}

const changeStatus = (status: QuoteStatus, actionLabel: string) => {
  if (saving.value) return
  if (!quoteReady.value || locked.value) {
    Message.warning('当前报价单不可操作，请重新打开后再试')
    return
  }
  if (!dataId.value) {
    Message.warning('请先保存报价单')
    return
  }
  if (!String(form.projectName || '').trim()) {
    Message.warning('请填写项目名称')
    return
  }
  const quoteId = dataId.value
  const version = detailRequestVersion
  Modal.confirm({
    title: `确认${actionLabel}`,
    content: status === 'CONFIRM' || status === 'CANCEL'
      ? `${actionLabel}后单据将锁定，不可再修改内容。是否继续？`
      : `确认将报价单${actionLabel}？`,
    onOk: async () => {
      if (saving.value || !quoteReady.value || version !== detailRequestVersion || quoteId !== dataId.value) {
        Message.warning('报价单已切换，请在当前单据重新操作')
        return
      }
      saving.value = true
      try {
        await updateQuote({ ...buildPayload(status), id: quoteId })
        if (version !== detailRequestVersion || quoteId !== dataId.value) return
        Message.success(`${actionLabel}成功`)
        await loadDetail()
      } finally {
        saving.value = false
      }
    },
  })
}

const resetQuoteForm = () => {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, {
    status: 'DRAFT', taxRate: 4, quoteUnit: appStore.getCompany() || '',
    quoteDate: dayjs().format(DATETIME_FORMAT), terms: defaultTerms,
  })
  form.validUntil = calcValidUntil(form.quoteDate)
  materialLines.value = []
  serviceLines.value = []
  ensureDefaultRows()
}

const loadDetail = async () => {
  const version = ++detailRequestVersion
  const quoteId = dataId.value
  detailLoading.value = true
  loadedQuoteId.value = undefined
  syncingValidUntil.value = true
  goodsPrompted.value = false
  pendingGoodsLineUid.value = undefined
  resetQuoteForm()
  try {
    if (!quoteId) {
      await loadGoodsOptions()
      if (version === detailRequestVersion && quoteId === dataId.value) loadedQuoteId.value = quoteId
      return
    }
    const { data } = await getQuote(quoteId)
    if (version !== detailRequestVersion || quoteId !== dataId.value) return
    if (!data || String(data.id) !== quoteId) {
      Message.warning('报价单不存在或无权访问')
      return
    }
    Object.assign(form, data)
    form.status = String(data.status || 'DRAFT').toUpperCase()
    form.quoteDate = data.quoteDate
      ? dayjs(data.quoteDate).format(DATETIME_FORMAT)
      : dayjs().format(DATETIME_FORMAT)
    form.validUntil = data.validUntil
      ? dayjs(data.validUntil).format(DATETIME_FORMAT)
      : calcValidUntil(form.quoteDate)
    form.taxRate = data.taxRate ?? 4
    form.terms = data.terms || defaultTerms
    if (!form.quoteUnit) {
      form.quoteUnit = appStore.getCompany() || ''
    }
    materialLines.value = (data.details || [])
      .filter((d) => d.lineType === 'MATERIAL')
      .map((row) => ({
        uid: String(row.id ?? crypto.randomUUID()),
        lineType: 'MATERIAL' as const,
        goodsId: toBizId(row.goodsId),
        itemName: row.itemName,
        brandModel: row.brandModel,
        unit: row.unit || '台',
        quantity: Number(row.quantity ?? 1),
        price: Number(row.price ?? 0),
        amount: Number(row.amount ?? 0),
      }))
    serviceLines.value = (data.details || [])
      .filter((d) => d.lineType !== 'MATERIAL')
      .map((row) => ({
        uid: String(row.id ?? crypto.randomUUID()),
        lineType: 'SERVICE' as const,
        itemName: row.itemName,
        description: row.description,
        unit: row.unit || '项',
        quantity: Number(row.quantity ?? 1),
        price: Number(row.price ?? 0),
        amount: Number(row.amount ?? 0),
      }))
    ensureDefaultRows()
    await loadGoodsOptions()
    if (version === detailRequestVersion && quoteId === dataId.value) loadedQuoteId.value = quoteId
  } catch {
    // 请求错误由统一拦截器反馈；不可保留上一张报价或恢复写操作。
  } finally {
    if (version === detailRequestVersion && quoteId === dataId.value) {
      detailLoading.value = false
      syncingValidUntil.value = false
    }
  }
}

const goBack = () => router.push('/quote/index')

const onExportWord = async () => {
  const quoteId = dataId.value
  if (!quoteId || !quoteReady.value || saving.value) return
  await useDownload(
    () => downloadQuoteDocx(quoteId),
    buildQuoteExportFileName(form.projectName, form.quoteNo, '.docx'),
    '.docx',
  )
}

const onExportPdf = async () => {
  const quoteId = dataId.value
  if (!quoteId || !quoteReady.value || saving.value) return
  await useDownload(
    () => downloadQuotePdf(quoteId),
    buildQuoteExportFileName(form.projectName, form.quoteNo, '.pdf'),
    '.pdf',
  )
}

onMounted(loadDetail)
watch(() => route.query.id, loadDetail)
watch(() => form.quoteDate, (date) => {
  if (!date || syncingValidUntil.value || locked.value) return
  form.validUntil = calcValidUntil(date)
})
onUnmounted(() => {
  detailRequestVersion++
  loadedQuoteId.value = undefined
})
ensureDefaultRows()
</script>

<style scoped>
.quote-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
}
.quote-form__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}
.quote-form__lock-tip {
  color: var(--color-text-3);
  font-size: 12px;
}
.quote-form__status-cell {
  text-align: left !important;
}
.quote-form__status-hint {
  margin-left: 12px;
  color: var(--color-text-3);
  font-size: 11px;
}
.quote-form__body {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
  align-items: stretch;
}
.quote-form__panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-2);
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  overflow: hidden;
}
.quote-form__panel--entry {
  flex: 0 0 60%;
  width: 60%;
}
.quote-form__panel--preview-panel {
  flex: 0 0 40%;
  width: 40%;
}
.quote-form__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border-2);
  background: var(--color-fill-1);
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}
.quote-form__panel-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px 0;
}
.quote-form__section {
  padding: 12px 16px 16px;
  border-bottom: 1px solid var(--color-border-2);
}
.quote-form__section--last {
  border-bottom: none;
}
.quote-form__info-table {
  width: 100%;
  border-collapse: collapse;
}
.quote-form__info-table td {
  border: 1px solid var(--color-border-2);
  padding: 6px 8px;
  vertical-align: middle;
  text-align: center;
  font-size: 11px;
}
.quote-form__info-table td:nth-child(odd) {
  width: 120px;
  background: var(--color-fill-1);
}
.quote-form__section-title { margin: 0 0 8px; font-size: 14px; }
@media (max-width: 1200px) {
  .quote-form__body {
    flex-direction: column;
  }
  .quote-form__panel {
    width: 100%;
    flex: none;
  }
}
</style>
