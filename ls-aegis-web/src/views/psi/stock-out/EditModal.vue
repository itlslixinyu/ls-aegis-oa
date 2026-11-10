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
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="出库类型" required>
            <a-select v-model="form.outType" :disabled="readonly || form.outType === 'REPAIR'" @change="onOutTypeChange">
              <a-option value="SALE">销售出库</a-option>
              <a-option value="OTHER">其他出库</a-option>
              <a-option v-if="form.outType === 'REPAIR'" value="REPAIR">维修出库</a-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="出库日期" required>
            <a-date-picker v-model="form.outDate" show-time :format="DATETIME_FORMAT" :value-format="DATETIME_FORMAT" :disabled="readonly || !editorReady" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库" required>
            <a-select
              v-model="form.warehouseId"
              :options="warehouseOptions"
              :disabled="readonly || !editorReady"
              allow-search
              placeholder="请选择仓库"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户" :required="form.outType === 'SALE'">
            <a-select
              v-model="form.unitId"
              :options="unitOptions"
              :disabled="readonly || form.outType !== 'SALE'"
              allow-search
              allow-clear
              placeholder="请选择客户"
              @change="onUnitChange"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="form.outType === 'SALE'" :span="12">
          <a-form-item label="销售合同" required>
            <a-select
              v-model="form.contractId"
              :options="contractOptions"
              :disabled="readonly || !form.unitId"
              allow-search
              allow-clear
              :placeholder="form.unitId ? '请选择已确认销售合同' : '请先选择客户'"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea v-model="form.remark" :disabled="readonly || !editorReady" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="psi-biz-form__section">
        <div class="psi-biz-form__section-title">
          <h4>明细</h4>
          <div v-if="!readonly" style="display: flex; gap: 8px; align-items: center;">
            <a-link @click="downloadExcelTemplate">下载模板</a-link>
            <a-upload :custom-request="handleExcelUpload" :show-file-list="false" accept=".xlsx,.xls">
              <a-button type="outline" size="mini">Excel 导入明细</a-button>
            </a-upload>
            <a-button type="outline" size="mini" @click="addLine">加行</a-button>
          </div>
        </div>
        <a-table class="psi-biz-form__table" :data="form.items" :pagination="false" row-key="uid" size="small" :bordered="{ cell: true }">
          <template #columns>
            <a-table-column title="商品" :width="240">
              <template #cell="{ record }">
                <a-select
                  v-model="record.goodsId"
                  :options="goodsOptions"
                  :disabled="readonly || !editorReady"
                  allow-search
                  placeholder="请选择商品"
                />
              </template>
            </a-table-column>
            <a-table-column title="数量" :width="120">
              <template #cell="{ record }">
                <a-input-number v-model="record.quantity" :min="0" :precision="4" :disabled="readonly || !editorReady" />
              </template>
            </a-table-column>
            <a-table-column title="含税单价" :width="130">
              <template #cell="{ record }">
                <a-input-number v-model="record.taxInclusivePrice" :min="0" :precision="6" :disabled="readonly || !editorReady" />
              </template>
            </a-table-column>
            <a-table-column title="税率%" :width="110">
              <template #cell="{ record }">
                <a-select v-model="record.taxRate" :disabled="readonly || !editorReady">
                  <a-option :value="13">13</a-option>
                  <a-option :value="9">9</a-option>
                  <a-option :value="6">6</a-option>
                  <a-option :value="3">3</a-option>
                  <a-option :value="1">1</a-option>
                </a-select>
              </template>
            </a-table-column>
            <a-table-column v-if="!readonly" title="操作" :width="72">
              <template #cell="{ rowIndex }">
                <a-link status="danger" @click="form.items.splice(rowIndex, 1)">删除</a-link>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </div>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { Message, type RequestOption } from '@arco-design/web-vue'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { useWindowSize } from '@vueuse/core'
import {
  addStockOut,
  downloadStockOutImportTemplate,
  getStockOut,
  listContractSale,
  listGoodsOptions,
  listUnitOptions,
  listWarehouseOptions,
  parseImportStockOut,
  updateStockOut,
} from '@/apis/psi'
import { useDownload } from '@/hooks'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const readonly = ref(false)
const dataId = ref('')
let editorRequestVersion = 0
const editorReady = ref(false)
const editorSaving = ref(false)
const isUpdate = computed(() => !!dataId.value && !readonly.value)
const title = computed(() => (readonly.value ? '出库详情' : isUpdate.value ? '修改出库草稿' : '新增出库草稿'))

const warehouseOptions = ref<{ label: string, value: string }[]>([])
const goodsOptions = ref<{ label: string, value: string }[]>([])
const unitOptions = ref<{ label: string, value: string }[]>([])
const contractOptions = ref<{ label: string, value: string }[]>([])

const form = reactive<any>({
  outType: 'SALE',
  outDate: undefined,
  warehouseId: undefined,
  unitId: undefined,
  contractId: undefined,
  remark: '',
  items: [] as any[],
})

const loadContractOptions = async (unitId?: string | number, keepContractId?: string | number) => {
  contractOptions.value = []
  if (!unitId) {
    return
  }
  const { data } = await listContractSale({
    unitId: String(unitId),
    docStatus: 'CONFIRMED',
    page: 1,
    size: 200,
    sort: ['id,desc'],
  })
  contractOptions.value = (data?.list || []).map((c) => ({
    label: c.name || c.contractNo,
    value: String(c.id),
  }))
  ensureSelectOpt(contractOptions.value, keepContractId, form.contractName)
}

const loadOptions = async () => {
  const [w, g, u] = await Promise.all([
    listWarehouseOptions(),
    listGoodsOptions(),
    listUnitOptions(form.outType === 'SALE' ? 'CUSTOMER' : undefined),
  ])
  warehouseOptions.value = toLabelValueOpts(w.data || [])
  goodsOptions.value = toLabelValueOpts(g.data || [])
  unitOptions.value = toLabelValueOpts(u.data || [])
}

const onOutTypeChange = async () => {
  form.unitId = undefined
  form.contractId = undefined
  contractOptions.value = []
  const { data } = await listUnitOptions(form.outType === 'SALE' ? 'CUSTOMER' : undefined)
  unitOptions.value = toLabelValueOpts(data || [])
}

const onUnitChange = async () => {
  form.contractId = undefined
  await loadContractOptions(form.unitId)
}

const addLine = () => {
  form.items.push({ uid: crypto.randomUUID(), goodsId: undefined, quantity: 1, taxInclusivePrice: 0, taxRate: 13 })
}

const downloadExcelTemplate = () => {
  useDownload(() => downloadStockOutImportTemplate())
}

const handleExcelUpload = (options: RequestOption) => {
  const controller = new AbortController()
  const version = editorRequestVersion
  const id = dataId.value
  ;(async () => {
    const { onError, onSuccess, fileItem, name = 'file' } = options
    const formData = new FormData()
    formData.append(name as string, fileItem.file as Blob)
    try {
      const res = await parseImportStockOut(formData)
      if (controller.signal.aborted || version !== editorRequestVersion || id !== dataId.value) {
        onError(new Error('导入已取消或单据已切换'))
        return
      }
      const items = res.data?.items || []
      if (!items.length) {
        Message.warning('未解析到有效明细')
        onError(new Error('empty'))
        return
      }
      form.items = items.map((i: any) => ({
        uid: crypto.randomUUID(),
        goodsId: toBizId(i.goodsId),
        quantity: Number(i.quantity),
        taxInclusivePrice: Number(i.taxInclusivePrice ?? 0),
        taxRate: Number(i.taxRate ?? 13),
      }))
      items.forEach((i: any) => ensureSelectOpt(goodsOptions.value, i.goodsId, i.goodsName))
      Message.success(`已载入 ${items.length} 行明细，请核对后保存草稿`)
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
  editorRequestVersion++
  editorReady.value = false
  form.outType = 'SALE'
  form.outDate = undefined
  form.warehouseId = undefined
  form.unitId = undefined
  form.contractId = undefined
  form.contractName = undefined
  form.remark = ''
  form.items = []
  contractOptions.value = []
  dataId.value = ''
  readonly.value = false
}

const save = async () => {
  if (!editorReady.value || editorSaving.value) return false
  if (readonly.value) return true
  const version = editorRequestVersion
  const id = dataId.value
  editorSaving.value = true
  try {
    if (!form.warehouseId || !form.outDate || !form.items.length) {
      Message.warning('请完善仓库、日期与明细')
      return false
    }
    if (form.outType === 'SALE' && !form.unitId) {
      Message.warning('销售出库请选择客户')
      return false
    }
    if (form.outType === 'SALE' && !form.contractId) {
      Message.warning('销售出库请绑定销售合同')
      return false
    }
    if (form.items.some((i: any) => !i.goodsId)) {
      Message.warning('请选择商品')
      return false
    }
    const payload = {
      outType: form.outType,
      outDate: form.outDate,
      warehouseId: toBizId(form.warehouseId),
      unitId: toBizId(form.unitId),
      contractId: form.outType === 'SALE' ? toBizId(form.contractId) : undefined,
      remark: form.remark,
      items: form.items.map((i: any) => ({
        goodsId: toBizId(i.goodsId),
        quantity: i.quantity,
        taxInclusivePrice: i.taxInclusivePrice,
        taxRate: i.taxRate,
      })),
    }
    if (isUpdate.value) {
      await updateStockOut(payload, id)
      if (version !== editorRequestVersion || id !== dataId.value) return false
      Message.success('修改成功')
    } else {
      await addStockOut(payload)
      if (version !== editorRequestVersion || id !== dataId.value) return false
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  } finally {
    editorSaving.value = false
  }
}

const fill = async (id: string, version: number) => {
  const { data } = await getStockOut(id)
  if (version !== editorRequestVersion || id !== dataId.value) return false
  if (!data || String(data.id) !== id) {
    Message.warning('单据不存在或无权访问')
    return false
  }
  form.outType = data.outType
  form.outDate = data.outDate
  form.warehouseId = toBizId(data.warehouseId)
  form.unitId = toBizId(data.unitId)
  form.contractId = toBizId(data.contractId)
  form.contractName = data.contractName
  form.remark = data.remark
  form.items = (data.items || []).map((i) => ({
    uid: crypto.randomUUID(),
    goodsId: toBizId(i.goodsId),
    quantity: Number(i.quantity),
    taxInclusivePrice: Number(i.taxInclusivePrice),
    taxRate: Number(i.taxRate),
  }))
  ensureSelectOpt(warehouseOptions.value, form.warehouseId, (data as any).warehouseName)
  ensureSelectOpt(unitOptions.value, form.unitId, (data as any).unitName)
  ;(data.items || []).forEach((i: any) => ensureSelectOpt(goodsOptions.value, i.goodsId, i.goodsName))
  if (form.outType === 'SALE' && form.unitId) {
    await loadContractOptions(form.unitId, form.contractId)
  }
  return version === editorRequestVersion && id === dataId.value
}

const open = async (version = editorRequestVersion) => {
  await loadOptions()
  if (version !== editorRequestVersion) return
  editorReady.value = true
  visible.value = true
}

const onAdd = async () => {
  reset()
  addLine()
  await open()
}
const onUpdate = async (id: string) => {
  reset()
  const version = editorRequestVersion
  dataId.value = id
  try {
    await loadOptions()
    if (version !== editorRequestVersion || id !== dataId.value) return
    if (!(await fill(id, version))) return
    if (version !== editorRequestVersion || id !== dataId.value) return
    editorReady.value = true
    visible.value = true
  } catch {
    if (version === editorRequestVersion) visible.value = false
  }
}
const onView = async (id: string) => {
  reset()
  readonly.value = true
  const version = editorRequestVersion
  dataId.value = id
  try {
    await loadOptions()
    if (version !== editorRequestVersion || id !== dataId.value) return
    if (!(await fill(id, version))) return
    if (version !== editorRequestVersion || id !== dataId.value) return
    editorReady.value = true
    visible.value = true
  } catch {
    if (version === editorRequestVersion) visible.value = false
  }
}

defineExpose({ onAdd, onUpdate, onView })
</script>

<style lang="scss">
@import '../styles/psi-form.scss';
</style>
