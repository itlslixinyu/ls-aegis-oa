<template>
  <a-modal
    v-model:visible="visible"
    :title="title"
    :mask-closable="false"
    :width="width >= 640 ? 640 : '100%'"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <GiForm ref="formRef" v-model="form" :columns="columns" layout="vertical" />
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import {
  addRepairRecord,
  getRepairRecord,
  listRepairOrder,
  updateRepairRecord,
} from '@/apis/eoms'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useResetReactive } from '@/hooks'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const dataId = ref('')
const visible = ref(false)
/** 从工单页补录时锁定关联工单，不可改选 */
const orderLocked = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改维修记录' : '新增维修记录'))
const formRef = ref<InstanceType<typeof GiForm>>()
const orderOptions = ref<{ label: string; value: string }[]>([])

const [form, resetForm] = useResetReactive({
  repairOrderId: undefined as string | undefined,
  faultReason: '',
  checkProcess: '',
  solution: '',
  result: '',
  remark: '',
})

const columns = computed<ColumnItem[]>(() => [
  {
    label: '关联工单',
    field: 'repairOrderId',
    type: 'select',
    span: 24,
    required: true,
    props: {
      options: orderOptions.value,
      allowSearch: true,
      disabled: isUpdate.value || orderLocked.value,
      placeholder: '请选择维修中的工单',
    },
  },
  {
    label: '故障原因',
    field: 'faultReason',
    type: 'textarea',
    span: 24,
    required: true,
    props: { maxLength: 500, autoSize: { minRows: 2, maxRows: 4 } },
  },
  {
    label: '检测过程',
    field: 'checkProcess',
    type: 'textarea',
    span: 24,
    required: true,
    props: { maxLength: 1000, autoSize: { minRows: 2, maxRows: 5 } },
  },
  {
    label: '解决方案',
    field: 'solution',
    type: 'textarea',
    span: 24,
    required: true,
    props: { maxLength: 1000, autoSize: { minRows: 2, maxRows: 5 } },
  },
  {
    label: '维修结果',
    field: 'result',
    type: 'textarea',
    span: 24,
    required: true,
    props: { maxLength: 500, autoSize: { minRows: 2, maxRows: 4 } },
  },
  { label: '备注', field: 'remark', type: 'textarea', span: 24, props: { maxLength: 500 } },
])

const loadOrderOptions = async () => {
  const [proc, accepted] = await Promise.all([
    listRepairOrder({ status: 'PROCESSING', page: 1, size: 200 }),
    listRepairOrder({ status: 'ACCEPTED', page: 1, size: 200 }),
  ])
  const unwrap = (raw: any) => {
    if (Array.isArray(raw)) return raw
    if (Array.isArray(raw?.list)) return raw.list
    if (Array.isArray(raw?.records)) return raw.records
    if (Array.isArray(raw?.rows)) return raw.rows
    return []
  }
  const list = [...unwrap(proc.data), ...unwrap(accepted.data)]
  const seen = new Set<string>()
  orderOptions.value = list
    .map((o: any) => ({
      label: `${o.orderNo}${o.customerName ? ` · ${o.customerName}` : ''}`,
      value: String(o.id),
    }))
    .filter((o) => {
      if (seen.has(o.value)) return false
      seen.add(o.value)
      return true
    })
}

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
  orderLocked.value = false
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    const payload = { ...form }
    if (isUpdate.value) {
      await updateRepairRecord({ ...payload, id: dataId.value })
      Message.success('修改成功')
    } else {
      await addRepairRecord(payload)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = async () => {
  dataId.value = ''
  reset()
  await loadOrderOptions()
  visible.value = true
}

/** 从工单页补录：预填并锁定工单 */
const onAddForOrder = async (orderId: string | number, orderNo?: string) => {
  dataId.value = ''
  reset()
  orderLocked.value = true
  const id = String(orderId)
  form.repairOrderId = id
  orderOptions.value = [
    {
      label: orderNo ? `${orderNo}` : id,
      value: id,
    },
  ]
  visible.value = true
}

const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  await loadOrderOptions()
  const { data } = await getRepairRecord(id)
  Object.assign(form, {
    ...data,
    repairOrderId: data?.repairOrderId != null ? String(data.repairOrderId) : undefined,
  })
  // 编辑时把当前工单补进选项，避免状态已变后下拉空白
  if (form.repairOrderId && data?.orderNo) {
    const exists = orderOptions.value.some((o) => o.value === form.repairOrderId)
    if (!exists) {
      orderOptions.value.unshift({
        label: data.orderNo,
        value: form.repairOrderId,
      })
    }
  }
  visible.value = true
}

defineExpose({ onAdd, onAddForOrder, onUpdate })
</script>
