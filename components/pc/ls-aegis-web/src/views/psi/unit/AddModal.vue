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
import { addUnit, getUnit, updateUnit } from '@/apis/psi'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useDict } from '@/hooks/app'
import { useResetReactive } from '@/hooks'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()
const { psi_unit_type } = useDict('psi_unit_type')

const dataId = ref('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改往来单位' : '新增往来单位'))
const formRef = ref<InstanceType<typeof GiForm>>()

const [form, resetForm] = useResetReactive({
  status: 1,
  unitType: 'CUSTOMER',
  unitNature: 'ENTERPRISE',
})

const columns: ColumnItem[] = reactive([
  { label: '单位全称', field: 'unitName', type: 'input', span: 24, required: true, props: { maxLength: 128 } },
  {
    label: '单位类型',
    field: 'unitType',
    type: 'select',
    span: 12,
    required: true,
    props: { options: psi_unit_type },
  },
  {
    label: '单位性质',
    field: 'unitNature',
    type: 'select',
    span: 12,
    required: true,
    props: {
      options: [
        { label: '企业', value: 'ENTERPRISE' },
        { label: '个人', value: 'PERSON' },
      ],
    },
  },
  { label: '信用代码', field: 'creditCode', type: 'input', span: 24, props: { maxLength: 64 } },
  { label: '地址', field: 'address', type: 'input', span: 24, props: { maxLength: 256 } },
  {
    label: '状态',
    field: 'status',
    type: 'switch',
    span: 24,
    props: { type: 'round', checkedValue: 1, uncheckedValue: 2, checkedText: '启用', uncheckedText: '禁用' },
  },
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
    if (isUpdate.value) {
      await updateUnit(form, dataId.value)
      Message.success('修改成功')
    } else {
      await addUnit(form)
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
  visible.value = true
}

const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  const { data } = await getUnit(id)
  Object.assign(form, data)
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>
