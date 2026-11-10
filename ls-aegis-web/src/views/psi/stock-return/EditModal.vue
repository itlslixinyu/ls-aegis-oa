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
          <a-form-item label="退货类型" required>
            <a-select v-model="form.returnType" :disabled="readonly || isUpdate" :options="returnTypeOptions" @change="onTypeChange" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="退货日期" required>
            <a-date-picker v-model="form.returnDate" show-time :format="DATETIME_FORMAT" :value-format="DATETIME_FORMAT" :disabled="readonly" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库" required>
            <a-select
              v-model="form.warehouseId"
              :options="warehouseOptions"
              :disabled="readonly"
              allow-search
              placeholder="请选择仓库"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="form.returnType === 'SALE_RETURN' ? '客户' : '供应商'">
            <a-select
              v-model="form.unitId"
              :options="unitOptions"
              :disabled="readonly"
              allow-search
              allow-clear
              placeholder="请选择往来单位"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="form.returnType === 'SALE_RETURN' ? '原出库单' : '原入库单'" required>
            <a-select
              v-model="originDocId"
              :options="originDocOptions"
              :disabled="readonly || isUpdate"
              allow-search
              placeholder="请选择已确认原单"
              @change="loadOriginItems"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea v-model="form.remark" :disabled="readonly" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="psi-biz-form__section">
        <div class="psi-biz-form__section-title">
          <h4>退货明细（来自原单）</h4>
        </div>
        <a-table class="psi-biz-form__table" :data="form.items" :pagination="false" row-key="uid" size="small" :bordered="{ cell: true }">
          <template #columns>
            <a-table-column title="商品" :width="240">
              <template #cell="{ record }">
                <a-select v-model="record.goodsId" :options="goodsOptions" disabled />
              </template>
            </a-table-column>
            <a-table-column title="退货数量" :width="120">
              <template #cell="{ record }">
                <a-input-number v-model="record.quantity" :min="0" :precision="4" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column title="含税单价" :width="130">
              <template #cell="{ record }">
                <a-input-number v-model="record.taxInclusivePrice" :min="0" :precision="6" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column title="税率%" :width="110">
              <template #cell="{ record }">
                <a-select v-model="record.taxRate" :disabled="readonly">
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
import { Message } from '@arco-design/web-vue'
import { DATETIME_FORMAT } from '@/constant/datetime'
import { useWindowSize } from '@vueuse/core'
import {
  addStockReturn,
  getStockIn,
  getStockOut,
  getStockReturn,
  listGoodsOptions,
  listStockIn,
  listStockOut,
  listUnitOptions,
  listWarehouseOptions,
  updateStockReturn,
} from '@/apis/psi'
import { useDict } from '@/hooks/app'
import { dictSelectOptions, findDictLabel } from '@/utils/dict'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const { psi_return_type, psi_stock_in_type, psi_stock_out_type } = useDict('psi_return_type', 'psi_stock_in_type', 'psi_stock_out_type')
const returnTypeOptions = computed(() => dictSelectOptions(psi_return_type.value))

const visible = ref(false)
const readonly = ref(false)
const dataId = ref('')
const isUpdate = computed(() => !!dataId.value && !readonly.value)
const title = computed(() => (readonly.value ? '退货详情' : isUpdate.value ? '修改退货草稿' : '新增退货草稿'))

const warehouseOptions = ref<{ label: string, value: string }[]>([])
const goodsOptions = ref<{ label: string, value: string }[]>([])
const unitOptions = ref<{ label: string, value: string }[]>([])
const originDocOptions = ref<{ label: string, value: string }[]>([])
const originDocId = ref<string>()

const form = reactive<any>({
  returnType: 'SALE_RETURN',
  returnDate: undefined,
  warehouseId: undefined,
  unitId: undefined,
  originStockOutId: undefined,
  originStockInId: undefined,
  remark: '',
  items: [] as any[],
})

const loadOriginDocs = async () => {
  if (form.returnType === 'SALE_RETURN') {
    const { data } = await listStockOut({ docStatus: 'CONFIRMED', page: 1, size: 200, sort: ['id,desc'] })
    originDocOptions.value = (data?.list || []).map((d: any) => ({
      label: `${d.docNo}（${findDictLabel(psi_stock_out_type.value, d.outType)}）`,
      value: String(d.id),
    }))
  } else {
    const { data } = await listStockIn({ docStatus: 'CONFIRMED', page: 1, size: 200, sort: ['id,desc'] })
    originDocOptions.value = (data?.list || []).map((d: any) => ({
      label: `${d.docNo}（${findDictLabel(psi_stock_in_type.value, d.inType)}）`,
      value: String(d.id),
    }))
  }
}

const loadOptions = async () => {
  const unitType = form.returnType === 'SALE_RETURN' ? 'CUSTOMER' : 'SUPPLIER'
  const [w, g, u] = await Promise.all([
    listWarehouseOptions(),
    listGoodsOptions(),
    listUnitOptions(unitType),
  ])
  warehouseOptions.value = toLabelValueOpts(w.data || [])
  goodsOptions.value = toLabelValueOpts(g.data || [])
  unitOptions.value = toLabelValueOpts(u.data || [])
  await loadOriginDocs()
}

const onTypeChange = async () => {
  originDocId.value = undefined
  form.originStockOutId = undefined
  form.originStockInId = undefined
  form.unitId = undefined
  form.items = []
  await loadOptions()
}

const loadOriginItems = async () => {
  if (!originDocId.value) return
  if (form.returnType === 'SALE_RETURN') {
    form.originStockOutId = originDocId.value
    form.originStockInId = undefined
    const { data } = await getStockOut(String(originDocId.value))
    form.warehouseId = toBizId(data.warehouseId)
    form.unitId = toBizId(data.unitId)
    form.items = (data.items || []).map((i) => ({
      uid: crypto.randomUUID(),
      originItemId: toBizId(i.id),
      goodsId: toBizId(i.goodsId),
      quantity: Number(i.quantity),
      taxInclusivePrice: Number(i.taxInclusivePrice),
      taxRate: Number(i.taxRate),
    }))
  } else {
    form.originStockInId = originDocId.value
    form.originStockOutId = undefined
    const { data } = await getStockIn(String(originDocId.value))
    form.warehouseId = toBizId(data.warehouseId)
    form.unitId = toBizId(data.unitId)
    form.items = (data.items || []).map((i) => ({
      uid: crypto.randomUUID(),
      originItemId: toBizId(i.id),
      goodsId: toBizId(i.goodsId),
      quantity: Number(i.quantity),
      taxInclusivePrice: Number(i.taxInclusivePrice),
      taxRate: Number(i.taxRate),
    }))
  }
  ensureSelectOpt(warehouseOptions.value, form.warehouseId, (form as any).warehouseName)
  ensureSelectOpt(unitOptions.value, form.unitId, (form as any).unitName)
  form.items.forEach((row: any) => ensureSelectOpt(goodsOptions.value, row.goodsId, row.goodsName))
}

const reset = () => {
  form.returnType = 'SALE_RETURN'
  form.returnDate = undefined
  form.warehouseId = undefined
  form.unitId = undefined
  form.originStockOutId = undefined
  form.originStockInId = undefined
  form.remark = ''
  form.items = []
  originDocId.value = undefined
  dataId.value = ''
  readonly.value = false
}

const save = async () => {
  if (readonly.value) return true
  try {
    if (!form.warehouseId || !form.returnDate || !form.items.length) {
      Message.warning('请完善仓库、日期与明细')
      return false
    }
    if (form.returnType === 'SALE_RETURN' && !form.originStockOutId) {
      Message.warning('请选择原出库单')
      return false
    }
    if (form.returnType === 'PURCHASE_RETURN' && !form.originStockInId) {
      Message.warning('请选择原入库单')
      return false
    }
    const payload = {
      returnType: form.returnType,
      returnDate: form.returnDate,
      warehouseId: form.warehouseId,
      unitId: form.unitId,
      originStockOutId: form.originStockOutId,
      originStockInId: form.originStockInId,
      remark: form.remark,
      items: form.items.map((i: any) => ({
        originItemId: i.originItemId,
        goodsId: i.goodsId,
        quantity: i.quantity,
        taxInclusivePrice: i.taxInclusivePrice,
        taxRate: i.taxRate,
      })),
    }
    if (isUpdate.value) {
      await updateStockReturn(payload, dataId.value)
      Message.success('修改成功')
    } else {
      await addStockReturn(payload)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const fill = async (id: string) => {
  const { data } = await getStockReturn(id)
  form.returnType = data.returnType
  form.returnDate = data.returnDate
  form.warehouseId = toBizId(data.warehouseId)
  form.unitId = toBizId(data.unitId)
  form.originStockOutId = toBizId(data.originStockOutId)
  form.originStockInId = toBizId(data.originStockInId)
  form.remark = data.remark
  originDocId.value = form.originStockOutId || form.originStockInId
  form.items = (data.items || []).map((i) => ({
    uid: crypto.randomUUID(),
    originItemId: toBizId(i.originItemId),
    goodsId: toBizId(i.goodsId),
    quantity: Number(i.quantity),
    taxInclusivePrice: Number(i.taxInclusivePrice),
    taxRate: Number(i.taxRate),
  }))
  ensureSelectOpt(warehouseOptions.value, form.warehouseId, (data as any).warehouseName)
  ensureSelectOpt(unitOptions.value, form.unitId, (data as any).unitName)
  ensureSelectOpt(originDocOptions.value, originDocId.value, (data as any).originDocNo)
  ;(data.items || []).forEach((i: any) => ensureSelectOpt(goodsOptions.value, i.goodsId, i.goodsName))
}

const open = async () => {
  await loadOptions()
  visible.value = true
}

const onAdd = async () => {
  reset()
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
