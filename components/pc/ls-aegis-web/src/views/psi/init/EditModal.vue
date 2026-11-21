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
          <a-form-item label="期初日期" required>
            <a-date-picker v-model="form.initDate" show-time :format="DATETIME_FORMAT" :value-format="DATETIME_FORMAT" :disabled="readonly" />
          </a-form-item>
        </a-col>
        <a-col v-if="docType === 'STOCK'" :span="12">
          <a-form-item label="仓库" required>
            <a-select v-model="form.warehouseId" :options="warehouseOptions" :disabled="readonly" allow-search />
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
          <h4>明细</h4>
          <a-button v-if="!readonly" type="outline" size="mini" @click="addLine">加行</a-button>
        </div>
        <a-table class="psi-biz-form__table" :data="form.items" :pagination="false" row-key="uid" size="small" :bordered="{ cell: true }">
          <template #columns>
            <template v-if="docType === 'STOCK'">
              <a-table-column title="商品" :width="240">
                <template #cell="{ record }">
                  <a-select v-model="record.goodsId" :options="goodsOptions" :disabled="readonly" allow-search />
                </template>
              </a-table-column>
              <a-table-column title="数量" :width="120">
                <template #cell="{ record }">
                  <a-input-number v-model="record.quantity" :min="0" :precision="4" :disabled="readonly" />
                </template>
              </a-table-column>
              <a-table-column title="未税单价" :width="140">
                <template #cell="{ record }">
                  <a-input-number v-model="record.untaxPrice" :min="0" :precision="6" :disabled="readonly" />
                </template>
              </a-table-column>
            </template>
            <template v-else>
              <a-table-column :title="docType === 'AR' ? '客户' : '供应商'" :width="260">
                <template #cell="{ record }">
                  <a-select v-model="record.unitId" :options="unitOptions" :disabled="readonly" allow-search />
                </template>
              </a-table-column>
              <a-table-column title="含税金额" :width="160">
                <template #cell="{ record }">
                  <a-input-number v-model="record.taxInclusiveAmount" :min="0" :precision="2" :disabled="readonly" />
                </template>
              </a-table-column>
            </template>
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
  addInitDoc,
  addInitStock,
  getInitDoc,
  getInitStock,
  listGoodsOptions,
  listUnitOptions,
  listWarehouseOptions,
  updateInitDoc,
  updateInitStock,
} from '@/apis/psi'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const readonly = ref(false)
const dataId = ref('')
const docType = ref('STOCK')
const isUpdate = computed(() => !!dataId.value && !readonly.value)
const typeLabel = computed(() => ({ STOCK: '库存', AR: '应收', AP: '应付', PREPAY: '预付' }[docType.value] || ''))
const title = computed(() => `${readonly.value ? '' : isUpdate.value ? '修改' : '新增'}${typeLabel.value}期初${readonly.value ? '详情' : ''}`)

const warehouseOptions = ref<{ label: string, value: string }[]>([])
const goodsOptions = ref<{ label: string, value: string }[]>([])
const unitOptions = ref<{ label: string, value: string }[]>([])
const form = reactive<any>({
  initDate: undefined,
  warehouseId: undefined,
  remark: '',
  items: [] as any[],
})

const loadOptions = async () => {
  if (docType.value === 'STOCK') {
    const [w, g] = await Promise.all([listWarehouseOptions(), listGoodsOptions()])
    warehouseOptions.value = toLabelValueOpts(w.data || [])
    goodsOptions.value = toLabelValueOpts(g.data || [])
  } else {
    const unitType = docType.value === 'AR' ? 'CUSTOMER' : 'SUPPLIER'
    const { data } = await listUnitOptions(unitType)
    unitOptions.value = toLabelValueOpts(data || [])
  }
}

const addLine = () => {
  if (docType.value === 'STOCK') {
    form.items.push({ uid: crypto.randomUUID(), goodsId: undefined, quantity: 1, untaxPrice: 0 })
  } else {
    form.items.push({ uid: crypto.randomUUID(), unitId: undefined, taxInclusiveAmount: 0 })
  }
}

const reset = () => {
  form.initDate = undefined
  form.warehouseId = undefined
  form.remark = ''
  form.items = []
  dataId.value = ''
  readonly.value = false
}

const save = async () => {
  if (readonly.value) return true
  try {
    if (!form.initDate || !form.items.length) {
      Message.warning('请完善日期与明细')
      return false
    }
    if (docType.value === 'STOCK') {
      if (!form.warehouseId) {
        Message.warning('请选择仓库')
        return false
      }
      const payload = {
        initDate: form.initDate,
        warehouseId: form.warehouseId,
        remark: form.remark,
        items: form.items.map((i: any) => ({
          goodsId: i.goodsId,
          quantity: i.quantity,
          untaxPrice: i.untaxPrice,
        })),
      }
      if (isUpdate.value) await updateInitStock(payload, dataId.value)
      else await addInitStock(payload)
    } else {
      const payload = {
        type: docType.value,
        initDate: form.initDate,
        remark: form.remark,
        items: form.items.map((i: any) => ({
          unitId: i.unitId,
          taxInclusiveAmount: i.taxInclusiveAmount,
        })),
      }
      if (isUpdate.value) await updateInitDoc(payload, dataId.value)
      else await addInitDoc(payload)
    }
    Message.success('保存成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const fill = async (type: string, id: string) => {
  if (type === 'STOCK') {
    const { data } = await getInitStock(id)
    form.initDate = data.initDate
    form.warehouseId = toBizId(data.warehouseId)
    form.remark = data.remark
    form.items = (data.items || []).map((i) => ({
      uid: crypto.randomUUID(),
      goodsId: toBizId(i.goodsId),
      quantity: Number(i.quantity),
      untaxPrice: Number(i.untaxPrice),
    }))
    ensureSelectOpt(warehouseOptions.value, form.warehouseId, (data as any).warehouseName)
    ;(data.items || []).forEach((i: any) => ensureSelectOpt(goodsOptions.value, i.goodsId, i.goodsName))
  } else {
    const { data } = await getInitDoc(type, id)
    form.initDate = data.initDate
    form.remark = data.remark
    form.items = (data.items || []).map((i) => ({
      uid: crypto.randomUUID(),
      unitId: toBizId(i.unitId),
      taxInclusiveAmount: Number(i.taxInclusiveAmount),
    }))
    ;(data.items || []).forEach((i: any) => ensureSelectOpt(unitOptions.value, i.unitId, i.unitName))
  }
}

const open = async (type: string) => {
  docType.value = type
  await loadOptions()
  visible.value = true
}

const onAdd = async (type: string) => {
  reset()
  docType.value = type
  addLine()
  await open(type)
}
const onUpdate = async (type: string, id: string) => {
  reset()
  docType.value = type
  dataId.value = id
  await loadOptions()
  await fill(type, id)
  visible.value = true
}
const onView = async (type: string, id: string) => {
  reset()
  readonly.value = true
  docType.value = type
  dataId.value = id
  await loadOptions()
  await fill(type, id)
  visible.value = true
}

defineExpose({ onAdd, onUpdate, onView })
</script>

<style lang="scss">
@import '../styles/psi-form.scss';
</style>
