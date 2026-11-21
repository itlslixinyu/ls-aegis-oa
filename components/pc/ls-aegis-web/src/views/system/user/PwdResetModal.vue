<template>
  <a-modal
    v-model:visible="visible"
    title="重置密码"
    :mask-closable="false"
    :esc-to-close="false"
    :width="width >= 500 ? 500 : '100%'"
    draggable
    @before-ok="save"
    @close="reset"
  >
    <GiForm ref="formRef" v-model="form" :columns="columns" />
  </a-modal>
</template>

<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { useWindowSize } from '@vueuse/core'
import { resetUserPwd } from '@/apis/system'
import { type ColumnItem, GiForm } from '@/components/GiForm'
import { useResetReactive } from '@/hooks'
import { encryptTransport } from '@/utils/encrypt'

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const { width } = useWindowSize()
const dataId = ref('')
const visible = ref(false)
const formRef = ref<InstanceType<typeof GiForm>>()

const [form, resetForm] = useResetReactive({
  username: '',
  newPassword: undefined as string | undefined,
})

const columns: ColumnItem[] = reactive([
  // 与密码同属一个 form，满足浏览器「密码表单需含用户名字段」要求（视觉隐藏）
  {
    label: '用户名',
    field: 'username',
    type: 'input',
    span: 24,
    formItemProps: {
      style: { display: 'none' },
    },
    props: {
      autocomplete: 'username',
      readonly: true,
    },
  },
  {
    label: '密码',
    field: 'newPassword',
    type: 'input-password',
    span: 24,
    required: true,
    props: {
      autocomplete: 'new-password',
    },
  },
])

const reset = () => {
  formRef.value?.formRef?.resetFields()
  resetForm()
}

const save = async () => {
  try {
    const isInvalid = await formRef.value?.formRef?.validate()
    if (isInvalid) return false
    await resetUserPwd({ newPassword: await encryptTransport(form.newPassword) || '' }, dataId.value)
    Message.success('重置成功，用户下次登录须修改密码')
    emit('save-success')
    return true
  } catch (error) {
    return false
  }
}

const onOpen = (id: string, name?: string) => {
  reset()
  dataId.value = id
  form.username = name || ''
  visible.value = true
}

defineExpose({ onOpen })
</script>
