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
          <a-form-item label="调整日期" required>
            <a-date-picker v-model="form.adjustDate" show-time :format="DATETIME_FORMAT" :value-format="DATETIME_FORMAT" :disabled="readonly" />
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
            <a-table-column title="商品" :width="200">
              <template #cell="{ record }">
                <a-select
                  v-model="record.goodsId"
                  :options="goodsOptions"
                  :disabled="readonly"
                  allow-search
                  placeholder="请选择商品"
                />
              </template>
            </a-table-column>
            <a-table-column title="调整类型" :width="120">
              <template #cell="{ record }">
                <a-select v-model="record.adjustType" :disabled="readonly">
                  <a-option value="PROFIT">盘盈</a-option>
                  <a-option value="LOSS">盘亏</a-option>
                </a-select>
              </template>
            </a-table-column>
            <a-table-column title="数量" :width="120">
              <template #cell="{ record }">
                <a-input-number v-model="record.quantity" :min="0" :precision="4" :disabled="readonly" />
              </template>
            </a-table-column>
            <a-table-column title="未税单价" :width="130">
              <template #cell="{ record }">
                <a-input-number v-model="record.untaxPrice" :min="0" :precision="6" :disabled="readonly" />
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
  addStockAdjust,
  getStockAdjust,
  listGoodsOptions,
  listWarehouseOptions,
  updateStockAdjust,
} from '@/apis/psi'
import { ensureSelectOpt, toBizId, toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const readonly = ref(false)
const dataId = ref('')
const isUpdate = computed(() => !!dataId.value && !readonly.value)
const title = computed(() => (readonly.value ? '调整详情' : isUpdate.value ? '修改调整草稿' : '新增调整草稿'))

const warehouseOptions = ref<{ label: string, value: string }[]>([])
const goodsOptions = ref<{ label: string, value: string }[]>([])

const form = reactive<any>({
  adjustDate: undefined,
  warehouseId: undefined,
  remark: '',
  items: [] as any[],
})

const loadOptions = async () => {
  const [w, g] = await Promise.all([listWarehouseOptions(), listGoodsOptions()])
  warehouseOptions.value = toLabelValueOpts(w.data || [])
  goodsOptions.value = toLabelValueOpts(g.data || [])
}

const addLine = () => {
  form.items.push({ uid: crypto.randomUUID(), goodsId: undefined, adjustType: 'PROFIT', quantity: 1, untaxPrice: 0 })
}

const reset = () => {
  form.adjustDate = undefined
  form.warehouseId = undefined
  form.remark = ''
  form.items = []
  dataId.value = ''
  readonly.value = false
}

const save = async () => {
  if (readonly.value) return true
  try {
    if (!form.adjustDate || !form.warehouseId || !form.items.length) {
      Message.warning('请完善调整日期、仓库与明细')
      return false
    }
    if (form.items.some((i: any) => !i.goodsId || !i.adjustType)) {
      Message.warning('请完善商品与调整类型')
      return false
    }
    if (form.items.some((i: any) => i.adjustType === 'PROFIT' && (i.untaxPrice == null || Number(i.untaxPrice) < 0))) {
      Message.warning('盘盈行请填写未税单价')
      return false
    }
    const payload = {
      adjustDate: form.adjustDate,
      warehouseId: form.warehouseId,
      remark: form.remark,
      items: form.items.map((i: any) => ({
        goodsId: i.goodsId,
        adjustType: i.adjustType,
        quantity: i.quantity,
        untaxPrice: i.untaxPrice,
      })),
    }
    if (isUpdate.value) {
      await updateStockAdjust(payload, dataId.value)
      Message.success('修改成功')
    } else {
      await addStockAdjust(payload)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const fill = async (id: string) => {
  const { data } = await getStockAdjust(id)
  form.adjustDate = data.adjustDate
  form.warehouseId = toBizId(data.warehouseId)
  form.remark = data.remark
  form.items = (data.items || []).map((i: any) => ({
    uid: crypto.randomUUID(),
    goodsId: toBizId(i.goodsId),
    adjustType: i.adjustType,
    quantity: Number(i.quantity),
    untaxPrice: i.untaxPrice != null ? Number(i.untaxPrice) : 0,
  }))
  ensureSelectOpt(warehouseOptions.value, form.warehouseId, (data as any).warehouseName)
  ;(data.items || []).forEach((i: any) => ensureSelectOpt(goodsOptions.value, i.goodsId, i.goodsName))
}

const open = async () => {
  await loadOptions()
  visible.value = true
}

const onAdd = async () => {
  reset()
  addLine()
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
