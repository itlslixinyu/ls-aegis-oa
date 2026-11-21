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
import { addAccount, getAccount, updateAccount } from '@/apis/psi'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useResetReactive } from '@/hooks'

const emit = defineEmits<{ (e: 'save-success'): void }>()
const { width } = useWindowSize()

const dataId = ref('')
const visible = ref(false)
const isUpdate = computed(() => !!dataId.value)
const title = computed(() => (isUpdate.value ? '修改银行账户' : '新增银行账户'))
const formRef = ref<InstanceType<typeof GiForm>>()

const [form, resetForm] = useResetReactive({
  status: 1,
})

const columns: ColumnItem[] = reactive([
  { label: '户名', field: 'accountName', type: 'input', span: 24, required: true, props: { maxLength: 128 } },
  { label: '开户行', field: 'bankName', type: 'input', span: 24, required: true, props: { maxLength: 128 } },
  { label: '账号', field: 'accountNo', type: 'input', span: 24, required: true, props: { maxLength: 64 } },
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
  dataId.value = ''
}

const save = async () => {
  try {
    const valid = await formRef.value?.formRef?.validate()
    if (valid) return false
    if (isUpdate.value) {
      await updateAccount(form, dataId.value)
      Message.success('修改成功')
    } else {
      await addAccount(form)
      Message.success('新增成功')
    }
    emit('save-success')
    return true
  } catch {
    return false
  }
}

const onAdd = async () => {
  reset()
  visible.value = true
}
const onUpdate = async (id: string) => {
  reset()
  dataId.value = id
  const { data } = await getAccount(id)
  Object.assign(form, data)
  visible.value = true
}

defineExpose({ onAdd, onUpdate })
</script>
