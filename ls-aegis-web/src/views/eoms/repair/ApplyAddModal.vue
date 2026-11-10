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
    <GiForm ref="formRef" v-model="form" :columns="columns" layout="vertical">
      <template #photos>
        <PlatformFileField
          v-model="form.photos"
          multiple
          variant="drag"
          drag-title="点击或拖拽上传现场照片"
          parent-path="/eoms/repair"
          accept=".png,.jpg,.jpeg"
          tip="支持 png / jpg，可多张；上传后进入平台文件中心，可点击预览。"
        />
      </template>
    </GiForm>
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { addRepairApply } from '@/apis/eoms'
import { listUnitOptions } from '@/apis/psi'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import PlatformFileField from '@/components/PlatformFileField/index.vue'
import { useResetReactive } from '@/hooks'
import { useDict } from '@/hooks/app'
import { toLabelValueOpts } from '@/utils/biz-id'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const { eoms_fault_type, eoms_urgent_level } = useDict('eoms_fault_type', 'eoms_urgent_level')

const visible = ref(false)
const title = '新增报修'
const formRef = ref<InstanceType<typeof GiForm>>()
const customerOptions = ref<{ label: string, value: string }[]>([])

const [form, resetForm] = useResetReactive({
  photos: undefined as string | undefined,
})

const loadCustomers = async () => {
  const { data } = await listUnitOptions('CUSTOMER')
  customerOptions.value = toLabelValueOpts(data || [])
}

const columns: ColumnItem[] = reactive([
  {
    label: '客户',
    field: 'customerId',
    type: 'select',
    span: 24,
    required: true,
    props: {
      options: customerOptions,
      allowSearch: true,
    },
  },
  { label: '联系人', field: 'contactName', type: 'input', span: 12, required: true, props: { maxLength: 50 } },
  { label: '联系电话', field: 'contactPhone', type: 'input', span: 12, required: true, props: { maxLength: 11 } },
  {
    label: '故障类型',
    field: 'faultType',
    type: 'select',
    span: 12,
    props: {
      options: computed(() =>
        (eoms_fault_type.value || []).map((d) => ({ label: d.label, value: d.value })),
      ),
      allowClear: true,
    },
  },
  {
    label: '紧急程度',
    field: 'urgentLevel',
    type: 'select',
    span: 12,
    props: {
      options: computed(() =>
        (eoms_urgent_level.value || []).map((d) => ({ label: d.label, value: d.value })),
      ),
      allowClear: true,
    },
  },
  { label: '地址', field: 'address', type: 'input', span: 24, props: { maxLength: 200 } },
  { label: '故障描述', field: 'faultDesc', type: 'textarea', span: 24, required: true },
  { label: '现场照片', field: 'photos', span: 24 },
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
    await addRepairApply(form)
    Message.success('新增成功')
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = async () => {
  reset()
  await loadCustomers()
  visible.value = true
}

defineExpose({ onAdd })
</script>
