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
import { getSettlement, updateSettlement } from '@/apis/eoms'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useResetReactive } from '@/hooks'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const dataId = ref('')
const visible = ref(false)
const title = '编辑结算单'
let editRequest = 0
const formRef = ref<InstanceType<typeof GiForm>>()

const [form, resetForm] = useResetReactive({
  laborAmount: 0,
  materialAmount: 0,
  totalIncome: 0,
})

watch(
  () => [form.laborAmount, form.materialAmount],
  ([labor, material]) => {
    form.totalIncome = Number(labor || 0) + Number(material || 0)
  },
)

const columns: ColumnItem[] = reactive([
  { label: '工单编号', field: 'orderNo', type: 'input', span: 12, disabled: true },
  { label: '客户名称', field: 'customerName', type: 'input', span: 12, disabled: true },
  { label: '人工费', field: 'laborAmount', type: 'input-number', span: 8, props: { min: 0, precision: 2 } },
  { label: '材料费', field: 'materialAmount', type: 'input-number', span: 8, props: { min: 0, precision: 2 } },
  { label: '合计', field: 'totalIncome', type: 'input-number', span: 8, disabled: true, props: { min: 0, precision: 2 } },
  { label: '备注', field: 'remark', type: 'textarea', span: 24 },
])

const reset = () => {
  editRequest += 1
  dataId.value = ''
  formRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  const id = dataId.value
  const request = editRequest
  if (!id || !visible.value) return false
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    if (request !== editRequest || id !== dataId.value || !visible.value) return false
    await updateSettlement({ ...form, id })
    if (request !== editRequest || id !== dataId.value || !visible.value) return false
    Message.success('修改成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onUpdate = async (id: string) => {
  visible.value = false
  reset()
  const request = editRequest
  const { data } = await getSettlement(id)
  if (request !== editRequest) return
  if (!data || data.status !== 'DRAFT' || String(data.id) !== String(id)) {
    Message.warning('仅可编辑结算草稿，请刷新列表')
    return
  }
  Object.assign(form, data)
  dataId.value = String(id)
  visible.value = true
}

defineExpose({ onUpdate })
</script>
