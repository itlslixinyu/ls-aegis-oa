<template>
  <a-modal
    v-model:visible="visible"
    title="工单用料"
    :footer="false"
    :mask-closable="false"
    :width="width >= 880 ? 880 : '100%'"
    unmount-on-close
  >
    <a-form v-if="editable" :model="form" layout="vertical" @submit.prevent="onAdd">
      <a-row :gutter="12">
        <a-col :span="8">
          <a-form-item label="商品" required>
            <a-select
              v-model="form.materialId"
              :options="goodsOptions"
              allow-search
              placeholder="进销存商品"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="出库仓库" required>
            <a-select
              v-model="form.warehouseId"
              :options="warehouseOptions"
              allow-search
              placeholder="请选择仓库"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="数量" required>
            <a-input-number v-model="form.quantity" :min="0.01" :precision="2" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="销售单价">
            <a-input-number v-model="form.salePrice" :min="0" :precision="2" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="预估成本单价" extra="结算以实际库存出库的 FIFO 成本为准">
            <a-input-number v-model="form.costPrice" :min="0" :precision="2" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label=" ">
            <a-button type="primary" :loading="saving" @click="onAdd">添加</a-button>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <a-table :data="rows" :pagination="false" :loading="loading" row-key="id" size="small">
      <template #columns>
        <a-table-column title="商品" data-index="materialName" :ellipsis="true" :tooltip="true" />
        <a-table-column title="编号" data-index="materialNo" :width="120" />
        <a-table-column title="仓库" :width="120">
          <template #cell="{ record }">
            {{ warehouseName(record.warehouseId) }}
          </template>
        </a-table-column>
        <a-table-column title="规格" data-index="specification" :width="100" />
        <a-table-column title="数量" data-index="quantity" :width="80" />
        <a-table-column title="销售单价" data-index="salePrice" :width="100" />
        <a-table-column title="销售金额" data-index="saleAmount" :width="100" />
        <a-table-column v-if="editable" title="操作" :width="72">
          <template #cell="{ record }">
            <a-link status="danger" @click="onDelete(record)">删除</a-link>
          </template>
        </a-table-column>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { Message, Modal } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import {
  addSettlementMaterial,
  deleteSettlementMaterial,
  listSettlementMaterial,
} from '@/apis/eoms'
import { listGoodsOptions, listWarehouseOptions } from '@/apis/psi'
import { useResetReactive } from '@/hooks'
import { toLabelValueOpts } from '@/utils/biz-id'

const { width } = useWindowSize()

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const editable = ref(false)
const repairOrderId = ref<string | number>('')
const rows = ref<any[]>([])
const goodsOptions = ref<{ label: string, value: string }[]>([])
const warehouseOptions = ref<{ label: string, value: string }[]>([])

const [form, resetForm] = useResetReactive({
  materialId: undefined as string | undefined,
  warehouseId: undefined as string | undefined,
  quantity: 1,
  salePrice: 0,
  costPrice: 0,
})

const unwrapList = (raw: any) => {
  if (Array.isArray(raw)) return raw
  if (Array.isArray(raw?.list)) return raw.list
  if (Array.isArray(raw?.records)) return raw.records
  return []
}

const goodsName = (id?: string | number) => {
  const hit = goodsOptions.value.find((i) => String(i.value) === String(id))
  if (!hit?.label) return ''
  const idx = hit.label.lastIndexOf('（')
  return idx > 0 ? hit.label.slice(0, idx) : hit.label
}

const warehouseName = (id?: string | number) => {
  if (id == null || id === '') return '-'
  const hit = warehouseOptions.value.find((i) => String(i.value) === String(id))
  return hit?.label || String(id)
}

const loadRows = async () => {
  loading.value = true
  try {
    const { data } = await listSettlementMaterial({ repairOrderId: repairOrderId.value, page: 1, size: 200 })
    rows.value = unwrapList(data)
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  const [g, w] = await Promise.all([listGoodsOptions(), listWarehouseOptions()])
  goodsOptions.value = toLabelValueOpts(g.data || [])
  warehouseOptions.value = toLabelValueOpts(w.data || [])
}

const onAdd = async () => {
  if (!form.materialId) {
    Message.warning('请选择进销存商品')
    return
  }
  if (!form.warehouseId) {
    Message.warning('请选择出库仓库')
    return
  }
  if (!form.quantity || Number(form.quantity) <= 0) {
    Message.warning('数量必须大于 0')
    return
  }
  saving.value = true
  try {
    await addSettlementMaterial({
      repairOrderId: repairOrderId.value,
      materialId: form.materialId,
      warehouseId: form.warehouseId,
      materialName: goodsName(form.materialId),
      quantity: form.quantity,
      salePrice: form.salePrice ?? 0,
      costPrice: form.costPrice ?? 0,
    })
    Message.success('已添加')
    resetForm()
    await loadRows()
  } finally {
    saving.value = false
  }
}

const onDelete = (record: any) => {
  Modal.warning({
    title: '提示',
    content: `删除用料「${record.materialName}」？`,
    hideCancel: false,
    onOk: async () => {
      await deleteSettlementMaterial(record.id)
      Message.success('已删除')
      await loadRows()
    },
  })
}

const onOpen = async (orderId: string | number, canEdit: boolean) => {
  repairOrderId.value = orderId
  editable.value = canEdit
  resetForm()
  visible.value = true
  await Promise.all([loadOptions(), loadRows()])
}

defineExpose({ onOpen })
</script>
