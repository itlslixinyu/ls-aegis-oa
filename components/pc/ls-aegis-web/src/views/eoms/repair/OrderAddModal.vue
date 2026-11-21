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
import { addRepairOrder, listEngineerOptions } from '@/apis/eoms'
import { listUnitOptions } from '@/apis/psi'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useResetReactive } from '@/hooks'
import { toSelectOpts, toUserSelectOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const visible = ref(false)
const title = '新增工单'
const formRef = ref<InstanceType<typeof GiForm>>()
const customerOptions = ref<{ label: string, value: string }[]>([])
const engineerOptions = ref<{ label: string, value: string }[]>([])

const [form, resetForm] = useResetReactive({})

const loadOptions = async () => {
  const [cRes, eRes] = await Promise.all([
    listUnitOptions('CUSTOMER'),
    listEngineerOptions(),
  ])
  customerOptions.value = toSelectOpts(cRes.data || [])
  engineerOptions.value = toUserSelectOpts(eRes.data || [])
}

const columns: ColumnItem[] = reactive([
  {
    label: '客户',
    field: 'customerId',
    type: 'select',
    span: 12,
    required: true,
    props: { options: customerOptions, allowSearch: true },
  },
  {
    label: '工程师',
    field: 'engineerId',
    type: 'select',
    span: 12,
    props: { options: engineerOptions, allowSearch: true, allowClear: true },
  },
  { label: '维修地址', field: 'address', type: 'input', span: 24, props: { maxLength: 200 } },
  { label: '备注', field: 'remark', type: 'textarea', span: 24 },
])

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    await addRepairOrder(form)
    Message.success('新增成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = async () => {
  reset()
  await loadOptions()
  visible.value = true
}

defineExpose({ onAdd })
</script>
